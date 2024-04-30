/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import nl.tno.mids.compare.options.EntityType;
import nl.tno.mids.compare.options.ModelType;

/**
 * Set of related input models.
 */
public class ModelSet {
    final String name;

    final Map<Path, List<String>> descriptions;

    final Map<String, Model> models;

    Variant<ModelSet> modelSetVariant = null;

    private final ModelType modelType;

    private final EntityType entityType;

    /**
     * @param name Name of model set.
     * @param models Mapping of entity names to models that are part of the model set.
     * @param modelType Type of models in model set.
     * @param entityType Type of entities in model set.
     * @param descriptions Descriptions of the model set.
     */
    public ModelSet(String name, Map<String, Model> models, ModelType modelType, EntityType entityType,
            Map<Path, List<String>> descriptions)
    {
        this.name = name;
        this.descriptions = descriptions;
        this.models = models;
        this.modelType = modelType;
        this.entityType = entityType;
    }

    /**
     * @return The models in this model set.
     */
    public List<Model> getModels() {
        return new ArrayList<>(models.values());
    }

    /**
     * @return The models with behavior in this model set.
     */
    public List<Model> getModelsWithBehavior() {
        return models.values().stream().filter(model -> model.hasBehavior()).collect(Collectors.toList());
    }

    /**
     * @return The compare subset the model belongs to.
     */
    public ModelType getModelType() {
        return modelType;
    }

    /**
     * @return The type of entities in this model set.
     */
    public EntityType getEntityType() {
        return entityType;
    }

    /**
     * @return The entities.
     */
    public Set<String> getEntities() {
        return models.keySet();
    }

    /**
     * Retrieve the model for a given entity.
     * 
     * @param entityName The entity name.
     * @return {@link Model} for the given entity in this {@link ModelSet}, or {@code null} if there is no model in the
     *     model set for the entity.
     */
    public Model getEntityModel(String entityName) {
        return models.get(entityName);
    }

    /**
     * Add empty models for missing entities to model set.
     * 
     * @param entityNames Names of entities which should be present in the model set.
     */
    void completeModelSet(Set<String> entityNames) {
        Set<String> missingEntities = Sets.difference(entityNames, getEntities());
        for (String missingEntity: missingEntities) {
            models.put(missingEntity, new Model(missingEntity));
        }
    }

    /**
     * Retrieve the variant for the given entity present in this model set.
     * 
     * @param entityName The entity name.
     * @return {@link Variant} for the given entity in this {@link ModelSet}.
     */
    public Variant<Model> getEntityVariant(String entityName) {
        Preconditions.checkArgument(models.containsKey(entityName),
                entityType.getCapitalizedName() + " " + entityName + " not present in model set " + name);
        return getEntityModel(entityName).getVariant();
    }

    /**
     * @return The name of the model set.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The size of the model set.
     */
    public int getNumberOfModels() {
        return models.size();
    }

    /**
     * @return The size of the model set, counting only models with behavior.
     */
    public int getNumberOfModelsWithBehavior() {
        return getModelsWithBehavior().size();
    }

    /**
     * @return The model set variant.
     */
    public Variant<ModelSet> getModelSetVariant() {
        return modelSetVariant;
    }

    /**
     * @param modelSetVariant The model set variant to set.
     */
    public void setModelSetVariant(Variant<ModelSet> modelSetVariant) {
        this.modelSetVariant = modelSetVariant;
    }

    /**
     * @return The descriptions of the model set.
     */
    public Map<Path, List<String>> getDescriptions() {
        return descriptions;
    }

    /**
     * Compute the number of different entities for two given model sets.
     * 
     * @param left {@link ModelSet} describing one model set.
     * @param right {@link ModelSet} describing other model set.
     * @return Number of different entities.
     */
    public static int getDifferenceCount(ModelSet left, ModelSet right) {
        int differenceCount = 0;

        // Model sets have the same entities, so get the entities from left.
        for (String entity: left.getEntities()) {
            Variant<Model> leftVariant = left.getEntityModel(entity).getVariant();
            Variant<Model> rightVariant = right.getEntityModel(entity).getVariant();

            if (leftVariant != rightVariant) {
                differenceCount += 1;
            }
        }

        return differenceCount;
    }
}
