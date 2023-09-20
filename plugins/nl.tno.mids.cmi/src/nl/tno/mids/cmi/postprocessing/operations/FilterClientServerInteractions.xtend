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

import com.google.common.base.Preconditions
import java.nio.file.Path
import java.util.Map
import java.util.Set
import nl.tno.mids.cif.extensions.AutomatonExtensions
import nl.tno.mids.cif.extensions.EdgeExtensions
import nl.tno.mids.cmi.api.basic.CmiBasicComponentQueries
import nl.tno.mids.cmi.api.basic.CmiBasicServiceFragmentQueries
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries
import nl.tno.mids.cmi.api.general.CmiGeneralModifications
import nl.tno.mids.cmi.postprocessing.PostProcessingModel
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCifSpec
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.escet.cif.metamodel.cif.Specification
import org.eclipse.escet.cif.metamodel.cif.automata.Edge
import org.eclipse.escet.common.emf.EMFHelper
import org.eclipse.xtend.lib.annotations.Accessors

/**  Filter models to keep only the interactions between two components (i.e. a client and server). */
@Accessors
class FilterClientServerInteractions extends PostProcessingOperation<FilterClientServerInteractionsOptions> {
    override getPreconditionSubset() { return new PostProcessingPreconditionSubset(false, false); }

    override getResultSubset() { return new PostProcessingResultSubset(false, false); }

    override applyOperation(Map<String, PostProcessingModel> models, Set<String> selectedComponents,
        Path relativeResolvePath, IProgressMonitor monitor) {

        monitor.subTask("Filtering client/server interactions: " + options.componentName1 + " and " +
            options.componentName2)
        models.forEach[component, model|preconditionSubset.ensureSubset(model)]

        // Remove all other components.
        val componentsToRemove = newHashSet
        models.filter[k, v|k != options.componentName1 && k != options.componentName2].forEach [ k, v |
            componentsToRemove.add(k)
        ]
        componentsToRemove.forEach [ k |
            models.remove(k)
        ]

        // Apply communication filtering to preserved component models.
        val component1 = models.get(options.componentName1)
        val component2 = models.get(options.componentName2)

        Preconditions.checkNotNull(component1, "Component not found: " + options.componentName1)
        Preconditions.checkNotNull(component2, "Component not found: " + options.componentName2)

        val cifSpec1 = component1.cifSpec
        filterInteractions(cifSpec1, options.componentName2)
        val cifSpec2 = component2.cifSpec
        filterInteractions(cifSpec2, options.componentName1)

        models.put(options.componentName1, new PostProcessingModelCifSpec(cifSpec1, options.componentName1,
            getResultStatus(component1.status)))
        models.put(options.componentName2, new PostProcessingModelCifSpec(cifSpec2, options.componentName2,
            getResultStatus(component2.status)))
    }

    private def void filterInteractions(Specification model, String otherComponentName) {
        val automaton = CmiBasicComponentQueries.getSingleComponentWithBehavior(model)
        val initialEdges = CmiBasicServiceFragmentQueries.getServiceFragmentInitialEdges(automaton)

        val edgesToRemove = newHashSet

        for (initialEdge : initialEdges) {
            val fragmentEdges = CmiBasicServiceFragmentQueries.getServiceFragmentEdges(automaton, initialEdge)

            if (!fragmentEdges.exists[isCommunicationWith(it, otherComponentName)]) {
                edgesToRemove += initialEdge
            }
        }

        edgesToRemove.forEach[EMFHelper.removeFromParentContainment(it)]
        AutomatonExtensions.removeUnreachableLocations(automaton)
        if (automaton.alphabet !== null) {
            AutomatonExtensions.updateAlphabet(automaton)
        }

        // Remove unused event declarations.
        CmiGeneralModifications.removeUnusedEvents(model)

        // Remove non-behavior components with no remaining contribution.
        CmiGeneralModifications.removeEmptyGroups(model)
    }

    // Does given event involve communication with the given component?
    private static def isCommunicationWith(Edge edge, String componentName) {
        val event = EdgeExtensions.getEventDecl(edge, false)

        val eventInfo = CmiGeneralEventQueries.getEventInfo(event)
        return eventInfo.declCompInfo.toString == componentName ||
            (eventInfo.otherCompInfo !== null && eventInfo.otherCompInfo.toString == componentName)
    }
}
