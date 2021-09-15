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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.libj.lang.Assertions;

/**
 * Lightweight {@code toString(...)} functions for parsing and marshaling
 * {@link Map Map&lt;String,?&gt;} (JSON Object), {@link List List&lt;?&gt;}
 * (JSON Array), {@link Boolean} (JSON Boolean Value), {@link Number} (JSON
 * Number Value), {@link String} (JSON String Value) from and into JSON string
 * representations.
 */
public final class JSON {
  private static Object parseValue(final JsonReader reader, final char[] chars, final int start, final int len) {
    final char ch = chars[start];
    if (ch == 'n') {
      if (len != 4 || chars[start + 1] != 'u' || chars[start + 2] != 'l' || chars[start + 3] != 'l')
        throw new JsonParseException(new String(reader.buf(), 0, reader.getPosition()), start);

      return null;
    }

    if (ch == 't') {
      if (len != 4 || chars[start + 1] != 'r' || chars[start + 2] != 'u' || chars[start + 3] != 'e')
        throw new JsonParseException(new String(reader.buf(), 0, reader.getPosition()), start);

      return Boolean.TRUE;
    }

    if (ch == 'f') {
      if (len != 5 || chars[start + 1] != 'a' || chars[start + 2] != 'l' || chars[start + 3] != 's' || chars[start + 4] != 'e')
        throw new JsonParseException(new String(reader.buf(), 0, reader.getPosition()), start);

      return Boolean.FALSE;
    }

    if (ch == '"') {
      if (chars[start + len - 1] != '"')
        throw new JsonParseException(new String(reader.buf(), 0, reader.getPosition()), start);

      return JsonUtil.unescape(chars, start + 1, len - 2).toString();
    }

    if ('0' <= ch && ch <= '9' || ch == '-') {
      try {
        return new BigDecimal(chars, start, len);
      }
      catch (final NumberFormatException e) {
        throw new JsonParseException(new String(reader.buf(), 0, reader.getPosition()), start, e);
      }
    }

    throw new JsonParseException(new String(reader.buf(), 0, reader.getPosition()), start);
  }

  /**
   * Parses the JSON document provided by the specified {@link String json},
   * returning:
   * <ul>
   * <li>A {@link Map Map&lt;String,?&gt;}, if the document started with
   * <code>{</code> (i.e. the document is a JSON Object).</li>
   * <li>A {@link List List&lt;?&gt;}, if the document started with
   * <code>[</code> (i.e. the document is a JSON Array).</li>
   * <li>A {@link String}, if the document represents a JSON String Value.</li>
   * <li>A {@link Boolean}, if the document represents a JSON Boolean
   * Value.</li>
   * <li>A {@link BigDecimal}, if the document represents a JSON Number
   * Value.</li>
   * <li>{@code null}, if the document represents a JSON Null Value.</li>
   * </ul>
   *
   * @param json The input {@link String} providing the JSON document.
   * @return A string of the JSON document provided by the specified
   *         {@link Reader reader} with insignificant whitespace stripped.
   * @throws JsonParseException If a violation has occurred of the JSON document
   *           well-formed criteria as expressed by the RFC 4627 specification.
   * @throws IOException If an I/O error has occurred.
   * @throws IllegalArgumentException If {@code reader} is null.
   */
  public static Object parse(final String json) throws JsonParseException, IOException {
    try (final StringReader reader = new StringReader(Assertions.assertNotNull(json))) {
      return parse(reader);
    }
  }

