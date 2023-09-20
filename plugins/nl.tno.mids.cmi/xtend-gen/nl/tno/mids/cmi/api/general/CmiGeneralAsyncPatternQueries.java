package nl.tno.mids.cmi.api.general;

import com.google.common.base.Objects;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import nl.tno.mids.cif.extensions.EdgeExtensions;
import nl.tno.mids.cmi.api.info.ComponentInfo;
import nl.tno.mids.cmi.api.info.EventFunctionExecutionSide;
import nl.tno.mids.cmi.api.info.EventInfo;
import nl.tno.mids.pps.extensions.info.EventFunctionExecutionType;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;

@SuppressWarnings("all")
public class CmiGeneralAsyncPatternQueries {
  /**
   * Is the given edge the start of an asynchronous pattern:
   * 
   * <ul>
   * <li>Asynchronous handler (only if receiving)</li>
   * <li>FCN call (only if sending)</li>
   * <li>Request call of request/wait pattern (only if sending)</li>
   * </ul>
   * 
   * @param edge The possible start edge.
   * @return {@code true} if the event is the start of an asynchronous pattern, {@code false} otherwise.
   */
  public static boolean isAsyncPatternStart(final Edge edge) {
    final Event event = EdgeExtensions.getEventDecl(edge, true);
    if ((event == null)) {
      return false;
    }
    final EventInfo eventInfo = CmiGeneralEventQueries.getEventInfo(event);
    boolean _isReceivingEdge = CmiGeneralAsyncPatternQueries.isReceivingEdge(edge);
    if (_isReceivingEdge) {
      return (Objects.equal(eventInfo.otherType, EventFunctionExecutionType.ASYNCHRONOUS_HANDLER) && 
        Objects.equal(eventInfo.otherSide, EventFunctionExecutionSide.START));
    } else {
      boolean _isSendingEdge = CmiGeneralAsyncPatternQueries.isSendingEdge(edge);
      if (_isSendingEdge) {
        return ((Objects.equal(eventInfo.declType, EventFunctionExecutionType.FCN_CALL) && 
          Objects.equal(eventInfo.declSide, EventFunctionExecutionSide.START)) || (Objects.equal(eventInfo.declType, EventFunctionExecutionType.REQUEST_CALL) && 
          Objects.equal(eventInfo.declSide, EventFunctionExecutionSide.START)));
      } else {
        return false;
      }
    }
  }
  
  /**
   * Is the given edge the end of an asynchronous pattern:
   * 
   * <ul>
   * <li>FCN callback</li>
   * <li>Wait call of request/wait pattern</li>
   * <li>Asynchronous reply for an asynchronous handler</li>
   * </ul>
   * 
   * @param edge The possible end edge.
   * @return {@code true} if the edge is the end of an asynchronous pattern, {@code false} otherwise.
   */
  public static boolean isAsyncPatternEnd(final Edge edge) {
    final Event event = EdgeExtensions.getEventDecl(edge, true);
    if ((event == null)) {
      return false;
    }
    final EventInfo eventInfo = CmiGeneralEventQueries.getEventInfo(event);
    return (((Objects.equal(eventInfo.otherType, EventFunctionExecutionType.FCN_CALLBACK) && 
      Objects.equal(eventInfo.otherSide, EventFunctionExecutionSide.START)) || (Objects.equal(eventInfo.declType, EventFunctionExecutionType.WAIT_CALL) && 
      Objects.equal(eventInfo.declSide, EventFunctionExecutionSide.START))) || (Objects.equal(eventInfo.declType, EventFunctionExecutionType.ASYNCHRONOUS_RESULT) && 
      Objects.equal(eventInfo.declType, EventFunctionExecutionSide.START)));
  }
  
  /**
   * Do the two given edges form an asynchronous pattern?
   * 
   * @param cache Cache of event info for events.
   * @param startEdge The potential start edge of the pattern.
   * @param endEdge The potential end edge of the pattern.
   * @return {@code true} if the two given edges form an asynchronous pattern, {@code false} otherwise.
   */
  public static boolean isAsyncPatternPair(final HashMap<Event, EventInfo> cache, final Edge startEdge, final Edge endEdge) {
    final Event startEvent = EdgeExtensions.getEventDecl(startEdge, true);
    final Event endEvent = EdgeExtensions.getEventDecl(endEdge, true);
    if (((startEvent == null) || (endEvent == null))) {
      return false;
    }
    final Function<Event, EventInfo> _function = (Event event) -> {
      return CmiGeneralEventQueries.getEventInfo(event);
    };
    final EventInfo startInfo = cache.computeIfAbsent(startEvent, _function);
    final Function<Event, EventInfo> _function_1 = (Event event) -> {
      return CmiGeneralEventQueries.getEventInfo(event);
    };
    final EventInfo endInfo = cache.computeIfAbsent(endEvent, _function_1);
    final boolean isReceiving = CmiGeneralAsyncPatternQueries.isReceivingEdge(startEdge);
    final boolean isSending = CmiGeneralAsyncPatternQueries.isSendingEdge(startEdge);
    boolean _notEquals = (!Objects.equal(startInfo.interfaceName, endInfo.interfaceName));
    if (_notEquals) {
      return false;
    }
    boolean _notEquals_1 = (!Objects.equal(startInfo.functionName, endInfo.functionName));
    if (_notEquals_1) {
      return false;
    }
    if ((((((Objects.equal(startInfo.declCompInfo, endInfo.otherCompInfo) && 
      Objects.equal(startInfo.declType, EventFunctionExecutionType.FCN_CALL)) && 
      Objects.equal(startInfo.declSide, EventFunctionExecutionSide.START)) && isSending) && 
      Objects.equal(endInfo.otherType, EventFunctionExecutionType.FCN_CALLBACK)) && 
      Objects.equal(endInfo.otherSide, EventFunctionExecutionSide.START))) {
      return true;
    }
    if ((((((Objects.equal(startInfo.declCompInfo, endInfo.declCompInfo) && 
      Objects.equal(startInfo.declType, EventFunctionExecutionType.REQUEST_CALL)) && 
      Objects.equal(startInfo.declSide, EventFunctionExecutionSide.START)) && isSending) && 
      Objects.equal(endInfo.declType, EventFunctionExecutionType.WAIT_CALL)) && 
      Objects.equal(endInfo.declSide, EventFunctionExecutionSide.START))) {
      return true;
    }
    if ((((((Objects.equal(startInfo.otherCompInfo, endInfo.declCompInfo) && 
      Objects.equal(startInfo.otherType, EventFunctionExecutionType.ASYNCHRONOUS_HANDLER)) && 
      Objects.equal(startInfo.otherSide, EventFunctionExecutionSide.START)) && isReceiving) && 
      Objects.equal(endInfo.declType, EventFunctionExecutionType.ASYNCHRONOUS_RESULT)) && 
      Objects.equal(endInfo.declSide, EventFunctionExecutionSide.START))) {
      return true;
    }
    return false;
  }
  
