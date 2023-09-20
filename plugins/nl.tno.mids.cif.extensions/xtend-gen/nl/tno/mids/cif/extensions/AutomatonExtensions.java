package nl.tno.mids.cif.extensions;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifEdgeUtils;
import org.eclipse.escet.cif.common.CifEventUtils;
import org.eclipse.escet.cif.common.CifLocationUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.common.java.Lists;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure2;

@SuppressWarnings("all")
public class AutomatonExtensions {
  private AutomatonExtensions() {
  }
  
  /**
   * Returns the single initial location of this automaton. Throws an exception if multiple exist. Only considers
   * locations to be trivially initial (i.e. not dependent on some condition which is not trivially true).
   */
  public static Location initialLocation(final Automaton aut) {
    final Set<Location> set = AutomatonExtensions.initialLocations(aut);
    int _size = set.size();
    boolean _equals = (_size == 1);
    if (_equals) {
      return ((Location[])Conversions.unwrapArray(set, Location.class))[0];
    } else {
      boolean _isEmpty = set.isEmpty();
      if (_isEmpty) {
        throw new RuntimeException("Requested single initial location, while non exists.");
      } else {
        final Function1<Location, CharSequence> _function = (Location it) -> {
          return CifLocationUtils.getName(it);
        };
        String _join = IterableExtensions.<Location>join(set, "", ", ", ".", _function);
        String _plus = ("Requested single initial location, while multiple exist. Namely " + _join);
        String _plus_1 = (_plus + " Check whether automaton ");
        String _name = aut.getName();
        String _plus_2 = (_plus_1 + _name);
        String _plus_3 = (_plus_2 + " is deterministic");
        throw new RuntimeException(_plus_3);
      }
    }
  }
  
  /**
   * Returns the set of initial locations of this automaton. Only considers locations to be trivially initial
   * (i.e. not dependent on some condition which is not trivially true).
   */
  public static Set<Location> initialLocations(final Automaton aut) {
    final Function1<Location, Boolean> _function = (Location it) -> {
      return Boolean.valueOf(LocationExtensions.isInitialLocation(it));
    };
    return IterableExtensions.<Location>toSet(IterableExtensions.<Location>filter(aut.getLocations(), _function));
  }
  
  /**
   * Change the names of locations in the automaton to loc1, loc2, loc3, ..., in the order they are contained
   * in the automaton.
   */
  public static void renumberLocations(final Automaton aut) {
    final Procedure2<Location, Integer> _function = (Location loc, Integer index) -> {
      loc.setName(("loc" + Integer.valueOf(((index).intValue() + 1))));
    };
    IterableExtensions.<Location>forEach(aut.getLocations(), _function);
  }
  
  /**
   * Ensures that the single initial location in {@code automaton} is also the first location occurring in
   * {@code automaton.getLocations()}. Also applies location renumbering to get consistent location names.
   * 
   * @param automaton The input automaton.
   */
  public static void ensureInitialLocationIsFirstLocation(final Automaton automaton) {
    final Location initialState = AutomatonExtensions.initialLocation(automaton);
    Location _head = IterableExtensions.<Location>head(automaton.getLocations());
    boolean _tripleNotEquals = (_head != initialState);
    if (_tripleNotEquals) {
      automaton.getLocations().remove(initialState);
      automaton.getLocations().add(0, initialState);
    }
    AutomatonExtensions.renumberLocations(automaton);
  }
  
