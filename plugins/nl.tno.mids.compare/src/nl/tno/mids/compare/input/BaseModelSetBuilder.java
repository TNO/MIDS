/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.input;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.escet.cif.metamodel.cif.Specification;

import com.google.common.base.Preconditions;

import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.options.ModelType;

/**
 * Base functionality for model set builders.
 */
public abstract class BaseModelSetBuilder {
    final protected String modelSetName;

    final protected Map<String, Model> models = new TreeMap<String, Model>();

    /**
     * @param modelSetName Name of model set to build.
     */
    public BaseModelSetBuilder(String modelSetName) {
        this.modelSetName = modelSetName;
    }

    /**
     * Create model set from current set of models.
     * 
     * @return Model set created by this builder.
     */
    public ModelSet createModelSet() {
        return createModelSet(new HashMap<>());
    }

    /**
     * Create model set from current set of models with given descriptions.
     * 
     * @param descriptions Descriptions of the model set.
     * @return Model set created by this builder, with given descriptions.
     */
    public abstract ModelSet createModelSet(Map<Path, List<String>> descriptions);

    protected abstract ModelType getModelType();

    /**
     * Add multiple models to builder.
     * 
     * @param models Models to be added to this builder.
     * @return Model set builder containing the added models.
     */
    public BaseModelSetBuilder addAll(List<? extends Model> models) {
        models.forEach(m -> add(m));
        return this;
    }

    abstract void add(Specification cifSpecification, String specificationName, List<String> warnings);

    /**
     * Add {@link Model} to builder.
     * 
     * @param model Model to add.
     */
    public void add(Model model) {
        // Ensure no duplicate models.
        Preconditions.checkArgument(!models.containsKey(model.getEntityName()),
                String.format("Multiple models for %s in model set %s", model.getEntityName(), modelSetName));

        // Add model.
        this.models.put(model.getEntityName(), model);
    }

    protected abstract void validate();
}
