/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.postprocessing.operations;

import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.escet.common.java.Pair;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.util.automata.fsa.NFAs;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries;
import nl.tno.mids.cmi.api.info.EventInfo;
import nl.tno.mids.cmi.postprocessing.PostProcessingModel;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCompactDfa;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset;

/**
 * Renames functions based on a mapping from old to new function names. This post-processing operation assumes that no
 * filtering has been applied (e.g., component exclusion/inclusion filtering).
 */
public class RenameFunctions extends PostProcessingOperation<RenameFunctionsOptions> {
    /**
     * Construct {@link RenameFunctions} operation.
     * 
     * @param options Configuration of operation to construct.
     */
    public RenameFunctions(RenameFunctionsOptions options) {
        super(options);
    }

    @Override
    public PostProcessingPreconditionSubset getPreconditionSubset() {
        return new PostProcessingPreconditionSubset(false, false);
    }

    @Override
    public PostProcessingResultSubset getResultSubset() {
        return new PostProcessingResultSubset(false, false);
    }

    @Override
    public void applyOperation(Map<String, PostProcessingModel> models, Set<String> selectedComponents,
            Path relativeResolvePath, IProgressMonitor monitor)
    {
        monitor.subTask("Renaming functions");

        // Apply renaming to every model in 'models'.
        for (Entry<String, PostProcessingModel> entry: models.entrySet()) {
            String component = entry.getKey();
            PostProcessingModel model = entry.getValue();
            // Get an automaton representation of the current model.
            CompactDFA<String> dfa = model.getCompactDfa();

            // Apply renaming to every event in the automaton model.
            CompactNFA<String> nfa = AutomataLibUtil.dfaToNfa(dfa);
            CompactNFA<String> renamedNfa = AutomataLibUtil.rename(nfa,
                    eventName -> renameEvent(eventName, options.functionMappings));
            CompactDFA<String> renamedDfa = NFAs.determinize(renamedNfa, true, false);
            CompactDFA<String> minimalRenamedDfa = AutomataLibUtil.minimizeDFA(renamedDfa);

            models.put(component,
                    new PostProcessingModelCompactDfa(minimalRenamedDfa, component, getResultStatus(model.status)));
        }
    }

    /**
     * Given an event name {@code eventName} that complies to the CMI naming scheme, renames events based on a provided
     * mapping of old to new function names.
     * 
     * @param eventName The event name subject to renaming. Should be compliant to the CMI naming scheme.
     * @param functionMappings Mapping of old function names to new function names.
     * @return A CMI-compliant event name to which the specified renaming is applied, but is otherwise identical to
     *     {@code eventName}.
     */
    private String renameEvent(String eventName, Map<Pair<String, String>, Pair<String, String>> functionMappings) {
        EventInfo info = CmiGeneralEventQueries.getEventInfo(eventName);

        Pair<String, String> newFunctionName = functionMappings.get(Pair.pair(info.interfaceName, info.functionName));
        if (newFunctionName != null) {
            info = new EventInfo(info.declCompInfo, info.asyncDirection, newFunctionName.left, newFunctionName.right,
                    info.declType, info.declSide, info.otherType, info.otherSide, info.otherCompInfo);
        }

        return info.toString();
    }
}
