/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.api.general

import java.util.List
import nl.tno.mids.cif.extensions.CifExtensions
import org.eclipse.escet.cif.common.CifCollectUtils
import org.eclipse.escet.cif.common.CifEventUtils
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent
import org.eclipse.escet.cif.metamodel.cif.Component
import org.eclipse.escet.cif.metamodel.cif.Group
import org.eclipse.escet.cif.metamodel.cif.Specification
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton
import org.eclipse.escet.cif.metamodel.cif.declarations.Event
import org.eclipse.escet.common.java.Sets

class CmiGeneralModifications {
    /**
     * Recursively removes all CIF groups within the given CIF {@link ComplexComponent} that are empty (i.e., that do
     * not have any declarations, nested sub-components, etc.). Note that the group emptiness check only considers CIF
     * constructs that are in-scope for CMI (so no component definition/instantiation, equations, etc.).
     * 
     * <p>
     * If {@code component} is itself an empty CIF group, it is not deleted.
     * </p>
     * 
     * @param component The component from which empty CIF groups are to be removed.
     */
    def static void removeEmptyGroups(ComplexComponent component) {
        if (component instanceof Group) {
            for (Component child : Sets.list2set(component.getComponents())) {
                removeEmptyGroups(child as ComplexComponent)

                if (child instanceof Group) {
                    if (child.declarations.isEmpty && child.invariants.empty && child.components.empty) {
                        component.components.remove(child)
                    }
                }
            }
        }
    }

    /**
     * Remove all events from a model that are not used in the model.
     * 
     * <p>The alphabet of all the automata are used to determine whether events are used or not.</p>
     * 
     * @param model Model from which to remove unused events.
     */
    def static void removeUnusedEvents(Specification model) {
        // Get all events that are used in the model, based on the alphabets of the automata.
        val List<Automaton> automata = newArrayList()
        CifCollectUtils.collectAutomata(model, automata)
        val events = automata.flatMap[CifEventUtils.getAlphabet(it)]

        // Remove unused event declarations.
        CifExtensions.removeDeclarations(model, [decl|decl instanceof Event && !events.contains(decl)])
    }
}
