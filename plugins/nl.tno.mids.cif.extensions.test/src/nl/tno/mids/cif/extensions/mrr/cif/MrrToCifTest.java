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

import static org.eclipse.escet.common.java.Lists.copy;
import static org.eclipse.escet.common.java.Sets.setc;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.escet.cif.common.CifEdgeUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.explorer.CifAutomatonBuilder;
import org.eclipse.escet.cif.explorer.ExplorerStateFactory;
import org.eclipse.escet.cif.explorer.app.AutomatonNameOption;
import org.eclipse.escet.cif.explorer.runtime.BaseState;
import org.eclipse.escet.cif.explorer.runtime.Explorer;
import org.eclipse.escet.cif.explorer.runtime.ExplorerBuilder;
import org.eclipse.escet.cif.io.CifReader;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.TauExpression;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.escet.common.app.framework.Application;
import org.eclipse.escet.common.app.framework.DummyApplication;
import org.eclipse.escet.common.app.framework.io.AppStreams;
import org.eclipse.escet.common.app.framework.options.Options;
import org.eclipse.escet.common.emf.EMFHelper;
import org.eclipse.escet.common.java.Strings;
import org.junit.jupiter.api.Test;

import com.google.common.base.Preconditions;

import nl.tno.mids.cif.extensions.mrr.MrrModifyUtils;
import nl.tno.mids.cif.extensions.mrr.data.MRR;
import nl.tno.mids.cif.extensions.mrr.data.MrrWithWord;
import nl.tno.mids.cif.extensions.mrr.data.RepetitionMRR;

/** {@link MrrToCif} tests. Also tests {@link CifToMrr}. */
public class MrrToCifTest {
    private static final String INPUT_NO_REPETITIONS = "abc";

    private static final String INPUT_SIMPLE1 = "abbba";

    private static final String INPUT_SIMPLE2 = Strings.duplicate(INPUT_SIMPLE1, 2);

    private static final String INPUT_NESTED = Strings.duplicate("aaabbb", 2);

    // hh (x (aa b^3 c^3)^2 y)^4 i^2
    // No loop detected for 'h' and 'a'.
    private static final String INPUT_COMPLEX_PART_PART = "aabbbccc";

    private static final String INPUT_COMPLEX_PART = "x" + Strings.duplicate(INPUT_COMPLEX_PART_PART, 2) + "y";

    private static final String INPUT_COMPLEX = "hh" + Strings.duplicate(INPUT_COMPLEX_PART, 4) + "ii";

    private static final Predicate<RepetitionMRR<CifMrrLetter>> ALWAYS_ACCEPT = (r) -> r != null;

    @Test
    public void testNoRepetitions() {
        Specification spec = CifMrrTestUtils.wordToCif(INPUT_NO_REPETITIONS);
        String txt = CifMrrTestUtils.specToStr(spec);
        test(txt, txt, null, MrrToCifMode.PLAIN);
        test(txt, txt, null, MrrToCifMode.DATA);
    }

