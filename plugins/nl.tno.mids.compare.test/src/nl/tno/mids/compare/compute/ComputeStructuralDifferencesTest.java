/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.compute;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffAutomaton;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffKind;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffProperty;

import nl.tno.mids.compare.data.MidsTransitionProperty;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.options.CompareAlgorithm;
import nl.tno.mids.compare.util.TestAutomata;

class ComputeStructuralDifferencesTest {
    private static final String COMPONENT_A_NAME = "A";

    /**
     * Determines whether the given transition property has any (nested) {@link DiffKind#ADDED added} or
     * {@link DiffKind#REMOVED removed} properties.
     * 
     * @param property The property to check.
     * @return {@code true} if {@code property} has any changes, {@code false} otherwise.
     */
    private boolean hasChanges(DiffProperty<MidsTransitionProperty> property) {
        return property.getDiffKind() != DiffKind.UNCHANGED || property.getProperty().getAnnotations().stream()
                .anyMatch(annotation -> annotation.getDiffKind() != DiffKind.UNCHANGED);
    }

    /**
     * Determines whether there are any {@link DiffKind#ADDED added} or {@link DiffKind#REMOVED removed} states, initial
     * state arrows or (parts of) transitions in the given difference automaton.
     * 
     * @param diff The difference automaton to check.
     * @return {@code true} if {@code diff} has any changes, {@code false} otherwise.
     */
    private boolean hasChanges(DiffAutomaton<MidsTransitionProperty> diff) {
        return diff.countStates(state -> state.getProperty().getStateDiffKind() != DiffKind.UNCHANGED) > 0
                || diff.countInitialStates(
                        initState -> initState.getProperty().getInitDiffKind() != DiffKind.UNCHANGED) > 0
                || diff.countTransitions(property -> hasChanges(property.getProperty())) > 0;
    }

    /**
     * Test that a simple automaton is not structural different from an identical automaton.
     */
    @Test
    public void testCompareStructuralBasicEquality() {
        Model modelLeft = TestAutomata.createModelA(COMPONENT_A_NAME);
        Model modelRight = TestAutomata.createModelA(COMPONENT_A_NAME);

        DiffAutomaton<MidsTransitionProperty> diff = ComputeStructuralDifferences.performStructuralComparison(
                modelLeft.getStructuralAutomaton(), modelRight.getStructuralAutomaton(), CompareAlgorithm.DYNAMIC,
                false);

        assertFalse(hasChanges(diff));
    }

    /**
     * Test that a simple automaton is structurally different if it contains an extra tau edge.
     */
    @Test
    public void testCompareStructuralWithTau() {
        Model modelLeft = TestAutomata.createModelA(COMPONENT_A_NAME);
        Model modelRight = TestAutomata.createModelAWithTau(COMPONENT_A_NAME);

        DiffAutomaton<MidsTransitionProperty> diff = ComputeStructuralDifferences.performStructuralComparison(
                modelLeft.getStructuralAutomaton(), modelRight.getStructuralAutomaton(), CompareAlgorithm.DYNAMIC,
                false);

        assertTrue(hasChanges(diff));
    }

    /**
     * Test that a simple automaton is structurally different from a flower automaton with the same alphabet.
     */
    @Test
    public void testCompareStructuralLessStates() {
        Model modelLeft = TestAutomata.createModelA(COMPONENT_A_NAME);
        Model modelRight = TestAutomata.createModelD(COMPONENT_A_NAME);

        DiffAutomaton<MidsTransitionProperty> diff = ComputeStructuralDifferences.performStructuralComparison(
                modelLeft.getStructuralAutomaton(), modelRight.getStructuralAutomaton(), CompareAlgorithm.DYNAMIC,
                false);

        assertTrue(hasChanges(diff));
    }

    /**
     * Test that a simple automaton is structurally different from a larger automaton with the same alphabet.
     */
    @Test
    public void testCompareStructuralMoreStates() {
        Model modelLeft = TestAutomata.createModelA(COMPONENT_A_NAME);
        Model modelRight = TestAutomata.createModelB(COMPONENT_A_NAME);

        DiffAutomaton<MidsTransitionProperty> diff = ComputeStructuralDifferences.performStructuralComparison(
                modelLeft.getStructuralAutomaton(), modelRight.getStructuralAutomaton(), CompareAlgorithm.DYNAMIC,
                false);

        assertTrue(hasChanges(diff));
    }

    /**
     * Test that a simple automaton is structurally different from an automaton with a different alphabet.
     */
    @Test
    public void testCompareStructuralDifferentAlphabet() {
        Model modelLeft = TestAutomata.createModelA(COMPONENT_A_NAME);
        Model modelRight = TestAutomata.createModelC(COMPONENT_A_NAME);

        DiffAutomaton<MidsTransitionProperty> diff = ComputeStructuralDifferences.performStructuralComparison(
                modelLeft.getStructuralAutomaton(), modelRight.getStructuralAutomaton(), CompareAlgorithm.DYNAMIC,
                false);

        assertTrue(hasChanges(diff));
    }
}
