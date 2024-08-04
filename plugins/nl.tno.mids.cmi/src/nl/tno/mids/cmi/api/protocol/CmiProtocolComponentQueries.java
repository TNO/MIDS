/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.api.protocol;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.common.position.metamodel.position.PositionObject;

/**
 * Queries related to components in protocol models.
 */
public class CmiProtocolComponentQueries {
    /**
     * Get the component in which the given object is found. If the given object is a component, it is itself returned.
     * 
     * <p>
     * Components are CIF groups in the root of the model.
     * </p>
     * 
     * @param object The object.
     * @return The component.
     */
    public static Group getComponent(PositionObject object) {
        PositionObject result = object;
        if (result instanceof Specification || result == null) {
            throw new RuntimeException("Given object not contained in a component: " + object);
        }
        while (!isComponent(result)) {
            result = (PositionObject)result.eContainer();
            if (result instanceof Specification || result == null) {
                throw new RuntimeException("Given object not contained in a component: " + object);
            }
        }
        return (Group)result;
    }

    /**
     * Does the given object represent a component?
     * 
     * @param object The object.
     * @return {@code true} if it represents a component, {@code false} otherwise.
     */
    public static boolean isComponent(PositionObject object) {
        // Must be group.
        if (!(object instanceof Group)) {
            return false;
        }

        // Must be directly contained in the specification.
        return object.eContainer() instanceof Specification;
    }

    /**
     * Get the components without behavior of the model.
     * 
     * <p>
     * Components are CIF groups in the root of the model.
     * </p>
     * 
     * @param model The model.
     * @return The components.
     */
    public static List<Group> getComponentsWithoutBehavior(Specification model) {
        // All components are top-level groups of the CIF specification.
        // No other non-protocol automata and groups must be present at the top-level.
        return model.getComponents().stream().filter(c -> isComponent(c)).map(Group.class::cast)
                .collect(Collectors.toList());
    }
}
