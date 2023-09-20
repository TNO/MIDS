/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.automatalib.extensions.cif;

import static org.eclipse.escet.common.java.Lists.list;
import static org.eclipse.escet.common.java.Maps.map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.io.CifReader;
import org.eclipse.escet.cif.merger.CifMerger;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.cif.prettyprinter.CifPrettyPrinter;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.escet.common.app.framework.options.Options;
import org.eclipse.escet.common.app.framework.output.OutputMode;
import org.eclipse.escet.common.app.framework.output.OutputModeOption;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;

class AutomataLibToAndFromCifTest {
    @BeforeEach
    public void beforeTest() {
        AppEnv.registerSimple();
        Options.set(OutputModeOption.class, OutputMode.ERROR);
    }

    @AfterEach
    public void afterTest() {
        AppEnv.unregisterApplication();
    }

    @Test
    public void testRoundTripSimple() {
        // Create CIF specification.
        StringBuilder specTxt = new StringBuilder();
        specTxt.append("event e1;\n");
        specTxt.append("event e2;\n");
        specTxt.append("automaton aut:\n");
        specTxt.append("  location loc1:\n");
        specTxt.append("    initial;\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge e1 goto loc2;\n");
        specTxt.append("  location loc2:\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge e2 goto loc3;\n");
        specTxt.append("  location loc3:\n");
        specTxt.append("    marked false;\n");
        specTxt.append("    edge e1 goto loc4;\n");
        specTxt.append("    edge e2 goto loc3;\n");
        specTxt.append("  location loc4:\n");
        specTxt.append("    marked false;\n");
        specTxt.append("    edge e1 goto loc1;\n");
        specTxt.append("    edge e2 goto loc2;\n");
        specTxt.append("end");
        CifReader reader = new CifReader().init("dummy", "dummy", false);
        Specification spec = reader.read(specTxt.toString());

        // Convert to AutomataLib.
        Automaton cifAut = spec.getComponents().stream().filter(c -> c instanceof Automaton).map(c -> (Automaton)c)
                .collect(Collectors.toList()).get(0);
        CompactDFA<String> dfa1 = CifToAutomataLib.cifAutomatonToCompactDfa(cifAut, false);
        CompactNFA<String> nfa1 = CifToAutomataLib.cifAutomatonToCompactNfa(cifAut, false);
        FastDFA<String> dfa2 = CifToAutomataLib.cifAutomatonToFastDfa(cifAut, false);
        FastNFA<String> nfa2 = CifToAutomataLib.cifAutomatonToFastNfa(cifAut, false);

        // Convert back to CIF.
        Specification specDfa1 = AutomataLibToCif.fsaToCifSpecification(dfa1, "aut", true);
        Specification specNfa1 = AutomataLibToCif.fsaToCifSpecification(nfa1, "aut", true);
        Specification specDfa2 = AutomataLibToCif.fsaToCifSpecification(dfa2, "aut", true);
        Specification specNfa2 = AutomataLibToCif.fsaToCifSpecification(nfa2, "aut", true);

        // Convert CIF specifications to text.
        String textDfa1 = CifPrettyPrinter.boxSpec(specDfa1).toString();
        String textNfa1 = CifPrettyPrinter.boxSpec(specNfa1).toString();
        String textDfa2 = CifPrettyPrinter.boxSpec(specDfa2).toString();
        String textNfa2 = CifPrettyPrinter.boxSpec(specNfa2).toString();

        // Compare round trip.
        assertEquals(specTxt.toString(), textDfa1);
        assertEquals(specTxt.toString(), textNfa1);
        assertEquals(specTxt.toString(), textDfa2);
        assertEquals(specTxt.toString(), textNfa2);
    }

    @Test
    public void testRoundTripNfa() {
        // Create CIF specification.
        StringBuilder specTxt = new StringBuilder();
        specTxt.append("event e1;\n");
        specTxt.append("automaton aut:\n");
        specTxt.append("  location loc1:\n");
        specTxt.append("    initial;\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge e1 goto loc1;\n");
        specTxt.append("    edge e1 goto loc2;\n");
        specTxt.append("  location loc2:\n");
        specTxt.append("    marked false;\n");
        specTxt.append("end");
        CifReader reader = new CifReader().init("dummy", "dummy", false);
        Specification spec = reader.read(specTxt.toString());

        // Convert to AutomataLib.
        Automaton cifAut = spec.getComponents().stream().filter(c -> c instanceof Automaton).map(c -> (Automaton)c)
                .collect(Collectors.toList()).get(0);
        CompactNFA<String> nfa1 = CifToAutomataLib.cifAutomatonToCompactNfa(cifAut, false);
        FastNFA<String> nfa2 = CifToAutomataLib.cifAutomatonToFastNfa(cifAut, false);

        // Convert back to CIF.
        Specification specNfa1 = AutomataLibToCif.fsaToCifSpecification(nfa1, "aut", true);
        Specification specNfa2 = AutomataLibToCif.fsaToCifSpecification(nfa2, "aut", true);

        // Convert CIF specifications to text.
        String textNfa1 = CifPrettyPrinter.boxSpec(specNfa1).toString();
        String textNfa2 = CifPrettyPrinter.boxSpec(specNfa2).toString();

        // Compare round trip.
        assertEquals(specTxt.toString(), textNfa1);
        assertEquals(specTxt.toString(), textNfa2);
    }

