/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.pps.extensions.queries;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

import nl.esi.pps.tmsc.Event;
import nl.esi.pps.tmsc.Execution;
import nl.esi.pps.tmsc.FullScopeTMSC;
import nl.esi.pps.tmsc.ScopedTMSC;
import nl.esi.pps.tmsc.TMSC;
import nl.esi.pps.tmsc.util.TmscQueries;

/** General querying functionality for {@link Event events}. */
public class TmscEventQueries {
    private TmscEventQueries() {
    }

    /**
     * Determines whether {@code event} is in scope of {@code tmsc}.
     * 
     * @param tmsc The TMSC whose scope to consider.
     * @param event The event whose scope to check.
     * @return {@code true} if {@code event} is in scope of {@code tmsc}, {@code false} otherwise.
     */
    public static boolean isInScope(TMSC tmsc, Event event) {
        Preconditions.checkNotNull(tmsc, "Expected a non-null TMSC.");
        Preconditions.checkNotNull(event, "Expected a non-null event.");

        if (tmsc instanceof FullScopeTMSC) {
            return event.getTmsc().equals(tmsc);
        } else if (tmsc instanceof ScopedTMSC) {
            return TmscQueries.isInScope(event, (ScopedTMSC)tmsc);
        } else {
            throw new RuntimeException("Unknown TMSC type.");
        }
    }

    /**
     * Finds all source and target events of all given executions in {@code executions} that are in scope of
     * {@code tmsc}, as well as all the source and target events of all their (nested) sub-executions that are in scope.
     * 
     * @param tmsc The TMSC whose scope is to be considered.
     * @param executions The executions that form the basis for the search, which must all be in scope of {@code tmsc}.
     * @return All source and target {@link Event events} of {@code executions} and their nested sub-executions.
     */
    public static List<Event> collectEventsInTree(TMSC tmsc, Iterable<Execution> executions) {
        List<Event> collectedEvents = new ArrayList<>();
        Deque<Execution> todoExecutions = new LinkedList<>();
        Iterables.addAll(todoExecutions, executions);

        while (!todoExecutions.isEmpty()) {
            Execution todoExecution = todoExecutions.pop();

            Preconditions.checkArgument(TmscExecutionQueries.isInScope(tmsc, todoExecution),
                    "Expected all given executions to be in scope of the given TMSC.");

            collectedEvents.add(todoExecution.getEntry());
            collectedEvents.add(todoExecution.getExit());
            Iterables.addAll(todoExecutions, TmscExecutionQueries.getChildrenInScope(tmsc, todoExecution));
        }

        return collectedEvents;
    }
}
