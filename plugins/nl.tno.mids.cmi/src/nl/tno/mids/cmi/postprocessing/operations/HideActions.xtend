/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.postprocessing.operations

import java.nio.file.Path
import java.util.Map
import java.util.Set
import java.util.regex.Pattern
import net.automatalib.automata.fsa.impl.compact.CompactNFA
import net.automatalib.util.automata.fsa.NFAs
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil
import nl.tno.mids.cmi.postprocessing.PostProcessingModel
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCompactDfa
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.xtend.lib.annotations.Accessors

/** Hide actions matching a given regular expression. */
@Accessors
class HideActions extends PostProcessingOperation<HideActionsOptions> {
    override getPreconditionSubset() { return new PostProcessingPreconditionSubset(false, null); }

    override getResultSubset() { return new PostProcessingResultSubset(false, false); }

    override applyOperation(Map<String, PostProcessingModel> models, Set<String> selectedComponents,
        Path relativeResolvePath, IProgressMonitor monitor) {

        monitor.subTask("Hiding actions")
        val pattern = Pattern.compile(options.pattern)

        for (component : selectedComponents) {
            val componentModel = models.get(component)
            preconditionSubset.ensureSubset(componentModel)

            val previousDfa = componentModel.getCompactDfa()

            // Compute initial and final events of service fragments, i.e. the incoming and outgoing events of the 
            // initial state, so we can avoid filtering them later on.
            val initialState = previousDfa.initialState
            val serviceFragmentEvents = previousDfa.getLocalInputs(initialState);
            for (state : previousDfa.states) {
                for (input : previousDfa.getLocalInputs(state)) {
                    if (previousDfa.getSuccessor(state, input) == initialState) {
                        serviceFragmentEvents.add(input);
                    }
                }
            }

            // Perform rename based on pattern and protected service fragment events.
            val newNfa = AutomataLibUtil.rename(previousDfa, [a|new CompactNFA<String>(a)],
                previousDfa.getInputAlphabet(), [ t |
                    (pattern.matcher(t).matches && !serviceFragmentEvents.contains(t)) ? "tau" : t
                ])

            // Convert result to normalized DFA.
            val newDfa = NFAs.determinize(newNfa, newNfa.inputAlphabet, true, false)
            val newDfaNoTau = AutomataLibUtil.normalizeWeakTrace(newDfa)
            models.put(component, new PostProcessingModelCompactDfa(newDfaNoTau, component,
                getResultStatus(componentModel.status)))
        }
    }
}
