/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.postprocessing.status;

import org.eclipse.escet.cif.metamodel.cif.Specification;

import nl.tno.mids.cif.extensions.CIFOperations;
import nl.tno.mids.cmi.postprocessing.PostProcessingModel;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCifSpec;

/**
 * Functionality to remove data during post-processing.
 */
public class EnsureNoData {
    /**
     * Remove data from a given {@link PostProcessingModel} if present.
     * 
     * @param postProcessingModel {@link PostProcessingModel} to process.
     * @return a {@link PostProcessingModel} containing the same behavior as the input, but without variables, guards or
     *     updates.
     */
    public static PostProcessingModel ensureNoData(PostProcessingModel postProcessingModel) {
        if (postProcessingModel.status.dataIsNotPresent()) {
            return postProcessingModel;
        }

        // Remove data by converting to state space.
        Specification newSpec = CIFOperations.convertToStateSpace(postProcessingModel.getCifSpec());
        CIFOperations.renameAutomaton(newSpec, "statespace", postProcessingModel.name);
        return new PostProcessingModelCifSpec(newSpec, postProcessingModel.name,
                ensureNoData(postProcessingModel.status));
    }

    /**
     * Remove data from a given {@link PostProcessingStatus} if present.
     * 
     * @param postProcessingStatus {@link PostProcessingStatus} to process.
     * @return a {@link PostProcessingStatus} with tau as input and without data.
     */
    public static PostProcessingStatus ensureNoData(PostProcessingStatus postProcessingStatus) {
        if (postProcessingStatus.dataIsNotPresent()) {
            return postProcessingStatus;
        }

        return new PostProcessingStatus(false, postProcessingStatus.tauIsPresent());
    }
}
