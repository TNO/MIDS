/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.automatalib.extensions.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;

class AldebaranUtilTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("testAutomata")
    void testCompactDFA(String name, String testInput) throws IOException {
        ByteArrayInputStream testInputStream = new ByteArrayInputStream(testInput.getBytes(StandardCharsets.UTF_8));

        CompactDFA<String> compactDFA = AldebaranUtil.readAldebaranCompactDfa(testInputStream);

        ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
        AldebaranUtil.writeAldebaran(compactDFA, testOutputStream);
        assertEquals(testInput, testOutputStream.toString(StandardCharsets.UTF_8.name()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testAutomata")
    void testFastDFA(String name, String testInput) throws IOException {
        ByteArrayInputStream testInputStream = new ByteArrayInputStream(testInput.getBytes(StandardCharsets.UTF_8));

        FastDFA<String> fastDFA = AldebaranUtil.readAldebaranFastDfa(testInputStream);

        ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
        AldebaranUtil.writeAldebaran(fastDFA, testOutputStream);
        assertEquals(testInput, testOutputStream.toString(StandardCharsets.UTF_8.name()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testAutomata")
    void testCompactNFA(String name, String testInput) throws IOException {
        ByteArrayInputStream testInputStream = new ByteArrayInputStream(testInput.getBytes(StandardCharsets.UTF_8));

        CompactNFA<String> compactNFA = AldebaranUtil.readAldebaranCompactNfa(testInputStream);

        ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
        AldebaranUtil.writeAldebaran(compactNFA, testOutputStream);
        assertEquals(testInput, testOutputStream.toString(StandardCharsets.UTF_8.name()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testAutomata")
    void testFastNFA(String name, String testInput) throws IOException {
        ByteArrayInputStream testInputStream = new ByteArrayInputStream(testInput.getBytes(StandardCharsets.UTF_8));

        FastNFA<String> fastNFA = AldebaranUtil.readAldebaranFastNfa(testInputStream);

        ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();
        AldebaranUtil.writeAldebaran(fastNFA, testOutputStream);
        assertEquals(testInput, testOutputStream.toString(StandardCharsets.UTF_8.name()));
    }

    private static Stream<Arguments> testAutomata() throws IOException {
        StringBuilder testSmall = new StringBuilder();
        testSmall.append("des (0,3,4)\n");
        testSmall.append("(0,\"a\",1)\n");
        testSmall.append("(0,\"b\",2)\n");
        testSmall.append("(2,\"c\",3)\n");

        StringBuilder testDiningPhilosophers = new StringBuilder();
        testDiningPhilosophers.append("des (0,13,11)\n");
        testDiningPhilosophers.append("(0,\"lock(p2, f2)\",1)\n");
        testDiningPhilosophers.append("(0,\"lock(p1, f1)\",2)\n");
        testDiningPhilosophers.append("(1,\"lock(p1, f1)\",3)\n");
        testDiningPhilosophers.append("(1,\"lock(p2, f1)\",4)\n");
        testDiningPhilosophers.append("(2,\"lock(p2, f2)\",3)\n");
        testDiningPhilosophers.append("(2,\"lock(p1, f2)\",5)\n");
        testDiningPhilosophers.append("(4,\"eat(p2)\",6)\n");
        testDiningPhilosophers.append("(5,\"eat(p1)\",7)\n");
        testDiningPhilosophers.append("(6,\"free(p2, f2)\",8)\n");
        testDiningPhilosophers.append("(7,\"free(p1, f1)\",9)\n");
        testDiningPhilosophers.append("(8,\"free(p2, f1)\",0)\n");
        testDiningPhilosophers.append("(9,\"free(p1, f2)\",0)\n");
        testDiningPhilosophers.append("(9,\"dead\",10)\n");

        StringBuilder testIndustrial = new StringBuilder(); // Anonymized industrial test case.
        testIndustrial.append("des (3,29,16)\n");
        testIndustrial.append("(0,\"a\",1)\n");
        testIndustrial.append("(0,\"e\",5)\n");
        testIndustrial.append("(0,\"b\",1)\n");
        testIndustrial.append("(1,\"f\",0)\n");
        testIndustrial.append("(2,\"e\",15)\n");
        testIndustrial.append("(2,\"f\",1)\n");
        testIndustrial.append("(3,\"a\",8)\n");
        testIndustrial.append("(3,\"e\",3)\n");
        testIndustrial.append("(4,\"d\",2)\n");
        testIndustrial.append("(5,\"a\",6)\n");
        testIndustrial.append("(5,\"e\",5)\n");
        testIndustrial.append("(5,\"b\",4)\n");
        testIndustrial.append("(6,\"f\",5)\n");
        testIndustrial.append("(7,\"e\",15)\n");
        testIndustrial.append("(7,\"f\",6)\n");
        testIndustrial.append("(8,\"c\",7)\n");
        testIndustrial.append("(9,\"f\",6)\n");
        testIndustrial.append("(10,\"e\",9)\n");
        testIndustrial.append("(10,\"f\",6)\n");
        testIndustrial.append("(11,\"e\",10)\n");
        testIndustrial.append("(11,\"f\",6)\n");
        testIndustrial.append("(12,\"e\",11)\n");
        testIndustrial.append("(12,\"f\",6)\n");
        testIndustrial.append("(13,\"e\",12)\n");
        testIndustrial.append("(13,\"f\",6)\n");
        testIndustrial.append("(14,\"e\",13)\n");
        testIndustrial.append("(14,\"f\",6)\n");
        testIndustrial.append("(15,\"e\",14)\n");
        testIndustrial.append("(15,\"f\",6)\n");

        return Stream.of(Arguments.of("Small", testSmall.toString()),
                Arguments.of("DiningPhilosophers", testDiningPhilosophers.toString()),
                Arguments.of("Industrial", testIndustrial.toString()));
    }
}
