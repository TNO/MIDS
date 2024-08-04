/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.api.protocol;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.metamodel.cif.ComplexComponent;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;

import com.google.common.base.Preconditions;

import nl.tno.mids.cmi.api.general.CmiGeneralQueries;

/** Queries related to protocol models. */
public class CmiProtocolQueries {
    /** Protocol automaton name prefix. */
    private static final String PROTOCOL_NAME_PREFIX = "Protocol_";

    /**
     * Is the given model in the protocol subset, i.e, does it contain a single automaton with a protocol name?
     * 
     * @param model The model.
     * @return {@code true} if the model contains a protocol, {@code false} otherwise.
     */
    public static boolean isProtocolCmiModel(Specification model) {
        List<Automaton> automatonList = model.getComponents().stream().filter(c -> c instanceof Automaton)
                .map(c -> (Automaton)c).collect(Collectors.toList());

        // Only one protocol automaton per model.
        if (automatonList.size() != 1) {
            return false;
        }

        // Protocol names starts with the word 'Protocol'.
        Automaton protocolAutomaton = automatonList.get(0);
        return isProtocolName(protocolAutomaton.getName());
    }

    /**
     * Does the given CIF component represent a protocol?
     * 
     * @param component The component to check.
     * @return {@code true} if the component represents a protocol, {@code false} otherwise.
     */
    public static boolean isProtocol(ComplexComponent component) {
        return component.eContainer() instanceof Specification && component instanceof Automaton
                && isProtocolName(component.getName());
    }

    /**
     * Does the given CIF component name represent a protocol?
     * 
     * @param componentName The componentName to check.
     * @return {@code true} if the component name represents a protocol, {@code false} otherwise.
     */
    public static boolean isProtocolName(String componentName) {
        return componentName.startsWith(PROTOCOL_NAME_PREFIX);
    }

    /**
     * Get the automaton representing the protocol in the model.
     * 
     * @param model The model.
     * @return The protocol automaton.
     */
    public static Automaton getProtocol(Specification model) {
        List<Automaton> automatonList = model.getComponents().stream().filter(c -> c instanceof Automaton)
                .map(c -> (Automaton)c).collect(Collectors.toList());
        Preconditions.checkArgument(automatonList.size() == 1,
                "Protocol models must contain exactly one protocol automaton.");
        return automatonList.get(0);
    }

    /**
     * Get the name of a protocol.
     * 
     * @param model The model.
     * @return The name of the protocol.
     */
    public static String getProtocolName(Specification model) {
        Automaton protocol = getProtocol(model);
        return getProtocolName(protocol);
    }

    /**
     * Get the name of a protocol.
     * 
     * @param protocol The protocol.
     * @return The name of the protocol.
     */
    public static String getProtocolName(Automaton protocol) {
        Specification model = CmiGeneralQueries.getModel(protocol);
        Preconditions.checkArgument(isProtocolCmiModel(model));
        Preconditions.checkArgument(protocol.eContainer().equals(model)); // Protocols are top-level CIF automata.
        return protocol.getName(); // No parent, so can use non-absolute name.
    }

    /**
     * Create the name of the protocol between two given components.
     * 
     * @param component1 Name of first component participating in protocol.
     * @param component2 Name of second component participating in protocol.
     * @return Name of protocol between first and second component.
     */
    public static String createProtocolName(String component1, String component2) {
        return PROTOCOL_NAME_PREFIX + component1 + "_" + component2;
    }
}
