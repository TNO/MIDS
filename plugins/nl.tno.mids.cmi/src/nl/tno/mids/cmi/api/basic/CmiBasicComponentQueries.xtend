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

import org.eclipse.escet.cif.metamodel.cif.ComplexComponent
import org.eclipse.escet.cif.metamodel.cif.Group
import org.eclipse.escet.cif.metamodel.cif.Specification
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton
import org.eclipse.escet.common.java.Assert
import org.eclipse.escet.common.position.metamodel.position.PositionObject

class CmiBasicComponentQueries {
    /**
     * Does the given object represent a component?
     * 
     * @param object The object.
     * @return {@code true} if it represents a component, {@code false} otherwise.
     */
    static def isComponent(PositionObject object) {
        // Must be automaton or group.
        if (!(object instanceof Automaton || object instanceof Group)) {
            return false
        }

        // Must be directly contained in the specification.
        return object.eContainer instanceof Specification
    }

    /**
     * Get the component in which the given object is found. If the given object is a component, it is itself returned.
     * 
     * @param object The object.
     * @return The component.
     */
    static def getComponent(PositionObject object) {
        var PositionObject result = object
        if (result instanceof Specification || result === null) {
            throw new RuntimeException("Given object not contained in a component: " + object)
        }
        while (!isComponent(result)) {
            result = result.eContainer as PositionObject
            if (result instanceof Specification || result === null) {
                throw new RuntimeException("Given object not contained in a component: " + object)
            }
        }
        return result as ComplexComponent
    }

    /**
     * Get the components of the model.
     * 
     * <p>Components are CIF automata or CIF groups in the root of the model.</p>
     * 
     * @param model The model.
     * @return The components.
     */
    static def getComponents(Specification model) {
        // All components are top-level automata or groups of the CIF specification.
        // No other automata and groups must be present at the top-level.
        return model.components.filter[it.isComponent].map[it as ComplexComponent].toList
    }

    /**
     * Get the components with behavior of the model.
     * 
     * <p>This includes all components that are CIF automata.</p>
     * 
     * @param model The model.
     * @return The components.
     */
    static def getComponentsWithBehavior(Specification model) {
        val components = getComponents(model)
        return components.filter(Automaton).toList
    }

    /**
     * Get the single component with behavior of the model. There should be exactly one such component.
     * 
     * <p>This is the only component that is a CIF automaton.</p>
     * 
     * @param model The model.
     * @return The component.
     */
    static def getSingleComponentWithBehavior(Specification model) {
        val componentList = getComponentsWithBehavior(model);
        Assert.check(componentList.size() == 1);
        return componentList.head
    }

    /**
     * Get the components without behavior of the model.
     * 
     * <p>This includes all components that are CIF groups.</p>
     * 
     * @param model The model.
     * @return The components.
     */
    static def getComponentsWithoutBehavior(Specification model) {
        val components = getComponents(model)
        return components.filter(Group).toList
    }
}
