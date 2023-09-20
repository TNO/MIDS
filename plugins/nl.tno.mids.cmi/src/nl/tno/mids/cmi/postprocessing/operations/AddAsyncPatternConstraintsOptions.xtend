/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.postprocessing.operations

import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider
import org.eclipse.xtend.lib.annotations.Accessors

@Accessors
class AddAsyncPatternConstraintsOptions extends PostProcessingOperationOptions {

    override validate() throws IllegalStateException {
        // No options to validate.
    }

    override toString() {
        val builder = new StringBuilder()
        builder.append(PostProcessingOperationProvider.getOperationFormalName(AddAsyncPatternConstraints))
        builder.append(" ")
        builder.append(super.toString)
        return builder.toString
    }
}
