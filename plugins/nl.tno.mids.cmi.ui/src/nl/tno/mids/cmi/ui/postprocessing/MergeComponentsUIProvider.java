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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import nl.tno.mids.cmi.postprocessing.operations.MergeComponents;
import nl.tno.mids.cmi.postprocessing.operations.MergeComponentsOptions;
import nl.tno.mids.cmi.postprocessing.operations.MergeComponentsProvider;

public class MergeComponentsUIProvider
        extends PostProcessingOperationUIProvider<MergeComponents, MergeComponentsOptions, MergeComponentsProvider>
{
    private Text txtMergeComponentsPattern;

    @Override
    public MergeComponentsProvider getOperationProvider() {
        return new MergeComponentsProvider();
    }

    @Override
    public void addUI(Composite container, Runnable validate) {
        createLabel(container, "Merge components pattern:", null);
        txtMergeComponentsPattern = new Text(container, SWT.BORDER);
        txtMergeComponentsPattern.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
        txtMergeComponentsPattern.addModifyListener(e -> validate());
    }

    @Override
    public void updateUIEnablement() {
        // Always enabled.
    }

    @Override
    public String validate() {
        if (txtMergeComponentsPattern.getText().trim().isEmpty()) {
            return "Enter a pattern to select components to merge";
        }
        try {
            Pattern.compile(txtMergeComponentsPattern.getText().trim());
        } catch (PatternSyntaxException e) {
            return "Invalid regular expression pattern for components to merge: " + e.toString();
        }
        return null;
    }

    @Override
    public Class<MergeComponentsOptions> getOptionsClass() {
        return MergeComponentsOptions.class;
    }

    @Override
    public MergeComponentsOptions createOptions() {
        MergeComponentsOptions options = new MergeComponentsOptions();
        options.setPattern(txtMergeComponentsPattern.getText().trim());
        return options;
    }

    @Override
    protected void applyOptionsInternal(MergeComponentsOptions options) {
        txtMergeComponentsPattern.setText(options.pattern);
    }
}
