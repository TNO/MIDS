/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.postprocessing.operations

import com.google.common.base.Preconditions
import java.nio.file.Path
import java.util.Map
import java.util.Set
import java.util.regex.Pattern
import net.automatalib.automata.fsa.impl.FastNFA
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import net.automatalib.util.automata.fsa.NFAs
import nl.tno.mids.automatalib.extensions.cif.AutomataLibToCif
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil
import nl.tno.mids.cif.extensions.AutomatonExtensions
import nl.tno.mids.cmi.api.basic.CmiBasicComponentQueries
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries
import nl.tno.mids.cmi.api.info.ComponentInfo
import nl.tno.mids.cmi.postprocessing.PostProcessingModel
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCifSpec
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCompactDfa
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.xtend.lib.annotations.Accessors

/**
 * Merge multiple runtime components, for instance multiple instances of the same executable, into a single runtime
 * component.
 */
@Accessors
class MergeComponents extends PostProcessingOperation<MergeComponentsOptions> {
    Pattern pattern;

    override getPreconditionSubset() {
        return new PostProcessingPreconditionSubset(false, false);
    }

    override getResultSubset() {
        return new PostProcessingResultSubset(false, false);
    }

    override applyOperation(Map<String, PostProcessingModel> models, Set<String> selectedComponents,
        Path relativeResolvePath, IProgressMonitor monitor) {

        // Because all models may be updated, ensure all are in the correct subset.
        models.forEach[component, model|preconditionSubset.ensureSubset(model)]

        // Initialize rename mapping from old component names to new component names.
        val componentRenameMap = newHashMap

        // Collect all components to merge.
        val componentsToMerge = selectedComponents.filter [ key |
            getComponentNameMatcher(key).matches
        ].toSet

        // Process all components that need to be merged.
        while (!componentsToMerge.empty) {
            // Pick a component to merge.
            val ComponentInfo componentInfo = new ComponentInfo(componentsToMerge.head)
            val matcher = getComponentNameMatcher(componentInfo)

            val matchResult = matcher.matches
            Preconditions.checkArgument(matchResult, "Expected match for " + componentsToMerge.head)

            // Extract name of component to be merged.
            val baseInfo = new ComponentInfo(matcher.group("name"), null, componentInfo.traced)

            // Collect all components to merge with the selected component.
            val currentComponentsToMerge = componentsToMerge.filter [ key |
                key.isComponentToMerge(baseInfo)
            ].toSet

            currentComponentsToMerge.forEach [ key |
                componentRenameMap.put(new ComponentInfo(key), baseInfo)
            ]

            // Pick first component as start model for merging.
            val head = currentComponentsToMerge.head
            val firstDfa = models.get(head).compactDfa
            val firstFastNfa = AutomataLibUtil.dfaToNfa(AutomataLibUtil.copy(firstDfa, firstDfa.inputAlphabet))

            // Avoid merging start model with itself.
            val otherComponentsToMerge = currentComponentsToMerge.filter[k|k != head]
            val otherComponentsModels = models.filter[k, v|otherComponentsToMerge.contains(k)]

            // Merge component models together into one model.
            val resultNfa = otherComponentsModels.values.fold(firstFastNfa, [ left, right |
                mergeInitialStates(left, right.compactDfa)
            ])
            val resultDfa = NFAs.determinize(resultNfa, resultNfa.inputAlphabet, true, false)

            // Minimize DFA separately, for better performance.
            val minimizedResultDfa = AutomataLibUtil.minimizeDFA(resultDfa)

            // Remove merged models.
            currentComponentsToMerge.forEach [ key |
                componentsToMerge.remove(key)
                models.remove(key)
            ]

            // Add merged model to set of models.
            Preconditions.checkArgument(!models.containsKey(baseInfo.toString),
                baseInfo.toString + " already exists in model set")
            models.put(baseInfo.toString, new PostProcessingModelCompactDfa(minimizedResultDfa, baseInfo.toString,
                new PostProcessingStatus(false, false)))

        }

        // At this point, there is one model per set of models to merge, but that model contains the identity of the old
        // components that have been merged, and other models still reference to individual component names. To address
        // this, rename all merged component references in events to the names of the merged components.
        models.entrySet.forEach [ entry |
            // Convert to NFA, as multiple events may be renamed to the same event name. Then rename, and determinize
            // back to a DFA.
            val nfa = AutomataLibUtil.dfaToNfa(entry.value.compactDfa)
            val renamedNfa = AutomataLibUtil.rename(nfa, [ event |
                normalizeMergedComponentNamesInEvent(event, componentRenameMap)
            ])
            val renamedDfa = NFAs.determinize(renamedNfa, renamedNfa.inputAlphabet, true, false)

            // Minimize DFA separately, for better performance.
            val minimizedRenamedDfa = AutomataLibUtil.minimizeDFA(renamedDfa)

            // Convert back to a CIF model.
            val renamedCif = AutomataLibToCif.fsaToCifSpecification(minimizedRenamedDfa, entry.value.name, true)

            // Ensure the initial location is the first location.
            val automaton = CmiBasicComponentQueries.getSingleComponentWithBehavior(renamedCif)
            AutomatonExtensions.ensureInitialLocationIsFirstLocation(automaton)

            // Store the new model.
            entry.value = new PostProcessingModelCifSpec(renamedCif, entry.value.name,
                getResultStatus(entry.value.status))
        ]
    }

