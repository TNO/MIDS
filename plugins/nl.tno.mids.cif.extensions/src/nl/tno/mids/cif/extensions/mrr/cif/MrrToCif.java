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

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newAssignment;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBinaryExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariable;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariableExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEdge;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEdgeEvent;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEventExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newIntExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newIntType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newLocation;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newTauExpression;
import static org.eclipse.escet.common.emf.EMFHelper.deepclone;
import static org.eclipse.escet.common.java.Lists.first;
import static org.eclipse.escet.common.java.Lists.last;
import static org.eclipse.escet.common.java.Lists.listc;
import static org.eclipse.escet.common.java.Sets.set;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.escet.cif.common.CifEdgeUtils;
import org.eclipse.escet.cif.common.CifScopeUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.automata.Assignment;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeReceive;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeSend;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.IntExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.TauExpression;
import org.eclipse.escet.common.emf.EMFHelper;

import com.google.common.base.Preconditions;

import nl.tno.mids.cif.extensions.mrr.data.ConcatenationMRR;
import nl.tno.mids.cif.extensions.mrr.data.LetterMRR;
import nl.tno.mids.cif.extensions.mrr.data.MRR;
import nl.tno.mids.cif.extensions.mrr.data.MrrWithWord;
import nl.tno.mids.cif.extensions.mrr.data.RepetitionMRR;

public class MrrToCif {
    public static final String COUNTER_NAME_BASE = "cnt";

    /**
     * Adapts the CIF automaton, based on the given MRR.
     *
     * @param mrr The MRR.
     * @param mode The MRR to CIF mode.
     */
    public static void mrrToCif(MrrWithWord<CifMrrLetter> mrrWithWord, MrrToCifMode mode) {
        // Prepare internal data.
        Automaton aut = CifMrrUtils.getAutomaton(mrrWithWord.mrr);
        MrrToCifData data = new MrrToCifData(aut, mrrWithWord.word);

        // Remove all edges and intermediate locations for the word.
        removeOldStates(data);

        // Create new edges and locations for the MRR.
        MrrToCifResult mrrResult = mrrToCif(mrrWithWord.mrr, data, mode);

        // Connect the location before the original sequence to the MRR result.
        // Connect: preLoc -> mrrResult.start
        // Don't allow merging locations, as we want to maintain 'preLoc', as it may
        // be the start or end of other MRRs.
        Location preLoc = first(data.word).letter.sourceLoc;
        MrrToCifResult preResult = new MrrToCifResult(null, null, preLoc, preLoc);
        MrrToCifResult preMrrResult = processConcat(preResult, mrrResult, data, false);

        // Connect the previous result to the location after the original sequence.
        // Connect: preMrrResult.end -> postLoc
        // Don't allow merging locations, as we want to maintain 'postLoc', as it may
        // be the start or end of other MRRs.
        Location postLoc = last(data.word).letter.targetLoc;
        MrrToCifResult postResult = new MrrToCifResult(null, null, postLoc, postLoc);
        processConcat(preMrrResult, postResult, data, false);

        // Provide all the new locations with a unique name.
        giveNamesToNewLocations(data);
    }

    private static void removeOldStates(MrrToCifData data) {
        // Sanity checks.
        for (int i = 0; i < data.word.size(); i++) {
            // Check edge.
            LetterMRR<CifMrrLetter> letter = data.word.get(i);
            Edge edge = letter.letter.edge;
            Preconditions.checkState(!edge.isUrgent());
            Preconditions.checkState(edge.getGuards().isEmpty());
            Preconditions.checkState(edge.getUpdates().isEmpty());
            Preconditions.checkState(edge.getEvents().size() == 1);
            EdgeEvent edgeEvent = edge.getEvents().get(0);
            Preconditions.checkState(!(edgeEvent instanceof EdgeSend));
            Preconditions.checkState(!(edgeEvent instanceof EdgeReceive));
            Preconditions.checkState(!(edgeEvent.getEvent() instanceof TauExpression));

            // Check location.
            if (i > 0) {
                Location loc = letter.letter.sourceLoc;
                Preconditions.checkState(!loc.isUrgent());
                Preconditions.checkState(loc.getInitials().isEmpty());
                Preconditions.checkState(loc.getInvariants().isEmpty());
                Preconditions.checkState(loc.getEquations().isEmpty());
            }
        }

        // Remove the edges for each of the letters. Collect locations to remove.
        List<Location> locationsToRemove = listc(data.word.size() - 1);
        for (int i = 0; i < data.word.size(); i++) {
            LetterMRR<CifMrrLetter> letter = data.word.get(i);
            EMFHelper.removeFromParentContainment(letter.letter.edge);
            if (i > 0) {
                locationsToRemove.add(letter.letter.sourceLoc);
            }
        }

        // Remove locations all at once. This is way more efficient,
        // as an EMF EList is an ArrayList.
        data.automaton.getLocations().removeAll(locationsToRemove);
    }

