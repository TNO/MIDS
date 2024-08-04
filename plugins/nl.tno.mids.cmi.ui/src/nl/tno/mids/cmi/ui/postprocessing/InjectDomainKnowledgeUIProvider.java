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

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;

import com.google.common.base.Preconditions;

import nl.tno.mids.cmi.postprocessing.operations.InjectDomainKnowledge;
import nl.tno.mids.cmi.postprocessing.operations.InjectDomainKnowledgeOperator;
import nl.tno.mids.cmi.postprocessing.operations.InjectDomainKnowledgeOptions;
import nl.tno.mids.cmi.postprocessing.operations.InjectDomainKnowledgeProvider;

public class InjectDomainKnowledgeUIProvider extends
        PostProcessingOperationUIProvider<InjectDomainKnowledge, InjectDomainKnowledgeOptions, InjectDomainKnowledgeProvider>
{
    private Text txtInjectModelPath;

    private Combo cmbInjectOperator;

    private Button btnInjectBrowseModelPath;

    @Override
    public InjectDomainKnowledgeProvider getOperationProvider() {
        return new InjectDomainKnowledgeProvider();
    }

    @Override
    public void addUI(Composite container, Runnable validate) {
        createLabel(container, "Path to model to inject:", null);
        txtInjectModelPath = new Text(container, SWT.BORDER);
        txtInjectModelPath.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
        txtInjectModelPath.addModifyListener(e -> validate.run());

        btnInjectBrowseModelPath = new Button(container, SWT.NONE);
        btnInjectBrowseModelPath.setText("Browse...");
        btnInjectBrowseModelPath.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog browse = new FileDialog(container.getShell());
                browse.setText("Select domain knowledge model");
                browse.setFilterExtensions(new String[] {"*.cif"});
                browse.setFileName(txtInjectModelPath.getText());
                String file = browse.open();
                if (null != file) {
                    txtInjectModelPath.setText(file);
                }
            }
        });

        createLabel(container, "Combine with operator:", null);
        cmbInjectOperator = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        cmbInjectOperator.setItems(
                Arrays.stream(InjectDomainKnowledgeOperator.values()).map(x -> x.description).toArray(String[]::new));
        cmbInjectOperator.setText(InjectDomainKnowledgeOperator.PARALLEL_COMPOSITION.description);
        cmbInjectOperator.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false, 2, 1));
    }

    @Override
    public void updateUIEnablement() {
        // Always enabled.
    }

    @Override
    public String validate() {
        // We don't validate the path, as we don't have the path against which to resolve it available here.
        // We do check a path is provided.
        if (txtInjectModelPath.getText().trim().isEmpty()) {
            return "Select a domain knowledge model";
        }
        return null;
    }

    @Override
    public Class<InjectDomainKnowledgeOptions> getOptionsClass() {
        return InjectDomainKnowledgeOptions.class;
    }

    @Override
    public InjectDomainKnowledgeOptions createOptions() {
        InjectDomainKnowledgeOptions injectDomainKnowledgeOptions = new InjectDomainKnowledgeOptions();
        injectDomainKnowledgeOptions.setModelPath(txtInjectModelPath.getText());
        InjectDomainKnowledgeOperator operator = null;
        for (InjectDomainKnowledgeOperator value: InjectDomainKnowledgeOperator.values()) {
            if (value.description.equals(cmbInjectOperator.getText())) {
                operator = value;
            }
        }
        Preconditions.checkNotNull(operator);
        injectDomainKnowledgeOptions.setOperator(operator);
        return injectDomainKnowledgeOptions;
    }

    @Override
    public void applyOptionsInternal(InjectDomainKnowledgeOptions options) {
        txtInjectModelPath.setText(options.modelPath);
        cmbInjectOperator.setText(options.operator.description);
    }
}
