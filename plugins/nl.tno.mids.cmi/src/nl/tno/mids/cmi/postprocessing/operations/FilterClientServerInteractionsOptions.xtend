/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.postprocessing.operations

import com.google.common.base.Preconditions
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider
import org.eclipse.xtend.lib.annotations.Accessors

@Accessors
class FilterClientServerInteractionsOptions extends PostProcessingOperationOptions {
    String componentName1 = "";
    String componentName2 = "";

    override validate() throws IllegalStateException {
        Preconditions.checkArgument(!componentName1.empty)
        Preconditions.checkArgument(!componentName2.empty)
    }

    override toString() {
        val builder = new StringBuilder()
        builder.append(PostProcessingOperationProvider.getOperationFormalName(FilterClientServerInteractions))
        builder.append(" ")
        builder.append(componentName1)
        builder.append(" ")
        builder.append(componentName2)
        builder.append(" ")
        builder.append(super.toString)
        return builder.toString
    }
}
