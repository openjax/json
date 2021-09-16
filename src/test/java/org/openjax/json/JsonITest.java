/* Copyright (c) 2021 OpenJAX
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
import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.libj.test.JUnitUtil;
import org.libj.util.ArrayUtil;

@RunWith(Parameterized.class)
public class JsonITest extends AbstractTest {
  @Parameterized.Parameters(name = "{0}")
  public static URL[] resources() throws IOException {
    return ArrayUtil.subArray(JUnitUtil.sortBySize(JUnitUtil.getResources("", ".*\\.json")), 0, 32);
  }

  @Parameterized.Parameter(0)
  public URL resource;

  @Test
  public void testParse() throws IOException {
    final String expected = JSON.stripWhitespace(readFully(resource));
    final String actual = JSON.toString(JSON.parse(expected));
    assertEquals(expected, actual);
  }
}