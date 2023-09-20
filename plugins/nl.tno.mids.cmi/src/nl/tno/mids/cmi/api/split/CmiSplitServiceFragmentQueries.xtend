/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.api.split

import java.util.List
import nl.tno.mids.cif.extensions.AutomatonExtensions
import nl.tno.mids.cif.extensions.EdgeExtensions
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries
import nl.tno.mids.cmi.api.general.CmiGeneralQueries
import nl.tno.mids.cmi.api.info.EventFunctionExecutionSide
import nl.tno.mids.pps.extensions.info.EventFunctionExecutionType
import org.eclipse.escet.cif.common.CifCollectUtils
import org.eclipse.escet.cif.common.CifTextUtils
import org.eclipse.escet.cif.metamodel.cif.Group
import org.eclipse.escet.cif.metamodel.cif.Specification
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton
import org.eclipse.escet.common.java.Assert
import org.eclipse.escet.common.position.metamodel.position.PositionObject

class CmiSplitServiceFragmentQueries {
    /**
     * Is the given model within the split subset, i.e., only components with split service fragments?
     * 
     * <p>This check is implemented by checking that all CIF automata are service fragment automata, rather than
     * component automata.</p>
     * 
     * @return {@code true} if the model is within the split subset with only components with split service fragments,
     *      {@code false} otherwise.
     * @note Use {@link CmiGeneralQueries#detectSubset} instead, for additional robustness.
     */
    static def boolean isSplitCmiModelWithOnlySplitServiceFragments(Specification model) {
        // Check that no automaton is contained in the root of the specification.
        val List<Automaton> automata = newArrayList
        CifCollectUtils.collectAutomata(model, automata)
        if (automata.exists[it.eContainer instanceof Specification]) {
            return false
        }

        // Get groups in root of specification, representing components.
        var components = model.getComponents().filter[it instanceof Group].map[it as Group].toSet

        // All automata must be contained (possible via intermediate groups) in component groups.
        for (Automaton aut : automata) {
            // Get component being/containing the automaton.
            var PositionObject component = aut
            while (component.eContainer !== null && component.eContainer instanceof Group &&
                !(component.eContainer instanceof Specification)) {
                component = component.eContainer as PositionObject
            }

            // Make sure the component is a group.
            if (!(component instanceof Group)) {
                return false;
            }

            // Make sure the component contains the automaton.
            if (!components.contains(component as Group)) {
                return false
            }
        }

        // All automata should be service fragment automata.
        if (!automata.forall[it.isServiceFragment]) {
            return false
        }

        // Is 'split' model.
        return true
    }

    /**
     * Get the service fragments of a model or component.
     * 
     * @param modelOrComponent The model or component.
     * @return The service fragments.
     */
    static def List<Automaton> getServiceFragments(Group modelOrComponent) {
        Assert.check(modelOrComponent instanceof Specification ||
            CmiSplitComponentQueries.isComponent(modelOrComponent))
        val List<Automaton> automata = newArrayList
        CifCollectUtils.collectAutomata(modelOrComponent, automata)
        Assert.check(automata.forall[it.isServiceFragment])
        return automata
    }

    /**
     * Is the given CIF automaton a service fragment?
     * 
     * <p>An automaton is a service fragment if it has a single initial location, with a single outgoing edge, with a
     * single non-tau event, and the automaton name matches the service fragment name derived from the event name.</p>
     * 
     * @param automaton The automaton.
     * @return {@code true} if the automaton is a service fragment, {@code false} otherwise.
     */
    private static def isServiceFragment(Automaton automaton) {
        // Check/get single initial location.
        val initialLocations = AutomatonExtensions.initialLocations(automaton)
        if (initialLocations.size != 1) {
            return false
        }
        val initialLocation = initialLocations.head

        // Check/get single single outgoing edge.
        if (initialLocation.edges.size != 1) {
            return false
        }
        val edge = initialLocation.edges.head

        // Check/get single event.
        val events = EdgeExtensions.getEventDecls(edge, true)
        if (events.size != 1) {
            return false
        }
        val event = events.head

        // Check non-tau event.
        if (event === null) {
            return false
        }

        // For service fragments, absolute name should be a valid event name.
        val eventName = CifTextUtils.getAbsName(event, false)
        if (!CmiGeneralEventQueries.isValidEventName(eventName)) {
            return false
        }

        // Get service fragment name.
        val serviceFragmentName = eventName.replace('.', '_')

        // Check automaton name.
        return automaton.name == serviceFragmentName
    }

