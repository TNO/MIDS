/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.api.basic

import nl.tno.mids.cif.extensions.AutomatonExtensions
import nl.tno.mids.cif.extensions.EdgeExtensions
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries
import org.eclipse.escet.cif.common.CifTextUtils
import org.eclipse.escet.cif.metamodel.cif.Specification
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable
import org.eclipse.escet.cif.metamodel.cif.declarations.Event
import org.eclipse.escet.cif.metamodel.java.CifConstructors
import org.eclipse.escet.common.emf.EMFHelper
import org.eclipse.escet.common.java.Assert

class CmiBasicModifications {
    /**
     * Split component automata into separate service fragment automata, contained in a group that represents the
     * entire component.
     * 
     * <p>
     * Note that:
     * <ul>
     * <li>The resulting service fragment automata collectively do not have the same behavior as the original component
     * automaton, as the service fragments are no longer guaranteed to be mutually exclusive. They may interleave and
     * multiple of them may be 'active' at the same time.</li>
     * <li>The alphabets of the service fragment automata may be subsets of the original component automaton.</li>
     * <li>Variables shared between service fragments will no longer be shared between service fragment automata. 
     * Service fragments access local copies of variables instead. In particular, asynchronous constraints no longer 
     * constrain behavior as intended.</li>
     * </ul>
     * This transformation is intended for operations such a comparison or visualization where the state space of the
     * complete specification is not relevant.
     * </p>
     * 
     * @param model The specification containing components to be split into service fragment automata.
     *      Is modified in-place.
     */
    static def void splitServiceFragments(Specification model) {
        // Get top-level automata representing the components with behavior.
        val automata = CmiBasicComponentQueries.getComponentsWithBehavior(model)

        // First move declarations to the new groups.
        val automataGroupMap = newHashMap
        for (automaton : automata) {
            // Create new group for component, to replace component automaton.
            val group = CifConstructors.newGroup
            group.name = automaton.name
            model.components.add(group)

            // Move event declarations, to keep their absolute identities intact, and to avoid cloning them later on.
            val eventDecls = newArrayList
            eventDecls.addAll(automaton.declarations.filter(Event))
            group.declarations.addAll(eventDecls)

            // Move component invariants to keep them in the component rather than having them duplicated in each
            // service fragment automaton.
            group.invariants.addAll(automaton.invariants)
            automataGroupMap.put(automaton, group)
        }

        // Split components into service fragments.
        for (automaton : automata) {
            // Process each service fragment.
            CmiBasicServiceFragmentQueries.getServiceFragmentInitialEvents(automaton).forEach [ initialEvent |
                // During the transformation, the model is not fully split or non-split, so we cannot use the API to get
                // the event name. In this case, the absolute name should be equivalent.
                val eventName = CifTextUtils.getAbsName(initialEvent, false)
                Assert.check(CmiGeneralEventQueries.isValidEventName(eventName))
                val automatonGroup = automataGroupMap.get(automaton)
                automatonGroup.components.add(splitServiceFragment(eventName, automaton))
            ]

            // Remove original component automaton.
            model.components.remove(automaton)

        }
    }

    /**
     * Splits a single service fragment, creating a dedicated automaton for it.
     * 
     * <p>Note that references to declarations contained in the {@code scourceAutomaton} will be updated to refer to
     * copies contained in the resulting automaton. This ensures that local declarations shared between service
     * fragments will not be shared between service fragment automata.</p>
     * 
     * @param initialEventName The name of the initial event of the service fragment to split.
     * @param sourceAutomaton Automaton currently containing the service fragment.
     * @return Automaton containing the split service fragment.
     */
    private static def splitServiceFragment(String initialEventName, Automaton sourceAutomaton) {
        // Create new automaton for service fragment.
        val newAutomaton = EMFHelper.deepclone(sourceAutomaton)

        // Remove location and edges not part of the service fragment.
        val initialLocation = AutomatonExtensions.initialLocation(newAutomaton)
        initialLocation.edges.removeIf [
            !CifTextUtils.getAbsName(EdgeExtensions.getEventDecl(it, false), false).equals(initialEventName)
        ]
        AutomatonExtensions.removeUnreachableLocations(newAutomaton)

        // Remove discrete variables not used by this service fragment.
        val usedVariables = AutomatonExtensions.getReferencedDiscVars(newAutomaton)
        val notUsedVariables = newArrayList
        notUsedVariables.addAll(newAutomaton.declarations.filter(DiscVariable).filter[!usedVariables.contains(it)])
        notUsedVariables.forEach[EMFHelper.removeFromParentContainment(it)]

        // Rename locations to ensure consecutive numbering.
        AutomatonExtensions.renumberLocations(newAutomaton)

        // Give service fragment automaton its new name. Do this at the end to ensure absolute names were kept intact
        // until this moment.
        newAutomaton.name = initialEventName.replace(".", "_")

        // Return the new service fragment automaton.
        return newAutomaton
    }
}
