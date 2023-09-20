/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cif.extensions.mrr.cif;

import static org.eclipse.escet.common.java.Lists.list;
import static org.eclipse.escet.common.java.Lists.set2list;
import static org.eclipse.escet.common.java.Sets.setc;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.escet.cif.common.CifEdgeUtils;
import org.eclipse.escet.cif.common.CifLocationUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;

import nl.tno.mids.cif.extensions.AutomatonExtensions;
import nl.tno.mids.cif.extensions.CifExtensions;
import nl.tno.mids.cif.extensions.mrr.ComputeMRR;
import nl.tno.mids.cif.extensions.mrr.data.MRR;
import nl.tno.mids.cif.extensions.mrr.data.MrrWithWord;

/** Convert CIF to {@link MRR}. */
public class CifToMrr {
    public static List<MrrWithWord<CifMrrLetter>> cifToMrr(Specification specification, CifToMrrConfig config,
            IProgressMonitor monitor)
    {
        return cifToMrr(set2list(CifExtensions.allAutomata(specification)), config, monitor);
    }

    public static List<MrrWithWord<CifMrrLetter>> cifToMrr(List<Automaton> automata, CifToMrrConfig config,
            IProgressMonitor monitor)
    {
        int work = automata.stream().collect(Collectors.summingInt(a -> a.getLocations().size()));
        SubMonitor subMonitor = SubMonitor.convert(monitor, work);
        List<MrrWithWord<CifMrrLetter>> results = new ArrayList<>();
        for (Automaton aut: automata) {
            cifToMrr(aut, config, results, subMonitor.split(aut.getLocations().size()));
        }
        return results;
    }

    private static void cifToMrr(Automaton aut, CifToMrrConfig config, List<MrrWithWord<CifMrrLetter>> results,
            IProgressMonitor monitor)
    {
        int numberOfLocations = aut.getLocations().size();
        SubMonitor subMonitor = SubMonitor.convert(monitor, numberOfLocations);
        subMonitor.subTask("Computing repetitions for \"" + CifTextUtils.getAbsName(aut, false) + "\"");
        Set<Location> visitedLocs = setc(numberOfLocations);
        Deque<Location> todoLocs = new LinkedList<>();
        todoLocs.addAll(AutomatonExtensions.initialLocations(aut));
        while (!todoLocs.isEmpty()) {
            // Get next location to consider.
            Location loc = todoLocs.pop();
            visitedLocs.add(loc);

            // Assumption: 'loc' is the start of an MRR and not part of any other MRR other
            // than the ones starting here, or ending it that location.
            for (Edge edge: loc.getEdges()) {
                subMonitor.setWorkRemaining(numberOfLocations - visitedLocs.size());
                Location afterLoc = cifToMrr(aut, loc, edge, visitedLocs, config, results, subMonitor);

                // If the end of the sequence has outgoing edges, and was not yet considered,
                // ensure it is considered.
                if (!afterLoc.getEdges().isEmpty() && !todoLocs.contains(afterLoc) && !visitedLocs.contains(afterLoc)) {
                    todoLocs.push(afterLoc);
                }
            }
        }
    }

    // No SubMonitor is to be created. The given monitor has 'number of non-visited
    // locations' units of work left.
    // Splits of exactly as many as are you visited.
    private static Location cifToMrr(Automaton aut, Location loc, Edge edge, Set<Location> visitedLocs,
            CifToMrrConfig config, List<MrrWithWord<CifMrrLetter>> results, SubMonitor nonVisitedLocsMonitor)
    {
        // Assumption: 'edge' is the start of an MRR and not part of any other MRR.

        // Initialize the path/word.
        CifMrrLetter letter = new CifMrrLetter(edge);
        List<CifMrrLetter> word = list(letter);

        // Find the path along locations with a single incoming and outgoing edge.
        Location lastLoc = CifEdgeUtils.getTarget(edge);
        while (lastLoc.getEdges().size() == 1 && !visitedLocs.contains(lastLoc) && !isJoinLocation(lastLoc)) {
            // Add next letter to path/word.
            Edge lastLocEdge = lastLoc.getEdges().get(0);
            letter = new CifMrrLetter(lastLocEdge);
            word.add(letter);

            // Proceed to move along the path/word.
            visitedLocs.add(lastLoc);
            lastLoc = CifEdgeUtils.getTarget(lastLocEdge);
        }

        // Compute MRR for the word and add it as result.
        MrrWithWord<CifMrrLetter> result = ComputeMRR.computeMRR(word, config,
                nonVisitedLocsMonitor.split(word.size()));
        results.add(result);

        // Return last location.
        return lastLoc;
    }

    /**
     * Are there multiple incoming edges that join at the given location?
     *
     * @param loc The given location.
     * @return {@code true} if it is a join location, {@code false} otherwise.
     */
    private static boolean isJoinLocation(Location loc) {
        Automaton aut = CifLocationUtils.getAutomaton(loc);
        return aut.getLocations().stream().flatMap(l -> l.getEdges().stream())
                .filter(e -> CifEdgeUtils.getTarget(e) == loc).count() > 1;
    }
}
