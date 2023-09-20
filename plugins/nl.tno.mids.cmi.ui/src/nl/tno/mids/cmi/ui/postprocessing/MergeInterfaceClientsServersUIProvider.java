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
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import nl.tno.mids.cmi.postprocessing.operations.MergeInterfaceClientsServers;
import nl.tno.mids.cmi.postprocessing.operations.MergeInterfaceClientsServersOptions;
import nl.tno.mids.cmi.postprocessing.operations.MergeInterfaceClientsServersProvider;

public class MergeInterfaceClientsServersUIProvider extends
        PostProcessingOperationUIProvider<MergeInterfaceClientsServers, MergeInterfaceClientsServersOptions, MergeInterfaceClientsServersProvider>
{
    private Button btnMergeClients;

    private Button btnMergeServers;

    private Text txtMergeInterface;

    @Override
    public MergeInterfaceClientsServersProvider getOperationProvider() {
        return new MergeInterfaceClientsServersProvider();
    }

    @Override
    public void addUI(Composite container, Runnable validate) {
        container.setLayout(createGridLayout(1));

        btnMergeClients = new Button(container, SWT.CHECK);
        btnMergeClients.setText("Merge interface clients");
        btnMergeClients.setSelection(true);
        btnMergeClients.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> validate()));

        btnMergeServers = new Button(container, SWT.CHECK);
        btnMergeServers.setText("Merge interface servers");
        btnMergeServers.setSelection(true);
        btnMergeServers.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> validate()));

        createLabel(container, "Interface name to merge (empty for all interfaces):", null);
        txtMergeInterface = new Text(container, SWT.BORDER);
        txtMergeInterface.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
    }

    @Override
    public void updateUIEnablement() {
        // No UI.
    }

    @Override
    public String validate() {
        if (!btnMergeClients.getSelection() && !btnMergeServers.getSelection()) {
            return "Current settings don't impose any modifications";
        } else {
            return null;
        }
    }

    @Override
    public Class<MergeInterfaceClientsServersOptions> getOptionsClass() {
        return MergeInterfaceClientsServersOptions.class;
    }

    @Override
    public MergeInterfaceClientsServersOptions createOptions() {
        MergeInterfaceClientsServersOptions options = new MergeInterfaceClientsServersOptions();
        options.mergeClients = btnMergeClients.getSelection();
        options.mergeServers = btnMergeServers.getSelection();
        options.mergeInterface = txtMergeInterface.getText();
        return options;
    }

    @Override
    protected void applyOptionsInternal(MergeInterfaceClientsServersOptions options) {
        btnMergeClients.setSelection(options.mergeClients);
        btnMergeServers.setSelection(options.mergeServers);
        txtMergeInterface.setText(options.mergeInterface);
    }
}
