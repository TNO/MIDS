/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.postprocessing.operations

import com.google.common.base.Preconditions
import java.nio.file.Path
import java.util.Map
import java.util.Set
import nl.tno.mids.cif.extensions.AutomatonExtensions
import nl.tno.mids.cif.extensions.EdgeExtensions
import nl.tno.mids.cif.extensions.ExpressionExtensions
import nl.tno.mids.cmi.api.basic.CmiBasicComponentQueries
import nl.tno.mids.cmi.api.general.CmiGeneralAsyncConstraintsQueries
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries
import nl.tno.mids.cmi.api.info.EventInfo
import nl.tno.mids.cmi.postprocessing.PostProcessingModel
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCifSpec
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.escet.cif.common.CifValueUtils
import org.eclipse.escet.cif.metamodel.cif.Specification
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton
import org.eclipse.escet.cif.metamodel.cif.automata.Edge
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression
import org.eclipse.escet.cif.metamodel.java.CifConstructors
import org.eclipse.xtend.lib.annotations.Accessors

/**
 * Add constraints to the models to enforce asynchronous patterns (e.g. requests/replies). These constraints enforce 
 * that after a request, a corresponding reply must happen before the same request can happen again.
 */
@Accessors
abstract class AddAsyncPatternConstraintsBase<U extends PostProcessingOperationOptions> extends PostProcessingOperation<U> {

    override getPreconditionSubset() { return new PostProcessingPreconditionSubset(null, null); }

    override getResultSubset() { return new PostProcessingResultSubset(true, null); }

    override applyOperation(Map<String, PostProcessingModel> models, Set<String> selectedComponents,
        Path relativeResolvePath, IProgressMonitor monitor) {

        monitor.subTask(taskName)

        for (component : selectedComponents) {
            val model = models.get(component)
            preconditionSubset.ensureSubset(model)
            val cifSpec = model.cifSpec
            processModel(cifSpec)
            models.put(component, new PostProcessingModelCifSpec(cifSpec, component, getResultStatus(model.status)))
        }
    }

    abstract def String getTaskName()

    private def void processModel(Specification model) {
        // Query the model.
        val component = CmiBasicComponentQueries.getSingleComponentWithBehavior(model)
        val events = CmiGeneralEventQueries.getEvents(model)
        val eventInfoMap = events.toMap([it], [CmiGeneralEventQueries.getEventInfo(it)])

        // Process all edges.
        AutomatonExtensions.getAllEdges(component).forEach [ edge |
            // Process non-tau events.
            val event = EdgeExtensions.getEventDecl(edge, true)
            if (event !== null) {
                val eventInfo = eventInfoMap.get(event)
                if (isAsyncPatternStart(edge)) {
                    processAsyncPatternStart(component, edge, eventInfo)
                }
            }
        ]
    }

    private def void processAsyncPatternStart(Automaton component, Edge startEdge, EventInfo startEventInfo) {
        // Get matching pattern end edges.
        val endEdges = getMatchingPatternEndEdges(component, startEdge)

        // Add constraints for the pairs of start/end edges.
        for (endEdge : endEdges) {
            // Get constraint variable. We do it here and not earlier to prevent creating variables that are not needed.
            val constraintVariable = getOrCreateConstraintVariable(component, startEventInfo)

            // Add constraints.
            addConstraintToStartEdge(startEdge, constraintVariable)
            addConstraintToEndEdge(endEdge, constraintVariable)
        }

    }

    /** Is this event info for the start of an asynchronous pattern? */
    abstract def boolean isAsyncPatternStart(Edge edge)

    /** Collect matching end pattern edges for given pattern start event. */
    private def getMatchingPatternEndEdges(Automaton component, Edge startEdge) {
        val startEvent = EdgeExtensions.getEventDecl(startEdge, true)
        if (startEvent === null) {
            return newArrayList
        }

        val componentEdges = AutomatonExtensions.getAllEdges(component)
        val matchingEdges = componentEdges.filter [ edge |
            val otherEvent = EdgeExtensions.getEventDecl(edge, true)
            if (otherEvent === null) {
                return false
            } else {
                return isMatchingAsyncPatternEnd(startEdge, edge)
            }
        ]
        return matchingEdges.toList
    }

