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
import java.io.StringReader;

import org.junit.Test;
import org.libj.io.Readers;

public class JsonReaderTest extends AbstractTest {
  @Test
  public void testSetIndex() throws IOException {
    try (final JsonReader reader = new JsonReader(new StringReader("  [false, true]"), true)) {
      // Starting index of the reader is -1, because no tokens have been read
      assertEquals(-1, reader.getIndex());
      assertEquals(0, reader.size());

      // First token is "[" (because whitespace is ignored), index=0
      assertEquals("[", reader.readToken());
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
      assertEquals("[", reader.readToken());
      // Assert the index has been advanced properly
      assertEquals(0, reader.getIndex());
    }
  }

  @Test
  public void testScopeEnd() throws IOException {
    failString("  {", JsonParseException.class, "Missing closing scope character: '}' [errorOffset: 3]");
    failString("[[]", JsonParseException.class, "Missing closing scope character: ']' [errorOffset: 3]");
  }

  @Test
  public void testScopeMiddle() throws IOException {
    failString("{[", JsonParseException.class, "Missing closing scope character: ']' [errorOffset: 2]");
    failString("[}", JsonParseException.class, "Expected character ']', but encountered '}' [errorOffset: 1]");
    failString("{]", JsonParseException.class, "Expected character '}', but encountered ']' [errorOffset: 1]");
    failString("{\"foo\":[[[]]}", JsonParseException.class, "Expected character ']', but encountered '}' [errorOffset: 12]");
    failString("[[[{]]", JsonParseException.class, "Expected character '}', but encountered ']' [errorOffset: 4]");
  }

  @Test
  public void testExpectedColon() throws IOException {
    failString("  {foo, bar}  ", JsonParseException.class, "Expected character '\"', but encountered 'f' [errorOffset: 3]");
    failString("{\"foo\", bar}", JsonParseException.class, "Expected character ':', but encountered ',' [errorOffset: 6]");
    failString("{\"foo\"{ bar}", JsonParseException.class, "Expected character ':', but encountered '{' [errorOffset: 6]");
    failString("{\"foo\"[ bar}", JsonParseException.class, "Expected character ':', but encountered '[' [errorOffset: 6]");
    failString("{\"foo\"} bar}", JsonParseException.class, "Expected character ':', but encountered '}' [errorOffset: 6]");
    failString("{\"foo\"] bar}", JsonParseException.class, "Expected character ':', but encountered ']' [errorOffset: 6]");
    failString("{\"foo\" bar }", JsonParseException.class, "Expected character ':', but encountered 'b' [errorOffset: 7]");
  }

  @Test
  public void testNoContentExpected() throws IOException {
    failString("  { foo : [   ]  }  ", JsonParseException.class, "Expected character '\"', but encountered 'f' [errorOffset: 4]");
    failString("{ \"foo\"  \"bar\" }", JsonParseException.class, "Expected character ':', but encountered '\"' [errorOffset: 9]");
    failString("{\"foo\": \"bar\"} f", JsonParseException.class, "No content is expected at this point: f [errorOffset: 15]");
  }

  @Test
  public void testTerm() throws IOException {
    passString("{\"foo\": null}");

    failString("{\"foo\": nulll}", JsonParseException.class, "Illegal character: 'l' [errorOffset: 12]");
    failString(".", JsonParseException.class, "Expected character '{' or '[', but encountered '.' [errorOffset: 0]");
    failString("x", JsonParseException.class, "Expected character '{' or '[', but encountered 'x' [errorOffset: 0]");
    failString("[x]", JsonParseException.class, "Illegal character: 'x' [errorOffset: 1]");
    failString("[null: x]", JsonParseException.class, "Illegal character: ':' [errorOffset: 5]");
    failString("[null, x]", JsonParseException.class, "Illegal character: 'x' [errorOffset: 7]");
    failString("{\"foo\": xyz}", JsonParseException.class, "Illegal character: 'x' [errorOffset: 8]");
  }

