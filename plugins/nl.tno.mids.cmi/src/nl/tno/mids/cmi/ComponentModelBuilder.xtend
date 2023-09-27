/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi

import com.google.common.base.Preconditions
import java.util.LinkedHashMap
import java.util.Map
import java.util.NoSuchElementException
import net.automatalib.automata.ShrinkableAutomaton
import net.automatalib.automata.concepts.InputAlphabetHolder
import net.automatalib.automata.fsa.DFA
import net.automatalib.automata.fsa.impl.FastDFA
import net.automatalib.words.Word
import net.automatalib.words.WordBuilder
import nl.esi.pps.architecture.instantiated.Executor
import nl.esi.pps.tmsc.Event
import nl.esi.pps.tmsc.ExitEvent
import nl.esi.pps.tmsc.Lifeline
import nl.esi.pps.tmsc.TMSC
import nl.tno.mids.automatalib.extensions.cif.AutomataLibToCif
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil
import nl.tno.mids.automatalib.extensions.util.IncrementalMutableDFATreeBuilder
import nl.tno.mids.pps.extensions.queries.TmscEventQueries
import nl.tno.mids.pps.extensions.queries.TmscExecutionQueries
import nl.tno.mids.pps.extensions.queries.TmscLifelineQueries
import org.eclipse.escet.cif.metamodel.cif.Specification
import org.eclipse.xtend.lib.annotations.Accessors

import static extension nl.tno.mids.automatalib.extensions.util.AutomataLibUtil.*
import static extension nl.tno.mids.cmi.utils.CifNamesUtil.*
import static extension nl.tno.mids.cmi.utils.TimestampHelper.*

class ComponentModelBuilder {

    val LinkedHashMap<String, IncrementalMutableDFATreeBuilder<String>> componentAutomata
    @Accessors boolean synchronous

    new() {
        this(false)
    }

    new(boolean synchronous) {
        componentAutomata = new LinkedHashMap
        this.synchronous = synchronous
    }

    /**
     * Builds and inserts models for all execution call stacks that are in scope of {@code tmsc}.
     * 
     * @param tmsc The {@link TMSC} that determines the scope for model construction.
     */
    def void insert(TMSC tmsc) {
        TmscLifelineQueries.nonEmptyLifelinesOf(tmsc).forEach[insert(it, tmsc)]
    }

    /**
     * Builds and inserts models for all execution call stacks on {@code lifeline} that are in scope of {@code tmsc}.
     * 
     * @param lifeline The {@link Lifeline} from which models are to be constructed.
     * @param tmsc The {@link TMSC} that determines the scope for model construction.
     */
    def void insert(Lifeline lifeline, TMSC tmsc) {
        val iterator = lifeline.events.filter[TmscEventQueries.isInScope(tmsc, it)].sortBy[timestamp].iterator
        val builder = lifeline.builder
        var Event event;

        try {
            while (iterator.hasNext) { // iterates over tasks (call stacks)
                val wordBuilder = new WordBuilder
                do {
                    event = iterator.next
                    wordBuilder.append(event.asCifName(tmsc, synchronous))
                } while (!(event instanceof ExitEvent &&
                    TmscExecutionQueries.getRootInScope(tmsc, event.execution) === null))
                builder.insert(wordBuilder.toWord)
            }
        } catch (NoSuchElementException cause) {
            throw new RuntimeException("Invalid stack at event " + event + "(" + event.component + ")@" +
                event.timestamp.readable, cause)
        }
    }

    def void insert(String componentLabel, Word<String> word) {
        val builder = getBuilder(componentLabel)
        builder.insert(word)
    }

    def getComponentLabels() {
        return componentAutomata.keySet
    }

    def getPTA(Executor executor) {
        return this.getPTA(executor.asCifName)
    }

    def getPTA(String componentLabel) {
        return componentAutomata.get(componentLabel).getFastDFA(true) // enforces a clean copy
    }

    def getPTAs() {
        val ptas = new LinkedHashMap(componentAutomata.size)
        componentAutomata.forEach [ k, v |
            ptas.put(k, getPTA(k))
        ]
        return ptas
    }

    def getDFA(Executor executor) {
        return getDFA(executor.asCifName)
    }

    def getDFA(String componentLabel) {
        // Get PTA.
        // Does not enforce a clean copy, but uses caching of builder to store result.
        val pta = getPTA(componentLabel)

        // Get minimal DFA.
        val dfa = AutomataLibUtil.minimizeDFA(pta)

        // Loop the DFA.
        dfa.loop

        return dfa
    }

    def getDFAs() {
        val dfas = new LinkedHashMap(componentAutomata.size)
        componentAutomata.forEach [ k, v |
            dfas.put(k, getDFA(k))
        ]
        return dfas
    }

    /**
     * Returns a CIF specification with the all the models learned by this learner
     */
    def getCifComposition() {
        return getCifComposition(getDFAs)
    }

    /**
     * Returns a CIF specification with the specified automata. Allows use of this.getCifComposition(this.getPTAs()) 
     * to obtain PTA composition.
     */
    def getCifComposition(Map<String, FastDFA<String>> automata) {
        return AutomataLibToCif.fsasToCifSpecification(automata, true);
    }

    /**
     * Returns a map from string to specification, containing a CIF specification for each model learned by this learner
     */
    def getCifModels() {
        return getCifModels(getDFAs)
    }

    /**
     * Returns a map from string to specification, containing a CIF specification for each model passed.
     */
    def getCifModels(Map<String, FastDFA<String>> automata) {
        val ret = new LinkedHashMap<String, Specification>(automata.size)
        automata.forEach [ name, dfa |
            ret.put(name, getCifModel(name, dfa));
        ]
        return ret
    }

    /**
     * Returns a CIF specification for the model passed.
     */
    def getCifModel(String name, FastDFA<String> dfa) {
        return AutomataLibToCif.fsaToCifSpecification(dfa, name, true);
    }

    def protected getBuilder(Lifeline lifeline) {
        return getBuilder(lifeline.executor.asCifName)
    }

    def protected getBuilder(String componentLabel) {
        if (componentAutomata.containsKey(componentLabel)) {
            return componentAutomata.get(componentLabel)
        } else {
            val builder = new IncrementalMutableDFATreeBuilder
            componentAutomata.put(componentLabel, builder)
            return builder
        }
    }

    def protected static <I, S, A extends ShrinkableAutomaton<S, I, S, Boolean, Void> & DFA<S, I> & InputAlphabetHolder<I>> A loop(
        A dfa) {
        // We only support a single initial state.
        Preconditions.checkState(dfa.initialStates.size == 1)

        // Only the single final state should be accepting, for a minimized PTA.
        var acceptingStates = dfa.states.filter[dfa.getStateProperty(it)].toList
        Preconditions.checkState(acceptingStates.size == 1)
        var acceptingState = acceptingStates.get(0)
        if (dfa.size > 1) {
            // If multiple states in PTA, accepting state must not be the initial state.
            Preconditions.checkState(!acceptingState.equals(dfa.initialState))
        }

        // Merge accepting final state into the initial state to introduce a loop.
        dfa.merge(dfa.initialState, acceptingState, true)
        dfa.setStateProperty(dfa.initialState, true)
        return dfa
    }
}
