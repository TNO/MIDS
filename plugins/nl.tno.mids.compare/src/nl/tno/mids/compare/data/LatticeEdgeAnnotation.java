/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.data;

/** Difference information for a lattice edge. */
public class LatticeEdgeAnnotation {
    private final int added;

    private final int changed;

    private final int removed;

    /**
     * Construct lattice edge annotation.
     * 
     * @param added The number of added objects.
     * @param changed The number of changed objects.
     * @param removed The number of removed objects.
     */
    public LatticeEdgeAnnotation(int added, int changed, int removed) {
        this.added = added;
        this.changed = changed;
        this.removed = removed;
    }

    /**
     * @return The number of added objects.
     */
    public int getAdded() {
        return added;
    }

    /**
     * @return The number of changed objects.
     */
    public int getChanged() {
        return changed;
    }

    /**
     * @return The number of removed objects.
     */
    public int getRemoved() {
        return removed;
    }
}