  @Test
  public void testString() throws IOException {
    passString("{\"foo\": \"\"}");
    passString("{\"foo\": \"\"}");
    passString("{\"foo\": \"b\\\"ar\"}");
    passString("{\"foo\": \"\"}");
    passString("{\"foo\": \"ba\\\"r\"}");

    failString("{\"foo\": \"bar}", JsonParseException.class, "Unterminated string [errorOffset: 8]");
    failString("{\"foo\": 'bar'}", JsonParseException.class, "Illegal character: ''' [errorOffset: 8]");
  }

  @Test
  public void testBoolean() throws IOException {
    passString("{\"foo\":  true}");
    passString("{\"foo\": false}");
    failString("{\"foo\": truee}", JsonParseException.class, "Illegal character: 'e' [errorOffset: 12]");
    failString("{\"foo\": falss}", JsonParseException.class, "Illegal character: 's' [errorOffset: 12]");
  }

  @Test
  public void testNumber() throws IOException {
    passString("{\"foo\": -0}");
    passString("{\"foo\": 0}");
    passString("{\"foo\": 2931}");
    passString("{\"foo\": 2931.32}");
    passString("{\"foo\": 10e0}");
    passString("{\"foo\": 10E1}");
    passString("{\"foo\": 10e+12}");
    passString("{\"foo\": 10E-12}");

    failString("{\"foo\": -.5}", JsonParseException.class, "Integer component required before fraction part [errorOffset: 9]");
    failString("{\"foo\": 001}", JsonParseException.class, "Leading zeros are not allowed [errorOffset: 8]");
    failString("{\"foo\": 0.}", JsonParseException.class, "Decimal point must be followed by one or more digits [errorOffset: 10]");
    failString("{\"foo\": 0.0.}", JsonParseException.class, "Illegal character: '.' [errorOffset: 11]");
    failString("{\"foo\": --0}", JsonParseException.class, "Illegal character: '-' [errorOffset: 9]");
    failString("{\"foo\": 10E-}", JsonParseException.class, "Expected digit, but encountered '}' [errorOffset: 12]");
    failString("{\"foo\": 10E+}", JsonParseException.class, "Expected digit, but encountered '}' [errorOffset: 12]");
    failString("{\"foo\": 10E--}", JsonParseException.class, "Illegal character: '-' [errorOffset: 12]");
    failString("{\"foo\": 10E++}", JsonParseException.class, "Illegal character: '+' [errorOffset: 12]");
    failString("{\"foo\": 10E+1.}", JsonParseException.class, "Illegal character: '.' [errorOffset: 13]");
    failString("{\"foo\": 10E01}", JsonParseException.class, "Leading zeros are not allowed [errorOffset: 11]");
    failString("{\"foo\": 10E+01}", JsonParseException.class, "Leading zeros are not allowed [errorOffset: 12]");
  }

  @Test
  public void testMarkReset() throws IOException {
    final String json = "{\"a\": \"$\", \"b\": 5, \"c\": false, \"d\": [], \"e\": {}}";
    final JsonReader reader = new JsonReader(new StringReader(json));
    for (int i = 0; i < 2; ++i)
      logger.debug(reader.readToken());

    reader.mark(-1);
    for (int i = 0; i < 10; ++i, reader.reset()) {
      reader.readToken();
      assertEquals("\"$\"", reader.readToken());
    }

    for (int i = 0; i < 3; ++i)
      logger.debug(reader.readToken());

    logger.debug(String.valueOf((char)reader.read()));
    reader.mark(-1);
    for (int i = 0; i < 10; ++i, reader.reset()) {
      assertEquals("b\"", reader.readToken());
      assertEquals(":", reader.readToken());
      assertEquals("5", reader.readToken());
    }

    for (int i = 0; i < 6; ++i)
      logger.debug(reader.readToken());

    for (int i = 0; i < 3; ++i)
      logger.debug(String.valueOf((char)reader.read()));

    reader.mark(-1);
    for (int i = 0; i < 10; ++i, reader.reset()) {
      assertEquals("se", reader.readToken());
      assertEquals(",", reader.readToken());
      assertEquals("\"d\"", reader.readToken());
      Readers.readFully(reader);
    }
  }

  @Test
  public void testBlank() throws IOException {
    passString("");
  }

  @Test
  public void testEmpty() throws IOException {
    passString("{}");
    passString("[]");
  }
}