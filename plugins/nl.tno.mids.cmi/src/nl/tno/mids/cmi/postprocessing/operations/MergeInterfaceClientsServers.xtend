/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.postprocessing.operations

import java.nio.file.Path
import java.util.Map
import java.util.Set
import net.automatalib.util.automata.fsa.NFAs
import nl.tno.mids.automatalib.extensions.cif.AutomataLibToCif
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil
import nl.tno.mids.cif.extensions.AutomatonExtensions
import nl.tno.mids.cmi.api.basic.CmiBasicComponentQueries
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries
import nl.tno.mids.cmi.api.info.ComponentInfo
import nl.tno.mids.cmi.api.info.EventFunctionExecutionSide
import nl.tno.mids.cmi.postprocessing.PostProcessingModel
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCifSpec
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset
import nl.tno.mids.pps.extensions.info.EventFunctionExecutionType
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.xtend.lib.annotations.Accessors

/** Merge multiple clients and/or servers of interfaces into a single instance, considering them a single runtime component. */
@Accessors
class MergeInterfaceClientsServers extends PostProcessingOperation<MergeInterfaceClientsServersOptions> {
    override getPreconditionSubset() { return new PostProcessingPreconditionSubset(false, false) }

    override getResultSubset() { return new PostProcessingResultSubset(false, false) }

    override applyOperation(Map<String, PostProcessingModel> models, Set<String> selectedComponents,
        Path relativeResolvePath, IProgressMonitor monitor) {

        selectedComponents.forEach [ component |
            val baseComponentInfo = new ComponentInfo(component)
            val model = models.get(component)

            preconditionSubset.ensureSubset(model)

            // Get automaton representation of model.
            val nfa = AutomataLibUtil.dfaToNfa(model.compactDfa)

            // Apply renaming to all events in automaton.
            val renamedNfa = AutomataLibUtil.rename(nfa, [ event |
                normalizeInterfaceClientsServersInEvent(event, baseComponentInfo)
            ])

            // After renaming, automaton may no longer be deterministic, so make it deterministic again.
            val renamedDfa = NFAs.determinize(renamedNfa, renamedNfa.inputAlphabet, true, false)

            // Minimize DFA separately, for better performance.
            val minimizedDfa = AutomataLibUtil.minimizeDFA(renamedDfa)

            val renamedCif = AutomataLibToCif.fsaToCifSpecification(minimizedDfa, component, true)

            // Ensure the initial location is the first location.
            val automaton = CmiBasicComponentQueries.getSingleComponentWithBehavior(renamedCif)
            AutomatonExtensions.ensureInitialLocationIsFirstLocation(automaton)

            models.put(component, new PostProcessingModelCifSpec(renamedCif, component, getResultStatus(model.status)))
        ]
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
    private def normalizeInterfaceClientsServersInEvent(String eventName, ComponentInfo baseComponentInfo) {
        var eventInfo = CmiGeneralEventQueries.getEventInfo(eventName)

        // Apply filter if enabled.
        if (!eventInfo.interfaceName.equals(options.mergeInterface) && !options.mergeInterface.empty) {
            return eventName
        }

        // Create abstract component representing the interface
        val interfaceCompInfo = new ComponentInfo(eventInfo.interfaceName, null, false)

        // If the event comes from another component, merge it if it is a request from a client.
        if (!eventInfo.declCompInfo.equals(baseComponentInfo) && options.mergeClients &&
            CmiGeneralEventQueries.isRequestEvent(eventInfo)) {
            // Apply merging of components, and also function call types if necessary.
            switch (eventInfo.declType) {
                case BLOCKING_CALL,
                case FCN_CALL,
                case LIBRARY_CALL,
                case REQUEST_CALL:
                    eventInfo = eventInfo.withDeclCompInfo(interfaceCompInfo, EventFunctionExecutionType.CALL,
                        EventFunctionExecutionSide.START)
                default:
                    eventInfo = eventInfo.withDeclCompInfo(interfaceCompInfo)
            }
        }

        // If the event is send to another component, merge it if it is a reply to a client.
        if (eventInfo.otherCompInfo !== null) {
            if (!eventInfo.otherCompInfo.equals(baseComponentInfo) && options.mergeClients &&
                CmiGeneralEventQueries.isResponseEvent(eventInfo)) {
                // Apply merging of components, and also function call types if necessary.
                switch (eventInfo.otherType) {
                    case BLOCKING_CALL,
                    case FCN_CALLBACK,
                    case LIBRARY_CALL,
                    case WAIT_CALL:
                        eventInfo = eventInfo.withOtherCompInfo(EventFunctionExecutionType.CALL,
                            EventFunctionExecutionSide.END, interfaceCompInfo)
                    default:
                        eventInfo = eventInfo.withOtherCompInfo(interfaceCompInfo)
                }
            }
        }

        // If the event is send to another component, merge it if it is a request to a server.
        if (eventInfo.otherCompInfo !== null) {
            if (!eventInfo.otherCompInfo.equals(baseComponentInfo) && options.mergeServers &&
                CmiGeneralEventQueries.isRequestEvent(eventInfo)) {
                // Apply merging of components, and also function call types if necessary.
                switch (eventInfo.otherType) {
                    case ASYNCHRONOUS_HANDLER,
                    case SYNCHRONOUS_HANDLER:
                        eventInfo = eventInfo.withOtherCompInfo(EventFunctionExecutionType.HANDLER,
                            EventFunctionExecutionSide.START, interfaceCompInfo)
                    default:
                        eventInfo = eventInfo.withOtherCompInfo(interfaceCompInfo)
                }
            }
        }

        // If the event comes from another component, merge it if it is a reply from a server.
        if (!eventInfo.declCompInfo.equals(baseComponentInfo) && options.mergeServers &&
            CmiGeneralEventQueries.isResponseEvent(eventInfo)) {
            // Apply merging of components, and also function call types if necessary.
            switch (eventInfo.declType) {
                case ASYNCHRONOUS_RESULT,
                case SYNCHRONOUS_HANDLER:
                    eventInfo = eventInfo.withDeclCompInfo(interfaceCompInfo, EventFunctionExecutionType.HANDLER,
                        EventFunctionExecutionSide.END)
                default:
                    eventInfo = eventInfo.withDeclCompInfo(interfaceCompInfo)
            }
        }

        return eventInfo.toString
    }

}
