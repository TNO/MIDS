package nl.tno.mids.pps.extensions.cmi;

import nl.esi.pps.architecture.implemented.Function;
import nl.esi.pps.tmsc.Dependency;
import nl.esi.pps.tmsc.Event;
import nl.esi.pps.tmsc.FullScopeTMSC;
import nl.esi.pps.tmsc.Lifeline;
import nl.esi.pps.tmsc.ScopedTMSC;
import nl.esi.pps.tmsc.TMSC;
import nl.esi.pps.tmsc.util.TmscQueries;
import nl.tno.mids.pps.extensions.info.EventFunctionExecutionType;
import org.eclipse.escet.common.java.Strings;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

/**
 * A CMI preparer for {@link TMSC TMSCs} based on textual syntax that contain information as annotations.
 */
@SuppressWarnings("all")
public class AnnotatedTextPreparer extends CmiPreparer {
  @Override
  public boolean appliesTo(final Dependency dependency) {
    return AnnotatedTextPreparer.isAnnotatedTextDependency(dependency);
  }
  
  @Override
  protected ScopedTMSC scope(final FullScopeTMSC tmsc, final String scopeName) {
    ScopedTMSC _createScopedTMSC = TmscQueries.createScopedTMSC(tmsc.getDependencies(), scopeName);
    final Procedure1<ScopedTMSC> _function = (ScopedTMSC it) -> {
      it.setParentScope(tmsc);
    };
    return ObjectExtensions.<ScopedTMSC>operator_doubleArrow(_createScopedTMSC, _function);
  }
  
  @Override
  protected String componentNameFor(final Lifeline lifeline) {
    return lifeline.getExecutor().getName();
  }
  
  @Override
  protected String functionNameFor(final Event event) {
    return event.getFunction().getOperation().getName();
  }
  
  @Override
  protected String interfaceNameFor(final Event event) {
    return event.getFunction().getOperation().getInterface().getName();
  }
  
  @Override
  protected EventFunctionExecutionType executionTypeFor(final Event event) {
    String _execType = AnnotatedTextPreparer.getExecType(event.getFunction());
    if (_execType != null) {
      switch (_execType) {
        case "async":
          return EventFunctionExecutionType.ASYNCHRONOUS_HANDLER;
        case "arslt":
          return EventFunctionExecutionType.ASYNCHRONOUS_RESULT;
        case "blk":
          return EventFunctionExecutionType.BLOCKING_CALL;
        case "call":
          return EventFunctionExecutionType.CALL;
        case "evt":
          return EventFunctionExecutionType.EVENT_RAISE;
        case "evtcb":
          return EventFunctionExecutionType.EVENT_CALLBACK;
        case "evtsub":
          return EventFunctionExecutionType.EVENT_SUBSCRIBE_CALL;
        case "evtsubh":
          return EventFunctionExecutionType.EVENT_SUBSCRIBE_HANDLER;
        case "evtunsub":
          return EventFunctionExecutionType.EVENT_UNSUBSCRIBE_CALL;
        case "evtunsubh":
          return EventFunctionExecutionType.EVENT_UNSUBSCRIBE_HANDLER;
        case "fcn":
          return EventFunctionExecutionType.FCN_CALL;
        case "fcncb":
          return EventFunctionExecutionType.FCN_CALLBACK;
        case "handler":
          return EventFunctionExecutionType.HANDLER;
        case "lib":
          return EventFunctionExecutionType.LIBRARY_CALL;
        case "req":
          return EventFunctionExecutionType.REQUEST_CALL;
        case "sync":
          return EventFunctionExecutionType.SYNCHRONOUS_HANDLER;
        case "trig":
          return EventFunctionExecutionType.TRIGGER_CALL;
        case "trigh":
          return EventFunctionExecutionType.TRIGGER_HANDLER;
        case "unkn":
          return EventFunctionExecutionType.UNKNOWN;
        case "wait":
          return EventFunctionExecutionType.WAIT_CALL;
      }
    }
    String _fmt = Strings.fmt("Unknown annotated function type: %s.", AnnotatedTextPreparer.getExecType(event.getFunction()));
    throw new RuntimeException(_fmt);
  }
  
  /**
   * @param dependency The input dependency.
   * @return {@code true} if {@code dependency} is a annotated text dependency, meaning that its source and target
   *     events are both annotated with respect to {@link #isAnnotatedTextEvent}; {@code false} otherwise.
   */
  private static boolean isAnnotatedTextDependency(final Dependency dependency) {
    return (AnnotatedTextPreparer.isAnnotatedTextEvent(dependency.getSource()) && AnnotatedTextPreparer.isAnnotatedTextEvent(dependency.getTarget()));
  }
  
  /**
   * @param event The input {@link Event}.
   * @return {@code true} if {@code event} is an {@link #isSetExecType(Function) annotated} text
   *     {@link Event event}, {@code false} otherwise.
   */
  private static boolean isAnnotatedTextEvent(final Event event) {
    return AnnotatedTextPreparer.isSetExecType(event.getFunction());
  }
  
  private static String getExecType(final Function container) {
    final String key = "execType";
    final Object value = container.getProperties().get(key);
    return (String) value;
  }
  
  private static void setExecType(final Function container, final String value) {
    final String key = "execType";
    container.getProperties().put(key, value);
  }
  
  /**
   * Returns whether the value of the '{@link nl.tno.mids.pps.extensions.cmi.AnnotatedTextPreparer#getExecType <em>execType</em>}' property is set on {@code container}.
   */
  private static boolean isSetExecType(final Function container) {
    final String key = "execType";
    return container.getProperties().containsKey(key);
  }
  
  /**
   * Unsets the value of the '{@link nl.tno.mids.pps.extensions.cmi.AnnotatedTextPreparer#getExecType <em>execType</em>}' property on {@code container}.
   */
  private static void unsetExecType(final Function container) {
    final String key = "execType";
    container.getProperties().remove(key);
  }
}
