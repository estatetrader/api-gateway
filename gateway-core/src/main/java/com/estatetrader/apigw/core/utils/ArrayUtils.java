package com.estatetrader.apigw.core.utils;

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * ArrayUtils contains some methods that you can call to find out the most efficient increments by which to grow arrays.
 */
public class ArrayUtils {
    private ArrayUtils() { /* cannot be instantiated */}

    public static <T> T[] mergeElement(T[] array1, T[] array2, BiPredicate<T, T> elementMatcher, BiFunction<T, T, T> merger) {
        if (array1.length < array2.length) {
            T[] ts = array1;
            array1 = array2;
            array2 = ts;
        }
        List<T> list = null;
        for (T b : array2) {
            T a = null;
            for (T x : array1) {
                if (elementMatcher.test(x, b)) {
                    a = x;
                    break;
                }
            }
            if (a == null) {
                if (list == null) {
                    list = new ArrayList<>(Arrays.asList(array1));
                }
                list.add(b);
            } else {
                T m = merger.apply(a, b);
                if (!Objects.equals(a, m)) {
                    if (list == null) {
                        list = new ArrayList<>(Arrays.asList(array1));
                    }
                    for (int i = 0; i < list.size(); i++) {
                        if (Objects.equals(list.get(i), a)) {
                            list.set(i, m);
                        }
                    }
                }
            }
        }
        return list == null ? array1 : list.toArray(Arrays.copyOf(array1, list.size()));
    }
}