    @Test
    public void testNfaCifModelConvertToAutomataLibDfa() {
        // Create CIF specification, an NFA.
        Specification spec = CifConstructors.newSpecification();
        spec.setName("specification");

        Automaton aut = CifConstructors.newAutomaton();
        aut.setName("aut");
        spec.getComponents().add(aut);

        Event evt = CifConstructors.newEvent(false, "evt", null, null);
        aut.getDeclarations().add(evt);

        Location loc = CifConstructors.newLocation();
        aut.getLocations().add(loc);

        EdgeEvent edgeEvent1 = CifConstructors
                .newEdgeEvent(CifConstructors.newEventExpression(evt, null, CifConstructors.newBoolType()), null);
        EdgeEvent edgeEvent2 = CifConstructors
                .newEdgeEvent(CifConstructors.newEventExpression(evt, null, CifConstructors.newBoolType()), null);
        Edge edge1 = CifConstructors.newEdge(list(edgeEvent1), null, null, null, null, false);
        Edge edge2 = CifConstructors.newEdge(list(edgeEvent2), null, null, null, null, false);
        loc.getEdges().add(edge1);
        loc.getEdges().add(edge2);

        // Convert CIF NFA to AutomataLib DFA.
        assertThrows(IllegalStateException.class, () -> { CifToAutomataLib.cifAutomatonToFastDfa(aut, true); });
    }

    @Test
    public void testMultiInitialStates() {
        // Create CIF specification.
        StringBuilder specTxt = new StringBuilder();
        specTxt.append("event e1;\n");
        specTxt.append("automaton aut:\n");
        specTxt.append("  location loc1:\n");
        specTxt.append("    initial;\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge e1 goto loc2;\n");
        specTxt.append("  location loc2:\n");
        specTxt.append("    initial;\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge e1 goto loc3;\n");
        specTxt.append("  location loc3:\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge e1 goto loc1;\n");
        specTxt.append("end");
        CifReader reader = new CifReader().init("dummy", "dummy", false);
        Specification spec = reader.read(specTxt.toString());

        // Convert to AutomataLib.
        Automaton cifAut = spec.getComponents().stream().filter(c -> c instanceof Automaton).map(c -> (Automaton)c)
                .collect(Collectors.toList()).get(0);
        assertThrows(IllegalStateException.class, () -> { CifToAutomataLib.cifAutomatonToCompactDfa(cifAut, false); });
        assertThrows(IllegalStateException.class, () -> { CifToAutomataLib.cifAutomatonToFastDfa(cifAut, false); });
        CompactNFA<String> nfa1 = CifToAutomataLib.cifAutomatonToCompactNfa(cifAut, false);
        FastNFA<String> nfa2 = CifToAutomataLib.cifAutomatonToFastNfa(cifAut, false);

        // Convert back to CIF.
        Specification specNfa1 = AutomataLibToCif.fsaToCifSpecification(nfa1, "aut", true);
        Specification specNfa2 = AutomataLibToCif.fsaToCifSpecification(nfa2, "aut", true);

        // Convert CIF specifications to text.
        String textNfa1 = CifPrettyPrinter.boxSpec(specNfa1).toString();
        String textNfa2 = CifPrettyPrinter.boxSpec(specNfa2).toString();

        // Compare round trip.
        assertEquals(specTxt.toString(), textNfa1);
        assertEquals(specTxt.toString(), textNfa2);
    }

    @Test
    public void testNoInitialStates() {
        // Create CIF specification.
        StringBuilder specTxt = new StringBuilder();
        specTxt.append("event e1;\n");
        specTxt.append("automaton aut:\n");
        specTxt.append("  location loc1:\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge e1 goto loc2;\n");
        specTxt.append("  location loc2:\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge e1 goto loc1;\n");
        specTxt.append("end");
        CifReader reader = new CifReader().init("dummy", "dummy", false);
        Specification spec = reader.read(specTxt.toString());

        // Convert to AutomataLib.
        Automaton cifAut = spec.getComponents().stream().filter(c -> c instanceof Automaton).map(c -> (Automaton)c)
                .collect(Collectors.toList()).get(0);
        CompactDFA<String> dfa1 = CifToAutomataLib.cifAutomatonToCompactDfa(cifAut, false);
        CompactNFA<String> nfa1 = CifToAutomataLib.cifAutomatonToCompactNfa(cifAut, false);
        FastDFA<String> dfa2 = CifToAutomataLib.cifAutomatonToFastDfa(cifAut, false);
        FastNFA<String> nfa2 = CifToAutomataLib.cifAutomatonToFastNfa(cifAut, false);

        // Convert back to CIF.
        Specification specDfa1 = AutomataLibToCif.fsaToCifSpecification(dfa1, "aut", true);
        Specification specNfa1 = AutomataLibToCif.fsaToCifSpecification(nfa1, "aut", true);
        Specification specDfa2 = AutomataLibToCif.fsaToCifSpecification(dfa2, "aut", true);
        Specification specNfa2 = AutomataLibToCif.fsaToCifSpecification(nfa2, "aut", true);

        // Convert CIF specifications to text.
        String textDfa1 = CifPrettyPrinter.boxSpec(specDfa1).toString();
        String textNfa1 = CifPrettyPrinter.boxSpec(specNfa1).toString();
        String textDfa2 = CifPrettyPrinter.boxSpec(specDfa2).toString();
        String textNfa2 = CifPrettyPrinter.boxSpec(specNfa2).toString();

        // Compare round trip.
        assertEquals(specTxt.toString(), textDfa1);
        assertEquals(specTxt.toString(), textNfa1);
        assertEquals(specTxt.toString(), textDfa2);
        assertEquals(specTxt.toString(), textNfa2);
    }

