package nl.tno.mids.cmi.utils;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import nl.esi.pps.architecture.instantiated.Executor;
import nl.esi.pps.tmsc.Dependency;
import nl.esi.pps.tmsc.Event;
import nl.esi.pps.tmsc.ExitEvent;
import nl.esi.pps.tmsc.TMSC;
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries;
import nl.tno.mids.cmi.api.info.ComponentInfo;
import nl.tno.mids.cmi.api.info.EventAsyncDirection;
import nl.tno.mids.cmi.api.info.EventFunctionExecutionSide;
import nl.tno.mids.pps.extensions.info.EventFunctionExecutionType;
import nl.tno.mids.pps.extensions.queries.TmscDependencyQueries;
import nl.tno.mids.pps.extensions.queries.TmscEventQueries;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.Strings;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.StringExtensions;

/**
 * Utilities for obtaining CMI-compliant names from various {@link TMSC} artifacts.
 */
@SuppressWarnings("all")
public class CifNamesUtil {
  private CifNamesUtil() {
  }
  
  /**
   * @param executor The {@link Executor executor} whose CIF component name is requested.
   * @return The CIF component name of the given {@code executor}.
   */
  public static String asCifName(final Executor executor) {
    boolean _isNullOrEmpty = StringExtensions.isNullOrEmpty(CifNamesUtil.getComponentName(executor));
    boolean _not = (!_isNullOrEmpty);
    Preconditions.checkArgument(_not, "Expected a non-empty component name.");
    final String name = CifNamesUtil.asCifName(CifNamesUtil.getComponentName(executor));
    new ComponentInfo(name);
    return name;
  }
  
