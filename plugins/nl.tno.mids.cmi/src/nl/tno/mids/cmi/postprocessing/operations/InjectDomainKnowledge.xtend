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
import java.nio.file.Paths
import java.util.Map
import java.util.Set
import net.automatalib.automata.fsa.impl.compact.CompactDFA
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil
import nl.tno.mids.cif.extensions.FileExtensions
import nl.tno.mids.cmi.postprocessing.PostProcessingModel
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCifSpec
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCompactDfa
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.SubMonitor
import org.eclipse.xtend.lib.annotations.Accessors

@Accessors
class InjectDomainKnowledge extends PostProcessingOperation<InjectDomainKnowledgeOptions> {
    override getPreconditionSubset() { return new PostProcessingPreconditionSubset(false, false); }

    override getResultSubset() { return new PostProcessingResultSubset(false, false); }

    override applyOperation(Map<String, PostProcessingModel> models, Set<String> selectedComponents,
        Path relativeResolvePath, IProgressMonitor monitor) {

        val subMonitor = SubMonitor.convert(monitor, selectedComponents.size)
        for (component : selectedComponents) {
            val previousModel = models.get(component)
            preconditionSubset.ensureSubset(previousModel)

            val newDfa = injectDomainKnowledge(previousModel.getCompactDfa(), relativeResolvePath, options,
                subMonitor.split(1))
            if (newDfa !== null) {
                // Domain knowledge injection did apply. Replace model.
                models.put(component,
                    new PostProcessingModelCompactDfa(newDfa, component, getResultStatus(previousModel.status)))
            }
        }
    }

    /**
     * Apply domain knowledge injection.
     * 
     * @param previousDfa The DFA on which to inject the domain knowledge.
     * @param relativeResolvePath The absolute path of the input file. During post-processing, all relative paths are
     *      to be resolved against the directory that contains this file.
     * @param options The domain knowledge injection options.
     * @param monitor The progress monitor.
     * @return The DFA resulting from injection, or {@code null} if domain knowledge injection did not apply.
     */
    private static def injectDomainKnowledge(CompactDFA<String> previousDfa, Path relativeResolvePath,
        InjectDomainKnowledgeOptions options, IProgressMonitor monitor) {

        val subMonitor = SubMonitor.convert(monitor, "Injecting domain knowledge: " + options.toString, 71)

        // Load CIF model containing domain knowledge to inject.
        subMonitor.split(10)
        val absOrRelInjectPath = Paths.get(options.modelPath)
        val absInjectResolvePath = relativeResolvePath.parent
        val absInjectPath = absInjectResolvePath.resolve(absOrRelInjectPath).normalize
        var injectSpec = FileExtensions.loadCIF(absInjectPath)

        // Get DFA containing domain knowledge to inject.
        // We need it as a plain DFA to allow AutomataLib operations.
        // We don't currently support 'tau' events, as 'tau' does not synchronize, which is a special case.
        subMonitor.split(10)
        val injectionModel = new PostProcessingModelCifSpec(injectSpec, "<inject>",
            new PostProcessingStatus(true, true))
        val ensuredInjectionModel = new PostProcessingPreconditionSubset(false, false).ensureSubset(injectionModel)
        val injectDfa = ensuredInjectionModel.getCompactDfa()

        // Check DFA to inject for no accepting states (empty language).
        subMonitor.split(1)
        if (injectDfa.states.stream.allMatch[!injectDfa.isAccepting(it)]) {
            throw new RuntimeException("Domain knowledge model \"" + options.modelPath +
                "\" does not have any accepting states. Ensure the CIF model contains appropriate marking.")
        }

        // Apply operation.
        subMonitor.split(50)
        val newDfa = switch (options.operator) {
            case DIFFERENCE_LEFT:
                AutomataLibUtil.differenceMinimized(injectDfa, previousDfa)
            case DIFFERENCE_RIGHT:
                AutomataLibUtil.differenceMinimized(previousDfa, injectDfa)
            case EXCLUSIVE_OR:
                AutomataLibUtil.xorMinimized(previousDfa, injectDfa)
            case INTERSECTION:
                AutomataLibUtil.intersectionMinimized(previousDfa, injectDfa)
            case PARALLEL_COMPOSITION:
                AutomataLibUtil.parallelCompositionMinimized(previousDfa, injectDfa)
            case UNION:
                AutomataLibUtil.unionMinimized(previousDfa, injectDfa)
            default:
                throw new RuntimeException("Unknown operator: " + options.operator)
        }

        // Return result.
        return newDfa
    }
}