  /**
   * Parses the JSON document provided by the specified {@link Reader reader},
   * returning:
   * <ul>
   * <li>A {@link Map Map&lt;String,?&gt;}, if the document started with
   * <code>{</code> (i.e. the document is a JSON Object).</li>
   * <li>A {@link List List&lt;?&gt;}, if the document started with
   * <code>[</code> (i.e. the document is a JSON Array).</li>
   * <li>A {@link String}, if the document represents a JSON String Value.</li>
   * <li>A {@link Boolean}, if the document represents a JSON Boolean
   * Value.</li>
   * <li>A {@link BigDecimal}, if the document represents a JSON Number
   * Value.</li>
   * <li>{@code null}, if the document represents a JSON Null Value.</li>
   * </ul>
   *
   * @param reader The input {@link Reader} providing the JSON document.
   * @return A string of the JSON document provided by the specified
   *         {@link Reader reader} with insignificant whitespace stripped.
   * @throws JsonParseException If a violation has occurred of the JSON document
   *           well-formed criteria as expressed by the RFC 4627 specification.
   * @throws IOException If an I/O error has occurred.
   * @throws IllegalArgumentException If {@code reader} is null.
   */
  public static Object parse(final Reader reader) throws JsonParseException, IOException {
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
            if (ch == '{')
              stack.add(current = new LinkedHashMap<>());
            else if (ch == '[')
              stack.add(current = new ArrayList<>());

            propertyNames.add(propertyName);
            propertyName = null;
          }

