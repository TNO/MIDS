/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cif.extensions.mrr.cif;

public enum MrrToCifMode {
    /**
     * Use plain event-based automata. Only reduces sequence or introduces infinite loops.
     */
    PLAIN("Use plain edges"),

    /**
     * Use data (discrete variables, guards and assignments) to represent the exact same behavior as the MRR, but with a
     * smaller model.
     */
    DATA("Use data (variables, guards, assignments)");

    public final String description;

    private MrrToCifMode(String description) {
        this.description = description;
    }
}
