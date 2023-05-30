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

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.libj.lang.Classes;
import org.libj.lang.Numbers;

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
   * @return {@code true} if {@code ch} is a structural char, otherwise {@code false}.
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
   * @return {@code true} if the specified {@code int} is an ANSI whitespace char; otherwise {@code false}.
   * @see <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC4627</a>
   */
  public static boolean isWhitespace(final int ch) {
    return ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t';
  }

  /**
   * Parses a number from the specified string by the rules defined in <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627,
   * Section 2.4</a>.
   *
   * @param <T> The type parameter for the return instance.
   * @param type The class of the return instance.
   * @param str The {@link CharSequence} to parse.
   * @param strict Whether to enforce strict compliance to the <a href="https://www.ietf.org/rfc/rfc4627.txt">JSON Specification</a>
   *          while parsing the JSON document.
   * @return An instance of class {@code type} representing the parsed string.
   * @throws JsonParseException If a parsing error has occurred.
   * @throws IllegalArgumentException If the specified string is empty, or if an instance of the specific class type does not define
   *           {@code <init>(String)}, {@code valueOf(String)}, or {@code fromString(String)}.
   * @throws NullPointerException If {@code str} is null.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Number>T parseNumber(final Class<? extends T> type, CharSequence str, final boolean strict) throws JsonParseException {
    if (str.length() == 0)
      throw new IllegalArgumentException("Empty string");

    int i = 0;
    int ch = str.charAt(i);
    if (ch != '-' && (ch < '0' || '9' < ch))
      throw new JsonParseException("Illegal first character: '" + (char)ch + "'", i);

    int first = ch;
    int last = first;
    final int len = str.length();
    for (boolean hasDot = false; ++i < len; last = ch) { // [N]
      ch = str.charAt(i);
      if (ch == '.') {
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

    if (last == '.')
      throw new JsonParseException("Decimal point must be followed by one or more digits", i);

    int expStart = -1;
    if (i < len) {
      if (ch != 'e' && ch != 'E')
        throw new JsonParseException("Illegal character: '" + (char)ch + "'", i);

      last = ch;
      for (expStart = i + 1; ++i < len; last = ch) { // [N]
        ch = str.charAt(i);
        if (ch == '-' || ch == '+') {
          first = '~';
          if (i > expStart)
            throw new JsonParseException("Illegal character: '" + (char)ch + "'", i);
        }
        else if (ch < '0' || '9' < ch) {
          break;
        }
        else if (last == '0' && i == expStart + (first == '~' ? 2 : 1)) {
          if (strict)
            throw new JsonParseException("Leading zeros are not allowed", i - 1);
        }
      }

      if (last == 'e' || last == 'E')
        throw new JsonParseException("\"" + last + "\" must be followed by one or more digits", i);
    }

    if (ch < '0' || '9' < ch)
      throw new JsonParseException("Expected digit, but encountered '" + (char)ch + "'", i);

    // FIXME: Fail fast?! (i.e., earlier?)
    if (type == null)
      return null;

    // If we have exponential form, and the return type is not BigDecimal, then convert
    // to non-exponential form (unless we can immediately return a BigInteger)
    if (expStart > -1 && !BigDecimal.class.isAssignableFrom(type)) {
      if (type == BigInteger.class)
        return (T)new BigDecimal(str.toString()).toBigInteger();

      str = new BigDecimal(str.toString()).toPlainString();
    }

    if (type == BigDecimal.class)
      return (T)new BigDecimal(str.toString());

    if (type == BigInteger.class)
      return (T)new BigInteger(str.toString());

    if (type == Long.class || type == long.class)
      return (T)Long.valueOf(str.toString());

    if (type == Integer.class || type == int.class)
      return (T)Integer.valueOf(str.toString());

    if (type == Short.class || type == short.class)
      return (T)Short.valueOf(str.toString());

    if (type == Byte.class || type == byte.class)
      return (T)Byte.valueOf(str.toString());

    if (type == Double.class || type == double.class)
      return (T)Double.valueOf(str.toString());

    if (type == Float.class || type == float.class)
      return (T)Float.valueOf(str.toString());

    try {
      return Classes.newInstance(type, str.toString());
    }
    catch (final IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new UnsupportedOperationException("Unsupported type: " + type.getName(), e);
    }
  }

  /**
   * Escapes characters in the specified string that must be escaped as defined in
   * <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section 2.5</a>. This includes quotation mark ({@code "\""}), reverse
   * solidus ({@code "\\"}), and the control characters ({@code U+0000} through {@code U+001F}).
   * <p>
   * This method escapes the following characters in string-literal two-character form:
   *
   * <pre>
   * {'\n', '\r', '\t', '\b', '\f'} -&gt; {"\\n", "\\r", "\\t", "\\b", "\\f"}
   * </pre>
   *
   * @param str The string to be escaped.
   * @return The escaped representation of the specified string.
   * @throws NullPointerException If {@code str} is null.
   * @see #unescape(CharSequence)
   */
  public static StringBuilder escape(final CharSequence str) {
    return escape(new StringBuilder(str.length()), str);
  }

  /**
   * Escapes characters in the specified string that must be escaped as defined in
   * <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section 2.5</a>. This includes quotation mark ({@code "\""}), reverse
   * solidus ({@code "\\"}), and the control characters ({@code U+0000} through {@code U+001F}).
   * <p>
   * This method escapes the following characters in string-literal two-character form:
   *
   * <pre>
   * {'\n', '\r', '\t', '\b', '\f'} -&gt; {"\\n", "\\r", "\\t", "\\b", "\\f"}
   * </pre>
   *
   * @param out The {@link StringBuilder} to which the escaped contents of {@code str} are to be appended.
   * @param str The string to be escaped.
   * @return The provided {@link StringBuilder} with the escaped representation of {@code str}.
   * @throws NullPointerException If {@code out} or {@code str} is null.
   * @see #unescape(CharSequence)
   */
  public static StringBuilder escape(final StringBuilder out, final CharSequence str) {
    for (int i = 0, i$ = str.length(); i < i$; ++i) { // [N]
      final char ch = str.charAt(i);
      /*
       * From RFC 4627, "All Unicode characters may be placed within the quotation marks except for the characters that must be
       * escaped: quotation mark, reverse solidus, and the control characters (U+0000 through U+001F)."
       */
      switch (ch) {
        case '"':
        case '\\':
          out.append('\\').append(ch);
          break;
        case '\n':
          out.append("\\n");
          break;
        case '\r':
          out.append("\\r");
          break;
        case '\t':
          out.append("\\t");
          break;
        case '\b':
          out.append("\\b");
          break;
        case '\f':
          out.append("\\f");
          break;
        default:
          if (ch <= 0x1F)
            out.append(String.format("\\u%04x", (int)ch));
          else
            out.append(ch);
      }
    }

    return out;
  }

  /**
   * Escapes characters in the specified {@code char[]} that must be escaped as defined in
   * <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section 2.5</a>. This includes quotation mark ({@code "\""}), reverse
   * solidus ({@code "\\"}), and the control characters ({@code U+0000} through {@code U+001F}).
   * <p>
   * This method escapes the following characters in string-literal two-character form:
   *
   * <pre>
   * {'\n', '\r', '\t', '\b', '\f'} -&gt; {"\\n", "\\r", "\\t", "\\b", "\\f"}
   * </pre>
   *
   * @param chars The {@code char[]} to be escaped.
   * @param offset The initial offset.
   * @param len The length.
   * @return The escaped representation of the specified {@code char[]}.
   * @throws NullPointerException If {@code chars} is null.
   * @throws ArrayIndexOutOfBoundsException If {@code offset} is negative, or {@code offset + len >= chars.length}.
   * @see #unescape(char[],int,int)
   */
  public static StringBuilder escape(final char[] chars, final int offset, final int len) {
    return escape(new StringBuilder(chars.length), chars, offset, len);
  }

  /**
   * Escapes characters in the specified {@code char[]} that must be escaped as defined in
   * <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section 2.5</a>. This includes quotation mark ({@code "\""}), reverse
   * solidus ({@code "\\"}), and the control characters ({@code U+0000} through {@code U+001F}).
   * <p>
   * This method escapes the following characters in string-literal two-character form:
   *
   * <pre>
   * {'\n', '\r', '\t', '\b', '\f'} -&gt; {"\\n", "\\r", "\\t", "\\b", "\\f"}
   * </pre>
   *
   * @param out The {@link StringBuilder} to which the escaped contents of {@code chars} are to be appended.
   * @param chars The {@code char[]} to be escaped.
   * @param offset The initial offset.
   * @param len The length.
   * @return The provided {@link StringBuilder} with the escaped representation of the specified {@code char[]}.
   * @throws NullPointerException If {@code out} or {@code chars} is null.
   * @throws ArrayIndexOutOfBoundsException If {@code offset} is negative, or {@code offset + len >= chars.length}.
   * @see #unescape(char[],int,int)
   */
  public static StringBuilder escape(final StringBuilder out, final char[] chars, final int offset, final int len) {
    for (int i = offset, length = offset + len; i < length; ++i) { // [N]
      final char ch = chars[i];
      /*
       * From RFC 4627, "All Unicode characters may be placed within the quotation marks except for the characters that must be
       * escaped: quotation mark, reverse solidus, and the control characters (U+0000 through U+001F)."
       */
      switch (ch) {
        case '"':
        case '\\':
          out.append('\\').append(ch);
          break;
        case '\n':
          out.append("\\n");
          break;
        case '\r':
          out.append("\\r");
          break;
        case '\t':
          out.append("\\t");
          break;
        case '\b':
          out.append("\\b");
          break;
        case '\f':
          out.append("\\f");
          break;
        default:
          if (ch <= 0x1F)
            out.append(String.format("\\u%04x", (int)ch));
          else
            out.append(ch);
      }
    }

    return out;
  }

  /**
   * Unescapes string-literal unicode ({@code "\u000A"}) and two-character ({@code "\n"}) escape codes, <i>except for the double
   * quote ({@code "\""}) and reverse solidus ({@code "\\"})</i>, into UTF-8 as defined in
   * <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section 2.5</a>.
   * <p>
   * This method deliberately excludes the double quote ({@code "\""}) and reverse solidus ({@code "\\"}), as these characters are
   * necessary to be able to differentiate the double quote from string boundaries, and thus the reverse solidus from the escape
   * character.
   *
   * @param str The string to be unescaped.
   * @return The unescaped representation of the specified string, with the escaped form of the double quote ({@code "\""}) and
   *         reverse solidus ({@code "\\"}) preserved.
   * @throws NullPointerException If {@code str} is null.
   * @see #escape(CharSequence)
   * @see #unescape(CharSequence)
   */
  public static StringBuilder unescapeForString(final CharSequence str) {
    return unescapeForString(new StringBuilder(str.length()), str);
  }

  /**
   * Unescapes string-literal unicode ({@code "\u000A"}) and two-character ({@code "\n"}) escape codes, <i>except for the double
   * quote ({@code "\""}) and reverse solidus ({@code "\\"})</i>, into UTF-8 as defined in
   * <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section 2.5</a>.
   * <p>
   * This method deliberately excludes the double quote ({@code "\""}) and reverse solidus ({@code "\\"}), as these characters are
   * necessary to be able to differentiate the double quote from string boundaries, and thus the reverse solidus from the escape
   * character.
   *
   * @param out The {@link StringBuilder} to which the unescaped contents of {@code str} are to be appended.
   * @param str The string to be unescaped.
   * @return The provided {@link StringBuilder} with the unescaped representation of of {@code str}, with the escaped form of the
   *         double quote ({@code "\""}) and reverse solidus ({@code "\\"}) preserved.
   * @throws NullPointerException If {@code out} or {@code str} is null.
   * @see #escape(CharSequence)
   * @see #unescape(CharSequence)
   */
  public static StringBuilder unescapeForString(final StringBuilder out, final CharSequence str) {
    for (int i = 0, i$ = str.length(); i < i$; ++i) { // [N]
      char ch = str.charAt(i);
      if (ch == '\\') {
        ch = str.charAt(++i);
        if (ch == '"' || ch == '\\')
          out.append('\\');
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
          for (int j = 0, j$ = unicode.length; j < j$; ++j) // [A]
            unicode[j] = str.charAt(i + j);

          i += unicode.length - 1;
          ch = (char)Integer.parseInt(new String(unicode), 16);
        }
      }

      out.append(ch);
    }

    return out;
  }

  /**
   * Unescapes string-literal unicode ({@code "\u000A"}) and two-character ({@code "\n"}) escape codes, <i>except for the double
   * quote ({@code "\""}) and reverse solidus ({@code "\\"})</i>, into UTF-8 as defined in
   * <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section 2.5</a>.
   * <p>
   * This method deliberately excludes the double quote ({@code "\""}) and reverse solidus ({@code "\\"}), as these characters are
   * necessary to be able to differentiate the double quote from string boundaries, and thus the reverse solidus from the escape
   * character.
   *
   * @param chars The {@code char[]} to be unescaped.
   * @param offset The initial offset.
   * @param len The length.
   * @return The unescaped representation of the specified string, with the escaped form of the double quote ({@code "\""}) and
   *         reverse solidus ({@code "\\"}) preserved.
   * @throws NullPointerException If {@code chars} is null.
   * @throws ArrayIndexOutOfBoundsException If {@code offset} is negative, or {@code offset + len >= chars.length}.
   * @see #escape(char[],int,int)
   * @see #unescape(char[],int,int)
   */
  public static StringBuilder unescapeForString(final char[] chars, final int offset, final int len) {
    return unescapeForString(new StringBuilder(chars.length), chars, offset, len);
  }

  /**
   * Unescapes string-literal unicode ({@code "\u000A"}) and two-character ({@code "\n"}) escape codes, <i>except for the double
   * quote ({@code "\""}) and reverse solidus ({@code "\\"})</i>, into UTF-8 as defined in
   * <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section 2.5</a>.
   * <p>
   * This method deliberately excludes the double quote ({@code "\""}) and reverse solidus ({@code "\\"}), as these characters are
   * necessary to be able to differentiate the double quote from string boundaries, and thus the reverse solidus from the escape
   * character.
   *
   * @param out The {@link StringBuilder} to which the unescaped contents of {@code chars} are to be appended.
   * @param chars The {@code char[]} to be unescaped.
   * @param offset The initial offset.
   * @param len The length.
   * @return The provided {@link StringBuilder} with the unescaped representation of {@code chars}, with the escaped form of the
   *         double quote ({@code "\""}) and reverse solidus ({@code "\\"}) preserved.
   * @throws NullPointerException If {@code out} or {@code chars} is null.
   * @throws ArrayIndexOutOfBoundsException If {@code offset} is negative, or {@code offset + len >= chars.length}.
   * @see #escape(char[],int,int)
   * @see #unescape(char[],int,int)
   */
  public static StringBuilder unescapeForString(final StringBuilder out, final char[] chars, final int offset, final int len) {
    for (int i = offset, length = offset + len; i < length; ++i) { // [A]
      char ch = chars[i];
      if (ch == '\\') {
        ch = chars[++i];
        if (ch == '"' || ch == '\\')
          out.append('\\');
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
          ch = (char)Numbers.parseInt(chars, ++i, 4, 16, '\0');
          i += 3;
        }
      }

      out.append(ch);
    }

    return out;
  }

  /**
   * Unescapes string-literal unicode ({@code "\u000A"}) and two-character ({@code "\n"}) escape codes into UTF-8 as defined in
   * <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section 2.5</a>.
   *
   * @param str The string to be unescaped.
   * @return The unescaped representation of the specified string.
   * @throws NullPointerException If {@code str} is null.
   * @see #escape(CharSequence)
   */
  public static StringBuilder unescape(final CharSequence str) {
    return unescape(new StringBuilder(str.length()), str);
  }

  /**
   * Unescapes string-literal unicode ({@code "\u000A"}) and two-character ({@code "\n"}) escape codes into UTF-8 as defined in
   * <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section 2.5</a>.
   *
   * @param out The {@link StringBuilder} to which the unescaped contents of {@code str} are to be appended.
   * @param str The string to be unescaped.
   * @return The provided {@link StringBuilder} with the unescaped representation of {@code str}.
   * @throws NullPointerException If {@code out} or {@code str} is null.
   * @see #escape(CharSequence)
   */
  public static StringBuilder unescape(final StringBuilder out, final CharSequence str) {
    for (int i = 0, i$ = str.length(); i < i$; ++i) { // [N]
      char ch = str.charAt(i);
      if (ch == '\\') {
        ch = str.charAt(++i);
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
          for (int j = 0, j$ = unicode.length; j < j$; ++j) // [A]
            unicode[j] = str.charAt(i + j);

          i += unicode.length - 1;
          ch = (char)Integer.parseInt(new String(unicode), 16);
        }
      }

      out.append(ch);
    }

    return out;
  }

  /**
   * Unescapes string-literal unicode ({@code "\u000A"}) and two-character ({@code "\n"}) escape codes into UTF-8 as defined in
   * <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section 2.5</a>.
   *
   * @param chars The {@code char[]} to be unescaped.
   * @param offset The initial offset.
   * @param len The length.
   * @return The unescaped representation of the specified string.
   * @throws NullPointerException If {@code chars} is null.
   * @throws ArrayIndexOutOfBoundsException If {@code offset} is negative, or {@code offset + len >= chars.length}.
   * @see #escape(char[],int,int)
   */
  public static StringBuilder unescape(final char[] chars, final int offset, final int len) {
    return unescape(new StringBuilder(chars.length), chars, offset, len);
  }

  /**
   * Unescapes string-literal unicode ({@code "\u000A"}) and two-character ({@code "\n"}) escape codes into UTF-8 as defined in
   * <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section 2.5</a>.
   *
   * @param out The {@link StringBuilder} to which the unescaped contents of {@code chars} are to be appended.
   * @param chars The {@code char[]} to be unescaped.
   * @param offset The initial offset.
   * @param len The length.
   * @return The provided {@link StringBuilder} with the unescaped representation of {@code chars}.
   * @throws NullPointerException If {@code out} or {@code chars} is null.
   * @throws ArrayIndexOutOfBoundsException If {@code offset} is negative, or {@code offset + len >= chars.length}.
   * @see #escape(char[],int,int)
   */
  public static StringBuilder unescape(final StringBuilder out, final char[] chars, final int offset, final int len) {
    for (int i = offset, length = offset + len; i < length; ++i) { // [A]
      char ch = chars[i];
      if (ch == '\\') {
        ch = chars[++i];
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
          for (int j = 0, j$ = unicode.length; j < j$; ++j) // [A]
            unicode[j] = chars[i + j];

          i += unicode.length - 1;
          ch = (char)Integer.parseInt(new String(unicode), 16);
        }
      }

      out.append(ch);
    }

    return out;
  }

  private JsonUtil() {
  }
}