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
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import org.libj.io.UnsynchronizedStringReader;
import org.libj.lang.Buffers;
import org.libj.lang.Numbers;
import org.libj.util.primitive.ArrayIntList;
import org.libj.util.primitive.ArrayLongList;
import org.libj.util.primitive.LongIterable;
import org.libj.util.primitive.LongIterator;

/**
 * Validating {@link Reader} for JSON streams that reads JSON tokens sequentially, while asserting the content conforms to the
 * <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627</a> specification.
 * <p>
 * This implementation provides the following features:
 * <ul>
 * <li>Regular read of JSON tokens, sequentially returning each token {@link String}: {@link #readToken()}. (Please see
 * {@link #readToken()} for a definition of: {@code token}).</li>
 * <li>Optimized read of JSON tokens, sequentially returning the {@code int} starting position of each token:
 * {@link #readTokenStart()}. (This position can be dereferenced via: {@link JsonReader#buf()}).</li>
 * <li>Implements the {@link LongIterable} interface, to sequentially iterate over each token: {@link #iterator()}.</li>
 * <li>Caches and indexes each token: {@link #getIndex()}.</li>
 * <li>Allows to read back previously read tokens: {@link #setIndex(int)}.</li>
 * <li>Support partial reads of tokens with {@link #read()}, {@link #read(char[])}. and {@link #read(char[], int, int)} methods.
 * </ul>
 */
public class JsonReader extends JsonReplayReader implements LongIterable, LongIterator {
  private static final char[][] literals = {{'n', 'u', 'l', 'l'}, {'t', 'r', 'u', 'e'}, {'f', 'a', 'l', 's', 'e'}};

  /** Number of characters in the JSON document */
  private static final int DEFAULT_BUFFER_SIZE = 2048;

  /** Number of tokens in the JSON document */
  private static final int DEFAULT_TOKENS_SIZE = 128;

  /** Number of [] or {} scopes in the JSON document */
  private static final int DEFAULT_SCOPE_SIZE = 2;

  /** Resize factor for scope buffer */
  private static final double DEFAULT_SCOPE_RESIZE_FACTOR = 2;

  /**
   * Returns a {@link JsonReader} with the specified {@link Reader reader} providing the data stream.
   * <p>
   * If the specified {@link Reader reader} is an instance of {@link JsonReader}, the same instance is returned. Otherwise, a new
   * {@link JsonReader} wrapping the specified {@link Reader reader} is returned.
   *
   * @param reader The {@link Reader} providing the data stream.
   * @return A {@link JsonReader} with the specified {@link Reader reader} providing the data stream.
   * @throws IllegalArgumentException If {@code reader} is null.
   */
  public static JsonReader of(final Reader reader) {
    return reader instanceof JsonReader ? (JsonReader)reader : new JsonReader(reader);
  }

  private ArrayLongList positions = new ArrayLongList(DEFAULT_TOKENS_SIZE);
  private ArrayList<long[]> scopes = new ArrayList<>(DEFAULT_SCOPE_SIZE);
  private ArrayIntList depths = new ArrayIntList(DEFAULT_SCOPE_SIZE);
  /** The index of the token last read */
  private int index = -1;
  private int markedIndex = index;

  private long[] scope = new long[DEFAULT_SCOPE_SIZE];
  private int depth = -1;
  private int nextStart = 0;

  private final boolean ignoreWhitespace;

  /**
   * Construct a new {@link JsonReader} for JSON content to be read from the specified {@link Reader}, <b>that ignores inter-token
   * whitespace</b>. This constructor is equivalent to calling {@code new JsonReader(reader, true)}.
   *
   * @param reader The {@link Reader} from which JSON is to be read.
   * @throws IllegalArgumentException If {@code reader} is null.
   */
  public JsonReader(final Reader reader) {
    this(reader, true);
  }

  /**
   * Construct a new {@link JsonReader} for JSON content to be read from the specified {@link Reader}.
   *
   * @param reader The {@link Reader} from which JSON is to be read.
   * @param ignoreWhitespace If {@code ignoreWhitespace == false}, inter-token whitespace will <b>not</b> be skipped.
   * @throws IllegalArgumentException If {@code reader} is null.
   */
  public JsonReader(final Reader reader, final boolean ignoreWhitespace) {
    super(reader, DEFAULT_BUFFER_SIZE);
    this.ignoreWhitespace = ignoreWhitespace;
  }

