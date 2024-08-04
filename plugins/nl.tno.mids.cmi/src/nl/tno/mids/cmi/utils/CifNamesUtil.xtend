/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.utils

import com.google.common.base.Preconditions
import java.util.stream.Collectors
import nl.esi.emf.properties.xtend.PersistedProperty
import nl.esi.pps.architecture.instantiated.Executor
import nl.esi.pps.tmsc.Dependency
import nl.esi.pps.tmsc.Event
import nl.esi.pps.tmsc.ExitEvent
import nl.esi.pps.tmsc.TMSC
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries
import nl.tno.mids.cmi.api.info.ComponentInfo
import nl.tno.mids.cmi.api.info.EventAsyncDirection
import nl.tno.mids.cmi.api.info.EventFunctionExecutionSide
import nl.tno.mids.pps.extensions.info.EventFunctionExecutionType
import nl.tno.mids.pps.extensions.queries.TmscDependencyQueries
import nl.tno.mids.pps.extensions.queries.TmscEventQueries
import org.eclipse.escet.common.java.Assert
import org.eclipse.escet.common.java.Strings

/** Utilities for obtaining CMI-compliant names from various {@link TMSC} artifacts. */
class CifNamesUtil {
    @PersistedProperty(Event)
    static val String functionName

    @PersistedProperty(Event)
    static val String interfaceName

    @PersistedProperty(Event)
    static val EventFunctionExecutionType executionType

    @PersistedProperty(Executor)
    static val String componentName

    private new() {
    }

    /** 
     * @param executor The {@link Executor executor} whose CIF component name is requested.
     * @return The CIF component name of the given {@code executor}.
     */
    def static String asCifName(Executor executor) {
        Preconditions.checkArgument(!executor.componentName.nullOrEmpty, "Expected a non-empty component name.")

        // Get CIF-compatible component name.
        val name = asCifName(executor.componentName);

        // Ensure 'name' is API compatible.
        new ComponentInfo(name)

        return name
    }