  /**
   * Re-orders the locations in the {@code automaton}, starting from each initial location in depth-first order.
   * Additionally, for each location, outgoing edges are sorted based on event name. Any locations unreachable from
   * any initial location are removed. If there are no initial locations, all locations are replaced by a single
   * non-initial, unmarked location with no outgoing edges. Also applies location renumbering to get consistent location
   * names.
   * 
   * <p>
   * If the automaton has a single initial location and no locations with multiple edges with the same event, the
   * result will be fully normalized. If any location contains multiple edges with the same event, those will have the
   * same order in the output as in the input. If there are multiple initial locations, they are initially sorted
   * based on their name, and then renamed afterwards.
   * </p>
   * 
   * @param automaton The input automaton.
   */
  public static void normalizeLocations(final Automaton automaton) {
    final Set<Location> initialLocations = AutomatonExtensions.initialLocations(automaton);
    boolean _isEmpty = initialLocations.isEmpty();
    boolean _not = (!_isEmpty);
    if (_not) {
      final LinkedHashSet<Location> visitedLocations = new LinkedHashSet<Location>();
      final HashSet<Location> stackContents = CollectionLiterals.<Location>newHashSet();
      automaton.getLocations().clear();
      final LinkedList<Location> stack = CollectionLiterals.<Location>newLinkedList();
      final Function1<Location, String> _function = (Location it) -> {
        return it.getName();
      };
      stack.addAll(IterableExtensions.<Location, String>sortBy(initialLocations, _function));
      stackContents.addAll(stack);
      while ((!stack.isEmpty())) {
        {
          final Location nextLoc = stack.pop();
          stackContents.remove(nextLoc);
          visitedLocations.add(nextLoc);
          final Comparator<Edge> _function_1 = (Edge l, Edge r) -> {
            return AutomatonExtensions.compareEdges(l, r);
          };
          ECollections.<Edge>sort(nextLoc.getEdges(), _function_1);
          List<Edge> _reverseView = ListExtensions.<Edge>reverseView(nextLoc.getEdges());
          for (final Edge edge : _reverseView) {
            {
              final Location newLoc = CifEdgeUtils.getTarget(edge);
              if (((!visitedLocations.contains(newLoc)) && (!stackContents.contains(newLoc)))) {
                stack.push(newLoc);
                stackContents.add(newLoc);
              }
            }
          }
        }
      }
      automaton.getLocations().addAll(visitedLocations);
    } else {
      automaton.getLocations().clear();
      automaton.getLocations().add(CifConstructors.newLocation());
    }
    AutomatonExtensions.renumberLocations(automaton);
  }
  
  /**
   * Compares two edges based on the absolute non-escaped names of their associated events. Edges with tau events
   * (explicitly or implicitly) are considered 'smaller' than ones with non-tau events.
   * 
   * <p>This method does not support multiple events on a single edge.</p>
   * 
   * @param left Left edge to compare.
   * @param right Right edge to compare.
   * @return A negative integer if {@code left} precedes {@code right}, zero if they are equal, and a positive integer
   *         if {@code right} precedes {@code left}.
   */
  private static int compareEdges(final Edge left, final Edge right) {
    final Event eventLeft = EdgeExtensions.getEventDecl(left, true);
    final Event eventRight = EdgeExtensions.getEventDecl(right, true);
    if (((eventLeft == null) && (eventRight == null))) {
      return 0;
    }
    if ((eventLeft == null)) {
      return (-1);
    }
    if ((eventRight == null)) {
      return 1;
    }
    return CifTextUtils.getAbsName(eventLeft, false).compareTo(CifTextUtils.getAbsName(eventRight, false));
  }
  
  /**
   * Retrieve all edges of a given automaton.
   */
  public static Set<Edge> getAllEdges(final Automaton automaton) {
    final Function1<Location, EList<Edge>> _function = (Location loc) -> {
      return loc.getEdges();
    };
    return IterableExtensions.<Edge>toSet(IterableExtensions.<Location, Edge>flatMap(automaton.getLocations(), _function));
  }
  
  /**
   * Generates a new alphabet and stores this in the automaton. Does not consider channel communication!
   * Note that this procedure generates a new alphabet, with new event expressions.
   */
  public static void updateAlphabet(final Automaton automaton) {
    AutomatonExtensions.removeAlphabet(automaton);
    final Set<Event> eventAlphabet = CifEventUtils.getAlphabet(automaton);
    automaton.setAlphabet(CifConstructors.newAlphabet());
    final Consumer<Event> _function = (Event evt) -> {
      EList<Expression> _events = automaton.getAlphabet().getEvents();
      EventExpression _newEventExpression = CifConstructors.newEventExpression();
      final Procedure1<EventExpression> _function_1 = (EventExpression it) -> {
        it.setEvent(evt);
        it.setType(CifConstructors.newBoolType());
      };
      EventExpression _doubleArrow = ObjectExtensions.<EventExpression>operator_doubleArrow(_newEventExpression, _function_1);
      _events.add(_doubleArrow);
    };
    eventAlphabet.forEach(_function);
  }
  
  /**
   * Removes the alphabet from the automaton.
   */
  private static void removeAlphabet(final Automaton automaton) {
    automaton.setAlphabet(null);
  }
  