  /**
   * Construct a new {@link JsonReader} for JSON content to be read from the specified string, <b>that ignores inter-token
   * whitespace</b>. This constructor is equivalent to calling {@code new JsonReader(str, true)}.
   *
   * @param str The string with the JSON document to be read.
   * @throws IllegalArgumentException If {@code str} is null.
   */
  public JsonReader(final String str) {
    this(str, true);
  }

  /**
   * Construct a new {@link JsonReader} for JSON content to be read from the specified string.
   *
   * @param str The string with the JSON document to be read.
   * @param ignoreWhitespace If {@code ignoreWhitespace == false}, inter-token whitespace will <b>not</b> be skipped.
   * @throws IllegalArgumentException If {@code str} is null.
   */
  public JsonReader(final String str, final boolean ignoreWhitespace) {
    super(new UnsynchronizedStringReader(str), DEFAULT_BUFFER_SIZE);
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
   * Sets the token index of this reader, such that {@link #getIndex()} returns the specified index. Note, that calling
   * {@link #readToken()} after this method will return the token with index {@code index + 1}.
   *
   * @param index The index to be set.
   * @throws IllegalArgumentException If {@code index < -1 || size() <= index}.
   */
  public void setIndex(final int index) {
    if (this.index == index)
      return;

    if (index < -1 || size() <= index)
      throw new IllegalArgumentException("Index out of range [-1," + (size() - 1) + "]: " + index);

    setIndex0(index);
  }

  /**
   * Supporting method to set the index, such that {@link #getIndex()} returns the specified index. Since the index is one minus the
   * next token index to be read, a special case is made for index = -1, because there is no previous "end position" for the first
   * token.
   *
   * @param index The index to be set.
   * @return The start position of the token at {@code index + 1}.
   * @throws IndexOutOfBoundsException If {@code index < -1 || size() <= index}.
   */
  private int setIndex0(int index) {
    this.index = index;
    final int start;
    if (index > -1) {
      final long position = positions.get(index);
      start = Numbers.Composite.decodeInt(position, 0);
      final int end = Numbers.Composite.decodeInt(position, 1);
      setPosition(end);
      depth = depths.get(index);
    }
    else {
      final long position = positions.get(++index);
      start = Numbers.Composite.decodeInt(position, 0);
      setPosition(start);
      depth = 0;
    }

    scope = scopes.get(index);
    return start;
  }

  /**
   * Returns the number of tokens read thus far. The value returned by this method defines the upper bound of
   * {@link #setIndex(int)}.
   *
   * @return The number of tokens read thus far. The value returned by this method defines the upper bound of
   *         {@link #setIndex(int)}.
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
    return Numbers.Composite.decodeInt(positions.get(index), 0);
  }

  /**
   * Returns the end position at {@code index}.
   *
   * @param index The token index.
   * @return The end position at {@code index}.
   */
  protected int getEndPosition(final int index) {
    return Numbers.Composite.decodeInt(positions.get(index), 1);
  }

  /**
   * Set the buffer to position {@code p}, such that a subsequent call to {@link #read()} will return the char at {@code p + 1}.
   *
   * @param p The position.
   * @throws IllegalArgumentException If {@code p} is negative, or if {@code p} is greater than the length of the underlying buffer.
   */
  protected void setPosition(final int p) {
    buffer.reset(p);
  }

  /**
   * Returns the buffer position of the most recently read char.
   *
   * @return The buffer position of the most recently read char.
   */
  public int getPosition() {
    return buffer.size();
  }

  /**
   * Read the next <u>JSON token</u>, and return a {@linkplain org.libj.lang.Numbers.Composite#encode(int,int) composite}
   * {@code long} of the <u>offset index</u> and <u>token length</u>, which can be decoded with
   * {@link org.libj.lang.Numbers.Composite#decodeInt(long,int) Composite#decodeInt(long,int)}.
   * <p>
   * A <u>JSON token</u> is one of:
   * <ul>
   * <li>Structural:
   * <ul>
   * <li>A character that is one of:
   *
   * <pre>
   * <code>{ } [ ] : ,</code>
   * </pre>
   *
   * </li>
   * </ul>
   * </li>
   * <li>A property key:
   * <ul>
   * <li>A string that matches:
   *
   * <pre>{@code ^".*"$}</pre>
   *
   * </li>
   * </ul>
   * </li>
   * <li>A property or array member value:
   * <ul>
   * <li>A string that matches:
   *
   * <pre>{@code ^".*"$}</pre>
   *
   * </li>
   * <li>A number that matches:
   *
   * <pre>{@code ^-?(0|[1-9]\d*)(\.\d+)?([eE][+-]?([1-9]\d*))?$}</pre>
   *
   * </li>
   * <li>A literal that matches:
   *
   * <pre>{@code ^null|true|false$}</pre>
   *
   * </li>
   * </ul>
   * </li>
   * <li>Whitespace:
   * <ul>
   * <li>Whitespace string that matches:
   *
   * <pre>{@code ^[ \n\r\t]+$}</pre>
   *
   * </li>
   * </ul>
   * </li>
   * </ul>
   *
   * @implNote If this instance ignores whitespace, this method will skip whitespace tokens.
   * @return A {@linkplain org.libj.lang.Numbers.Composite#encode(int,int) composite} {@code long} of the offset index and length
   *         into the underlying {@link JsonReader}, or {@code -1} if the end of content has been reached.
   * @throws IOException If an I/O error has occurred.
   * @throws JsonParseException If the content is not well formed.
   * @see org.libj.lang.Numbers.Composite#decodeInt(long,int)
   */
  public long readToken() throws IOException, JsonParseException {
    final int start;
    final int end;
    if (index == -1 || getPosition() == getEndPosition(index)) {
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
      return -1;

    // Sanity check, which should never occur
    if (start == end)
      throw new IllegalStateException("Illegal JSON content [errorOffset: " + start + "]");

    return Numbers.Composite.encode(start, end - start);
  }

  /**
   * Supporting method for {@link #read()} and {@link #read(char[],int,int)} to advance to the next token if the end of the current
   * token has been reached.
   *
   * @return {@code true} if there are more chars to read.
   * @throws IOException If an I/O error has occurred.
   * @throws JsonParseException If the content is not well formed.
   * @see #nextToken()
   */
  private boolean hasRemaining() throws IOException, JsonParseException {
    if (index != -1 && getPosition() < getEndPosition(index))
      return true;

    final int start = nextToken();
    if (start == -1)
      return false;

    setPosition(start);
    return true;
  }

  @Override
  public void mark(final int readlimit) {
    markedIndex = getIndex();
    super.mark(readlimit);
  }

  @Override
  public void reset() {
    setIndex(markedIndex);
    super.reset();
  }

  /**
   * {@inheritDoc}
   * <p>
   * Characters read with this method advance the characters of the tokens to which they belong. Therefore, when partially reading a
   * token with {@link #read()}, subsequent calls to {@link #readToken()} will return <i>the remaining characters of the token that
   * have not yet been returned by {@link #read()}</i>. Characters read with this method undergo the same token-level error checking
   * as in {@link #readTokenStart()} or {@link #readToken()}.
   *
   * @return The character read, as an integer in the range 0 to 65535 ({@code 0x00-0xffff}), or -1 if the end of the stream has
   *         been reached.
   * @throws IOException If an I/O error has occurred.
   * @throws JsonParseException If the content is not well formed.
   * @see #readTokenStart()
   * @see #readToken()
   */
  @Override
  public int read() throws IOException {
    return hasRemaining() ? super.read() : -1;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Characters read with this method advance the characters of the tokens to which they belong. Therefore, when partially reading a
   * token with {@link #read(char[],int,int)}, subsequent calls to {@link #readToken()} will return <i>the remaining characters of
   * the token that have not yet been returned by {@link #read()}</i>. Characters read with this method undergo the same token-level
   * error checking as in {@link #readTokenStart()} or {@link #readToken()}.
   *
   * @param cbuf Destination buffer.
   * @param off Offset at which to start storing characters.
   * @param len Maximum number of characters to read.
   * @return The number of characters read, or -1 if the end of the stream has been reached.
   * @throws IOException If an I/O error has occurred.
   * @throws IndexOutOfBoundsException If {@code off} is negative, or {@code len} is negative, or {@code len} is greater than
   *           {@code cbuf.length - off}.
   * @throws JsonParseException If the content is not well formed.
   * @see #readTokenStart()
   * @see #readToken()
   */
  @Override
  public int read(final char[] cbuf, int off, int len) throws IOException {
    for (int count = 0;;) { // [N]
      if (len == 0 || !hasRemaining())
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
   * Characters read with this method advance the characters of the tokens to which they belong. Therefore, when partially reading a
   * token with {@link #read(char[])}, subsequent calls to {@link #readToken()} will return the remaining characters of the token
   * that have not yet been returned by {@link #read()}. Characters read with this method undergo the same token-level validation as
   * in {@link #readTokenStart()} or {@link #readToken()}.
   *
   * @param cbuf Destination buffer.
   * @return The number of characters read, or -1 if the end of the stream has been reached.
   * @throws IOException If an I/O error has occurred.
   * @throws IndexOutOfBoundsException If {@code off} is negative, or {@code len} is negative, or {@code len} is greater than
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
   * Returns this {@link JsonReader}, since it is itself an implementation of the {@link LongIterator} interface. The iterator
   * iterates over the JSON token strings produced by {@code JsonReader.readToken()}.
   *
   * @return This instance.
   */
  @Override
  public LongIterator iterator() {
    return this;
  }

  /**
   * Returns {@code true} if the iteration has more tokens. (In other words, returns {@code true} if {@link #next} would return a
   * token rather than throw an exception).
   *
   * @return {@code true} if the iteration has more tokens.
   * @throws UncheckedIOException If an {@link IOException} occurs while reading from the underlying stream.
   * @throws JsonParseException If the content is not well formed.
   */
  @Override
  public boolean hasNext() throws UncheckedIOException, JsonParseException {
    if (index < size())
      return true;

    try {
      nextToken();
      return index < size();
    }
    catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Returns the next {@linkplain org.libj.lang.Numbers.Composite#encode(int,int) composite} {@code long} of the offset index and
   * length into the underlying {@link JsonReader}.
   *
   * @return The next {@linkplain org.libj.lang.Numbers.Composite#encode(int,int) composite} {@code long} of the offset index and
   *         length into the underlying {@link JsonReader}.
   * @throws NoSuchElementException If the iteration has no more tokens.
   * @throws UncheckedIOException If an {@link IOException} occurs while reading from the underlying stream.
   * @throws JsonParseException If the content is not well formed.
   * @see #readToken()
   */
  @Override
  public long next() throws UncheckedIOException, JsonParseException {
    if (!hasNext())
      throw new NoSuchElementException();

    try {
      return readToken();
    }
    catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Returns the index of nearest non-whitespace token that is {@code offset} distance back from {@link #index}.
   *
   * @param offset The offset back from {@code index} of the first non-whitespace token to check.
   * @return The index of nearest non-whitespace token that is {@code offset} distance back from {@link #index}.
   */
  private int nearestNonWsToken(int offset) {
    if (index < offset)
      return -1;

    final char[] buf = buf();
    if (ignoreWhitespace)
      return buf[getStartPosition(index - offset)];

    // Advance the offset if the current index points to whitespace
    char ch = buf[getStartPosition(index)];
    if (JsonUtil.isWhitespace(ch))
      ++offset;

    // Advance the offset if the offset itself points to whitespace
    while (offset++ < index)
      if (!JsonUtil.isWhitespace(ch = buf[getStartPosition(index + 1 - offset)]))
        return ch;

    return -1;
  }

  /**
   * Supporting method to read until the end of the next token, and return the start position of the token that was just read.
   *
   * @return The start index of the next token.
   * @throws IOException If an I/O error has occurred.
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
   * Read until the end of the next token, and return the start index of the token that was just read. The end index of the token
   * can be retrieved with a subsequent call to {@link #getPosition()}. If the end of content has been reached, this method returns
   * -1.
   *
   * @return The start index of the next token.
   * @throws IOException If an I/O error has occurred.
   * @throws JsonParseException If the content is not well formed.
   * @see #nextToken()
   */
  protected int readTokenStart() throws IOException, JsonParseException {
    if (0 < size() && index > -1 && getPosition() != getEndPosition(index))
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
        final int start = getPosition(); // 3502
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
   * Returns the {@code char[]} buffer of the underlying {@link JsonReplayReader}.
   *
   * @return The {@code char[]} buffer of the underlying {@link JsonReplayReader}.
   */
  public char[] buf() {
    return buffer.buf();
  }

  /**
   * Appends {@code len} characters starting at {@code off} from {@link #buf()} to the provided {@link StringBuilder}.
   *
   * @param builder The {@link StringBuilder}.
   * @param off The starting offset in {@link #buf()} from which to append.
   * @param len The number of characters from {@link #buf()} to append.
   */
  public void bufToString(final StringBuilder builder, final int off, final int len) {
    builder.append(buf(), off, len);
  }

  /**
   * Returns a string from {@link #buf()} starting from {@code off} with {@code len} characters.
   *
   * @param off The starting offset in {@link #buf()} from which to append.
   * @param len The number of characters from {@link #buf()} to append.
   * @return A string from {@link #buf()} starting from {@code off} with {@code len} characters.
   */
  public String bufToString(final int off, final int len) {
    return new String(buf(), off, len);
  }

  /**
   * Returns the {@code char} from {@link #buf()} at the index of {@code off}.
   *
   * @param off The index in {@link #buf()}.
   * @return The {@code char} from {@link #buf()} at the index of {@code off}.
   */
  public char bufToChar(final int off) {
    return buf()[off];
  }

  /**
   * Called from {@link #readTokenStart()} to advance the token index, adjust the return index, and to unread the last read char.
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
    positions.add(Numbers.Composite.encode(--pos, getPosition()));
    return pos;
  }

  /**
   * Read until the first non-whitespace char. If whitespace is not ignored, this method will set {@link #nextStart} to the starting
   * position of the whitespace.
   *
   * @param ch The first char to test whether it is not whitespace.
   * @return The first non-whitespace char.
   * @throws IOException If an I/O error has occurred.
   * @throws JsonParseException If content was found that was not expected.
   */
  private int readNonWS(int ch) throws IOException, JsonParseException {
    if (!JsonUtil.isWhitespace(ch))
      return ch;

    final int start = getPosition();
    while (JsonUtil.isWhitespace(ch) && (ch = super.read()) != -1);
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
   * @throws IOException If an I/O error has occurred.
   * @throws JsonParseException If the string is not terminated.
   */
  private int readQuoted() throws IOException {
    final int start = getPosition();
    boolean escaped = false;
    for (int ch; (ch = super.read()) != -1;) { // [N]
      if (escaped)
        escaped = false;
      else if (ch == '\\')
        escaped = true;
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
   * @throws IOException If an I/O error has occurred.
   * @throws JsonParseException If the content is not well formed.
   */
  private int readUnquoted(int ch) throws IOException, JsonParseException {
    // Read number
    if (ch == '-' || '0' <= ch && ch <= '9') {
      int first = ch;
      int prev = first;
      boolean hasDot = false;
      for (int i = 0; (ch = super.read()) != -1; ++i, prev = ch) { // [N]
        if (ch == '.') {
          if (first == '-' && i == 0)
            throw new JsonParseException("Integer component required before fraction part", getPosition() - 1);

          if (hasDot)
            throw new JsonParseException("Illegal character: '" + (char)ch + "'", getPosition() - 1);

          hasDot = true;
        }
        else if (ch < '0' || '9' < ch) {
          break;
        }
        else if (prev == '0' && i == (first == '-' ? 1 : 0)) {
          throw new JsonParseException("Leading zeros are not allowed", getPosition() - 2);
        }
      }

      if (prev == '.')
        throw new JsonParseException("Decimal point must be followed by one or more digits", getPosition() - 1);

      if (ch == 'e' || ch == 'E') {
        prev = ch;
        for (int i = 0; (ch = super.read()) != -1; ++i, prev = ch) { // [N]
          if (ch == '-' || ch == '+') {
            first = '~';
            if (i > 0)
              throw new JsonParseException("Illegal character: '" + (char)ch + "'", getPosition() - 1);
          }
          else if (ch < '0' || '9' < ch) {
            break;
          }
          else if (prev == '0' && i == (first == '~' ? 2 : 1)) {
            throw new JsonParseException("Leading zeros are not allowed", getPosition() - 2);
          }
        }

        if (prev == 'e' || prev == 'E')
          throw new JsonParseException("\"" + prev + "\" must be followed by one or more digits", getPosition() - 1);

        if (prev == '-' || prev == '+')
          throw new JsonParseException("Expected digit, but encountered '" + (char)ch + "'", getPosition() - 1);
      }

      if (ch != ']' && ch != '}' && ch != ',' && !JsonUtil.isWhitespace(ch))
        throw new JsonParseException(ch == -1 ? "Unexpected end of document" : "Illegal character: '" + (char)ch + "'", getPosition() - 1);

      return ch;
    }

    // Read literal
    for (int i = 0, i$ = literals.length; i < i$; ++i) { // [A]
      if (ch == literals[i][0]) {
        final char[] literal = literals[i];
        for (int j = 1, j$ = literal.length; j < j$; ++j) // [A]
          if ((ch = super.read()) != literal[j])
            throw new JsonParseException(ch == -1 ? "Unexpected end of document" : "Illegal character: '" + (char)ch + "'", getPosition() - 1);

        ch = super.read();
        if (!JsonUtil.isStructural(ch) && !JsonUtil.isWhitespace(ch))
          break;

        return ch;
      }
    }

    throw new JsonParseException(ch == -1 ? "Unexpected end of document" : "Illegal character: '" + (char)ch + "'", getPosition() - 1);
  }

  @Override
  public synchronized void close() throws IOException {
    super.close();
    // Commented out, to allow this reader to be used after it is closed.
    // this.positions = null;
    // this.scopes.clear();
    // this.scopes = null;
    // this.depths = null;
    // this.scope = null;
  }
}