    private static MrrToCifResult mrrToCif(MRR<CifMrrLetter> mrr, MrrToCifData data, MrrToCifMode mode) {
        if (mrr instanceof LetterMRR<?>) {
            LetterMRR<CifMrrLetter> letter = (LetterMRR<CifMrrLetter>)mrr;
            return processLetter(letter, data, mode);
        } else if (mrr instanceof ConcatenationMRR<?>) {
            ConcatenationMRR<CifMrrLetter> concat = (ConcatenationMRR<CifMrrLetter>)mrr;
            return processConcat(concat, data, mode);
        } else if (mrr instanceof RepetitionMRR<?>) {
            RepetitionMRR<CifMrrLetter> repeat = (RepetitionMRR<CifMrrLetter>)mrr;
            return processRepeat(repeat, data, mode);
        } else {
            throw new RuntimeException("Unexpected MRR: " + mrr.getClass().getName());
        }
    }

    private static MrrToCifResult processLetter(LetterMRR<CifMrrLetter> letter, MrrToCifData data, MrrToCifMode mode) {
        // Construct edge.
        EventExpression eventRef = newEventExpression();
        eventRef.setEvent(letter.letter.event);
        eventRef.setType(newBoolType());

        EdgeEvent edgeEvent = newEdgeEvent();
        edgeEvent.setEvent(eventRef);

        Edge edge = newEdge();
        edge.getEvents().add(edgeEvent);

        // The second condition below regarding letter being the last is not essential,
        // but sometimes prevents creating superfluous tau-transitions.
        if (letter.letter.isTargetLocMarked() && !last(data.word).equals(letter)) {
            // Add new marked location
            Location loc = newLocation();
            loc.getMarkeds().add(CifValueUtils.makeTrue());
            data.automaton.getLocations().add(loc);

            // Connect: edge -> loc
            edge.setTarget(loc);

            // Result: (edge, loc)
            return new MrrToCifResult(edge, null, null, loc);
        } else {
            // Result: (edge, edge)
            return new MrrToCifResult(edge, edge, null, null);
        }
    }

    private static MrrToCifResult processConcat(ConcatenationMRR<CifMrrLetter> concat, MrrToCifData data,
            MrrToCifMode mode)
    {
        // Process children.
        List<MrrToCifResult> childResults = listc(concat.sequence.size());
        for (MRR<CifMrrLetter> child: concat.sequence) {
            MrrToCifResult childResult = mrrToCif(child, data, mode);
            childResults.add(childResult);
        }

        // Concatenate child results.
        MrrToCifResult concatResult = null;
        for (MrrToCifResult childResult: childResults) {
            if (concatResult == null) {
                concatResult = childResult;
            } else {
                // Connect: concatResult.end -> childResult.start
                // Allow merging locations, as these are all internal to the word/MRR.
                concatResult = processConcat(concatResult, childResult, data, true);
            }
        }
        return concatResult;
    }

