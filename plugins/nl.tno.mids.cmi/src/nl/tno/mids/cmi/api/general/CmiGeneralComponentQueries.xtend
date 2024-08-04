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
import nl.tno.mids.cmi.api.basic.CmiBasicComponentQueries
import nl.tno.mids.cmi.api.info.ComponentInfo
import nl.tno.mids.cmi.api.protocol.CmiProtocolComponentQueries
import nl.tno.mids.cmi.api.protocol.CmiProtocolQueries
import nl.tno.mids.cmi.api.split.CmiSplitComponentQueries
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent
import org.eclipse.escet.cif.metamodel.cif.Group
import org.eclipse.escet.cif.metamodel.cif.Specification
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton
import org.eclipse.escet.common.java.Assert
import org.eclipse.escet.common.position.metamodel.position.PositionObject

class CmiGeneralComponentQueries {
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

        // Must not be a protocol.
        if (CmiProtocolQueries.isProtocol(object as ComplexComponent)) {
            return false;
        }

        // Must be directly contained in the specification.
        return object.eContainer instanceof Specification
    }

    /**
     * Get the component in which the given object is found. If the given object is a component, it is itself returned.
     * 
     * <p>Components are CIF automata or CIF groups in the root of the model.</p>
     * 
     * <p>Protocols are not components, even if they are represented as CIF automata in the root of the model. Objects
     * contained in protocols are not contained in any component.</p>
     * 
     * @param object The object.
     * @return The component.
     */
    protected static def ComplexComponent getComponent(PositionObject object) {
        val model = CmiGeneralQueries.getModel(object)
        val subset = CmiGeneralQueries.detectSubset(model)
        return getComponent(object, subset)
    }

    /**
     * Get the component in which the given object is found, assuming a given API subset. 
     * If the given object is a component, it is itself returned.
     * 
     * <p>Components are CIF automata or CIF groups in the root of the model.</p>
     * 
     * <p>Protocols are not components, even if they are represented as CIF automata in the root of the model. Objects
     * contained in protocols are not contained in any component.</p>
     * 
     * @param object The object.
     * @param subset The API subset of the model.
     * @return The component.
     */
    protected static def ComplexComponent getComponent(PositionObject object, CmiSubset subset) {
        switch (subset) {
            case PROTOCOL: return CmiProtocolComponentQueries.getComponent(object)
            case BASIC: return CmiBasicComponentQueries.getComponent(object)
            case SPLIT: return CmiSplitComponentQueries.getComponent(object)
            default: throw new RuntimeException("Unknown subset")
        }
    }

    /**
     * Get the components of the model.
     * 
     * <p>Components are CIF automata or CIF groups in the root of the model.</p>
     * 
     * <p>Protocols are not components, even if they are represented as CIF automata in the root of the model.</p>
     * 
     * @param model The model.
     * @return The components.
     */
    static def getComponents(Specification model) {
        // All components are top-level automata or groups of the CIF specification.
        // No other non-protocol automata and groups must be present at the top-level.
        return model.components.filter[it.isComponent].map[it as ComplexComponent].toList
    }

    /**
     * Get the components with behavior of the model.
     * 
     * <p>For 'basic' models, this includes all components that are CIF automata. For 'split' models, this includes all
     * components (CIF groups) that contain service fragments (CIF automata). Models in the 'protocol' subset do not
     * have components with behavior.</p>
     * 
     * @param model The model.
     * @return The components.
     */
    static def List<ComplexComponent> getComponentsWithBehavior(Specification model) {
        switch (CmiGeneralQueries.detectSubset(model)) {
            case PROTOCOL: return newArrayList
            case BASIC: return newArrayList(CmiBasicComponentQueries.getComponentsWithBehavior(model))
            case SPLIT: return newArrayList(CmiSplitComponentQueries.getComponentsWithBehavior(model))
            default: throw new RuntimeException("Unknown subset")
        }
    }

    /**
     * Get the components without behavior of the model.
     * 
     * <p>For 'basic' models, this includes all components that are CIF groups. For 'split' models, this includes all
     * components (CIF groups) that don't contain service fragments (CIF automata). For 'protocol' models, this includes
     * all components.</p>
     * 
     * @param model The model.
     * @return The components.
     */
    static def getComponentsWithoutBehavior(Specification model) {
        switch (CmiGeneralQueries.detectSubset(model)) {
            case PROTOCOL: return CmiProtocolComponentQueries.getComponentsWithoutBehavior(model)
            case BASIC: return CmiBasicComponentQueries.getComponentsWithoutBehavior(model)
            case SPLIT: return CmiSplitComponentQueries.getComponentsWithoutBehavior(model)
            default: throw new RuntimeException("Unknown subset")
        }
    }

    /**
     * Get the name of a component.
     * 
     * @param component The component.
     * @return The name of the component.
     */
    static def getComponentName(ComplexComponent component) {
        Assert.check(isComponent(component))
        Assert.check(component.eContainer instanceof Specification) // Components are top-level CIF automata/groups.
        return component.getName() // No parent, so can use non-absolute name.
    }

    /**
     * Returns information about the component, i.e. information about the parts of the component name.
     * 
     * @param component The component.
     * @return The component information.
     */
    static def getComponentInfo(ComplexComponent component) {
        return new ComponentInfo(getComponentName(component))
    }
}
