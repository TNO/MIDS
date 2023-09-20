/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.postprocessing.status;

import com.google.common.base.Preconditions;

import nl.tno.mids.cmi.postprocessing.PostProcessingModel;

class EnsureSubset {
    /**
     * Ensure that the postProcessingModel fits the requested subset. If it already does, it is returned unmodified. If
     * it does not yet satisfy the requested subset, it is converted to fit the subset.
     * 
     * @param postProcessingModel
     * @param subsetToEnsure
     */
    static PostProcessingModel ensureSubset(PostProcessingModel postProcessingModel,
            PostProcessingPreconditionSubset subsetToEnsure)
    {
        // Check arguments.
        Preconditions.checkArgument(subsetToEnsure.dataIsOptional() || subsetToEnsure.dataIsNotAllowed());
        Preconditions.checkArgument(subsetToEnsure.tauIsOptional() || subsetToEnsure.tauIsNotAllowed());

        // Ensure no data is present.
        if (postProcessingModel.status.dataIsPresent() && subsetToEnsure.dataIsNotAllowed()) {
            return ensureSubset(EnsureNoData.ensureNoData(postProcessingModel), subsetToEnsure);
        }

        // Ensure no tau is present.
        if (postProcessingModel.status.tauIsPresent() && subsetToEnsure.tauIsNotAllowed()) {
            return ensureSubset(EnsureNoTau.ensureNoTau(postProcessingModel), subsetToEnsure);
        }

        // Already in requested subset.
        return postProcessingModel;
    }

    /**
     * Ensure that the postProcessingStatus reflects the requested subset. If it already does, it is returned
     * unmodified. If it does not yet satisfy the requested subset, it is converted to fit the subset.
     * 
     * @param postProcessingStatus
     * @param subsetToEnsure
     */
    static PostProcessingStatus ensureSubset(PostProcessingStatus postProcessingStatus,
            PostProcessingPreconditionSubset subsetToEnsure)
    {
        // Check arguments.
        Preconditions.checkArgument(subsetToEnsure.dataIsOptional() || subsetToEnsure.dataIsNotAllowed());
        Preconditions.checkArgument(subsetToEnsure.tauIsOptional() || subsetToEnsure.tauIsNotAllowed());

        // Ensure no data is present.
        if (postProcessingStatus.dataIsPresent() && subsetToEnsure.dataIsNotAllowed()) {
            return ensureSubset(EnsureNoData.ensureNoData(postProcessingStatus), subsetToEnsure);
        }

        // Ensure no tau is present.
        if (postProcessingStatus.tauIsPresent() && subsetToEnsure.tauIsNotAllowed()) {
            return ensureSubset(EnsureNoTau.ensureNoTau(postProcessingStatus), subsetToEnsure);
        }

        // Already in requested subset.
        return postProcessingStatus;
    }
}
