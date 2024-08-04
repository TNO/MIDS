/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.postprocessing;

public enum PostProcessingValidationResult {
    SERVICE_FRAGMENTS_NOT_ALLOWED("This operation may eliminate service fragments."),
    SERVICE_FRAGMENTS_REQUIRED("This operation may fail as it requires service fragments to be present."),
    DATA_NOT_ALLOWED("This operation may eliminate data."),
    DATA_REQUIRED("This operation may fail as it requires data to be present."),
    TAU_NOT_ALLOWED("This operation may eliminate tau."),
    TAU_REQUIRED("This operation may fail as it requires tau to be present.");

    public final String message;

    private PostProcessingValidationResult(String message) {
        this.message = message;
    }
}
