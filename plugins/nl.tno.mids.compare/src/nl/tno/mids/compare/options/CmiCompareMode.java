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

/** CMI compare mode. */
public enum CmiCompareMode {
    /** Compare with components as entities. */
    COMPONENTS("Compare component models"),

    /** Compare with protocols as entities. */
    PROTOCOLS("Compare protocol models"),

    /** Compare with service fragments as entities. */
    SERVICE_FRAGMENTS("Compare service fragment models"),

    /** Compare with entities based on heuristics. */
    AUTOMATIC("Automatic choice (service fragments if possible, otherwise components or protocols as appropriate)");

    /** Description of compare mode. */
    public final String description;

    CmiCompareMode(String description) {
        this.description = description;
    }
}
