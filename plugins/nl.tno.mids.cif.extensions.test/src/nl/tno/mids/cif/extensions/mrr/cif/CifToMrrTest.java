/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cif.extensions.mrr.cif;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.junit.jupiter.api.Test;

import com.google.common.base.Preconditions;

import nl.tno.mids.cif.extensions.mrr.data.MrrWithWord;

/** {@link CifToMrr} tests. */
public class CifToMrrTest {
    @Test
    public void testExample1() {
        // Example 1 (Bottum-Up Approach) from the paper.
        test("abbcaabbca", "(abbca)^2", 6);
    }

    @Test
    public void testExample2() {
        // Example 2 (Greedy Approach) from the paper.
        test("abaababaabbaab", "(aba)^2(baab)^2", 9);
    }

    @Test
    public void testPreviousImpl() {
        // Test from previous implementation.
        test("acab" + "acab" + "ab" + "acab", "ac(abacab)^2", 9);
    }

    @Test
    public void testNested() {
        // Test various levels of nesting.
        String s = "aabbbccc";
        String s2 = "x" + s + s + "y";
        String s3 = "hh" + s2 + s2 + s2 + s2 + "ii";
        test(s3, "hh(x(aa(b)^3(c)^3)^2y)^4(i)^2", 14);
    }

    @Test
    public void testTwo() {
        // Test repetitions of two, which can be either represented using repetition, or
        // not, for same cost.
        // Assumes cost of letters and repetitions is 1.
        test("aabbccddeeffgghh", "aabbccddeeffgg(h)^2", 16);
    }

    public void test(String word, String expectedTxt, int expectedCost) {
        // Get CIF specification.
        Specification spec = CifMrrTestUtils.wordToCif(word);

        // Convert CIF to MRR.
        CifToMrrConfig config = new CifToMrrConfig();
        List<MrrWithWord<CifMrrLetter>> results = CifToMrr.cifToMrr(spec, config, new NullProgressMonitor());
        Preconditions.checkState(results.size() == 1);
        MrrWithWord<CifMrrLetter> result = results.get(0);

        // Check MRR.
        String actualTxt = result.mrr.toSingleLineString().replace(" ", "");
        assertEquals(expectedTxt, actualTxt);

        int actualCost = result.mrr.getCost();
        assertEquals(expectedCost, actualCost);

        int actualDomainSize = result.mrr.getDomainSize();
        assertEquals(word.length(), actualDomainSize);
    }
}
