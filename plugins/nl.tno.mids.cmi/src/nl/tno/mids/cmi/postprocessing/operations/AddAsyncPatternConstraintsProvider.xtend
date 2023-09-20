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

class AddAsyncPatternConstraintsProvider extends PostProcessingOperationProvider<AddAsyncPatternConstraints, AddAsyncPatternConstraintsOptions> {
    override getOperationReadableName() {
        return "Add asynchronous pattern constraints"
    }

    override getOperationDescription() {
        return "Add constraints to the models to enforce asynchronous patterns (e.g. requests/replies)."
    }

    override getOperationClass() {
        return AddAsyncPatternConstraints
    }

    override getOperationOptionsClass() {
        return AddAsyncPatternConstraintsOptions
    }

    override getOperation(AddAsyncPatternConstraintsOptions options) {
        return new AddAsyncPatternConstraints(options)
    }

    override getOptions(String args) {
        Preconditions.checkArgument(args.empty, "Invalid arguments for AddAsyncPatternConstraints operation: " + args)
        return new AddAsyncPatternConstraintsOptions();
    }

    override writeOptions(AddAsyncPatternConstraintsOptions options) {
        return ""
    }

    override supportsFilteredComponentsAsInput() {
        return true
    }
}