  /**
   * Remove unreachable locations, i.e. locations not reachable from an initial location.
   * 
   * <p>Does not take guards into account when computing reachability, which may result in over-approximation of reachable
   * locations. Thus, the result may contain locations that according to the full semantics are unreachable.</p>
   */
  public static void removeUnreachableLocations(final Automaton automaton) {
    final HashSet<Location> reachableLocations = CollectionLiterals.<Location>newHashSet();
    Set<Location> _initialLocations = AutomatonExtensions.initialLocations(automaton);
    for (final Location initialLocation : _initialLocations) {
      reachableLocations.addAll(LocationExtensions.getReachableLocations(initialLocation));
    }
    final Predicate<Location> _function = (Location it) -> {
      boolean _contains = reachableLocations.contains(it);
      return (!_contains);
    };
    automaton.getLocations().removeIf(_function);
  }
  
  /**
   * Construct a multi-map linking locations to incoming edges.
   * 
   * <p>The resulting multi-map does not explicitly include locations with no incoming edges. For those locations,
   * the {@link SetMultimap#get} method will return an empty set rather than {@code null}.</p>
   */
  public static SetMultimap<Location, Edge> getIncomingEdgeMap(final Automaton automaton) {
    final SetMultimap<Location, Edge> resultMap = MultimapBuilder.linkedHashKeys().linkedHashSetValues().<Location, Edge>build();
    final Consumer<Edge> _function = (Edge edge) -> {
      resultMap.put(EdgeExtensions.getDestination(edge), edge);
    };
    AutomatonExtensions.getAllEdges(automaton).forEach(_function);
    return resultMap;
  }
  
  /**
   * Does the given model contain edges with data references?
   * 
   * @param model The model.
   * @return {@code true} if the model contains at least one automaton with at least one edge containing an update or
   *      a guard referencing data, {@code false} otherwise.
   */
  public static boolean hasData(final Specification model) {
    boolean _xblockexpression = false;
    {
      final List<Automaton> automata = Lists.<Automaton>list();
      CifCollectUtils.<List<Automaton>>collectAutomata(model, automata);
      final Function1<Automaton, Boolean> _function = (Automaton it) -> {
        return Boolean.valueOf(AutomatonExtensions.hasData(it));
      };
      _xblockexpression = IterableExtensions.<Automaton>exists(automata, _function);
    }
    return _xblockexpression;
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
        return Boolean.valueOf(AutomatonExtensions.hasData(it_1));
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
   * Retrieve the set of {@link DiscVariable} variables referenced from any edge in the given automaton.
   */
  public static Set<DiscVariable> getReferencedDiscVars(final Automaton automaton) {
    final Function1<Location, Iterable<DiscVariable>> _function = (Location it) -> {
      final Function1<Edge, Set<DiscVariable>> _function_1 = (Edge it_1) -> {
        return EdgeExtensions.getReferencedDiscVars(it_1);
      };
      return IterableExtensions.<Edge, DiscVariable>flatMap(it.getEdges(), _function_1);
    };
    return IterableExtensions.<DiscVariable>toSet(IterableExtensions.<Location, DiscVariable>flatMap(automaton.getLocations(), _function));
  }
  
  /**
   * Does the given model contain 'tau' edges?
   * 
   * @param model The model.
   * @return {@code true} if the model contains at least one automaton with at least one 'tau' edge, {@code false}
   *      otherwise.
   */
  public static boolean hasTau(final Specification model) {
    boolean _xblockexpression = false;
    {
      final List<Automaton> automata = Lists.<Automaton>list();
      CifCollectUtils.<List<Automaton>>collectAutomata(model, automata);
      final Function1<Automaton, Boolean> _function = (Automaton it) -> {
        return Boolean.valueOf(AutomatonExtensions.hasTau(it));
      };
      _xblockexpression = IterableExtensions.<Automaton>exists(automata, _function);
    }
    return _xblockexpression;
  }
  
  /**
   * Does the given automaton contain 'tau' edges?
   * 
   * @param automaton The CIF automaton.
   * @return {@code true} if the automaton contains at least one tau edge, {@code false} otherwise.
   */
  private static boolean hasTau(final Automaton automaton) {
    final Function1<Location, Boolean> _function = (Location it) -> {
      final Function1<Edge, Boolean> _function_1 = (Edge edge) -> {
        return Boolean.valueOf(EdgeExtensions.getEventDecls(edge, true).contains(null));
      };
      return Boolean.valueOf(IterableExtensions.<Edge>exists(it.getEdges(), _function_1));
    };
    return IterableExtensions.<Location>exists(automaton.getLocations(), _function);
  }
}
