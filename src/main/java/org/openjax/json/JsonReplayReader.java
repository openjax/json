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

import java.io.IOException;
import java.io.Reader;

import org.libj.io.ReplayReader;
import org.libj.lang.Numbers;

/**
 * A {@link ReplayReader} that transparently unescapes string-literal unicode ({@code "\u000A"}) and two-character ({@code "\n"})
 * escape codes as defined in <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section 2.5</a>:
 * <p>
 * <blockquote><i>Any character may be escaped. If the character is in the Basic Multilingual Plane (U+0000 through U+FFFF), then it
 * may be represented as a six-character sequence: a reverse solidus, followed by the lowercase letter u, followed by four
 * hexadecimal digits that encode the character's code point.</i></blockquote>
 * <p>
 * This implementation unescapes all string-literal codes except for the double quote ({@code "\""}) and reverse solidus
 * ({@code "\\"}), as these characters are necessary to be able to differentiate the double quote from string boundaries, and thus
 * the reverse solidus from the escape character.
 */
class JsonReplayReader extends ReplayReader {
  private final char[] unicode = new char[4];
  private int pos = -1;
  private char readAhead = '\0';

  /**
   * Creates a new {@link JsonReplayReader} using the specified reader as its source, and the provided initial size for the
   * re-readable buffer.
   *
   * @param in A {@link Reader} providing the underlying stream.
   * @param initialSize An int specifying the initial buffer size of the re-readable buffer.
   * @throws IllegalArgumentException If {@code in} is null, or if {@code initialSize} is negative.
   */
  JsonReplayReader(final Reader in, final int initialSize) {
    super(in, initialSize);
  }

  /**
   * Creates a new {@link JsonReplayReader} using the specified reader as its source, and default initial size of 32 for the
   * re-readable buffer.
   *
   * @param in A {@link Reader} providing the underlying stream.
   * @throws IllegalArgumentException If {@code in} is null.
   */
  JsonReplayReader(final Reader in) {
    super(in);
  }

  /**
   * Reads a single character, and transparently unescapes string-literal unicode ({@code "\u000A"}) and two-character
   * ({@code "\n"}) escape codes into UTF-8 as defined in <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section 2.5</a>.
   * <p>
   * {@inheritDoc}
   *
   * @throws JsonParseException If an escaped sequence is not terminated.
   */
  @Override
  public int read() throws IOException {
    if (buffer.available() > 0)
      return buffer.read();

    if (closed)
      return -1;

    if (readAhead != '\0') {
      buffer.write(readAhead);
      final char tmp = readAhead;
      readAhead = '\0';
      return tmp;
    }

    int ch = in.read();
    if (ch == -1)
      return -1;

    final int code;
    ++pos;
    if (ch == '\\') {
      ch = in.read();
      if (ch == -1)
        return -1;

      ++pos;
      if (ch == '"' || ch == '\\') {
        readAhead = (char)ch;
        ch = '\\';
      }
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
        // FIXME: Optimize this code to remove char[]
        for (int i = 0; i < unicode.length; ++i) { // [A]
          final int c = in.read();
          if (c == -1)
            throw new JsonParseException("Unterminated escape sequence", pos);

          unicode[i] = (char)c;
        }

        code = Numbers.parseInt(unicode, 16, '\0');
        if (code == '\0')
          throw new NumberFormatException(String.valueOf(unicode));

        ch = (char)code;
      }
    }

    buffer.write(ch);
    return ch;
  }

  /**
   * Reads characters into an array, and transparently unescapes string-literal unicode ({@code "\u000A"}) and two-character
   * ({@code "\n"}) escape codes into UTF-8, as defined in <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section 2.5</a>.
   * <p>
   * {@inheritDoc}
   *
   * @throws JsonParseException If an escaped sequence is not terminated.
   */
  @Override
  public int read(final char[] cbuf) throws IOException {
    return read(cbuf, 0, cbuf.length);
  }

  /**
   * Reads characters into a portion of an array, and transparently unescapes string-literal unicode ({@code "\u000A"}) and
   * two-character ({@code "\n"}) escape codes into UTF-8, as defined in <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627,
   * Section 2.5</a>.
   * <p>
   * {@inheritDoc}
   *
   * @throws JsonParseException If an escaped sequence is not terminated.
   */
  @Override
  public int read(final char[] cbuf, final int off, final int len) throws IOException {
    int i = 0;
    for (int ch; i < len && (ch = read()) != -1; ++i) // [A]
      cbuf[off + i] = (char)ch;

    return i;
  }
}