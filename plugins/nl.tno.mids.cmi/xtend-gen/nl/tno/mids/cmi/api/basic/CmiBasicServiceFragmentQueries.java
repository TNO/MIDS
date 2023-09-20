package nl.tno.mids.cmi.api.basic;

import com.google.common.base.Objects;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import nl.tno.mids.cif.extensions.AutomatonExtensions;
import nl.tno.mids.cif.extensions.EdgeExtensions;
import nl.tno.mids.cif.extensions.LocationExtensions;
import nl.tno.mids.cmi.api.general.CmiGeneralComponentQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralQueries;
import nl.tno.mids.cmi.api.general.CmiSubset;
import nl.tno.mids.cmi.api.protocol.CmiProtocolQueries;
import org.eclipse.emf.common.util.EList;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifEdgeUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.Sets;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;

@SuppressWarnings("all")
public class CmiBasicServiceFragmentQueries {
  /**
   * Is the given model within the basic subset, i.e., without split service fragments and not a protocol?
   * 
   * <p>This check is implemented by checking that all CIF automata match components with behavior, rather than
   * being service fragment automata, and do not have protocol names.</p>
   * 
   * @return {@code true} if the model is within the basic subset without split service fragments, {@code false}
   *      otherwise.
   * @note Use {@link CmiGeneralQueries#detectSubset} instead, for additional robustness.
   */
  public static boolean isBasicCmiModelWithNoSplitServiceFragments(final Specification model) {
    final List<Automaton> automata = CollectionLiterals.<Automaton>newArrayList();
    CifCollectUtils.<List<Automaton>>collectAutomata(model, automata);
    final Set<Automaton> automataSet = Sets.<Automaton, Automaton>list2set(automata);
    final Set<Automaton> componentsWithBehavior = Sets.<Automaton, Automaton>list2set(CmiBasicComponentQueries.getComponentsWithBehavior(model));
    if (((!automataSet.equals(componentsWithBehavior)) || CmiProtocolQueries.isProtocolCmiModel(model))) {
      return false;
    }
    return true;
  }
  
  /**
   * Does the given model contain only components that can be split into separate service fragments?
   * 
   * @param model The model.
   * @return {@code true} if the model contains only components that can be split, {@code false} otherwise.
   */
  public static boolean canBeSplitIntoServiceFragments(final Specification model) {
    CmiSubset _detectSubset = CmiGeneralQueries.detectSubset(model);
    boolean _equals = Objects.equal(_detectSubset, CmiSubset.PROTOCOL);
    if (_equals) {
      return false;
    }
    boolean _isEmpty = CmiBasicComponentQueries.getComponentsWithBehavior(model).isEmpty();
    if (_isEmpty) {
      return false;
    }
    boolean _isBasicCmiModelWithNoSplitServiceFragments = CmiBasicServiceFragmentQueries.isBasicCmiModelWithNoSplitServiceFragments(model);
    boolean _not = (!_isBasicCmiModelWithNoSplitServiceFragments);
    if (_not) {
      return false;
    }
    final List<Automaton> componentsWithBehavior = CmiBasicComponentQueries.getComponentsWithBehavior(model);
    final Function1<Automaton, Boolean> _function = (Automaton it) -> {
      return Boolean.valueOf(CmiBasicServiceFragmentQueries.canBeSplitIntoServiceFragments(it));
    };
    return IterableExtensions.<Automaton>forall(componentsWithBehavior, _function);
  }
  
  /**
   * Can the given component be split into separate service fragments?
   * 
   * @param component The component with behavior.
   * @return {@code true} if the automaton can be split, {@code false} otherwise.
   */
  private static boolean canBeSplitIntoServiceFragments(final Automaton component) {
    final List<Event> initialEvents = CmiBasicServiceFragmentQueries.getServiceFragmentInitialEvents(component);
    int _size = initialEvents.size();
    int _size_1 = IterableExtensions.<Event>toSet(initialEvents).size();
    boolean _notEquals = (_size != _size_1);
    if (_notEquals) {
      return false;
    }
    final Set<Edge> allEdges = AutomatonExtensions.getAllEdges(component);
    final Function<Edge, Event> _function = (Edge edge) -> {
      return EdgeExtensions.getEventDecl(edge, true);
    };
    final Predicate<Event> _function_1 = (Event event) -> {
      return (event != null);
    };
    final Set<Event> allEvents = allEdges.stream().<Event>map(_function).filter(_function_1).collect(Collectors.<Event>toSet());
    final Function1<Event, Boolean> _function_2 = (Event it) -> {
      boolean _hasValidEventName = CmiGeneralEventQueries.hasValidEventName(it);
      return Boolean.valueOf((!_hasValidEventName));
    };
    boolean _exists = IterableExtensions.<Event>exists(allEvents, _function_2);
    if (_exists) {
      return false;
    }
    final Location initialLocation = AutomatonExtensions.initialLocation(component);
    boolean _equals = LocationExtensions.getCoReachableLocations(initialLocation).equals(
      LocationExtensions.getReachableLocations(initialLocation));
    boolean _not = (!_equals);
    if (_not) {
      return false;
    }
    return true;
  }
  
