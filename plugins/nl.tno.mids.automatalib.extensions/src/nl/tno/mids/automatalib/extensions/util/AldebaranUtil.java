/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.automatalib.extensions.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

import net.automatalib.SupportsGrowingAlphabet;
import net.automatalib.automata.Automaton;
import net.automatalib.automata.MutableAutomaton;
import net.automatalib.automata.concepts.InputAlphabetHolder;
import net.automatalib.automata.concepts.StateIDs;
import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.automata.graphs.TransitionEdge;
import net.automatalib.graphs.Graph;
import net.automatalib.graphs.concepts.NodeIDs;
import net.automatalib.visualization.VisualizationHelper;
import net.automatalib.visualization.VisualizationHelper.CommonAttrs;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.GrowingMapAlphabet;

/**
 * Utilities related to Aldebaran format.
 */
public class AldebaranUtil {
    // ----------
    // Aldebaran LTS file format
    // See
    // https://www.mcrl2.org/web/user_manual/language_reference/lts.html#aldebaran-format
    // See http://cadp.inria.fr/man/aldebaran.html
    // ----------
    private static final Pattern HEADER_PATTERN = Pattern.compile("des \\((\\d+),(\\d+),(\\d+)\\)");

    private static final Pattern EDGE_PATTERN = Pattern.compile("\\((\\d+),\"([^\"]*)\",(\\d+)\\)");

    /**
     * Read an Aldebaran LTS as {@link CompactDFA}.
     *
     * <ul>
     * <li>Each LTS label is considered an input for the DFA.</li>
     * <li>All states will be marked as accepting states.</li>
     * </ul>
     *
     * @param stream Stream to read. No need to provide a buffered stream.
     * @return The DFA.
     * @throws IOException In case of an I/O error.
     */
    public static CompactDFA<String> readAldebaranCompactDfa(InputStream stream) throws IOException {
        Alphabet<String> alphabet = new GrowingMapAlphabet<>();
        CompactDFA<String> dfa = new CompactDFA<>(alphabet);
        return readAldebaranFsa(dfa, stream);
    }

    /**
     * Read an Aldebaran LTS as {@link CompactNFA}.
     *
     * <ul>
     * <li>Each LTS label is considered an input for the NFA.</li>
     * <li>All states will be marked as accepting states.</li>
     * </ul>
     *
     * @param stream Stream to read. No need to provide a buffered stream.
     * @return The NFA.
     * @throws IOException In case of an I/O error.
     */
    public static CompactNFA<String> readAldebaranCompactNfa(InputStream stream) throws IOException {
        Alphabet<String> alphabet = new GrowingMapAlphabet<>();
        CompactNFA<String> nfa = new CompactNFA<>(alphabet);
        return readAldebaranFsa(nfa, stream);
    }

    /**
     * Read an Aldebaran LTS as {@link FastDFA}.
     *
     * <ul>
     * <li>Each LTS label is considered an input for the DFA.</li>
     * <li>All states will be marked as accepting states.</li>
     * </ul>
     *
     * @param stream Stream to read. No need to provide a buffered stream.
     * @return The DFA.
     * @throws IOException In case of an I/O error.
     */
    public static FastDFA<String> readAldebaranFastDfa(InputStream stream) throws IOException {
        Alphabet<String> alphabet = new GrowingMapAlphabet<>();
        FastDFA<String> dfa = new FastDFA<>(alphabet);
        return readAldebaranFsa(dfa, stream);
    }

    /**
     * Read an Aldebaran LTS as {@link FastNFA}.
     *
     * <ul>
     * <li>Each LTS label is considered an input for the NFA.</li>
     * <li>All states will be marked as accepting states.</li>
     * </ul>
     *
     * @param stream Stream to read. No need to provide a buffered stream.
     * @return The NFA.
     * @throws IOException In case of an I/O error.
     */
    public static FastNFA<String> readAldebaranFastNfa(InputStream stream) throws IOException {
        Alphabet<String> alphabet = new GrowingMapAlphabet<>();
        FastNFA<String> nfa = new FastNFA<>(alphabet);
        return readAldebaranFsa(nfa, stream);
    }

