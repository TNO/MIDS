/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.ui.postprocessing;

import org.eclipse.swt.widgets.Composite;

import nl.tno.mids.cmi.postprocessing.operations.AddAsyncPatternConstraints;
import nl.tno.mids.cmi.postprocessing.operations.AddAsyncPatternConstraintsOptions;
import nl.tno.mids.cmi.postprocessing.operations.AddAsyncPatternConstraintsProvider;

public class AddAsyncPatternConstraintsUIProvider extends
        PostProcessingOperationUIProvider<AddAsyncPatternConstraints, AddAsyncPatternConstraintsOptions, AddAsyncPatternConstraintsProvider>
{
    @Override
    public AddAsyncPatternConstraintsProvider getOperationProvider() {
        return new AddAsyncPatternConstraintsProvider();
    }

    @Override
    public void addUI(Composite container, Runnable validate) {
        // No UI.
    }

    @Override
    public void updateUIEnablement() {
        // No UI.
    }

    @Override
    public String validate() {
        return null;
    }

    @Override
    public Class<AddAsyncPatternConstraintsOptions> getOptionsClass() {
        return AddAsyncPatternConstraintsOptions.class;
    }

    @Override
    public AddAsyncPatternConstraintsOptions createOptions() {
        return new AddAsyncPatternConstraintsOptions();
    }

    @Override
    protected void applyOptionsInternal(AddAsyncPatternConstraintsOptions options) {
        // No option arguments.
    }
}
