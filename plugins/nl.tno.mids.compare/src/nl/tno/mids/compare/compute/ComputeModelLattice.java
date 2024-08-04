/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.compute;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import com.google.common.base.Preconditions;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.fsa.DFAs;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.Entity;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.Variant;

/** Compute relations between variants of entities based on language inclusion and encode them in a lattice. */
public class ComputeModelLattice extends ComputeLattice<Model> {
    private final ComparisonData comparisonData;

    /**
     * @param comparisonData Comparison data containing entities with variants to relate.
     */
    public ComputeModelLattice(ComparisonData comparisonData) {
        this.comparisonData = comparisonData;
    }

    /**
     * Compute a variant inclusion lattice for each entity, based on all model variants of that entity.
     * <p>
     * Every arrow in a lattice amounts to language inclusion of the source model in the target model.
     * </p>
     * 
     * @param monitor Progress monitor to track execution.
     */
    public void computeLattices(IProgressMonitor monitor) {
        SubMonitor subMonitor = SubMonitor.convert(monitor, comparisonData.getEntities().size());

        for (Entity entity: comparisonData.getEntities()) {
            subMonitor.split(1);

            entity.getLattice().addAll(computeLattice(entity.getVariants()));
        }
    }

    @Override
    protected boolean inPartialOrder(Variant<Model> variant, Variant<Model> otherVariant) {
        Preconditions.checkArgument(variant != otherVariant);
        Model model = variant.getValue();
        Model otherModel = otherVariant.getValue();

        if (!model.hasBehavior()) {
            return true;
        }

        if (!otherModel.hasBehavior()) {
            return false;
        }

        CompactDFA<String> dfaLeft = model.getLanguageAutomaton();
        CompactDFA<String> dfaRight = otherModel.getLanguageAutomaton();

        return compareLanguageInclusion(dfaLeft, dfaRight);
    }

    /**
     * Compare two DFAs based on language inclusion.
     * 
     * @param dfaLeft DFA to compare.
     * @param dfaRight Other DFA to compare.
     * @return {@code true} if the language of {@code dfaLeft} is included in the language of {@code dfaRight},
     *     {@code false} otherwise.
     */
    public static boolean compareLanguageInclusion(CompactDFA<String> dfaLeft, CompactDFA<String> dfaRight) {
        AutomataLibUtil.synchronizeAlphabets(dfaLeft, dfaRight);

        // Compute DFA describing words not in language of dfaRight, i.e., 'not(dfaRight)'.
        CompactDFA<String> dfaRightComplement = DFAs.complement(dfaRight, dfaRight.getInputAlphabet());

        // Use intersection to determine if the language of dfaLeft contains words not in language of dfaRight, i.e.,
        // 'dfaLeft and not(dfaRight)'.
        CompactDFA<String> intersectionDfa = AutomataLibUtil.intersectionMinimized(dfaLeft, dfaRightComplement);

        // If the intersection is empty, i.e., 'dfaLeft and not(dfaRight) = empty', the language of dfaLeft is contained
        // in dfaRight. Assumes intersection has no unreachable states.
        return DFAs.acceptsEmptyLanguage(intersectionDfa);
    }
}