    /** Are the given start edge and other edge a matching asynchronous pattern pair? */
    abstract def boolean isMatchingAsyncPatternEnd(Edge startEdge, Edge otherEdge)

    /** Create variable representing the asynchronous constraint. */
    private def getOrCreateConstraintVariable(Automaton component, EventInfo eventInfo) {
        val variableName = eventInfo.toString.replace(".", "_") +
            CmiGeneralAsyncConstraintsQueries.ASYNC_PATTERN_CONSTRAINT_VAR_POSTFIX
        var variable = component.declarations.findFirst [ decl |
            decl.name == variableName
        ] as DiscVariable
        if (variable === null) {
            variable = CifConstructors.newDiscVariable
            variable.name = variableName
            variable.type = CifConstructors.newBoolType
            variable.value = CifConstructors.newVariableValue
            variable.value.values.add(CifValueUtils.makeFalse)
            component.declarations.add(variable)
        }
        return variable
    }

    /** Add guard and update to start edge of asynchronous constraint, if not yet present. */
    private def addConstraintToStartEdge(Edge startEdge, DiscVariable constraintVariable) {
        Preconditions.checkArgument(isAsyncPatternStart(startEdge))
        val variablesPresent = startEdge.guards.flatMap[ExpressionExtensions.getReferencedDiscVars(it)].toSet
        // If {@link constraintVariable} is already present, a guard and update have already been added. Because an edge 
        // cannot be both a start and an edge edge of a pattern, they must have been added by this method, so don't add 
        // them again.
        if (!variablesPresent.contains(constraintVariable)) {
            addGuardToEdge(startEdge, constraintVariable, CifValueUtils.makeFalse)
            startEdge.updates.add(makeVariableUpdate(constraintVariable, CifValueUtils.makeTrue))
        }
    }

    /** Add guard and update to end edge of asynchronous constraint, if not yet present. */
    private def addConstraintToEndEdge(Edge endEdge, DiscVariable constraintVariable) {
        Preconditions.checkArgument(!isAsyncPatternStart(endEdge))
        val variablesPresent = endEdge.guards.flatMap[ExpressionExtensions.getReferencedDiscVars(it)].toSet
        // If {@link constraintVariable} is already present, a guard and update have already been added. Because an edge 
        // cannot be both a start and an edge edge of a pattern, they must have been added by this method, so don't add 
        // them again.
        if (!variablesPresent.contains(constraintVariable)) {
            addGuardToEdge(endEdge, constraintVariable, CifValueUtils.makeTrue)
            endEdge.updates.add(makeVariableUpdate(constraintVariable, CifValueUtils.makeFalse))
        }
    }

    /** Add guard to a given edge comparing a given variable to a given value. */
    private def addGuardToEdge(Edge edge, DiscVariable constraintVariable, Expression value) {
        edge.guards.add(makeComparisonExpression(constraintVariable, value))
    }

    /** Create assignment setting a given variable to a given value. */
    private def makeVariableUpdate(DiscVariable variable, Expression value) {
        val varAddressable = CifConstructors.newDiscVariableExpression
        varAddressable.variable = variable
        varAddressable.type = CifConstructors.newBoolType

        val assignment = CifConstructors.newAssignment
        assignment.addressable = varAddressable
        assignment.value = value

        return assignment
    }

    /** Create expression comparing a given variable to a given value. */
    private def makeComparisonExpression(DiscVariable variable, Expression value) {
        val varLeft = CifConstructors.newDiscVariableExpression
        varLeft.variable = variable
        varLeft.type = CifConstructors.newBoolType

        val expr = CifConstructors.newBinaryExpression
        expr.left = varLeft
        expr.right = value
        expr.operator = BinaryOperator.EQUAL
        expr.type = CifConstructors.newBoolType

        return expr
    }
}
