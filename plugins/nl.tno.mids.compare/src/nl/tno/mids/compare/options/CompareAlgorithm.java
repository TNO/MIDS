/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.options;

import com.github.tno.gltsdiff.glts.lts.BaseLTS;
import com.github.tno.gltsdiff.glts.lts.LTSStateProperty;
import com.github.tno.gltsdiff.matchers.KuhnMunkresMatcher;
import com.github.tno.gltsdiff.matchers.Matcher;
import com.github.tno.gltsdiff.matchers.lts.BruteForceLTSMatcher;
import com.github.tno.gltsdiff.matchers.lts.DynamicLTSMatcher;
import com.github.tno.gltsdiff.matchers.lts.WalkinshawLTSMatcher;
import com.github.tno.gltsdiff.operators.combiners.Combiner;
import com.github.tno.gltsdiff.scorers.lts.DynamicLTSScorer;
import com.github.tno.gltsdiff.scorers.lts.WalkinshawGlobalLTSScorer;
import com.github.tno.gltsdiff.scorers.lts.WalkinshawLocalLTSScorer;

/** Compare algorithm used to compute differences between models. */
public enum CompareAlgorithm {
    /**
     * Indicates the use of a lightweight structural comparison algorithm, which has good performance but gives
     * sub-optimal results.
     */
    LIGHTWEIGHT("Lightweight"),

    /**
     * Indicates the use of a heavyweight structural comparison algorithm, which has sub-optimal performance but gives
     * good results.
     */
    HEAVYWEIGHT("Heavyweight"),

    /**
     * Indicates the use of a structural comparison algorithm that dynamically determines whether to use a lightweight
     * or heavyweight comparison algorithm, based on how big the input automata are.
     */
    DYNAMIC("Dynamic"),

    /**
     * Indicates the use of Walkinshaw global scoring and Walkinshaw matching. Good for reproducing the algorithms as
     * presented in the paper: N. Walkinshaw and K. Bogdanov, "Automated Comparison of State-Based Software Models in
     * terms of their Language and Structure", TOSEM, vol. 22, no. 2, 2013.
     */
    WALKINSHAW("Walkinshaw"),

    /**
     * Indicates the use of the brute force comparison algorithm, which gives optimal results but is also much more
     * computationally intensive (in the worst case) than the heavyweight algorithm.
     */
    BRUTEFORCE("Brute force");

    private final String description;

    private CompareAlgorithm(String description) {
        this.description = description;
    }

    /**
     * @return A {@link String} description of this choice of comparison algorithm.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Instantiates a corresponding matching algorithm.
     * 
     * @param lhs The LHS automaton involved in the comparison.
     * @param rhs The RHS automaton involved in the comparison.
     * @param statePropertyCombiner The combiner for state properties.
     * @param transitionPropertyCombiner The combiner for transition properties.
     * @return A matching algorithm that corresponds to the current choice.
     * @throws IllegalArgumentException In case the comparison algorithm choice is unknown.
     */
    public <S extends LTSStateProperty, T, U extends BaseLTS<S, T>> Matcher<S, T, U> getMatcher(
            Combiner<S> statePropertyCombiner, Combiner<T> transitionPropertyCombiner) throws IllegalArgumentException
    {
        switch (this) {
            case DYNAMIC:
                return new DynamicLTSMatcher<>(statePropertyCombiner, transitionPropertyCombiner,
                        new DynamicLTSScorer<>(statePropertyCombiner, transitionPropertyCombiner));
            case HEAVYWEIGHT:
                return new KuhnMunkresMatcher<>(
                        new WalkinshawGlobalLTSScorer<>(statePropertyCombiner, transitionPropertyCombiner),
                        statePropertyCombiner);
            case LIGHTWEIGHT:
                return new WalkinshawLTSMatcher<>(
                        new WalkinshawLocalLTSScorer<>(statePropertyCombiner, transitionPropertyCombiner),
                        statePropertyCombiner, transitionPropertyCombiner);
            case WALKINSHAW:
                return new WalkinshawLTSMatcher<>(
                        new WalkinshawGlobalLTSScorer<>(statePropertyCombiner, transitionPropertyCombiner),
                        statePropertyCombiner, transitionPropertyCombiner);
            case BRUTEFORCE:
                return new BruteForceLTSMatcher<>(statePropertyCombiner, transitionPropertyCombiner);
            default:
                throw new IllegalArgumentException("Unknown case.");
        }
    }
}
