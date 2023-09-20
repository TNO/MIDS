/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.pps.extensions.cmi

import nl.esi.emf.properties.xtend.PersistedProperty
import nl.esi.pps.architecture.implemented.Function
import nl.esi.pps.tmsc.Dependency
import nl.esi.pps.tmsc.Event
import nl.esi.pps.tmsc.Lifeline
import nl.esi.pps.tmsc.TMSC
import nl.tno.mids.pps.extensions.info.EventFunctionExecutionType
import org.eclipse.escet.common.java.Strings

/** A CMI preparer for {@link TMSC TMSCs} based on textual syntax that contain information as annotations. */
class AnnotatedTextPreparer extends CmiPreparer {

    @PersistedProperty(Function)
    static val String execType

    override appliesTo(Dependency dependency) {
        return AnnotatedTextUtils.isAnnotatedTextDependency(dependency)
    }

    override protected scope(TMSC tmsc, String scopeName) {
        scopeOnDependencies(tmsc, scopeName, [true])
    }

    override protected componentNameFor(Lifeline lifeline) {
        lifeline.executor.name
    }

    override protected functionNameFor(Event event) {
        event.function.operation.name
    }

    override protected interfaceNameFor(Event event) {
        event.function.operation.interface.name
    }

    override protected executionTypeFor(Event event) {
        switch (event.function.execType) {
            case "async": return EventFunctionExecutionType.ASYNCHRONOUS_HANDLER
            case "arslt": return EventFunctionExecutionType.ASYNCHRONOUS_RESULT
            case "blk": return EventFunctionExecutionType.BLOCKING_CALL
            case "call": return EventFunctionExecutionType.CALL
            case "evt": return EventFunctionExecutionType.EVENT_RAISE
            case "evtcb": return EventFunctionExecutionType.EVENT_CALLBACK
            case "evtsub": return EventFunctionExecutionType.EVENT_SUBSCRIBE_CALL
            case "evtsubh": return EventFunctionExecutionType.EVENT_SUBSCRIBE_HANDLER
            case "evtunsub": return EventFunctionExecutionType.EVENT_UNSUBSCRIBE_CALL
            case "evtunsubh": return EventFunctionExecutionType.EVENT_UNSUBSCRIBE_HANDLER
            case "fcn": return EventFunctionExecutionType.FCN_CALL
            case "fcncb": return EventFunctionExecutionType.FCN_CALLBACK
            case "handler": return EventFunctionExecutionType.HANDLER
            case "lib": return EventFunctionExecutionType.LIBRARY_CALL
            case "req": return EventFunctionExecutionType.REQUEST_CALL
            case "sync": return EventFunctionExecutionType.SYNCHRONOUS_HANDLER
            case "trig": return EventFunctionExecutionType.TRIGGER_CALL
            case "trigh": return EventFunctionExecutionType.TRIGGER_HANDLER
            case "unkn": return EventFunctionExecutionType.UNKNOWN
            case "wait": return EventFunctionExecutionType.WAIT_CALL
        }
        throw new RuntimeException(Strings.fmt("Unknown annotated function type: %s.", event.function.execType))
    }
}
