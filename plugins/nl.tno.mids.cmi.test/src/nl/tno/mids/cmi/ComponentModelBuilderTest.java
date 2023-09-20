/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.lsat.common.emf.ecore.resource.Persistor;
import org.eclipse.lsat.common.emf.ecore.resource.PersistorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.automatalib.automata.concepts.InputAlphabetHolder;
import net.automatalib.automata.concepts.StateIDs;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.words.Word;
import nl.esi.pps.tmsc.FullScopeTMSC;
import nl.esi.pps.tmsc.xtext.TmscXtextStandaloneSetup;
import nl.esi.pps.tmsc.xtext.generator.TmscXtextToTmscTransformation;
import nl.esi.pps.tmsc.xtext.generator.TmscXtextToTmscTransformation.Result;
import nl.esi.pps.tmsc.xtext.tmscXtext.TmscXtextModel;
import nl.tno.mids.cif.extensions.FileExtensions;
import nl.tno.mids.common.unittest.FileCompare;
import nl.tno.mids.pps.extensions.cmi.CmiPreparers;

class ComponentModelBuilderTest {
    @BeforeAll
    static void beforeAll() {
        TmscXtextStandaloneSetup.doSetup();
    }

    @Nested
    class BasicWords {
        @Test
        void testEmpty() {
            ComponentModelBuilder builder = new ComponentModelBuilder();
            for (int i = 0; i < 10; i++) {
                builder.insert("testComponent", Word.fromSymbols());
            }

            FastDFA<String> pta = builder.getPTA("testComponent");
            String ptaExpected = "state 0 (initial) (accepting)\n";
            assertEquals(ptaExpected, dfaToString(pta));

            FastDFA<String> dfa = builder.getDFA("testComponent");
            String dfaExpected = "state 0 (initial) (accepting)\n";
            assertEquals(dfaExpected, dfaToString(dfa));
        }

        @Test
        void testServiceFragments() {
            ComponentModelBuilder builder = new ComponentModelBuilder();
            builder.insert("testComponent", Word.fromSymbols("f1", "f1_ret"));
            builder.insert("testComponent", Word.fromSymbols("f1", "f2", "f2_ret", "f1_ret"));
            builder.insert("testComponent", Word.fromSymbols("f3", "f3_ret"));
            builder.insert("testComponent", Word.fromSymbols("f3", "f2", "f2_ret", "f3_ret"));

            FastDFA<String> pta = builder.getPTA("testComponent");
            String ptaExpected = "state 0 (initial)\n" + //
                    "  f1 -> 1\n" + //
                    "  f3 -> 6\n" + //
                    "state 1\n" + //
                    "  f1_ret -> 2\n" + //
                    "  f2 -> 3\n" + //
                    "state 2 (accepting)\n" + //
                    "state 3\n" + //
                    "  f2_ret -> 4\n" + //
                    "state 4\n" + //
                    "  f1_ret -> 5\n" + //
                    "state 5 (accepting)\n" + //
                    "state 6\n" + //
                    "  f2 -> 8\n" + //
                    "  f3_ret -> 7\n" + //
                    "state 7 (accepting)\n" + //
                    "state 8\n" + //
                    "  f2_ret -> 9\n" + //
                    "state 9\n" + //
                    "  f3_ret -> 10\n" + //
                    "state 10 (accepting)\n";
            assertEquals(ptaExpected, dfaToString(pta));

            FastDFA<String> dfa = builder.getDFA("testComponent");
            String dfaExpected = "state 0 (initial) (accepting)\n" + //
                    "  f1 -> 1\n" + //
                    "  f3 -> 2\n" + //
                    "state 1\n" + //
                    "  f1_ret -> 0\n" + //
                    "  f2 -> 4\n" + //
                    "state 2\n" + //
                    "  f2 -> 5\n" + //
                    "  f3_ret -> 0\n" + //
                    "state 3\n" + //
                    "  f3_ret -> 0\n" + //
                    "state 4\n" + //
                    "  f2_ret -> 6\n" + //
                    "state 5\n" + //
                    "  f2_ret -> 3\n" + //
                    "state 6\n" + //
                    "  f1_ret -> 0\n";
            assertEquals(dfaExpected, dfaToString(dfa));
        }

