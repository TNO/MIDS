/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.util;

import java.util.Arrays;

import org.eclipse.escet.cif.metamodel.cif.Specification;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.GrowingMapAlphabet;
import nl.tno.mids.automatalib.extensions.cif.AutomataLibToCif;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.RepetitionCount;
import nl.tno.mids.gltsdiff.extensions.AnnotatedProperty;

/** Constructors for test models. */
public class TestAutomata {
    private static final String DEFAULT_COMPONENT_NAME = "CompA";

    private static final String DEFAULT_COMPONENT_EVENT_A = DEFAULT_COMPONENT_NAME + ".a__a__blk";

    private static final String DEFAULT_COMPONENT_EVENT_B = DEFAULT_COMPONENT_NAME + ".b__b__blk";

    private static final String DEFAULT_COMPONENT_EVENT_C = DEFAULT_COMPONENT_NAME + ".c__c__blk";

    private static final String TAU_EVENT = "tau";

    private static final int BIG_AUTOMATON_SIZE = 5000;

    /**
     * Create a model with two events, a and b, and two states that form a loop.
     * 
     * @param componentName Name of the component represented by the model.
     * @return The created model.
     */
    public static Model createModelA(String componentName) {
        final Alphabet<String> sigma = new GrowingMapAlphabet<>(
                Arrays.asList(DEFAULT_COMPONENT_EVENT_A, DEFAULT_COMPONENT_EVENT_B));
        final CompactDFA<String> dfa = new CompactDFA<>(sigma);

        final int q0 = dfa.addIntInitialState(true);
        final int q1 = dfa.addIntState(true);

        dfa.setTransition(q0, sigma.getSymbolIndex(DEFAULT_COMPONENT_EVENT_A), q1);
        dfa.setTransition(q1, sigma.getSymbolIndex(DEFAULT_COMPONENT_EVENT_B), q0);

        return createModelFromDfa(dfa, componentName);
    }

    /**
     * Create a model that is identical to the one created by {@link #createModelA()}, except that an extra tau edge has
     * been inserted in between the events a and b.
     * 
     * @param componentName Name of the component represented by the model.
     * @return The created model.
     */
    public static Model createModelAWithTau(String componentName) {
        final Alphabet<String> sigma = new GrowingMapAlphabet<>(
                Arrays.asList(DEFAULT_COMPONENT_EVENT_A, DEFAULT_COMPONENT_EVENT_B, TAU_EVENT));
        final CompactDFA<String> dfa = new CompactDFA<>(sigma);

        final int q0 = dfa.addIntInitialState(true);
        final int q1 = dfa.addIntState(true);
        final int q2 = dfa.addIntState(true);

        dfa.setTransition(q0, sigma.getSymbolIndex(DEFAULT_COMPONENT_EVENT_A), q1);
        dfa.setTransition(q1, sigma.getSymbolIndex(TAU_EVENT), q2);
        dfa.setTransition(q2, sigma.getSymbolIndex(DEFAULT_COMPONENT_EVENT_B), q0);

        return createModelFromDfa(dfa, componentName);
    }

    /**
     * Create an model with two events, a and b, three states that form a loop.
     * 
     * @param componentName Name of the component represented by the model.
     * @return The created model.
     */
    public static Model createModelB(String componentName) {
        final Alphabet<String> sigma = new GrowingMapAlphabet<>(
                Arrays.asList(DEFAULT_COMPONENT_EVENT_A, DEFAULT_COMPONENT_EVENT_B));
        final CompactDFA<String> dfa = new CompactDFA<>(sigma);

        final int q0 = dfa.addIntInitialState(true);
        final int q1 = dfa.addIntState(true);
        final int q2 = dfa.addIntState(true);

        dfa.setTransition(q0, sigma.getSymbolIndex(DEFAULT_COMPONENT_EVENT_A), q1);
        dfa.setTransition(q1, sigma.getSymbolIndex(DEFAULT_COMPONENT_EVENT_B), q2);
        dfa.setTransition(q2, sigma.getSymbolIndex(DEFAULT_COMPONENT_EVENT_B), q0);

        return createModelFromDfa(dfa, componentName);
    }

    /**
     * Create a model with two events, a and b, that describes a union of {@link #createModelA} and
     * {@link #createModelB}.
     * 
     * @param componentName Name of the component represented by the model.
     * @return The created model.
     */
    public static Model createModelAB(String componentName) {
        final Alphabet<String> sigma = new GrowingMapAlphabet<>(
                Arrays.asList(DEFAULT_COMPONENT_EVENT_A, DEFAULT_COMPONENT_EVENT_B));
        final CompactDFA<String> dfa = new CompactDFA<>(sigma);

        final int q0 = dfa.addIntInitialState(true);
        final int q1 = dfa.addIntState(true);
        final int q2 = dfa.addIntState(true);

        dfa.setTransition(q0, sigma.getSymbolIndex(DEFAULT_COMPONENT_EVENT_A), q1);
        dfa.setTransition(q1, sigma.getSymbolIndex(DEFAULT_COMPONENT_EVENT_B), q2);
        dfa.setTransition(q2, sigma.getSymbolIndex(DEFAULT_COMPONENT_EVENT_A), q1);
        dfa.setTransition(q2, sigma.getSymbolIndex(DEFAULT_COMPONENT_EVENT_B), q0);

        return createModelFromDfa(dfa, componentName);
    }