    private def normalizeMergedComponentNamesInEvent(String eventName,
        Map<ComponentInfo, ComponentInfo> componentRenameMap) {
        var eventInfo = CmiGeneralEventQueries.getEventInfo(eventName)

        if (componentRenameMap.containsKey(eventInfo.declCompInfo)) {
            eventInfo = eventInfo.withDeclCompInfo(componentRenameMap.get(eventInfo.declCompInfo))
        }

        if (eventInfo.otherCompInfo !== null) {
            if (componentRenameMap.containsKey(eventInfo.otherCompInfo)) {
                eventInfo = eventInfo.withOtherCompInfo(componentRenameMap.get(eventInfo.otherCompInfo))
            }
        }

        return eventInfo.toString
    }

    private def getComponentNameMatcher(String componentName) {
        getComponentNameMatcher(new ComponentInfo(componentName))
    }

    private def getComponentNameMatcher(ComponentInfo componentInfo) {
        getPattern().matcher(componentInfo.name)
    }

    private def getPattern() {
        if (pattern === null) {
            pattern = Pattern.compile(options.pattern)
        }
        return pattern
    }

    private def isComponentToMerge(String componentName, ComponentInfo baseInfo) {
        val componentInfo = new ComponentInfo(componentName)
        val matcher = getComponentNameMatcher(componentInfo)
        if (matcher.matches) {
            return matcher.group("name") == baseInfo.name && componentInfo.traced == baseInfo.traced
        } else {
            return false
        }
    }

    private def mergeInitialStates(FastNFA<String> left, CompactDFA<String> right) {
        // Check if initial states can be merged.
        Preconditions.checkArgument(left.initialStates.size == 1)
        Preconditions.checkArgument(left.isAccepting(left.initialStates.head) == right.isAccepting(right.initialState))

        // Make sure all necessary events are in the alphabet.
        right.inputAlphabet.forEach[left.addAlphabetSymbol(it)]

        // Copy states, creating a map linking old states to new states.
        val stateMap = newHashMap
        stateMap.put(right.initialState, left.initialStates.head)
        right.states.filter[it != right.initialState].forEach [
            val newState = left.addState(right.isAccepting(it));
            stateMap.put(it, newState)
        ]

        // Copy transitions based on state map.
        right.states.forEach [ state |
            val localInputs = right.getLocalInputs(state);
            localInputs.forEach [ input |
                val nextState = right.getSuccessor(state, input);
                left.addTransition(stateMap.get(state), input, stateMap.get(nextState))
            ];
        ]
        return left
    }
}
