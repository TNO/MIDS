/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.common.eclipse.ui.bulkrename;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class BulkRenameDialog extends TitleAreaDialog {
    private final static String TITLE = "Bulk Rename";

    private final static String MESSAGE = "Rename the selected files or folders:";

    private final static String USAGE = "Give the new name and use # as placeholder for the consecutive numbers, e.g.: 'Abc#'";

    private final static int COLUMNS = 2;

    private Text txtTargetName;

    private String targetName;

    private BulkRenamer bulkRenamer;

    public BulkRenameDialog(Shell parentShell, BulkRenamer bulkRenamer) {
        super(parentShell);
        this.bulkRenamer = bulkRenamer;
    }

    @Override
    public void create() {
        super.create();
        setTitle(TITLE);
        setMessage(MESSAGE);
        validate();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite)super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(COLUMNS, false);
        container.setLayout(layout);

        createOutputArea(container);

        return container;
    }

    private void createOutputArea(Composite container) {
        createLabel(container, USAGE, allColumn());
        createLabel(container, "Target file/folder name:", null);
        txtTargetName = new Text(container, SWT.BORDER);
        txtTargetName.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
        txtTargetName.addModifyListener(e -> validate());
    }

    private void validate() {
        String errMsg = bulkRenamer.validate(txtTargetName.getText(), null);
        setErrorMessage(errMsg);
        this.getButton(IDialogConstants.OK_ID).setEnabled(errMsg == null);
    }

    @Override
    protected void okPressed() {
        targetName = txtTargetName.getText();

        // Done.
        super.okPressed();
    }

    public String targetName() {
        return targetName;
    }

    private Label createLabel(Composite container, String txt, GridData layout) {
        Label lbl = new Label(container, SWT.NONE);
        lbl.setText(txt);
        if (layout != null) {
            lbl.setLayoutData(layout);
        }
        return lbl;
    }

    private GridData allColumn() {
        GridData ret = new GridData(SWT.FILL, SWT.TOP, true, false);
        ret.horizontalSpan = COLUMNS;
        return ret;
    }
}
