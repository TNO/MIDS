package nl.tno.mids.cif.extensions;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.EList;
import org.eclipse.escet.cif.common.CifEdgeUtils;
import org.eclipse.escet.cif.common.CifEventUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeReceive;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeSend;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.TauExpression;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;

@SuppressWarnings("all")
public class EdgeExtensions {
  /**
   * Returns the single event on this edge. Throws a exception if multiple events are declared on this edge.
   * If {@code allowTau} is false, an exception is thrown as well if the edge has no event, or the event is represented
   * by a TauExpression.
   * 
   * <p>If {@code allowTau} is true, the edge might return null if tau is on the edge. In order to check for multiple
   * event, tau is considered an event, though it will not be returned as such.</p>
   */
  public static Event getEventDecl(final Edge edge, final boolean allowTau) {
    final List<Event> list = EdgeExtensions.getEventDecls(edge, allowTau);
    int _size = list.size();
    boolean _equals = (_size == 1);
    if (_equals) {
      return list.get(0);
    } else {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("Called getEvent while multiple events exist for edge (");
      String _name = CifEdgeUtils.getSource(edge).getName();
      _builder.append(_name);
      _builder.append("->");
      String _name_1 = EdgeExtensions.getDestination(edge).getName();
      _builder.append(_name_1);
      _builder.append("), namely ");
      final Function1<Event, CharSequence> _function = (Event e) -> {
        String _xifexpression = null;
        if ((e == null)) {
          _xifexpression = "tau";
        } else {
          _xifexpression = e.getName();
        }
        return _xifexpression;
      };
      String _join = IterableExtensions.<Event>join(list, "", ", ", ".", _function);
      String _plus = (_builder.toString() + _join);
      throw new RuntimeException(_plus);
    }
  }
  
  /**
   * Returns the list of all events declared on this edge.
   * 
   * <p>If {@code allowTau} is false, an exception is thrown as well if the edge has no event, or an event is represented
   * by a TauExpression.</p>
   * 
   * <p>If {@code allowTau} is true, null will be added to the list when no edgeEvent is found, or for each event that is
   * represented by a TauExpression.</p>
   */
  public static List<Event> getEventDecls(final Edge edge, final boolean allowTau) {
    final Function1<EdgeEvent, Expression> _function = (EdgeEvent it) -> {
      return it.getEvent();
    };
    final Function1<EventExpression, Event> _function_1 = (EventExpression it) -> {
      return it.getEvent();
    };
    final List<Event> evts = IterableExtensions.<Event>toList(IterableExtensions.<EventExpression, Event>map(Iterables.<EventExpression>filter(ListExtensions.<EdgeEvent, Expression>map(edge.getEvents(), _function), EventExpression.class), _function_1));
    if (allowTau) {
      final Function1<EdgeEvent, Expression> _function_2 = (EdgeEvent it) -> {
        return it.getEvent();
      };
      final Consumer<TauExpression> _function_3 = (TauExpression it) -> {
        evts.add(null);
      };
      Iterables.<TauExpression>filter(ListExtensions.<EdgeEvent, Expression>map(edge.getEvents(), _function_2), TauExpression.class).forEach(_function_3);
      boolean _isEmpty = edge.getEvents().isEmpty();
      if (_isEmpty) {
        evts.add(null);
      }
    } else {
      if (((!IterableExtensions.isEmpty(Iterables.<TauExpression>filter(ListExtensions.<EdgeEvent, Expression>map(edge.getEvents(), ((Function1<EdgeEvent, Expression>) (EdgeEvent it) -> {
        return it.getEvent();
      })), TauExpression.class))) || edge.getEvents().isEmpty())) {
        throw new RuntimeException("Found tau event on edge, while this is not allowed by allowTau");
      }
    }
    return evts;
  }
  
  /**
   * Returns the target location of this Edge
   * 
   * <p>This is a convenience method, to allow for the use of CifLocationUtils::getTarget as extension, and avoid conflict
   * with Edge::getTarget</p>
   */
  public static Location getDestination(final Edge edge) {
    return CifEdgeUtils.getTarget(edge);
  }
  
