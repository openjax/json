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

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Test;
import org.libj.lang.Strings;

public class JsonTypesTest {
  private static final char[] whitespace = {' ', '\n', '\r', '\t'};

  @Test
  public void testIsWhitespace() {
    for (int i = 0, i$ = whitespace.length; i < i$; ++i) // [A]
      assertTrue(JsonUtil.isWhitespace(whitespace[i]));

    for (int i = 0; i < 100; ++i) // [N]
      assertFalse(JsonUtil.isWhitespace(Strings.getRandomAlpha(1).charAt(0)));
  }

  private static void testPass(final String number, final Class<? extends Number> type) {
    JsonUtil.parseNumber(type, number, true);
  }

  private static void testFail(final Class<? extends Exception> cls, final String number) {
    try {
      JsonUtil.parseNumber(BigDecimal.class, number, true);
      fail("Expected " + cls.getSimpleName());
    }
    catch (final Exception e) {
      if (!cls.isInstance(e))
        throw e;
    }
  }

  @Test
  public void testParseNumberFail() {
    testFail(NullPointerException.class, null);
    testFail(IllegalArgumentException.class, "");
    testFail(JsonParseException.class, "a");
    testFail(JsonParseException.class, ".1");
    testFail(JsonParseException.class, "-.1");
    testFail(JsonParseException.class, "0..");
    testFail(JsonParseException.class, "00");
    testFail(JsonParseException.class, "01");
    testFail(JsonParseException.class, "1.");
    testFail(JsonParseException.class, "1.E");
    testFail(JsonParseException.class, "1.0E");
    testFail(JsonParseException.class, "1.0E--");
    testFail(JsonParseException.class, "1.0E-+");
    testFail(JsonParseException.class, "1.0E++");
    testFail(JsonParseException.class, "1.0E+-");
    testFail(JsonParseException.class, "1.0E01");
    testFail(JsonParseException.class, "1.0E-01");
    testFail(JsonParseException.class, "1.0E+01");
    testFail(JsonParseException.class, "1.0E+1.");
    testPass("0e0", double.class);
  }

  private static String randomNeg(final boolean plusOk) {
    final double random = Math.random();
    if (plusOk)
      return random < 0.3333 ? "" : random < 0.6666 ? "-" : "+";

    return random < 0.5 ? "" : "-";
  }

  private static String randomInt(final int len) {
    String string = Strings.getRandomNumeric((int)(Math.random() * (len - 1)) + 1);
    int i = 0;
    for (; i < string.length(); ++i) // [N]
      if (string.charAt(i) != '0')
        break;

    string = string.substring(i);
    return string.length() > 0 ? string : randomInt(len);
  }

  private static String randomExp() {
    return Math.random() < 0.5 ? "e" : "E";
  }

  @Test
  public void testParseNumberInteger() {
    for (int i = 0; i < 1000; ++i) // [N]
      testPass(randomNeg(false) + randomInt(100), BigInteger.class);
  }

  @Test
  public void testParseNumberIntegerExp() {
    for (int i = 0; i < 1000; ++i) // [N]
      testPass(randomNeg(false) + randomInt(100) + randomExp() + (Math.random() < 0.5 ? "" : "+") + randomInt(5), BigInteger.class);
  }

  @Test
  public void testParseNumberDecimal() {
    for (int i = 0; i < 1000; ++i) // [N]
      testPass(randomNeg(false) + randomInt(100) + "." + randomInt(100), double.class);
  }

  @Test
  public void testParseNumberDecimalExp() {
    for (int i = 0; i < 1000; ++i) // [N]
      testPass(randomNeg(false) + randomInt(100) + "." + randomInt(100) + randomExp() + randomNeg(true) + randomInt(5), float.class);
  }

  @Test
  public void testEscape() {
    assertEquals("\\\\\\\"\\r\\t\\f\\b\\u001a\\u0006\\n", JsonUtil.escape("\\\"\r\t\f\b" + (char)0x1A + (char)0x06 + "\n").toString());
  }

  @Test
  public void testUnescape() {
    assertEquals("\\", JsonUtil.unescape("\\\\").toString());
    assertEquals("\"", JsonUtil.unescape("\\\"").toString());
    assertEquals("\n", JsonUtil.unescape("\\n").toString());
    assertEquals("\r", JsonUtil.unescape("\\r").toString());
    assertEquals("\t", JsonUtil.unescape("\\t").toString());
    assertEquals("\f", JsonUtil.unescape("\\f").toString());
    assertEquals("\b", JsonUtil.unescape("\\b").toString());
    assertEquals("ĥ", JsonUtil.unescape("\\u0125").toString());
    assertEquals("Ħ", JsonUtil.unescape("\\u0126").toString());
    assertEquals("\\\"\r\t\f\bĥĦ\n", JsonUtil.unescape("\\\\\\\"\\r\\t\\f\\b\\u0125\\u0126\\n").toString());
  }

  @Test
  public void testUnescapeForString() {
    assertEquals("\\\\", JsonUtil.unescapeForString("\\\\").toString());
    assertEquals("\\\"", JsonUtil.unescapeForString("\\\"").toString());
    assertEquals("\n", JsonUtil.unescapeForString("\\n").toString());
    assertEquals("\r", JsonUtil.unescapeForString("\\r").toString());
    assertEquals("\t", JsonUtil.unescapeForString("\\t").toString());
    assertEquals("\f", JsonUtil.unescapeForString("\\f").toString());
    assertEquals("\b", JsonUtil.unescapeForString("\\b").toString());
    assertEquals("ĥ", JsonUtil.unescapeForString("\\u0125").toString());
    assertEquals("Ħ", JsonUtil.unescapeForString("\\u0126").toString());
    assertEquals("\\\\\\\"\r\t\f\bĥĦ\n", JsonUtil.unescapeForString("\\\\\\\"\\r\\t\\f\\b\\u0125\\u0126\\n").toString());
  }
}