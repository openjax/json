/* Copyright (c) 2018 OpenJAX
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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Utility functions for operations pertaining to JSON.
 */
public final class JsonUtil {
  /**
   * Tests whether {@code ch} is a structural char, which is one of:
   *
   * <pre>
   * <code>{ } [ ] : ,</code>
   * </pre>
   *
   * @param ch The char to test.
   * @return {@code true} if {@code ch} is a structural char, otherwise
   *         {@code false}.
   */
  public static boolean isStructural(final int ch) {
    return ch == '{' || ch == '}' || ch == '[' || ch == ']' || ch == ':' || ch == ',';
  }

  /**
   * Tests whether the specified {@code int} is JSON whitespace char, which is
   * one of:
   *
   * <pre>
   * {@code ' '}, {@code '\n'}, {@code '\r'}, or {@code '\t'}
   * </pre>
   *
   * @param ch The {@code int} to test.
   * @return {@code true} if the specified {@code int} is an ANSI whitespace
   *         char; otherwise {@code false}.
   * @see <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC4627</a>
   */
  public static boolean isWhitespace(final int ch) {
    return ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t';
  }

  /**
   * Parses a number (as an integer) from the specified string by the rules
   * defined in <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section
   * 2.4</a>.
   *
   * @param string The string to parse.
   * @return A {@link BigInteger} representing the parsed integer.
   * @throws JsonParseException If a parsing error has occurred.
   * @throws IllegalArgumentException If the specified string is empty.
   * @throws NullPointerException If the specified string is null.
   */
  public static BigInteger parseInteger(final String string) throws JsonParseException {
    return (BigInteger)parseNumber(string, false);
  }

  /**
   * Parses a number (as a decimal) from the specified string by the rules
   * defined in <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section
   * 2.4</a>.
   *
   * @param string The string to parse.
   * @return A {@link BigDecimal} representing the parsed decimal.
   * @throws JsonParseException If a parsing error has occurred.
   * @throws IllegalArgumentException If the specified string is empty.
   * @throws NullPointerException If the specified string is null.
   */
  public static BigDecimal parseDecimal(final String string) throws JsonParseException {
    return (BigDecimal)parseNumber(string, true);
  }

  static Number parseNumber(final String string, final boolean isDecimal) throws JsonParseException {
    if (string.length() == 0)
      throw new IllegalArgumentException("Empty string");

    int i = 0;
    int ch = string.charAt(i);
    if (ch != '-' && (ch < '0'|| '9' < ch))
      throw new JsonParseException("Illegal first character: '" + (char)ch + "'", i);

    int first = ch;
    int last = first;
    final int len = string.length();
    for (boolean hasDot = false; ++i < len; last = ch) {
      ch = string.charAt(i);
      if (isDecimal && ch == '.') {
        if (first == '-' && i == 1)
          throw new JsonParseException("Integer component required before fraction part", i);

        if (hasDot)
          throw new JsonParseException("Illegal character: '" + (char)ch + "'", i);

        hasDot = true;
      }
      else if (ch < '0' || '9' < ch) {
        break;
      }
      else if (last == '0' && i == (first == '-' ? 2 : 1)) {
        throw new JsonParseException("Leading zeros are not allowed", i - 1);
      }
    }

    if (isDecimal && last == '.')
      throw new JsonParseException("Decimal point must be followed by one or more digits", i);

    int expStart = -1;
    if (i < len) {
      if (ch != 'e' && ch != 'E')
        throw new JsonParseException("Illegal character: '" + (char)ch + "'", i);

      last = ch;
      for (expStart = i + 1; ++i < len; last = ch) {
        ch = string.charAt(i);
        if (ch == '-' || ch == '+') {
          first = '~';
          if (i > expStart)
            throw new JsonParseException("Illegal character: '" + (char)ch + "'", i);
        }
        else if (ch < '0' || '9' < ch) {
          break;
        }
        else if (last == '0' && i == expStart + (first == '~' ? 2 : 1)) {
          throw new JsonParseException("Leading zeros are not allowed", i - 1);
        }
      }

      if (last == 'e' || last == 'E')
        throw new JsonParseException("\"" + last + "\" must be followed by one or more digits", i);
    }

    if (ch < '0' || '9' < ch)
      throw new JsonParseException("Expected digit, but encountered '" + (char)ch + "'", i);

    return isDecimal ? new BigDecimal(string) : expStart == -1 ? new BigInteger(string) : new BigDecimal(string).toBigInteger();
  }

