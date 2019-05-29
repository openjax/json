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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Lightweight {@code toString(...)} functions for marshaling {@code Map} (JSON
 * object) and {@code List} (JSON array) into JSON document representations.
 */
public final class JSON {
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
      return spaces == null ? list : list.replaceAll("\n", spaces);
    }

    if (obj instanceof Map) {
      final String map = toString((Map<String,?>)obj, spaces);
      return spaces == null ? map : map.replaceAll("\n", spaces);
    }

    throw new IllegalArgumentException("Illegal object of class: " + obj.getClass().getName());
  }

  /**
   * Returns a JSON document encoding of the specified {@code Map<String,?>}
   * representing a JSON object.
   * <p>
   * <i><b>NOTE</b>: The property values of the specified map may only be
   * instances of {@code Boolean}, {@code Number}, {@code String}, {@code List},
   * and {@code Map}. Objects of other classes will result in an
   * {@code IllegalArgumentException}.</i>
   *
   * @param object The JSON object, represented as a {@code Map<String,?>}.
   * @return A JSON document encoding of the specified {@code Map<String,?>}
   *         representing a JSON object.
   * @throws IllegalArgumentException If a property value of the specified map
   *           is of a class that is not one of {@code Boolean}, {@code Number},
   *           {@code String}, {@code List}, or {@code Map}.
   * @throws NullPointerException If {@code properties} is null.
   */
  public static String toString(final Map<String,?> object) {
    return toString(object, 0);
  }

  /**
   * Returns a JSON document encoding of the specified {@code Map<String,?>}
   * representing a JSON object.
   * <p>
   * <i><b>NOTE</b>: The property values of the specified map may only be
   * instances of {@code Boolean}, {@code Number}, {@code String}, {@code List},
   * and {@code Map}. Objects of other classes will result in an
   * {@code IllegalArgumentException}.</i>
   *
   * @param object The JSON object, represented as a {@code Map<String,?>}.
   * @param indent Number of spaces to indent child elements. If the specified
   *          indent value is greater than {@code 0}, child elements are
   *          indented and placed on a new line. If the indent value is
   *          {@code 0}, child elements are not indented, nor placed on a new
   *          line.
   * @return A JSON document encoding of the specified {@code Map<String,?>}
   *         representing a JSON object.
   * @throws IllegalArgumentException If a property value of the specified map
   *           is of a class that is not one of {@code Boolean}, {@code Number},
   *           {@code String}, {@code List}, or {@code Map}; or if
   *           {@code indent} is negative.
   * @throws NullPointerException If {@code properties} is null.
   */
  public static String toString(final Map<String,?> object, final int indent) {
    return toString(object, makeIndent(indent));
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
   * <p>
   * <i><b>NOTE</b>: The member values of the specified list may only be
   * instances of {@code Boolean}, {@code Number}, {@code String}, {@code List},
   * and {@code Map}. Objects of other classes will result in an
   * {@code IllegalArgumentException}.</i>
   *
   * @param array The JSON array, represented as a {@code List<?>}.
   * @return A JSON document encoding of the specified {@code List<?>}
   *         representing a JSON array, or {@code null} if {@code array} is
   *         null.
   * @throws IllegalArgumentException If a member of the specified list is of a
   *           class that is not one of {@code Boolean}, {@code Number},
   *           {@code String}, {@code List}, or {@code Map}.
   */
  public static String toString(final List<?> array) {
    return toString(array, 0);
  }

  /**
   * Returns a JSON document encoding (with the specified indentation) of the
   * specified {@code List<?>} representing a JSON array, or {@code null} if
   * {@code array} is null.
   * <p>
   * <i><b>NOTE</b>: The member values of the specified list may only be
   * instances of {@code Boolean}, {@code Number}, {@code String}, {@code List},
   * and {@code Map}. Objects of other classes will result in an
   * {@code IllegalArgumentException}.</i>
   *
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
   *           class that is not one of {@code Boolean}, {@code Number},
   *           {@code String}, {@code List}, or {@code Map}; or if
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
    for (int i = 0; i < array.size(); ++i) {
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
      if (backUp)
        builder.setLength(builder.length() - 1);
      else
        builder.setCharAt(builder.length() - 1, '\n');
    }

    return builder.append(']').toString();
  }

  private JSON() {
  }
}