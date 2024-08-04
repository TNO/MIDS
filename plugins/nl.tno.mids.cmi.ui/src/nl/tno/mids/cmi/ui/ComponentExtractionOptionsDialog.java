/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.ui;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.common.base.Preconditions;

import nl.tno.mids.cmi.ComponentExtractionOptions;
import nl.tno.mids.cmi.OutputMode;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus;

/**
 * Dialog to configure CMI model extraction.
 *
 */
public class ComponentExtractionOptionsDialog extends TitleAreaDialog {
    private final static String TITLE = "Model Extraction Options";

    private final static String MESSAGE = "Extracting CIF models from a timed message sequence chart using the following options:";

    private final static int COLUMNS = 2;

    // Output area
    private Text txtOutputFolder;

    private Button btnBrowseOutput;

    private Button btnRadioComponentOutput;

    private Button btnSaveYed;

    private Button btnRadioProtocolOutput;

    private Label lblProtocolComponent1;

    private Text txtProtocolComponent1;

    private Label lblProtocolComponent2;

    private Text txtProtocolComponent2;

    private Label lblProtocolScope;

    private Text txtProtocolScope;

    // Pre-processing area
    private Text txtComponentsInclusionRegEx;

    private Text txtComponentsExclusionRegEx;

    private Button btnUntracedHandleSynchronously;

    // Component extraction area
    private Button btnSynchronizeDependentTransitions;

    private Button btnModelPerComponent;

    // Post-processing area
    private org.eclipse.swt.widgets.List lstPostProcessing;

    private Combo cmbPostProcessingMessages;

    private Button btnPostProcessingAdd;

    private Button btnPostProcessingRemove;

    private Button btnPostProcessingEdit;

    private Button btnPostProcessingUp;

    private Button btnPostProcessingDown;

    private List<PostProcessingOperationOptions> postProcessingOptions;

    // Input path
    private IPath inputFolder;

    // Resulting options
    private ComponentExtractionOptions options;

    /**
     * @param parentShell Parent container for UI components.
     * @param inputFolder Path to the input folder.
     */
    public ComponentExtractionOptionsDialog(Shell parentShell, IPath inputFolder) {
        super(parentShell);
        this.inputFolder = inputFolder;
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
        addSeperator(container);
        createPreProcessingArea(container);
        addSeperator(container);
        createComponentExtractionArea(container);
        addSeperator(container);
        createPostProcessingArea(container);

        btnRadioComponentOutput.notifyListeners(SWT.Selection, null);

        return container;
    }

    private void addSeperator(Composite container) {
        Label separator = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
        separator.setLayoutData(allColumn());
    }

