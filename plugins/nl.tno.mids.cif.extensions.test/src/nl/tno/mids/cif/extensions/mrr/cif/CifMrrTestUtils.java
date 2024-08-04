/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cif.extensions.mrr.cif;

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newAutomaton;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEdge;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEdgeEvent;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEvent;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEventExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newLocation;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newSpecification;
import static org.eclipse.escet.common.java.Maps.mapc;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;
import org.eclipse.escet.cif.prettyprinter.CifPrettyPrinter;
import org.eclipse.escet.common.box.CodeBox;
import org.eclipse.escet.common.box.MemoryCodeBox;

import com.google.common.base.Preconditions;

public class CifMrrTestUtils {
    public static Specification wordToCif(String word) {
        List<String> alphabetList = word.chars().mapToObj(i -> String.valueOf((char)i)).collect(Collectors.toList());
        Set<String> alphabet = new LinkedHashSet<>(alphabetList);

        Specification spec = newSpecification();
        Automaton aut = newAutomaton();
        spec.getComponents().add(aut);
        aut.setName("aut");

        Map<String, Event> eventMap = mapc(alphabet.size());
        for (String letter: alphabet) {
            Event event = newEvent();
            event.setName(letter);
            spec.getDeclarations().add(event);
            eventMap.put(letter, event);
        }

        Location loc = newLocation();
        aut.getLocations().add(loc);
        loc.setName("loc1");
        loc.getInitials().add(CifValueUtils.makeTrue());

        for (int i = 0; i < word.length(); i++) {
            Location loc2 = newLocation();
            aut.getLocations().add(loc2);
            loc2.setName("loc" + Integer.toString(i + 2));

            Edge edge = newEdge();
            loc.getEdges().add(edge);
            edge.setTarget(loc2);

            char letter = word.charAt(i);
            EventExpression eventRef = newEventExpression();
            Event event = eventMap.get(String.valueOf(letter));
            Preconditions.checkNotNull(event);
            eventRef.setEvent(event);

            EdgeEvent edgeEvent = newEdgeEvent();
            edge.getEvents().add(edgeEvent);
            edgeEvent.setEvent(eventRef);

            loc = loc2;
        }

        return spec;
    }

    public static String specToStr(Specification spec) {
        CodeBox code = new MemoryCodeBox();
        CifPrettyPrinter.boxSpec(spec, code);
        return code.toString();
    }
}