    /**
     * Gives the CIF event name for the specified {@code event} in the scope of {@code scopeTmsc}.
     * 
     * @param event The {@link TMSC} {@link Event event}, which should be in scope of {@code scopeTmsc}.
     * @param scopeTmsc The contextual {@link TMSC}.
     * @param synchronous Whether components are synchronized, requiring the same event name for both sides of a TMSC
     *                    dependency, or are not synchronized (e.g. in case of synchronization with middleware that is
     *                    added later).
     * @return The CIF event name.
     */
    def static String asCifName(Event event, TMSC scopeTmsc, boolean synchronous) {
        Preconditions.checkArgument(TmscEventQueries.isInScope(scopeTmsc, event),
            "Expected the given event to be within scope.")
        Preconditions.checkArgument(!event.functionName.nullOrEmpty, "Expected a non-empty function name.")
        Preconditions.checkArgument(!event.interfaceName.nullOrEmpty, "Expected a non-empty interface name.")
        Preconditions.checkNotNull(event.executionType, "Expected a non-null function execution type.")

        var name = new StringBuilder(100)

        // Get dependency and event for other side of dependency, if any.
        var Dependency dependency
        var Event otherEvent

        if (!TmscDependencyQueries.hasIncomingMessageDependencies(event, scopeTmsc) &&
            !TmscDependencyQueries.hasOutgoingMessageDependencies(event, scopeTmsc)) {
            // No dependencies, internal event to component itself.
            dependency = null
            otherEvent = null

        } else if (!TmscDependencyQueries.hasIncomingMessageDependencies(event, scopeTmsc) &&
            TmscDependencyQueries.hasOutgoingMessageDependencies(event, scopeTmsc)) {
            dependency = TmscDependencyQueries.getOutgoingMessageDependency(event, scopeTmsc)
            otherEvent = dependency.target
        } else if (TmscDependencyQueries.hasIncomingMessageDependencies(event, scopeTmsc) &&
            !TmscDependencyQueries.hasOutgoingMessageDependencies(event, scopeTmsc)) {
            dependency = TmscDependencyQueries.getIncomingMessageDependency(event, scopeTmsc)
            otherEvent = dependency.source
        } else {
            throw new RuntimeException(Strings.fmt("Event %s has multiple message dependencies.", event))
        }

        if (otherEvent !== null) {
            Preconditions.checkArgument(!otherEvent.functionName.nullOrEmpty, "Expected a non-empty function name.")
            Preconditions.checkArgument(!otherEvent.interfaceName.nullOrEmpty, "Expected a non-empty interface name.")
            Preconditions.checkNotNull(otherEvent.executionType, "Expected a non-null function execution type.")

            // Check function identity consistency. Both sides must be referring to the same function on the same
            // interface.
            Preconditions.checkArgument(event.interfaceName == otherEvent.interfaceName,
                event.interfaceName + " != " + otherEvent.interfaceName)
            Preconditions.checkArgument(event.functionName == otherEvent.functionName,
                event.interfaceName + "." + event.functionName + " != " + otherEvent.interfaceName + "." +
                    otherEvent.functionName)

            // Check function dependency consistency.
            Preconditions.checkNotNull(dependency)
            Preconditions.checkArgument((dependency.source === event) != (dependency.source === otherEvent))
        }

        // Add component name as CIF component. Determines event placement.
        if (dependency === null || !synchronous) {
            // Internal: the event is placed at the component on which it happened.
            // Asynchronous: the event is placed at the component on which it happened.
            name.append(event.lifeline.executor.asCifName)
        } else {
            // Synchronous: the event is placed at the source of the dependency.
            name.append(dependency.source.lifeline.executor.asCifName)
        }
        name.append('.');

        // Direction in case of asynchronous communication.
        // Ensures that e.g. communication to/from middleware can be distinguished.
        if (!synchronous) {
            name.append(
                if (dependency === null) {
                    EventAsyncDirection.INTERNAL.prefix
                } else if (dependency.source === event) {
                    EventAsyncDirection.SEND.prefix
                } else {
                    EventAsyncDirection.RECEIVE.prefix
                }
            )
        }

        // Check interface name to ensure it can later be separated again from the function name (for the API).
        Preconditions.checkArgument(!event.interfaceName.contains("__"))
        // Check function name to ensure it can later be separated again from the postfix (for the API).
        Preconditions.checkArgument(!event.functionName.contains("__"))

        // Add interface and function.
        name.append(event.interfaceName)
        name.append("__")
        name.append(event.functionName)
        name.append("_")

        // Add post-fixes. These always start with an underscore, giving a total of two underscores between event name
        // and postfix.
        if (otherEvent === null) {
            name.addEventPostFix(event)

        } else if (dependency.source === event) {
            name.addEventPostFix(event)
            name.addEventPostFix(otherEvent)

        } else {
            name.addEventPostFix(otherEvent)
            name.addEventPostFix(event)
        }

        // Add other side identity.
        if (dependency === null) {
            // Internal event. Component is its own 'other side'. Omit it for brevity.
        } else if (synchronous) {
            // Events are placed at the source of the dependency, so the 'other side' is the target.
            name.append("__")
            name.append(dependency.target.lifeline.executor.asCifName)

        } else {
            // Events are placed at the component on which the event happens.
            // Whichever side of the dependency is not the same component is the 'other side'.
            val otherSideEvent = dependency.source === event ? dependency.target : dependency.source
            name.append("__")
            name.append(otherSideEvent.lifeline.executor.asCifName)
        }

        // Get CIF-compatible event name.
        val cifName = name.toString.asCifAbsoluteName

        // Ensure name is API compatible.
        CmiGeneralEventQueries.getEventInfo(cifName)

        // Return name.
        return cifName
    }

    /**
     * @param name The name to add postfix to.
     * @param event The event defining the postfix to add.
     */
    protected def static StringBuilder addEventPostFix(StringBuilder name, Event event) {
        name.append(event.executionType.postfix)
        if (event instanceof ExitEvent) {
            name.append(EventFunctionExecutionSide.END.getPostfix)
        }
    }

    /** 
     * @param name The name to format.
     * @return The given absolute {@code name} formatted as a valid CIF identifier.
     */
    def private static String asCifAbsoluteName(String name) {
        return name.split("\\.").stream.map[asCifName].collect(Collectors.joining("."));
    }

    /** 
     * @param name The name to format.
     * @return The given {@code name} formatted as a valid CIF identifier.
     */
    def private static String asCifName(String name) {
        // Ensure name not empty.
        Assert.check(!name.isEmpty)

        // Replace characters not supported in CIF identifiers by underscores.
        var String cifName = name.replaceAll("[^a-zA-Z0-9_]", "_")

        // Add underscore to name if starts with a digit.
        if (cifName.matches("^[0-9]")) {
            cifName = "_" + cifName
        }

        // Return valid CIF identifier.
        return cifName;
    }
}
