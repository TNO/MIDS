/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cif.extensions

import java.util.List
import java.util.Set
import org.eclipse.escet.cif.common.CifScopeUtils
import org.eclipse.escet.cif.metamodel.cif.automata.Assignment
import org.eclipse.escet.cif.metamodel.cif.automata.ElifUpdate
import org.eclipse.escet.cif.metamodel.cif.automata.IfUpdate
import org.eclipse.escet.cif.metamodel.cif.automata.Update
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression
import org.eclipse.escet.common.java.Sets

class ExpressionExtensions {
    def static Set<DiscVariable> getReferencedDiscVars(Expression expr) {
        val List<Expression> refExprs = newArrayList
        CifScopeUtils.collectRefExprs(expr, refExprs)
        val refDiscVars = getReferencedVariables(refExprs)
        return refDiscVars
    }

    /**
     * @return The set of all discrete variables that occur in {@code update}.
     */
    def static Set<DiscVariable> getReferencedDiscVars(Update update) {
        if (update instanceof Assignment) {
            return Sets.union(getReferencedDiscVars(update.addressable), getReferencedDiscVars(update.value))
        } else if (update instanceof IfUpdate) {
            val variables = Sets.set

            update.guards.forEach[guard|variables.addAll(getReferencedDiscVars(guard))]
            update.thens.forEach[then|variables.addAll(getReferencedDiscVars(then))]
            update.elifs.forEach[elif|variables.addAll(getReferencedDiscVars(elif))]
            update.elses.forEach[el|variables.addAll(getReferencedDiscVars(el))]

            return variables
        } else {
            throw new RuntimeException("Unknown update type.")
        }
    }

    /**
     * @return The set of all discrete variables that occur in {@code update}.
     */
    private def static Set<DiscVariable> getReferencedDiscVars(ElifUpdate update) {
        val variables = Sets.set

        update.guards.forEach[guard|variables.addAll(getReferencedDiscVars(guard))]
        update.thens.forEach[then|variables.addAll(getReferencedDiscVars(then))]

        return variables
    }

    private def static Set<DiscVariable> getReferencedVariables(List<Expression> refExprs) {
        val refObjs = refExprs.map[CifScopeUtils.getRefObjFromRef(it)]
        val refDiscVars = refObjs.filter(DiscVariable).toSet
        return refDiscVars
    }
}
