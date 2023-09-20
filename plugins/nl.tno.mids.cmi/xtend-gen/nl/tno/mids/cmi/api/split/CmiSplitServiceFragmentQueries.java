package nl.tno.mids.cmi.api.split;

import com.google.common.base.Objects;
import java.util.List;
import java.util.Set;
import nl.tno.mids.cif.extensions.AutomatonExtensions;
import nl.tno.mids.cif.extensions.EdgeExtensions;
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralQueries;
import nl.tno.mids.cmi.api.info.EventFunctionExecutionSide;
import nl.tno.mids.cmi.api.info.EventInfo;
import nl.tno.mids.pps.extensions.info.EventFunctionExecutionType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.metamodel.cif.Component;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.position.metamodel.position.PositionObject;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class CmiSplitServiceFragmentQueries {
  /**
   * Is the given model within the split subset, i.e., only components with split service fragments?
   * 
   * <p>This check is implemented by checking that all CIF automata are service fragment automata, rather than
   * component automata.</p>
   * 
   * @return {@code true} if the model is within the split subset with only components with split service fragments,
   *      {@code false} otherwise.
   * @note Use {@link CmiGeneralQueries#detectSubset} instead, for additional robustness.
   */
  public static boolean isSplitCmiModelWithOnlySplitServiceFragments(final Specification model) {
    final List<Automaton> automata = CollectionLiterals.<Automaton>newArrayList();
    CifCollectUtils.<List<Automaton>>collectAutomata(model, automata);
    final Function1<Automaton, Boolean> _function = (Automaton it) -> {
      EObject _eContainer = it.eContainer();
      return Boolean.valueOf((_eContainer instanceof Specification));
    };
    boolean _exists = IterableExtensions.<Automaton>exists(automata, _function);
    if (_exists) {
      return false;
    }
    final Function1<Component, Boolean> _function_1 = (Component it) -> {
      return Boolean.valueOf((it instanceof Group));
    };
    final Function1<Component, Group> _function_2 = (Component it) -> {
      return ((Group) it);
    };
    Set<Group> components = IterableExtensions.<Group>toSet(IterableExtensions.<Component, Group>map(IterableExtensions.<Component>filter(model.getComponents(), _function_1), _function_2));
    for (final Automaton aut : automata) {
      {
        PositionObject component = aut;
        while ((((component.eContainer() != null) && (component.eContainer() instanceof Group)) && 
          (!(component.eContainer() instanceof Specification)))) {
          EObject _eContainer = component.eContainer();
          component = ((PositionObject) _eContainer);
        }
        if ((!(component instanceof Group))) {
          return false;
        }
        boolean _contains = components.contains(((Group) component));
        boolean _not = (!_contains);
        if (_not) {
          return false;
        }
      }
    }
    final Function1<Automaton, Boolean> _function_3 = (Automaton it) -> {
      return Boolean.valueOf(CmiSplitServiceFragmentQueries.isServiceFragment(it));
    };
    boolean _forall = IterableExtensions.<Automaton>forall(automata, _function_3);
    boolean _not = (!_forall);
    if (_not) {
      return false;
    }
    return true;
  }
  
  /**
   * Get the service fragments of a model or component.
   * 
   * @param modelOrComponent The model or component.
   * @return The service fragments.
   */
  public static List<Automaton> getServiceFragments(final Group modelOrComponent) {
    Assert.check(((modelOrComponent instanceof Specification) || 
      CmiSplitComponentQueries.isComponent(modelOrComponent)));
    final List<Automaton> automata = CollectionLiterals.<Automaton>newArrayList();
    CifCollectUtils.<List<Automaton>>collectAutomata(modelOrComponent, automata);
    final Function1<Automaton, Boolean> _function = (Automaton it) -> {
      return Boolean.valueOf(CmiSplitServiceFragmentQueries.isServiceFragment(it));
    };
    Assert.check(IterableExtensions.<Automaton>forall(automata, _function));
    return automata;
  }
  
  /**
   * Is the given CIF automaton a service fragment?
   * 
   * <p>An automaton is a service fragment if it has a single initial location, with a single outgoing edge, with a
   * single non-tau event, and the automaton name matches the service fragment name derived from the event name.</p>
   * 
   * @param automaton The automaton.
   * @return {@code true} if the automaton is a service fragment, {@code false} otherwise.
   */
  private static boolean isServiceFragment(final Automaton automaton) {
    final Set<Location> initialLocations = AutomatonExtensions.initialLocations(automaton);
    int _size = initialLocations.size();
    boolean _notEquals = (_size != 1);
    if (_notEquals) {
      return false;
    }
    final Location initialLocation = IterableExtensions.<Location>head(initialLocations);
    int _size_1 = initialLocation.getEdges().size();
    boolean _notEquals_1 = (_size_1 != 1);
    if (_notEquals_1) {
      return false;
    }
    final Edge edge = IterableExtensions.<Edge>head(initialLocation.getEdges());
    final List<Event> events = EdgeExtensions.getEventDecls(edge, true);
    int _size_2 = events.size();
    boolean _notEquals_2 = (_size_2 != 1);
    if (_notEquals_2) {
      return false;
    }
    final Event event = IterableExtensions.<Event>head(events);
    if ((event == null)) {
      return false;
    }
    final String eventName = CifTextUtils.getAbsName(event, false);
    boolean _isValidEventName = CmiGeneralEventQueries.isValidEventName(eventName);
    boolean _not = (!_isValidEventName);
    if (_not) {
      return false;
    }
    final String serviceFragmentName = eventName.replace(".", "_");
    String _name = automaton.getName();
    return Objects.equal(_name, serviceFragmentName);
  }
  
  /**
   * Returns the event for the given service fragment.
   * 
   * @param serviceFragment The service fragment.
   * @return The event.
   */
  public static Event getServiceFragmentEvent(final Automaton serviceFragment) {
    boolean _isServiceFragment = CmiSplitServiceFragmentQueries.isServiceFragment(serviceFragment);
    String _absName = CifTextUtils.getAbsName(serviceFragment);
    String _plus = ("Automaton " + _absName);
    String _plus_1 = (_plus + 
      " is not a service fragment and has no service fragment event.");
    Assert.check(_isServiceFragment, _plus_1);
    return EdgeExtensions.getEventDecl(IterableExtensions.<Edge>head(IterableExtensions.<Location>head(AutomatonExtensions.initialLocations(serviceFragment)).getEdges()), true);
  }
  
  /**
   * Does the given service fragment handle an event (un)subscription?
   * 
   * @param serviceFragment The given service fragment.
   * @return {@code true} if the given service fragment handles an event (un)subscription, {@code false} otherwise.
   */
  public static boolean isEventSubscriptionOrUnsubscriptionServiceFragment(final Automaton serviceFragment) {
    final Event event = CmiSplitServiceFragmentQueries.getServiceFragmentEvent(serviceFragment);
    final EventInfo eventInfo = CmiGeneralEventQueries.getEventInfo(event);
    return (Objects.equal(eventInfo.otherSide, EventFunctionExecutionSide.START) && (Objects.equal(eventInfo.otherType, EventFunctionExecutionType.EVENT_SUBSCRIBE_HANDLER) || 
      Objects.equal(eventInfo.otherType, EventFunctionExecutionType.EVENT_UNSUBSCRIBE_HANDLER)));
  }
  
  /**
   * Does the given service fragment handle a client request?
   * 
   * @param serviceFragment The service fragment.
   * @return {@code true} if the service fragment handles a client request, {@code false} otherwise.
   */
  public static boolean isClientRequestServiceFragment(final Automaton serviceFragment) {
    final Event event = CmiSplitServiceFragmentQueries.getServiceFragmentEvent(serviceFragment);
    final EventInfo eventInfo = CmiGeneralEventQueries.getEventInfo(event);
    if ((eventInfo.otherType == null)) {
      return false;
    }
    final EventFunctionExecutionType _switchValue = eventInfo.otherType;
    if (_switchValue != null) {
      switch (_switchValue) {
        case ASYNCHRONOUS_HANDLER:
        case EVENT_SUBSCRIBE_HANDLER:
        case EVENT_UNSUBSCRIBE_HANDLER:
        case SYNCHRONOUS_HANDLER:
        case TRIGGER_HANDLER:
        case HANDLER:
          return true;
        case BLOCKING_CALL:
        case CALL:
        case EVENT_CALLBACK:
        case FCN_CALLBACK:
        case LIBRARY_CALL:
        case WAIT_CALL:
        case UNKNOWN:
          return false;
        case ASYNCHRONOUS_RESULT:
        case EVENT_RAISE:
        case EVENT_SUBSCRIBE_CALL:
        case EVENT_UNSUBSCRIBE_CALL:
        case FCN_CALL:
        case REQUEST_CALL:
        case TRIGGER_CALL:
          throw new RuntimeException(("Not a valid start event of a service fragment: " + eventInfo));
        default:
          throw new RuntimeException(("Unknown event execution type: " + eventInfo.otherType));
      }
    } else {
      throw new RuntimeException(("Unknown event execution type: " + eventInfo.otherType));
    }
  }
  
  /**
   * Does the given service fragment handle a server response?
   * 
   * @param serviceFragment The service fragment.
   * @return {@code true} if the service fragment handles a server response, {@code false} otherwise.
   */
  public static boolean isServerResponseServiceFragment(final Automaton serviceFragment) {
    final Event event = CmiSplitServiceFragmentQueries.getServiceFragmentEvent(serviceFragment);
    final EventInfo eventInfo = CmiGeneralEventQueries.getEventInfo(event);
    if ((eventInfo.otherType == null)) {
      return false;
    }
    final EventFunctionExecutionType _switchValue = eventInfo.otherType;
    if (_switchValue != null) {
      switch (_switchValue) {
        case ASYNCHRONOUS_HANDLER:
        case EVENT_SUBSCRIBE_HANDLER:
        case EVENT_UNSUBSCRIBE_HANDLER:
        case SYNCHRONOUS_HANDLER:
        case TRIGGER_HANDLER:
        case HANDLER:
        case UNKNOWN:
          return false;
        case BLOCKING_CALL:
        case CALL:
        case EVENT_CALLBACK:
        case FCN_CALLBACK:
        case LIBRARY_CALL:
        case WAIT_CALL:
          return true;
        case ASYNCHRONOUS_RESULT:
        case EVENT_RAISE:
        case EVENT_SUBSCRIBE_CALL:
        case EVENT_UNSUBSCRIBE_CALL:
        case FCN_CALL:
        case REQUEST_CALL:
        case TRIGGER_CALL:
          throw new RuntimeException(("Not a valid start event of a service fragment: " + eventInfo));
        default:
          throw new RuntimeException(("Unknown event execution type: " + eventInfo.otherType));
      }
    } else {
      throw new RuntimeException(("Unknown event execution type: " + eventInfo.otherType));
    }
  }
  
  /**
   * Does the given service fragment handle an internal or untraced event?
   * 
   * @param serviceFragment The service fragment.
   * @return {@code true} if the service fragment handles an internal or untraced event, {@code false} otherwise.
   */
  public static boolean isInternalServiceFragment(final Automaton serviceFragment) {
    final Event event = CmiSplitServiceFragmentQueries.getServiceFragmentEvent(serviceFragment);
    final EventInfo eventInfo = CmiGeneralEventQueries.getEventInfo(event);
    boolean _equals = Objects.equal(eventInfo.declType, EventFunctionExecutionType.UNKNOWN);
    if (_equals) {
      return true;
    }
    return (eventInfo.otherType == null);
  }
}