    /**
     * Concatenate two MRR to CIF results.
     *
     * @param result1 The first MRR to CIF result.
     * @param result2 The second MRR to CIF result.
     * @param data The internal data.
     * @param allowLocMerge Whether to allow merging locations ({@code true}) or require an additional tau edge to be
     *     inserted ({@code false}).
     * @return The concatenation result.
     */
    private static MrrToCifResult processConcat(MrrToCifResult result1, MrrToCifResult result2, MrrToCifData data,
            boolean allowLocMerge)
    {
        // Initialize result: (result1.start, result2.end)
        Edge concatStartEdge = (result1.startIsEdge()) ? result1.startEdge : null;
        Edge concatEndEdge = (result2.endIsEdge()) ? result2.endEdge : null;
        Location concatStartLoc = (result1.startIsLocation()) ? result1.startLoc : null;
        Location concatEndLoc = (result2.endIsLocation()) ? result2.endLoc : null;

        // Connect end of result1 to start of result2.
        // Connect: result1.end -> result2.start
        if (result1.endIsEdge() && result2.startIsEdge()) {
            // Add new location.
            Location loc = newLocation();
            data.automaton.getLocations().add(loc);

            // Connect: result1.endEdge -> loc -> result2.startEdge
            result1.endEdge.setTarget(loc);
            loc.getEdges().add(result2.startEdge);
        } else if (result1.endIsEdge() && result2.startIsLocation()) {
            // Connect: result1.endEdge -> result2.startLoc
            result1.endEdge.setTarget(result2.startLoc);
        } else if (result1.endIsLocation() && result2.startIsEdge()) {
            // Connect: result1.endLoc -> result2.startEdge
            result1.endLoc.getEdges().add(result2.startEdge);
        } else if (result1.endIsLocation() && result2.startIsLocation()) {
            if (allowLocMerge) {
                // Merge locations, keeping result1.endLoc, favoring it over keeping
                // result2.startLoc.
                mergeLocIntoLoc(result2.startLoc, result1.endLoc, data);

                // If result2 starts and ends at the removed location, update concatenation end
                // result.
                if (result2.endIsLocation() && result2.startLoc == result2.endLoc) {
                    concatEndLoc = result1.endLoc;
                }
            } else {
                // Connect the two locations by means of a 'tau' edge.
                TauExpression tauRef = newTauExpression(null, newBoolType());
                EdgeEvent tauEdgeEvent = newEdgeEvent();
                tauEdgeEvent.setEvent(tauRef);
                Edge tauEdge = newEdge();
                tauEdge.getEvents().add(tauEdgeEvent);

                result1.endLoc.getEdges().add(tauEdge);
                tauEdge.setTarget(result2.startLoc);
            }
        } else {
            throw new RuntimeException("Unexpected concatenation case.");
        }

        // Return concatenation result.
        return new MrrToCifResult(concatStartEdge, concatEndEdge, concatStartLoc, concatEndLoc);
    }

    private static MrrToCifResult processRepeat(RepetitionMRR<CifMrrLetter> repeat, MrrToCifData data,
            MrrToCifMode mode)
    {
        // Check assumptions.
        int origCount = repeat.getCount();
        Integer modiCount = repeat.getModifiedCount();
        Preconditions.checkArgument(origCount >= 2);
        Preconditions.checkArgument(modiCount == null || modiCount >= 1);

        // Process this repeat.
        if (modiCount == null) {
            return processRepeatInfinite(repeat, data, mode);
        } else if (modiCount > origCount) {
            throw new RuntimeException("Increasing the repetition count is not supported.");
        } else if (mode == MrrToCifMode.PLAIN || modiCount == 1) {
            return processRepeatFinitePlain(repeat, data, mode);
        } else if (mode == MrrToCifMode.DATA) {
            return processRepeatData(repeat, data);
        } else {
            throw new RuntimeException("Unexpected combination of original and modified counts.");
        }
    }

