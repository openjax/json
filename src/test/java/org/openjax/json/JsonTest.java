/* Copyright (c) 2019 OpenJAX
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.junit.Test;

public class JsonTest {
  @Test
  @SuppressWarnings("unchecked")
  public void test() throws IOException, JsonParseException {
    final LinkedHashMap<String,Object> object = new LinkedHashMap<>();
    object.put("string", "str\ni\tn†\u4324g");
    object.put("number", BigDecimal.valueOf(Math.PI));
    object.put("boolean", false);
    object.put("null", null);

    final ArrayList<Object> array = new ArrayList<>();
    array.add(true);
    array.add(Math.E);
    array.add("hello world");
    array.add(array.clone());
    array.add(object.clone());

    object.put("array", array);

    final LinkedHashMap<String,Object> clone = (LinkedHashMap<String,Object>)object.clone();
    object.put("object", clone);

    for (int i = 0; i < 100; ++i) { // [N]
      final String json = JSON.toString(object, i);
      // System.err.println(json + "\n");
      assertEquals(json, JSON.toString(JSON.parse(json), i));
    }
  }
}