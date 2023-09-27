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

import java.util.Collections;
import java.util.List;

import org.eclipse.lsat.common.xtend.Queries;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import nl.esi.pps.tmsc.Dependency;
import nl.esi.pps.tmsc.Execution;
import nl.esi.pps.tmsc.TMSC;

/** General querying functionality for {@link Dependency dependencies}. */
public class TmscExecutionQueries {
    private TmscExecutionQueries() {
    }

    /**
     * Determines whether {@code execution} is in scope of {@code tmsc}.
     * An execution is considered to be in scope if both its entry and exit events are in scope.
     * 
     * @param tmsc The TMSC whose scope to consider.
     * @param execution The execution whose scope to check.
     * @return {@code true} if {@code execution} is in scope of {@code tmsc}, {@code false} otherwise.
     */
    public static boolean isInScope(TMSC tmsc, Execution execution) {
        Preconditions.checkNotNull(tmsc, "Expected a non-null TMSC.");
        Preconditions.checkNotNull(execution, "Expected a non-null dependency.");

        return TmscEventQueries.isInScope(tmsc, execution.getEntry())
                && TmscEventQueries.isInScope(tmsc, execution.getExit());
    }

    /**
     * Gives the first parent execution of {@code execution} that is in scope of {@code tmsc}, or {@code null} if no
     * such execution exists.
     * 
     * @param tmsc The TMSC whose scope is to be considered.
     * @param execution The input execution for which to search for parent executions.
     * @return The first parent execution of {@code execution} that is in scope of {@code tmsc}, or {@code null} if no
     *     such execution exists.
     */
    public static Execution getParentInScope(TMSC tmsc, Execution execution) {
        Preconditions.checkNotNull(tmsc, "Expected a non-null TMSC.");
        Preconditions.checkNotNull(execution, "Expected a non-null execution.");

        Execution parent = execution.getParent();

        if (parent == null) {
            return null;
        } else if (isInScope(tmsc, parent)) {
            return parent;
        } else {
            return getParentInScope(tmsc, parent);
        }
    }

    /**
     * Gives the last (farthest away) parent execution of {@code execution} that is still in scope of {@code tmsc},
     * i.e., its scoped root execution, or {@code null} if no such execution exists.
     * 
     * @param tmsc The TMSC whose scope is to be considered.
     * @param execution The input execution for which to search for root executions.
     * @return The scoped root execution of {@code execution} that is still in scope of {@code tmsc}, or {@code null} if
     *     no such execution exists.
     */
    public static Execution getRootInScope(TMSC tmsc, Execution execution) {
        Preconditions.checkNotNull(tmsc, "Expected a non-null TMSC.");
        Preconditions.checkNotNull(execution, "Expected a non-null execution.");

        Execution parent = getParentInScope(tmsc, execution);

        if (parent == null) {
            return null;
        }

        Execution root = getRootInScope(tmsc, parent);

        if (root != null) {
            return root;
        } else if (isInScope(tmsc, parent)) {
            return parent;
        } else {
            return null;
        }
    }

    /**
     * Get a list of all sub-executions of {@code execution} that are in scope of {@code tmsc}.
     * 
     * @param tmsc The TMSC whose scope to consider.
     * @param execution The execution whose sub-executions are to be found.
     * @return The list of sub-executions of {@code execution} that are in scope of {@code tmsc}.
     */
    public static List<Execution> getChildrenInScope(TMSC tmsc, Execution execution) {
        Preconditions.checkNotNull(tmsc, "Expected a non-null TMSC.");
        Preconditions.checkNotNull(execution, "Expected a non-null execution.");

        return ImmutableList.copyOf(Queries.findNearest(
                Queries.walkTree(Collections.singleton(execution), exec -> exec.getChildren()),
                exec -> isInScope(tmsc, exec)));
    }
}
