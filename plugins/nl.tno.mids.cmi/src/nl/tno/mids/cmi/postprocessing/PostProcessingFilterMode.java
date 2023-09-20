/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.postprocessing;

public enum PostProcessingFilterMode {
    NONE("No filtering (apply operation to all components)"),

    INCLUSION("Inclusion filter (apply operation only to matching components)"),

    EXCLUSION("Exclusion filter (apply operation only to non-matching components)");

    public final String description;

    PostProcessingFilterMode(String description) {
        this.description = description;
    }
}
