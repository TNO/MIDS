package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import nl.tno.mids.cif.extensions.AutomatonExtensions;
import nl.tno.mids.cif.extensions.EdgeExtensions;
import nl.tno.mids.cif.extensions.ExpressionExtensions;
import nl.tno.mids.cmi.api.basic.CmiBasicComponentQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralAsyncConstraintsQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries;
import nl.tno.mids.cmi.api.info.EventInfo;
import nl.tno.mids.cmi.postprocessing.PostProcessingModel;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCifSpec;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Assignment;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.declarations.Declaration;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator;
import org.eclipse.escet.cif.metamodel.cif.expressions.DiscVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

/**
 * Add constraints to the models to enforce asynchronous patterns (e.g. requests/replies). These constraints enforce
 * that after a request, a corresponding reply must happen before the same request can happen again.
 */
@Accessors
@SuppressWarnings("all")
public abstract class AddAsyncPatternConstraintsBase<U extends PostProcessingOperationOptions> extends PostProcessingOperation<U> {
  @Override
  public PostProcessingPreconditionSubset getPreconditionSubset() {
    return new PostProcessingPreconditionSubset(null, null);
  }
  
  @Override
  public PostProcessingResultSubset getResultSubset() {
    return new PostProcessingResultSubset(Boolean.valueOf(true), null);
  }
  
  @Override
  public void applyOperation(final Map<String, PostProcessingModel> models, final Set<String> selectedComponents, final Path relativeResolvePath, final IProgressMonitor monitor) {
    monitor.subTask(this.getTaskName());
    for (final String component : selectedComponents) {
      {
        final PostProcessingModel model = models.get(component);
        this.getPreconditionSubset().ensureSubset(model);
        final Specification cifSpec = model.getCifSpec();
        this.processModel(cifSpec);
        PostProcessingStatus _resultStatus = this.getResultStatus(model.status);
        PostProcessingModelCifSpec _postProcessingModelCifSpec = new PostProcessingModelCifSpec(cifSpec, component, _resultStatus);
        models.put(component, _postProcessingModelCifSpec);
      }
    }
  }
  
  public abstract String getTaskName();
  
  private void processModel(final Specification model) {
    final Automaton component = CmiBasicComponentQueries.getSingleComponentWithBehavior(model);
    final List<Event> events = CmiGeneralEventQueries.getEvents(model);
    final Function1<Event, Event> _function = (Event it) -> {
      return it;
    };
    final Function1<Event, EventInfo> _function_1 = (Event it) -> {
      return CmiGeneralEventQueries.getEventInfo(it);
    };
    final Map<Event, EventInfo> eventInfoMap = IterableExtensions.<Event, Event, EventInfo>toMap(events, _function, _function_1);
    final Consumer<Edge> _function_2 = (Edge edge) -> {
      final Event event = EdgeExtensions.getEventDecl(edge, true);
      if ((event != null)) {
        final EventInfo eventInfo = eventInfoMap.get(event);
        boolean _isAsyncPatternStart = this.isAsyncPatternStart(edge);
        if (_isAsyncPatternStart) {
          this.processAsyncPatternStart(component, edge, eventInfo);
        }
      }
    };
    AutomatonExtensions.getAllEdges(component).forEach(_function_2);
  }
  
  private void processAsyncPatternStart(final Automaton component, final Edge startEdge, final EventInfo startEventInfo) {
    final List<Edge> endEdges = this.getMatchingPatternEndEdges(component, startEdge);
    for (final Edge endEdge : endEdges) {
      {
        final DiscVariable constraintVariable = this.getOrCreateConstraintVariable(component, startEventInfo);
        this.addConstraintToStartEdge(startEdge, constraintVariable);
        this.addConstraintToEndEdge(endEdge, constraintVariable);
      }
    }
  }
  
  /**
   * Is this event info for the start of an asynchronous pattern?
   */
  public abstract boolean isAsyncPatternStart(final Edge edge);
  
  /**
   * Collect matching end pattern edges for given pattern start event.
   */
  private List<Edge> getMatchingPatternEndEdges(final Automaton component, final Edge startEdge) {
    final Event startEvent = EdgeExtensions.getEventDecl(startEdge, true);
    if ((startEvent == null)) {
      return CollectionLiterals.<Edge>newArrayList();
    }
    final Set<Edge> componentEdges = AutomatonExtensions.getAllEdges(component);
    final Function1<Edge, Boolean> _function = (Edge edge) -> {
      final Event otherEvent = EdgeExtensions.getEventDecl(edge, true);
      if ((otherEvent == null)) {
        return Boolean.valueOf(false);
      } else {
        return Boolean.valueOf(this.isMatchingAsyncPatternEnd(startEdge, edge));
      }
    };
    final Iterable<Edge> matchingEdges = IterableExtensions.<Edge>filter(componentEdges, _function);
    return IterableExtensions.<Edge>toList(matchingEdges);
  }
  
  /**
   * Are the given start edge and other edge a matching asynchronous pattern pair?
   */
  public abstract boolean isMatchingAsyncPatternEnd(final Edge startEdge, final Edge otherEdge);
  
