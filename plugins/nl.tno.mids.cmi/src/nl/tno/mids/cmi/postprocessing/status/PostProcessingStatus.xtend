/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.postprocessing.status

import java.util.List
import nl.tno.mids.cmi.postprocessing.PostProcessingValidationResult
import org.eclipse.escet.common.java.Lists

class PostProcessingStatus {
    /** Whether the model contains data, e.g. variables, guards and updates ({@code true}) or not ({@code false}). */
    package val boolean data;

    /** Whether the model contains tau events ({@code true}) or not ({@code false}). */
    package val boolean tau ;

    new(boolean data, boolean tau) {
        this.data = data;
        this.tau = tau;
    }

    /**
     * This method validates that the given {@link PostProcessingSubset} can be applied to the receiving {@link PostProcessingStatus}.
     * The method will return a list of messages from {@link PostProcessingValidationResult} when failing. If valid,
     * the method will return an empty list, i.e. no issues.
     * 
     * @param preConditionSubset
     *            The postProcessingSubset to apply to the receiver     
     * @return messages
     *            A list of messages. Empty in case of no issues.
     */
    def List<String> validate(PostProcessingPreconditionSubset preConditionSubset) {
        val messages = Lists.list()

        if (dataIsPresent && preConditionSubset.dataIsNotAllowed) {
            messages.add(PostProcessingValidationResult.DATA_NOT_ALLOWED.message)
        }
        if (dataIsNotPresent && preConditionSubset.dataIsRequired) {
            messages.add(PostProcessingValidationResult.DATA_REQUIRED.message)
        }

        if (tauIsPresent && preConditionSubset.tauIsNotAllowed) {
            messages.add(PostProcessingValidationResult.TAU_NOT_ALLOWED.message)
        }
        if (tauIsNotPresent && preConditionSubset.tauIsRequired) {
            messages.add(PostProcessingValidationResult.TAU_REQUIRED.message)
        }
        return messages
    }

    def dataIsPresent() {
        return data;
    }

    def dataIsNotPresent() {
        return !data;
    }

    def tauIsPresent() {
        return tau;
    }

    def tauIsNotPresent() {
        return !tau;
    }

}
