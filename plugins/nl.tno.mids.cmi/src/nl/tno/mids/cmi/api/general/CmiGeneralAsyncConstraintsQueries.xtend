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
import nl.tno.mids.cif.extensions.ExpressionExtensions
import org.eclipse.escet.cif.common.CifCollectUtils
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton
import org.eclipse.escet.cif.metamodel.cif.automata.Edge
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable

class CmiGeneralAsyncConstraintsQueries {
    public static val ASYNC_PATTERN_CONSTRAINT_VAR_POSTFIX = "_async_var"

    /**
     * Does the given model or component contain a constraint enforcing an asynchronous pattern?
     * 
     * @param modelOrComponent The model or component.
     * @return {@code true} if the given model or component contains a constraint enforcing an asynchronous pattern,
     *      {@code false} otherwise.
     */
    static def hasAsyncConstraints(ComplexComponent modelOrComponent) {
        val List<Automaton> automata = newArrayList
        CifCollectUtils.collectAutomata(modelOrComponent, automata)
        return automata.flatMap[declarations].filter(DiscVariable).exists[isAsyncConstraintVariable]
    }

    /**
     * Does the given edge contain a constraint enforcing an asynchronous pattern?
     * 
     * @param edge The edge.
     * @return {@code true} if the given edge contains a constraint enforcing an asynchronous pattern, {@code false}
     *      otherwise.
     */
    static def hasAsyncConstraint(Edge edge) {
        val variables = edge.guards.flatMap[ExpressionExtensions.getReferencedDiscVars(it)].toSet
        variables.addAll(edge.updates.flatMap[ExpressionExtensions.getReferencedDiscVars(it)])
        return variables.exists[isAsyncConstraintVariable]
    }

    /**
     * Does the given variable corresponds to a constraint enforcing an asynchronous pattern?
     * 
     * @param variable The variable potentially matching such a constraint.
     * @return {@code true} if the given variable corresponds to a constraint enforcing an asynchronous pattern,
     *      {@code false} otherwise.
     */
    private static def isAsyncConstraintVariable(DiscVariable variable) {
        return variable.name.endsWith(ASYNC_PATTERN_CONSTRAINT_VAR_POSTFIX)
    }
}
