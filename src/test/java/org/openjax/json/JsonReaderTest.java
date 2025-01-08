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

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.libj.io.Readers;
import org.libj.lang.Numbers;

public class JsonReaderTest extends AbstractTest {
  @Test
  public void testSetIndex() throws IOException {
    try (final JsonReader reader = new JsonReader("  [false, true]", true)) {
      // Starting index of the reader is -1, because no tokens have been read
      assertEquals(-1, reader.getIndex());
      assertEquals(0, reader.size());

      // First token is "[" (because whitespace is ignored), index=0
      assertEquals("[", tokenString(reader));
      // Assert the index has been advanced properly
      assertEquals(0, reader.getIndex());
      // The buffered size of the reader will be 2, because the extra token is
      // read ahead
      assertEquals(2, reader.size());

      try {
        reader.setIndex(2);
        fail("Expected IllegalArgumentException");
      }
      catch (final IllegalArgumentException e) {
      }

      // Move back to the beginning, to re-read the first token
      reader.setIndex(-1);
      // Read the first token with the Reader#read() method
      assertEquals('[', reader.read());
      // Assert the index has been advanced properly
      assertEquals(0, reader.getIndex());

      // Move back to the beginning, to re-read the first token
      reader.setIndex(-1);
      // Read the first token with the JsonReader#readToen() method
      assertEquals("[", tokenString(reader));
      // Assert the index has been advanced properly
      assertEquals(0, reader.getIndex());
    }
  }

  @Test
  public void testScopeEnd() throws IOException {
    assertFail("  {", JsonParseException.class, "Missing closing scope character: '}' [errorOffset: 3]");
    assertFail("[[]", JsonParseException.class, "Missing closing scope character: ']' [errorOffset: 3]");
  }

  @Test
  public void testScopeMiddle() throws IOException {
    assertFail("{[", JsonParseException.class, "Missing closing scope character: ']' [errorOffset: 2]");
    assertFail("[}", JsonParseException.class, "Expected character ']', but encountered '}' [errorOffset: 1]");
    assertFail("{]", JsonParseException.class, "Expected character '}', but encountered ']' [errorOffset: 1]");
    assertFail("{\"foo\":[[[]]}", JsonParseException.class, "Expected character ']', but encountered '}' [errorOffset: 12]");
    assertFail("[[[{]]", JsonParseException.class, "Expected character '}', but encountered ']' [errorOffset: 4]");
  }

  @Test
  public void testExpectedColon() throws IOException {
    assertFail("  {foo, bar}  ", JsonParseException.class, "Expected character '\"', but encountered 'f' [errorOffset: 3]");
    assertFail("{\"foo\", bar}", JsonParseException.class, "Expected character ':', but encountered ',' [errorOffset: 6]");
    assertFail("{\"foo\"{ bar}", JsonParseException.class, "Expected character ':', but encountered '{' [errorOffset: 6]");
    assertFail("{\"foo\"[ bar}", JsonParseException.class, "Expected character ':', but encountered '[' [errorOffset: 6]");
    assertFail("{\"foo\"} bar}", JsonParseException.class, "Expected character ':', but encountered '}' [errorOffset: 6]");
    assertFail("{\"foo\"] bar}", JsonParseException.class, "Expected character ':', but encountered ']' [errorOffset: 6]");
    assertFail("{\"foo\" bar }", JsonParseException.class, "Expected character ':', but encountered 'b' [errorOffset: 7]");
  }

  @Test
  public void testNoContentExpected() throws IOException {
    assertFail("  { foo : [   ]  }  ", JsonParseException.class, "Expected character '\"', but encountered 'f' [errorOffset: 4]");
    assertFail("{ \"foo\"  \"bar\" }", JsonParseException.class, "Expected character ':', but encountered '\"' [errorOffset: 9]");
    assertFail("{\"foo\": \"bar\"} f", JsonParseException.class, "No content is expected at this point: f [errorOffset: 15]");
  }

  @Test
  public void testTerm() throws IOException {
    assertPass("{\"foo\": null}");

    assertFail("{\"foo\": nulll}", JsonParseException.class, "Expected character '\"', but encountered 'l' [errorOffset: 12]");
    assertFail(".", JsonParseException.class, "Unexpected character: '.' [errorOffset: 0]");
    assertFail("x", JsonParseException.class, "Unexpected character: 'x' [errorOffset: 0]");
    assertFail("[x]", JsonParseException.class, "Unexpected character: 'x' [errorOffset: 1]");
    assertFail("[null: x]", JsonParseException.class, "Unexpected character: ':' [errorOffset: 5]");
    assertFail("[null, x]", JsonParseException.class, "Unexpected character: 'x' [errorOffset: 7]");
    assertFail("{\"foo\": xyz}", JsonParseException.class, "Unexpected character: 'x' [errorOffset: 8]");
  }

