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

import static org.libj.lang.Assertions.*;

import java.io.IOException;

/**
 * Handler interface used for parsing JSON with
 * {@link JsonParser#parse(JsonReader)}.
 */
public interface JsonParser {
  /**
   * Parse the JSON document with this {@link JsonParser}.
   *
   * @param reader The {@link JsonReader} from which JSON is read.
   * @return {@code true} if the document has been read entirely. {@code false}
   *         if parsing was aborted by a handler callback. If a handler aborts
   *         parsing, subsequent calls to {@link #parse(JsonReader)} will resume
   *         from the position at which parsing was previously aborted.
   * @throws IOException If an I/O error has occurred.
   * @throws JsonParseException If the content is not a well formed JSON term.
   * @throws IllegalArgumentException If {@code reader} is null.
   */
  default boolean parse(final JsonReader reader) throws IOException, JsonParseException {
    assertNotNull(reader);
    boolean started = false;
    for (int off, depth = 0; (off = reader.readTokenStart()) != -1;) {
      final char ch = reader.buf()[off];
      final int len = reader.getPosition() - off;
      if (len == 1 && JsonUtil.isStructural(ch)) {
        if (ch == '{' || ch == '[') {
          ++depth;
        }
        else if ((ch == '}' || ch == ']') && --depth == 0) {
          if (!structural(ch))
            return false;

          endDocument();
          return true;
        }

        if (!started) {
          startDocument();
          started = true;
        }

        if (!structural(ch))
          return false;
      }
      else if (JsonUtil.isWhitespace(ch)) {
        if (started && !whitespace(reader.buf(), off, len))
          return false;
      }
      else if (!started) {
        startDocument();
        if (!characters(reader.buf(), off, len))
          return false;

        endDocument();
        return true;
      }
      else if (!characters(reader.buf(), off, len)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Called when the <u>document's start</u> is encountered.
   * <p>
   * The document's start is defined as the first non-whitespace character.
   */
  void startDocument();

  /**
   * Called when the <u>document's end</u> is encountered.
   * <p>
   * The document's end is defined as:
   * <ul>
   * <li>The last closing <code>}</code> character, if the document started with
   * <code>{</code> (i.e. the document is a JSON Object).</li>
   * <li>The last closing <code>]</code> character, if the document started with
   * <code>[</code> (i.e. the document is a JSON Array).</li>
   * <li>The last non-whitespace character, if the document represents a JSON
   * Value.</li>
   * </ul>
   */
  void endDocument();

  /**
   * Called when a <u>structural token</u> is encountered.
   * <p>
   * A structural token is one of:
   *
   * <pre>
   * <code>{ } [ ] : ,</code>
   * </pre>
   *
   * @param ch The structural token.
   * @return {@code true} to continue parsing, {@code false} to abort.
   */
  boolean structural(char ch);

  /**
   * Called when <u>token characters</u> are encountered.
   * <p>
   * Token characters are:
   * <ul>
   * <li>A property key:
   * <ul>
   * <li>A string that matches:
   *
   * <pre>
   * {@code ^".*"$}
   * </pre>
   *
   * </li>
   * </ul>
   * </li>
   * <li>A property or array value:
   * <ul>
   * <li>A string that matches:
   *
   * <pre>
   * {@code ^".*"$}
   * </pre>
   *
   * </li>
   * <li>A number that matches:
   *
   * <pre>
   * {@code ^-?(0|[1-9]\d*)(\.\d+)?([eE][+-]?([1-9]\d*))?$}
   * </pre>
   *
   * </li>
   * <li>A literal that matches:
   *
   * <pre>
   * {@code ^null|true|false$}
   * </pre>
   *
   * </li>
   * </ul>
   * </li>
   * </ul>
   *
   * @param chars A reference to the underlying {@code char[]} buffer.
   * @param start The start index of the token.
   * @param len The length of the token.
   * @return {@code true} to continue parsing, {@code false} to abort.
   * @implNote For token characters representing a string, all string-literal
   *           unicode ({@code "\u000A"}) and two-character ({@code "\n"})
   *           escape codes are replaced with their single-character unicode
   *           representations, as defined in
   *           <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627, Section
   *           2.5</a>.
   */
  boolean characters(char[] chars, int start, int len);

  /**
   * Called when <u>whitespace characters</u> are encountered after the call to
   * {@link #startDocument()} has occurred, and not after the call to
   * {@link #endDocument()} has occurred.
   * <p>
   * Whitespace characters match:
   *
   * <pre>
   * {@code ^[ \n\r\t]+$}
   * </pre>
   *
   * @param chars A reference to the underlying {@code char[]} buffer.
   * @param start The start index of the token.
   * @param len The length of the token.
   * @return {@code true} to continue parsing, {@code false} to abort.
   */
  boolean whitespace(char[] chars, int start, int len);
}