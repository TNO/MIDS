/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.api.info;

/** The side of a function execution represented by an event. */
public enum EventFunctionExecutionSide {
    /** The start of a function execution. */
    START("", ""),

    /** The end of a function execution, i.e. the return of a function execution. */
    END("return", "_ret");

    private final String fullPostfix;

    private final String postfix;

    EventFunctionExecutionSide(String fullPostfix, String postfix) {
        this.fullPostfix = fullPostfix;
        this.postfix = postfix;
    }

    public String getFullPostfix() {
        return this.fullPostfix;
    }

    public String getPostfix() {
        return this.postfix;
    }

    /**
     * Detects a postfix of the given text matching a postfix for one of the literals of
     * {@code EventFunctionExecutionSide}.
     * 
     * @param text The text.
     * @return The literal for which a postfix matched the text. Never {@code null}.
     */
    public static EventFunctionExecutionSide detectPostfix(String text) {
        if (text.endsWith(END.postfix)) {
            return END;
        }
        return START;
    }

    /**
     * Detects a prefix of the given text matching a postfix for one of the literals of
     * {@code EventFunctionExecutionSide}.
     * 
     * @param text The text.
     * @return The literal for which a prefix matched the text. Never {@code null}.
     */
    public static EventFunctionExecutionSide detectPrefix(String text) {
        if (text.startsWith(END.postfix)) {
            return END;
        }
        return START;
    }
}
