/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.postprocessing.status;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.cmi.postprocessing.PostProcessingModel;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCompactDfa;

/**
 * Functionality to remove tau edges during post-processing.
 */
public class EnsureNoTau {
    /**
     * Remove tau from a given {@link PostProcessingModel} if present.
     * 
     * @param postProcessingModel {@link PostProcessingModel} to process.
     * @return a {@link PostProcessingModel} weak trace equivalent to the input. As part of the processing, data is also
     *     removed from the model.
     */
    public static PostProcessingModel ensureNoTau(PostProcessingModel postProcessingModel) {
        if (postProcessingModel.status.tauIsNotPresent()) {
            return postProcessingModel;
        }

        // Removing tau events only possible if no data.
        if (postProcessingModel.status.dataIsPresent()) {
            return ensureNoTau(EnsureNoData.ensureNoData(postProcessingModel));
        }

        // Eliminate tau events by using weak-trace normalization.
        CompactDFA<String> dfa = postProcessingModel.getCompactDfa();
        if (dfa.getInputAlphabet().contains("tau")) {
            // Eliminate tau.
            CompactDFA<String> result = AutomataLibUtil.normalizeWeakTrace(dfa);
            return new PostProcessingModelCompactDfa(result, postProcessingModel.name,
                    ensureNoTau(postProcessingModel.status));
        } else {
            // Already no tau.
            return postProcessingModel.recategorizeAsNoTau();
        }
    }

    /**
     * Remove tau from a given {@link PostProcessingStatus} if present.
     * 
     * @param postProcessingStatus {@link PostProcessingStatus} to process.
     * @return a {@link PostProcessingStatus} without tau and without data if the input had tau, data as input
     *     otherwise.
     */
    public static PostProcessingStatus ensureNoTau(PostProcessingStatus postProcessingStatus) {
        if (postProcessingStatus.tauIsNotPresent()) {
            return postProcessingStatus;
        }

        // Removing tau events only possible if no data.
        if (postProcessingStatus.dataIsPresent()) {
            return ensureNoTau(EnsureNoData.ensureNoData(postProcessingStatus));
        }

        return new PostProcessingStatus(postProcessingStatus.dataIsPresent(), false);
    }
}
