package nl.tno.mids.cmi.api.general;

import com.google.common.base.Objects;
import java.util.List;
import nl.tno.mids.cif.extensions.EdgeExtensions;
import nl.tno.mids.cif.extensions.ExpressionExtensions;
import nl.tno.mids.cif.extensions.mrr.cif.MrrToCif;
import org.eclipse.emf.common.util.EList;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifEdgeUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator;
import org.eclipse.escet.cif.metamodel.cif.expressions.DiscVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.IntExpression;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class CmiGeneralDataQueries {
  /**
   * Does the given model contain edges with data references?
   * 
   * @param model The model.
   * @return {@code true} if the model contains at least one automaton with at least one edge containing an update or
   *      a guard referencing data, {@code false} otherwise.
   */
  public static boolean hasData(final Specification model) {
    final List<Automaton> automata = CollectionLiterals.<Automaton>newArrayList();
    CifCollectUtils.<List<Automaton>>collectAutomata(model, automata);
    final Function1<Automaton, Boolean> _function = (Automaton it) -> {
      return Boolean.valueOf(CmiGeneralDataQueries.hasData(it));
    };
    return IterableExtensions.<Automaton>exists(automata, _function);
  }
  
  /**
   * Does the given CIF automaton contain edges with data references?
   * 
   * @param automaton The CIF automaton.
   * @return {@code true} if the automaton contains at least one edge containing an update or a guard referencing
   *      data, {@code false} otherwise.
   */
  private static boolean hasData(final Automaton automaton) {
    final Function1<Location, Boolean> _function = (Location it) -> {
      final Function1<Edge, Boolean> _function_1 = (Edge it_1) -> {
        return Boolean.valueOf(CmiGeneralDataQueries.hasData(it_1));
      };
      return Boolean.valueOf(IterableExtensions.<Edge>exists(it.getEdges(), _function_1));
    };
    return IterableExtensions.<Location>exists(automaton.getLocations(), _function);
  }
  
  /**
   * Does the given edge reference data?
   * 
   * @param edge The edge.
   * @return {@code true} if the edge has an update or a guard referencing data, {@code false} otherwise.
   */
  private static boolean hasData(final Edge edge) {
    return ((!edge.getUpdates().isEmpty()) || IterableExtensions.<Expression>exists(edge.getGuards(), ((Function1<Expression, Boolean>) (Expression it) -> {
      int _size = ExpressionExtensions.getReferencedDiscVars(it).size();
      return Boolean.valueOf((_size > 0));
    })));
  }
  
  /**
   * Does the given model have repetitions constrained using data?
   * 
   * @param model The model.
   * @return {@code true} if the model has repetitions constrained by data, {@code false} otherwise.
   */
  public static boolean hasDataRepetitions(final Specification model) {
    final List<Automaton> automata = CollectionLiterals.<Automaton>newArrayList();
    CifCollectUtils.<List<Automaton>>collectAutomata(model, automata);
    final Function1<Automaton, Boolean> _function = (Automaton it) -> {
      final Function1<Location, Boolean> _function_1 = (Location it_1) -> {
        final Function1<Edge, Boolean> _function_2 = (Edge it_2) -> {
          return Boolean.valueOf(CmiGeneralDataQueries.isDataRepetitionStart(it_2));
        };
        return Boolean.valueOf(IterableExtensions.<Edge>exists(it_1.getEdges(), _function_2));
      };
      return Boolean.valueOf(IterableExtensions.<Location>exists(it.getLocations(), _function_1));
    };
    return IterableExtensions.<Automaton>exists(automata, _function);
  }
  
  /**
   * Is the given edge an entry edge for a repetition?
   * 
   * @param edge The given edge.
   * @return {@code true} if the given edge is a entry edge for a repetition, {@code false} otherwise.
   */
  public static boolean isDataRepetitionEntry(final Edge edge) {
    Event _eventDecl = EdgeExtensions.getEventDecl(edge, true);
    boolean _tripleNotEquals = (_eventDecl != null);
    if (_tripleNotEquals) {
      return false;
    }
    final Function1<Edge, Boolean> _function = (Edge it) -> {
      return Boolean.valueOf(CmiGeneralDataQueries.isDataRepetitionStart(it));
    };
    boolean _exists = IterableExtensions.<Edge>exists(CifEdgeUtils.getTarget(edge).getEdges(), _function);
    boolean _not = (!_exists);
    if (_not) {
      return false;
    }
    final Function1<Update, Boolean> _function_1 = (Update it) -> {
      boolean _isRepetitionUpdate = CmiGeneralDataQueries.isRepetitionUpdate(it);
      return Boolean.valueOf((!_isRepetitionUpdate));
    };
    return IterableExtensions.<Update>forall(edge.getUpdates(), _function_1);
  }
  
  /**
   * Is the given edge an exit edge for a repetition?
   * 
   * @param edge The given edge.
   * @return {@code true} if the given edge is an exit edge for a repetition, {@code false} otherwise.
   */
  public static boolean isDataRepetitionExit(final Edge edge) {
    Event _eventDecl = EdgeExtensions.getEventDecl(edge, true);
    boolean _tripleNotEquals = (_eventDecl != null);
    if (_tripleNotEquals) {
      return false;
    }
    EList<Expression> _guards = edge.getGuards();
    for (final Expression guard : _guards) {
      if ((guard instanceof BinaryExpression)) {
        if ((((((BinaryExpression)guard).getLeft() instanceof DiscVariableExpression) && Objects.equal(((BinaryExpression)guard).getOperator(), BinaryOperator.EQUAL)) && 
          (((BinaryExpression)guard).getRight() instanceof IntExpression))) {
          Expression _left = ((BinaryExpression)guard).getLeft();
          boolean _isRepetitionVariable = CmiGeneralDataQueries.isRepetitionVariable(((DiscVariableExpression) _left).getVariable());
          if (_isRepetitionVariable) {
            return true;
          }
        }
      }
    }
    return false;
  }
  
  /**
   * Is the given edge a start edge for a repetition?
   * 
   * @param edge The given edge.
   * @return {@code true} if the given edge is a start edge for a repetition, {@code false} otherwise.
   */
  public static boolean isDataRepetitionStart(final Edge edge) {
    EList<Expression> _guards = edge.getGuards();
    for (final Expression guard : _guards) {
      if ((guard instanceof BinaryExpression)) {
        if ((((((BinaryExpression)guard).getLeft() instanceof DiscVariableExpression) && 
          Objects.equal(((BinaryExpression)guard).getOperator(), BinaryOperator.LESS_THAN)) && (((BinaryExpression)guard).getRight() instanceof IntExpression))) {
          Expression _left = ((BinaryExpression)guard).getLeft();
          boolean _isRepetitionVariable = CmiGeneralDataQueries.isRepetitionVariable(((DiscVariableExpression) _left).getVariable());
          if (_isRepetitionVariable) {
            return true;
          }
        }
      }
    }
    return false;
  }
  
  /**
   * Get the number of iterations for a repetition.
   * 
   * @param edge The edge representing the {@link #isDataRepetitionStart start of a repetition}.
   * @return The iteration count of the repetition.
   */
  public static Integer getDataRepetitionCount(final Edge edge) {
    Assert.check(CmiGeneralDataQueries.isDataRepetitionStart(edge));
    Integer iterationCount = null;
    EList<Expression> _guards = edge.getGuards();
    for (final Expression guard : _guards) {
      if ((guard instanceof BinaryExpression)) {
        if ((((((BinaryExpression)guard).getLeft() instanceof DiscVariableExpression) && 
          Objects.equal(((BinaryExpression)guard).getOperator(), BinaryOperator.LESS_THAN)) && (((BinaryExpression)guard).getRight() instanceof IntExpression))) {
          Expression _left = ((BinaryExpression)guard).getLeft();
          boolean _isRepetitionVariable = CmiGeneralDataQueries.isRepetitionVariable(((DiscVariableExpression) _left).getVariable());
          if (_isRepetitionVariable) {
            Assert.check((iterationCount == null));
            Expression _right = ((BinaryExpression)guard).getRight();
            iterationCount = Integer.valueOf(((IntExpression) _right).getValue());
          }
        }
      }
    }
    Assert.notNull(iterationCount);
    return iterationCount;
  }
  
  /**
   * @return {@code true} if the specified variable name is related to repetition data, or {@code false} otherwise.
   */
  public static boolean isRepetitionVariableName(final String varName) {
    return varName.startsWith(MrrToCif.COUNTER_NAME_BASE);
  }
  
  /**
   * @return {@code true} if the specified variable is related to repetition data, or {@code false} otherwise.
   */
  public static boolean isRepetitionVariable(final DiscVariable variable) {
    return CmiGeneralDataQueries.isRepetitionVariableName(variable.getName());
  }
  
  /**
   * Indicates whether the specified {@code guard} is a guard that only involves repetition-related data.
   */
  public static boolean isRepetitionGuard(final Expression guard) {
    final Function1<DiscVariable, Boolean> _function = (DiscVariable it) -> {
      return Boolean.valueOf(CmiGeneralDataQueries.isRepetitionVariable(it));
    };
    return IterableExtensions.<DiscVariable>forall(ExpressionExtensions.getReferencedDiscVars(guard), _function);
  }
  
  /**
   * Indicates whether the specified {@code update} is an update that only involves repetition-related data.
   */
  public static boolean isRepetitionUpdate(final Update update) {
    final Function1<DiscVariable, Boolean> _function = (DiscVariable it) -> {
      return Boolean.valueOf(CmiGeneralDataQueries.isRepetitionVariable(it));
    };
    return IterableExtensions.<DiscVariable>forall(ExpressionExtensions.getReferencedDiscVars(update), _function);
  }
}
