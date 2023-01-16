/* Copyright (c) 2020 OpenJAX
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.libj.io.Readers;
import org.libj.io.UnicodeReader;
import org.libj.lang.Numbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

public abstract class AbstractTest {
  static final Logger logger = LoggerFactory.getLogger(AbstractTest.class);

  private static final boolean testIterator = true;
  private static final boolean testReadBack = true;
  private static final boolean testReadChar = true;
  private static final boolean testReadBuff = true;

  private static Double random = null;

  private static double random() {
    return random != null ? random : Math.random();
  }

  private static String compact(final String json) throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final JsonFactory factory = new JsonFactory();
    final JsonParser parser = factory.createParser(json);
    try (final JsonGenerator generator = factory.createGenerator(out)) {
      while (parser.nextToken() != null)
        generator.copyCurrentEvent(parser);
    }

    return new String(out.toByteArray());
  }

  private static void readRemainder(final JsonReader reader, final StringBuilder builder) throws IOException {
    final long token = reader.readToken();
    if (token != -1) {
      final String remainder = new String(reader.buf(), Numbers.Composite.decodeInt(token, 0), Numbers.Composite.decodeInt(token, 1));
      builder.append(remainder);
    }
  }

  private static void readBuff(final int length, final JsonReader reader, final StringBuilder builder) throws IOException {
    final char[] cbuf = new char[length];
    final int start = (int)(random() * cbuf.length * .5);
    final int end = start + (int)(random() * cbuf.length * .5);
    final int read = reader.read(cbuf, start, end - start);
    if (read > 0)
      builder.append(cbuf, start, read);
  }

  private static void testString(final String json, final boolean testSetIndex) throws IOException {
    testString(json, testSetIndex, false);
    testString(json, testSetIndex, true);
  }

  private static void testString(final String json, final boolean testSetIndex, final boolean ignoreWhitespace) throws IOException {
    final String unescaped = JsonUtil.unescapeForString(json).toString();
    try (final JsonReader reader = new JsonReader(json, ignoreWhitespace)) {
      final StringBuilder builder = new StringBuilder();
      final int gap = (int)(json.length() / 100d);
      int cut = 0;
      int index = 0;
      // Test that reader.readToken() and iterator.next() are properly synced
      if (testReadChar) {
        for (int i = 0, ch; i < random() * 10 && (ch = reader.read()) != -1; ++i) // [N]
          builder.append((char)ch);

        if (!testReadBuff)
          readRemainder(reader, builder);
      }

      if (testReadBuff) {
        for (int i = 0; i < random() * 10; ++i) // [N]
          readBuff((int)(random() * 20), reader, builder);

        readRemainder(reader, builder);
      }

      index = reader.getIndex() + 1;
      for (long token = -1; (token = !testIterator || random() < .5 ? reader.readToken() : reader.iterator().hasNext() ? reader.iterator().next() : -1) != -1;) { // [N]
        final int off = Numbers.Composite.decodeInt(token, 0);
        final int len = Numbers.Composite.decodeInt(token, 1);
        if (len == 0)
          fail("Index: " + reader.getIndex() + ", Position: " + reader.getPosition() + ": length == 0");

        final String str = new String(reader.buf(), off, len);
        if (ignoreWhitespace || !str.matches("\\s+"))
          assertEquals("ignoreWhitespace: " + ignoreWhitespace + ", Index: " + reader.getIndex() + ", Position: " + reader.getPosition(), str.trim(), str);

        if (testIterator)
          for (int i = 0; i < (int)(Math.random() * 10); ++i) // [N]
            reader.iterator().hasNext();

        if (index == reader.getIndex()) {
          builder.append(str);
          if (testReadChar && random() < .5) {
            for (int i = 0, ch; i < random() * 20 && (ch = reader.read()) != -1; ++i) // [N]
              builder.append((char)ch);

            if (!testReadBuff)
              readRemainder(reader, builder);
          }

          if (testReadBuff) {
            for (int i = 0; i < random() * 10; ++i) // [N]
              readBuff((int)(random() * 20), reader, builder);

            readRemainder(reader, builder);
          }

          index = reader.getIndex() + 1;
          if (testReadBack && testSetIndex && reader.getPosition() > gap * cut)
            reader.setIndex((int)(reader.getIndex() - random() * reader.getIndex() / ++cut));
        }
        else {
          // If the content is being re-read, ensure that the token is equal to
          // what was read previously
          final String expected = unescaped.substring(reader.getPosition() - len, reader.getPosition());
          assertEquals("ignoreWhitespace: " + ignoreWhitespace + ", Index: " + reader.getIndex() + ", Position: " + reader.getPosition(), expected, str);
        }
      }

      final String expected = ignoreWhitespace ? JsonUtil.unescapeForString(compact(json.trim())).toString() : unescaped.trim();
      assertEquals("ignoreWhitespace: " + ignoreWhitespace, expected, builder.toString());
    }
  }

  static String readFully(final URL resource) throws IOException {
    try (final InputStream in = resource.openStream()) {
      return Readers.readFully(new UnicodeReader(in));
    }
  }

  void assertPass(final URL resource) throws IOException {
    testString(readFully(resource), true);
  }

  static void assertPass(final String json) throws IOException {
    testString(json, false, false);
  }

  static void assertFail(final String json, final Class<? extends Exception> cls, final String message) throws IOException {
    try {
      testString(json, false, false);
      fail("Expected " + cls.getSimpleName());
    }
    catch (final IOException e) {
      throw e;
    }
    catch (final Exception e) {
      assertSame(cls, e.getClass());
      assertEquals(message, e.getMessage());
    }
  }
}