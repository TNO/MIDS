/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cif.extensions.mrr.cif;

import org.eclipse.escet.cif.common.CifEdgeUtils;
import org.eclipse.escet.cif.common.CifEventUtils;
import org.eclipse.escet.cif.common.CifLocationUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.java.Assert;

import com.google.common.base.Preconditions;

import nl.tno.mids.cif.extensions.mrr.data.MRR;

/** A CIF {@link MRR} letter. */
public class CifMrrLetter {
    /** The CIF automaton. */
    public final Automaton aut;

    /** The CIF source location of the edge, in the original CIF specification. */
    public final Location sourceLoc;

    /** The CIF target location of the edge, in the original CIF specification. */
    public final Location targetLoc;

    /** The CIF edge. */
    public final Edge edge;

    /** The CIF event. */
    public final Event event;

    /** The CIF event name. */
    public final String name;

    public CifMrrLetter(Edge edge) {
        this.sourceLoc = CifEdgeUtils.getSource(edge);
        this.targetLoc = CifEdgeUtils.getTarget(edge);
        this.aut = CifLocationUtils.getAutomaton(sourceLoc);
        this.edge = edge;

        // Exactly one event, not 'tau'.
        Preconditions.checkArgument(edge.getEvents().size() == 1); // Exactly one edge event (no tau, no multiple).
        EdgeEvent edgeEvent = edge.getEvents().get(0);
        this.event = CifEventUtils.getEventFromEdgeEvent(edgeEvent);
        Preconditions.checkNotNull(event); // No tau.

        name = CifTextUtils.getAbsName(event, false);
    }

    /**
     * Determines whether {@link #targetLoc} is (trivially) marked, i.e., is accepting.
     */
    public boolean isTargetLocMarked() {
        if (targetLoc.getMarkeds().isEmpty()) {
            return false;
        } else {
            // Make sure that all predicates in targetLoc.getMarkeds are trivial
            targetLoc.getMarkeds().stream().forEach(m -> Assert.check(CifValueUtils.hasSingleValue(m, false, true)));

            return CifValueUtils.isTriviallyTrue(targetLoc.getMarkeds(), false, true);
        }
    }
}