          return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean characters(final char[] chars, final int start, final int len) {
          if (current instanceof List) {
            ((List<Object>)current).add(parseValue(in, chars, start, len));
          }
          else if (propertyName == null) {
            propertyName = new String(chars, start + 1, len - 2);
          }
          else {
            ((Map<String,Object>)current).put(propertyName, parseValue(in, chars, start, len));
            propertyName = null;
          }

          return true;
        }
      }.parse(in);

      return stack.get(0);
    }
  }

  /**
   * Returns a string of the JSON document provided by the specified
   * {@code json} string with insignificant whitespace stripped.
   *
   * @param json The input {@link String} with the JSON document.
   * @return A string of the JSON document provided by the specified
   *         {@code json} string with insignificant whitespace stripped.
   * @throws JsonParseException If a violation has occurred of the JSON document
   *           well-formed criteria as expressed by the RFC 4627 specification.
   * @throws IOException If an I/O error has occurred.
   * @throws IllegalArgumentException If {@code json} is null.
   */
  public static String stripWhitespace(final String json) throws JsonParseException, IOException {
    try (final StringReader reader = new StringReader(Assertions.assertNotNull(json))) {
      return stripWhitespace(reader);
    }
  }

  /**
   * Returns a {@link String} of the JSON document provided by the specified
   * {@link Reader reader} with insignificant whitespace stripped.
   *
   * @param reader The input {@link Reader} providing the JSON document.
   * @return A string of the JSON document provided by the specified
   *         {@link Reader reader} with insignificant whitespace stripped.
   * @throws JsonParseException If a violation has occurred of the JSON document
   *           well-formed criteria as expressed by the RFC 4627 specification.
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

  private static String makeIndent(final int indent) {
    if (indent == 0)
      return null;

    if (indent < 0)
      throw new IllegalArgumentException("indent (" + indent + ") must be non-negative");

    final char[] chars = new char[indent + 1];
    Arrays.fill(chars, 1, chars.length, ' ');
    chars[0] = '\n';
    return new String(chars);
  }

  @SuppressWarnings("unchecked")
  private static String encode(final Object obj, final String spaces) {
    if (obj == null)
      return "null";

    if (obj instanceof String)
      return JsonUtil.escape((String)obj).insert(0, '"').append('"').toString();

    if (obj instanceof Boolean || obj instanceof Number)
      return obj.toString();

    if (obj instanceof List) {
      final String list = toString((List<?>)obj, spaces);
      return spaces == null ? list : list.replace("\n", spaces);
    }

    if (obj instanceof Map) {
      final String map = toString((Map<String,?>)obj, spaces);
      return spaces == null ? map : map.replace("\n", spaces);
    }

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
   * @implNote The property values of the specified map may only be instances of
   *           {@link Map Map&lt;String,?&gt;}, {@link List List&lt;?&gt;},
   *           {@link String}, {@link Number}, {@link Boolean}, and
   *           {@code null}. Objects of other classes will result in an
   *           {@link IllegalArgumentException}.
   * @param json The JSON value represented as a {@link Map
   *          Map&lt;String,?&gt;}, {@link List List&lt;?&gt;}, {@link String},
   *          {@link Number}, {@link Boolean}, and {@code null}.
   * @return A string encoding of the {@code json} object.
   * @throws IllegalArgumentException If {@code json} is null, or if
   *           {@code json} or a property value of the specified {@link Map
   *           Map&lt;String,?&gt;} or member of the {@link List List&lt;?&gt;}
   *           is of a class that is not one of {@link Map Map&lt;String,?&gt;},
   *           {@link List List&lt;?&gt;}, {@link String}, {@link Number},
   *           {@link Boolean}, and {@code null}.
   */
  @SuppressWarnings("unchecked")
  public static String toString(final Object json) {
    if (json == null)
      return null;

    if (json instanceof Map)
      return toString((Map<String,?>)json);

    if (json instanceof List)
      return toString((List<?>)json);

    if (json instanceof CharSequence) {
      final StringBuilder s = new StringBuilder();
      s.append('"');
      JsonUtil.escape(s, (CharSequence)json);
      s.append('"');
      return s.toString();
    }

    if (json instanceof Number || json instanceof Boolean)
      return String.valueOf(json);

    throw new IllegalArgumentException("Unknown object type: " + json.getClass().getName());
  }

  /**
   * Returns a JSON string encoding of the specified {@link Map
   * Map&lt;String,?&gt;} representing a JSON object, or {@code null} if
   * {@code object} is null.
   *
   * @implNote The property values of the specified map may only be instances of
   *           {@link Map Map&lt;String,?&gt;}, {@link List List&lt;?&gt;},
   *           {@link String}, {@link Number}, {@link Boolean}, and
   *           {@code null}. Objects of other classes will result in an
   *           {@link IllegalArgumentException}.
   * @param object The JSON object, represented as a {@link Map
   *          Map&lt;String,?&gt;}.
   * @return A JSON string encoding of the specified {@link Map
   *         Map&lt;String,?&gt;} representing a JSON object.
   * @throws IllegalArgumentException If {@code object} is null, or if a
   *           property value of the specified {@link Map Map&lt;String,?&gt;}
   *           is of a class that is not one of {@link Map Map&lt;String,?&gt;},
   *           {@link List List&lt;?&gt;}, {@link String}, {@link Number},
   *           {@link Boolean}, and {@code null}.
   */
  public static String toString(final Map<String,?> object) {
    return object == null ? null : toString(object, 0);
  }

  /**
   * Returns a JSON string encoding of the specified {@link Map
   * Map&lt;String,?&gt;} representing a JSON object, or {@code null} if
   * {@code object} is null.
   *
   * @implNote The property values of the specified map may only be instances of
   *           {@link Map Map&lt;String,?&gt;}, {@link List List&lt;?&gt;},
   *           {@link String}, {@link Number}, {@link Boolean}, and
   *           {@code null}. Objects of other classes will result in an
   *           {@link IllegalArgumentException}.
   * @param object The JSON object, represented as a {@link Map
   *          Map&lt;String,?&gt;}.
   * @param indent Number of spaces to indent child elements. If the specified
   *          indent value is greater than {@code 0}, child elements are
   *          indented and placed on a new line. If the indent value is
   *          {@code 0}, child elements are not indented, nor placed on a new
   *          line.
   * @return A JSON string encoding of the specified {@link Map
   *         Map&lt;String,?&gt;} representing a JSON object.
   * @throws IllegalArgumentException If {@code object} is null, or if a
   *           property value of the specified {@link Map Map&lt;String,?&gt;}
   *           is of a class that is not one of {@link Map Map&lt;String,?&gt;},
   *           {@link List List&lt;?&gt;}, {@link String}, {@link Number},
   *           {@link Boolean} and {@code null}, or if {@code indent} is
   *           negative.
   */
  public static String toString(final Map<String,?> object, final int indent) {
    return object == null ? null : toString(object, makeIndent(indent));
  }

  private static String toString(final Map<String,?> object, final String spaces) {
    final StringBuilder builder = new StringBuilder();
    builder.append('{');
    if (object != null)
      for (final Map.Entry<String,?> entry : object.entrySet()) {
        if (spaces != null)
          builder.append(spaces);

        builder.append("\"").append(JsonUtil.escape(entry.getKey())).append("\":");
        if (spaces != null)
          builder.append(' ');

        builder.append(encode(entry.getValue(), spaces)).append(',');
      }

    if (builder.length() > 1) {
      if (spaces == null)
        builder.setLength(builder.length() - 1);
      else
        builder.setCharAt(builder.length() - 1, '\n');
    }

    return builder.append('}').toString();
  }

  /**
   * Returns a JSON string encoding (with no indentation) of the specified
   * {@link List List&lt;?&gt;} representing a JSON array, or {@code null} if
   * {@code array} is null.
   *
   * @implNote The property values of the specified map may only be instances of
   *           {@link Map Map&lt;String,?&gt;}, {@link List List&lt;?&gt;},
   *           {@link String}, {@link Number}, {@link Boolean}, and
   *           {@code null}. Objects of other classes will result in an
   *           {@link IllegalArgumentException}.
   * @param array The JSON array, represented as a {@link List List&lt;?&gt;}.
   * @return A JSON string encoding of the specified {@link List
   *         List&lt;?&gt;} representing a JSON array, or {@code null} if
   *         {@code array} is null.
   * @throws IllegalArgumentException If {@code array} is null, or if a member
   *           value of the specified {@link List List&lt;?&gt;} is of a class
   *           that is not one of {@link Map Map&lt;String,?&gt;}, {@link List
   *           List&lt;?&gt;}, {@link String}, {@link Number}, {@link Boolean},
   *           and {@code null}.
   */
  public static String toString(final List<?> array) {
    return toString(array, 0);
  }

  /**
   * Returns a JSON string encoding (with the specified indentation) of the
   * specified {@link List List&lt;?&gt;} representing a JSON array, or
   * {@code null} if {@code array} is null.
   *
   * @implNote The property values of the specified map may only be instances of
   *           {@link Map Map&lt;String,?&gt;}, {@link List List&lt;?&gt;},
   *           {@link String}, {@link Number}, {@link Boolean}, and
   *           {@code null}. Objects of other classes will result in an
   *           {@link IllegalArgumentException}.
   * @param array The JSON array, represented as a {@link List List&lt;?&gt;}.
   * @param indent Number of spaces to indent child elements. If the specified
   *          indent value is greater than {@code 0}, child elements are
   *          indented and placed on a new line. If the indent value is
   *          {@code 0}, child elements are not indented, nor placed on a new
   *          line.
   * @return A JSON string encoding of the specified {@link List
   *         List&lt;?&gt;} representing a JSON array, or {@code null} if
   *         {@code array} is null.
   * @throws IllegalArgumentException If {@code array} is null, or if a member
   *           value of the specified {@link List List&lt;?&gt;} is of a class
   *           that is not one of {@link Map Map&lt;String,?&gt;}, {@link List
   *           List&lt;?&gt;}, {@link String}, {@link Number}, {@link Boolean}
   *           and {@code null}, or if {@code indent} is negative.
   */
  public static String toString(final List<?> array, final int indent) {
    return toString(array, makeIndent(indent));
  }

  private static String toString(final List<?> array, final String spaces) {
    if (array == null)
      return "null";

    final StringBuilder builder = new StringBuilder();
    builder.append('[');
    boolean backUp = false;
    for (int i = 0, len = array.size(); i < len; ++i) {
      final Object member = array.get(i);
      final String str = JSON.encode(member, spaces);
      if (member instanceof Map || member instanceof List) {
        if (i > 0 && spaces != null)
          builder.append(' ');
        else
          backUp = true;
      }
      else if (spaces != null) {
        builder.append(spaces);
      }

      builder.append(backUp && spaces != null ? str.replaceAll(spaces, "\n") : str).append(',');
    }

    if (builder.length() > 1) {
      if (backUp || spaces == null)
        builder.setLength(builder.length() - 1);
      else
        builder.setCharAt(builder.length() - 1, '\n');
    }

    return builder.append(']').toString();
  }

  private JSON() {
  }
}