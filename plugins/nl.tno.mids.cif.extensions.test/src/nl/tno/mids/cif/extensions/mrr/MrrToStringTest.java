/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cif.extensions.mrr;

import static org.eclipse.escet.common.java.Lists.list;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import nl.tno.mids.cif.extensions.mrr.data.ConcatenationMRR;
import nl.tno.mids.cif.extensions.mrr.data.LetterMRR;
import nl.tno.mids.cif.extensions.mrr.data.RepetitionMRR;

public class MrrToStringTest {
    @Test
    public void testLetter() {
        LetterMRR<String> l = new LetterMRR<String>("a", createConfig());
        assertEquals("a", l.toSingleLineString());
        assertEquals("a", l.toMultiLineString());
    }

    @Test
    public void testLetterConcat() {
        LetterMRR<String> l1 = new LetterMRR<String>("a", createConfig());
        LetterMRR<String> l2 = new LetterMRR<String>("b", createConfig());
        LetterMRR<String> l3 = new LetterMRR<String>("c", createConfig());
        ConcatenationMRR<String> c = new ConcatenationMRR<String>(list(l1, l2, l3));
        assertEquals("a b c", c.toSingleLineString());
        assertEquals("a\nb\nc", c.toMultiLineString());
    }

    @Test
    public void testLetterRepeat() {
        MrrConfig<String> config = createConfig();
        LetterMRR<String> l = new LetterMRR<String>("a", config);
        RepetitionMRR<String> c = new RepetitionMRR<String>(l, 3, config);
        assertEquals("(a)^3", c.toSingleLineString());
        assertEquals("|\\ 3 repeats\n| a\n|/ 3 domain letters covered", c.toMultiLineString());
    }

    @Test
    public void testCombi() {
        MrrConfig<String> config = createConfig();
        LetterMRR<String> l1 = new LetterMRR<String>("a", config);
        LetterMRR<String> l2 = new LetterMRR<String>("b", config);
        LetterMRR<String> l3 = new LetterMRR<String>("c", config);
        LetterMRR<String> l4 = new LetterMRR<String>("d", config);
        LetterMRR<String> l5 = new LetterMRR<String>("e", config);
        ConcatenationMRR<String> c12 = new ConcatenationMRR<String>(list(l1, l2));
        ConcatenationMRR<String> c45 = new ConcatenationMRR<String>(list(l4, l5));
        RepetitionMRR<String> r12 = new RepetitionMRR<String>(c12, 3, config);
        ConcatenationMRR<String> c12345 = new ConcatenationMRR<String>(list(r12, l3, c45));
        assertEquals("(a b)^3 c d e", c12345.toSingleLineString());
        assertEquals("|\\ 3 repeats\n| a\n| b\n|/ 6 domain letters covered\nc\nd\ne", c12345.toMultiLineString());
    }

    @Test
    public void testNested() {
        MrrConfig<String> config = createConfig();
        LetterMRR<String> l1 = new LetterMRR<String>("a", config);
        LetterMRR<String> l2 = new LetterMRR<String>("b", config);
        LetterMRR<String> l3 = new LetterMRR<String>("c", config);
        RepetitionMRR<String> r1 = new RepetitionMRR<String>(l1, 3, config);
        RepetitionMRR<String> r2 = new RepetitionMRR<String>(l2, 4, config);
        ConcatenationMRR<String> c12 = new ConcatenationMRR<String>(list(r1, r2));
        ConcatenationMRR<String> c123 = new ConcatenationMRR<String>(list(c12, l3));
        RepetitionMRR<String> r123 = new RepetitionMRR<String>(c123, 2, config);
        assertEquals("((a)^3 (b)^4 c)^2", r123.toSingleLineString());
        assertEquals("|\\ 2 repeats\n| |\\ 3 repeats\n| | a\n| |/ 3 domain letters covered\n| |\\ 4 repeats\n| | b\n"
                + "| |/ 4 domain letters covered\n| c\n|/ 16 domain letters covered", r123.toMultiLineString());
    }

    private MrrConfig<String> createConfig() {
        return new MrrConfig<String>() {
            @Override
            public int getLetterIntRepresentative(String domainLetter) {
                assertEquals(1, domainLetter.length());
                return domainLetter.codePointAt(0);
            }

            @Override
            public int getLetterCost(String domainLetter) {
                return 1;
            }

            @Override
            public int getRepetitionCost(int repetitionCount) {
                return 1;
            }

            @Override
            public String getPrintLetterText(String domainLetter) {
                return domainLetter;
            }
        };
    }
}
