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

import nl.tno.mids.cmi.postprocessing.operations.RenameFunctions;
import nl.tno.mids.cmi.postprocessing.operations.RenameFunctionsOptions;
import nl.tno.mids.cmi.postprocessing.operations.RenameFunctionsProvider;

public class RenameFunctionsUIProvider
        extends PostProcessingOperationUIProvider<RenameFunctions, RenameFunctionsOptions, RenameFunctionsProvider>
{
    private Text txtFunctionMappings;

    @Override
    public RenameFunctionsProvider getOperationProvider() {
        return new RenameFunctionsProvider();
    }

    @Override
    public void addUI(Composite container, Runnable validate) {
        container.setLayout(createGridLayout(1));

        createLabel(container, "Function mappings:", null);
        txtFunctionMappings = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        txtFunctionMappings.setLayoutData(new GridData(GridData.FILL_BOTH));
        txtFunctionMappings.addModifyListener(e -> validate());
    }

    @Override
    public void updateUIEnablement() {
        // No UI.
    }

    @Override
    public String validate() {
        String functionMappingsString = txtFunctionMappings.getText().trim();
        if (functionMappingsString.isEmpty()) {
            return "Enter at least one function mapping";
        }
        RenameFunctionsOptions options = new RenameFunctionsOptions(functionMappingsString);
        if (options.isFoundNotMatching()) {
            return "One of the function mappings does not match the expected pattern";
        }
        if (options.isFoundDuplicate()) {
            return "Multiple mappings for the same function are not allowed";
        }
        return null;
    }

    @Override
    public Class<RenameFunctionsOptions> getOptionsClass() {
        return RenameFunctionsOptions.class;
    }

    @Override
    public RenameFunctionsOptions createOptions() {
        String functionMappingsString = txtFunctionMappings.getText().trim();
        return new RenameFunctionsOptions(functionMappingsString);
    }

    @Override
    protected void applyOptionsInternal(RenameFunctionsOptions options) {
        String functionMappingsString = options.mappingToString("\n");
        txtFunctionMappings.setText(functionMappingsString);
    }
}
