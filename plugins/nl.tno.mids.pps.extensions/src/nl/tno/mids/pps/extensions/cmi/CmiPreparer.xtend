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

import java.nio.file.Path
import java.util.List
import java.util.function.Predicate
import nl.esi.emf.properties.PropertiesContainer
import nl.esi.emf.properties.xtend.PersistedProperty
import nl.esi.pps.architecture.instantiated.Executor
import nl.esi.pps.tmsc.Dependency
import nl.esi.pps.tmsc.Event
import nl.esi.pps.tmsc.FullScopeTMSC
import nl.esi.pps.tmsc.Lifeline
import nl.esi.pps.tmsc.LifelineSegment
import nl.esi.pps.tmsc.ScopedTMSC
import nl.esi.pps.tmsc.TMSC
import nl.esi.pps.tmsc.TmscFactory
import nl.esi.pps.tmsc.util.TmscQueries
import nl.tno.mids.pps.extensions.info.EventFunctionExecutionType
import nl.tno.mids.pps.extensions.queries.TmscLifelineQueries
import org.apache.commons.lang3.tuple.Pair
import org.eclipse.lsat.common.util.PairwiseIterable

/**
 * A transformation for preparing {@link TMSC TMSCs} for CMI. This transformation determines and annotates all
 * information that is essential for performing CMI.
 */
abstract class CmiPreparer {
    static extension val TmscFactory m_tmsc = TmscFactory.eINSTANCE

    @PersistedProperty(Executor)
    static val String componentName

    @PersistedProperty(Event)
    static val String functionName

    @PersistedProperty(Event)
    static val String interfaceName

    @PersistedProperty(Event)
    static val EventFunctionExecutionType executionType

    abstract def boolean appliesTo(Dependency dependency)

    /**
     * Transforms the given {@code tmsc} by adding a {@link ScopedTMSC scope} to it named {@code scopeName} that
     * contains all {@link Event events} and {@link Dependency dependencies} that are to be considered by CMI. Moreover,
     * this new {@link ScopedTMSC scope} is prepared in the sense that essential information required by CMI is
     * determined and annotated by means of properties (see also {@link PropertiesContainer}).
     * 
     * @param tmsc The {@link TMSC} to be scoped.
     * @param scopeName The name of the {@link ScopedTMSC scope} to create.
     * @param warnings The warnings produced during the operation.
     * @param tmscPath The path to TMSC file, that can be used to find additional files. May be {@code null} if TMSC is
     *      not loaded from a file, in which case no additional files can be located.
     * @return The CMI {@link ScopedTMSC scope} that has been added to {@code tmsc}.
     */
    def ScopedTMSC prepare(FullScopeTMSC tmsc, String scopeName, List<String> warnings, Path tmscPath) {
        if (tmsc.fullScope.childScopes.exists[it.name.equals(scopeName)]) {
            warnings.add("Removed existing scope named " + scopeName)
        }

        // Remove any old scope named 'scopeName', just to be sure.
        tmsc.fullScope.childScopes.removeIf[it.name.equals(scopeName)]

        // Scope the given TMSC as defined by this preparer.
        val scopedTmsc = scope(tmsc, scopeName)

        // Determine (default) component names of all non-empty lifelines of the scoped TMSC.
        for (lifeline : TmscLifelineQueries.nonEmptyLifelinesOf(scopedTmsc)) {
            lifeline.executor.componentName = componentNameFor(lifeline)
        }

        // Determine CMI-specific information for all events of the scoped TMSC.
        for (event : scopedTmsc.events) {
            event.functionName = functionNameFor(event)
            event.interfaceName = interfaceNameFor(event)
            event.executionType = executionTypeFor(event)
        }

        // Return the scoped TMSC that is now prepared for CMI.
        scopedTmsc
    }

