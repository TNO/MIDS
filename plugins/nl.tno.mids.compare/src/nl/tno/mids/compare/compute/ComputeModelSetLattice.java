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

import org.eclipse.core.runtime.IProgressMonitor;

import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.Entity;
import nl.tno.mids.compare.data.LatticeNode;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.data.Variant;
import nl.tno.mids.compare.options.EntityType;

/** Compute relations between model sets based on entity variant lattices and encode them in a lattice. */
public class ComputeModelSetLattice extends ComputeLattice<ModelSet> {
    private final ComparisonData comparisonData;

    private final EntityType entityType;

    /**
     * @param comparisonData Comparison data containing model sets to relate.
     */
    public ComputeModelSetLattice(ComparisonData comparisonData) {
        this.comparisonData = comparisonData;
        this.entityType = comparisonData.getEntityType();
    }

    /**
     * Compute a variant inclusion lattice, based on all model set variants.
     * <p>
     * Every arrow in a lattice amounts to language inclusion of the models of the source model set in the models of the
     * target model set.
     * </p>
     * 
     * @param monitor Progress monitor to track execution.
     */
    public void computeModelSetLattice(IProgressMonitor monitor) {
        comparisonData.getLattice().addAll(computeLattice(comparisonData.getModelSetVariants()));
    }

    @Override
    protected boolean inPartialOrder(Variant<ModelSet> modelSetVariant, Variant<ModelSet> otherModelSetVariant) {
        ModelSet modelSet = modelSetVariant.getValue();
        ModelSet otherModelSet = otherModelSetVariant.getValue();

        for (String entityName: modelSet.getEntities()) {
            Entity entity = comparisonData.getEntityByName(entityName);

            // For every child model, check if there is a parent that includes its behavior.
            Model childModel = otherModelSet.getEntityModel(entityName);
            LatticeNode<Variant<Model>> childLatticeNode = findLatticeNode(entity, entityType, childModel);
            Model possParentModel = modelSet.getEntityModel(entityName);
            LatticeNode<Variant<Model>> possParentLatticeNode = findLatticeNode(entity, entityType, possParentModel);
            if (childLatticeNode != possParentLatticeNode && !childLatticeNode.isDescendantOf(possParentLatticeNode)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Find lattice node corresponding to a given model.
     * 
     * @param entity Entity containing lattice to search.
     * @param entityType Type of entity contained in lattice.
     * @param model Model to find.
     * @return Lattice node containing model that is language equivalent to the given DFA.
     * @throws IllegalStateException If the requested lattice node does not exist.
     */
    static LatticeNode<Variant<Model>> findLatticeNode(Entity entity, EntityType entityType, Model model) {
        for (LatticeNode<Variant<Model>> latticeNode: entity.getLattice()) {
            if (model.getVariant() == latticeNode.getValue()) {
                return latticeNode;
            }
        }

        throw new IllegalStateException(
                entityType.getCapitalizedName() + " " + entity.getName() + " has an invalid lattice.");
    }
}
