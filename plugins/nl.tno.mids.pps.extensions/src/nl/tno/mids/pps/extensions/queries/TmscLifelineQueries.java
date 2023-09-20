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

import java.util.List;
import java.util.stream.Collectors;

import nl.esi.pps.tmsc.Execution;
import nl.esi.pps.tmsc.Lifeline;
import nl.esi.pps.tmsc.TMSC;

/** General querying functionality for {@link Lifeline lifelines}. */
public class TmscLifelineQueries {
    private TmscLifelineQueries() {
    }

    /**
     * @param lifeline The input {@link Lifeline}.
     * @param scopeTmsc The {@link TMSC} used to determine whether or not {@code lifeline} is empty.
     * @return {@code true} if {@code lifeline} is empty with respect to {@code scopeTmsc}, i.e., does not have any
     *     {@link Execution executions} that are in scope of {@code scopeTmsc}; {@code false} otherwise.
     */
    public static boolean isEmpty(Lifeline lifeline, TMSC scopeTmsc) {
        return lifeline.getExecutions().stream().noneMatch(exec -> TmscDependencyQueries.isInScope(scopeTmsc, exec));
    }

    /**
     * @param scopeTmsc The {@link TMSC} whose non-empty {@link Lifeline lifelines} are requested.
     * @return A complete {@link List} of all {@link Lifeline lifelines} containing {@link Execution executions} that
     *     are in scope of {@code scopeTmsc}.
     */
    public static List<Lifeline> nonEmptyLifelinesOf(TMSC scopeTmsc) {
        return scopeTmsc.getFullScope().getLifelines().stream().filter(lifeline -> !isEmpty(lifeline, scopeTmsc))
                .collect(Collectors.toList());
    }
}