    private static MrrToCifResult processRepeatInfinite(RepetitionMRR<CifMrrLetter> repeat, MrrToCifData data,
            MrrToCifMode mode)
    {
        // Process child.
        MrrToCifResult childResult = mrrToCif(repeat.getChild(), data, mode);

        // Process repeat.
        if (childResult.startIsEdge() && childResult.endIsEdge()) {
            // Add new location.
            Location loc = newLocation();
            data.automaton.getLocations().add(loc);

            // Connect to new location, introducing infinite loop.
            // Connect: loc -> childResult.startEdge -> ... -> childResult.endEdge -> loc
            loc.getEdges().add(childResult.startEdge);
            childResult.endEdge.setTarget(loc);

            // Result: (loc, loc)
            return new MrrToCifResult(null, null, loc, loc);
        } else if (childResult.startIsEdge() && childResult.endIsLocation()) {
            // Connect start edge to end location, introducing infinite loop.
            // Connect: childResult.endLoc -> childResult.startEdge
            childResult.endLoc.getEdges().add(childResult.startEdge);

            // Result: (childResult.endLoc, childResult.endLoc)
            return new MrrToCifResult(null, null, childResult.endLoc, childResult.endLoc);
        } else if (childResult.startIsLocation() && childResult.endIsEdge()) {
            // Connect end edge to start location, introducing infinite loop.
            // Connect: childResult.endEdge -> childResult.startLoc
            childResult.endEdge.setTarget(childResult.startLoc);

            // Result: (childResult.startLoc, childResult.startLoc)
            return new MrrToCifResult(null, null, childResult.startLoc, childResult.startLoc);
        } else if (childResult.startIsLocation() && childResult.endIsLocation()) {
            // Merge locations, keeping childResult.startLoc, if applicable.
            if (childResult.startLoc != childResult.endLoc) {
                mergeLocIntoLoc(childResult.endLoc, childResult.startLoc, data);
            }

            // Result: (childResult.startLoc, childResult.startLoc)
            return new MrrToCifResult(null, null, childResult.startLoc, childResult.startLoc);
        } else {
            throw new RuntimeException("Unexpected plain infinite repeat case.");
        }
    }

    private static MrrToCifResult processRepeatFinitePlain(RepetitionMRR<CifMrrLetter> repeat, MrrToCifData data,
            MrrToCifMode mode)
    {
        // Consider the child the desired finite number of times.
        MrrToCifResult repeatResult = null;
        for (int i = 0; i < repeat.getModifiedCount(); i++) {
            // Process child.
            MrrToCifResult childResult = mrrToCif(repeat.getChild(), data, mode);

            // Combine with previous child results.
            if (repeatResult == null) {
                repeatResult = childResult;
            } else {
                // Connect: repeatResult.end -> childResult.start
                // Allow merging locations, as these are all internal to the word/MRR.
                repeatResult = processConcat(repeatResult, childResult, data, true);
            }
        }
        return repeatResult;
    }

