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

import nl.tno.mids.cmi.api.basic.CmiBasicServiceFragmentQueries
import nl.tno.mids.cmi.api.protocol.CmiProtocolQueries
import nl.tno.mids.cmi.api.split.CmiSplitServiceFragmentQueries
import org.eclipse.escet.cif.common.CifScopeUtils
import org.eclipse.escet.cif.metamodel.cif.Specification
import org.eclipse.escet.cif.metamodel.cif.declarations.Event
import org.eclipse.escet.common.java.Assert
import org.eclipse.escet.common.position.metamodel.position.PositionObject

class CmiGeneralQueries {
    /**
     * Get the model in which the given object is found. If the given object is a model, it is itself returned.
     * 
     * @return The model.
     */
    static def getModel(PositionObject object) {
        return CifScopeUtils.getSpecRoot(CifScopeUtils.getScope(object))
    }

    /**
     * Does the given model use synchronous component composition (naming)?
     * 
     * @param model The model.
     * @return {@code true} if the model uses synchronous component composition (naming), {@code false} if
     *      it uses asynchronous component composition (naming).
     */
    static def usesSynchronousComposition(Specification model) {
        var Boolean synchronous = null;
        val events = CmiGeneralEventQueries.getEvents(model)
        Assert.check(!events.isEmpty,
            "Can't determine whether model uses synchronous composition: model has no events.")
        for (Event event : events) {
            val info = CmiGeneralEventQueries.getEventInfo(event)
            if (info.asyncDirection !== null) {
                Assert.check(synchronous != true) // null or false
                synchronous = false
            } else {
                Assert.check(synchronous != false) // null or true
                synchronous = true
            }
        }
        return synchronous
    }

    /**
     * Detects the subset of the given model.
     * 
     * @param model The model.
     * @return The subset.
     */
    static def detectSubset(Specification model) {
        val possibleSubsets = newHashSet
        if (CmiProtocolQueries.isProtocolCmiModel(model)) {
            possibleSubsets.add(CmiSubset.PROTOCOL)
        }
        if (CmiBasicServiceFragmentQueries.isBasicCmiModelWithNoSplitServiceFragments(model)) {
            possibleSubsets.add(CmiSubset.BASIC)
        }
        if (CmiSplitServiceFragmentQueries.isSplitCmiModelWithOnlySplitServiceFragments(model)) {
            possibleSubsets.add(CmiSubset.SPLIT)
        }
        Assert.check(!possibleSubsets.empty, "Model is not in any subset, and thus not a valid CMI model.")
        Assert.check(possibleSubsets.size < 2,
            "Model is in multiple subsets: " + possibleSubsets + ". Subset detection thus has a bug.")
        return possibleSubsets.head;
    }
}