  /**
   * Gives the CIF event name for the specified {@code event} in the scope of {@code scopeTmsc}.
   * 
   * @param event The {@link TMSC} {@link Event event}, which should be in scope of {@code scopeTmsc}.
   * @param scopeTmsc The contextual {@link TMSC}.
   * @param synchronous Whether components are synchronized, requiring the same event name for both sides of a TMSC
   *                    dependency, or are not synchronized (e.g. in case of synchronization with middleware that is
   *                    added later).
   * @return The CIF event name.
   */
  public static String asCifName(final Event event, final TMSC scopeTmsc, final boolean synchronous) {
    Preconditions.checkArgument(TmscEventQueries.isInScope(scopeTmsc, event), 
      "Expected the given event to be within scope.");
    boolean _isNullOrEmpty = StringExtensions.isNullOrEmpty(CifNamesUtil.getFunctionName(event));
    boolean _not = (!_isNullOrEmpty);
    Preconditions.checkArgument(_not, "Expected a non-empty function name.");
    boolean _isNullOrEmpty_1 = StringExtensions.isNullOrEmpty(CifNamesUtil.getInterfaceName(event));
    boolean _not_1 = (!_isNullOrEmpty_1);
    Preconditions.checkArgument(_not_1, "Expected a non-empty interface name.");
    Preconditions.<EventFunctionExecutionType>checkNotNull(CifNamesUtil.getExecutionType(event), "Expected a non-null function execution type.");
    StringBuilder name = new StringBuilder(100);
    Dependency dependency = null;
    Event otherEvent = null;
    if (((!TmscDependencyQueries.hasIncomingMessageDependencies(event, scopeTmsc)) && 
      (!TmscDependencyQueries.hasOutgoingMessageDependencies(event, scopeTmsc)))) {
      dependency = null;
      otherEvent = null;
    } else {
      if (((!TmscDependencyQueries.hasIncomingMessageDependencies(event, scopeTmsc)) && 
        TmscDependencyQueries.hasOutgoingMessageDependencies(event, scopeTmsc))) {
        dependency = TmscDependencyQueries.getOutgoingMessageDependency(event, scopeTmsc);
        otherEvent = dependency.getTarget();
      } else {
        if ((TmscDependencyQueries.hasIncomingMessageDependencies(event, scopeTmsc) && 
          (!TmscDependencyQueries.hasOutgoingMessageDependencies(event, scopeTmsc)))) {
          dependency = TmscDependencyQueries.getIncomingMessageDependency(event, scopeTmsc);
          otherEvent = dependency.getSource();
        } else {
          String _fmt = Strings.fmt("Event %s has multiple message dependencies.", event);
          throw new RuntimeException(_fmt);
        }
      }
    }
    if ((otherEvent != null)) {
      boolean _isNullOrEmpty_2 = StringExtensions.isNullOrEmpty(CifNamesUtil.getFunctionName(otherEvent));
      boolean _not_2 = (!_isNullOrEmpty_2);
      Preconditions.checkArgument(_not_2, "Expected a non-empty function name.");
      boolean _isNullOrEmpty_3 = StringExtensions.isNullOrEmpty(CifNamesUtil.getInterfaceName(otherEvent));
      boolean _not_3 = (!_isNullOrEmpty_3);
      Preconditions.checkArgument(_not_3, "Expected a non-empty interface name.");
      Preconditions.<EventFunctionExecutionType>checkNotNull(CifNamesUtil.getExecutionType(otherEvent), "Expected a non-null function execution type.");
      String _interfaceName = CifNamesUtil.getInterfaceName(event);
      String _interfaceName_1 = CifNamesUtil.getInterfaceName(otherEvent);
      boolean _equals = Objects.equal(_interfaceName, _interfaceName_1);
      String _interfaceName_2 = CifNamesUtil.getInterfaceName(event);
      String _plus = (_interfaceName_2 + " != ");
      String _interfaceName_3 = CifNamesUtil.getInterfaceName(otherEvent);
      String _plus_1 = (_plus + _interfaceName_3);
      Preconditions.checkArgument(_equals, _plus_1);
      String _functionName = CifNamesUtil.getFunctionName(event);
      String _functionName_1 = CifNamesUtil.getFunctionName(otherEvent);
      boolean _equals_1 = Objects.equal(_functionName, _functionName_1);
      String _interfaceName_4 = CifNamesUtil.getInterfaceName(event);
      String _plus_2 = (_interfaceName_4 + ".");
      String _functionName_2 = CifNamesUtil.getFunctionName(event);
      String _plus_3 = (_plus_2 + _functionName_2);
      String _plus_4 = (_plus_3 + " != ");
      String _interfaceName_5 = CifNamesUtil.getInterfaceName(otherEvent);
      String _plus_5 = (_plus_4 + _interfaceName_5);
      String _plus_6 = (_plus_5 + ".");
      String _functionName_3 = CifNamesUtil.getFunctionName(otherEvent);
      String _plus_7 = (_plus_6 + _functionName_3);
      Preconditions.checkArgument(_equals_1, _plus_7);
      Preconditions.<Dependency>checkNotNull(dependency);
      Event _source = dependency.getSource();
      boolean _tripleEquals = (_source == event);
      Event _source_1 = dependency.getSource();
      boolean _tripleEquals_1 = (_source_1 == otherEvent);
      boolean _notEquals = (_tripleEquals != _tripleEquals_1);
      Preconditions.checkArgument(_notEquals);
    }
    if (((dependency == null) || (!synchronous))) {
      name.append(CifNamesUtil.asCifName(event.getLifeline().getExecutor()));
    } else {
      name.append(CifNamesUtil.asCifName(dependency.getSource().getLifeline().getExecutor()));
    }
    name.append(".");
    if ((!synchronous)) {
      String _xifexpression = null;
      if ((dependency == null)) {
        _xifexpression = EventAsyncDirection.INTERNAL.getPrefix();
      } else {
        String _xifexpression_1 = null;
        Event _source_2 = dependency.getSource();
        boolean _tripleEquals_2 = (_source_2 == event);
        if (_tripleEquals_2) {
          _xifexpression_1 = EventAsyncDirection.SEND.getPrefix();
        } else {
          _xifexpression_1 = EventAsyncDirection.RECEIVE.getPrefix();
        }
        _xifexpression = _xifexpression_1;
      }
      name.append(_xifexpression);
    }
    boolean _contains = CifNamesUtil.getInterfaceName(event).contains("__");
    boolean _not_4 = (!_contains);
    Preconditions.checkArgument(_not_4);
    boolean _contains_1 = CifNamesUtil.getFunctionName(event).contains("__");
    boolean _not_5 = (!_contains_1);
    Preconditions.checkArgument(_not_5);
    name.append(CifNamesUtil.getInterfaceName(event));
    name.append("__");
    name.append(CifNamesUtil.getFunctionName(event));
    name.append("_");
    if ((otherEvent == null)) {
      CifNamesUtil.addEventPostFix(name, event);
    } else {
      Event _source_3 = dependency.getSource();
      boolean _tripleEquals_3 = (_source_3 == event);
      if (_tripleEquals_3) {
        CifNamesUtil.addEventPostFix(name, event);
        CifNamesUtil.addEventPostFix(name, otherEvent);
      } else {
        CifNamesUtil.addEventPostFix(name, otherEvent);
        CifNamesUtil.addEventPostFix(name, event);
      }
    }
    if ((dependency == null)) {
    } else {
      if (synchronous) {
        name.append("__");
        name.append(CifNamesUtil.asCifName(dependency.getTarget().getLifeline().getExecutor()));
      } else {
        Event _xifexpression_2 = null;
        Event _source_4 = dependency.getSource();
        boolean _tripleEquals_4 = (_source_4 == event);
        if (_tripleEquals_4) {
          _xifexpression_2 = dependency.getTarget();
        } else {
          _xifexpression_2 = dependency.getSource();
        }
        final Event otherSideEvent = _xifexpression_2;
        name.append("__");
        name.append(CifNamesUtil.asCifName(otherSideEvent.getLifeline().getExecutor()));
      }
    }
    final String cifName = CifNamesUtil.asCifAbsoluteName(name.toString());
    CmiGeneralEventQueries.getEventInfo(cifName);
    return cifName;
  }
  
