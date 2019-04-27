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

package org.openjax.standard.json;

/**
 * Handler interface used for parsing JSON with {@link JsonParser#parse(JsonHandler)}.
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
   * <pre><code>{ } [ ] : ,</code></pre>
   *
   * @param ch The structural token.
   * @return {@code true} to continue parsing, {@code false} to abort.
   */
  boolean structural(char ch);

  /**
   * Called when token characters are encountered. Token characters are:
   * <ul>
   * <li>A property key:<ul>
   * <li>A string that matches:<pre>^".*"$</pre></li></ul></li>
   * <li>A property or array member value:<ul>
   * <li>A string that matches:<pre>^".*"$</pre></li>
   * <li>A number that matches:<pre>{@code ^-?(([0-9])|([1-9][0-9]+))(\.[\.0-9]+)?([eE][+-]?(([0-9])|([1-9][0-9]+)))?$}</pre></li>
   * <li>A literal that matches:<pre>{@code ^(null)|(true)|(false)$}</pre></li></ul></li>
   * </ul>
   *
   * @param chars A reference to the underlying {@code char[]} buffer.
   * @param start The start index of the token.
   * @param end The end index of the token.
   * @return {@code true} to continue parsing, {@code false} to abort.
   */
  boolean characters(char[] chars, int start, int end);

  /**
   * Called when whitespace characters are encountered. Whitespace characters
   * match:
   *
   * <pre>{@code ^[ \n\r\t]+$}</pre>
   *
   * @param chars A reference to the underlying {@code char[]} buffer.
   * @param start The start index of the token.
   * @param end The end index of the token.
   * @return {@code true} to continue parsing, {@code false} to abort.
   */
  boolean whitespace(char[] chars, int start, int end);
}