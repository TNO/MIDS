/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.ui.postprocessing;

import org.eclipse.swt.widgets.Composite;

import nl.tno.mids.cmi.postprocessing.operations.PrefixClose;
import nl.tno.mids.cmi.postprocessing.operations.PrefixCloseOptions;
import nl.tno.mids.cmi.postprocessing.operations.PrefixCloseProvider;

public class PrefixCloseUIProvider
        extends PostProcessingOperationUIProvider<PrefixClose, PrefixCloseOptions, PrefixCloseProvider>
{
    @Override
    public PrefixCloseProvider getOperationProvider() {
        return new PrefixCloseProvider();
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
    public Class<PrefixCloseOptions> getOptionsClass() {
        return PrefixCloseOptions.class;
    }

    @Override
    public PrefixCloseOptions createOptions() {
        return new PrefixCloseOptions();
    }

    @Override
    protected void applyOptionsInternal(PrefixCloseOptions options) {
        // No option arguments.
    }
}