    @Test
    public void testAutomatonEventNamingConflicts() {
        // Create CIF specification.
        StringBuilder specTxt = new StringBuilder();
        specTxt.append("group g:\n");
        specTxt.append("  group h:\n");
        specTxt.append("    event e1;\n");
        specTxt.append("  end\n");
        specTxt.append("end\n");
        specTxt.append("automaton aut:\n");
        specTxt.append("  location loc1:\n");
        specTxt.append("    initial;\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge g.h.e1 goto loc1;\n");
        specTxt.append("end");
        CifReader reader = new CifReader().init("dummy", "dummy", false);
        Specification spec = reader.read(specTxt.toString());

        // Convert to AutomataLib.
        Automaton cifAut = spec.getComponents().stream().filter(c -> c instanceof Automaton).map(c -> (Automaton)c)
                .collect(Collectors.toList()).get(0);
        CompactDFA<String> dfa1 = CifToAutomataLib.cifAutomatonToCompactDfa(cifAut, false);
        CompactNFA<String> nfa1 = CifToAutomataLib.cifAutomatonToCompactNfa(cifAut, false);
        FastDFA<String> dfa2 = CifToAutomataLib.cifAutomatonToFastDfa(cifAut, false);
        FastNFA<String> nfa2 = CifToAutomataLib.cifAutomatonToFastNfa(cifAut, false);

        // Test automata is ancestor of event.
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsaToCifSpecification(dfa1, "g", true); });
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsaToCifSpecification(nfa1, "g", true); });
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsaToCifSpecification(dfa2, "g", true); });
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsaToCifSpecification(nfa2, "g", true); });

        // Test automata and event same absolute name.
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsaToCifSpecification(dfa1, "g.h.e1", true); });
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsaToCifSpecification(nfa1, "g.h.e1", true); });
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsaToCifSpecification(dfa2, "g.h.e1", true); });
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsaToCifSpecification(nfa2, "g.h.e1", true); });

        // Test automata ancestor group and event same absolute name.
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsaToCifSpecification(dfa1, "g.h.e1.i.aut", true); });
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsaToCifSpecification(nfa1, "g.h.e1.i.aut", true); });
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsaToCifSpecification(dfa2, "g.h.e1.i.aut", true); });
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsaToCifSpecification(nfa2, "g.h.e1.i.aut", true); });
    }

    @Test
    public void testEvtLocNamesOverlap() {
        // Create CIF specification.
        StringBuilder specTxt = new StringBuilder();
        specTxt.append("automaton aut:\n");
        specTxt.append("  event loc2;\n");
        specTxt.append("  event loc3;\n");
        specTxt.append("  event loc3_;\n");
        specTxt.append("  event loc3__;\n");
        specTxt.append("  location loc1:\n");
        specTxt.append("    initial;\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge loc2 goto loc2_;\n");
        specTxt.append("    edge loc3 goto loc2_;\n");
        specTxt.append("    edge loc3_ goto loc2_;\n");
        specTxt.append("    edge loc3__ goto loc2_;\n");
        specTxt.append("  location loc2_:\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge loc2 goto loc3___;\n");
        specTxt.append("  location loc3___:\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge loc2 goto loc4;\n");
        specTxt.append("  location loc4:\n");
        specTxt.append("    marked;\n");
        specTxt.append("end");
        CifReader reader = new CifReader().init("dummy", "dummy", false);
        Specification spec = reader.read(specTxt.toString());

        // Convert to AutomataLib.
        Automaton cifAut = spec.getComponents().stream().filter(c -> c instanceof Automaton).map(c -> (Automaton)c)
                .collect(Collectors.toList()).get(0);
        CompactDFA<String> dfa1 = CifToAutomataLib.cifAutomatonToCompactDfa(cifAut, false);
        CompactNFA<String> nfa1 = CifToAutomataLib.cifAutomatonToCompactNfa(cifAut, false);
        FastDFA<String> dfa2 = CifToAutomataLib.cifAutomatonToFastDfa(cifAut, false);
        FastNFA<String> nfa2 = CifToAutomataLib.cifAutomatonToFastNfa(cifAut, false);

        // Convert back to CIF.
        Specification specDfa1 = AutomataLibToCif.fsaToCifSpecification(dfa1, "aut", true);
        Specification specNfa1 = AutomataLibToCif.fsaToCifSpecification(nfa1, "aut", true);
        Specification specDfa2 = AutomataLibToCif.fsaToCifSpecification(dfa2, "aut", true);
        Specification specNfa2 = AutomataLibToCif.fsaToCifSpecification(nfa2, "aut", true);

        // Convert CIF specifications to text.
        String textDfa1 = CifPrettyPrinter.boxSpec(specDfa1).toString();
        String textNfa1 = CifPrettyPrinter.boxSpec(specNfa1).toString();
        String textDfa2 = CifPrettyPrinter.boxSpec(specDfa2).toString();
        String textNfa2 = CifPrettyPrinter.boxSpec(specNfa2).toString();

        // Compare round trip.
        assertEquals(specTxt.toString(), textDfa1);
        assertEquals(specTxt.toString(), textNfa1);
        assertEquals(specTxt.toString(), textDfa2);
        assertEquals(specTxt.toString(), textNfa2);
    }

    @Test
    public void testAbsoluteNames() {
        // Create CIF specification.
        StringBuilder specTxt = new StringBuilder();
        specTxt.append("event e1;\n");
        specTxt.append("event e2;\n");
        specTxt.append("group a:\n");
        specTxt.append("  event a1;\n");
        specTxt.append("  event a2;\n");
        specTxt.append("  group b:\n");
        specTxt.append("    event b1;\n");
        specTxt.append("    event b2;\n");
        specTxt.append("    automaton c:\n");
        specTxt.append("      event c1;\n");
        specTxt.append("      event c2;\n");
        specTxt.append("      location loc1:\n");
        specTxt.append("        marked;\n");
        specTxt.append("        edge e1 goto loc1;\n");
        specTxt.append("        edge e2 goto loc1;\n");
        specTxt.append("        edge a1 goto loc1;\n");
        specTxt.append("        edge a2 goto loc1;\n");
        specTxt.append("        edge b1 goto loc1;\n");
        specTxt.append("        edge b2 goto loc1;\n");
        specTxt.append("        edge c1 goto loc1;\n");
        specTxt.append("        edge c2 goto loc1;\n");
        specTxt.append("        edge g.g1 goto loc1;\n");
        specTxt.append("        edge g.g2 goto loc1;\n");
        specTxt.append("        edge g.h.h1 goto loc1;\n");
        specTxt.append("        edge g.h.h2 goto loc1;\n");
        specTxt.append("    end\n");
        specTxt.append("  end\n");
        specTxt.append("end\n");
        specTxt.append("group g:\n");
        specTxt.append("  event g1;\n");
        specTxt.append("  event g2;\n");
        specTxt.append("  group h:\n");
        specTxt.append("    event h1;\n");
        specTxt.append("    event h2;\n");
        specTxt.append("  end\n");
        specTxt.append("end");
        CifReader reader = new CifReader().init("dummy", "dummy", false);
        Specification spec = reader.read(specTxt.toString());

        // Convert to AutomataLib.
        Group groupA = spec.getComponents().stream().filter(c -> c instanceof Group).map(c -> ((Group)c))
                .filter(g -> g.getName().equals("a")).collect(Collectors.toList()).get(0);
        Group groupB = groupA.getComponents().stream().filter(c -> c instanceof Group).map(c -> ((Group)c))
                .filter(g -> g.getName().equals("b")).collect(Collectors.toList()).get(0);
        Automaton cifAut = groupB.getComponents().stream().filter(c -> c instanceof Automaton).map(c -> ((Automaton)c))
                .collect(Collectors.toList()).get(0);
        CompactDFA<String> dfa1 = CifToAutomataLib.cifAutomatonToCompactDfa(cifAut, false);
        CompactNFA<String> nfa1 = CifToAutomataLib.cifAutomatonToCompactNfa(cifAut, false);
        FastDFA<String> dfa2 = CifToAutomataLib.cifAutomatonToFastDfa(cifAut, false);
        FastNFA<String> nfa2 = CifToAutomataLib.cifAutomatonToFastNfa(cifAut, false);

        // Convert back to CIF.
        Specification specDfa1 = AutomataLibToCif.fsaToCifSpecification(dfa1, "a.b.c", true);
        Specification specNfa1 = AutomataLibToCif.fsaToCifSpecification(nfa1, "a.b.c", true);
        Specification specDfa2 = AutomataLibToCif.fsaToCifSpecification(dfa2, "a.b.c", true);
        Specification specNfa2 = AutomataLibToCif.fsaToCifSpecification(nfa2, "a.b.c", true);

        // Convert CIF specifications to text.
        String textDfa1 = CifPrettyPrinter.boxSpec(specDfa1).toString();
        String textNfa1 = CifPrettyPrinter.boxSpec(specNfa1).toString();
        String textDfa2 = CifPrettyPrinter.boxSpec(specDfa2).toString();
        String textNfa2 = CifPrettyPrinter.boxSpec(specNfa2).toString();

        // Compare round trip.
        assertEquals(specTxt.toString(), textDfa1);
        assertEquals(specTxt.toString(), textNfa1);
        assertEquals(specTxt.toString(), textDfa2);
        assertEquals(specTxt.toString(), textNfa2);
    }

    @Test
    public void testTau() {
        // Create CIF specification.
        StringBuilder specTxt = new StringBuilder();
        specTxt.append("event $tau;\n");
        specTxt.append("automaton aut:\n");
        specTxt.append("  location loc1:\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge $tau goto loc3;\n");
        specTxt.append("    edge tau goto loc2;\n");
        specTxt.append("  location loc2:\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge tau goto loc2;\n");
        specTxt.append("  location loc3:\n");
        specTxt.append("    marked;\n");
        specTxt.append("end");
        CifReader reader = new CifReader().init("dummy", "dummy", false);
        Specification spec = reader.read(specTxt.toString());

        // Convert to AutomataLib.
        Automaton cifAut = spec.getComponents().stream().filter(c -> c instanceof Automaton).map(c -> (Automaton)c)
                .collect(Collectors.toList()).get(0);
        CompactDFA<String> dfa1 = CifToAutomataLib.cifAutomatonToCompactDfa(cifAut, false);
        CompactNFA<String> nfa1 = CifToAutomataLib.cifAutomatonToCompactNfa(cifAut, false);
        FastDFA<String> dfa2 = CifToAutomataLib.cifAutomatonToFastDfa(cifAut, false);
        FastNFA<String> nfa2 = CifToAutomataLib.cifAutomatonToFastNfa(cifAut, false);

        // Convert back to CIF.
        Specification specDfa1 = AutomataLibToCif.fsaToCifSpecification(dfa1, "aut", true);
        Specification specNfa1 = AutomataLibToCif.fsaToCifSpecification(nfa1, "aut", true);
        Specification specDfa2 = AutomataLibToCif.fsaToCifSpecification(dfa2, "aut", true);
        Specification specNfa2 = AutomataLibToCif.fsaToCifSpecification(nfa2, "aut", true);

        // Convert CIF specifications to text.
        String textDfa1 = CifPrettyPrinter.boxSpec(specDfa1).toString();
        String textNfa1 = CifPrettyPrinter.boxSpec(specNfa1).toString();
        String textDfa2 = CifPrettyPrinter.boxSpec(specDfa2).toString();
        String textNfa2 = CifPrettyPrinter.boxSpec(specNfa2).toString();

        // Compare round trip.
        assertEquals(specTxt.toString(), textDfa1);
        assertEquals(specTxt.toString(), textNfa1);
        assertEquals(specTxt.toString(), textDfa2);
        assertEquals(specTxt.toString(), textNfa2);
    }

    @Test
    public void testAllAcceptingAddMarking() {
        // Create CIF specification.
        StringBuilder specTxt = new StringBuilder();
        specTxt.append("event e1;\n");
        specTxt.append("automaton aut:\n");
        specTxt.append("  location loc1:\n");
        specTxt.append("    marked false;\n");
        specTxt.append("    edge e1 goto loc2;\n");
        specTxt.append("  location loc2:\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge e1 goto loc3;\n");
        specTxt.append("  location loc3:\n");
        specTxt.append("    marked true;\n");
        specTxt.append("end");
        CifReader reader = new CifReader().init("dummy", "dummy", false);
        Specification spec = reader.read(specTxt.toString());

        // Convert to AutomataLib.
        Automaton cifAut = spec.getComponents().stream().filter(c -> c instanceof Automaton).map(c -> (Automaton)c)
                .collect(Collectors.toList()).get(0);
        CompactDFA<String> dfa1 = CifToAutomataLib.cifAutomatonToCompactDfa(cifAut, true);
        CompactNFA<String> nfa1 = CifToAutomataLib.cifAutomatonToCompactNfa(cifAut, true);
        FastDFA<String> dfa2 = CifToAutomataLib.cifAutomatonToFastDfa(cifAut, true);
        FastNFA<String> nfa2 = CifToAutomataLib.cifAutomatonToFastNfa(cifAut, true);

        // Convert back to CIF.
        Specification specDfa1 = AutomataLibToCif.fsaToCifSpecification(dfa1, "aut", true);
        Specification specNfa1 = AutomataLibToCif.fsaToCifSpecification(nfa1, "aut", true);
        Specification specDfa2 = AutomataLibToCif.fsaToCifSpecification(dfa2, "aut", true);
        Specification specNfa2 = AutomataLibToCif.fsaToCifSpecification(nfa2, "aut", true);

        // Convert CIF specifications to text.
        String textDfa1 = CifPrettyPrinter.boxSpec(specDfa1).toString();
        String textNfa1 = CifPrettyPrinter.boxSpec(specNfa1).toString();
        String textDfa2 = CifPrettyPrinter.boxSpec(specDfa2).toString();
        String textNfa2 = CifPrettyPrinter.boxSpec(specNfa2).toString();

        // Compare round trip.
        String expected = specTxt.toString().replace("marked true", "marked").replace("marked false", "marked");
        assertEquals(expected, textDfa1);
        assertEquals(expected, textNfa1);
        assertEquals(expected, textDfa2);
        assertEquals(expected, textNfa2);
    }

    @Test
    public void testAllAcceptingDontAddMarking() {
        // Create CIF specification.
        StringBuilder specTxt = new StringBuilder();
        specTxt.append("event e1;\n");
        specTxt.append("automaton aut:\n");
        specTxt.append("  location loc1:\n");
        specTxt.append("    marked false;\n");
        specTxt.append("    edge e1 goto loc2;\n");
        specTxt.append("  location loc2:\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge e1 goto loc3;\n");
        specTxt.append("  location loc3:\n");
        specTxt.append("    marked true;\n");
        specTxt.append("end");
        CifReader reader = new CifReader().init("dummy", "dummy", false);
        Specification spec = reader.read(specTxt.toString());

        // Convert to AutomataLib.
        Automaton cifAut = spec.getComponents().stream().filter(c -> c instanceof Automaton).map(c -> (Automaton)c)
                .collect(Collectors.toList()).get(0);
        CompactDFA<String> dfa1 = CifToAutomataLib.cifAutomatonToCompactDfa(cifAut, true);
        CompactNFA<String> nfa1 = CifToAutomataLib.cifAutomatonToCompactNfa(cifAut, true);
        FastDFA<String> dfa2 = CifToAutomataLib.cifAutomatonToFastDfa(cifAut, true);
        FastNFA<String> nfa2 = CifToAutomataLib.cifAutomatonToFastNfa(cifAut, true);

        // Convert back to CIF.
        Specification specDfa1 = AutomataLibToCif.fsaToCifSpecification(dfa1, "aut", false);
        Specification specNfa1 = AutomataLibToCif.fsaToCifSpecification(nfa1, "aut", false);
        Specification specDfa2 = AutomataLibToCif.fsaToCifSpecification(dfa2, "aut", false);
        Specification specNfa2 = AutomataLibToCif.fsaToCifSpecification(nfa2, "aut", false);

        // Convert CIF specifications to text.
        String textDfa1 = CifPrettyPrinter.boxSpec(specDfa1).toString();
        String textNfa1 = CifPrettyPrinter.boxSpec(specNfa1).toString();
        String textDfa2 = CifPrettyPrinter.boxSpec(specDfa2).toString();
        String textNfa2 = CifPrettyPrinter.boxSpec(specNfa2).toString();

        // Compare round trip.
        StringBuilder expectedTxt = new StringBuilder();
        expectedTxt.append("event e1;\n");
        expectedTxt.append("automaton aut:\n");
        expectedTxt.append("  location loc1:\n");
        expectedTxt.append("    edge e1 goto loc2;\n");
        expectedTxt.append("  location loc2:\n");
        expectedTxt.append("    edge e1 goto loc3;\n");
        expectedTxt.append("  location loc3;\n");
        expectedTxt.append("end");

        assertEquals(expectedTxt.toString(), textDfa1);
        assertEquals(expectedTxt.toString(), textNfa1);
        assertEquals(expectedTxt.toString(), textDfa2);
        assertEquals(expectedTxt.toString(), textNfa2);
    }

    @Test
    public void testNotAllAcceptingAddMarkingPartialMarked() {
        // Create CIF specification.
        StringBuilder specTxt = new StringBuilder();
        specTxt.append("event e1;\n");
        specTxt.append("automaton aut:\n");
        specTxt.append("  location loc1:\n");
        specTxt.append("    marked false;\n");
        specTxt.append("    edge e1 goto loc2;\n");
        specTxt.append("  location loc2:\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge e1 goto loc3;\n");
        specTxt.append("  location loc3:\n");
        specTxt.append("    marked true;\n");
        specTxt.append("end");
        CifReader reader = new CifReader().init("dummy", "dummy", false);
        Specification spec = reader.read(specTxt.toString());

        // Convert to AutomataLib.
        Automaton cifAut = spec.getComponents().stream().filter(c -> c instanceof Automaton).map(c -> (Automaton)c)
                .collect(Collectors.toList()).get(0);
        CompactDFA<String> dfa1 = CifToAutomataLib.cifAutomatonToCompactDfa(cifAut, false);
        CompactNFA<String> nfa1 = CifToAutomataLib.cifAutomatonToCompactNfa(cifAut, false);
        FastDFA<String> dfa2 = CifToAutomataLib.cifAutomatonToFastDfa(cifAut, false);
        FastNFA<String> nfa2 = CifToAutomataLib.cifAutomatonToFastNfa(cifAut, false);

        // Convert back to CIF.
        Specification specDfa1 = AutomataLibToCif.fsaToCifSpecification(dfa1, "aut", true);
        Specification specNfa1 = AutomataLibToCif.fsaToCifSpecification(nfa1, "aut", true);
        Specification specDfa2 = AutomataLibToCif.fsaToCifSpecification(dfa2, "aut", true);
        Specification specNfa2 = AutomataLibToCif.fsaToCifSpecification(nfa2, "aut", true);

        // Convert CIF specifications to text.
        String textDfa1 = CifPrettyPrinter.boxSpec(specDfa1).toString();
        String textNfa1 = CifPrettyPrinter.boxSpec(specNfa1).toString();
        String textDfa2 = CifPrettyPrinter.boxSpec(specDfa2).toString();
        String textNfa2 = CifPrettyPrinter.boxSpec(specNfa2).toString();

        // Compare round trip.
        String expected = specTxt.toString().replace("marked true", "marked");
        assertEquals(expected, textDfa1);
        assertEquals(expected, textNfa1);
        assertEquals(expected, textDfa2);
        assertEquals(expected, textNfa2);
    }

    @Test
    public void testNotAllAcceptingDontAddMarkingPartialMarked() {
        // Create CIF specification.
        StringBuilder specTxt = new StringBuilder();
        specTxt.append("event e1;\n");
        specTxt.append("automaton aut:\n");
        specTxt.append("  location loc1:\n");
        specTxt.append("    marked false;\n");
        specTxt.append("    edge e1 goto loc2;\n");
        specTxt.append("  location loc2:\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge e1 goto loc3;\n");
        specTxt.append("  location loc3:\n");
        specTxt.append("    marked true;\n");
        specTxt.append("end");
        CifReader reader = new CifReader().init("dummy", "dummy", false);
        Specification spec = reader.read(specTxt.toString());

        // Convert to AutomataLib.
        Automaton cifAut = spec.getComponents().stream().filter(c -> c instanceof Automaton).map(c -> (Automaton)c)
                .collect(Collectors.toList()).get(0);
        CompactDFA<String> dfa1 = CifToAutomataLib.cifAutomatonToCompactDfa(cifAut, false);
        CompactNFA<String> nfa1 = CifToAutomataLib.cifAutomatonToCompactNfa(cifAut, false);
        FastDFA<String> dfa2 = CifToAutomataLib.cifAutomatonToFastDfa(cifAut, false);
        FastNFA<String> nfa2 = CifToAutomataLib.cifAutomatonToFastNfa(cifAut, false);

        // Convert back to CIF.
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsaToCifSpecification(dfa1, "aut", false); });
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsaToCifSpecification(nfa1, "aut", false); });
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsaToCifSpecification(dfa2, "aut", false); });
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsaToCifSpecification(nfa2, "aut", false); });
    }

    @Test
    public void testMultipleAutomataTransformSeparate() {
        // Create CIF specification.
        StringBuilder specTxt = new StringBuilder();
        specTxt.append("event e1;\n");
        specTxt.append("automaton aut1:\n");
        specTxt.append("  event a1;\n");
        specTxt.append("  location loc1:\n");
        specTxt.append("    initial;\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge e1 goto loc2;\n");
        specTxt.append("    edge a1 goto loc2;\n");
        specTxt.append("    edge g.g1 goto loc2;\n");
        specTxt.append("    edge g.aut2.a2 goto loc2;\n");
        specTxt.append("  location loc2:\n");
        specTxt.append("    marked;\n");
        specTxt.append("end\n");
        specTxt.append("group g:\n");
        specTxt.append("  event g1;\n");
        specTxt.append("  automaton aut2:\n");
        specTxt.append("    event a2;\n");
        specTxt.append("    location loc1:\n");
        specTxt.append("      initial;\n");
        specTxt.append("      marked;\n");
        specTxt.append("    location loc2:\n");
        specTxt.append("      marked;\n");
        specTxt.append("      edge e1 goto loc2;\n");
        specTxt.append("      edge aut1.a1 goto loc2;\n");
        specTxt.append("      edge g1 goto loc2;\n");
        specTxt.append("      edge a2 goto loc2;\n");
        specTxt.append("  end\n");
        specTxt.append("end");
        CifReader reader = new CifReader().init("dummy", "dummy", false);
        Specification spec = reader.read(specTxt.toString());

        // Convert to AutomataLib.
        Automaton aut1 = spec.getComponents().stream().filter(c -> c instanceof Automaton).map(c -> (Automaton)c)
                .collect(Collectors.toList()).get(0);
        Group groupG = spec.getComponents().stream().filter(c -> c instanceof Group).map(c -> (Group)c)
                .collect(Collectors.toList()).get(0);
        Automaton aut2 = groupG.getComponents().stream().filter(c -> c instanceof Automaton).map(c -> (Automaton)c)
                .collect(Collectors.toList()).get(0);

        CompactDFA<String> aut1Dfa1 = CifToAutomataLib.cifAutomatonToCompactDfa(aut1, false);
        CompactNFA<String> aut1Nfa1 = CifToAutomataLib.cifAutomatonToCompactNfa(aut1, false);
        FastDFA<String> aut1Dfa2 = CifToAutomataLib.cifAutomatonToFastDfa(aut1, false);
        FastNFA<String> aut1Nfa2 = CifToAutomataLib.cifAutomatonToFastNfa(aut1, false);

        CompactDFA<String> aut2Dfa1 = CifToAutomataLib.cifAutomatonToCompactDfa(aut2, false);
        CompactNFA<String> aut2Nfa1 = CifToAutomataLib.cifAutomatonToCompactNfa(aut2, false);
        FastDFA<String> aut2Dfa2 = CifToAutomataLib.cifAutomatonToFastDfa(aut2, false);
        FastNFA<String> aut2Nfa2 = CifToAutomataLib.cifAutomatonToFastNfa(aut2, false);

        // Convert back to CIF.
        Specification aut1SpecDfa1 = AutomataLibToCif.fsaToCifSpecification(aut1Dfa1, "aut1", true);
        Specification aut1SpecNfa1 = AutomataLibToCif.fsaToCifSpecification(aut1Nfa1, "aut1", true);
        Specification aut1SpecDfa2 = AutomataLibToCif.fsaToCifSpecification(aut1Dfa2, "aut1", true);
        Specification aut1SpecNfa2 = AutomataLibToCif.fsaToCifSpecification(aut1Nfa2, "aut1", true);

        Specification aut2SpecDfa1 = AutomataLibToCif.fsaToCifSpecification(aut2Dfa1, "g.aut2", true);
        Specification aut2SpecNfa1 = AutomataLibToCif.fsaToCifSpecification(aut2Nfa1, "g.aut2", true);
        Specification aut2SpecDfa2 = AutomataLibToCif.fsaToCifSpecification(aut2Dfa2, "g.aut2", true);
        Specification aut2SpecNfa2 = AutomataLibToCif.fsaToCifSpecification(aut2Nfa2, "g.aut2", true);

        // Convert CIF specifications to text.
        String aut1TextDfa1 = CifPrettyPrinter.boxSpec(aut1SpecDfa1).toString();
        String aut1TextNfa1 = CifPrettyPrinter.boxSpec(aut1SpecNfa1).toString();
        String aut1TextDfa2 = CifPrettyPrinter.boxSpec(aut1SpecDfa2).toString();
        String aut1TextNfa2 = CifPrettyPrinter.boxSpec(aut1SpecNfa2).toString();

        String aut2TextDfa1 = CifPrettyPrinter.boxSpec(aut2SpecDfa1).toString();
        String aut2TextNfa1 = CifPrettyPrinter.boxSpec(aut2SpecNfa1).toString();
        String aut2TextDfa2 = CifPrettyPrinter.boxSpec(aut2SpecDfa2).toString();
        String aut2TextNfa2 = CifPrettyPrinter.boxSpec(aut2SpecNfa2).toString();

        // Get expected single automaton results.
        StringBuilder expectedAut1 = new StringBuilder();
        expectedAut1.append("event e1;\n");
        expectedAut1.append("automaton aut1:\n");
        expectedAut1.append("  event a1;\n");
        expectedAut1.append("  location loc1:\n");
        expectedAut1.append("    initial;\n");
        expectedAut1.append("    marked;\n");
        expectedAut1.append("    edge e1 goto loc2;\n");
        expectedAut1.append("    edge a1 goto loc2;\n");
        expectedAut1.append("    edge g.g1 goto loc2;\n");
        expectedAut1.append("    edge g.aut2.a2 goto loc2;\n");
        expectedAut1.append("  location loc2:\n");
        expectedAut1.append("    marked;\n");
        expectedAut1.append("end\n");
        expectedAut1.append("group g:\n");
        expectedAut1.append("  event g1;\n");
        expectedAut1.append("  group aut2:\n");
        expectedAut1.append("    event a2;\n");
        expectedAut1.append("  end\n");
        expectedAut1.append("end");

        StringBuilder expectedAut2 = new StringBuilder();
        expectedAut2.append("event e1;\n");
        expectedAut2.append("group g:\n");
        expectedAut2.append("  event g1;\n");
        expectedAut2.append("  automaton aut2:\n");
        expectedAut2.append("    event a2;\n");
        expectedAut2.append("    location loc1:\n");
        expectedAut2.append("      initial;\n");
        expectedAut2.append("      marked;\n");
        expectedAut2.append("    location loc2:\n");
        expectedAut2.append("      marked;\n");
        expectedAut2.append("      edge e1 goto loc2;\n");
        expectedAut2.append("      edge aut1.a1 goto loc2;\n");
        expectedAut2.append("      edge g1 goto loc2;\n");
        expectedAut2.append("      edge a2 goto loc2;\n");
        expectedAut2.append("  end\n");
        expectedAut2.append("end\n");
        expectedAut2.append("group aut1:\n");
        expectedAut2.append("  event a1;\n");
        expectedAut2.append("end");

        // Compare round trip (single automaton).
        assertEquals(expectedAut1.toString(), aut1TextDfa1);
        assertEquals(expectedAut1.toString(), aut1TextNfa1);
        assertEquals(expectedAut1.toString(), aut1TextDfa2);
        assertEquals(expectedAut1.toString(), aut1TextNfa2);

        assertEquals(expectedAut2.toString(), aut2TextDfa1);
        assertEquals(expectedAut2.toString(), aut2TextNfa1);
        assertEquals(expectedAut2.toString(), aut2TextDfa2);
        assertEquals(expectedAut2.toString(), aut2TextNfa2);

        // Merge single automaton specifications.
        CifMerger mergerDfa1 = new CifMerger();
        CifMerger mergerNfa1 = new CifMerger();
        CifMerger mergerDfa2 = new CifMerger();
        CifMerger mergerNfa2 = new CifMerger();
        Specification mergedDfa1 = mergerDfa1.merge(aut1SpecDfa1, aut2SpecDfa1);
        Specification mergedNfa1 = mergerNfa1.merge(aut1SpecNfa1, aut2SpecNfa1);
        Specification mergedDfa2 = mergerDfa2.merge(aut1SpecDfa2, aut2SpecDfa2);
        Specification mergedNfa2 = mergerNfa2.merge(aut1SpecNfa2, aut2SpecNfa2);

        // Convert merged CIF specifications to text.
        String mergedTextDfa1 = CifPrettyPrinter.boxSpec(mergedDfa1).toString();
        String mergedTextNfa1 = CifPrettyPrinter.boxSpec(mergedNfa1).toString();
        String mergedTextDfa2 = CifPrettyPrinter.boxSpec(mergedDfa2).toString();
        String mergedTextNfa2 = CifPrettyPrinter.boxSpec(mergedNfa2).toString();

        // Compare round trip.
        assertEquals(specTxt.toString(), mergedTextDfa1);
        assertEquals(specTxt.toString(), mergedTextNfa1);
        assertEquals(specTxt.toString(), mergedTextDfa2);
        assertEquals(specTxt.toString(), mergedTextNfa2);
    }

    @Test
    public void testMultipleAutomataTransformTogether() {
        // Create CIF specification.
        StringBuilder specTxt = new StringBuilder();
        specTxt.append("event e1;\n");
        specTxt.append("group g:\n");
        specTxt.append("  event g1;\n");
        specTxt.append("  automaton aut2:\n");
        specTxt.append("    event a2;\n");
        specTxt.append("    location loc1:\n");
        specTxt.append("      initial;\n");
        specTxt.append("      marked;\n");
        specTxt.append("    location loc2:\n");
        specTxt.append("      marked;\n");
        specTxt.append("      edge e1 goto loc2;\n");
        specTxt.append("      edge aut1.a1 goto loc2;\n");
        specTxt.append("      edge g1 goto loc2;\n");
        specTxt.append("      edge a2 goto loc2;\n");
        specTxt.append("  end\n");
        specTxt.append("end\n");
        specTxt.append("automaton aut1:\n");
        specTxt.append("  event a1;\n");
        specTxt.append("  location loc1:\n");
        specTxt.append("    initial;\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge e1 goto loc2;\n");
        specTxt.append("    edge a1 goto loc2;\n");
        specTxt.append("    edge g.g1 goto loc2;\n");
        specTxt.append("    edge g.aut2.a2 goto loc2;\n");
        specTxt.append("  location loc2:\n");
        specTxt.append("    marked;\n");
        specTxt.append("end");
        CifReader reader = new CifReader().init("dummy", "dummy", false);
        Specification spec = reader.read(specTxt.toString());

        // Convert to AutomataLib.
        Map<String, CompactDFA<String>> dfas1 = CifToAutomataLib.cifSpecificationToCompactDfas(spec, false);
        Map<String, CompactNFA<String>> nfas1 = CifToAutomataLib.cifSpecificationToCompactNfas(spec, false);
        Map<String, FastDFA<String>> dfas2 = CifToAutomataLib.cifSpecificationToFastDfas(spec, false);
        Map<String, FastNFA<String>> nfas2 = CifToAutomataLib.cifSpecificationToFastNfas(spec, false);

        // Convert back to CIF.
        Specification specDfa1 = AutomataLibToCif.fsasToCifSpecification(dfas1, true);
        Specification specNfa1 = AutomataLibToCif.fsasToCifSpecification(nfas1, true);
        Specification specDfa2 = AutomataLibToCif.fsasToCifSpecification(dfas2, true);
        Specification specNfa2 = AutomataLibToCif.fsasToCifSpecification(nfas2, true);

        // Convert CIF specifications to text.
        String textDfa1 = CifPrettyPrinter.boxSpec(specDfa1).toString();
        String textNfa1 = CifPrettyPrinter.boxSpec(specNfa1).toString();
        String textDfa2 = CifPrettyPrinter.boxSpec(specDfa2).toString();
        String textNfa2 = CifPrettyPrinter.boxSpec(specNfa2).toString();

        // Compare round trip.
        assertEquals(specTxt.toString(), textDfa1);
        assertEquals(specTxt.toString(), textNfa1);
        assertEquals(specTxt.toString(), textDfa2);
        assertEquals(specTxt.toString(), textNfa2);
    }

    @Test
    public void testAutomataNamesAreAncestors() {
        // Create CIF specification.
        StringBuilder specTxt = new StringBuilder();
        specTxt.append("event e1;\n");
        specTxt.append("automaton aut1:\n");
        specTxt.append("  location loc1:\n");
        specTxt.append("    initial;\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge e1 goto loc1;\n");
        specTxt.append("end\n");
        specTxt.append("automaton aut2:\n");
        specTxt.append("  location loc1:\n");
        specTxt.append("    initial;\n");
        specTxt.append("    marked;\n");
        specTxt.append("    edge e1 goto loc1;\n");
        specTxt.append("end");
        CifReader reader = new CifReader().init("dummy", "dummy", false);
        Specification spec = reader.read(specTxt.toString());

        // Convert to AutomataLib.
        Map<String, CompactDFA<String>> dfas = CifToAutomataLib.cifSpecificationToCompactDfas(spec, false);

        // Convert back to CIF.
        AutomataLibToCif.fsasToCifSpecification(dfas, true);

        Map<String, CompactDFA<String>> dfasRenamed = map();
        dfasRenamed.put("aut", dfas.get("aut1"));
        dfasRenamed.put("aut.aut", dfas.get("aut2"));
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsasToCifSpecification(dfasRenamed, true); });

        dfasRenamed.clear();
        dfasRenamed.put("g.aut", dfas.get("aut1"));
        dfasRenamed.put("g.aut.g.aut", dfas.get("aut2"));
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsasToCifSpecification(dfasRenamed, true); });

        dfasRenamed.clear();
        dfasRenamed.put("g.aut.g.aut", dfas.get("aut1"));
        dfasRenamed.put("g.aut", dfas.get("aut2"));
        assertThrows(ConversionPreconditionException.class,
                () ->
                { AutomataLibToCif.fsasToCifSpecification(dfasRenamed, true); });
    }
}