  /**
   * Get the initial events of service fragments in the given component.
   */
  public static List<Event> getServiceFragmentInitialEvents(final Automaton component) {
    final Function1<Edge, Event> _function = (Edge it) -> {
      return EdgeExtensions.getEventDecl(it, true);
    };
    return ListExtensions.<Edge, Event>map(CmiBasicServiceFragmentQueries.getServiceFragmentInitialEdges(component), _function);
  }
  
  /**
   * Get the initial edges of service fragments in the given component.
   */
  public static EList<Edge> getServiceFragmentInitialEdges(final Automaton component) {
    return AutomatonExtensions.initialLocation(component).getEdges();
  }
  
  /**
   * Get the edges that comprise a service fragment in the given component, based on the initial event of the service
   * fragment.
   * 
   * @param component The component containing the service fragment.
   * @param serviceFragmentInitialEvent Initial event of the service fragment.
   * @return The set of edges that together comprise the requested service fragment.
   * 
   * @note The results for different service fragments are not guaranteed to be disjoint.
   * @note The component automaton must contain an outgoing edge with the given event in its initial location.
   * @note The initial edge of the service fragment is guaranteed to be the first element of the resulting set.
   */
  public static Set<Edge> getServiceFragmentEdges(final Automaton component, final Event serviceFragmentInitialEvent) {
    final Location initialLocation = AutomatonExtensions.initialLocation(component);
    final Edge initialEdge = LocationExtensions.getEdge(initialLocation, serviceFragmentInitialEvent);
    String _componentName = CmiGeneralComponentQueries.getComponentName(component);
    String _plus = ("Component " + _componentName);
    String _plus_1 = (_plus + 
      " does not contain a service fragment starting with ");
    String _eventName = CmiGeneralEventQueries.getEventName(serviceFragmentInitialEvent);
    String _plus_2 = (_plus_1 + _eventName);
    String _plus_3 = (_plus_2 + ".");
    Assert.notNull(initialEdge, _plus_3);
    return CmiBasicServiceFragmentQueries.getServiceFragmentEdges(component, initialEdge);
  }
  
  /**
   * Get the edges that comprise a service fragment in the given component, based on the initial edge of the service
   * fragment.
   * 
   * @param component The component containing the service fragment.
   * @param serviceFragmentInitialEdge Initial edge of the service fragment.
   * @return The set of edges that together comprise the requested service fragment.
   * 
   * @note The results for different service fragments are not guaranteed to be disjoint.
   * @note The component automaton must contain an outgoing edge with the given event in its initial location.
   * @note The initial edge of the service fragment is guaranteed to be the first element of the resulting set.
   */
  public static Set<Edge> getServiceFragmentEdges(final Automaton component, final Edge initialEdge) {
    final Location initialLocation = AutomatonExtensions.initialLocation(component);
    Location _source = CifEdgeUtils.getSource(initialEdge);
    boolean _equals = Objects.equal(_source, initialLocation);
    String _componentName = CmiGeneralComponentQueries.getComponentName(component);
    String _plus = ("Initial service fragment edge does not belong to component" + _componentName);
    String _plus_1 = (_plus + ".");
    Assert.check(_equals, _plus_1);
    final LinkedHashSet<Edge> fragmentEdges = CollectionLiterals.<Edge>newLinkedHashSet(initialEdge);
    final Deque<Edge> edgeQueue = CollectionLiterals.<Edge>newLinkedList(initialEdge);
    while ((!edgeQueue.isEmpty())) {
      {
        final Edge edge = edgeQueue.pop();
        final Location edgeTarget = CifEdgeUtils.getTarget(edge);
        if ((edgeTarget != initialLocation)) {
          final Function1<Edge, Boolean> _function = (Edge it) -> {
            boolean _contains = fragmentEdges.contains(it);
            return Boolean.valueOf((!_contains));
          };
          final Consumer<Edge> _function_1 = (Edge it) -> {
            fragmentEdges.add(it);
            edgeQueue.add(it);
          };
          IterableExtensions.<Edge>filter(edgeTarget.getEdges(), _function).forEach(_function_1);
        }
      }
    }
    return fragmentEdges;
  }
}
