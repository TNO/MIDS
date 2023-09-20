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

import com.google.common.base.Preconditions
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider

class MergeComponentsProvider extends PostProcessingOperationProvider<MergeComponents, MergeComponentsOptions> {
    override getOperationReadableName() {
        return "Merge components"
    }

    override getOperationDescription() {
        return "Merge multiple runtime components, for instance multiple instances of the same executable, into a single runtime component."
    }

    override getOperationClass() {
        return MergeComponents
    }

    override getOperationOptionsClass() {
        return MergeComponentsOptions
    }

    override getOperation(MergeComponentsOptions options) {
        return new MergeComponents(options)
    }

    override getOptions(String args) {
        Preconditions.checkArgument(!args.trim.empty, "Invalid arguments for MergeComponents operation: " + args)
        val options = new MergeComponentsOptions
        options.pattern = args.trim
        return options
    }

    override writeOptions(MergeComponentsOptions options) {
        return options.pattern
    }

    override supportsFilteredComponentsAsInput() {
        return true
    }

}
