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
import org.libj.io.UnsynchronizedStringReader;

public class JsonReplayReaderTest {
  @Test
  public void test() throws IOException {
    final String expected = "{\"foo\":\"a\nb\rc\\\"d\\\\e\bf\bgみどりいろ\"}";
    try (final JsonReplayReader reader = new JsonReplayReader(new UnsynchronizedStringReader("{\"foo\":\"a\\nb\\rc\\\"d\\\\e\\bf\\bg\\u307f\\u3069\\u308a\\u3044\\u308d\"}"))) {
      reader.mark(0);
      assertEquals(expected, Readers.readFully(reader));
      reader.reset();
      assertEquals(expected, Readers.readFully(reader));
    }
  }
}