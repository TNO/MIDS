/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.api.general

import java.util.List
import nl.tno.mids.cif.extensions.EdgeExtensions
import nl.tno.mids.cmi.api.info.ComponentInfo
import nl.tno.mids.cmi.api.info.EventAsyncDirection
import nl.tno.mids.cmi.api.info.EventFunctionExecutionSide
import nl.tno.mids.cmi.api.info.EventInfo
import nl.tno.mids.pps.extensions.info.EventFunctionExecutionType
import org.eclipse.escet.cif.common.CifCollectUtils
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent
import org.eclipse.escet.cif.metamodel.cif.Specification
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton
import org.eclipse.escet.cif.metamodel.cif.declarations.Event
import org.eclipse.escet.common.java.Assert
import org.eclipse.escet.common.java.Strings

class CmiGeneralEventQueries {
    /**
     * Get the events of the model.
     * 
     * @return The events.
     */
    static def getEvents(Specification model) {
        val List<Event> events = newArrayList()
        CifCollectUtils.collectEvents(model, events)
        return events
    }

    /**
     * Get the name of an event.
     * 
     * @param event The event.
     * @return The name of the event.
     */
    static def String getEventName(Event event) {
        val component = CmiGeneralComponentQueries.getComponent(event)
        return getEventName(event, component)
    }

    /**
     * Get the name of an event, assuming a given API subset.
     * 
     * @param event The event.
     * @param subset The API subset of the model.
     * @return The name of the event.
     */
    static def String getEventName(Event event, CmiSubset subset) {
        val component = CmiGeneralComponentQueries.getComponent(event, subset)
        return getEventName(event, component)
    }

    /**
     * Get the name of an event.
     * 
     * @param event The event.
     * @param component The component that declares the event.
     * @return The name of the event.
     */
    private static def String getEventName(Event event, ComplexComponent component) {
        Assert.check(event.eContainer == component)
        return CmiGeneralComponentQueries.getComponentName(component) + "." + event.name
    }

    /**
     * Returns information about an event, i.e. information about the parts of the event name.
     * 
     * @param event The event.
     * @return The event information.
     */
    static def getEventInfo(Event event) {
        return getEventInfo(getEventName(event))
    }

    /**
     * Returns information about an event, i.e. information about the parts of the event name, assuming a given API subset.
     * 
     * @param event The event.
     * @param subset The API subset of the model.
     * @return The event information.
     */
    static def getEventInfo(Event event, CmiSubset subset) {
        return getEventInfo(getEventName(event, subset))
    }

    /**
     * Returns information about an event, i.e. information about the parts of the event name.
     * 
     * @param eventName The event name, as provided by {@link #getEventName}.
     * @return The event information.
     */
    static def getEventInfo(String eventName) {
        var name = eventName

        // Get declaring component information.
        val periodIdx = name.indexOf('.')
        Assert.check(periodIdx > 0, "Cannot separate declaring component name in " + name)
        val declCompName = name.substring(0, periodIdx)
        val declCompInfo = new ComponentInfo(declCompName)
        name = name.substring(periodIdx + 1)

        // Get interface name.
        val underscoreIdx = name.indexOf("__")
        Assert.check(underscoreIdx > 0, "Cannot separate interface and function name in " + name)
        var interfaceName = name.substring(0, underscoreIdx)
        name = name.substring(underscoreIdx + 2)

        // Get asynchronous direction.
        var EventAsyncDirection asyncDirection = EventAsyncDirection.detectPrefix(interfaceName)
        if (asyncDirection !== null) {
            interfaceName = interfaceName.substring(asyncDirection.prefix.length)
        }

        // Get function name.
        var postfixStart = name.indexOf("__")
        Assert.check(postfixStart > 0, name)
        while (name.charAt(postfixStart + 2).compareTo('_') == 0) {
            postfixStart++;
        }
        val functionName = name.substring(0, postfixStart)
        name = name.substring(postfixStart + 1)

        // Get other component information, if any.
        var ComponentInfo otherCompInfo = null
        var otherComponentStart = name.indexOf("__")
        if (otherComponentStart > 0) {
            val otherComponentText = name.substring(otherComponentStart + 2)
            name = name.substring(0, otherComponentStart)
            otherCompInfo = new ComponentInfo(otherComponentText)
        }

        // At this point, name should only contain the postfix.
        // Get side.
        val side = EventFunctionExecutionSide.detectPostfix(name)
        if (side == EventFunctionExecutionSide.END) {
            name = Strings.slice(name, null, -side.postfix.length)
        }

        // Get type.
        val type = EventFunctionExecutionType.detectPostfix(name)
        Assert.notNull(type, "Event " + eventName + " has no type")
        name = Strings.slice(name, null, -type.postfix.length)

        // Get declared/other type/side.
        var EventFunctionExecutionType declType
        var EventFunctionExecutionSide declSide
        var EventFunctionExecutionType otherType
        var EventFunctionExecutionSide otherSide

        if (otherCompInfo === null) {
            declType = type
            declSide = side

            otherType = null
            otherSide = null

        } else {
            declSide = EventFunctionExecutionSide.detectPostfix(name)
            if (declSide == EventFunctionExecutionSide.END) {
                name = Strings.slice(name, null, -declSide.postfix.length)
            }

            declType = EventFunctionExecutionType.detectPostfix(name)
            Assert.notNull(declType, "Event " + eventName + " has no type")
            name = Strings.slice(name, null, -declType.postfix.length)

            otherType = type
            otherSide = side
        }

        Assert.check(name.empty, "Event " + eventName + " contains unknown elements:" + name)

        // Return information.
        val info = new EventInfo(declCompInfo, asyncDirection, interfaceName, functionName, declType, declSide,
            otherType, otherSide, otherCompInfo)
        Assert.check(info.toString.equals(eventName), info.toString + " != " + eventName)
        return info
    }

