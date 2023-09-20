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

/** Direction of communication for events, in case of asynchronous component composition. */
public enum EventAsyncDirection {
    /** Internal event (no middleware interaction). */
    INTERNAL("internal", "i_"),

    /** Event on component that sends to the middleware. */
    SEND("send", "s_"),

    /** Event on component that receives from the middleware. */
    RECEIVE("receive", "r_");

    private final String name;

    private final String prefix;

    EventAsyncDirection(String name, String prefix) {
        this.name = name;
        this.prefix = prefix;
    }

    public String getName() {
        return this.name;
    }

    public String getPrefix() {
        return this.prefix;
    }

    /**
     * Detects a prefix of the given text matching a prefix for one of the literals of {@code EventAsyncDirection}.
     * 
     * @param text The text.
     * @return The literal for which a prefix matched the text, or {@code null} if no prefix matched.
     */
    public static EventAsyncDirection detectPrefix(String text) {
        for (EventAsyncDirection value: EventAsyncDirection.values()) {
            if (text.startsWith(value.prefix)) {
                return value;
            }
        }
        return null;
    }
}
