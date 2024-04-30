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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import nl.tno.mids.cmi.postprocessing.operations.HideActions;
import nl.tno.mids.cmi.postprocessing.operations.HideActionsOptions;
import nl.tno.mids.cmi.postprocessing.operations.HideActionsProvider;

public class HideActionsUIProvider
        extends PostProcessingOperationUIProvider<HideActions, HideActionsOptions, HideActionsProvider>
{
    private Text txtHidePattern;

    @Override
    public HideActionsProvider getOperationProvider() {
        return new HideActionsProvider();
    }

    @Override
    public void addUI(Composite container, Runnable validate) {
        createLabel(container, "Hide pattern:", null);
        txtHidePattern = new Text(container, SWT.BORDER);
        txtHidePattern.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
        txtHidePattern.addModifyListener(e -> validate());
    }

    @Override
    public void updateUIEnablement() {
        // Always enabled.
    }

    @Override
    public String validate() {
        if (txtHidePattern.getText().trim().isEmpty()) {
            return "Enter a pattern to select events to hide";
        }
        try {
            Pattern.compile(txtHidePattern.getText().trim());
        } catch (PatternSyntaxException e) {
            return "Invalid hide regular expression pattern: " + e.toString();
        }
        return null;
    }

    @Override
    public Class<HideActionsOptions> getOptionsClass() {
        return HideActionsOptions.class;
    }

    @Override
    public HideActionsOptions createOptions() {
        HideActionsOptions options = new HideActionsOptions();
        options.setPattern(txtHidePattern.getText().trim());
        return options;
    }

    @Override
    protected void applyOptionsInternal(HideActionsOptions options) {
        txtHidePattern.setText(options.pattern);
    }
}
