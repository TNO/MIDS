package nl.tno.mids.cmi.api.general;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Set;
import nl.tno.mids.cif.extensions.ExpressionExtensions;
import org.eclipse.emf.common.util.EList;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.declarations.Declaration;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class CmiGeneralAsyncConstraintsQueries {
  public static final String ASYNC_PATTERN_CONSTRAINT_VAR_POSTFIX = "_async_var";
  
  /**
   * Does the given model or component contain a constraint enforcing an asynchronous pattern?
   * 
   * @param modelOrComponent The model or component.
   * @return {@code true} if the given model or component contains a constraint enforcing an asynchronous pattern,
   *      {@code false} otherwise.
   */
  public static boolean hasAsyncConstraints(final ComplexComponent modelOrComponent) {
    final List<Automaton> automata = CollectionLiterals.<Automaton>newArrayList();
    CifCollectUtils.<List<Automaton>>collectAutomata(modelOrComponent, automata);
    final Function1<Automaton, EList<Declaration>> _function = (Automaton it) -> {
      return it.getDeclarations();
    };
    final Function1<DiscVariable, Boolean> _function_1 = (DiscVariable it) -> {
      return Boolean.valueOf(CmiGeneralAsyncConstraintsQueries.isAsyncConstraintVariable(it));
    };
    return IterableExtensions.<DiscVariable>exists(Iterables.<DiscVariable>filter(IterableExtensions.<Automaton, Declaration>flatMap(automata, _function), DiscVariable.class), _function_1);
  }
  
  /**
   * Does the given edge contain a constraint enforcing an asynchronous pattern?
   * 
   * @param edge The edge.
   * @return {@code true} if the given edge contains a constraint enforcing an asynchronous pattern, {@code false}
   *      otherwise.
   */
  public static boolean hasAsyncConstraint(final Edge edge) {
    final Function1<Expression, Set<DiscVariable>> _function = (Expression it) -> {
      return ExpressionExtensions.getReferencedDiscVars(it);
    };
    final Set<DiscVariable> variables = IterableExtensions.<DiscVariable>toSet(IterableExtensions.<Expression, DiscVariable>flatMap(edge.getGuards(), _function));
    final Function1<Update, Set<DiscVariable>> _function_1 = (Update it) -> {
      return ExpressionExtensions.getReferencedDiscVars(it);
    };
    Iterables.<DiscVariable>addAll(variables, IterableExtensions.<Update, DiscVariable>flatMap(edge.getUpdates(), _function_1));
    final Function1<DiscVariable, Boolean> _function_2 = (DiscVariable it) -> {
      return Boolean.valueOf(CmiGeneralAsyncConstraintsQueries.isAsyncConstraintVariable(it));
    };
    return IterableExtensions.<DiscVariable>exists(variables, _function_2);
  }
  
  /**
   * Does the given variable corresponds to a constraint enforcing an asynchronous pattern?
   * 
   * @param variable The variable potentially matching such a constraint.
   * @return {@code true} if the given variable corresponds to a constraint enforcing an asynchronous pattern,
   *      {@code false} otherwise.
   */
  private static boolean isAsyncConstraintVariable(final DiscVariable variable) {
    return variable.getName().endsWith(CmiGeneralAsyncConstraintsQueries.ASYNC_PATTERN_CONSTRAINT_VAR_POSTFIX);
  }
}
