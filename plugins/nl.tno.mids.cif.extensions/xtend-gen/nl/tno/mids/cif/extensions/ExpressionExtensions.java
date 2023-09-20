package nl.tno.mids.cif.extensions;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.eclipse.escet.cif.common.CifScopeUtils;
import org.eclipse.escet.cif.metamodel.cif.automata.Assignment;
import org.eclipse.escet.cif.metamodel.cif.automata.ElifUpdate;
import org.eclipse.escet.cif.metamodel.cif.automata.IfUpdate;
import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.common.java.Sets;
import org.eclipse.escet.common.position.metamodel.position.PositionObject;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;

@SuppressWarnings("all")
public class ExpressionExtensions {
  public static Set<DiscVariable> getReferencedDiscVars(final Expression expr) {
    final List<Expression> refExprs = CollectionLiterals.<Expression>newArrayList();
    CifScopeUtils.collectRefExprs(expr, refExprs);
    final Set<DiscVariable> refDiscVars = ExpressionExtensions.getReferencedVariables(refExprs);
    return refDiscVars;
  }
  
  /**
   * @return The set of all discrete variables that occur in {@code update}.
   */
  public static Set<DiscVariable> getReferencedDiscVars(final Update update) {
    if ((update instanceof Assignment)) {
      return Sets.<DiscVariable, DiscVariable, DiscVariable>union(ExpressionExtensions.getReferencedDiscVars(((Assignment)update).getAddressable()), ExpressionExtensions.getReferencedDiscVars(((Assignment)update).getValue()));
    } else {
      if ((update instanceof IfUpdate)) {
        final Set<DiscVariable> variables = Sets.<DiscVariable>set();
        final Consumer<Expression> _function = (Expression guard) -> {
          variables.addAll(ExpressionExtensions.getReferencedDiscVars(guard));
        };
        ((IfUpdate)update).getGuards().forEach(_function);
        final Consumer<Update> _function_1 = (Update then) -> {
          variables.addAll(ExpressionExtensions.getReferencedDiscVars(then));
        };
        ((IfUpdate)update).getThens().forEach(_function_1);
        final Consumer<ElifUpdate> _function_2 = (ElifUpdate elif) -> {
          variables.addAll(ExpressionExtensions.getReferencedDiscVars(elif));
        };
        ((IfUpdate)update).getElifs().forEach(_function_2);
        final Consumer<Update> _function_3 = (Update el) -> {
          variables.addAll(ExpressionExtensions.getReferencedDiscVars(el));
        };
        ((IfUpdate)update).getElses().forEach(_function_3);
        return variables;
      } else {
        throw new RuntimeException("Unknown update type.");
      }
    }
  }
  
  /**
   * @return The set of all discrete variables that occur in {@code update}.
   */
  private static Set<DiscVariable> getReferencedDiscVars(final ElifUpdate update) {
    final Set<DiscVariable> variables = Sets.<DiscVariable>set();
    final Consumer<Expression> _function = (Expression guard) -> {
      variables.addAll(ExpressionExtensions.getReferencedDiscVars(guard));
    };
    update.getGuards().forEach(_function);
    final Consumer<Update> _function_1 = (Update then) -> {
      variables.addAll(ExpressionExtensions.getReferencedDiscVars(then));
    };
    update.getThens().forEach(_function_1);
    return variables;
  }
  
  private static Set<DiscVariable> getReferencedVariables(final List<Expression> refExprs) {
    final Function1<Expression, PositionObject> _function = (Expression it) -> {
      return CifScopeUtils.getRefObjFromRef(it);
    };
    final List<PositionObject> refObjs = ListExtensions.<Expression, PositionObject>map(refExprs, _function);
    final Set<DiscVariable> refDiscVars = IterableExtensions.<DiscVariable>toSet(Iterables.<DiscVariable>filter(refObjs, DiscVariable.class));
    return refDiscVars;
  }
}
