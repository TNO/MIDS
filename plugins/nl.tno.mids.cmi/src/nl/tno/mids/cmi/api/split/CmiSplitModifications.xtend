/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.api.split

import org.eclipse.escet.cif.metamodel.cif.Group
import org.eclipse.escet.cif.metamodel.java.CifConstructors
import org.eclipse.escet.common.java.Assert

class CmiSplitModifications {
    /**
     * Move service fragments starting with event subscription or event unsubscription to a subgroup.
     * 
     * @param component Component containing service fragments to move.
     */
    static def groupEventSubUnsubFragments(Group component) {
        // Precondition check: component is really a component.
        Assert.check(CmiSplitComponentQueries.isComponent(component),
            "Group " + component.name + " is not a component.")

        // Get all service fragments.
        val serviceFragments = CmiSplitServiceFragmentQueries.getServiceFragments(component)

        // Create new group.
        val newGroup = CifConstructors.newGroup
        newGroup.name = "EventSubscriptionsAndUnsubscriptions"

        // Add event (un)subscription service fragments to new group.
        serviceFragments.forEach [ serviceFragment |
            if (CmiSplitServiceFragmentQueries.isEventSubscriptionOrUnsubscriptionServiceFragment(serviceFragment)) {
                newGroup.components.add(serviceFragment)
            }
        ]

        // Only add new group to new specification if it contains any moved service fragments.
        // Prevents adding new empty group.
        if (!newGroup.components.empty) {
            component.components.add(newGroup)
        }
    }
}
