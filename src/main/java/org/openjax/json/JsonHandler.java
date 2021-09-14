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

/**
 * Handler interface used for parsing JSON with
 * {@link JsonParser#parse(JsonReader,JsonHandler)}.
 */
public interface JsonHandler {
  /**
   * Called when the document's start is encountered.
   */
  void startDocument();

  /**
   * Called when the document's end is encountered.
   */
  void endDocument();

  /**
   * Called when a structural token is encountered. A structural token is one
   * of:
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
   * Called when token characters are encountered. Token characters are:
   * <ul>
   * <li>A property key:<ul>
   * <li>A string that matches:<pre>{@code ^".*"$}</pre></li></ul></li>
   * <li>A property or array member value:<ul>
   * <li>A string that matches:<pre>{@code ^".*"$}</pre></li>
   * <li>A number that matches:<pre>{@code ^-?(0|[1-9]\d*)(\.\d+)?([eE][+-]?([1-9]\d*))?$}</pre></li>
   * <li>A literal that matches:<pre>{@code ^null|true|false$}</pre></li></ul></li>
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
   * Called when whitespace characters are encountered. Whitespace characters
   * match:
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