  /**
   * Escapes characters in the specified string that must be escaped as defined
   * in <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section
   * 2.5</a>. This includes quotation mark ({@code "\""}), reverse solidus
   * ({@code "\\"}), and the control characters ({@code U+0000} through
   * {@code U+001F}).
   * <p>
   * This method escapes the following characters in string-literal
   * two-character form:
   *
   * <pre>
   * {'\n', '\r', '\t', '\b', '\f'} -&gt; {"\\n", "\\r", "\\t", "\\b", "\\f"}
   * </pre>
   *
   * @param string The string to be escaped.
   * @return The escaped representation of the specified string.
   * @see #unescape(String)
   */
  public static StringBuilder escape(final String string) {
    final StringBuilder builder = new StringBuilder(string.length());
    for (int i = 0, len = string.length(); i < len; ++i) {
      final char ch = string.charAt(i);
      /*
       * From RFC 4627, "All Unicode characters may be placed within the
       * quotation marks except for the characters that must be escaped:
       * quotation mark, reverse solidus, and the control characters (U+0000
       * through U+001F)."
       */
      switch (ch) {
        case '"':
        case '\\':
          builder.append('\\').append(ch);
          break;
        case '\n':
          builder.append("\\n");
          break;
        case '\r':
          builder.append("\\r");
          break;
        case '\t':
          builder.append("\\t");
          break;
        case '\b':
          builder.append("\\b");
          break;
        case '\f':
          builder.append("\\f");
          break;
        default:
          if (ch <= 0x1F)
            builder.append(String.format("\\u%04x", (int)ch));
          else
            builder.append(ch);
      }
    }

    return builder;
  }

  /**
   * Unescapes string-literal unicode ({@code "\u000A"}) and two-character
   * ({@code "\n"}) escape codes, <i>except for the double quote ({@code "\""})
   * and reverse solidus ({@code "\\"})</i>, into UTF-8 as defined in
   * <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section 2.5</a>.
   * <p>
   * This method deliberately excludes the double quote ({@code "\""}) and
   * reverse solidus ({@code "\\"}), as these characters are necessary to be
   * able to differentiate the double quote from string boundaries, and thus the
   * reverse solidus from the escape character.
   *
   * @param string The string to be unescaped.
   * @return The unescaped representation of the specified string, with the
   *         escaped form of the double quote ({@code "\""}) and reverse solidus
   *         ({@code "\\"}) preserved.
   * @see #unescape(String)
   */
  public static String unescapeForString(final String string) {
    final StringBuilder builder = new StringBuilder(string.length());
    for (int i = 0, len = string.length(); i < len; ++i) {
      char ch = string.charAt(i);
      if (ch == '\\') {
        ch = string.charAt(++i);
        if (ch == '"' || ch == '\\')
          builder.append('\\');
        else if (ch == 'n')
          ch = '\n';
        else if (ch == 'r')
          ch = '\r';
        else if (ch == 't')
          ch = '\t';
        else if (ch == 'b')
          ch = '\b';
        else if (ch == 'f')
          ch = '\f';
        else if (ch == 'u') {
          ++i;
          final char[] unicode = new char[4];
          for (int j = 0; j < unicode.length; ++j)
            unicode[j] = string.charAt(i + j);

          i += unicode.length - 1;
          ch = (char)Integer.parseInt(new String(unicode), 16);
        }
      }

      builder.append(ch);
    }

    return builder.toString();
  }

  /**
   * Unescapes string-literal unicode ({@code "\u000A"}) and two-character
   * ({@code "\n"}) escape codes into UTF-8 as defined in
   * <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section 2.5</a>.
   *
   * @param string The string to be unescaped.
   * @return The unescaped representation of the specified string.
   */
  public static String unescape(final String string) {
    final StringBuilder builder = new StringBuilder(string.length());
    for (int i = 0, len = string.length(); i < len; ++i) {
      char ch = string.charAt(i);
      if (ch == '\\') {
        ch = string.charAt(++i);
        if (ch == 'n')
          ch = '\n';
        else if (ch == 'r')
          ch = '\r';
        else if (ch == 't')
          ch = '\t';
        else if (ch == 'b')
          ch = '\b';
        else if (ch == 'f')
          ch = '\f';
        else if (ch == 'u') {
          ++i;
          final char[] unicode = new char[4];
          for (int j = 0; j < unicode.length; ++j)
            unicode[j] = string.charAt(i + j);

          i += unicode.length - 1;
          ch = (char)Integer.parseInt(new String(unicode), 16);
        }
      }

      builder.append(ch);
    }

    return builder.toString();
  }

  private JsonUtil() {
  }
}