/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.compute;

import java.util.Set;

import com.google.common.base.Preconditions;

import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.LatticeEdge;
import nl.tno.mids.compare.data.LatticeEdgeAnnotation;
import nl.tno.mids.compare.data.LatticeNode;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.data.Variant;

/** Compute differences between related model sets. */
public class ComputeModelSetDifferences {
    private final ComparisonData comparisonData;

    /**
     * @param data {@link ComparisonData} containing entities with variants to compute differences for.
     */
    public ComputeModelSetDifferences(ComparisonData data) {
        this.comparisonData = data;
    }

    /** Compute differences between related model sets. */
    public void computeDifferences() {
        for (LatticeNode<Variant<ModelSet>> parentLatticeNode: comparisonData.getLattice()) {
            for (LatticeEdge<Variant<ModelSet>> childLatticeEdge: parentLatticeNode.getChildEdges()) {
                LatticeNode<Variant<ModelSet>> childLatticeNode = childLatticeEdge.getTarget();
                childLatticeEdge
                        .addAnnotation(computeLinkData(parentLatticeNode.getValue(), childLatticeNode.getValue()));
            }
        }
    }

    /**
     * Compute difference counts for two model set variants.
     * 
     * @param lhs One of the model set variants.
     * @param rhs The other model set variant.
     * @return Computed {@link LatticeEdgeAnnotation}.
     */
    private LatticeEdgeAnnotation computeLinkData(Variant<ModelSet> lhs, Variant<ModelSet> rhs) {
        // Obtain all left-hand-side (LHS) and right-hand-side (RHS) entities.
        Set<String> lhsEntities = lhs.getValue().getEntities();
        Set<String> rhsEntities = rhs.getValue().getEntities();
        Preconditions.checkArgument(lhsEntities.equals(rhsEntities));

        int added = 0;
        int changed = 0;
        int removed = 0;

        for (String entity: lhsEntities) {
            Variant<Model> lhsVariant = lhs.getValue().getEntityVariant(entity);
            Variant<Model> rhsVariant = rhs.getValue().getEntityVariant(entity);

            if (lhsVariant != rhsVariant) {
                if (!lhsVariant.getValue().hasBehavior()) {
                    added++;
                } else if (!rhsVariant.getValue().hasBehavior()) {
                    removed++;
                } else {
                    changed++;
                }
            }
        }

        return new LatticeEdgeAnnotation(added, changed, removed);
    }
}
