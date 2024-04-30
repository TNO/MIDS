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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import nl.tno.mids.cmi.postprocessing.operations.RenameComponent;
import nl.tno.mids.cmi.postprocessing.operations.RenameComponentOptions;
import nl.tno.mids.cmi.postprocessing.operations.RenameComponentProvider;

public class RenameComponentUIProvider
        extends PostProcessingOperationUIProvider<RenameComponent, RenameComponentOptions, RenameComponentProvider>
{
    private Text txtOldComponentName;

    private Text txtNewComponentName;

    @Override
    public RenameComponentProvider getOperationProvider() {
        return new RenameComponentProvider();
    }

    @Override
    public void addUI(Composite container, Runnable validate) {
        container.setLayout(createGridLayout(1));

        createLabel(container, "Old component name:", null);
        txtOldComponentName = new Text(container, SWT.BORDER);
        txtOldComponentName.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false, 2, 1));
        txtOldComponentName.addModifyListener(e -> validate());

        createLabel(container, "New component name:", null);
        txtNewComponentName = new Text(container, SWT.BORDER);
        txtNewComponentName.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false, 2, 1));
        txtNewComponentName.addModifyListener(e -> validate());
    }

    @Override
    public void updateUIEnablement() {
        // No UI.
    }

    @Override
    public String validate() {
        if (txtOldComponentName.getText().trim().isEmpty()) {
            return "Enter old component name";
        }
        if (txtNewComponentName.getText().trim().isEmpty()) {
            return "Enter new component name";
        }
        return null;
    }

    @Override
    public Class<RenameComponentOptions> getOptionsClass() {
        return RenameComponentOptions.class;
    }

    @Override
    public RenameComponentOptions createOptions() {
        String oldComponentName = txtOldComponentName.getText().trim();
        String newComponentName = txtNewComponentName.getText().trim();
        return new RenameComponentOptions(oldComponentName, newComponentName);
    }

    @Override
    protected void applyOptionsInternal(RenameComponentOptions options) {
        txtOldComponentName.setText(options.getOldComponentName());
        txtNewComponentName.setText(options.getNewComponentName());
    }
}
