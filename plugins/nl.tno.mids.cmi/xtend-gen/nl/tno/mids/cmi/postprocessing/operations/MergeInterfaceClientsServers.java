package nl.tno.mids.cmi.postprocessing.operations;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.util.automata.fsa.NFAs;
import nl.tno.mids.automatalib.extensions.cif.AutomataLibToCif;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.cif.extensions.AutomatonExtensions;
import nl.tno.mids.cmi.api.basic.CmiBasicComponentQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries;
import nl.tno.mids.cmi.api.info.ComponentInfo;
import nl.tno.mids.cmi.api.info.EventFunctionExecutionSide;
import nl.tno.mids.cmi.api.info.EventInfo;
import nl.tno.mids.cmi.postprocessing.PostProcessingModel;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCifSpec;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus;
import nl.tno.mids.pps.extensions.info.EventFunctionExecutionType;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.xtend.lib.annotations.Accessors;

/**
 * Merge multiple clients and/or servers of interfaces into a single instance, considering them a single runtime component.
 */
@Accessors
@SuppressWarnings("all")
public class MergeInterfaceClientsServers extends PostProcessingOperation<MergeInterfaceClientsServersOptions> {
  @Override
  public PostProcessingPreconditionSubset getPreconditionSubset() {
    return new PostProcessingPreconditionSubset(Boolean.valueOf(false), Boolean.valueOf(false));
  }
  
  @Override
  public PostProcessingResultSubset getResultSubset() {
    return new PostProcessingResultSubset(Boolean.valueOf(false), Boolean.valueOf(false));
  }
  
  @Override
  public void applyOperation(final Map<String, PostProcessingModel> models, final Set<String> selectedComponents, final Path relativeResolvePath, final IProgressMonitor monitor) {
    final Consumer<String> _function = (String component) -> {
      final ComponentInfo baseComponentInfo = new ComponentInfo(component);
      final PostProcessingModel model = models.get(component);
      this.getPreconditionSubset().ensureSubset(model);
      final CompactNFA<String> nfa = AutomataLibUtil.<String>dfaToNfa(model.getCompactDfa());
      final Function<String, String> _function_1 = (String event) -> {
        return this.normalizeInterfaceClientsServersInEvent(event, baseComponentInfo);
      };
      final CompactNFA<String> renamedNfa = AutomataLibUtil.<String, String>rename(nfa, _function_1);
      final CompactDFA<String> renamedDfa = NFAs.<String>determinize(renamedNfa, renamedNfa.getInputAlphabet(), true, false);
      final CompactDFA<String> minimizedDfa = AutomataLibUtil.<String>minimizeDFA(renamedDfa);
      final Specification renamedCif = AutomataLibToCif.<Integer, Integer, Void, CompactDFA<String>>fsaToCifSpecification(minimizedDfa, component, true);
      final Automaton automaton = CmiBasicComponentQueries.getSingleComponentWithBehavior(renamedCif);
      AutomatonExtensions.ensureInitialLocationIsFirstLocation(automaton);
      PostProcessingStatus _resultStatus = this.getResultStatus(model.status);
      PostProcessingModelCifSpec _postProcessingModelCifSpec = new PostProcessingModelCifSpec(renamedCif, component, _resultStatus);
      models.put(component, _postProcessingModelCifSpec);
    };
    selectedComponents.forEach(_function);
  }
  