  /**
   * Create variable representing the asynchronous constraint.
   */
  private DiscVariable getOrCreateConstraintVariable(final Automaton component, final EventInfo eventInfo) {
    String _replace = eventInfo.toString().replace(".", "_");
    final String variableName = (_replace + 
      CmiGeneralAsyncConstraintsQueries.ASYNC_PATTERN_CONSTRAINT_VAR_POSTFIX);
    final Function1<Declaration, Boolean> _function = (Declaration decl) -> {
      String _name = decl.getName();
      return Boolean.valueOf(Objects.equal(_name, variableName));
    };
    Declaration _findFirst = IterableExtensions.<Declaration>findFirst(component.getDeclarations(), _function);
    DiscVariable variable = ((DiscVariable) _findFirst);
    if ((variable == null)) {
      variable = CifConstructors.newDiscVariable();
      variable.setName(variableName);
      variable.setType(CifConstructors.newBoolType());
      variable.setValue(CifConstructors.newVariableValue());
      variable.getValue().getValues().add(CifValueUtils.makeFalse());
      component.getDeclarations().add(variable);
    }
    return variable;
  }
  
  /**
   * Add guard and update to start edge of asynchronous constraint, if not yet present.
   */
  private boolean addConstraintToStartEdge(final Edge startEdge, final DiscVariable constraintVariable) {
    boolean _xblockexpression = false;
    {
      Preconditions.checkArgument(this.isAsyncPatternStart(startEdge));
      final Function1<Expression, Set<DiscVariable>> _function = (Expression it) -> {
        return ExpressionExtensions.getReferencedDiscVars(it);
      };
      final Set<DiscVariable> variablesPresent = IterableExtensions.<DiscVariable>toSet(IterableExtensions.<Expression, DiscVariable>flatMap(startEdge.getGuards(), _function));
      boolean _xifexpression = false;
      boolean _contains = variablesPresent.contains(constraintVariable);
      boolean _not = (!_contains);
      if (_not) {
        boolean _xblockexpression_1 = false;
        {
          this.addGuardToEdge(startEdge, constraintVariable, CifValueUtils.makeFalse());
          _xblockexpression_1 = startEdge.getUpdates().add(this.makeVariableUpdate(constraintVariable, CifValueUtils.makeTrue()));
        }
        _xifexpression = _xblockexpression_1;
      }
      _xblockexpression = _xifexpression;
    }
    return _xblockexpression;
  }
  
  /**
   * Add guard and update to end edge of asynchronous constraint, if not yet present.
   */
  private boolean addConstraintToEndEdge(final Edge endEdge, final DiscVariable constraintVariable) {
    boolean _xblockexpression = false;
    {
      boolean _isAsyncPatternStart = this.isAsyncPatternStart(endEdge);
      boolean _not = (!_isAsyncPatternStart);
      Preconditions.checkArgument(_not);
      final Function1<Expression, Set<DiscVariable>> _function = (Expression it) -> {
        return ExpressionExtensions.getReferencedDiscVars(it);
      };
      final Set<DiscVariable> variablesPresent = IterableExtensions.<DiscVariable>toSet(IterableExtensions.<Expression, DiscVariable>flatMap(endEdge.getGuards(), _function));
      boolean _xifexpression = false;
      boolean _contains = variablesPresent.contains(constraintVariable);
      boolean _not_1 = (!_contains);
      if (_not_1) {
        boolean _xblockexpression_1 = false;
        {
          this.addGuardToEdge(endEdge, constraintVariable, CifValueUtils.makeTrue());
          _xblockexpression_1 = endEdge.getUpdates().add(this.makeVariableUpdate(constraintVariable, CifValueUtils.makeFalse()));
        }
        _xifexpression = _xblockexpression_1;
      }
      _xblockexpression = _xifexpression;
    }
    return _xblockexpression;
  }
  
  /**
   * Add guard to a given edge comparing a given variable to a given value.
   */
  private boolean addGuardToEdge(final Edge edge, final DiscVariable constraintVariable, final Expression value) {
    return edge.getGuards().add(this.makeComparisonExpression(constraintVariable, value));
  }
  
  /**
   * Create assignment setting a given variable to a given value.
   */
  private Assignment makeVariableUpdate(final DiscVariable variable, final Expression value) {
    final DiscVariableExpression varAddressable = CifConstructors.newDiscVariableExpression();
    varAddressable.setVariable(variable);
    varAddressable.setType(CifConstructors.newBoolType());
    final Assignment assignment = CifConstructors.newAssignment();
    assignment.setAddressable(varAddressable);
    assignment.setValue(value);
    return assignment;
  }
  
  /**
   * Create expression comparing a given variable to a given value.
   */
  private BinaryExpression makeComparisonExpression(final DiscVariable variable, final Expression value) {
    final DiscVariableExpression varLeft = CifConstructors.newDiscVariableExpression();
    varLeft.setVariable(variable);
    varLeft.setType(CifConstructors.newBoolType());
    final BinaryExpression expr = CifConstructors.newBinaryExpression();
    expr.setLeft(varLeft);
    expr.setRight(value);
    expr.setOperator(BinaryOperator.EQUAL);
    expr.setType(CifConstructors.newBoolType());
    return expr;
  }
  
  public AddAsyncPatternConstraintsBase(final U options) {
    super(options);
  }
}