        @Test
        public void testPrefix() {
            ComponentModelBuilder builder = new ComponentModelBuilder();
            builder.insert("testComponent", Word.fromSymbols("a", "b", "c"));
            builder.insert("testComponent", Word.fromSymbols("a", "b", "c", "d", "e"));

            FastDFA<String> pta = builder.getPTA("testComponent");
            String ptaExpected = "state 0 (initial)\n" + //
                    "  a -> 1\n" + //
                    "state 1\n" + //
                    "  b -> 2\n" + //
                    "state 2\n" + //
                    "  c -> 3\n" + //
                    "state 3 (accepting)\n" + //
                    "  d -> 4\n" + //
                    "state 4\n" + //
                    "  e -> 5\n" + //
                    "state 5 (accepting)\n";
            assertEquals(ptaExpected, dfaToString(pta));

            assertThrows(IllegalStateException.class, () -> { builder.getDFA("testComponent"); });
        }

        @Test
        void testAllDisjoint() {
            ComponentModelBuilder builder = new ComponentModelBuilder();
            int nrOfWords = 100;
            int wordLength = 2;
            for (int wi = 0; wi < nrOfWords; wi++) {
                Word<String> word = Word.fromSymbols();
                for (int si = 0; si < wordLength; si++) {
                    word = word.append(String.valueOf((char)wi));
                }
                builder.insert("testComponent", word);
            }

            assertEquals((nrOfWords * wordLength) + 1, builder.getPTA("testComponent").size());
            assertEquals((nrOfWords * (wordLength - 1)) + 1, builder.getDFA("testComponent").size());
        }
    }

    @ParameterizedTest
    @ValueSource(strings =
    {"Abstract", // Test with unknown call and handler type.
            "Base", // Test with basic client-server communication.
            "BlkAsync", // Test with synchronous call that is handled asynchronously.
            "EmptyLifeline", // Test with a life-line with no events.
            "Event", // Test with subscribing, raising and un-subscribing an event.
            "FcnAsync", // Test with asynchronous call that is handled asynchronously.
            "FcnSync", // Test with asynchronous call that is handled synchronously.
            "Lattice", // Test with multiple variations of the same service fragment with overlap.
            "MultipleClient", // Test with multiple clients that call the same server function.
            "MultipleServer", // Test with single client calling the same function for multiple servers.
            "Nested", // Test with client calling a function on a server, which calls another function on
                      // another server.
            "Repeated", // Test with repetitions of the same calls.
            "ReqWait", // Test with an asynchronous call with a request-wait pattern.
            "SeperateApplications", // Test with two clients calling functions of two servers, with no interaction.
            "Tree", // Test with multiple variations of the same service fragment without overlap.
            "Untraced" // Test with calls to and calls from and untraced component.
    })
    void testBasicTmsc(String testName) throws IOException {
        Path basePath = Paths.get("testData/ComponentModelBuilder/").resolve(testName);
        Path baseTmsctPath = basePath.resolve("input").resolve(testName + ".tmsct");
        Path baseActualPath = basePath.resolve("output_actual/");
        Path baseExpectedPath = basePath.resolve("output_expected/");

        Persistor<EObject> persistor = new PersistorFactory().getPersistor();
        List<EObject> fileContent = persistor.loadAll(URI.createFileURI(baseTmsctPath.toString()));

        TmscXtextModel tmsctModel = (TmscXtextModel)fileContent.get(0);
        Result tmscResult = new TmscXtextToTmscTransformation().transform(tmsctModel);
        FullScopeTMSC tmsc = tmscResult.getTmsc();

        CmiPreparers.findFor(tmsc).prepare(tmsc, "CMI", new ArrayList<>(), null);

        ComponentModelBuilder builder = new ComponentModelBuilder(true);
        builder.insert(tmsc);

        Map<String, Specification> cifModels = builder.getCifModels();

        for (String componentName: cifModels.keySet()) {
            Path cifFilePath = baseActualPath.resolve(componentName + ".cif");
            FileExtensions.saveCIF(cifModels.get(componentName), cifFilePath);
        }

        FileCompare.checkDirectoriesEqual(baseExpectedPath, baseActualPath, p -> true);
    }

    private static <S, T, A extends DFA<S, String> & InputAlphabetHolder<String> & StateIDs<S>> String
            dfaToString(A automaton)
    {
        StringBuilder s = new StringBuilder();
        for (S state: automaton.getStates()) {
            // State.
            s.append("state ");
            s.append(automaton.getStateId(state));
            if (automaton.getInitialStates().contains(state)) {
                s.append(" (initial)");
            }
            if (automaton.isAccepting(state)) {
                s.append(" (accepting)");
            }
            s.append("\n");

            // Transitions.
            for (String input: automaton.getInputAlphabet()) {
                S target = automaton.getTransition(state, input);
                if (target != null) {
                    s.append("  ");
                    s.append(input);
                    s.append(" -> ");
                    s.append(automaton.getStateId(target));
                    s.append("\n");
                }
            }
        }
        return s.toString();
    }
}
