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

class PostProcessingResultSubset {
    /** Whether to indicate that at the end of the operation
     * <ul>
     * <li>data has been added or remains present ({@code true})</li>
     * <li>data has been removed or remains not present ({@code false})</li>
     * <li>any presence of data remains unchanged ({@code null}).</li>
     * </ul>
     */
    package val Boolean data;

    /** Whether to indicate that at the end of the operation
     * <ul>
     * <li>tau events have been added or remain present ({@code true})</li>
     * <li>tau events have been removed or remain not present ({@code false})</li>
     * <li>any presence of tau events remain unchanged ({@code null}).</li>
     * </ul>
     */
    package val Boolean tau;

    new(Boolean data, Boolean tau) {
        this.data = data;
        this.tau = tau;
    }

    /**
     * This method applies the resultSubset to the receiver returning a new instance of {@link PostProcessingStatus}
     * 
     * @param modificationSubset
     *            The modificationSubset to be applied.
     * @return a PostProcessingStatus reflecting the applied result subset.
     */
    def PostProcessingStatus apply(PostProcessingStatus postProcessingStatus) {
        return new PostProcessingStatus(apply(postProcessingStatus.dataIsPresent, data),
            apply(postProcessingStatus.tauIsPresent, tau))
    }

    def private boolean apply(boolean source, Boolean target) {
        if (target === null) {
            return source;
        }
        return target;
    }
}