    /**
     * Creates a {@link ScopedTMSC scoped TMSC} named {@code scopeName} out of {@code tmsc} that contains only the
     * {@link Event events} and {@link Dependency dependencies} that are relevant for CMI, according to this preparer.
     * <p>
     * Any implementer of this method can assume that {@code scopeName} is a free scope name in {@code tmsc}.
     * </p>
     * 
     * @param tmsc The {@link TMSC} to scope.
     * @param scopeName The name of the {@link ScopedTMSC scoped TMSC} to create.
     * @return The {@link ScopedTMSC scoped TMSC} containing only relevant information for CMI.
     */
    protected def ScopedTMSC scope(FullScopeTMSC tmsc, String scopeName)

    /**
     * Determines a component name for {@code lifeline}.
     * 
     * @param lifeline The {@link Lifeline} for which a component name is to be determined.
     * @return The component name for {@code lifeline}, as a {@link String}.
     */
    protected def String componentNameFor(Lifeline lifeline)

    /** 
     * Determines a function name for {@code event}.
     * 
     * @param event The {@link Event} for which a function name is to be determined.
     * @return The name of the function that is executed by the specified {@code event}, as a {@link String}.
     */
    protected def String functionNameFor(Event event)

    /** 
     * Determines an interface name for {@code event}.
     * 
     * @param event The {@link Event} for which an interface name is to be determined.
     * @return The interface name of the function that is executed in the given {@code event}, as a {@link String}.
     */
    protected def String interfaceNameFor(Event event)

    /** 
     * Determines the function execution type of {@code event}.
     * 
     * @param event The {@link Event} for which a function execution type is to be determined.
     * @return The execution type of the function of the given {@code event}, as a {@link String}.
     */
    protected def EventFunctionExecutionType executionTypeFor(Event event)

    /**
     * Helper method for creating a {@link ScopedTMSC scoped TMSC} named {@code scopeName} out of {@code tmsc} that
     * contains all {@link Dependency dependencies} of {@code tmsc} between the events that satisfy {@code predicate}.
     * 
     * @param tmsc The {@link FullScopeTMSC} to scope.
     * @param scopeName The name of the {@link ScopedTMSC scoped TMSC} to create.
     * @param predicate The {@link Predicate predicate} that determines which {@link Event events} are to be
     *                  included in the scope to create.
     * @return The {@link ScopedTMSC scoped TMSC} containing all {@link Dependency dependencies} between the events 
     *         that satisfy {@code predicate}, which has also been added to the scopes of {@code tmsc}.
     */
    protected final def ScopedTMSC scopeOnEvents(FullScopeTMSC tmsc, String scopeName, Predicate<? super Event> predicate) {
        // Split the dependencies in two sets, one in scope (true) and one outside scope (false).
        val scopeDependencies = tmsc.dependencies.filter[predicate.test(source) && predicate.test(target)]
        
        // Create a new TMSC scope named 'scopeName' and add all dependencies of 'tmsc' to it that satisfy 'predicate'.
        val scopedTmsc = TmscQueries.createScopedTMSC(scopeDependencies, scopeName)
        tmsc.childScopes += scopedTmsc

        if (scopedTmsc.dependencies.size == tmsc.dependencies.size) {
            // All dependencies are in scope; nothing more to do.
            return scopedTmsc
        }
        
        // CMI TMSCs require a compete order, so create missing life-line segments and add them to the scope.
        val lifelineSegments = tmsc.lifelines.flatMap[refineWithCompleteOrder(predicate)].toList
        scopedTmsc.dependencies += lifelineSegments
        tmsc.dependencies += lifelineSegments
        
        return scopedTmsc
    }
    
    private def Iterable<LifelineSegment> refineWithCompleteOrder(Lifeline lifeline, Predicate<? super Event> predicate) {
        val acceptedEvents = lifeline.events.filter[predicate.test(it)]
        return PairwiseIterable::of(acceptedEvents).reject[lifelineSegmentExists].map[createLifelineSegementFromPair]
    }
    
    private def boolean lifelineSegmentExists(Pair<Event, Event> eventPair) {
        return eventPair.left.fullScopeOutgoingDependencies.filter(LifelineSegment).exists[target === eventPair.right]
    }

    private def LifelineSegment createLifelineSegmentFromPair(Pair<Event, Event> eventPair) {
        return createLifelineSegment => [
            source = eventPair.left
            target = eventPair.right
            projection = true
        ]
    }
}
