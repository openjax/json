/* Copyright (c) 2018 lib4j
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

package org.lib4j.json;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.lib4j.io.ReplayReader;
import org.lib4j.util.ArrayIntList;
import org.lib4j.util.ArrayLongList;
import org.lib4j.util.Buffers;
import org.lib4j.util.Characters;
import org.lib4j.util.Numbers;

/**
 * Validating {@link Reader} and {@link Iterator} implementation for JSON
 * streams that reads JSON tokens sequentially, while asserting the content
 * conforms to the <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627</a>
 * specification.
 * <p>
 * This implementation provides the following features:
 * <ul>
 * <li>Regular read of JSON tokens, sequentially returning each token:
 * {@link #readToken()}. (Please see {@link #readToken()} for a definition of:
 * {@code token}.</li>
 * <li>Optimized read of JSON tokens, sequentially returning only the starting
 * position of each token: {@link #readTokenStart()}. (This position can be
 * dereferenced via: {@link JsonReader#buf()}).</li>
 * <li>Implements the {@link Iterable} interface, to sequentially iterate over
 * each token: {@link #iterator()}.</li>
 * <li>Indexes each token: {@link #getIndex()}.</li>
 * <li>Allows to read back previously read tokens: {@link #setIndex()}.</li>
 * <li>Support partial reads of tokens with {@link #read()},
 * {@link #read(char[])}. and {@link #read(char[], int, int)} methods.
 * </ul>
 */
public class JsonReader extends ReplayReader implements Iterable<String>, Iterator<String> {
  private static final char[][] literals = {{'n', 'u', 'l', 'l'}, {'t', 'r', 'u', 'e'}, {'f', 'a', 'l', 's', 'e'}};

  private static final int DEFAULT_BUFFER_SIZE = 2048;          // Number of characters in the JSON document
  private static final int DEFAULT_TOKENS_SIZE = 128;           // Number of tokens in the JSON document
  private static final int DEFAULT_SCOPE_SIZE = 2;              // Number of [] or {} scopes in the JSON document
  private static final double DEFAULT_SCOPE_RESIZE_FACTOR = 2;  // Resize factor for scope buffer

  /**
   * Returns {@code true} if {@code ch} is a structural char, which is one of:
   *
   * <pre><code>{ } [ ] : ,</code></pre>
   *
   * @param ch The char.
   * @return {@code true} if {@code ch} is a structural char.
   */
  protected static boolean isStructural(final int ch) {
    return ch == '{' || ch == '}' || ch == '[' || ch == ']' || ch == ':' || ch == ',';
  }

  private final ArrayLongList positions = new ArrayLongList(DEFAULT_TOKENS_SIZE);
  private final ArrayList<long[]> scopes = new ArrayList<>(DEFAULT_SCOPE_SIZE);
  private final ArrayIntList depths = new ArrayIntList(DEFAULT_SCOPE_SIZE);
  private int index = -1;

  private long[] scope = new long[DEFAULT_SCOPE_SIZE];
  private int depth = -1;
  private int nextStart = 0;

  private final boolean ignoreWhitespace;

  /**
   * Construct a new {@code JsonReader} for JSON content to be read from the
   * {@code reader} parameter instance, <b>that ignores inter-token
   * whitespace</b>. This constructor is equivalent to calling
   * {@code new JsonReader(reader, true)}.
   *
   * @param reader The {@code Reader} from which JSON is read.
   */
  public JsonReader(final Reader reader) {
    this(reader, true);
  }

  /**
   * Construct a new {@code JsonReader} for JSON content to be read from the
   * {@code reader} parameter instance.
   *
   * @param reader The {@code Reader} from which JSON is read.
   * @param ignoreWhitespace If {@code ignoreWhitespace == false}, inter-token
   *          whitespace will <b>not</b> be skipped.
   */
  public JsonReader(final Reader reader, final boolean ignoreWhitespace) {
    super(reader, DEFAULT_BUFFER_SIZE);
    this.ignoreWhitespace = ignoreWhitespace;
  }

