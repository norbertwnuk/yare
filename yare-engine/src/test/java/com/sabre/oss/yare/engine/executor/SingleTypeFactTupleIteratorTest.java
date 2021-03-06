/*
 * MIT License
 *
 * Copyright 2018 Sabre GLBL Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.sabre.oss.yare.engine.executor;

import org.junit.jupiter.api.Test;

import java.util.*;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SingleTypeFactTupleIteratorTest {

    @Test
    void shouldProperlyBehaveForNoData() {
        // given
        Map<String, List<Object>> facts = new HashMap<>();

        //when /then
        assertThatThrownBy(() -> new DefaultRulesExecutor.SingleTypeFactTupleIterator(facts))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldProperlyBehaveForEmptyGroup() {
        // given
        Map<String, List<Object>> facts = new HashMap<>();
        facts.put("a", Collections.emptyList());

        // when / then
        assertThatThrownBy(() -> new DefaultRulesExecutor.SingleTypeFactTupleIterator(facts))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldProperlyIterateThroughSingleFact() {
        // given
        Map<String, List<Object>> facts = new HashMap<>();
        facts.put("a", asList(new MyFact("A1"), new MyFact("A2"), new MyFact("A3")));

        DefaultRulesExecutor.SingleTypeFactTupleIterator iterator = new DefaultRulesExecutor.SingleTypeFactTupleIterator(facts);

        List<String> result = new ArrayList<>();

        // when
        while (iterator.hasNext()) {
            Map<String, Object> item = iterator.next();
            result.add(format("%s", item.get("a")));
        }

        // then
        assertThat(result).containsExactly(
                "A1",
                "A2",
                "A3"
        );
    }

    @Test
    void shouldProperlyIterateThroughGroupOfFacts() {
        // given
        Map<String, List<Object>> facts = new HashMap<>();
        facts.put("a", asList(new MyFact("A1"), new MyFact("A2"), new MyFact("A3")));
        facts.put("b", asList(new MyFact("B1"), new MyFact("B2")));
        facts.put("c", asList(new MyFact("C1")));
        facts.put("d", asList(new MyFact("D1"), new MyFact("D2")));

        //when /then
        assertThatThrownBy(() -> new DefaultRulesExecutor.SingleTypeFactTupleIterator(facts))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    private static class MyFact {
        private final String id;

        MyFact(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return id;
        }
    }
}
