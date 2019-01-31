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

package com.sabre.oss.yare.example;

import com.google.common.collect.ImmutableList;
import com.sabre.oss.yare.core.RuleSession;
import com.sabre.oss.yare.core.RulesEngine;
import com.sabre.oss.yare.core.RulesEngineBuilder;
import com.sabre.oss.yare.core.model.Rule;
import com.sabre.oss.yare.dsl.RuleDsl;
import com.sabre.oss.yare.engine.executor.DefaultRulesExecutorBuilder;
import org.immutables.value.Value;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static com.sabre.oss.yare.dsl.RuleDsl.and;
import static com.sabre.oss.yare.dsl.RuleDsl.castToCollection;
import static com.sabre.oss.yare.dsl.RuleDsl.contains;
import static com.sabre.oss.yare.dsl.RuleDsl.containsAny;
import static com.sabre.oss.yare.dsl.RuleDsl.equal;
import static com.sabre.oss.yare.dsl.RuleDsl.lessOrEqual;
import static com.sabre.oss.yare.dsl.RuleDsl.not;
import static com.sabre.oss.yare.dsl.RuleDsl.or;
import static com.sabre.oss.yare.dsl.RuleDsl.param;
import static com.sabre.oss.yare.dsl.RuleDsl.value;
import static com.sabre.oss.yare.dsl.RuleDsl.values;
import static com.sabre.oss.yare.invoker.java.MethodCallMetadata.method;
import static org.assertj.core.api.Assertions.assertThat;

public class AirlineMatchingTest {

    @Value.Immutable
    public static abstract class Itinerary {
        public abstract String getPassenger();

        public abstract List<ItineraryPart> getItineraryParts();
    }

    @Value.Immutable
    public static abstract class ItineraryPart {
        public abstract List<Flight> getFlights();

        public int getDistance() {
            return getFlights().stream().flatMapToInt(f -> IntStream.of(f.getDistance())).sum();
        }
    }

    @Value.Immutable
    public static abstract class Flight {
        public abstract String getCarrier();

        public abstract int getDepartureTime();

        public abstract int getDistance();
    }

    @Test
    void sample01() {
        // given
        List<Rule> rules = Collections.singletonList(RuleDsl.ruleBuilder()
                .name("Sample01")
                .fact("itinerary", ImmutableItinerary.class)
                .fact("itineraryPart", ImmutableItineraryPart.class)
                .fact("flight", ImmutableFlight.class)
                .predicate(
                        or(
                                and(
                                        equal(
                                                value("${itinerary.passenger}"),
                                                value("P1")
                                        ),
                                        lessOrEqual(
                                                value("${itineraryPart.distance}"),
                                                value(300)
                                        ),
                                        equal(
                                                value("${flight.carrier}"),
                                                value("A")
                                        )
                                ),
                                equal(
                                        value("${flight.departureTime}"),
                                        value(5)
                                )
                        )
                )
                .action("collect",
                        param("context", value("${ctx}")),
                        param("fact", value("${flight}")))
                .build());

        List<Itinerary> itineraries = Arrays.asList(
                ImmutableItinerary.builder().passenger("P1").addItineraryParts(
                        ImmutableItineraryPart.builder().addFlights(
                                ImmutableFlight.builder().carrier("A").departureTime(1).distance(100).build(),
                                ImmutableFlight.builder().carrier("B").departureTime(2).distance(200).build()
                        ).build(),
                        ImmutableItineraryPart.builder().addFlights(
                                ImmutableFlight.builder().carrier("C").departureTime(3).distance(300).build(),
                                ImmutableFlight.builder().carrier("D").departureTime(4).distance(400).build()
                        ).build()
                ).build(),
                ImmutableItinerary.builder().passenger("P2").addItineraryParts(
                        ImmutableItineraryPart.builder().addFlights(
                                ImmutableFlight.builder().carrier("E").departureTime(5).distance(500).build(),
                                ImmutableFlight.builder().carrier("F").departureTime(6).distance(600).build()
                        ).build()
                ).build());

        RulesEngine engine = new RulesEngineBuilder()
                .withRulesRepository(i -> rules)
                .withActionMapping("collect", method(new Actions(), (action) -> action.collect(null, null)))
                .build();
        RuleSession session = engine.createSession("test");
        List<Flight> matching = new ArrayList<>();

        itineraries.forEach(i -> {
            i.getItineraryParts().forEach(ip -> {
                ip.getFlights().forEach(f -> {
                    session.execute(matching, ImmutableList.of(i, ip, f));
                });
            });
        });

        // then
        assertThat(matching).containsExactly(
                itineraries.get(0).getItineraryParts().get(0).getFlights().get(0),
                itineraries.get(1).getItineraryParts().get(0).getFlights().get(0)
        );
    }

