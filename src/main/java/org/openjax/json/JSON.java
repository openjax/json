/* Copyright (c) 2019 OpenJAX
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * You should have received a copy of The MIT License (MIT) along with this
 * program. If not, see <http://opensource.org/licenses/MIT/>.
 */

package org.openjax.json;

import static org.libj.lang.Assertions.*;
import static org.openjax.json.JSON.Type.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.libj.lang.Numbers;
import org.libj.lang.Strings;
import org.libj.util.CollectionUtil;
import org.libj.util.function.BooleanFunction;
import org.libj.util.function.ObjBiIntFunction;

/**
 * Lightweight {@code toString(...)} functions for parsing and marshaling {@link Map Map&lt;String,?&gt;} (JSON Object), {@link List
 * List&lt;?&gt;} (JSON Array), {@link Boolean} (JSON Boolean Value), {@link Number} (JSON Number Value), {@link String} (JSON
 * String Value) from and into JSON string representations.
 */
public final class JSON {
  /**
   * Enum class representing the JSON types.
   *
   * @param <T> The type parameter of the "JSON to object" factory function returned by {@link TypeMap#get(Type)}.
   */
  public static final class Type<T> {
    public static final Type<Function<String,?>> STRING;
    public static final Type<ObjBiIntFunction<char[],?>> NUMBER;
    public static final Type<BooleanFunction<String>> BOOLEAN;
    public static final Type<Supplier<Map<String,?>>> OBJECT;
    public static final Type<Supplier<List<?>>> ARRAY;

    private static byte index = 0;

    private static final Type<?>[] values = {
      STRING = new Type<>("STRING"),
      NUMBER = new Type<>("NUMBER"),
      BOOLEAN = new Type<>("BOOLEAN"),
      OBJECT = new Type<>("OBJECT"),
      ARRAY = new Type<>("ARRAY"),
    };

    public static Type<?>[] values() {
      return values;
    }

    private final byte ordinal;
    private final String name;

    private Type(final String name) {
      this.ordinal = index++;
      this.name = name;
    }