  /**
   * Returns whether the edge is communicating within {@code context}, that is, whether it synchronizes across two
   * automata in {@code context}, or participates in either sending or receiving in channel communication.
   * Whether there is actually another party participating in this channel communication is not checked.
   * Assumes alphabets in automata are up to date when present. For more information, see {@link #isSynchronizedIn}.
   */
  public static boolean isCommunicatingIn(final Edge edge, final Collection<? extends ComplexComponent> context) {
    EList<EdgeEvent> _events = edge.getEvents();
    for (final EdgeEvent edgeEvent : _events) {
      if (((edgeEvent instanceof EdgeSend) || (edgeEvent instanceof EdgeReceive))) {
        return true;
      }
    }
    return EdgeExtensions.isSynchronizedIn(edge, context);
  }
  
  /**
   * Returns whether the edge has an event that synchronizes with an event in another automaton, that is, whether
   * there are two automata in {@code context} that have the event in its alphabet.
   * 
   * <p>This function makes heavy use of automata alphabets, so in order
   * to speed this up, a call to {@link automaton#updateAlphabet} might be worthwhile, to force explicit storing of
   * alphabets. If an alphabet is present in the automaton, ensure it is up to date. </p>
   */
  private static boolean isSynchronizedIn(final Edge edge, final Collection<? extends ComplexComponent> context) {
    final Function1<Event, Boolean> _function = (Event it) -> {
      return Boolean.valueOf(EdgeExtensions.isSynchronizedIn(it, context));
    };
    int _size = IterableExtensions.size(IterableExtensions.<Event>filter(IterableExtensions.<Event>filterNull(EdgeExtensions.getEventDecls(edge, true)), _function));
    return (_size > 0);
  }
  
  /**
   * Returns whether the the event synchronizes across two automata in {@code context}, that is, whether
   * there are two automata in {@code context} that have the event in its alphabet.
   * 
   * <p>This function makes heavy use of automata alphabets, so in order
   * to speed this up, a call to {@link automaton#updateAlphabet} might be worthwhile, to force explicit storing of
   * alphabets. If an alphabet is present in the automaton, ensure it is up to date. </p>
   */
  private static boolean isSynchronizedIn(final Event event, final Collection<? extends ComplexComponent> context) {
    final Function1<ComplexComponent, Set<Automaton>> _function = (ComplexComponent it) -> {
      return CifExtensions.allAutomata(it);
    };
    final List<Set<Event>> alphabets = CifEventUtils.getAlphabets(CollectionLiterals.<Automaton>newLinkedList(((Automaton[])Conversions.unwrapArray(IterableExtensions.flatMap(context, _function), Automaton.class))));
    final Function1<Set<Event>, Boolean> _function_1 = (Set<Event> it) -> {
      final Function1<Event, Boolean> _function_2 = (Event it_1) -> {
        String _absName = CifTextUtils.getAbsName(it_1, false);
        String _absName_1 = CifTextUtils.getAbsName(event, false);
        return Boolean.valueOf(Objects.equal(_absName, _absName_1));
      };
      return Boolean.valueOf(IterableExtensions.<Event>exists(it, _function_2));
    };
    int _size = IterableExtensions.size(IterableExtensions.<Set<Event>>filter(alphabets, _function_1));
    return (_size > 1);
  }
  
  /**
   * Retrieve the set of {@link DiscVariable} variables referenced from a given edge.
   */
  public static Set<DiscVariable> getReferencedDiscVars(final Edge edge) {
    final Function1<Expression, Set<DiscVariable>> _function = (Expression it) -> {
      return ExpressionExtensions.getReferencedDiscVars(it);
    };
    final Set<DiscVariable> referencedVariables = IterableExtensions.<DiscVariable>toSet(IterableExtensions.<Expression, DiscVariable>flatMap(edge.getGuards(), _function));
    final Function1<Update, Set<DiscVariable>> _function_1 = (Update it) -> {
      return ExpressionExtensions.getReferencedDiscVars(it);
    };
    Iterables.<DiscVariable>addAll(referencedVariables, IterableExtensions.<Update, DiscVariable>flatMap(edge.getUpdates(), _function_1));
    return referencedVariables;
  }
}