    private void createOutputArea(Composite container) {
        SelectionListener outputSelection = SelectionListener.widgetSelectedAdapter(e -> {
            if (btnRadioComponentOutput.getSelection()) {
                // Disable protocol fields.
                lblProtocolComponent1.setEnabled(false);
                txtProtocolComponent1.setEnabled(false);
                lblProtocolComponent2.setEnabled(false);
                txtProtocolComponent2.setEnabled(false);
                lblProtocolScope.setEnabled(false);
                txtProtocolScope.setEnabled(false);
                // Enable split component button.
                btnModelPerComponent.setEnabled(true);
            }

            if (btnRadioProtocolOutput.getSelection()) {
                // Enable protocol fields.
                lblProtocolComponent1.setEnabled(true);
                txtProtocolComponent1.setEnabled(true);
                lblProtocolComponent2.setEnabled(true);
                txtProtocolComponent2.setEnabled(true);
                lblProtocolScope.setEnabled(true);
                txtProtocolScope.setEnabled(true);
                // Disable split component button.
                btnModelPerComponent.setEnabled(false);
            }
            validate();
        });

        ModifyListener modifyListener = e -> validate();

        createWideLabel(container, "Output options:");

        Composite cmpOutputFolder = new Composite(container, SWT.NONE);
        cmpOutputFolder.setLayoutData(allColumn());

        GridLayout gridLayoutOutputFolder = new GridLayout();
        gridLayoutOutputFolder.numColumns = 3;
        gridLayoutOutputFolder.marginWidth = 0;
        gridLayoutOutputFolder.marginHeight = 0;
        cmpOutputFolder.setLayout(gridLayoutOutputFolder);

        createLabel(cmpOutputFolder, "Write output to folder:", null);
        txtOutputFolder = new Text(cmpOutputFolder, SWT.BORDER);
        GridData outputFolderData = new GridData(SWT.CENTER, SWT.CENTER, true, false);
        outputFolderData.widthHint = convertWidthInCharsToPixels(50);
        txtOutputFolder.setLayoutData(outputFolderData);
        txtOutputFolder.setText(inputFolder.toString());
        txtOutputFolder.addModifyListener(modifyListener);

        btnBrowseOutput = new Button(cmpOutputFolder, SWT.PUSH);
        btnBrowseOutput.setText("Browse");
        btnBrowseOutput.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        btnBrowseOutput.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(getShell());
                dialog.setFilterPath(txtOutputFolder.getText());
                String result = dialog.open();
                if (result != null) {
                    txtOutputFolder.setText(result);
                }
            }
        });

        btnSaveYed = new Button(container, SWT.CHECK);
        btnSaveYed.setText("Save models as yEd diagrams");
        btnSaveYed.setLayoutData(allColumn());

        btnRadioComponentOutput = new Button(container, SWT.RADIO);
        btnRadioComponentOutput.setText("Output component models");
        btnRadioComponentOutput.setSelection(true);
        btnRadioComponentOutput.addSelectionListener(outputSelection);

        btnRadioProtocolOutput = new Button(container, SWT.RADIO);
        btnRadioProtocolOutput.setText("Output protocol model");
        btnRadioProtocolOutput.addSelectionListener(outputSelection);

        lblProtocolComponent1 = createLabel(container, "First component:", null);
        txtProtocolComponent1 = new Text(container, SWT.BORDER);
        txtProtocolComponent1.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
        txtProtocolComponent1.addModifyListener(modifyListener);

        lblProtocolComponent2 = createLabel(container, "Second component:", null);
        txtProtocolComponent2 = new Text(container, SWT.BORDER);
        txtProtocolComponent2.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
        txtProtocolComponent2.addModifyListener(modifyListener);

        lblProtocolScope = createLabel(container, "Scope:", null);
        txtProtocolScope = new Text(container, SWT.BORDER);
        txtProtocolScope.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
        txtProtocolScope.addModifyListener(modifyListener);
        txtProtocolScope.setToolTipText(
                "Add components to scope of state space generation. These will not be present in the final protocol.");
    }

    private void createPreProcessingArea(Composite container) {
        createWideLabel(container, "Preprocessing options:");

        btnUntracedHandleSynchronously = new Button(container, SWT.CHECK);
        btnUntracedHandleSynchronously
                .setText("Convert events on untraced components to represent synchronously handled functions");
        btnUntracedHandleSynchronously.setLayoutData(allColumn());
        btnUntracedHandleSynchronously.setSelection(true);
    }

    private void createComponentExtractionArea(Composite container) {
        createWideLabel(container, "Component extraction options:");

        btnSynchronizeDependentTransitions = new Button(container, SWT.CHECK);
        btnSynchronizeDependentTransitions.setText("Synchronize dependent transitions");
        btnSynchronizeDependentTransitions.setSelection(true);

        btnModelPerComponent = new Button(container, SWT.CHECK);
        btnModelPerComponent.setText("Create separate CIF model per component");
        btnModelPerComponent.setSelection(true);
    }

    private void createPostProcessingArea(Composite container) {
        createWideLabel(container, "Component extraction postprocessing options:");

        new Label(container, SWT.HORIZONTAL).setText("Components inclusion regex:");
        txtComponentsInclusionRegEx = new Text(container, SWT.BORDER);
        txtComponentsInclusionRegEx.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));

        new Label(container, SWT.HORIZONTAL).setText("Components exclusion regex:");
        txtComponentsExclusionRegEx = new Text(container, SWT.BORDER);
        txtComponentsExclusionRegEx.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));

        Composite cmpPostProcessing = new Composite(container, SWT.NONE);
        cmpPostProcessing.setLayoutData(allColumn());

        GridLayout gridLayoutPostProcessing = new GridLayout();
        gridLayoutPostProcessing.numColumns = 2;
        gridLayoutPostProcessing.marginWidth = 0;
        gridLayoutPostProcessing.marginHeight = 0;
        cmpPostProcessing.setLayout(gridLayoutPostProcessing);

        Runnable updatePostProcessingUI = () -> {
            btnPostProcessingRemove.setEnabled(lstPostProcessing.getSelectionIndex() != -1);
            btnPostProcessingEdit.setEnabled(lstPostProcessing.getSelectionIndex() != -1);
            btnPostProcessingUp.setEnabled(
                    lstPostProcessing.getSelectionIndex() != -1 && lstPostProcessing.getSelectionIndex() > 0);
            btnPostProcessingDown.setEnabled(lstPostProcessing.getSelectionIndex() != -1
                    && lstPostProcessing.getSelectionIndex() < lstPostProcessing.getItemCount() - 1);

            cmbPostProcessingMessages.removeAll();
            cmbPostProcessingMessages.setVisible(false);
            PostProcessingStatus postProcessingStatus = new PostProcessingStatus(false, false);
            for (PostProcessingOperationOptions ppoo: postProcessingOptions) {
                List<String> messages = postProcessingStatus
                        .validate(ppoo.getProvider().getOperationFromRawOptions(ppoo).getPreconditionSubset());
                for (String message: messages) {
                    cmbPostProcessingMessages.add(ppoo.getProvider().getOperationReadableName() + " - " + message);
                    cmbPostProcessingMessages.setVisible(true);
                    cmbPostProcessingMessages.select(0);
                }

                postProcessingStatus = ppoo.getProvider().getOperationFromRawOptions(ppoo).getPreconditionSubset()
                        .apply(postProcessingStatus);
                postProcessingStatus = ppoo.getProvider().getOperationFromRawOptions(ppoo).getResultSubset()
                        .apply(postProcessingStatus);
            }
        };

        lstPostProcessing = new org.eclipse.swt.widgets.List(cmpPostProcessing, SWT.SINGLE | SWT.BORDER);
        lstPostProcessing.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 5));
        lstPostProcessing.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updatePostProcessingUI.run();
            }
        });

        btnPostProcessingAdd = new Button(cmpPostProcessing, SWT.PUSH);
        btnPostProcessingAdd.setText("Add");
        btnPostProcessingAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        btnPostProcessingAdd.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                PostProcessingOperationOptionsDialog dialog = new PostProcessingOperationOptionsDialog(getShell(),
                        "Add");
                if (dialog.open() != Window.OK) {
                    return;
                }
                PostProcessingOperationOptions postProcessing = dialog.getOptions();
                Preconditions.checkNotNull(postProcessing);
                lstPostProcessing.add(postProcessing.toString());
                postProcessingOptions.add(postProcessing);

                updatePostProcessingUI.run();
            }
        });

        btnPostProcessingRemove = new Button(cmpPostProcessing, SWT.PUSH);
        btnPostProcessingRemove.setText("Remove");
        btnPostProcessingRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        btnPostProcessingRemove.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = lstPostProcessing.getSelectionIndex();
                lstPostProcessing.remove(idx);
                postProcessingOptions.remove(idx);
                if (idx > 0) {
                    lstPostProcessing.select(idx - 1);
                } else if (lstPostProcessing.getItemCount() > 0) {
                    lstPostProcessing.select(0);
                }

                updatePostProcessingUI.run();
            }
        });

        btnPostProcessingEdit = new Button(cmpPostProcessing, SWT.PUSH);
        btnPostProcessingEdit.setText("Edit");
        btnPostProcessingEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        btnPostProcessingEdit.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = lstPostProcessing.getSelectionIndex();
                PostProcessingOperationOptionsDialog dialog = new PostProcessingOperationOptionsDialog(getShell(),
                        "Edit");
                dialog.setOptions(postProcessingOptions.get(idx));
                if (dialog.open() != Window.OK) {
                    return;
                }
                PostProcessingOperationOptions postProcessing = dialog.getOptions();
                Preconditions.checkNotNull(postProcessing);
                lstPostProcessing.setItem(idx, postProcessing.toString());
                postProcessingOptions.set(idx, postProcessing);

                updatePostProcessingUI.run();
            }
        });

        btnPostProcessingUp = new Button(cmpPostProcessing, SWT.PUSH);
        btnPostProcessingUp.setText("Move up");
        btnPostProcessingUp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        btnPostProcessingUp.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = lstPostProcessing.getSelectionIndex();

                String itemText = lstPostProcessing.getItem(idx);
                lstPostProcessing.setItem(idx, lstPostProcessing.getItem(idx - 1));
                lstPostProcessing.setItem(idx - 1, itemText);
                lstPostProcessing.select(idx - 1);

                PostProcessingOperationOptions postProcessing = postProcessingOptions.get(idx);
                postProcessingOptions.set(idx, postProcessingOptions.get(idx - 1));
                postProcessingOptions.set(idx - 1, postProcessing);

                updatePostProcessingUI.run();
            }
        });

        btnPostProcessingDown = new Button(cmpPostProcessing, SWT.PUSH);
        btnPostProcessingDown.setText("Move down");
        btnPostProcessingDown.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        btnPostProcessingDown.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = lstPostProcessing.getSelectionIndex();

                String itemText = lstPostProcessing.getItem(idx);
                lstPostProcessing.setItem(idx, lstPostProcessing.getItem(idx + 1));
                lstPostProcessing.setItem(idx + 1, itemText);
                lstPostProcessing.select(idx + 1);

                PostProcessingOperationOptions postProcessing = postProcessingOptions.get(idx);
                postProcessingOptions.set(idx, postProcessingOptions.get(idx + 1));
                postProcessingOptions.set(idx + 1, postProcessing);

                updatePostProcessingUI.run();
            }
        });

        cmbPostProcessingMessages = new Combo(cmpPostProcessing, SWT.DROP_DOWN | SWT.READ_ONLY);
        cmbPostProcessingMessages.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 6, 1));
        cmbPostProcessingMessages.setForeground(new Color(Display.getCurrent(), 255, 128, 0));
        postProcessingOptions = new ArrayList<>();

        updatePostProcessingUI.run();
    }

    private void validate() {
        String errMsg = null;

        try {
            Paths.get(txtOutputFolder.getText());
        } catch (InvalidPathException e) {
            errMsg = "'Output folder' must be a valid path";
        }

        if (btnRadioProtocolOutput.getSelection() && errMsg == null) {
            String protocolComponent1 = txtProtocolComponent1.getText().trim();
            String protocolComponent2 = txtProtocolComponent2.getText().trim();
            String scopeComponents = txtProtocolScope.getText().trim();
            if (protocolComponent1.trim().isEmpty()) {
                errMsg = "Enter first component name for protocol";
            } else if (protocolComponent2.trim().isEmpty()) {
                errMsg = "Enter second component name for protocol";
            } else if (protocolComponent1.equals(protocolComponent2)) {
                errMsg = "Component " + protocolComponent1 + " cannot have a protocol with itself";
            } else if (!scopeComponents.isEmpty()) {
                List<String> scopeComponentsList = Arrays.asList(scopeComponents.split(",")).stream().map(c -> c.trim())
                        .collect(Collectors.toList());
                Set<String> scopeComponentsSet = new HashSet<>(scopeComponentsList);
                if (scopeComponentsList.size() != scopeComponentsSet.size()) {
                    errMsg = "Extended protocol scope contains duplicates";
                } else if (scopeComponentsSet.contains(protocolComponent1)) {
                    errMsg = "Component " + protocolComponent1 + " already in protocol scope";
                } else if (scopeComponentsSet.contains(protocolComponent2)) {
                    errMsg = "Component " + protocolComponent2 + " already in protocol scope";
                }
            }
        }

        setErrorMessage(errMsg);
        Button okButton = this.getButton(IDialogConstants.OK_ID);
        if (okButton != null) {
            okButton.setEnabled(errMsg == null);
        }
    }

    @Override
    protected void okPressed() {
        options = new ComponentExtractionOptions();

        // Output
        options.getOutput().setPath(Paths.get(txtOutputFolder.getText()).toAbsolutePath().normalize());

        options.getOutput().setSaveYed(btnSaveYed.getSelection());
        if (btnRadioComponentOutput.getSelection()) {
            options.getOutput().setOutputMode(OutputMode.COMPONENTS);
        } else if (btnRadioProtocolOutput.getSelection()) {
            options.getOutput().setOutputMode(OutputMode.PROTOCOL);
            options.getOutput().setProtocolName1(txtProtocolComponent1.getText().trim());
            options.getOutput().setProtocolName2(txtProtocolComponent2.getText().trim());
            options.getOutput().setScope(Arrays.asList(txtProtocolScope.getText().split(",")).stream()
                    .map(s -> s.trim()).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        }

        // Pre-processing
        options.getPreProcessing().setUntracedHandleSynchronously(btnUntracedHandleSynchronously.getSelection());

        // Extraction
        options.getExtraction().setSynchronizeDependentTransitions(btnSynchronizeDependentTransitions.getSelection());
        options.getExtraction().setModelPerComponent(btnModelPerComponent.getSelection());

        // Post-processing
        options.getPostProcessing().setComponentsInclusionRegEx(txtComponentsInclusionRegEx.getText());
        options.getPostProcessing().setComponentsExclusionRegEx(txtComponentsExclusionRegEx.getText());
        options.getPostProcessing().getOperations().addAll(postProcessingOptions);

        // Done.
        super.okPressed();
    }

    private GridData allColumn() {
        GridData ret = new GridData(SWT.FILL, SWT.TOP, true, false);
        ret.horizontalSpan = COLUMNS;
        return ret;
    }

    /**
     * @return Option values selected in this dialog.
     */
    public ComponentExtractionOptions options() {
        return options;
    }

    private Label createWideLabel(Composite container, String txt) {
        return createLabel(container, txt, allColumn());
    }

    private Label createLabel(Composite container, String txt, GridData layout) {
        Label lbl = new Label(container, SWT.NONE);
        lbl.setText(txt);
        if (layout != null) {
            lbl.setLayoutData(layout);
        }
        return lbl;
    }
}
