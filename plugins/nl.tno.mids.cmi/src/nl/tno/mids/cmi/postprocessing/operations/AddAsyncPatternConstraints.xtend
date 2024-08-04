/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.postprocessing.operations

import java.util.HashMap
import nl.tno.mids.cmi.api.general.CmiGeneralAsyncPatternQueries
import nl.tno.mids.cmi.api.info.EventInfo
import org.eclipse.escet.cif.metamodel.cif.automata.Edge
import org.eclipse.escet.cif.metamodel.cif.declarations.Event
import org.eclipse.xtend.lib.annotations.Accessors

/** Add constraints to the models to enforce asynchronous patterns (e.g. requests/replies). */
@Accessors
class AddAsyncPatternConstraints extends AddAsyncPatternConstraintsBase<AddAsyncPatternConstraintsOptions> {

    val HashMap<Event, EventInfo> cache = newHashMap

    override getTaskName() { "Add asynchronous pattern constraints (general)" }

    override isMatchingAsyncPatternEnd(Edge startEdge, Edge endEdge) {
        return CmiGeneralAsyncPatternQueries.isAsyncPatternPair(cache, startEdge, endEdge)
    }

    override isAsyncPatternStart(Edge edge) {
        return CmiGeneralAsyncPatternQueries.isAsyncPatternStart(edge)
    }

}
