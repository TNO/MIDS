/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.automatalib.extensions.cif;

/**
 * Exception representing a failed precondition of automata conversion.
 */
public class ConversionPreconditionException extends RuntimeException {
    /**
     * @param message Message describing failed precondition.
     */
    public ConversionPreconditionException(String message) {
        super(message);
    }
}
