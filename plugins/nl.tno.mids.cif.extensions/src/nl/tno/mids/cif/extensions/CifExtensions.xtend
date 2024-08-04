/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cif.extensions

import com.google.common.base.Predicate
import java.util.Collection
import java.util.Set
import org.eclipse.emf.common.util.ECollections
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent
import org.eclipse.escet.cif.metamodel.cif.Component
import org.eclipse.escet.cif.metamodel.cif.Group
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton
import org.eclipse.escet.cif.metamodel.cif.declarations.Declaration
import org.eclipse.escet.common.java.Sets

import static extension nl.tno.mids.cif.extensions.AutomatonExtensions.*

class CifExtensions {

    /**
     * Returns a LinkedHashSet containing all automata within the complex component
     */
    def static Set<Automaton> allAutomata(ComplexComponent comp) {
        val lst = newLinkedHashSet
        return lst.addAutomata(comp)
    }

    /**
     * Adds all automata declared in the given component (recursively).
     * 
     * <p>Does not support component definition/instantiation (throws {@code UnsupportedOperationException}).</p>
     * 
     * @param automata The collection of automata, modified in place. 
     * @param comp The component.
     */
    private def static <C extends Collection<Automaton>> C addAutomata(C automata, Component comp) {
        switch (comp) {
            Automaton: automata.add(comp)
            Group: comp.components.forEach[automata.addAutomata(it)]
            default: throw new UnsupportedOperationException("addAutomata is not defined for type" + comp.class.name)
        }
        return automata
    }

    /**
     * Recursively removes all declarations in {@code component} that satisfy the given {@code predicate}.
     * 
     * @param component The component in which declarations are to be removed.
     * @param predicate The predicate that determines which declarations to remove.
     */
    def static void removeDeclarations(ComplexComponent component, Predicate<Declaration> predicate) {
        // Remove any declarations that are disallowed by the predicate.
        for (Declaration decl : Sets.list2set(component.getDeclarations())) {
            if (predicate.apply(decl)) {
                component.getDeclarations().remove(decl)
            }
        }

        // Remove recursively.
        if (component instanceof Group) {
            for (Component child : component.getComponents()) {
                removeDeclarations(child as ComplexComponent, predicate)
            }
        }
    }

    /**
     * Normalize the order of contained elements in a component.
     * 
     * <p>For complex components, sorts declarations.</p>
     * 
     * <p>For groups, additionally sorts nested components.</p>
     * 
     * <p>For automata, additionally {@link AutomatonExtensions#normalizeLocations normalizes} locations and edges.</p>
     * 
     * @param component {@link Component} containing elements to sort.
     */
    def static void normalizeOrder(Component component) {

        if (component instanceof ComplexComponent) {
            component.sortDeclarationsByType
            if (component instanceof Group) {
                ECollections.sort(component.components, [l, r|l.name.compareTo(r.name)])
                ECollections.sort(component.components, [l, r|l.eClass.name.compareTo(r.eClass.name)])

                component.components.forEach[normalizeOrder]
            } else if (component instanceof Automaton) {
                component.normalizeLocations
            }

        }
    }

    /**
     * Sort declarations in a complex component based on type and name.
     * 
     * @param complexComponent {@link ComplexComponent} containing declarations to sort.
     */
    private def static sortDeclarationsByType(ComplexComponent component) {
        ECollections.sort(component.declarations, [l, r|l.name.compareTo(r.name)])
        ECollections.sort(component.declarations, [l, r|l.eClass.name.compareTo(r.eClass.name)])
    }
}