    private static MrrToCifResult processRepeatData(RepetitionMRR<CifMrrLetter> repeat, MrrToCifData data) {
        // Introduce infinite loop.
        MrrToCifResult result = processRepeatInfinite(repeat, data, MrrToCifMode.DATA);

        // Get single start/end location of the loop.
        Preconditions.checkState(result.startIsLocation());
        Preconditions.checkState(result.endIsLocation());
        Preconditions.checkState(result.startLoc == result.endLoc);
        Location loopLoc = result.startLoc;

        // Get unique name for new variable to count loop executions.
        String name = COUNTER_NAME_BASE;
        Set<String> names = CifScopeUtils.getSymbolNamesForScope(data.automaton, null);
        if (names.contains(name)) {
            name = CifScopeUtils.getUniqueName(name, names, Collections.emptySet());
        }

        // Add new variable.
        DiscVariable var = newDiscVariable();
        var.setName(name);
        var.setType(newIntType(0, null, repeat.getModifiedCount()));
        data.automaton.getDeclarations().add(var);

        // Add 'tau' edge to enter the loop. Due to recursion on the MRR structure, we
        // don't have an existing edge for this yet. Also, if two loops are nested,
        // we need an extra edge to distinguish the inner loop from entering the inner
        // loop.
        TauExpression enterTauRef = newTauExpression(null, newBoolType());
        EdgeEvent enterTauEdgeEvent = newEdgeEvent();
        enterTauEdgeEvent.setEvent(enterTauRef);
        Edge enterTauEdge = newEdge();
        enterTauEdge.getEvents().add(enterTauEdgeEvent);

        enterTauEdge.setTarget(loopLoc);

        // Add guards to control loop entry.
        for (Edge edge: loopLoc.getEdges()) {
            // Create guard: cnt < 'modifiedRepetitionCount'
            BinaryExpression enterLoopGuard = newBinaryExpression();
            enterLoopGuard.setOperator(BinaryOperator.LESS_THAN);
            enterLoopGuard.setType(newBoolType());
            enterLoopGuard.setLeft(newDiscVariableExpression(null, deepclone(var.getType()), var));
            enterLoopGuard.setRight(newIntExpression(null, deepclone(var.getType()), repeat.getModifiedCount()));

            // Add guard.
            edge.getGuards().add(enterLoopGuard);
        }

        // Add assignments to increment loop execution counter.
        for (Location loc: data.automaton.getLocations()) {
            for (Edge edge: loc.getEdges()) {
                if (CifEdgeUtils.getTarget(edge) == loopLoc) {
                    // Create update: cnt := cnt + 1
                    Assignment incrAsgn = newAssignment();
                    incrAsgn.setAddressable(newDiscVariableExpression(null, deepclone(var.getType()), var));
                    BinaryExpression incrExpr = newBinaryExpression();
                    incrExpr.setOperator(BinaryOperator.ADDITION);
                    incrExpr.setType(deepclone(var.getType()));
                    incrExpr.setLeft(newDiscVariableExpression(null, deepclone(var.getType()), var));
                    incrExpr.setRight(newIntExpression(null, deepclone(var.getType()), 1));
                    incrAsgn.setValue(incrExpr);

                    // Add update.
                    edge.getUpdates().add(incrAsgn);
                }
            }
        }

        // Add 'tau' edge to exit the loop. Due to recursion on the MRR structure, we
        // don't have an existing edge for this yet. Also, if two loops are nested,
        // we need an extra edge to distinguish the inner loop from exiting the inner
        // loop.
        TauExpression exitTauRef = newTauExpression(null, newBoolType());
        EdgeEvent exitTauEdgeEvent = newEdgeEvent();
        exitTauEdgeEvent.setEvent(exitTauRef);
        Edge exitTauEdge = newEdge();
        exitTauEdge.getEvents().add(exitTauEdgeEvent);

        loopLoc.getEdges().add(exitTauEdge);

        // Add guard to control loop exit: cnt >= 'modifiedRepetitionCount'
        BinaryExpression exitLoopGuard = newBinaryExpression();
        exitLoopGuard.setOperator(BinaryOperator.EQUAL);
        exitLoopGuard.setType(newBoolType());
        exitLoopGuard.setLeft(newDiscVariableExpression(null, deepclone(var.getType()), var));
        exitLoopGuard.setRight(newIntExpression(null, deepclone(var.getType()), repeat.getModifiedCount()));

        exitTauEdge.getGuards().add(exitLoopGuard);

        // Add assignment to reset loop execution counter: cnt := 0
        // We do this on the exit loop edge, and not on the enter loop edge, to ensure
        // that for other parts of the model that we reach, we can't reach that with
        // either value 0 or the maximum value for our counter, as that would duplicate
        // that part of the statespace.
        Assignment resetAsgn = newAssignment();
        resetAsgn.setAddressable(newDiscVariableExpression(null, deepclone(var.getType()), var));
        IntExpression resetExpr = newIntExpression(null, deepclone(var.getType()), 0);
        resetAsgn.setValue(resetExpr);

        exitTauEdge.getUpdates().add(resetAsgn);

        // Return repeat result.
        return new MrrToCifResult(enterTauEdge, exitTauEdge, null, null);
    }

    private static void mergeLocIntoLoc(Location removeLoc, Location keepLoc, MrrToCifData data) {
        // Sanity checks.
        Preconditions.checkArgument(keepLoc != removeLoc);

        // Remove the location.
        EMFHelper.removeFromParentContainment(removeLoc);

        // Move edges.
        keepLoc.getEdges().addAll(removeLoc.getEdges());

        // Address initialization.
        if (!removeLoc.getInitials().isEmpty()) {
            Preconditions.checkArgument(CifValueUtils.isTriviallyTrue(removeLoc.getInitials(), true, true));
            Preconditions.checkArgument(keepLoc.getInitials().isEmpty());
            keepLoc.getInitials().addAll(removeLoc.getInitials());
        }

        // Redirect edge targets in entire automaton.
        for (Location loc: data.automaton.getLocations()) {
            for (Edge edge: loc.getEdges()) {
                Location target = CifEdgeUtils.getTarget(edge);
                if (target == removeLoc) {
                    edge.setTarget(keepLoc);
                }
            }
        }
    }

    private static void giveNamesToNewLocations(MrrToCifData data) {
        Set<String> names = CifScopeUtils.getSymbolNamesForScope(data.automaton, null);
        for (Location loc: data.automaton.getLocations()) {
            if (loc.getName() == null) {
                String name = "loc";
                name = CifScopeUtils.getUniqueName(name, names, set(name));
                loc.setName(name);
                names.add(name);
            }
        }
    }
}
