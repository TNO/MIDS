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
import nl.tno.mids.cif.extensions.ExpressionExtensions
import nl.tno.mids.cif.extensions.mrr.cif.MrrToCif
import org.eclipse.escet.cif.common.CifCollectUtils
import org.eclipse.escet.cif.common.CifEdgeUtils
import org.eclipse.escet.cif.metamodel.cif.Specification
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton
import org.eclipse.escet.cif.metamodel.cif.automata.Edge
import org.eclipse.escet.cif.metamodel.cif.automata.Update
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryExpression
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator
import org.eclipse.escet.cif.metamodel.cif.expressions.DiscVariableExpression
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression
import org.eclipse.escet.cif.metamodel.cif.expressions.IntExpression
import org.eclipse.escet.common.java.Assert

class CmiGeneralDataQueries {
    /**
     * Does the given model contain edges with data references?
     * 
     * @param model The model.
     * @return {@code true} if the model contains at least one automaton with at least one edge containing an update or
     *      a guard referencing data, {@code false} otherwise.
     */
    static def hasData(Specification model) {
        val List<Automaton> automata = newArrayList()
        CifCollectUtils.collectAutomata(model, automata)
        return automata.exists[hasData]
    }

    /**
     * Does the given CIF automaton contain edges with data references?
     * 
     * @param automaton The CIF automaton.
     * @return {@code true} if the automaton contains at least one edge containing an update or a guard referencing
     *      data, {@code false} otherwise.
     */
    private static def hasData(Automaton automaton) {
        return automaton.locations.exists[edges.exists[hasData]]
    }

    /**
     * Does the given edge reference data?
     * 
     * @param edge The edge.
     * @return {@code true} if the edge has an update or a guard referencing data, {@code false} otherwise.
     */
    private static def hasData(Edge edge) {
        return !edge.updates.empty || edge.guards.exists[ExpressionExtensions.getReferencedDiscVars(it).size > 0]
    }

    /**
     * Does the given model have repetitions constrained using data?
     * 
     * @param model The model.
     * @return {@code true} if the model has repetitions constrained by data, {@code false} otherwise.
     */
    static def hasDataRepetitions(Specification model) {
        val List<Automaton> automata = newArrayList()
        CifCollectUtils.collectAutomata(model, automata)
        return automata.exists[it.locations.exists[it.edges.exists[isDataRepetitionStart]]]
    }

    /**
     * Is the given edge an entry edge for a repetition?
     * 
     * @param edge The given edge.
     * @return {@code true} if the given edge is a entry edge for a repetition, {@code false} otherwise.
     */
    static def isDataRepetitionEntry(Edge edge) {
        // 'edge' must be a tau edge.
        if (EdgeExtensions.getEventDecl(edge, true) !== null) {
            return false
        }

        // The target location of 'edge' must start a repetition.
        if (!CifEdgeUtils.getTarget(edge).edges.exists[isDataRepetitionStart]) {
            return false
        }

        // Left to do: check that 'edge' does not have repetition-related updates.
        // Justification: the target location of 'edge' can have only two kinds of incoming edges, namely (1) repetition
        // entries, and (2) edges that mark the end of a repetition which have an update of the form 'cnt := cnt + 1'.
        // Since 'edge' must be either (1) or (2), it must in any case be repetition-related. To check that 'edge'
        // is not (2), it is sufficient to check that 'edge' does not have repetition-related updates.
        return edge.updates.forall[!isRepetitionUpdate(it)]
    }

    /**
     * Is the given edge an exit edge for a repetition?
     * 
     * @param edge The given edge.
     * @return {@code true} if the given edge is an exit edge for a repetition, {@code false} otherwise.
     */
    static def isDataRepetitionExit(Edge edge) {
        // 'edge' must be a tau edge.
        if (EdgeExtensions.getEventDecl(edge, true) !== null) {
            return false
        }

        // Left to do: check that 'edge' has a guard that is of the form 'cnt = N'.
        // Justification: if 'edge' has such a guard then 'edge' must in any case be repetition-related. Moreover, any
        // repetition-related edge with such a guard must be a repetition exit edge (there are no conflicts possible).
        for (guard : edge.getGuards()) {
            if (guard instanceof BinaryExpression) {
                if (guard.getLeft() instanceof DiscVariableExpression && guard.getOperator() == BinaryOperator.EQUAL &&
                    guard.getRight() instanceof IntExpression) {

                    if (isRepetitionVariable((guard.getLeft() as DiscVariableExpression).getVariable())) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /**
     * Is the given edge a start edge for a repetition?
     * 
     * @param edge The given edge.
     * @return {@code true} if the given edge is a start edge for a repetition, {@code false} otherwise.
     */
    static def isDataRepetitionStart(Edge edge) {
        // Find 'cnt < iterationCount' guard.
        for (guard : edge.getGuards()) {
            if (guard instanceof BinaryExpression) {
                if (guard.getLeft() instanceof DiscVariableExpression &&
                    guard.getOperator() == BinaryOperator.LESS_THAN && guard.getRight() instanceof IntExpression) {

                    if (isRepetitionVariable((guard.getLeft() as DiscVariableExpression).getVariable())) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Get the number of iterations for a repetition.
     * 
     * @param edge The edge representing the {@link #isDataRepetitionStart start of a repetition}.
     * @return The iteration count of the repetition.
     */
    static def getDataRepetitionCount(Edge edge) {
        Assert.check(isDataRepetitionStart(edge))

        // Find 'cnt < iterationCount' guard.
        var Integer iterationCount = null;
        for (guard : edge.getGuards()) {
            if (guard instanceof BinaryExpression) {
                if (guard.getLeft() instanceof DiscVariableExpression &&
                    guard.getOperator() == BinaryOperator.LESS_THAN && guard.getRight() instanceof IntExpression) {

                    if (isRepetitionVariable((guard.getLeft() as DiscVariableExpression).getVariable())) {
                        // Repetition (start) found.
                        Assert.check(iterationCount === null);
                        iterationCount = (guard.getRight() as IntExpression).getValue()
                    }
                }
            }
        }

        // Return iteration count.
        Assert.notNull(iterationCount)
        return iterationCount
    }

    /**
     * @return {@code true} if the specified variable name is related to repetition data, or {@code false} otherwise.
     */
    static def isRepetitionVariableName(String varName) {
        return varName.startsWith(MrrToCif.COUNTER_NAME_BASE)
    }

    /**
     * @return {@code true} if the specified variable is related to repetition data, or {@code false} otherwise.
     */
    static def isRepetitionVariable(DiscVariable variable) {
        return isRepetitionVariableName(variable.name)
    }

    /**
     * Indicates whether the specified {@code guard} is a guard that only involves repetition-related data.
     */
    static def isRepetitionGuard(Expression guard) {
        return ExpressionExtensions.getReferencedDiscVars(guard).forall[it.isRepetitionVariable]
    }

    /**
     * Indicates whether the specified {@code update} is an update that only involves repetition-related data.
     */
    static def isRepetitionUpdate(Update update) {
        return ExpressionExtensions.getReferencedDiscVars(update).forall[it.isRepetitionVariable]
    }
}