    /**
     * Is the given name a valid event name?
     * 
     * @param name The given name.
     * @return {@code true} if the name fits the API event naming scheme, {@code false} otherwise. 
     */
    static def isValidEventName(String name) {
        try {
            getEventInfo(name)
            return true
        } catch (AssertionError e) {
            return false
        }
    }

    /**
     * Does the given event have a valid name?
     * 
     * @param event The given event.
     * @return {@code true} if the name of the given event fits the API event naming scheme, {@code false} otherwise.
     */
    static def hasValidEventName(Event event) {
        return isValidEventName(getEventName(event))
    }

    /**
     * Does the given event represent a request?
     * 
     * @param eventInfo The given event.
     * @return {@code true} if the given event represents a request, {@code false} otherwise.
     */
    static def isRequestEvent(EventInfo eventInfo) {
        // Can only be a request if it communicates.
        if (eventInfo.otherCompInfo === null) {
            return false
        }

        // Decide based on function execution type. 
        switch (eventInfo.declType) {
            case BLOCKING_CALL,
            case CALL,
            case EVENT_RAISE,
            case EVENT_SUBSCRIBE_CALL,
            case EVENT_UNSUBSCRIBE_CALL,
            case FCN_CALL,
            case LIBRARY_CALL,
            case REQUEST_CALL,
            case TRIGGER_CALL,
            case WAIT_CALL:
                return true
            case ASYNCHRONOUS_RESULT,
            case HANDLER,
            case SYNCHRONOUS_HANDLER,
            case UNKNOWN:
                return false
            case ASYNCHRONOUS_HANDLER,
            case EVENT_CALLBACK,
            case EVENT_SUBSCRIBE_HANDLER,
            case EVENT_UNSUBSCRIBE_HANDLER,
            case FCN_CALLBACK,
            case TRIGGER_HANDLER:
                throw new RuntimeException("Not a valid 'declType': " + eventInfo)
            default: // UNKNOWN
                throw new RuntimeException("Unknown event execution type: " + eventInfo)
        }
    }

    /**
     * Does the given event represent a response?
     * 
     * @param eventInfo The given event.
     * @return {@code true} if the given event represents a response, {@code false} otherwise.
     */
    static def isResponseEvent(EventInfo eventInfo) {
        // Can only be a response if it communicates.
        if (eventInfo.otherCompInfo === null) {
            return false
        }

        // Decide based on function execution type. 
        switch (eventInfo.declType) {
            case BLOCKING_CALL,
            case CALL,
            case EVENT_RAISE,
            case EVENT_SUBSCRIBE_CALL,
            case EVENT_UNSUBSCRIBE_CALL,
            case FCN_CALL,
            case LIBRARY_CALL,
            case REQUEST_CALL,
            case TRIGGER_CALL,
            case UNKNOWN,
            case WAIT_CALL:
                return false
            case ASYNCHRONOUS_RESULT,
            case HANDLER,
            case SYNCHRONOUS_HANDLER:
                return true
            case ASYNCHRONOUS_HANDLER,
            case EVENT_CALLBACK,
            case EVENT_SUBSCRIBE_HANDLER,
            case EVENT_UNSUBSCRIBE_HANDLER,
            case FCN_CALLBACK,
            case TRIGGER_HANDLER:
                throw new RuntimeException("Not a valid 'declType': " + eventInfo)
            default: // UNKNOWN
                throw new RuntimeException("Unknown event execution type: " + eventInfo)
        }
    }

    /**
     * Is given event part of the communication between two given components?
     * 
     * @param event Event to check.
     * @param componentName1 Name of first communicating component.
     * @param componentName2 Name of second communicating component.
     * @return {@code true} if the event represents communication between the two components, {@code false} otherwise.
     */
    static def isCommunicationBetween(Event event, String componentName1, String componentName2) {
        val eventInfo = CmiGeneralEventQueries.getEventInfo(event)
        if (eventInfo.otherCompInfo === null) {
            return false
        }
        return ((eventInfo.declCompInfo.name.equals(componentName1) &&
            eventInfo.otherCompInfo.name.equals(componentName2)) ||
            (eventInfo.declCompInfo.name.equals(componentName2) && eventInfo.otherCompInfo.name.equals(componentName1)))
    }

    /**
     * Does the given model contain 'tau' edges?
     * 
     * @param model The model.
     * @return {@code true} if the model contains at least one automaton with at least one 'tau' edge, {@code false}
     *      otherwise.
     */
    static def hasTau(Specification model) {
        val List<Automaton> automata = newArrayList
        CifCollectUtils.collectAutomata(model, automata)
        automata.exists[hasTau]
    }

    /**
     * Does the given automaton contain 'tau' edges?
     * 
     * @param automaton The CIF automaton.
     * @return {@code true} if the automaton contains at least one tau edge, {@code false} otherwise.
     */
    private static def hasTau(Automaton automaton) {
        return automaton.locations.exists[edges.exists[edge|EdgeExtensions.getEventDecl(edge, true) === null]]
    }
}
