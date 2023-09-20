package nl.tno.mids.cmi.api.general;

import com.google.common.base.Objects;
import java.util.List;
import nl.tno.mids.cif.extensions.EdgeExtensions;
import nl.tno.mids.cmi.api.info.ComponentInfo;
import nl.tno.mids.cmi.api.info.EventAsyncDirection;
import nl.tno.mids.cmi.api.info.EventFunctionExecutionSide;
import nl.tno.mids.cmi.api.info.EventInfo;
import nl.tno.mids.pps.extensions.info.EventFunctionExecutionType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.Strings;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class CmiGeneralEventQueries {
  /**
   * Get the events of the model.
   * 
   * @return The events.
   */
  public static List<Event> getEvents(final Specification model) {
    final List<Event> events = CollectionLiterals.<Event>newArrayList();
    CifCollectUtils.<List<Event>>collectEvents(model, events);
    return events;
  }
  
  /**
   * Get the name of an event.
   * 
   * @param event The event.
   * @return The name of the event.
   */
  public static String getEventName(final Event event) {
    final ComplexComponent component = CmiGeneralComponentQueries.getComponent(event);
    return CmiGeneralEventQueries.getEventName(event, component);
  }
  
  /**
   * Get the name of an event, assuming a given API subset.
   * 
   * @param event The event.
   * @param subset The API subset of the model.
   * @return The name of the event.
   */
  public static String getEventName(final Event event, final CmiSubset subset) {
    final ComplexComponent component = CmiGeneralComponentQueries.getComponent(event, subset);
    return CmiGeneralEventQueries.getEventName(event, component);
  }
  
  /**
   * Get the name of an event.
   * 
   * @param event The event.
   * @param component The component that declares the event.
   * @return The name of the event.
   */
  private static String getEventName(final Event event, final ComplexComponent component) {
    EObject _eContainer = event.eContainer();
    boolean _equals = Objects.equal(_eContainer, component);
    Assert.check(_equals);
    String _componentName = CmiGeneralComponentQueries.getComponentName(component);
    String _plus = (_componentName + ".");
    String _name = event.getName();
    return (_plus + _name);
  }
  
  /**
   * Returns information about an event, i.e. information about the parts of the event name.
   * 
   * @param event The event.
   * @return The event information.
   */
  public static EventInfo getEventInfo(final Event event) {
    return CmiGeneralEventQueries.getEventInfo(CmiGeneralEventQueries.getEventName(event));
  }
  
  /**
   * Returns information about an event, i.e. information about the parts of the event name, assuming a given API subset.
   * 
   * @param event The event.
   * @param subset The API subset of the model.
   * @return The event information.
   */
  public static EventInfo getEventInfo(final Event event, final CmiSubset subset) {
    return CmiGeneralEventQueries.getEventInfo(CmiGeneralEventQueries.getEventName(event, subset));
  }
  
  /**
   * Returns information about an event, i.e. information about the parts of the event name.
   * 
   * @param eventName The event name, as provided by {@link #getEventName}.
   * @return The event information.
   */
  public static EventInfo getEventInfo(final String eventName) {
    String name = eventName;
    final int periodIdx = name.indexOf(".");
    Assert.check((periodIdx > 0), ("Cannot separate declaring component name in " + name));
    final String declCompName = name.substring(0, periodIdx);
    final ComponentInfo declCompInfo = new ComponentInfo(declCompName);
    name = name.substring((periodIdx + 1));
    final int underscoreIdx = name.indexOf("__");
    Assert.check((underscoreIdx > 0), ("Cannot separate interface and function name in " + name));
    String interfaceName = name.substring(0, underscoreIdx);
    name = name.substring((underscoreIdx + 2));
    EventAsyncDirection asyncDirection = EventAsyncDirection.detectPrefix(interfaceName);
    if ((asyncDirection != null)) {
      interfaceName = interfaceName.substring(asyncDirection.getPrefix().length());
    }
    int postfixStart = name.indexOf("__");
    Assert.check((postfixStart > 0), name);
    while ((Character.valueOf(name.charAt((postfixStart + 2))).compareTo(Character.valueOf('_')) == 0)) {
      postfixStart++;
    }
    final String functionName = name.substring(0, postfixStart);
    name = name.substring((postfixStart + 1));
    ComponentInfo otherCompInfo = null;
    int otherComponentStart = name.indexOf("__");
    if ((otherComponentStart > 0)) {
      final String otherComponentText = name.substring((otherComponentStart + 2));
      name = name.substring(0, otherComponentStart);
      ComponentInfo _componentInfo = new ComponentInfo(otherComponentText);
      otherCompInfo = _componentInfo;
    }
    final EventFunctionExecutionSide side = EventFunctionExecutionSide.detectPostfix(name);
    boolean _equals = Objects.equal(side, EventFunctionExecutionSide.END);
    if (_equals) {
      int _length = side.getPostfix().length();
      int _minus = (-_length);
      name = Strings.slice(name, null, Integer.valueOf(_minus));
    }
    final EventFunctionExecutionType type = EventFunctionExecutionType.detectPostfix(name);
    Assert.notNull(type, (("Event " + eventName) + " has no type"));
    int _length_1 = type.getPostfix().length();
    int _minus_1 = (-_length_1);
    name = Strings.slice(name, null, Integer.valueOf(_minus_1));
    EventFunctionExecutionType declType = null;
    EventFunctionExecutionSide declSide = null;
    EventFunctionExecutionType otherType = null;
    EventFunctionExecutionSide otherSide = null;
    if ((otherCompInfo == null)) {
      declType = type;
      declSide = side;
      otherType = null;
      otherSide = null;
    } else {
      declSide = EventFunctionExecutionSide.detectPostfix(name);
      boolean _equals_1 = Objects.equal(declSide, EventFunctionExecutionSide.END);
      if (_equals_1) {
        int _length_2 = declSide.getPostfix().length();
        int _minus_2 = (-_length_2);
        name = Strings.slice(name, null, Integer.valueOf(_minus_2));
      }
      declType = EventFunctionExecutionType.detectPostfix(name);
      Assert.notNull(declType, (("Event " + eventName) + " has no type"));
      int _length_3 = declType.getPostfix().length();
      int _minus_3 = (-_length_3);
      name = Strings.slice(name, null, Integer.valueOf(_minus_3));
      otherType = type;
      otherSide = side;
    }
    Assert.check(name.isEmpty(), ((("Event " + eventName) + " contains unknown elements:") + name));
    final EventInfo info = new EventInfo(declCompInfo, asyncDirection, interfaceName, functionName, declType, declSide, otherType, otherSide, otherCompInfo);
    boolean _equals_2 = info.toString().equals(eventName);
    String _string = info.toString();
    String _plus = (_string + " != ");
    String _plus_1 = (_plus + eventName);
    Assert.check(_equals_2, _plus_1);
    return info;
  }
  
  /**
   * Is the given name a valid event name?
   * 
   * @param name The given name.
   * @return {@code true} if the name fits the API event naming scheme, {@code false} otherwise.
   */
  public static boolean isValidEventName(final String name) {
    try {
      CmiGeneralEventQueries.getEventInfo(name);
      return true;
    } catch (final Throwable _t) {
      if (_t instanceof AssertionError) {
        return false;
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
  }
  
  /**
   * Does the given event have a valid name?
   * 
   * @param event The given event.
   * @return {@code true} if the name of the given event fits the API event naming scheme, {@code false} otherwise.
   */
  public static boolean hasValidEventName(final Event event) {
    return CmiGeneralEventQueries.isValidEventName(CmiGeneralEventQueries.getEventName(event));
  }
  
  /**
   * Does the given event represent a request?
   * 
   * @param eventInfo The given event.
   * @return {@code true} if the given event represents a request, {@code false} otherwise.
   */
  public static boolean isRequestEvent(final EventInfo eventInfo) {
    if ((eventInfo.otherCompInfo == null)) {
      return false;
    }
    final EventFunctionExecutionType _switchValue = eventInfo.declType;
    if (_switchValue != null) {
      switch (_switchValue) {
        case BLOCKING_CALL:
        case CALL:
        case EVENT_RAISE:
        case EVENT_SUBSCRIBE_CALL:
        case EVENT_UNSUBSCRIBE_CALL:
        case FCN_CALL:
        case LIBRARY_CALL:
        case REQUEST_CALL:
        case TRIGGER_CALL:
        case WAIT_CALL:
          return true;
        case ASYNCHRONOUS_RESULT:
        case HANDLER:
        case SYNCHRONOUS_HANDLER:
        case UNKNOWN:
          return false;
        case ASYNCHRONOUS_HANDLER:
        case EVENT_CALLBACK:
        case EVENT_SUBSCRIBE_HANDLER:
        case EVENT_UNSUBSCRIBE_HANDLER:
        case FCN_CALLBACK:
        case TRIGGER_HANDLER:
          throw new RuntimeException(("Not a valid \'declType\': " + eventInfo));
        default:
          throw new RuntimeException(("Unknown event execution type: " + eventInfo));
      }
    } else {
      throw new RuntimeException(("Unknown event execution type: " + eventInfo));
    }
  }
  
  /**
   * Does the given event represent a response?
   * 
   * @param eventInfo The given event.
   * @return {@code true} if the given event represents a response, {@code false} otherwise.
   */
  public static boolean isResponseEvent(final EventInfo eventInfo) {
    if ((eventInfo.otherCompInfo == null)) {
      return false;
    }
    final EventFunctionExecutionType _switchValue = eventInfo.declType;
    if (_switchValue != null) {
      switch (_switchValue) {
        case BLOCKING_CALL:
        case CALL:
        case EVENT_RAISE:
        case EVENT_SUBSCRIBE_CALL:
        case EVENT_UNSUBSCRIBE_CALL:
        case FCN_CALL:
        case LIBRARY_CALL:
        case REQUEST_CALL:
        case TRIGGER_CALL:
        case UNKNOWN:
        case WAIT_CALL:
          return false;
        case ASYNCHRONOUS_RESULT:
        case HANDLER:
        case SYNCHRONOUS_HANDLER:
          return true;
        case ASYNCHRONOUS_HANDLER:
        case EVENT_CALLBACK:
        case EVENT_SUBSCRIBE_HANDLER:
        case EVENT_UNSUBSCRIBE_HANDLER:
        case FCN_CALLBACK:
        case TRIGGER_HANDLER:
          throw new RuntimeException(("Not a valid \'declType\': " + eventInfo));
        default:
          throw new RuntimeException(("Unknown event execution type: " + eventInfo));
      }
    } else {
      throw new RuntimeException(("Unknown event execution type: " + eventInfo));
    }
  }
  
  /**
   * Is given event part of the communication between two given components?
   * 
   * @param event Event to check.
   * @param componentName1 Name of first communicating component.
   * @param componentName2 Name of second communicating component.
   * @return {@code true} if the event represents communication between the two components, {@code false} otherwise.
   */
  public static boolean isCommunicationBetween(final Event event, final String componentName1, final String componentName2) {
    final EventInfo eventInfo = CmiGeneralEventQueries.getEventInfo(event);
    if ((eventInfo.otherCompInfo == null)) {
      return false;
    }
    return ((eventInfo.declCompInfo.name.equals(componentName1) && 
      eventInfo.otherCompInfo.name.equals(componentName2)) || (eventInfo.declCompInfo.name.equals(componentName2) && eventInfo.otherCompInfo.name.equals(componentName1)));
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
      final List<Automaton> automata = CollectionLiterals.<Automaton>newArrayList();
      CifCollectUtils.<List<Automaton>>collectAutomata(model, automata);
      final Function1<Automaton, Boolean> _function = (Automaton it) -> {
        return Boolean.valueOf(CmiGeneralEventQueries.hasTau(it));
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
        Event _eventDecl = EdgeExtensions.getEventDecl(edge, true);
        return Boolean.valueOf((_eventDecl == null));
      };
      return Boolean.valueOf(IterableExtensions.<Edge>exists(it.getEdges(), _function_1));
    };
    return IterableExtensions.<Location>exists(automaton.getLocations(), _function);
  }
}