  /**
   * @param name The name to add postfix to.
   * @param event The event defining the postfix to add.
   */
  protected static StringBuilder addEventPostFix(final StringBuilder name, final Event event) {
    StringBuilder _xblockexpression = null;
    {
      name.append(CifNamesUtil.getExecutionType(event).getPostfix());
      StringBuilder _xifexpression = null;
      if ((event instanceof ExitEvent)) {
        _xifexpression = name.append(EventFunctionExecutionSide.END.getPostfix());
      }
      _xblockexpression = _xifexpression;
    }
    return _xblockexpression;
  }
  
  /**
   * @param name The name to format.
   * @return The given absolute {@code name} formatted as a valid CIF identifier.
   */
  private static String asCifAbsoluteName(final String name) {
    final Function<String, String> _function = (String it) -> {
      return CifNamesUtil.asCifName(it);
    };
    return ((List<String>)Conversions.doWrapArray(name.split("\\."))).stream().<String>map(_function).collect(Collectors.joining("."));
  }
  
  /**
   * @param name The name to format.
   * @return The given {@code name} formatted as a valid CIF identifier.
   */
  private static String asCifName(final String name) {
    boolean _isEmpty = name.isEmpty();
    boolean _not = (!_isEmpty);
    Assert.check(_not);
    String cifName = name.replaceAll("[^a-zA-Z0-9_]", "_");
    boolean _matches = cifName.matches("^[0-9]");
    if (_matches) {
      cifName = ("_" + cifName);
    }
    return cifName;
  }
  
  private static String getFunctionName(final Event container) {
    final String key = "functionName";
    final Object value = container.getProperties().get(key);
    return (String) value;
  }
  
  private static void setFunctionName(final Event container, final String value) {
    final String key = "functionName";
    if (value == null) {
    container.getProperties().remove(key);
    } else {
        container.getProperties().put(key, value);
    }
  }
  
  private static String getInterfaceName(final Event container) {
    final String key = "interfaceName";
    final Object value = container.getProperties().get(key);
    return (String) value;
  }
  
  private static void setInterfaceName(final Event container, final String value) {
    final String key = "interfaceName";
    if (value == null) {
    container.getProperties().remove(key);
    } else {
        container.getProperties().put(key, value);
    }
  }
  
  private static EventFunctionExecutionType getExecutionType(final Event container) {
    final String key = "executionType";
    final Object value = container.getProperties().get(key);
    return (EventFunctionExecutionType) value;
  }
  
  private static void setExecutionType(final Event container, final EventFunctionExecutionType value) {
    final String key = "executionType";
    if (value == null) {
    container.getProperties().remove(key);
    } else {
        container.getProperties().put(key, value);
    }
  }
  
  private static String getComponentName(final Executor container) {
    final String key = "componentName";
    final Object value = container.getProperties().get(key);
    return (String) value;
  }
  
  private static void setComponentName(final Executor container, final String value) {
    final String key = "componentName";
    if (value == null) {
    container.getProperties().remove(key);
    } else {
        container.getProperties().put(key, value);
    }
  }
}
