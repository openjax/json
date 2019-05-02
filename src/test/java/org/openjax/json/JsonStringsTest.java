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

import org.junit.Test;

public class JsonStringsTest {
  @Test
  public void testEscape() {
    assertEquals("\\\\\\\"\\r\\t\\f\\b\\u001a\\u0006\\n", JsonStrings.escape("\\\"\r\t\f\b" + (char)0x1A + (char)0x06 + "\n").toString());
  }

  @Test
  public void testUnescape() {
    assertEquals("\\", JsonStrings.unescape("\\\\"));
    assertEquals("\"", JsonStrings.unescape("\\\""));
    assertEquals("\n", JsonStrings.unescape("\\n"));
    assertEquals("\r", JsonStrings.unescape("\\r"));
    assertEquals("\t", JsonStrings.unescape("\\t"));
    assertEquals("\f", JsonStrings.unescape("\\f"));
    assertEquals("\b", JsonStrings.unescape("\\b"));
    assertEquals("ĥ", JsonStrings.unescape("\\u0125"));
    assertEquals("Ħ", JsonStrings.unescape("\\u0126"));
    assertEquals("\\\"\r\t\f\bĥĦ\n", JsonStrings.unescape("\\\\\\\"\\r\\t\\f\\b\\u0125\\u0126\\n"));
  }

  @Test
  public void testUnescapeForString() {
    assertEquals("\\\\", JsonStrings.unescapeForString("\\\\"));
    assertEquals("\\\"", JsonStrings.unescapeForString("\\\""));
    assertEquals("\n", JsonStrings.unescapeForString("\\n"));
    assertEquals("\r", JsonStrings.unescapeForString("\\r"));
    assertEquals("\t", JsonStrings.unescapeForString("\\t"));
    assertEquals("\f", JsonStrings.unescapeForString("\\f"));
    assertEquals("\b", JsonStrings.unescapeForString("\\b"));
    assertEquals("ĥ", JsonStrings.unescapeForString("\\u0125"));
    assertEquals("Ħ", JsonStrings.unescapeForString("\\u0126"));
    assertEquals("\\\\\\\"\r\t\f\bĥĦ\n", JsonStrings.unescapeForString("\\\\\\\"\\r\\t\\f\\b\\u0125\\u0126\\n"));
  }
}