  /**
   * Returns the index of the most recently read token.
   *
   * @return The index of the most recently read token.
   */
  public int getIndex() {
    return index;
  }

  /**
   * Sets the index for the next token to be read.
   *
   * @param index The index for the next token to be read.
   * @throws IllegalArgumentException If {@code index < 0 || size() <= index}.
   */
  public void setIndex(final int index) {
    if (this.index == index)
      return;

    if (index < 0 || size() <= index)
      throw new IllegalArgumentException("Index out of range [0," + (size() - 1) + "]: " + index);

    setIndex0(index);
  }

  /**
   * Supporting method to set the index for the next token to be read.
   *
   * @param index The index for the next token to be read.
   * @return The start position of the token at {@code index}.
   * @throws IndexOutOfBoundsException If {@code index < 0 || size() <= index}.
   */
  private int setIndex0(final int index) {
    this.index = index;
    setPosition(getEndPosition(index));
    scope = scopes.get(index);
    depth = depths.get(index);
    return getStartPosition(index);
  }

  /**
   * Returns the number of tokens read thus far.
   *
   * @return The number of tokens read thus far.
   */
  public int size() {
    return positions.size();
  }

  /**
   * Returns the start position at {@code index}.
   *
   * @param index The token index.
   * @return The start position at {@code index}.
   */
  protected int getStartPosition(final int index) {
    return Numbers.Compound.dencodeInt(positions.get(index), 0);
  }

  /**
   * Returns the end position at {@code index}.
   *
   * @param index The token index.
   * @return The end position at {@code index}.
   */
  protected int getEndPosition(final int index) {
    return Numbers.Compound.dencodeInt(positions.get(index), 1);
  }

  /**
   * Set the buffer to position {@code p}, such that a subsequent call to
   * {@code read()} will return the char at {@code p + 1}.
   *
   * @param p The position.
   * @throws IllegalArgumentException If {@code p} is negative, or if {@code p}
   *           exceeds the length of the underlying buffer.
   */
  protected void setPosition(final int p) {
    buffer.reset(p);
  }

  /**
   * Returns the buffer position of the most recently read char.
   *
   * @return The buffer position of the most recently read char.
   */
  protected int getPosition() {
    return buffer.size();
  }

  /**
   * Read the next JSON token. A JSON token is one of:
   * <ul>
   * <li>Structural:<ul>
   * <li>A character that is one of:<pre><code>{ } [ ] : ,</code></pre></li></ul></li>
   * <li>A property key:<ul>
   * <li>A string that matches:<pre>^".*"$</pre></li></ul></li>
   * <li>A property or array member value:<ul>
   * <li>A string that matches:<pre>^".*"$</pre></li>
   * <li>A number that matches:<pre>{@code ^-?(([0-9])|([1-9][0-9]+))(\.[\.0-9]+)?([eE][+-]?(([0-9])|([1-9][0-9]+)))?$}</pre></li>
   * <li>A literal that matches:<pre>{@code ^(null)|(true)|(false)$}</pre></li></ul></li>
   * <li>Whitespace:<ul>
   * <li>Whitespace that matches:<pre>{@code ^[ \n\r\t]+$}</pre></li></ul></li>
   * </ul>
   * <p>
   * <b>Note:</b> If this instance ignores whitespace, this method will skip
   * whitespace tokens.
   *
   * @return The next JSON token, or {@code null} if the end of content has
   *         been reached.
   * @throws IOException If an I/O error occurs.
   * @throws JsonParseException If the content is not well formed.
   */
  public String readToken() throws IOException, JsonParseException {
    final int start;
    final int end;
    if (getPosition() == getEndPosition(index)) {
      start = nextToken();
      end = getEndPosition(index);
    }
    else {
      start = getPosition();
      end = getEndPosition(index);
      setPosition(end);
    }

    // End of stream
    if (start == -1)
      return null;

    // Sanity check, which should never occur
    if (start == end)
      throw new IllegalStateException("Illegal JSON content [errorOffset: " + start + "]");

    return end - start == 1 ? String.valueOf(buf()[start]) : new String(buf(), start, end - start);
  }

