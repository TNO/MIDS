/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.pps.extensions.info;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** The type of a function execution. */
public enum EventFunctionExecutionType {
    /** Asynchronous handler. */
    ASYNCHRONOUS_HANDLER("async handler", "_async"),

    /** Asynchronous result call. */
    ASYNCHRONOUS_RESULT("async result", "_arslt"),

    /** Blocking call. */
    BLOCKING_CALL("blocking call", "_blk"),

    /** Event raise call. */
    EVENT_RAISE("event raise call", "_evt"),

    /** Event callback handler. */
    EVENT_CALLBACK("event callback", "_evtcb"),

    /** Event subscribe call. */
    EVENT_SUBSCRIBE_CALL("subscribe", "_evtsub"),

    /** Event subscribe handler. */
    EVENT_SUBSCRIBE_HANDLER("subscribe handler", "_evtsubh"),

    /** Event unsubscribe call. */
    EVENT_UNSUBSCRIBE_CALL("unsubscribe", "_evtunsub"),

    /** Event unsubscribe handler. */
    EVENT_UNSUBSCRIBE_HANDLER("unsubscribe handler", "_evtunsubh"),

    /** FCN call. */
    FCN_CALL("FCN call", "_fcn"),

    /** FCN callback handler. */
    FCN_CALLBACK("FCN callback", "_fcncb"),

    /** Library call. */
    LIBRARY_CALL("library call", "_lib"),

    /** Request call. */
    REQUEST_CALL("request", "_req"),

    /** Synchronous handler. */
    SYNCHRONOUS_HANDLER("sync handler", "_sync"),

    /** Trigger call. */
    TRIGGER_CALL("trigger", "_trig"),

    /** Trigger handler. */
    TRIGGER_HANDLER("trigger handler", "_trigh"),

    /** Unknown call or handler, on untraced component. */
    UNKNOWN("unknown", "_unkn"),

    /** Wait call. */
    WAIT_CALL("wait", "_wait"),

    /** Abstract function call. */
    CALL("call", "_call"),

    /** Abstract handler. */
    HANDLER("handler", "_handler");

    public static Set<String> POSTFIXES = Arrays.stream(EventFunctionExecutionType.values()).map(t -> t.getPostfix())
            .collect(Collectors.toSet());

    private final String name;

    private final String postfix;

    EventFunctionExecutionType(String name, String postfix) {
        this.name = name;
        this.postfix = postfix;
    }

    public String getName() {
        return this.name;
    }

    public String getPostfix() {
        return this.postfix;
    }

    /**
     * Detects a slice of the given text matching a postfix for one of the literals of
     * {@code EventFunctionExecutionType}. The literals are considered in the order they are defined.
     * 
     * @param text The text.
     * @return The literal for which a postfix was found in the text, or {@code null} if no postfix matched.
     */
    public static EventFunctionExecutionType containsPostfix(String text) {
        for (EventFunctionExecutionType value: EventFunctionExecutionType.values()) {
            if (text.contains(value.postfix)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Detects a postfix of the given text matching a postfix for one of the literals of
     * {@code EventFunctionExecutionType}.
     * 
     * @param text The text.
     * @return The literal for which a postfix matched the text, or {@code null} if no postfix matched.
     */
    public static EventFunctionExecutionType detectPostfix(String text) {
        for (EventFunctionExecutionType value: EventFunctionExecutionType.values()) {
            if (text.endsWith(value.postfix)) {
                return value;
            }
        }
        return null;
    }

    /**
     * Retrieves a postfix of the given text following the last postfix occurrence for one of the literals of
     * {@code EventFunctionExecutionType}.
     * 
     * @param text The text.
     * @return The postfix of the given text, or {@code null} if no postfix for a literal matched.
     */
    public static String textAfterPostFix(String text) {
        // Find last literal postfix match.
        int postfixIndex = 0;
        for (EventFunctionExecutionType value: EventFunctionExecutionType.values()) {
            int lastIndex = text.lastIndexOf(value.postfix);
            if (lastIndex >= 0) {
                postfixIndex = Integer.max(postfixIndex, lastIndex + value.postfix.length());
            }
        }

        // Return text after last match.
        if (postfixIndex > 0) {
            return text.substring(postfixIndex);
        } else {
            return null;
        }
    }
}
