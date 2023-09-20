/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.postprocessing.status

import nl.tno.mids.cmi.postprocessing.PostProcessingModel

class PostProcessingPreconditionSubset {
    /**  Whether
     * <ul>
     * <li>to require that data need to be present ({@code true})</li>
     * <li> to require that no data is present ({@code false})</li>
     * <li> data may or may not be present ({@code null}).</li>
     * </ul>
     */
    package  val Boolean data;

    /** Whether
     * <ul>
     * <li>to require that tau events need to be present ({@code true})</li>
     * <li> to require no tau events are present ({@code false})</li>
     * <li> tau events may or may not be present ({@code null}).</li>
     * </ul>
     */
    package  val Boolean tau;

    new(Boolean data, Boolean tau) {
        this.data = data;
        this.tau = tau;
    }

    def boolean dataIsNotAllowed() {
        return Boolean.FALSE.equals(data)
    }

    def boolean dataIsRequired() {
        return Boolean.TRUE.equals(data)
    }

    def boolean dataIsOptional() {
        return data === null
    }

    def boolean tauIsNotAllowed() {
        return Boolean.FALSE.equals(tau)
    }

    def boolean tauIsRequired() {
        return Boolean.TRUE.equals(tau)
    }

    def boolean tauIsOptional() {
        return tau === null
    }

    /**
     * This method applies the postprocessing status to the receiver returning a (new) instance of {@link PostProcessingStatus}
     * 
     * @param postProcessingStatus
     *            The PostProcessingStatus to apply this precondition subset on.
     * @return a PostProcessingStatus reflecting the ensured precondition subset.
     */
    def PostProcessingStatus apply(PostProcessingStatus postProcessingStatus) {
        return EnsureSubset.ensureSubset(postProcessingStatus, this)
    }

    /**
     * This method applies the postprocessing status to the receiver returning a (new) instance of {@link PostProcessingStatus}
     * 
     * @param postProcessingStatus
     *            The PostProcessingStatus to apply this precondition subset on.
     * @return a PostProcessingStatus reflecting the ensured precondition subset.
     */
    def PostProcessingModel ensureSubset(PostProcessingModel postProcessingModel) {
        return EnsureSubset.ensureSubset(postProcessingModel, this)
    }
}
