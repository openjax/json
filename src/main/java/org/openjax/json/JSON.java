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

import java.util.List;
import java.util.Map;

/**
 * Lightweight {@code toString(...)} functions for marshaling {@code Map} (JSON
 * object) and {@code List} (JSON array) into JSON document representations.
 */
public final class JSON {
  @SuppressWarnings("unchecked")
  private static String encode(final Object obj) {
    if (obj == null)
      return "null";

    if (obj instanceof String)
      return JsonStrings.escape((String)obj).insert(0, '"').append('"').toString();

    if (obj instanceof Boolean || obj instanceof Number)
      return obj.toString();

    if (obj instanceof List)
      return toString((List<?>)obj).replaceAll("\n", "\n  ");

    if (obj instanceof Map)
      return toString((Map<String,?>)obj).replaceAll("\n", "\n  ");

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
    final StringBuilder builder = new StringBuilder("{");
    if (object != null)
      for (final Map.Entry<String,?> entry : object.entrySet())
        builder.append("\n  \"").append(JsonStrings.escape(entry.getKey())).append("\": ").append(encode(entry.getValue())).append(',');

    if (builder.length() > 1)
      builder.setCharAt(builder.length() - 1, '\n');

    return builder.append('}').toString();
  }

  /**
   * Returns a JSON document encoding of the specified {@code List<?>}
   * representing a JSON array, or {@code null} if {@code array} is null.
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
    if (array == null)
      return "null";

    final StringBuilder builder = new StringBuilder("[");
    boolean backUp = false;
    for (int i = 0; i < array.size(); ++i) {
      final Object member = array.get(i);
      final String s = JSON.encode(member);
      if (member instanceof Map || member instanceof List) {
        if (i > 0)
          builder.append(' ');
        else
          backUp = true;
      }
      else {
        builder.append("\n  ");
      }

      builder.append(backUp ? s.replaceAll("\n  ", "\n") : s).append(',');
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