    /**
     * Create a model with two events, a and c, and two states that form a loop.
     * 
     * @param componentName Name of the component represented by the model.
     * @return The created model.
     */
    public static Model createModelC(String componentName) {
        final Alphabet<String> sigma = new GrowingMapAlphabet<>(
                Arrays.asList(DEFAULT_COMPONENT_EVENT_A, DEFAULT_COMPONENT_EVENT_C));
        final CompactDFA<String> dfa = new CompactDFA<>(sigma);

        final int q0 = dfa.addIntInitialState(true);
        final int q1 = dfa.addIntState(true);

        dfa.setTransition(q0, sigma.getSymbolIndex(DEFAULT_COMPONENT_EVENT_A), q1);
        dfa.setTransition(q1, sigma.getSymbolIndex(DEFAULT_COMPONENT_EVENT_C), q0);

        return createModelFromDfa(dfa, componentName);
    }

    /**
     * Create a model containing a flower automaton with two events, a and b.
     * 
     * @param componentName Name of the component represented by the model.
     * @return The created model.
     */
    public static Model createModelD(String componentName) {
        final Alphabet<String> sigma = new GrowingMapAlphabet<>(
                Arrays.asList(DEFAULT_COMPONENT_EVENT_A, DEFAULT_COMPONENT_EVENT_B));
        final CompactDFA<String> dfa = new CompactDFA<>(sigma);

        final int q0 = dfa.addIntInitialState(true);

        dfa.setTransition(q0, sigma.getSymbolIndex(DEFAULT_COMPONENT_EVENT_A), q0);
        dfa.setTransition(q0, sigma.getSymbolIndex(DEFAULT_COMPONENT_EVENT_B), q0);

        return createModelFromDfa(dfa, componentName);
    }

    /**
     * Create a model with an empty language.
     * 
     * @param componentName Name of the component represented by the model.
     * @return The created model.
     */
    public static Model createModelEmpty(String componentName) {
        final Alphabet<String> sigma = new GrowingMapAlphabet<>(
                Arrays.asList(DEFAULT_COMPONENT_EVENT_A, DEFAULT_COMPONENT_EVENT_B));
        final CompactDFA<String> dfa = new CompactDFA<>(sigma);

        dfa.addIntInitialState(false);

        return createModelFromDfa(dfa, componentName);
    }

    /**
     * Create a model with two events, a and c, and more than BIG_AUTOMATON_SIZE states that form a loop.
     * 
     * @param componentName Name of the component represented by the model.
     * @return The created model.
     */
    public static Model createModelBig(String componentName) {
        final Alphabet<String> sigma = new GrowingMapAlphabet<>(Arrays.asList(DEFAULT_COMPONENT_EVENT_A));
        final CompactDFA<String> dfa = new CompactDFA<>(sigma);

        int q0 = dfa.addIntInitialState(true);

        for (int i = 0; i < BIG_AUTOMATON_SIZE + 1; i++) {
            int q1 = dfa.addIntState(true);
            dfa.setTransition(q0, sigma.getSymbolIndex(DEFAULT_COMPONENT_EVENT_A), q1);
            q0 = q1;
        }

        int q2 = dfa.addIntState(true);
        dfa.setTransition(q0, sigma.getSymbolIndex(DEFAULT_COMPONENT_EVENT_A), q2);

        return createModelFromDfa(dfa, componentName);
    }

    /**
     * Create a model object based on the behavior in a given DFA.
     * 
     * @param dfa DFA describing the behavior of the model.
     * @param componentName Name of the component represented by the model.
     * @return The created model.
     */
    private static Model createModelFromDfa(CompactDFA<String> dfa, String componentName) {
        Specification spec = AutomataLibToCif.fsaToCifSpecification(dfa, componentName, true);
        CompactNFA<String> nfa = AutomataLibUtil.dfaToNfa(dfa);
        CompactDFA<String> minimalDfa = AutomataLibUtil.normalizeWeakTrace(dfa);
        CompactNFA<AnnotatedProperty<String, RepetitionCount>> annotatedNfa = AutomataLibUtil.rename(nfa,
                AnnotatedProperty::new);

        return new Model(spec, minimalDfa, annotatedNfa, componentName);
    }
}