  @Test
  public void testString() throws IOException {
    assertPass("{\"foo\": [\"Z&$GY6-4si[1\"]}");
    assertPass("{\"foo\": [\"[&]$b|6)?f)A$\"]}");
    assertPass("{\"foo\": \"\"}");
    assertPass("{\"foo\": \"b\\\"ar\"}");
    assertPass("{\"foo\": \"\"}");
    assertPass("{\"foo\": \"ba\\\"r\"}");

    assertFail("{\"foo\": \"bar}", JsonParseException.class, "Unterminated string [errorOffset: 8]");
    assertFail("{\"foo\": 'bar'}", JsonParseException.class, "Unexpected character: ''' [errorOffset: 8]");
  }

  @Test
  public void testBoolean() throws IOException {
    assertPass("{\"foo\":  true}");
    assertPass("{\"foo\": false}");
    assertFail("{\"foo\": truee}", JsonParseException.class, "Expected character '\"', but encountered 'e' [errorOffset: 12]");
    assertFail("{\"foo\": falss}", JsonParseException.class, "Unexpected character: 's' [errorOffset: 12]");
  }

  @Test
  public void testNumber() throws IOException {
    assertPass("{\"foo\": -0}");
    assertPass("{\"foo\": 0}");
    assertPass("{\"foo\": 2931}");
    assertPass("{\"foo\": 2931.32}");
    assertPass("{\"foo\": 10e0}");
    assertPass("{\"foo\": 10E1}");
    assertPass("{\"foo\": 10e+12}");
    assertPass("{\"foo\": 10E-12}");

    assertFail("{\"foo\": -.5}", JsonParseException.class, "Integer component required before fraction part [errorOffset: 9]");
    assertFail("{\"foo\": 001}", JsonParseException.class, "Leading zeros are not allowed [errorOffset: 8]");
    assertFail("{\"foo\": 0.}", JsonParseException.class, "Decimal point must be followed by one or more digits [errorOffset: 10]");
    assertFail("{\"foo\": 0.0.}", JsonParseException.class, "Unexpected character: '.' [errorOffset: 11]");
    assertFail("{\"foo\": --0}", JsonParseException.class, "Unexpected character: '-' [errorOffset: 9]");
    assertFail("{\"foo\": 10E-}", JsonParseException.class, "Expected digit, but encountered '}' [errorOffset: 12]");
    assertFail("{\"foo\": 10E+}", JsonParseException.class, "Expected digit, but encountered '}' [errorOffset: 12]");
    assertFail("{\"foo\": 10E--}", JsonParseException.class, "Unexpected character: '-' [errorOffset: 12]");
    assertFail("{\"foo\": 10E++}", JsonParseException.class, "Unexpected character: '+' [errorOffset: 12]");
    assertFail("{\"foo\": 10E+1.}", JsonParseException.class, "Unexpected character: '.' [errorOffset: 13]");
    assertFail("{\"foo\": 10E01}", JsonParseException.class, "Leading zeros are not allowed [errorOffset: 11]");
    assertFail("{\"foo\": 10E+01}", JsonParseException.class, "Leading zeros are not allowed [errorOffset: 12]");
  }

  private static String tokenString(final JsonReader reader) throws JsonParseException, IOException {
    final long token = reader.readToken();
    final int off = Numbers.Composite.decodeInt(token, 0);
    final int len = Numbers.Composite.decodeInt(token, 1);
    return new String(reader.buf(), off, len);
  }

  @Test
  public void testMarkReset() throws IOException {
    final String json = "{\"a\": \"$\", \"b\": 5, \"c\": false, \"d\": [], \"e\": {}}";
    final JsonReader reader = new JsonReader(json);
    for (int i = 0; i < 2; ++i) // [N]
      logger.debug(tokenString(reader));

    reader.mark(-1);
    for (int i = 0; i < 10; ++i, reader.reset()) { // [N]
      tokenString(reader);
      assertEquals("\"$\"", tokenString(reader));
    }

    for (int i = 0; i < 3; ++i) // [N]
      logger.debug(tokenString(reader));

    logger.debug(String.valueOf((char)reader.read()));
    reader.mark(-1);
    for (int i = 0; i < 10; ++i, reader.reset()) { // [N]
      assertEquals("b\"", tokenString(reader));
      assertEquals(":", tokenString(reader));
      assertEquals("5", tokenString(reader));
    }

    for (int i = 0; i < 6; ++i) // [N]
      logger.debug(tokenString(reader));

    for (int i = 0; i < 3; ++i) // [N]
      logger.debug(String.valueOf((char)reader.read()));

    reader.mark(-1);
    for (int i = 0; i < 10; ++i, reader.reset()) { // [N]
      assertEquals("se", tokenString(reader));
      assertEquals(",", tokenString(reader));
      assertEquals("\"d\"", tokenString(reader));
      Readers.readFully(reader);
    }
  }

  @Test
  public void testBlank() throws IOException {
    assertPass("");
  }

  @Test
  public void testEmpty() throws IOException {
    assertPass("{}");
    assertPass("[]");
  }
}