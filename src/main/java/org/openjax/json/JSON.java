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
 * Lightweight {@code toString(...)} functions for marshaling {@link Map} (JSON
 * object) and {@link List} (JSON array) into JSON document representations.
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

  private static final JsonParser jsonParser = new JsonParser();

  public static Object parse(final String json) {
    try (final StringReader reader = new StringReader(Assertions.assertNotNull(json))) {
      return parse(reader);
    }
  }

  public static Object parse(final Reader reader) {
    final List<Object> stack = new ArrayList<>();
    try (final JsonReader in = new JsonReader(reader)) {
      jsonParser.parse(in, new JsonHandler() {
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
      });

      return stack.get(0);
    }
    catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String stripWhitespace(final String json) {
    try (final StringReader reader = new StringReader(Assertions.assertNotNull(json))) {
      return stripWhitespace(reader);
    }
  }

  public static String stripWhitespace(final Reader reader) {
    final StringBuilder builder = new StringBuilder();
    try (final JsonReader in = new JsonReader(reader)) {
      jsonParser.parse(in, new JsonHandler() {
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
      });

      return builder.toString();
    }
    catch (final IOException e) {
      throw new IllegalStateException(e);
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
   * Returns a JSON document encoding of a {@code Map<String,?>} representing a
   * JSON object, or a {@code List<?>} representing a JSON array, or
   * {@code null} if {@code json} is null.
   *
   * @implNote The property values of the specified map may only be instances of
   *           {@link Boolean}, {@link Number}, {@link String}, {@link List},
   *           and {@link Map}. Objects of other classes will result in an
   *           {@link IllegalArgumentException}.
   * @param json The JSON object represented as a {@code Map<String,?>}, or an
   *          array, represented as a {@code List<?>}.
   * @return A JSON document encoding of the specified {@code Map<String,?>}
   *         representing a JSON object or {@code List<?>} representing an
   *         array.
   * @throws IllegalArgumentException If a property value of the specified
   *           {@code Map<String,?>} or member of the {@code List<?>} is of a
   *           class that is not one of {@link Boolean}, {@link Number},
   *           {@link String}, {@link List}, or {@link Map}.
   */
  @SuppressWarnings("unchecked")
  public static String toString(final Object json) {
    if (json instanceof Map)
      return toString((Map<String,?>)json);

    if (json instanceof List)
      return toString((List<?>)json);

    throw new IllegalArgumentException("Unknown object type: " + json.getClass().getName());
  }

  /**
   * Returns a JSON document encoding of the specified {@code Map<String,?>}
   * representing a JSON object, or {@code null} if {@code object} is null.
   *
   * @implNote The property values of the specified map may only be instances of
   *           {@link Boolean}, {@link Number}, {@link String}, {@link List},
   *           and {@link Map}. Objects of other classes will result in an
   *           {@link IllegalArgumentException}.
   * @param object The JSON object, represented as a {@code Map<String,?>}.
   * @return A JSON document encoding of the specified {@code Map<String,?>}
   *         representing a JSON object.
   * @throws IllegalArgumentException If a property value of the specified map
   *           is of a class that is not one of {@link Boolean}, {@link Number},
   *           {@link String}, {@link List}, or {@link Map}.
   */
  public static String toString(final Map<String,?> object) {
    return object == null ? null : toString(object, 0);
  }

  /**
   * Returns a JSON document encoding of the specified {@code Map<String,?>}
   * representing a JSON object, or {@code null} if {@code object} is null.
   *
   * @implNote The property values of the specified map may only be instances of
   *           {@link Boolean}, {@link Number}, {@link String}, {@link List},
   *           and {@link Map}. Objects of other classes will result in an
   *           {@link IllegalArgumentException}.
   * @param object The JSON object, represented as a {@code Map<String,?>}.
   * @param indent Number of spaces to indent child elements. If the specified
   *          indent value is greater than {@code 0}, child elements are
   *          indented and placed on a new line. If the indent value is
   *          {@code 0}, child elements are not indented, nor placed on a new
   *          line.
   * @return A JSON document encoding of the specified {@code Map<String,?>}
   *         representing a JSON object.
   * @throws IllegalArgumentException If a property value of the specified map
   *           is of a class that is not one of {@link Boolean}, {@link Number},
   *           {@link String}, {@link List}, or {@link Map}; or if
   *           {@code indent} is negative.
   */
  public static String toString(final Map<String,?> object, final int indent) {
    return object == null ? null : toString(object, makeIndent(indent));
  }

  private static String toString(final Map<String,?> object, final String spaces) {
    final StringBuilder builder = new StringBuilder("{");
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
   * Returns a JSON document encoding (with no indentation) of the specified
   * {@code List<?>} representing a JSON array, or {@code null} if {@code array}
   * is null.
   *
   * @implNote The member values of the specified list may only be instances of
   *           {@link Boolean}, {@link Number}, {@link String}, {@link List},
   *           and {@link Map}. Objects of other classes will result in an
   *           {@link IllegalArgumentException}.
   * @param array The JSON array, represented as a {@code List<?>}.
   * @return A JSON document encoding of the specified {@code List<?>}
   *         representing a JSON array, or {@code null} if {@code array} is
   *         null.
   * @throws IllegalArgumentException If a member of the specified list is of a
   *           class that is not one of {@link Boolean}, {@link Number},
   *           {@link String}, {@link List}, or {@link Map}.
   */
  public static String toString(final List<?> array) {
    return toString(array, 0);
  }

  /**
   * Returns a JSON document encoding (with the specified indentation) of the
   * specified {@code List<?>} representing a JSON array, or {@code null} if
   * {@code array} is null.
   *
   * @implNote The member values of the specified list may only be instances of
   *           {@link Boolean}, {@link Number}, {@link String}, {@link List},
   *           and {@link Map}. Objects of other classes will result in an
   *           {@link IllegalArgumentException}.
   * @param array The JSON array, represented as a {@code List<?>}.
   * @param indent Number of spaces to indent child elements. If the specified
   *          indent value is greater than {@code 0}, child elements are
   *          indented and placed on a new line. If the indent value is
   *          {@code 0}, child elements are not indented, nor placed on a new
   *          line.
   * @return A JSON document encoding of the specified {@code List<?>}
   *         representing a JSON array, or {@code null} if {@code array} is
   *         null.
   * @throws IllegalArgumentException If a member of the specified list is of a
   *           class that is not one of {@link Boolean}, {@link Number},
   *           {@link String}, {@link List}, or {@link Map}; or if
   *           {@code indent} is negative.
   */
  public static String toString(final List<?> array, final int indent) {
    return toString(array, makeIndent(indent));
  }

  private static String toString(final List<?> array, final String spaces) {
    if (array == null)
      return "null";

    final StringBuilder builder = new StringBuilder("[");
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