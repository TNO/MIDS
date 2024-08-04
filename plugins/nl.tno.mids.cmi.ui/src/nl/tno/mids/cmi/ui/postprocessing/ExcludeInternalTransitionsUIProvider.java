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

import nl.tno.mids.cmi.postprocessing.operations.ExcludeInternalTransitions;
import nl.tno.mids.cmi.postprocessing.operations.ExcludeInternalTransitionsOptions;
import nl.tno.mids.cmi.postprocessing.operations.ExcludeInternalTransitionsProvider;

public class ExcludeInternalTransitionsUIProvider extends
        PostProcessingOperationUIProvider<ExcludeInternalTransitions, ExcludeInternalTransitionsOptions, ExcludeInternalTransitionsProvider>
{
    @Override
    public ExcludeInternalTransitionsProvider getOperationProvider() {
        return new ExcludeInternalTransitionsProvider();
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
    public Class<ExcludeInternalTransitionsOptions> getOptionsClass() {
        return ExcludeInternalTransitionsOptions.class;
    }

    @Override
    public ExcludeInternalTransitionsOptions createOptions() {
        return new ExcludeInternalTransitionsOptions();
    }

    @Override
    protected void applyOptionsInternal(ExcludeInternalTransitionsOptions options) {
        // No option arguments.
    }
}
