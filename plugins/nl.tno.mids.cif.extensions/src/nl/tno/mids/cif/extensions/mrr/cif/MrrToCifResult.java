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

import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;

import com.google.common.base.Preconditions;

public class MrrToCifResult {
    public final Edge startEdge;

    public final Edge endEdge;

    public final Location startLoc;

    public final Location endLoc;

    public MrrToCifResult(Edge startEdge, Edge endEdge, Location startLoc, Location endLoc) {
        this.startEdge = startEdge;
        this.endEdge = endEdge;
        this.startLoc = startLoc;
        this.endLoc = endLoc;

        // For both start and end, either an edge or a location, but not both.
        Preconditions.checkArgument((startEdge == null) != (startLoc == null));
        Preconditions.checkArgument((endEdge == null) != (endLoc == null));
    }

    public boolean startIsEdge() {
        return startEdge != null;
    }

    public boolean startIsLocation() {
        return startLoc != null;
    }

    public boolean endIsEdge() {
        return endEdge != null;
    }

    public boolean endIsLocation() {
        return endLoc != null;
    }
}
