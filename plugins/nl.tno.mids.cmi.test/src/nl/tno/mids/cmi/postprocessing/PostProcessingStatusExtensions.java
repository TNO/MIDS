/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.postprocessing;

import org.eclipse.escet.cif.metamodel.cif.Specification;

import nl.tno.mids.cmi.api.general.CmiGeneralDataQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus;

/**
 * Extensions providing {@link PostProcessingStatus} for specifications.
 */
public class PostProcessingStatusExtensions {
    /**
     * Determine the post processing status of the given specification.
     * 
     * @param cifSpec Given specification.
     * @return {@link PostProcessingStatus} of specification.
     */
    public static PostProcessingStatus getPostProcessingStatus(Specification cifSpec) {
        boolean data = CmiGeneralDataQueries.hasData(cifSpec);
        boolean tau = CmiGeneralEventQueries.hasTau(cifSpec);
        return new PostProcessingStatus(data, tau);
    }
}
