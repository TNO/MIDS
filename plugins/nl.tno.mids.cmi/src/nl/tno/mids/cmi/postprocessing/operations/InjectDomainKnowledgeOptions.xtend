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

import java.util.Locale
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider
import org.eclipse.xtend.lib.annotations.Accessors

@Accessors
class InjectDomainKnowledgeOptions extends PostProcessingOperationOptions {
    /** The path to the model to inject. */
    public String modelPath

    /** The operator to use for the injection. */
    public InjectDomainKnowledgeOperator operator

    override validate() throws IllegalStateException {
        // No options to validate.
    }

    override toString() {
        val builder = new StringBuilder()
        builder.append(PostProcessingOperationProvider.getOperationFormalName(InjectDomainKnowledge))
        builder.append(" ")
        builder.append(operator.name.toLowerCase(Locale.US))
        builder.append(" ")
        builder.append(modelPath)
        builder.append(" ")
        builder.append(super.toString)
        return builder.toString
    }
}