  /**
   * Normalize events to removes differences in other components.
   * 
   * <p>There are four different patterns in which a client can invoke functionality:
   *   <ul>
   *     <li>Blocking call start, followed by a blocking call return.</li>
   *     <li>FCN call start, followed by an FCN callback start.</li>
   *     <li>Library call start, followed by a library call return.</li>
   *     <li>Request call start, followed by a wait call return.</li>
   *   </ul>
   *   If we are filtering client differences, these four patterns will be merged into one pattern.
   *   <ul>
   *     <li>Abstract call start, followed by an abstract call return.
   *   </ul>
   * </p>
   * 
   * <p>There are two different patterns in which a server can implement functionality:
   *   <ul>
   *     <li>Asynchronous handler start, followed by an asynchronous result call start.</li>
   *     <li>Synchronous handler start, followed by a synchronous handler return.</li>
   *   </ul>
   *   If we are filtering server differences, these two patterns will be merged into one pattern.
   *   <ul>
   *     <li>Abstract handler start, followed by abstract handler return.</li>
   *   </ul>
   * </p>
   * 
   * @param eventName Name of event to normalize.
   * @param baseComponentInfo {@link ComponentInfo} that represents the component with behavior.
   * @return normalized event name.
   */
  private String normalizeInterfaceClientsServersInEvent(final String eventName, final ComponentInfo baseComponentInfo) {
    EventInfo eventInfo = CmiGeneralEventQueries.getEventInfo(eventName);
    if (((!eventInfo.interfaceName.equals(this.options.mergeInterface)) && (!this.options.mergeInterface.isEmpty()))) {
      return eventName;
    }
    final ComponentInfo interfaceCompInfo = new ComponentInfo(eventInfo.interfaceName, null, false);
    if ((((!eventInfo.declCompInfo.equals(baseComponentInfo)) && this.options.mergeClients) && 
      CmiGeneralEventQueries.isRequestEvent(eventInfo))) {
      final EventFunctionExecutionType _switchValue = eventInfo.declType;
      if (_switchValue != null) {
        switch (_switchValue) {
          case BLOCKING_CALL:
          case FCN_CALL:
          case LIBRARY_CALL:
          case REQUEST_CALL:
            eventInfo = eventInfo.withDeclCompInfo(interfaceCompInfo, EventFunctionExecutionType.CALL, 
              EventFunctionExecutionSide.START);
            break;
          default:
            eventInfo = eventInfo.withDeclCompInfo(interfaceCompInfo);
            break;
        }
      } else {
        eventInfo = eventInfo.withDeclCompInfo(interfaceCompInfo);
      }
    }
    if ((eventInfo.otherCompInfo != null)) {
      if ((((!eventInfo.otherCompInfo.equals(baseComponentInfo)) && this.options.mergeClients) && 
        CmiGeneralEventQueries.isResponseEvent(eventInfo))) {
        final EventFunctionExecutionType _switchValue_1 = eventInfo.otherType;
        if (_switchValue_1 != null) {
          switch (_switchValue_1) {
            case BLOCKING_CALL:
            case FCN_CALLBACK:
            case LIBRARY_CALL:
            case WAIT_CALL:
              eventInfo = eventInfo.withOtherCompInfo(EventFunctionExecutionType.CALL, 
                EventFunctionExecutionSide.END, interfaceCompInfo);
              break;
            default:
              eventInfo = eventInfo.withOtherCompInfo(interfaceCompInfo);
              break;
          }
        } else {
          eventInfo = eventInfo.withOtherCompInfo(interfaceCompInfo);
        }
      }
    }
    if ((eventInfo.otherCompInfo != null)) {
      if ((((!eventInfo.otherCompInfo.equals(baseComponentInfo)) && this.options.mergeServers) && 
        CmiGeneralEventQueries.isRequestEvent(eventInfo))) {
        final EventFunctionExecutionType _switchValue_2 = eventInfo.otherType;
        if (_switchValue_2 != null) {
          switch (_switchValue_2) {
            case ASYNCHRONOUS_HANDLER:
            case SYNCHRONOUS_HANDLER:
              eventInfo = eventInfo.withOtherCompInfo(EventFunctionExecutionType.HANDLER, 
                EventFunctionExecutionSide.START, interfaceCompInfo);
              break;
            default:
              eventInfo = eventInfo.withOtherCompInfo(interfaceCompInfo);
              break;
          }
        } else {
          eventInfo = eventInfo.withOtherCompInfo(interfaceCompInfo);
        }
      }
    }
    if ((((!eventInfo.declCompInfo.equals(baseComponentInfo)) && this.options.mergeServers) && 
      CmiGeneralEventQueries.isResponseEvent(eventInfo))) {
      final EventFunctionExecutionType _switchValue_3 = eventInfo.declType;
      if (_switchValue_3 != null) {
        switch (_switchValue_3) {
          case ASYNCHRONOUS_RESULT:
          case SYNCHRONOUS_HANDLER:
            eventInfo = eventInfo.withDeclCompInfo(interfaceCompInfo, EventFunctionExecutionType.HANDLER, 
              EventFunctionExecutionSide.END);
            break;
          default:
            eventInfo = eventInfo.withDeclCompInfo(interfaceCompInfo);
            break;
        }
      } else {
        eventInfo = eventInfo.withDeclCompInfo(interfaceCompInfo);
      }
    }
    return eventInfo.toString();
  }
  
  public MergeInterfaceClientsServers(final MergeInterfaceClientsServersOptions options) {
    super(options);
  }
}
