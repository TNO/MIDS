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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import nl.tno.mids.compare.options.EntityType;

/**
 * Container for data related to a single compare run.
 */
public class ComparisonData {
    final List<ModelSet> modelSets;

    final List<Variant<ModelSet>> modelSetVariants = new ArrayList<>();

    final List<LatticeNode<Variant<ModelSet>>> lattice = new ArrayList<>();

    final SortedMap<String, Entity> entities = new TreeMap<>();

    private final EntityType entityType;

    private boolean modelSetLatticeIncomplete = false;

    /**
     * @param modelSets Input model sets of the comparison.
     */
    public ComparisonData(List<ModelSet> modelSets) {
        this.modelSets = modelSets;

        if (modelSets.isEmpty()) {
            this.entityType = new EntityType("entity", "entities");
            return;
        }

        Set<EntityType> entityTypes = modelSets.stream().map(ms -> ms.getEntityType()).collect(Collectors.toSet());

        Preconditions.checkState(entityTypes.size() >= 1, "Cannot determine entity type based on model sets.");

        Preconditions.checkArgument(entityTypes.size() <= 1,
                "Model sets found with different entity types, this is not supported.");

        this.entityType = entityTypes.iterator().next();

        Preconditions.checkNotNull(this.entityType, "Unable to identity entity type.");

        completeModelSets();
    }

    /**
     * @return The model sets.
     */
    public List<ModelSet> getModelSets() {
        return modelSets;
    }

    /**
     * @return The input model sets.
     */
    public List<ModelSet> getInputModelSets() {
        return modelSets.stream().filter(ms -> !ms.getModelSetVariant().computed).collect(Collectors.toList());
    }

    /**
     * @return The model set variants.
     */
    public List<Variant<ModelSet>> getModelSetVariants() {
        return modelSetVariants;
    }

    /**
     * @return The lattice relating model set variants.
     */
    public List<LatticeNode<Variant<ModelSet>>> getLattice() {
        return lattice;
    }

    /**
     * @return The entities.
     */
    public Collection<Entity> getEntities() {
        return entities.values();
    }

    /**
     * @return The names of all the entities.
     */
    public Set<String> getEntityNames() {
        return entities.keySet();
    }

    /**
     * @return The type of entities in the data.
     */
    public EntityType getEntityType() {
        return entityType;
    }

    /**
     * @return {@code true} if the variants of the model sets do not form a complete lattice, {@code false} otherwise.
     */
    public boolean isModelSetLatticeIncomplete() {
        return modelSetLatticeIncomplete;
    }

    /**
     * Mark variant lattice of model sets as incomplete.
     */
    public void setModelSetLatticeIncomplete() {
        this.modelSetLatticeIncomplete = true;
    }

    /**
     * Get {@link Entity} for a given entity name.
     * 
     * @param name Name of the {@link Entity} to retrieve.
     * @return Entity with the given name.
     */
    public Entity getEntityByName(String name) {
        return entities.computeIfAbsent(name, n -> new Entity(n));
    }

    /**
     * Set the number of each entity based on an ordering by entity name.
     */
    public void setEntityNumbers() {
        int i = 1;
        for (Entity entity: entities.values()) {
            entity.setNumber(i);
            i++;
        }
    }

    private void completeModelSets() {
        Set<String> entityNames = modelSets.stream().flatMap(modelSet -> modelSet.getEntities().stream())
                .collect(Collectors.toSet());
        for (ModelSet modelSet: modelSets) {
            modelSet.completeModelSet(entityNames);
        }
    }
}
