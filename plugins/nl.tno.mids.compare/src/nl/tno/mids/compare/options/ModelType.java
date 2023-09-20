/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.options;

import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.input.BaseModelSetBuilder;
import nl.tno.mids.compare.input.CifModelSetBuilder;
import nl.tno.mids.compare.input.CmiModelSetBuilder;

/** Type of models to compare. */
public enum ModelType {
    /** CIF - Plain CIF models. */
    CIF("CIF models"),

    /** CMI - Models constructed by Constructive Model Inference. */
    CMI("CMI models");

    /** Description of model type. */
    public final String description;

    ModelType(String description) {
        this.description = description;
    }

    /**
     * Return the model set builder corresponding to the receiving {@link ModelType}.
     * 
     * @param modelSetName The name of the {@link ModelSet} to be returned by the {@link BaseModelSetBuilder}.
     * @param options The compare options.
     * @return A model set builder corresponding to this model type.
     */
    public BaseModelSetBuilder getModelSetBuilder(String modelSetName, CompareOptions options) {
        switch (this) {
            case CIF:
                return new CifModelSetBuilder(modelSetName, options.entityType);
            case CMI:
                return new CmiModelSetBuilder(modelSetName, options.cmiCompareMode, options.entityType);
            default:
                throw new RuntimeException("Model set builder unknown.");
        }
    }
}