  /**
   * Collect all other asynchronous pattern start edges that form a valid asynchronous pattern with the given edge.
   * 
   * @param edge The potential asynchronous pattern end edge.
   * @return All other asynchronous pattern start edges, if the given edge is an asynchronous pattern end edge, or
   *      no edges otherwise.
   */
  public static HashSet<Edge> getAsyncPatternStarts(final Edge edge) {
    boolean _isAsyncPatternEnd = CmiGeneralAsyncPatternQueries.isAsyncPatternEnd(edge);
    if (_isAsyncPatternEnd) {
      return CmiGeneralAsyncPatternQueries.getAsyncPatternOtherEdges(edge);
    }
    return CollectionLiterals.<Edge>newHashSet();
  }
  
  /**
   * Collect all other asynchronous pattern end edges that form a valid asynchronous pattern with the given edge.
   * 
   * @param edge The potential asynchronous pattern start edge.
   * @return All other asynchronous pattern end edges, if the given edge is an asynchronous pattern start edge, or
   *      no edges otherwise.
   */
  public static HashSet<Edge> getAsyncPatternEnds(final Edge edge) {
    boolean _isAsyncPatternStart = CmiGeneralAsyncPatternQueries.isAsyncPatternStart(edge);
    if (_isAsyncPatternStart) {
      return CmiGeneralAsyncPatternQueries.getAsyncPatternOtherEdges(edge);
    }
    return CollectionLiterals.<Edge>newHashSet();
  }
  
  /**
   * Collect all other asynchronous pattern edges that form a valid asynchronous pattern with the given edge.
   * 
   * @param edge The potential asynchronous pattern edge.
   * @return All other asynchronous pattern edges with which the given edge forms a pattern, if the given edge is an
   *      asynchronous pattern edge, or no edges otherwise. Note that two edges can only form a pattern if they are
   *      part of the same component.
   */
  private static LinkedHashSet<Edge> getAsyncPatternOtherEdges(final Edge edge) {
    final LinkedHashSet<Edge> result = CollectionLiterals.<Edge>newLinkedHashSet();
    final Event event = EdgeExtensions.getEventDecl(edge, true);
    if ((event == null)) {
      return result;
    }
    final ComplexComponent component = CmiGeneralComponentQueries.getComponent(edge);
    final List<Automaton> automata = CollectionLiterals.<Automaton>newArrayList();
    if ((component instanceof Group)) {
      CifCollectUtils.<List<Automaton>>collectAutomata(component, automata);
    } else {
      automata.add(((Automaton) component));
    }
    final HashMap<Event, EventInfo> cache = CollectionLiterals.<Event, EventInfo>newHashMap();
    final Consumer<Automaton> _function = (Automaton automaton) -> {
      final Consumer<Location> _function_1 = (Location location) -> {
        final Consumer<Edge> _function_2 = (Edge otherEdge) -> {
          final Event otherEvent = EdgeExtensions.getEventDecl(otherEdge, true);
          if ((otherEvent != null)) {
            if ((CmiGeneralAsyncPatternQueries.isAsyncPatternPair(cache, edge, otherEdge) || CmiGeneralAsyncPatternQueries.isAsyncPatternPair(cache, otherEdge, edge))) {
              result.add(otherEdge);
            }
          }
        };
        location.getEdges().forEach(_function_2);
      };
      automaton.getLocations().forEach(_function_1);
    };
    automata.forEach(_function);
    return result;
  }
  
  private static boolean isReceivingEdge(final Edge edge) {
    final ComponentInfo edgeComponentInfo = CmiGeneralComponentQueries.getComponentInfo(
      CmiGeneralComponentQueries.getComponent(edge));
    final Event event = EdgeExtensions.getEventDecl(edge, true);
    if ((event == null)) {
      return false;
    }
    final EventInfo eventInfo = CmiGeneralEventQueries.getEventInfo(event);
    return Objects.equal(edgeComponentInfo, eventInfo.otherCompInfo);
  }
  
  private static boolean isSendingEdge(final Edge edge) {
    final ComponentInfo edgeComponentInfo = CmiGeneralComponentQueries.getComponentInfo(
      CmiGeneralComponentQueries.getComponent(edge));
    final Event event = EdgeExtensions.getEventDecl(edge, true);
    if ((event == null)) {
      return false;
    }
    final EventInfo eventInfo = CmiGeneralEventQueries.getEventInfo(event);
    return (Objects.equal(edgeComponentInfo, eventInfo.declCompInfo) && (eventInfo.otherCompInfo != null));
  }
}