    /**
     * Read an Aldebaran LTS as finite state automaton (FSA), e.g. DFA or NFA.
     *
     * <ul>
     * <li>Each LTS label is considered an input for the FSA.</li>
     * <li>All states will be marked as accepting states.</li>
     * </ul>
     *
     * @param stream Stream to read. No need to provide a buffered stream.
     * @param fsa The empty FSA into which to read the Aldebaran LTS.
     * @return The FSA.
     * @throws IOException In case of an I/O error.
     */
    public static <S, T, TP,
            M extends MutableAutomaton<S, String, T, Boolean, TP> & SupportsGrowingAlphabet<String> & StateIDs<S>> M
            readAldebaranFsa(M fsa, InputStream stream) throws IOException
    {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line = reader.readLine();
            line = line.trim(); // mCRL2 mcrl2-201808.0.3e86b2370d nightly produces whitespace after the header.
            Matcher matcher = HEADER_PATTERN.matcher(line);

            if (!matcher.matches()) {
                throw new IOException("Invalid header line: '" + line + "'.");
            }

            int firstState = Integer.valueOf(matcher.group(1));
            int nrOfTransitions = Integer.valueOf(matcher.group(2));
            int nrOfStates = Integer.valueOf(matcher.group(3));

            for (int i = 0; i < nrOfStates; i++) {
                S state;
                if (i == firstState) {
                    state = fsa.addInitialState(true);
                } else {
                    state = fsa.addState(true);
                }
                Preconditions.checkState(fsa.getStateId(state) == i);
            }

            int readTransitions = 0;
            while ((line = reader.readLine()) != null) {
                Matcher edgeMatcher = EDGE_PATTERN.matcher(line);
                if (!edgeMatcher.matches()) {
                    throw new IOException("Invalid edge line: '" + line + "'.");
                }

                int fromStateId = Integer.valueOf(edgeMatcher.group(1));
                String label = edgeMatcher.group(2);
                int toStateId = Integer.valueOf(edgeMatcher.group(3));

                fsa.addAlphabetSymbol(label);
                S fromState = fsa.getState(fromStateId);
                S toState = fsa.getState(toStateId);
                fsa.addTransition(fromState, label, toState, null);

                readTransitions++;
            }

            if (readTransitions != nrOfTransitions) {
                throw new IOException(
                        "Read " + readTransitions + " transitions, but expected " + nrOfTransitions + ".");
            }
        }

        return fsa;
    }

    /**
     * Write an Aldebaran LTS.
     *
     * <p>
     * <strong>WARNING</strong>: The Aldebaran format doesn't distinguish between accepting and non-accepting states.
     * Therefore, be careful when writing a finite state automaton (FSA), e.g. a DFA or an NFA. The FSA should only have
     * accepting states. This is <strong>NOT</strong> checked by this method!
     * </p>
     *
     * @param automaton The automaton. For instance a {@link CompactDFA} or {@link FastDFA}.
     * @param stream Stream to write. No need to provide a buffered stream.
     * @throws IllegalArgumentException If the automaton does not have exactly one initial state.
     * @throws IOException In case of an I/O error.
     */
    public static <S, T, A extends Automaton<S, String, T> & InputAlphabetHolder<String>> void
            writeAldebaran(A automaton, OutputStream stream) throws IOException
    {
        // Get graph view of automaton.
        Graph<S, TransitionEdge<String, T>> graph = automaton.transitionGraphView(automaton.getInputAlphabet());

        // Get node ID mapper. Expensive operation for certain automata representation, so reuse this.
        NodeIDs<S> nodeIds = graph.nodeIDs();

        // Get single initial state.
        if (automaton.getInitialStates().size() != 1) {
            throw new IllegalArgumentException("Expected automaton with one initial state, found "
                    + automaton.getInitialStates().size() + " initial states.");
        }
        S initialState = automaton.getInitialStates().iterator().next();
        int initialStateId = nodeIds.getNodeId(initialState);
        if (initialStateId < 0) {
            throw new IllegalArgumentException("Provided automaton does not have an initial state.");
        }

        // Get number of states and transitions.
        // Also check that states are numbered from zero and increasing without gaps.
        int nrOfStates = automaton.getStates().size();
        int nrOfTransitions = 0;
        int curNodeId = 0;
        for (S node: graph.getNodes()) {
            int nodeId = nodeIds.getNodeId(node);
            Preconditions.checkState(nodeId == curNodeId);
            curNodeId++;
            Collection<TransitionEdge<String, T>> outgoingEdges = graph.getOutgoingEdges(node);
            if (outgoingEdges != null) {
                nrOfTransitions += outgoingEdges.size();
            }
        }

        // Get visualization helper.
        VisualizationHelper<S, TransitionEdge<String, T>> visualizationHelper = graph.getVisualizationHelper();

        // Write Aldebaran format.
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(stream))) {
            // Write header.
            writer.write("des (");
            writer.write(Integer.toString(initialStateId));
            writer.write(",");
            writer.write(Integer.toString(nrOfTransitions));
            writer.write(",");
            writer.write(Integer.toString(nrOfStates));
            writer.write(")");
            writer.write("\n");

            // Write transitions.
            Map<String, String> properties = new HashMap<>();
            for (S sourceNode: graph.getNodes()) {
                Collection<TransitionEdge<String, T>> outgoingEdges = graph.getOutgoingEdges(sourceNode);
                if (outgoingEdges == null) {
                    continue;
                }
                for (TransitionEdge<String, T> transition: outgoingEdges) {
                    // Get source and target node IDs.
                    S targetNode = graph.getTarget(transition);
                    int sourceId = nodeIds.getNodeId(sourceNode);
                    int targetId = nodeIds.getNodeId(targetNode);

                    // Get label.
                    properties.clear();
                    boolean render = visualizationHelper.getEdgeProperties(sourceNode, transition, targetNode,
                            properties);
                    Preconditions.checkState(render);
                    String label = properties.get(CommonAttrs.LABEL);
                    label = label.replace("\"", "\\\"");

                    // Write transition.
                    writer.write("(");
                    writer.write(Integer.toString(sourceId));
                    writer.write(",\"");
                    writer.write(label);
                    writer.write("\",");
                    writer.write(Integer.toString(targetId));
                    writer.write(")\n");
                }
            }
        }
    }
}
