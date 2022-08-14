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
 * An exception signaling a violation of JSON document well-formed criteria as expressed by the
 * <a href="https://www.ietf.org/rfc/rfc4627.txt">RFC 4627</a> specification.
 */
public class JsonParseException extends RuntimeException {
  /**
   * The zero-based character offset into the string being parsed at which the error was found during parsing.
   */
  private final int errorOffset;

  /**
   * Constructs a JsonParseException with the specified detail message and offset. A detail message is a String that describes this
   * particular exception.
   *
   * @param message The detail message.
   * @param errorOffset The position where the error is found while parsing.
   * @param cause The cause.
   */
  public JsonParseException(final String message, final int errorOffset, final Exception cause) {
    super(message + " [errorOffset: " + errorOffset + "]", cause);
    this.errorOffset = errorOffset;
  }

  /**
   * Constructs a JsonParseException with the specified detail message and offset. A detail message is a String that describes this
   * particular exception.
   *
   * @param message The detail message.
   * @param errorOffset The position where the error is found while parsing.
   */
  public JsonParseException(final String message, final int errorOffset) {
    this(message, errorOffset, null);
  }

  /**
   * Returns the position where the error was found.
   *
   * @return The position where the error was found.
   */
  public int getErrorOffset() {
    return errorOffset;
  }
}