  /**
   * Supporting method for {@link #read()} and {@link #read(char[], int, int)}
   * to advance to the next token if the end of the current token has been
   * reached.
   *
   * @return {@code true} if there are more chars to read.
   * @throws IOException If an I/O error occurs.
   * @throws JsonParseException If the content is not well formed.
   * @see #nextToken()
   */
  private boolean hasRemainig() throws IOException, JsonParseException {
    if (index != -1 && getPosition() < getEndPosition(index))
      return true;

    final int start = nextToken();
    if (start == -1)
      return false;

    setPosition(start);
    return true;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Characters read with this method advance the characters of the tokens to
   * which they belong. Therefore, when partially reading a token with
   * {@code read()}, subsequent calls to {@code readToken()} will return <i>the
   * remaining characters of the token that have not yet been returned by
   * {@code read()}. Characters read with this method undergo the same
   * token-level error checking as in {@link #readTokenStart()} or
   * {@link #readToken()}.
   *
   * @return The character read, as an integer in the range 0 to 65535
   *         ({@code 0x00-0xffff}), or -1 if the end of the stream has been
   *         reached.
   * @throws IOException If an I/O error occurs.
   * @throws JsonParseException If the content is not well formed.
   * @see #readTokenStart()
   * @see #readToken()
   */
  @Override
  public int read() throws IOException {
    return hasRemainig() ? super.read() : -1;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Characters read with this method advance the characters of the tokens to
   * which they belong. Therefore, when partially reading a token with
   * {@code read(char[],int,int)}, subsequent calls to {@code readToken()} will
   * return <i>the remaining characters of the token that have not yet been
   * returned by {@code read()}. Characters read with this method undergo the
   * same token-level error checking as in {@link #readTokenStart()} or
   * {@link #readToken()}.
   *
   * @param cbuf Destination buffer.
   * @param off Offset at which to start storing characters.
   * @param len Maximum number of characters to read.
   * @return The number of characters read, or -1 if the end of the stream has
   *         been reached.
   * @throws IOException If an I/O error occurs.
   * @throws IndexOutOfBoundsException If {@code off} is negative, or
   *           {@code len} is negative, or {@code len} is greater than
   *           {@code cbuf.length - off}.
   * @throws JsonParseException If the content is not well formed.
   * @see #readTokenStart()
   * @see #readToken()
   */
  @Override
  public int read(final char[] cbuf, int off, int len) throws IOException {
    for (int count = 0;;) {
      if (len == 0 || !hasRemainig())
        return count;

      int remaining = getEndPosition(index) - getPosition();
      if (remaining > len)
        return count + super.read(cbuf, off, len);

      if (remaining > 0) {
        remaining = super.read(cbuf, off, remaining);
        len -= remaining;
        off += remaining;
        count += remaining;
      }
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * Characters read with this method advance the characters of the tokens to
   * which they belong. Therefore, when partially reading a token with
   * {@code read(char[])}, subsequent calls to {@code readToken()} will return
   * the remaining characters of the token that have not yet been returned by
   * {@code read()}. Characters read with this method undergo the same
   * token-level validation as in {@link #readTokenStart()} or
   * {@link #readToken()}.
   *
   * @param cbuf Destination buffer.
   * @param off Offset at which to start storing characters.
   * @param len Maximum number of characters to read.
   * @return The number of characters read, or -1 if the end of the stream has
   *         been reached.
   * @throws IOException If an I/O error occurs.
   * @throws IndexOutOfBoundsException If {@code off} is negative, or
   *           {@code len} is negative, or {@code len} is greater than
   *           {@code cbuf.length - off}.
   * @throws JsonParseException If the content is not well formed.
   * @see #readTokenStart()
   * @see #readToken()
   */
  @Override
  public int read(final char[] cbuf) throws IOException {
    return super.read(cbuf);
  }

  /**
   * Returns this JsonReader, since it is itself an implementation of the {@link Iterator} interface.
   * The iterator iterates over the JSON token strings produced by
   * {@code JsonReader.readToken()}.
   *
   * @return This instance.
   */
  @Override
  public Iterator<String> iterator() {
    return this;
  }

  /**
   * Returns {@code true} if the iteration has more tokens. (In other words,
   * returns {@code true} if {@link #next} would return a token rather than
   * throw an exception).
   *
   * @return {@code true} If the iteration has more tokens.
   * @throws IllegalStateException If an {@code IOException} occurs while
   *           reading from the underlying stream.
   * @throws JsonParseException If the content is not well formed.
   */
  @Override
  public boolean hasNext() throws IllegalStateException, JsonParseException {
    if (index < size())
      return true;

    try {
      nextToken();
      return index < size();
    }
    catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns the next token in the iteration.
   *
   * @return The next token in the iteration.
   * @throws NoSuchElementException If the iteration has no more tokens.
   * @throws IllegalStateException If an {@code IOException} occurs while
   *           reading from the underlying stream.
   * @throws JsonParseException If the content is not well formed.
   */
  @Override
  public String next() throws IllegalStateException, JsonParseException {
    if (!hasNext())
      throw new NoSuchElementException();

    try {
      return readToken();
    }
    catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns the nearest non-whitespace token that is {@code offset} distance
   * back from {@code index}.
   *
   * @param offset The offset back from {@code index} of the first
   *          non-whitespace token to check.
   * @return The nearest non-whitespace token that is {@code offset} distance
   *         back from {@code index}.
   */
  private int nearestNonWsToken(int offset) {
    if (index < offset)
      return -1;

    final char[] buf = buf();
    if (ignoreWhitespace)
      return buf[getStartPosition(index - offset)];

    // Advance the offset if the current index points to whitespace
    char ch = buf[getStartPosition(index)];
    if (Characters.isWhiteSpace(ch))
      ++offset;

    // Advance the offset if the offset itself points to whitespace
    while (offset++ < index)
      if (!Characters.isWhiteSpace(ch = buf[getStartPosition(index + 1 - offset)]))
        return ch;

    return -1;
  }

  /**
   * Supporting method to read until the end of the next token, and return the
   * start position of the token that was just read.
   *
   * @return The start index of the next token.
   * @throws IOException If an I/O error occurs.
   * @throws JsonParseException If the content is not well formed.
   * @see #readTokenStart()
   */
  private int nextToken() throws IOException, JsonParseException {
    int start = -1;
    if (index == -1) {
      // Perform the initial read-ahead
      start = readTokenStart();
      index = -1;
    }
    else if (getStartPosition(index + 1) == -1) {
      return start;
    }

    // Set next index
    setIndex0(index + 1);

    // Fast return if there is no need to re-read an already read token
    if (index < size() - 1)
      return getStartPosition(index);

    final int beforeIndex = index;
    try {
      // Advance to the next token
      readTokenStart();
    }
    finally {
      // Set to previous index only if index changed due to readTokenStart()
      if (beforeIndex < index)
        setIndex0(index - 1);
    }

    return start != -1 ? start : getStartPosition(index);
  }

  /**
   * Read until the end of the next token, and return the start index of the
   * token that was just read. The end index of the token can be retrieved with
   * a subsequent call to {@link #getPosition()}. If the end of content has been
   * reached, this method returns -1.
   *
   * @return The start index of the next token.
   * @throws IOException If an I/O error occurs.
   * @throws JsonParseException If the content is not well formed.
   * @see #nextToken()
   */
  protected int readTokenStart() throws IOException, JsonParseException {
    if (0 < size() && getPosition() != getEndPosition(index))
      throw new IllegalStateException("Buffer position (" + getPosition() + ") misaligned with end position (" + getEndPosition(index) + ") on index (" + index + ")");

    // Fast return if there is no need to re-read an already read token
    if (index < size() - 1)
      return setIndex0(index + 1);

    nextStart = 0;
    int ch = readNonWS(super.read());
    do {
      if (nextStart != 0 || ch == -1)
        return advance(ch, nextStart);

      final int ch1 = nearestNonWsToken(0);
      final int ch2 = ch1 == -1 ? -1 : nearestNonWsToken(1);
      // read not after property key
      if (ch2 == ':' || ch1 != '"' || !Buffers.get(scope, depth)) {
        if (ch == '{' || ch == '[') {
          if (ch1 == '{')
            throw new JsonParseException("Expected character '}', but encountered '" + (char)ch + "'", getPosition() - 1);

          if (ch == '{')
            Buffers.set(scope, ++depth, DEFAULT_SCOPE_RESIZE_FACTOR);
          else
            Buffers.clear(scope, ++depth);

          nextStart = getPosition();
          continue;
        }

        if (ch == '}' || ch == ']') {
          final char expected;
          if (Buffers.get(scope, depth--)) {
            Buffers.clear(scope, depth + 1);
            expected = '}';
          }
          else {
            expected = ']';
          }

          if (expected != ch)
            throw new JsonParseException("Expected character '" + expected + "', but encountered '" + (char)ch + "'", getPosition() - 1);

          nextStart = getPosition();
          continue;
        }

        // read ','
        if (ch == ',') {
          nextStart = getPosition();
          continue;
        }

        if (depth == -1)
          throw new JsonParseException("Expected character '{' or '[', but encountered '" + (char)ch + "'", getPosition() - 1);

        // read property key
        if (Buffers.get(scope, depth) && ch1 != ':') {
          if (ch != '"')
            throw new JsonParseException("Expected character '\"', but encountered '" + (char)ch + "'", getPosition() - 1);

          final int start = getPosition();
          ch = readQuoted();
          return advance(ch, start);
        }
      }
      else if (ch != ':') {
        throw new JsonParseException("Expected character ':', but encountered '" + (char)ch + "'", getPosition() - 1);
      }

      // Read property value or array member
      if (!Buffers.get(scope, depth) || ch1 == ':') {
        final int start = getPosition();
        ch = ch == '"' ? readQuoted() : readUnquoted(ch);
        return advance(ch, start);
      }

      nextStart = getPosition();
    }
    while ((ch = super.read()) != -1);

    if (depth >= 0)
      throw new JsonParseException("Missing closing scope character: '" + (Buffers.get(scope, depth) ? '}' : ']') + "'", getPosition());

    return advance(ch, nextStart);
  }

  /**
   * Returns the {@code char[]} buffer of the underlying {@code ReplayReader}.
   *
   * @return The {@code char[]} buffer of the underlying {@code ReplayReader}.
   */
  protected char[] buf() {
    return buffer.buf();
  }

  /**
   * Called from {@link #readTokenStart()} to advance the token index, adjust
   * the return index, and to unread the last read char.
   *
   * @param ch The last read char.
   * @param pos The return position.
   * @return The adjusted return position.
   */
  private int advance(final int ch, int pos) {
    // Move back a position of 1, because a single extra char has been read
    if (ch != -1)
      setPosition(getPosition() - 1);

    if (++index != size())
      throw new IllegalStateException("Index (" + index + ") misaligned with tokens count (" + size() + ")");

    scopes.add(scope.clone());
    depths.add(depth);
    positions.add(Numbers.Compound.encode(--pos, getPosition()));
    return pos;
  }

  /**
   * Read until the first non-whitespace char. If whitespace is not ignored,
   * this method will set {@link #nextStart} to the starting position of the
   * whitespace.
   *
   * @param ch The first char to test whether it is not whitespace.
   * @return The first non-whitespace char.
   * @throws IOException If an I/O error occurs.
   * @throws JsonParseException If content was found that was not expected.
   */
  private int readNonWS(int ch) throws IOException, JsonParseException {
    if (!Characters.isWhiteSpace(ch))
      return ch;

    final int start = getPosition();
    while (Characters.isWhiteSpace(ch) && (ch = super.read()) != -1);
    if (index == -1)
      return ch;

    if (!ignoreWhitespace && depth != -1 && start != getPosition())
      nextStart = start;

    if (depth == -1 && ch != -1)
      throw new JsonParseException("No content is expected at this point: " + (char)ch, getPosition() - 1);

    return ch;
  }

  /**
   * Read until the first unescaped {@code '"'} char.
   *
   * @return The char after the first unescaped {@code '"'} char.
   * @throws IOException If an I/O error occurs.
   * @throws JsonParseException If the string is not terminated.
   */
  private int readQuoted() throws IOException {
    final int start = getPosition();
    boolean esacped = false;
    for (int ch; (ch = super.read()) != -1;) {
      if (ch == '\\')
        esacped = true;
      else if (esacped)
        esacped = false;
      else if (ch == '"')
        return super.read();
    }

    throw new JsonParseException("Unterminated string", start - 1);
  }

  /**
   * Read until the first non-number or non-literal char.
   *
   * @param ch The first char to test whether it is a non-literal char.
   * @return The first non-literal char.
   * @throws IOException If an I/O error occurs.
   * @throws JsonParseException If the content is not well formed.
   */
  private int readUnquoted(int ch) throws IOException, JsonParseException {
    // Read number
    if (ch == '-' || '0' <= ch && ch <= '9') {
      int first = ch;
      int last = first;
      boolean hasDot = false;
      for (int i = 0; (ch = super.read()) != -1; ++i, last = ch) {
        if (ch == '.') {
          if (first == '-' && i == 0)
            throw new JsonParseException("Integer component required before fraction part", getPosition() - 1);

          if (hasDot)
            throw new JsonParseException("Illegal character: '" + (char)ch + "'", getPosition() - 1);

          hasDot = true;
        }
        else if (last == '0' && ch == '0' && i == (first == '-' ? 1 : 0)) {
          throw new JsonParseException("Leading zeros are not allowed", getPosition() - 2);
        }
        else if (ch < '0' || '9' < ch) {
          break;
        }
      }

      if (last == '.')
        throw new JsonParseException("Decimal point must be followed by one or more digits", getPosition() - 1);

      if (last == '-')
        throw new JsonParseException("Expected digit, but encountered '" + (char)ch + "'", getPosition() - 1);

      if (ch == 'e' || ch == 'E') {
        last = ch;
        for (int i = 0; (ch = super.read()) != -1; ++i, last = ch) {
          if (ch == '-' || ch == '+') {
            first = '~';
            if (i > 0)
              throw new JsonParseException("Illegal character: '" + (char)ch + "'", getPosition() - 1);
          }
          else if (ch < '0' || '9' < ch) {
            break;
          }
          else if (last == '0' && i == (first == '~' ? 2 : 1)) {
            throw new JsonParseException("Leading zeros are not allowed", getPosition() - 2);
          }
        }

        if (last == '-' || last == '+')
          throw new JsonParseException("Expected digit, but encountered '" + (char)ch + "'", getPosition() - 1);
      }

      if (ch != ']' && ch != '}' && ch != ',' && !Characters.isWhiteSpace(ch))
        throw new JsonParseException("Illegal character: '" + (char)ch + "'", getPosition() - 1);

      return ch;
    }

    // Read literal
    for (int i = 0; i < literals.length; ++i) {
      if (ch == literals[i][0]) {
        final char[] literal = literals[i];
        for (int j = 1; j < literal.length; ++j)
          if ((ch = super.read()) != literal[j])
            throw new JsonParseException("Illegal character: '" + (char)ch + "'", getPosition() - 1);

        ch = super.read();
        if (!isStructural(ch) && !Characters.isWhiteSpace(ch))
          break;

        return ch;
      }
    }

    throw new JsonParseException("Illegal character: '" + (char)ch + "'", getPosition() - 1);
  }
}