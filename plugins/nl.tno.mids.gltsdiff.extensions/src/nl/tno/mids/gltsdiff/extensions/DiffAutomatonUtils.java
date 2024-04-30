/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.gltsdiff.extensions;

import com.github.tno.gltsdiff.glts.lts.automaton.Automaton;
import com.github.tno.gltsdiff.glts.lts.automaton.AutomatonStateProperty;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffAutomaton;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffKind;
import com.github.tno.gltsdiff.operators.projectors.Projector;
import com.github.tno.gltsdiff.operators.projectors.lts.automaton.diff.DiffAutomatonStatePropertyProjector;
import com.github.tno.gltsdiff.operators.projectors.lts.automaton.diff.DiffKindProjector;
import com.github.tno.gltsdiff.operators.projectors.lts.automaton.diff.DiffPropertyProjector;
import com.google.common.base.Function;

/** Utility methods for working with {@link DiffAutomaton diff automata}. */
public class DiffAutomatonUtils {
    /** Constructor for the {@link DiffAutomatonUtils} class. */
    private DiffAutomatonUtils() {
        // Static class.
    }

    /**
     * Clone a difference automaton.
     * 
     * @param automaton The difference automaton to clone.
     * @return The clone.
     */
    public static <T> DiffAutomaton<T> clone(DiffAutomaton<T> automaton) {
        return automaton.map(DiffAutomaton::new, property -> property, property -> property);
    }

    /**
     * Projects a difference automaton along a given difference kind.
     * 
     * @param automaton The difference automaton to project.
     * @param projector The projector for projecting inner transition properties.
     * @param along The non-{@code null} difference kind to project along.
     * @return The projected difference automaton, containing only state and transition properties related to
     *     {@code along}.
     */
    public static <T> DiffAutomaton<T> project(DiffAutomaton<T> automaton, Projector<T, DiffKind> projector,
            DiffKind along)
    {
        DiffKindProjector diffKindProjector = new DiffKindProjector();
        return automaton.project(DiffAutomaton::new, new DiffAutomatonStatePropertyProjector<>(diffKindProjector),
                new DiffPropertyProjector<>(projector, diffKindProjector), along);
    }

    /**
     * Gives the left (LHS) projection of a difference automaton.
     * 
     * @param automaton The difference automaton to project.
     * @param projector The projector for projecting inner transition properties.
     * @return The left (LHS) projection of this difference automaton, containing only the states, initial states and
     *     transitions that are {@link DiffKind#REMOVED}.
     */
    public static <T> DiffAutomaton<T> projectLeft(DiffAutomaton<T> automaton, Projector<T, DiffKind> projector) {
        return project(automaton, projector, DiffKind.REMOVED);
    }

    /**
     * Gives the right (RHS) projection of a difference automaton.
     * 
     * @param automaton The difference automaton to project.
     * @param projector The projector for projecting inner transition properties.
     * @return The right (RHS) projection of this difference automaton, containing only the states, initial states and
     *     transitions that are {@link DiffKind#ADDED}.
     */
    public static <T> DiffAutomaton<T> projectRight(DiffAutomaton<T> automaton, Projector<T, DiffKind> projector) {
        return project(automaton, projector, DiffKind.ADDED);
    }

    /**
     * Converts a difference automaton to a simple automaton with potentially different transition properties.
     * 
     * @param automaton The difference automaton to convert.
     * @param <U> The target type of transition properties.
     * @param transitionPropertyMapper A function for mapping transition properties. Any transition with a property that
     *     is mapped to {@code null} will not be included in the returned simple automaton.
     * @return The non-{@code null} converted simple automaton.
     */
    public static <T, U> Automaton<U> toAutomaton(DiffAutomaton<T> automaton, Function<T, U> transitionPropertyMapper) {
        return automaton.map(Automaton::new,
                stateProperty -> new AutomatonStateProperty(stateProperty.isInitial(), stateProperty.isAccepting()),
                transitionProperty -> transitionPropertyMapper.apply(transitionProperty.getProperty()));
    }
}
