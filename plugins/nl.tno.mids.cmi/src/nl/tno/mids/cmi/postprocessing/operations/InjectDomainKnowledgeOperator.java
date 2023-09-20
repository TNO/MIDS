/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.postprocessing.operations;

public enum InjectDomainKnowledgeOperator {
    /** Intersection. */
    INTERSECTION("Intersection (and)"),

    /** Union. */
    UNION("Union (or)"),

    /** Difference (left), i.e. 'injectedModel \ previousModel'. */
    DIFFERENCE_LEFT("Difference left (injectedModel \\ previousModel)"),

    /** Difference (right), i.e. 'previousModel \ injectedModel'. */
    DIFFERENCE_RIGHT("Difference right (previousModel \\ injectedModel)"),

    /** Exclusive or. */
    EXCLUSIVE_OR("Exclusive or (xor)"),

    /** Parallel composition. */
    PARALLEL_COMPOSITION("Parallel composition");

    public final String description;

    InjectDomainKnowledgeOperator(String description) {
        this.description = description;
    }
}