    /**
     * Returns the event for the given service fragment.
     * 
     * @param serviceFragment The service fragment.
     * @return The event.
     */
    static def getServiceFragmentEvent(Automaton serviceFragment) {
        Assert.check(isServiceFragment(serviceFragment), "Automaton " + CifTextUtils.getAbsName(serviceFragment) +
            " is not a service fragment and has no service fragment event.")
        return EdgeExtensions.getEventDecl(AutomatonExtensions.initialLocations(serviceFragment).head.edges.head, true)
    }

    /**
     * Does the given service fragment handle an event (un)subscription?
     * 
     * @param serviceFragment The given service fragment.
     * @return {@code true} if the given service fragment handles an event (un)subscription, {@code false} otherwise.
     */
    static def isEventSubscriptionOrUnsubscriptionServiceFragment(Automaton serviceFragment) {
        val event = getServiceFragmentEvent(serviceFragment)
        val eventInfo = CmiGeneralEventQueries.getEventInfo(event)
        return eventInfo.otherSide == EventFunctionExecutionSide.START &&
            (eventInfo.otherType == EventFunctionExecutionType.EVENT_SUBSCRIBE_HANDLER ||
                eventInfo.otherType == EventFunctionExecutionType.EVENT_UNSUBSCRIBE_HANDLER)
    }

    /**
     * Does the given service fragment handle a client request?
     * 
     * @param serviceFragment The service fragment.
     * @return {@code true} if the service fragment handles a client request, {@code false} otherwise.
     */
    static def isClientRequestServiceFragment(Automaton serviceFragment) {
        val event = getServiceFragmentEvent(serviceFragment)
        val eventInfo = CmiGeneralEventQueries.getEventInfo(event)
        if (eventInfo.otherType === null) {
            return false;
        }

        switch (eventInfo.otherType) {
            case ASYNCHRONOUS_HANDLER,
            case EVENT_SUBSCRIBE_HANDLER,
            case EVENT_UNSUBSCRIBE_HANDLER,
            case SYNCHRONOUS_HANDLER,
            case TRIGGER_HANDLER,
            case HANDLER:
                return true
            case BLOCKING_CALL,
            case CALL,
            case EVENT_CALLBACK,
            case FCN_CALLBACK,
            case LIBRARY_CALL,
            case WAIT_CALL,
            case UNKNOWN:
                return false
            case ASYNCHRONOUS_RESULT,
            case EVENT_RAISE,
            case EVENT_SUBSCRIBE_CALL,
            case EVENT_UNSUBSCRIBE_CALL,
            case FCN_CALL,
            case REQUEST_CALL,
            case TRIGGER_CALL:
                throw new RuntimeException("Not a valid start event of a service fragment: " + eventInfo)
            default:
                throw new RuntimeException("Unknown event execution type: " + eventInfo.otherType)
        }
    }

    /**
     * Does the given service fragment handle a server response?
     * 
     * @param serviceFragment The service fragment.
     * @return {@code true} if the service fragment handles a server response, {@code false} otherwise.
     */
    static def isServerResponseServiceFragment(Automaton serviceFragment) {
        val event = getServiceFragmentEvent(serviceFragment)
        val eventInfo = CmiGeneralEventQueries.getEventInfo(event)
        if (eventInfo.otherType === null) {
            return false
        }

        switch (eventInfo.otherType) {
            case ASYNCHRONOUS_HANDLER,
            case EVENT_SUBSCRIBE_HANDLER,
            case EVENT_UNSUBSCRIBE_HANDLER,
            case SYNCHRONOUS_HANDLER,
            case TRIGGER_HANDLER,
            case HANDLER,
            case UNKNOWN:
                return false
            case BLOCKING_CALL,
            case CALL,
            case EVENT_CALLBACK,
            case FCN_CALLBACK,
            case LIBRARY_CALL,
            case WAIT_CALL:
                return true
            case ASYNCHRONOUS_RESULT,
            case EVENT_RAISE,
            case EVENT_SUBSCRIBE_CALL,
            case EVENT_UNSUBSCRIBE_CALL,
            case FCN_CALL,
            case REQUEST_CALL,
            case TRIGGER_CALL:
                throw new RuntimeException("Not a valid start event of a service fragment: " + eventInfo)
            default: // UNKNOWN
                throw new RuntimeException("Unknown event execution type: " + eventInfo.otherType)
        }
    }

    /**
     * Does the given service fragment handle an internal or untraced event?
     * 
     * @param serviceFragment The service fragment.
     * @return {@code true} if the service fragment handles an internal or untraced event, {@code false} otherwise.
     */
    static def isInternalServiceFragment(Automaton serviceFragment) {
        val event = getServiceFragmentEvent(serviceFragment)
        val eventInfo = CmiGeneralEventQueries.getEventInfo(event)
        if (eventInfo.declType == EventFunctionExecutionType.UNKNOWN) {
            return true
        }
        return eventInfo.otherType === null
    }
}
