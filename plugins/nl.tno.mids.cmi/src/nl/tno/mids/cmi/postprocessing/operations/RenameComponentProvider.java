/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.postprocessing.operations;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Preconditions;

import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;

/**
 * Configuration provider for {@link RenameComponent} operation.
 */
public class RenameComponentProvider extends PostProcessingOperationProvider<RenameComponent, RenameComponentOptions> {
    @Override
    public String getOperationReadableName() {
        return "Rename component";
    }

    @Override
    public String getOperationDescription() {
        return "Renames a specified component.";
    }

    @Override
    public Class<RenameComponent> getOperationClass() {
        return RenameComponent.class;
    }

    @Override
    public Class<RenameComponentOptions> getOperationOptionsClass() {
        return RenameComponentOptions.class;
    }

    @Override
    public RenameComponent getOperation(RenameComponentOptions options) {
        return new RenameComponent(options);
    }

    @Override
    public RenameComponentOptions getOptions(String args) {
        List<String> argList = Arrays.asList(args.split(","));
        Preconditions.checkArgument(argList.size() == 2, "Invalid arguments for RenameComponent operation: " + args);
        return new RenameComponentOptions(argList.get(0).trim(), argList.get(1).trim());
    }

    public String writeOptions(RenameComponentOptions options) {
        return options.getOldComponentName() + "," + options.getNewComponentName();
    }

    @Override
    public boolean supportsFilteredComponentsAsInput() {
        return false;
    }
}
