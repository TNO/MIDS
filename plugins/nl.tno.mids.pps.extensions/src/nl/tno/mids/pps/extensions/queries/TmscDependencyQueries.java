/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.pps.extensions.queries;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.escet.common.java.Strings;

import com.google.common.base.Preconditions;

import nl.esi.pps.tmsc.Dependency;
import nl.esi.pps.tmsc.Event;
import nl.esi.pps.tmsc.FullScopeTMSC;
import nl.esi.pps.tmsc.LifelineSegment;
import nl.esi.pps.tmsc.Message;
import nl.esi.pps.tmsc.ScopedTMSC;
import nl.esi.pps.tmsc.TMSC;

/** General querying functionality for {@link Dependency dependencies}. */
public class TmscDependencyQueries {
    private TmscDependencyQueries() {
    }

    /**
     * @param event The event whose incoming {@link LifelineSegment} dependencies are requested.
     * @param scopeTmsc The scope to use when searching for incoming dependencies.
     * @return A list of all incoming life-line segment dependencies of the event that are in scope of the
     *     {@code scopeTmsc}.
     */
    public static List<LifelineSegment> getIncomingLifelineSegmentDependencies(Event event, TMSC scopeTmsc) {
        return filterLifelineSegmentDependencies(event.getFullScopeIncomingDependencies(), scopeTmsc)
                .collect(Collectors.toList());
    }

    /**
     * @param event The event whose outgoing {@link LifelineSegment} dependencies are requested.
     * @param scopeTmsc The scope to use when searching for outgoing dependencies.
     * @return A list of all outgoing life-line segment dependencies of the event that are in scope of the
     *     {@code scopeTmsc}.
     */
    public static List<LifelineSegment> getOutgoingLifelineSegmentDependencies(Event event, TMSC scopeTmsc) {
        return filterLifelineSegmentDependencies(event.getFullScopeOutgoingDependencies(), scopeTmsc)
                .collect(Collectors.toList());
    }

    /**
     * @param event The event whose single incoming {@link Message} dependency is requested.
     * @param scopeTmsc The scope to use when searching for incoming dependencies.
     * @return The single incoming message dependency of {@code event} that is in scope of {@code scopeTmsc}.
     * @throws RuntimeException Thrown in case the event does not have exactly one incoming message dependency in scope
     *     of the {@code scopeTmsc}.
     */
    public static Message getIncomingMessageDependency(Event event, TMSC scopeTmsc) {
        List<Message> messages = filterMessageDependencies(event.getFullScopeIncomingDependencies(), scopeTmsc)
                .collect(Collectors.toList());

        if (messages.size() != 1) {
            throw new RuntimeException(
                    Strings.fmt("Expected a single incoming message dependency, but found %d.", messages.size()));
        }

        return messages.get(0);
    }

    /**
     * @param event The event whose single outgoing {@link Message} dependency is requested.
     * @param scopeTmsc The scope to use when searching for incoming dependencies.
     * @return The single outgoing message dependency of the event that is in scope of the {@code scopeTmsc}.
     * @throws RuntimeException Thrown in case the event does not have exactly one outgoing {@link Message} dependency
     *     in scope of the {@code scopeTmsc}.
     */
    public static Message getOutgoingMessageDependency(Event event, TMSC scopeTmsc) {
        List<Message> messages = filterMessageDependencies(event.getFullScopeOutgoingDependencies(), scopeTmsc)
                .collect(Collectors.toList());

        if (messages.size() != 1) {
            throw new RuntimeException(
                    Strings.fmt("Expected a single outgoing message dependency, but found %d.", messages.size()));
        }

        return messages.get(0);
    }

    /**
     * Determines whether {@code dependency} is in scope of {@code tmsc}.
     * 
     * @param tmsc The TMSC whose scope to consider.
     * @param dependency The dependency whose scope to check.
     * @return {@code true} if {@code dependency} is in scope of {@code tmsc}, {@code false} otherwise.
     */
    public static boolean isInScope(TMSC tmsc, Dependency dependency) {
        Preconditions.checkNotNull(tmsc, "Expected a non-null TMSC.");
        Preconditions.checkNotNull(dependency, "Expected a non-null dependency.");

        if (tmsc instanceof FullScopeTMSC) {
            return dependency.getTmsc().equals(tmsc);
        } else if (tmsc instanceof ScopedTMSC) {
            return dependency.getScopes().contains(tmsc);
        } else {
            throw new RuntimeException("Unknown TMSC type.");
        }
    }

    /**
     * @param event The event for which to determine whether it has any incoming {@link Message} dependencies.
     * @param scopeTmsc The scope to use when searching for incoming dependencies.
     * @return {@code true} if the event has at least one incoming {@link Message} dependency that is in scope of the
     *     {@code scopeTmsc}, {@code false} otherwise.
     */
    public static boolean hasIncomingMessageDependencies(Event event, TMSC scopeTmsc) {
        return filterMessageDependencies(event.getFullScopeIncomingDependencies(), scopeTmsc).findAny().isPresent();
    }

    /**
     * @param event The event for which to determine whether it has any outgoing {@link Message} dependencies.
     * @param scopeTmsc The scope to use when searching for outgoing dependencies.
     * @return {@code true} if {@code event} has at least one outgoing {@link Message} dependency that is in scope of
     *     {@code scopeTmsc}, {@code false} otherwise.
     */
    public static boolean hasOutgoingMessageDependencies(Event event, TMSC scopeTmsc) {
        return filterMessageDependencies(event.getFullScopeOutgoingDependencies(), scopeTmsc).findAny().isPresent();
    }

    /**
     * @param dependencies The collection of dependencies to filter for {@link LifelineSegment} dependencies.
     * @param scopeTmsc The TMSC that determines the scope of dependency retainment.
     * @return A stream of all life-line segments contained in {@code dependencies} that are in scope of
     *     {@code scopeTmsc}.
     */
    private static Stream<LifelineSegment> filterLifelineSegmentDependencies(Collection<Dependency> dependencies,
            TMSC scopeTmsc)
    {
        return dependencies.stream().filter(d -> d instanceof LifelineSegment && isInScope(scopeTmsc, d))
                .map(d -> (LifelineSegment)d);
    }

    /**
     * @param dependencies The collection of dependencies to filter for {@link Message} dependencies.
     * @param scopeTmsc The TMSC that determines the scope of dependency retainment.
     * @return A stream of all messages contained in {@code dependencies} that are in scope of {@code scopeTmsc}.
     */
    private static Stream<Message> filterMessageDependencies(Collection<Dependency> dependencies, TMSC scopeTmsc) {
        return dependencies.stream().filter(d -> d instanceof Message && isInScope(scopeTmsc, d)).map(d -> (Message)d);
    }
}
