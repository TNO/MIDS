package nl.tno.mids.pps.extensions.cmi;

import java.util.function.Predicate;
import nl.esi.pps.architecture.implemented.Function;
import nl.esi.pps.tmsc.Dependency;
import nl.esi.pps.tmsc.Event;
import nl.esi.pps.tmsc.Lifeline;
import nl.esi.pps.tmsc.ScopedTMSC;
import nl.esi.pps.tmsc.TMSC;
import nl.tno.mids.pps.extensions.info.EventFunctionExecutionType;
import org.eclipse.escet.common.java.Strings;

/**
 * A CMI preparer for {@link TMSC TMSCs} based on textual syntax that contain information as annotations.
 */
@SuppressWarnings("all")
public class AnnotatedTextPreparer extends CmiPreparer {
  @Override
  public boolean appliesTo(final Dependency dependency) {
    return AnnotatedTextUtils.isAnnotatedTextDependency(dependency);
  }
  
  @Override
  protected ScopedTMSC scope(final TMSC tmsc, final String scopeName) {
    final Predicate<Dependency> _function = (Dependency it) -> {
      return true;
    };
    return this.scopeOnDependencies(tmsc, scopeName, _function);
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
  
  private static String getExecType(final Function container) {
    final String key = "execType";
    final Object value = container.getProperties().get(key);
    return (String) value;
  }
  
  private static void setExecType(final Function container, final String value) {
    final String key = "execType";
    if (value == null) {
    container.getProperties().remove(key);
    } else {
        container.getProperties().put(key, value);
    }
  }
}
