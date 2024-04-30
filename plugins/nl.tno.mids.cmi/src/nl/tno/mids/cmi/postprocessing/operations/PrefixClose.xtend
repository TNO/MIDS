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

import java.nio.file.Path
import java.util.Map
import java.util.Set
import net.automatalib.util.automata.fsa.DFAs
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil
import nl.tno.mids.cmi.postprocessing.PostProcessingModel
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCompactDfa
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.xtend.lib.annotations.Accessors

@Accessors
class PrefixClose extends PostProcessingOperation<PrefixCloseOptions> {

    override getPreconditionSubset() { return new PostProcessingPreconditionSubset(false, null); }

    override getResultSubset() { return new PostProcessingResultSubset(false, null); }

    override applyOperation(Map<String, PostProcessingModel> models, Set<String> selectedComponents,
        Path relativeResolvePath, IProgressMonitor monitor) {

        monitor.subTask("Making prefix closed")
        for (component : selectedComponents) {
            val model = models.get(component)
            preconditionSubset.ensureSubset(model)
            val dfa = model.compactDfa

            if (!DFAs.isPrefixClosed(dfa, dfa.inputAlphabet)) {
                AutomataLibUtil.prefixClose(dfa)
                models.put(component, new PostProcessingModelCompactDfa(dfa, model.name, getResultStatus(model.status)))
            }
        }
    }
}