    @Test
    public void testSimple1PlainUnchanged() {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_SIMPLE1));
        test(input, input, null, MrrToCifMode.PLAIN);
    }

    @Test
    public void testSimple1DataUnchanged() throws IOException {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_SIMPLE1));
        String expected = loadModel("expected");
        test(input, expected, null, MrrToCifMode.DATA);
    }

    @Test
    public void testSimple2PlainUnchanged() {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_SIMPLE2));
        test(input, input, null, MrrToCifMode.PLAIN);
    }

    @Test
    public void testSimple2DataUnchanged() throws IOException {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_SIMPLE2));
        String expected = loadModel("expected");
        test(input, expected, null, MrrToCifMode.DATA);
    }

    @Test
    public void testComplexPlainUnchanged() {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_COMPLEX));
        test(input, input, null, MrrToCifMode.PLAIN);
    }

    @Test
    public void testComplexPlainRestrictSingle() {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_COMPLEX));
        // Two times 'h' and 'i', due to no loop detected there.
        String expected = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif("hhxaabcyi"));
        test(input, expected, m -> MrrModifyUtils.mrrRestrictMaxRepeat(m, 1, ALWAYS_ACCEPT), MrrToCifMode.PLAIN);
    }

    @Test
    public void testComplexPlainRestrictTwo() {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_COMPLEX));
        String expected = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif("hhxaabbccaabbccyxaabbccaabbccyii"));
        test(input, expected, m -> MrrModifyUtils.mrrRestrictMaxRepeat(m, 2, ALWAYS_ACCEPT), MrrToCifMode.PLAIN);
    }

    @Test
    public void testComplexPlainRestrictThree() {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_COMPLEX));
        String expected = CifMrrTestUtils
                .specToStr(CifMrrTestUtils.wordToCif("hhxaabbbcccaabbbcccyxaabbbcccaabbbcccyxaabbbcccaabbbcccyii"));
        test(input, expected, m -> MrrModifyUtils.mrrRestrictMaxRepeat(m, 3, ALWAYS_ACCEPT), MrrToCifMode.PLAIN);
    }

    @Test
    public void testComplexPlainAsInfinite() throws IOException {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_COMPLEX));
        String expected = loadModel("expected");
        test(input, expected, m -> MrrModifyUtils.mrrSetInfiniteRepeat(m, ALWAYS_ACCEPT), MrrToCifMode.PLAIN);
    }

    @Test
    public void testComplexDataUnchanged() throws IOException {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_COMPLEX));
        String expected = loadModel("expected");
        test(input, expected, null, MrrToCifMode.DATA);
    }

    @Test
    public void testComplexDataRestrictSingle() throws IOException {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_COMPLEX));
        // Two times 'h' and 'i', due to no loop detected there.
        String expected = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif("hhxaabcyi"));
        test(input, expected, m -> MrrModifyUtils.mrrRestrictMaxRepeat(m, 1, ALWAYS_ACCEPT), MrrToCifMode.DATA);
    }

    @Test
    public void testComplexDataRestrictTwo() throws IOException {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_COMPLEX));
        String expected = loadModel("expected");
        test(input, expected, m -> MrrModifyUtils.mrrRestrictMaxRepeat(m, 2, ALWAYS_ACCEPT), MrrToCifMode.DATA);
    }

    @Test
    public void testComplexDataRestrictThree() throws IOException {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_COMPLEX));
        String expected = loadModel("expected");
        test(input, expected, m -> MrrModifyUtils.mrrRestrictMaxRepeat(m, 3, ALWAYS_ACCEPT), MrrToCifMode.DATA);
    }

    @Test
    public void testComplexDataAsInfinite() throws IOException {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_COMPLEX));
        String expected = loadModel("expected");
        test(input, expected, m -> MrrModifyUtils.mrrSetInfiniteRepeat(m, ALWAYS_ACCEPT), MrrToCifMode.DATA);
    }

    @Test
    public void testNestedPlainUnchanged() {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_NESTED));
        test(input, input, null, MrrToCifMode.PLAIN);
    }

    @Test
    public void testNestedPlainRestrictSingle() {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_NESTED));
        String expected = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif("ab"));
        test(input, expected, m -> MrrModifyUtils.mrrRestrictMaxRepeat(m, 1, ALWAYS_ACCEPT), MrrToCifMode.PLAIN);
    }

    @Test
    public void testNestedPlainRestrictTwo() {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_NESTED));
        String expected = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif("aabbaabb"));
        test(input, expected, m -> MrrModifyUtils.mrrRestrictMaxRepeat(m, 2, ALWAYS_ACCEPT), MrrToCifMode.PLAIN);
    }

    @Test
    public void testNestedPlainRestrictThree() {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_NESTED));
        test(input, input, m -> MrrModifyUtils.mrrRestrictMaxRepeat(m, 3, ALWAYS_ACCEPT), MrrToCifMode.PLAIN);
    }

    @Test
    public void testNestedPlainAsInfinite() throws IOException {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_NESTED));
        String expected = loadModel("expected");
        test(input, expected, m -> MrrModifyUtils.mrrSetInfiniteRepeat(m, ALWAYS_ACCEPT), MrrToCifMode.PLAIN);
    }

    @Test
    public void testNestedDataUnchanged() throws IOException {
        String input = CifMrrTestUtils.specToStr(CifMrrTestUtils.wordToCif(INPUT_NESTED));
        String expected = loadModel("expected");
        test(input, expected, null, MrrToCifMode.DATA);
    }

    @Test
    public void testMultipleMrrsDataUnchanged() throws IOException {
        String input = loadModel("input");
        String expected = loadModel("expected");
        test(input, expected, null, MrrToCifMode.DATA);
    }

    /**
     * Performs a single test.
     *
     * @param input The CIF specification textual input.
     * @param expectedOutput The CIF specification expected textual output.
     * @param mrrModifier The MRR modifier, or {@code null} to not modify the MRR.
     * @param mode The MRR to CIF mode.
     */
    private void test(String input, String expectedOutput, Consumer<MRR<CifMrrLetter>> mrrModifier, MrrToCifMode mode) {
        // Read CIF specification.
        CifReader reader = new CifReader();
        reader.init(".", "/", false);
        Specification spec = reader.read(input);

        // CIF to MRR.
        CifToMrrConfig config = new CifToMrrConfig();
        List<MrrWithWord<CifMrrLetter>> results = CifToMrr.cifToMrr(spec, config, new NullProgressMonitor());

        // Modify MRRs, if requested.
        if (mrrModifier != null) {
            for (MrrWithWord<CifMrrLetter> result: results) {
                mrrModifier.accept(result.mrr);
            }
        }

        // MRR to CIF.
        for (MrrWithWord<CifMrrLetter> result: results) {
            MrrToCif.mrrToCif(result, mode);
        }

        // Reorder and relabel the output.
        reorderAndRelabel(spec);

        // Compare expected/actual result.
        String actualOutput = CifMrrTestUtils.specToStr(spec);
        assertEquals(expectedOutput.replace("\r", ""), actualOutput.replace("\r", ""));

        // Compare state space again original input, but only in case the behavior is
        // preserved exactly.
        if (mrrModifier == null) {
            try {
                // Explore statespace from MRR adapted CIF specification.
                ExplorerBuilder builder = new ExplorerBuilder(spec);
                builder.collectData();
                Explorer explorer = builder.buildExplorer(new ExplorerStateFactory());
                Application<?> app = new DummyApplication(new AppStreams());
                Options.set(AutomatonNameOption.class, null);
                List<BaseState> initials = explorer.getInitialStates(app);
                Preconditions.checkArgument(initials != null && !initials.isEmpty());
                Queue<BaseState> queue = new ArrayDeque<>();
                queue.addAll(initials);
                while (!queue.isEmpty()) {
                    BaseState state = queue.poll();
                    queue.addAll(state.getNewSuccessorStates());
                }
                explorer.renumberStates();
                explorer.minimizeEdges();

                // Obtain state space as CIF model.
                CifAutomatonBuilder statespaceBuilder = new CifAutomatonBuilder();
                Specification statespace = statespaceBuilder.createAutomaton(explorer, spec);
                statespace.getComponents().get(0).setName("aut"); // Override automaton name.
                ((Automaton)statespace.getComponents().get(0)).setAlphabet(null); // Remove explicit alphabet.

                // Eliminate 'tau' edges.
                elimTauEdges(statespace);

                // Reorder and relabel the output.
                reorderAndRelabel(statespace);

                // Compare statespace with original input (textually).
                String statespaceTxt = CifMrrTestUtils.specToStr(statespace);
                assertEquals(input.replace("\r", ""), statespaceTxt.replace("\r", ""));
            } finally {
                AppEnv.unregisterApplication();
            }
        }
    }

    private void reorderAndRelabel(Specification spec) {
        // Get single automaton.
        Automaton aut = (Automaton)spec.getComponents().get(0);

        // Reorder edges. Do this before finding locations, to ensure predictable order
        // of finding the locations.
        for (Location loc: aut.getLocations()) {
            List<Edge> edges = copy(loc.getEdges());
            edges = edges.stream().sorted((e1, e2) -> getName(e1).compareTo(getName(e2))).collect(Collectors.toList());
            loc.getEdges().clear();
            loc.getEdges().addAll(edges);
        }

        // Get initial location, assumed to be the first location.
        Location loc0 = aut.getLocations().get(0);
        Preconditions.checkArgument(CifValueUtils.isTriviallyTrue(loc0.getInitials(), true, true));

        // Depth First Search for locations.
        Set<Location> foundLocs = setc(aut.getLocations().size());
        Deque<Location> todoLocs = new LinkedList<>();
        todoLocs.push(loc0);
        while (!todoLocs.isEmpty()) {
            Location loc = todoLocs.pop();
            if (!foundLocs.contains(loc)) {
                foundLocs.add(loc);
                for (Edge edge: loc.getEdges()) {
                    todoLocs.add(CifEdgeUtils.getTarget(edge));
                }
            }
        }

        // Reorder locations.
        aut.getLocations().clear();
        aut.getLocations().addAll(foundLocs);

        // Rename locations.
        List<Location> locs = aut.getLocations();
        for (int i = 0; i < locs.size(); i++) {
            locs.get(i).setName("loc" + Integer.toString(i + 1));
        }
    }

    private String getName(Edge edge) {
        Preconditions.checkArgument(edge.getEvents().size() <= 1);
        if (edge.getEvents().isEmpty()) {
            return "tau";
        }
        Expression eventRef = edge.getEvents().get(0).getEvent();
        return (eventRef instanceof TauExpression) ? "tau" : ((EventExpression)eventRef).getEvent().getName();
    }

    private void elimTauEdges(Specification statespace) {
        Automaton aut = (Automaton)statespace.getComponents().get(0);
        OUTER:
        while (true) {
            // Process at most one 'tau' edge.
            for (Location loc: aut.getLocations()) {
                for (Edge edge: loc.getEdges()) {
                    Preconditions.checkState(edge.getEvents().size() <= 1);
                    boolean isTau = edge.getEvents().isEmpty()
                            || edge.getEvents().get(0).getEvent() instanceof TauExpression;
                    if (isTau) {
                        // Remove target location.
                        Location target = CifEdgeUtils.getTarget(edge);
                        aut.getLocations().remove(target);

                        // Remove edge.
                        EMFHelper.removeFromParentContainment(edge);

                        // Move edges.
                        loc.getEdges().addAll(target.getEdges());

                        // Retarget edges.
                        for (Location loc2: aut.getLocations()) {
                            for (Edge edge2: loc2.getEdges()) {
                                if (CifEdgeUtils.getTarget(edge2) == target) {
                                    edge2.setTarget(loc);
                                }
                            }
                        }

                        // Process again for next 'tau'.
                        continue OUTER;
                    }
                }
            }

            // No 'tau' edge found.
            break;
        }
    }

    private String loadModel(String dirName) throws IOException {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        String baseName = stackTrace[1].getMethodName();
        String resourcePath = getClass().getPackage().getName().replace(".", "/");
        resourcePath += "/" + dirName + "/" + baseName + ".cif";
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return "Resource not found: " + resourcePath;
            }
            return IOUtils.readLines(stream, StandardCharsets.UTF_8).stream().collect(Collectors.joining("\n"));
        }
    }
}
