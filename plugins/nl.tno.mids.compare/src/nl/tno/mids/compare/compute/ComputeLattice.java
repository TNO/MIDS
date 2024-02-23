/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.compute;

import java.util.ArrayList;
import java.util.List;

import nl.tno.mids.compare.data.LatticeEdge;
import nl.tno.mids.compare.data.LatticeNode;
import nl.tno.mids.compare.data.Variant;

/**
 * Compute relations between variants based on partial order and encode them in a lattice.
 * 
 * @param <T> Type of values contained in the variants.
 */
public abstract class ComputeLattice<T> {
    /**
     * Compute variant inclusion relations for a given list of distinct variants.
     * 
     * @param variants Variants to compute inclusion relations for.
     * @return The nodes of the new lattice.
     */
    protected List<LatticeNode<Variant<T>>> computeLattice(List<Variant<T>> variants) {
        List<LatticeNode<Variant<T>>> lattice = new ArrayList<>(variants.size());
        for (Variant<T> variant: variants) {
            LatticeNode<Variant<T>> latticeNode = new LatticeNode<>(variant);
            for (LatticeNode<Variant<T>> otherLatticeNode: lattice) {
                if (!latticeNode.isAncestorOf(otherLatticeNode) && !latticeNode.isDescendantOf(otherLatticeNode)
                        && inPartialOrder(latticeNode.getValue(), otherLatticeNode.getValue()))
                {
                    new LatticeEdge<>(latticeNode, otherLatticeNode);
                } else if (!otherLatticeNode.isAncestorOf(latticeNode) && !otherLatticeNode.isDescendantOf(latticeNode)
                        && inPartialOrder(otherLatticeNode.getValue(), latticeNode.getValue()))
                {
                    new LatticeEdge<>(otherLatticeNode, latticeNode);
                }
            }
            lattice.add(latticeNode);
        }
        return lattice;
    }

    /**
     * Determine whether the behavior of one variant is contained in that of another.
     * 
     * <p>
     * The relation defined by this operation should be transitive, reflexive and anti-symmetric.
     * </p>
     * 
     * @param variant Base variant to compare.
     * @param otherVariant Other variant to compare.
     * @return {@code true} if the behavior of {@code variant} is contained in the behavior of {@code otherVariant},
     *     {@code false} if it is not contained in that way.
     */
    protected abstract boolean inPartialOrder(Variant<T> variant, Variant<T> otherVariant);
}
