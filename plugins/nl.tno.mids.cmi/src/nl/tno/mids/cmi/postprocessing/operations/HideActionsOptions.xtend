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
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider
import org.eclipse.xtend.lib.annotations.Accessors

@Accessors
class HideActionsOptions extends PostProcessingOperationOptions {

    /** Regular expression pattern to use to match action names. Matching actions will be replaced by 'tau'. */
    public String pattern;

    override validate() throws IllegalStateException {
        Preconditions.checkState(pattern !== null)
        Preconditions.checkState(pattern.trim.length > 0)
        try {
            Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            throw new IllegalStateException("Invalid pattern for HideActions operation: " + pattern, e);
        }
    }

    override toString() {
        val builder = new StringBuilder()
        builder.append(PostProcessingOperationProvider.getOperationFormalName(HideActions))
        builder.append(" ")
        builder.append(pattern);
        builder.append(" ")
        builder.append(super.toString)
        return builder.toString
    }
}
