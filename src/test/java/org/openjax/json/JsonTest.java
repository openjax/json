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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.junit.Test;

public class JsonTest {
  @Test
  @SuppressWarnings("unchecked")
  public void test() {
    final LinkedHashMap<String,Object> properties = new LinkedHashMap<>();
    properties.put("string", "str\ni\tn†\u4324g");
    properties.put("number", BigDecimal.valueOf(Math.PI));
    properties.put("boolean", false);
    properties.put("null", null);

    final ArrayList<Object> array = new ArrayList<>();
    array.add(true);
    array.add(Math.E);
    array.add("hello world");
    array.add(array.clone());
    array.add(properties.clone());

    properties.put("array", array);

    final LinkedHashMap<String,Object> o2 = (LinkedHashMap<String,Object>)properties.clone();
    properties.put("object", o2);

    System.out.println(JSON.toString(properties));
  }
}