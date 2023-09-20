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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import nl.tno.mids.cmi.postprocessing.operations.FilterClientServerInteractions;
import nl.tno.mids.cmi.postprocessing.operations.FilterClientServerInteractionsOptions;
import nl.tno.mids.cmi.postprocessing.operations.FilterClientServerInteractionsProvider;

public class FilterClientServerInteractionsUIProvider extends
        PostProcessingOperationUIProvider<FilterClientServerInteractions, FilterClientServerInteractionsOptions, FilterClientServerInteractionsProvider>
{
    private Text txtComponent1;

    private Text txtComponent2;

    @Override
    public FilterClientServerInteractionsProvider getOperationProvider() {
        return new FilterClientServerInteractionsProvider();
    }

    @Override
    public void addUI(Composite container, Runnable validate) {
        createLabel(container, "First component:", null);
        txtComponent1 = new Text(container, SWT.BORDER);
        txtComponent1.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false, 2, 1));
        txtComponent1.addModifyListener(e -> validate());

        createLabel(container, "Second component:", null);
        txtComponent2 = new Text(container, SWT.BORDER);
        txtComponent2.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false, 2, 1));
        txtComponent2.addModifyListener(e -> validate());
    }

    @Override
    public void updateUIEnablement() {
        // Always enabled.
    }

    @Override
    public String validate() {
        if (txtComponent1.getText().trim().isEmpty()) {
            return "Enter first component name";
        }
        if (txtComponent2.getText().trim().isEmpty()) {
            return "Enter second component name";
        }
        return null;
    }

    @Override
    public Class<FilterClientServerInteractionsOptions> getOptionsClass() {
        return FilterClientServerInteractionsOptions.class;
    }

    @Override
    public FilterClientServerInteractionsOptions createOptions() {
        FilterClientServerInteractionsOptions options = new FilterClientServerInteractionsOptions();
        options.setComponentName1(txtComponent1.getText().trim());
        options.setComponentName2(txtComponent2.getText().trim());
        return options;
    }

    @Override
    protected void applyOptionsInternal(FilterClientServerInteractionsOptions options) {
        txtComponent1.setText(options.getComponentName1());
        txtComponent2.setText(options.getComponentName2());
    }
}