    @Test
    void shouldMatchWithSubsetOfAirlineCodes() {
        // given
        List<Rule> rules = Collections.singletonList(RuleDsl.ruleBuilder()
                .name("Should match airline when airline codes contain given")
                .fact("airline", Airline.class)
                .predicate(
                        contains(
                                castToCollection(value("${airline.airlineCodes}"), String.class),
                                values(String.class, "AAU", "AAV")
                        )
                )
                .action("collect",
                        param("context", value("${ctx}")),
                        param("fact", value("${airline}")))
                .build());
        List<Airline> facts = Arrays.asList(
                new Airline().withAirlineCodes(
                        Arrays.asList("AAU", "AAW", "AAV", "AFU")
                ),
                new Airline().withAirlineCodes(
                        Collections.singletonList("AAU")
                ),
                new Airline().withAirlineCodes(
                        Arrays.asList("PIU", "BRO")
                ));

        RulesEngine engine = new RulesEngineBuilder()
                .withRulesRepository(i -> rules)
                .withActionMapping("collect", method(new Actions(), (action) -> action.collect(null, null)))
                .build();
        RuleSession session = engine.createSession("airlines");

        // when
        List<Airline> matching = session.execute(new ArrayList<>(), facts);

        // then
        assertThat(matching).containsExactly(
                new Airline().withAirlineCodes(Arrays.asList("AAU", "AAW", "AAV", "AFU"))
        );
    }

    @Test
    void shouldMatchWhenContainingAnyFromSet() {
        //given
        List<Rule> rules = Collections.singletonList(
                RuleDsl.ruleBuilder()
                        .name("Should match airline when airline codes contain any of given")
                        .fact("airline", Airline.class)
                        .predicate(
                                containsAny(
                                        castToCollection(value("${airline.airlineCodes}"), String.class),
                                        values(String.class, "AAU", "PIU", "BRO")
                                )
                        )
                        .action("collect",
                                param("context", value("${ctx}")),
                                param("fact", value("${airline}")))
                        .build()
        );
        List<Airline> facts = Arrays.asList(
                new Airline().withAirlineCodes(
                        Arrays.asList("AAU", "AAW", "AAV", "AFU")
                ),
                new Airline().withAirlineCodes(
                        Collections.singletonList("AAU")
                ),
                new Airline().withAirlineCodes(
                        Arrays.asList("AAW", "AAV")
                ));

        RulesEngine engine = new RulesEngineBuilder()
                .withRulesRepository(i -> rules)
                .withActionMapping("collect", method(new Actions(), (action) -> action.collect(null, null)))
                .build();
        RuleSession session = engine.createSession("airlines");

        // when
        List<Airline> matching = session.execute(new ArrayList<>(), facts);

        // then
        assertThat(matching).containsExactly(
                new Airline().withAirlineCodes(Arrays.asList("AAU", "AAW", "AAV", "AFU")),
                new Airline().withAirlineCodes(Collections.singletonList("AAU"))
        );
    }

    @Test
    void shouldMatchNotRejectedAirlines() {
        //given
        List<Rule> rules = Arrays.asList(
                RuleDsl.ruleBuilder()
                        .name("Should mark airline as rejected when its name is equal to given")
                        .fact("airline", Airline.class)
                        .predicate(
                                equal(
                                        value("${airline.name}"),
                                        value("Lufthansa")
                                )
                        )
                        .action("setRejectedFlag",
                                param("airline", value("${airline}"))
                        )
                        .build(),
                RuleDsl.ruleBuilder()
                        .name("Should match not rejected airlines")
                        .fact("airline", Airline.class)
                        .predicate(
                                not(value("${airline.isRejected}"))
                        )
                        .action("collect",
                                param("context", value("${ctx}")),
                                param("airline", value("${airline}")))
                        .build()
        );
        List<Airline> airlines = Arrays.asList(
                new Airline().withName("Lufthansa"),
                new Airline().withName("Lot"),
                new Airline().withName("Wizz Air")
        );

        Actions actions = new Actions();
        RulesEngine engine = new RulesEngineBuilder()
                .withRulesRepository(i -> rules)
                .withActionMapping("setRejectedFlag", method(actions, (action) -> action.reject(null)))
                .withActionMapping("collect", method(actions, (action) -> action.collect(null, null)))
                .withRulesExecutorBuilder(new DefaultRulesExecutorBuilder()
                        .withSequentialMode(true))
                .build();
        RuleSession session = engine.createSession("airlines");

        //when
        List<Airline> matching = session.execute(new ArrayList<>(), airlines);

        //then
        assertThat(matching).containsExactly(
                new Airline().withName("Lot"),
                new Airline().withName("Wizz Air")
        );
    }

    public static class Airline {
        public String name;
        public List<String> airlineCodes;
        public boolean isRejected = false;

        public Airline withName(String name) {
            this.name = name;
            return this;
        }

        public Airline withAirlineCodes(final List<String> airlineCodes) {
            this.airlineCodes = airlineCodes;
            return this;
        }

        public Airline withIsRejected(boolean isRejected) {
            this.isRejected = isRejected;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Airline airline = (Airline) o;
            return isRejected == airline.isRejected &&
                    Objects.equals(name, airline.name) &&
                    Objects.equals(airlineCodes, airline.airlineCodes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, airlineCodes, isRejected);
        }
    }

    public static class Actions {
        public void collect(List<Object> results, Object fact) {
            results.add(fact);
        }

        public void reject(Airline airline) {
            airline.withIsRejected(true);
        }
    }
}