    public byte ordinal() {
      return ordinal;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  /**
   * A map between JSON {@linkplain Type} and the "JSON to object" factory function of type <code>Type{@link Type &lt;T&gt;}</code>.
   */
  @SuppressWarnings("unchecked")
  public static class TypeMap {
    private final Object[] map = new Object[5];

    /**
     * Associates the provided {@link Type key} with the specified "JSON to object" factory function of type
     * <code>Type{@link Type &lt;T&gt;}</code>.
     *
     * @param <T> The type parameter of the "JSON to object" factory function defined in {@link Type}.
     * @param key The {@link Type key}.
     * @param factory The "JSON to object" factory function of type <code>Type{@link Type &lt;T&gt;}</code>.
     * @return {@code this} instance.
     */
    public <T>TypeMap put(final Type<T> key, final T factory) {
      map[key.ordinal()] = factory;
      return this;
    }

    /**
     * Returns the function of type <code>Type{@link Type &lt;T&gt;}</code> for the provided {@link Type key}.
     *
     * @param <T> The type parameter of the "JSON to object" factory function defined in {@link Type}.
     * @param key The {@link Type key}.
     * @return The function of type <code>Type{@link Type &lt;T&gt;}</code> for the provided {@link Type key}.
     */
    public <T>T get(final Type<T> key) {
      return (T)map[key.ordinal()];
    }
  }

  private static Object parseValue(final JsonReader reader, final char[] chars, final int start, final int len, final TypeMap typeMap) {
    final char ch = chars[start];
    if (ch == '"') {
      if (chars[start + len - 1] != '"')
        throw new JsonParseException(new String(reader.buf(), 0, reader.getPosition()), start);

      final String s = JsonUtil.unescape(chars, start + 1, len - 2).toString();
      final Function<String,?> factory = typeMap == null ? null : typeMap.get(STRING);
      return factory == null ? s : factory.apply(s);
    }

    if (ch == 'n') {
      if (len != 4 || chars[start + 1] != 'u' || chars[start + 2] != 'l' || chars[start + 3] != 'l')
        throw new JsonParseException(new String(reader.buf(), 0, reader.getPosition()), start);

      return null;
    }

    if (ch == 't') {
      if (len != 4 || chars[start + 1] != 'r' || chars[start + 2] != 'u' || chars[start + 3] != 'e')
        throw new JsonParseException(new String(reader.buf(), 0, reader.getPosition()), start);

      final BooleanFunction<String> factory = typeMap == null ? null : typeMap.get(BOOLEAN);
      return factory == null ? Boolean.TRUE : factory.apply(Boolean.TRUE);
    }

    if (ch == 'f') {
      if (len != 5 || chars[start + 1] != 'a' || chars[start + 2] != 'l' || chars[start + 3] != 's' || chars[start + 4] != 'e')
        throw new JsonParseException(new String(reader.buf(), 0, reader.getPosition()), start);

      final BooleanFunction<String> factory = typeMap == null ? null : typeMap.get(BOOLEAN);
      return factory == null ? Boolean.FALSE : factory.apply(Boolean.FALSE);
    }

    if ('0' <= ch && ch <= '9' || ch == '-') {
      try {
        final ObjBiIntFunction<char[],?> factory = typeMap == null ? null : typeMap.get(NUMBER);
        return factory == null ? new BigDecimal(chars, start, len) : factory.apply(chars, start, len);
      }
      catch (final NumberFormatException e) {
        throw new JsonParseException(new String(reader.buf(), 0, reader.getPosition()), start, e);
      }
    }

    throw new JsonParseException(new String(reader.buf(), 0, reader.getPosition()), start);
  }

  /**
   * Parses the JSON document provided by the specified {@link String json}, returning:
   * <ul>
   * <li>A {@link Map Map&lt;String,?&gt;}, if the document started with <code>{</code> (i.e. the document is a JSON Object).</li>
   * <li>A {@link List List&lt;?&gt;}, if the document started with <code>[</code> (i.e. the document is a JSON Array).</li>
   * <li>A {@link String}, if the document represents a JSON String Value.</li>
   * <li>A {@link Boolean}, if the document represents a JSON Boolean Value.</li>
   * <li>A {@link BigDecimal}, if the document represents a JSON Number Value.</li>
   * <li>{@code null}, if the document represents a JSON Null Value.</li>
   * </ul>
   *
   * @param json The input {@link String} providing the JSON document.
   * @return A string of the JSON document provided by the specified {@link Reader reader} with insignificant whitespace stripped.
   * @throws JsonParseException If a violation has occurred of the JSON document well-formed criteria as expressed by the RFC 4627
   *           specification.
   * @throws IllegalArgumentException If {@code reader} is null.
   */
  public static Object parse(final String json) throws JsonParseException {
    try (final StringReader reader = new StringReader(assertNotNull(json))) {
      return parse(reader, null);
    }
    catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Parses the JSON document provided by the specified {@link String json}, returning:
   * <ul>
   * <li>A {@link Map Map&lt;String,?&gt;}, if the document started with <code>{</code> (i.e. the document is a JSON Object).</li>
   * <li>A {@link List List&lt;?&gt;}, if the document started with <code>[</code> (i.e. the document is a JSON Array).</li>
   * <li>A {@link String}, if the document represents a JSON String Value.</li>
   * <li>A {@link Boolean}, if the document represents a JSON Boolean Value.</li>
   * <li>A {@link BigDecimal}, if the document represents a JSON Number Value.</li>
   * <li>{@code null}, if the document represents a JSON Null Value.</li>
   * </ul>
   *
   * @param json The input {@link String} providing the JSON document.
   * @param typeMap The {@link TypeMap} with factory mappings for JSON {@linkplain Type types}.
   * @return A string of the JSON document provided by the specified {@link Reader reader} with insignificant whitespace stripped.
   * @throws JsonParseException If a violation has occurred of the JSON document well-formed criteria as expressed by the RFC 4627
   *           specification.
   * @throws IllegalArgumentException If {@code reader} is null.
   */
  public static Object parse(final String json, final TypeMap typeMap) throws JsonParseException {
    try (final StringReader reader = new StringReader(assertNotNull(json))) {
      return parse(reader, typeMap);
    }
    catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Parses the JSON document provided by the specified {@link Reader reader}, returning:
   * <ul>
   * <li>A {@link Map Map&lt;String,?&gt;}, if the document started with <code>{</code> (i.e. the document is a JSON Object).</li>
   * <li>A {@link List List&lt;?&gt;}, if the document started with <code>[</code> (i.e. the document is a JSON Array).</li>
   * <li>A {@link String}, if the document represents a JSON String Value.</li>
   * <li>A {@link Boolean}, if the document represents a JSON Boolean Value.</li>
   * <li>A {@link BigDecimal}, if the document represents a JSON Number Value.</li>
   * <li>{@code null}, if the document represents a JSON Null Value.</li>
   * </ul>
   *
   * @param reader The input {@link Reader} providing the JSON document.
   * @return A string of the JSON document provided by the specified {@link Reader reader} with insignificant whitespace stripped.
   * @throws JsonParseException If a violation has occurred of the JSON document well-formed criteria as expressed by the RFC 4627
   *           specification.
   * @throws IOException If an I/O error has occurred.
   * @throws IllegalArgumentException If {@code reader} is null.
   */
  // FIXME: Test: null, JSON value
  public static Object parse(final Reader reader) throws JsonParseException, IOException {
    return parse(reader, null);
  }

  /**
   * Parses the JSON document provided by the specified {@link Reader reader}, returning:
   * <ul>
   * <li>A {@link Map Map&lt;String,?&gt;}, if the document started with <code>{</code> (i.e. the document is a JSON Object).</li>
   * <li>A {@link List List&lt;?&gt;}, if the document started with <code>[</code> (i.e. the document is a JSON Array).</li>
   * <li>A {@link String}, if the document represents a JSON String Value.</li>
   * <li>A {@link Boolean}, if the document represents a JSON Boolean Value.</li>
   * <li>A {@link BigDecimal}, if the document represents a JSON Number Value.</li>
   * <li>{@code null}, if the document represents a JSON Null Value.</li>
   * </ul>
   *
   * @param reader The input {@link Reader} providing the JSON document.
   * @param typeMap The {@link TypeMap} with factory mappings for JSON {@linkplain Type types}.
   * @return A string of the JSON document provided by the specified {@link Reader reader} with insignificant whitespace stripped.
   * @throws JsonParseException If a violation has occurred of the JSON document well-formed criteria as expressed by the RFC 4627
   *           specification.
   * @throws IOException If an I/O error has occurred.
   * @throws IllegalArgumentException If {@code reader} is null.
   */
  // FIXME: Test: null, JSON value
  public static Object parse(final Reader reader, final TypeMap typeMap) throws JsonParseException, IOException {
    final List<Object> stack = new ArrayList<>();
    try (final JsonReader in = JsonReader.of(reader)) {
      new JsonParser() {
        private final List<String> propertyNames = new ArrayList<>();
        private Object current;
        private String propertyName;

        @Override
        public void startDocument() {
        }

        @Override
        public void endDocument() {
        }

        @Override
        public boolean whitespace(final char[] chars, final int start, final int len) {
          return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean structural(final char ch) {
          if (ch == '}' || ch == ']') {
            final int head = stack.size() - 1;
            if (head == 0)
              return true;

            final Object object = stack.remove(head);
            current = stack.get(head - 1);
            propertyName = propertyNames.remove(head);
            if (propertyName != null) {
              ((Map<String,Object>)current).put(propertyName, object);
              propertyName = null;
            }
            else if (current != null) {
              ((List<Object>)current).add(object);
            }
          }
          else if (ch == ':' || ch == ',') {
          }
          else {
            if (ch == '{') {
              final Supplier<Map<String,?>> factory = typeMap == null ? null : typeMap.get(Type.OBJECT);
              stack.add(current = factory == null ? new LinkedHashMap<>() : factory.get());
            }
            else if (ch == '[') {
              final Supplier<List<?>> factory = typeMap == null ? null : typeMap.get(Type.ARRAY);
              stack.add(current = factory == null ? new ArrayList<>() : factory.get());
            }

            propertyNames.add(propertyName);
            propertyName = null;
          }

          return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean characters(final char[] chars, final int start, final int len) {
          if (current instanceof List) {
            ((List<Object>)current).add(parseValue(in, chars, start, len, typeMap));
          }
          else if (propertyName == null) {
            propertyName = new String(chars, start + 1, len - 2);
          }
          else {
            ((Map<String,Object>)current).put(propertyName, parseValue(in, chars, start, len, typeMap));
            propertyName = null;
          }

          return true;
        }
      }.parse(in);

      return stack.get(0);
    }
  }

  /**
   * Returns a string of the JSON document provided by the specified {@code json} string with insignificant whitespace stripped.
   *
   * @param json The input {@link String} with the JSON document.
   * @return A string of the JSON document provided by the specified {@code json} string with insignificant whitespace stripped.
   * @throws JsonParseException If a violation has occurred of the JSON document well-formed criteria as expressed by the RFC 4627
   *           specification.
   * @throws IllegalArgumentException If {@code json} is null.
   */
  public static String stripWhitespace(final String json) throws JsonParseException {
    try (final StringReader reader = new StringReader(assertNotNull(json))) {
      return stripWhitespace(reader);
    }
    catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Returns a {@link String} of the JSON document provided by the specified {@link Reader reader} with insignificant whitespace
   * stripped.
   *
   * @param reader The input {@link Reader} providing the JSON document.
   * @return A string of the JSON document provided by the specified {@link Reader reader} with insignificant whitespace stripped.
   * @throws JsonParseException If a violation has occurred of the JSON document well-formed criteria as expressed by the RFC 4627
   *           specification.
   * @throws IOException If an I/O error has occurred.
   * @throws IllegalArgumentException If {@code reader} is null.
   */
  public static String stripWhitespace(final Reader reader) throws JsonParseException, IOException {
    final StringBuilder builder = new StringBuilder();
    try (final JsonReader in = JsonReader.of(reader)) {
      new JsonParser() {
        @Override
        public void startDocument() {
        }

        @Override
        public void endDocument() {
        }

        @Override
        public boolean whitespace(final char[] chars, final int start, final int len) {
          return true;
        }

        @Override
        public boolean structural(final char ch) {
          builder.append(ch);
          return true;
        }

        @Override
        public boolean characters(final char[] chars, final int start, final int len) {
          if (chars[start] == '"') {
            builder.append('"');
            JsonUtil.escape(builder, chars, start + 1, len - 2);
            builder.append('"');
          }
          else {
            builder.append(chars, start, len);
          }

          return true;
        }
      }.parse(in);

      return builder.toString();
    }
  }

  private static final String[] indentsArray = {"", " ", "  ", "   ", "    ", "     ", "      ", "       ", "        ", "         ", "          ", "           ", "            ", "             ", "              ", "               ", "                ", "                 ", "                  ", "                   ", "                    "};
  private static final Map<Integer,String> indentsMap = new HashMap<>();

  private static String makeIndent(final int indent) {
    if (indent <= 20)
      return indentsArray[indent];

    String spaces = indentsMap.get(indent);
    if (spaces == null)
      indentsMap.put(indent, spaces = Strings.repeat(' ', indent));

    return spaces;
  }

  @SuppressWarnings("unchecked")
  private static StringBuilder encode(final StringBuilder builder, final Object obj, final int indent, final String spaces) {
    if (obj == null)
      return builder.append("null");

    if (obj instanceof String) {
      builder.append('"');
      JsonUtil.escape(builder, (String)obj);
      return builder.append('"');
    }

    if (obj instanceof Float)
      return builder.append(toString(((Float)obj).floatValue()));

    if (obj instanceof Double)
      return builder.append(toString(((Double)obj).doubleValue()));

    if (obj instanceof Number || obj instanceof Boolean)
      return builder.append(obj);

    if (obj instanceof List)
      return toString(builder, (List<?>)obj, indent, spaces);

    if (obj instanceof Map)
      return toString(builder, (Map<String,?>)obj, indent, spaces);

    throw new IllegalArgumentException("Illegal object of class: " + obj.getClass().getName());
  }

  /**
   * Returns a JSON string encoding of the {@code json} object of class:
   * <ul>
   * <li>{@link Map Map&lt;String,?&gt;} representing a JSON Object.</li>
   * <li>{@link List List&lt;?&gt;} representing a JSON Array.</li>
   * <li>{@link String} representing a JSON String Value.</li>
   * <li>{@link Number} representing a JSON Number Value.</li>
   * <li>{@link Boolean} representing a JSON Boolean Value.</li>
   * <li>{@code null} representing a JSON Null Value.</li>
   * </ul>
   *
   * @implNote The property values of the specified map may only be instances of {@link Map Map&lt;String,?&gt;}, {@link List
   *           List&lt;?&gt;}, {@link String}, {@link Number}, {@link Boolean}, and {@code null}. Objects of other classes will
   *           result in an {@link IllegalArgumentException}.
   * @param json The JSON value represented as a {@link Map Map&lt;String,?&gt;}, {@link List List&lt;?&gt;}, {@link String},
   *          {@link Number}, {@link Boolean}, and {@code null}.
   * @return A string encoding of the {@code json} object.
   * @throws IllegalArgumentException If {@code json} is null, or if {@code json} or a property value of the specified {@link Map
   *           Map&lt;String,?&gt;} or member of the {@link List List&lt;?&gt;} is of a class that is not one of {@link Map
   *           Map&lt;String,?&gt;}, {@link List List&lt;?&gt;}, {@link String}, {@link Number}, {@link Boolean}, and {@code null}.
   */
  public static String toString(final Object json) {
    return toString(json, 0);
  }

  /**
   * Returns a JSON string encoding of the {@code json} object of class:
   * <ul>
   * <li>{@link Map Map&lt;String,?&gt;} representing a JSON Object.</li>
   * <li>{@link List List&lt;?&gt;} representing a JSON Array.</li>
   * <li>{@link String} representing a JSON String Value.</li>
   * <li>{@link Number} representing a JSON Number Value.</li>
   * <li>{@link Boolean} representing a JSON Boolean Value.</li>
   * <li>{@code null} representing a JSON Null Value.</li>
   * </ul>
   *
   * @implNote The property values of the specified map may only be instances of {@link Map Map&lt;String,?&gt;}, {@link List
   *           List&lt;?&gt;}, {@link String}, {@link Number}, {@link Boolean}, and {@code null}. Objects of other classes will
   *           result in an {@link IllegalArgumentException}.
   * @param json The JSON value represented as a {@link Map Map&lt;String,?&gt;}, {@link List List&lt;?&gt;}, {@link String},
   *          {@link Number}, {@link Boolean}, and {@code null}.
   * @param indent Number of spaces to indent child elements. If the specified indent value is greater than {@code 0}, child
   *          elements are indented and placed on a new line. If the indent value is {@code 0}, child elements are not indented, nor
   *          placed on a new line.
   * @return A string encoding of the {@code json} object.
   * @throws IllegalArgumentException If {@code json} is null, or if {@code json} or a property value of the specified {@link Map
   *           Map&lt;String,?&gt;} or member of the {@link List List&lt;?&gt;} is of a class that is not one of {@link Map
   *           Map&lt;String,?&gt;}, {@link List List&lt;?&gt;}, {@link String}, {@link Number}, {@link Boolean} and {@code null},
   *           or if {@code indent} is negative.
   */
  @SuppressWarnings("unchecked")
  public static String toString(final Object json, final int indent) {
    if (json == null)
      return "null";

    if (json instanceof Map)
      return toString((Map<String,?>)json, indent);

    if (json instanceof List)
      return toString((List<?>)json, indent);

    if (json instanceof CharSequence) {
      final StringBuilder s = new StringBuilder();
      s.append('"');
      JsonUtil.escape(s, (CharSequence)json);
      s.append('"');
      return s.toString();
    }

    if (json instanceof Float)
      return toString(((Float)json).floatValue());

    if (json instanceof Double)
      return toString(((Double)json).doubleValue());

    if (json instanceof Number || json instanceof Boolean)
      return json.toString();

    throw new IllegalArgumentException("Unknown object class: " + json.getClass().getName());
  }

  /**
   * Returns a JSON string encoding of the provided {@code float}. If the provided {@code float} represents {@code NaN} or infinity,
   * this method returns {@code "null"}. Otherwise, for all other {@code float} values, this method returns
   * {@link Float#toString(float)}.
   *
   * @param json The {@code float} to encode.
   * @return A JSON string encoding of the provided {@code float}.
   */
  public static String toString(final float json) {
    return !Float.isFinite(json) ? "null" : Numbers.isWhole(json) ? Integer.toString((int)json) : Float.toString(json);
  }

  /**
   * Returns a JSON string encoding of the provided {@code double}. If the provided {@code double} represents {@code NaN} or
   * infinity, this method returns {@code "null"}. Otherwise, for all other {@code double} values, this method returns
   * {@link Double#toString(double)}.
   *
   * @param json The {@code double} to encode.
   * @return A JSON string encoding of the provided {@code double}.
   */
  public static String toString(final double json) {
    return !Double.isFinite(json) ? "null" : Numbers.isWhole(json) ? Long.toString((long)json) : Double.toString(json);
  }

  /**
   * Returns a JSON string encoding of the specified {@link Map Map&lt;String,?&gt;} representing a JSON object, or {@code null} if
   * {@code object} is null.
   *
   * @implNote The property values of the specified map may only be instances of {@link Map Map&lt;String,?&gt;}, {@link List
   *           List&lt;?&gt;}, {@link String}, {@link Number}, {@link Boolean}, and {@code null}. Objects of other classes will
   *           result in an {@link IllegalArgumentException}.
   * @param object The JSON object, represented as a {@link Map Map&lt;String,?&gt;}.
   * @return A JSON string encoding of the specified {@link Map Map&lt;String,?&gt;} representing a JSON object.
   * @throws IllegalArgumentException If {@code object} is null, or if a property value of the specified {@link Map
   *           Map&lt;String,?&gt;} is of a class that is not one of {@link Map Map&lt;String,?&gt;}, {@link List List&lt;?&gt;},
   *           {@link String}, {@link Number}, {@link Boolean}, and {@code null}.
   */
  public static String toString(final Map<String,?> object) {
    return object == null ? "null" : toString(object, 0);
  }

  /**
   * Returns a JSON string encoding of the specified {@link Map Map&lt;String,?&gt;} representing a JSON object, or {@code null} if
   * {@code object} is null.
   *
   * @implNote The property values of the specified map may only be instances of {@link Map Map&lt;String,?&gt;}, {@link List
   *           List&lt;?&gt;}, {@link String}, {@link Number}, {@link Boolean}, and {@code null}. Objects of other classes will
   *           result in an {@link IllegalArgumentException}.
   * @param object The JSON object, represented as a {@link Map Map&lt;String,?&gt;}.
   * @param indent Number of spaces to indent child elements. If the specified indent value is greater than {@code 0}, child
   *          elements are indented and placed on a new line. If the indent value is {@code 0}, child elements are not indented, nor
   *          placed on a new line.
   * @return A JSON string encoding of the specified {@link Map Map&lt;String,?&gt;} representing a JSON object.
   * @throws IllegalArgumentException If {@code object} is null, or if a property value of the specified {@link Map
   *           Map&lt;String,?&gt;} is of a class that is not one of {@link Map Map&lt;String,?&gt;}, {@link List List&lt;?&gt;},
   *           {@link String}, {@link Number}, {@link Boolean} and {@code null}, or if {@code indent} is negative.
   */
  public static String toString(final Map<String,?> object, final int indent) {
    assertNotNegative(indent, "indent (%d) must be non-negative", indent);
    return object == null ? "null" : toString(new StringBuilder(), object, indent, indent == 0 ? null : makeIndent(indent)).toString();
  }

  private static StringBuilder toString(final StringBuilder builder, final Map<String,?> object, final int indent, final String spaces) {
    builder.append('{');
    if (object.size() > 0) {
      for (final Map.Entry<String,?> entry : object.entrySet()) { // [S]
        if (spaces != null)
          builder.append('\n').append(spaces);

        builder.append("\"").append(JsonUtil.escape(entry.getKey())).append("\":");
        if (spaces != null)
          builder.append(' ');

        JSON.encode(builder, entry.getValue(), indent, spaces == null ? null : makeIndent(spaces.length() + indent));
        builder.append(',');
      }

      if (spaces == null)
        builder.setLength(builder.length() - 1);
      else
        builder.setCharAt(builder.length() - 1, '\n');
    }

    if (spaces != null)
      builder.append(makeIndent(spaces.length() - indent));

    return builder.append('}');
  }

  /**
   * Returns a JSON string encoding (with no indentation) of the specified {@link List List&lt;?&gt;} representing a JSON array, or
   * {@code null} if {@code array} is null.
   *
   * @implNote The property values of the specified map may only be instances of {@link Map Map&lt;String,?&gt;}, {@link List
   *           List&lt;?&gt;}, {@link String}, {@link Number}, {@link Boolean}, and {@code null}. Objects of other classes will
   *           result in an {@link IllegalArgumentException}.
   * @param array The JSON array, represented as a {@link List List&lt;?&gt;}.
   * @return A JSON string encoding of the specified {@link List List&lt;?&gt;} representing a JSON array, or {@code null} if
   *         {@code array} is null.
   * @throws IllegalArgumentException If {@code array} is null, or if a member value of the specified {@link List List&lt;?&gt;} is
   *           of a class that is not one of {@link Map Map&lt;String,?&gt;}, {@link List List&lt;?&gt;}, {@link String},
   *           {@link Number}, {@link Boolean}, and {@code null}.
   */
  public static String toString(final List<?> array) {
    return toString(array, 0);
  }

  /**
   * Returns a JSON string encoding (with the specified indentation) of the specified {@link List List&lt;?&gt;} representing a JSON
   * array, or {@code null} if {@code array} is null.
   *
   * @implNote The property values of the specified map may only be instances of {@link Map Map&lt;String,?&gt;}, {@link List
   *           List&lt;?&gt;}, {@link String}, {@link Number}, {@link Boolean}, and {@code null}. Objects of other classes will
   *           result in an {@link IllegalArgumentException}.
   * @param array The JSON array, represented as a {@link List List&lt;?&gt;}.
   * @param indent Number of spaces to indent child elements. If the specified indent value is greater than {@code 0}, child
   *          elements are indented and placed on a new line. If the indent value is {@code 0}, child elements are not indented, nor
   *          placed on a new line.
   * @return A JSON string encoding of the specified {@link List List&lt;?&gt;} representing a JSON array, or {@code null} if
   *         {@code array} is null.
   * @throws IllegalArgumentException If {@code array} is null, or if a member value of the specified {@link List List&lt;?&gt;} is
   *           of a class that is not one of {@link Map Map&lt;String,?&gt;}, {@link List List&lt;?&gt;}, {@link String},
   *           {@link Number}, {@link Boolean} and {@code null}, or if {@code indent} is negative.
   */
  public static String toString(final List<?> array, final int indent) {
    assertNotNegative(indent, "indent (%d) must be non-negative", indent);
    return array == null ? "null" : toString(new StringBuilder(), array, indent, indent == 0 ? null : makeIndent(indent)).toString();
  }

  private static StringBuilder toString(final StringBuilder builder, final List<?> array, final int indent, final String spaces) {
    builder.append('[');
    boolean backUp = false;
    final int size = array.size();
    if (size > 0) {
      if (CollectionUtil.isRandomAccess(array)) {
        for (int i = 0; i < size; ++i) // [RA]
          backUp = toString(builder, array.get(i), spaces, indent, backUp, i);
      }
      else {
        final Iterator<?> iterator = array.iterator();
        for (int i = 0; i < size; ++i) // [I]
          backUp = toString(builder, iterator.next(), spaces, indent, backUp, i);
      }

      if (backUp || spaces == null) {
        builder.setLength(builder.length() - 1);
      }
      else {
        builder.setCharAt(builder.length() - 1, '\n');
        builder.append(makeIndent(spaces.length() - indent));
      }
    }

    return builder.append(']');
  }

  private static boolean toString(final StringBuilder builder, final Object member, final String spaces, final int indent, boolean backUp, final int i) {
    if (member instanceof Map || member instanceof List) {
      if (i > 0 && spaces != null)
        builder.append(' ');
      else
        backUp = true;
    }
    else if (spaces != null) {
      builder.append('\n').append(spaces);
    }

    JSON.encode(builder, member, indent, spaces == null ? null : makeIndent(spaces.length()));
    builder.append(',');
    return backUp;
  }

  private JSON() {
  }
}