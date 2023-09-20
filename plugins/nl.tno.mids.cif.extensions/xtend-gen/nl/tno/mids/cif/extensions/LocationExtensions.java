package nl.tno.mids.cif.extensions;

import com.google.common.collect.SetMultimap;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.emf.common.util.EList;
import org.eclipse.escet.cif.common.CifEdgeUtils;
import org.eclipse.escet.cif.common.CifLocationUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Exceptions;

@SuppressWarnings("all")
public class LocationExtensions {
  private LocationExtensions() {
  }
  
  /**
   * Assuming a deterministic automaton, returns the outgoing edge from location {@code state}
   * with event {@code event}. Result is {@code null} if no such edge exists. For non-deterministic automata,
   * use {@code getEdges} instead.
   */
  public static Edge getEdge(final Location state, final Event event) {
    try {
      List<Edge> _edges = null;
      if (state!=null) {
        _edges=CifLocationUtils.getEdges(state, event);
      }
      final List<Edge> edges = _edges;
      if (((edges != null) && (edges.size() > 1))) {
        throw new NonDeterministicChoiceException(state, event);
      } else {
        if (((edges != null) && edges.isEmpty())) {
          return null;
        }
      }
      Edge _get = null;
      if (edges!=null) {
        _get=edges.get(0);
      }
      return _get;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  /**
   * Determines whether location is a possible initial location:
   * @param location The location to check for.
   * @return Boolean indicating whether location is possibly an initial location
   */
  public static boolean isInitialLocation(final Location location) {
    return ((location.getInitials().size() > 0) && CifValueUtils.isTriviallyTrue(location.getInitials(), true, true));
  }
  
  /**
   * Get the set of locations reachable from a given location.
   * 
   * <p>Does not take guards into account when computing reachability, which may result in over-approximation of reachable
   * locations. Thus, the result may contain locations that according to the full semantics are unreachable.</p>
   */
  public static Set<Location> getReachableLocations(final Location location) {
    final LinkedHashSet<Location> visitedLocations = CollectionLiterals.<Location>newLinkedHashSet(location);
    final Deque<Location> locationQueue = CollectionLiterals.<Location>newLinkedList(location);
    while ((!locationQueue.isEmpty())) {
      {
        final Location currentLoc = locationQueue.pop();
        EList<Edge> _edges = currentLoc.getEdges();
        for (final Edge edge : _edges) {
          {
            final Location target = CifEdgeUtils.getTarget(edge);
            boolean _contains = visitedLocations.contains(target);
            boolean _not = (!_contains);
            if (_not) {
              visitedLocations.add(target);
              locationQueue.add(target);
            }
          }
        }
      }
    }
    return visitedLocations;
  }
  
  /**
   * Get the set of locations co-reachable from a given location, i.e the locations from which the given location is reachable.
   * 
   * <p>Does not take guards into account when computing reachability, which may result in over-approximation of reachable
   * locations. Thus, the result may contain locations where according to the full semantics the initial state is unreachable.</p>
   */
  public static Set<Location> getCoReachableLocations(final Location location) {
    final LinkedHashSet<Location> visitedLocations = CollectionLiterals.<Location>newLinkedHashSet(location);
    final Deque<Location> locationQueue = CollectionLiterals.<Location>newLinkedList(location);
    final SetMultimap<Location, Edge> incomingEdges = AutomatonExtensions.getIncomingEdgeMap(CifLocationUtils.getAutomaton(location));
    while ((!locationQueue.isEmpty())) {
      {
        final Location currentLoc = locationQueue.pop();
        Set<Edge> _get = incomingEdges.get(currentLoc);
        for (final Edge edge : _get) {
          {
            final Location source = CifEdgeUtils.getSource(edge);
            boolean _contains = visitedLocations.contains(source);
            boolean _not = (!_contains);
            if (_not) {
              visitedLocations.add(source);
              locationQueue.add(source);
            }
          }
        }
      }
    }
    return visitedLocations;
  }
}
