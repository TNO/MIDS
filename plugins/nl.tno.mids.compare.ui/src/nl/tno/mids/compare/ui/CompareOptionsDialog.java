/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.ui;

import java.nio.file.Paths;
import java.util.Arrays;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

import nl.tno.mids.compare.options.CmiCompareMode;
import nl.tno.mids.compare.options.CompareAlgorithm;
import nl.tno.mids.compare.options.CompareOptions;
import nl.tno.mids.compare.options.HslColorScheme;
import nl.tno.mids.compare.options.ModelType;

class CompareOptionsDialog extends TitleAreaDialog {
    private final static String TITLE = "Model Compare Options";

    private final static String MESSAGE = "Compare sets of CIF models using the following options:";

    private final static int COLUMNS = 3;

    private Label lblApplyPostprocessing;

    private Button btnApplyPostprocessing;

    private Label lblCompareAlgorithm;

    private Combo cmbCompareAlgorithm;

    private Label lblSvgGenerationTimeout;

    private Spinner spnSvgGenerationTimeout;

    private Label lblUnionIntersectionSizeLimit;

    private Spinner spnUnionIntersectionSizeLimit;

    private Label lblStructuralCompareSizeLimit;

    private Spinner spnStructuralCompareSizeLimit;

    private Label lblOutputPath;

    private Text txtOutputPath;

    private Button btnBrowseModelRepo;

    private CompareOptions compareOptions;

    private Combo cmbColorScheme;

    private Label lblColorScheme;

    private Label lblModelType;

    private Combo cmbModelType;

    private Label lblCmiCompareMode;

    private Combo cmbCmiCompareMode;

    CompareOptionsDialog(Shell parentShell, CompareOptions compareOptions) {
        super(parentShell);
        this.compareOptions = compareOptions;
    }

    /**
     * Create dialog window.
     */
    @Override
    public void create() {
        super.create();
        setTitle(TITLE);
        setMessage(MESSAGE);
    }

    /**
     * Create contents of dialog window.
     * 
     * @param parent Container of dialog window.
     * 
     * @return Created dialog area.
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite)super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        GridLayout layout = new GridLayout(COLUMNS, false);
        container.setLayout(layout);

        { // output folder controls
            lblOutputPath = new Label(container, SWT.NONE);
            lblOutputPath.setText("Select output folder");

            txtOutputPath = new Text(container, SWT.BORDER);
            txtOutputPath.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
            txtOutputPath.setText(compareOptions.outputPath.toString());

            btnBrowseModelRepo = new Button(container, SWT.NONE);
            btnBrowseModelRepo.setText("Browse...");
            btnBrowseModelRepo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    DirectoryDialog browse = new DirectoryDialog(getShell());
                    browse.setFilterPath(compareOptions.outputPath.toString());
                    browse.setText("Select output folder");
                    browse.setMessage("Please select a folder to store output");
                    String directory = browse.open();
                    if (null != directory) {
                        txtOutputPath.setText(directory);
                    }
                }
            });
        }
        { // model type selection control
            lblModelType = new Label(container, SWT.NONE);
            lblModelType.setText("Model type");

            cmbModelType = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
            cmbModelType.setItems(Arrays.stream(ModelType.values()).map(mt -> mt.description).toArray(String[]::new));
            cmbModelType.setText(compareOptions.modelType.description);
            cmbModelType.setLayoutData(spanColumns(2));
            cmbModelType
                    .addModifyListener(e ->
                    { cmbCmiCompareMode.setEnabled(cmbModelType.getText().equals(ModelType.CMI.description)); });
        }
        { // CMI compare mode
            lblCmiCompareMode = new Label(container, SWT.NONE);
            lblCmiCompareMode.setText("CMI compare mode");

            cmbCmiCompareMode = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
            cmbCmiCompareMode
                    .setItems(Arrays.stream(CmiCompareMode.values()).map(mt -> mt.description).toArray(String[]::new));
            cmbCmiCompareMode.setText(compareOptions.cmiCompareMode.description);
            cmbCmiCompareMode.setLayoutData(spanColumns(2));
        }
        { // color scheme selection control
            lblColorScheme = new Label(container, SWT.NONE);
            lblColorScheme.setText("Color scheme");

            cmbColorScheme = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
            cmbColorScheme
                    .setItems(Arrays.stream(HslColorScheme.values()).map(cs -> cs.description).toArray(String[]::new));
            cmbColorScheme.setText(compareOptions.colorScheme.description);
            cmbColorScheme.setLayoutData(spanColumns(2));
        }
        { // apply post processing to the comparison results
            lblApplyPostprocessing = new Label(container, SWT.NONE);
            lblApplyPostprocessing.setText("Apply post-processing to the comparison results");

            btnApplyPostprocessing = new Button(container, SWT.CHECK);
            btnApplyPostprocessing.setLayoutData(spanColumns(2));
            btnApplyPostprocessing.setSelection(compareOptions.applyPostprocessing);
        }
        { // structural comparison algorithm selection control
            lblCompareAlgorithm = new Label(container, SWT.NONE);
            lblCompareAlgorithm.setText("Structural comparison algorithm");

            cmbCompareAlgorithm = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
            cmbCompareAlgorithm.setItems(
                    Arrays.stream(CompareAlgorithm.values()).map(a -> a.getDescription()).toArray(String[]::new));
            cmbCompareAlgorithm.setText(compareOptions.compareAlgorithm.getDescription());
            cmbCompareAlgorithm.setLayoutData(spanColumns(2));
        }
        { // SVG generation timeout
            lblSvgGenerationTimeout = new Label(container, SWT.NONE);
            lblSvgGenerationTimeout.setText("Timeout for generating SVG files, in seconds");

            spnSvgGenerationTimeout = new Spinner(container, SWT.BORDER);
            spnSvgGenerationTimeout.setLayoutData(spanColumns(2));
            spnSvgGenerationTimeout.setValues(compareOptions.svgGenerationTimeout, 1, 3600, 0, 1, 1);
        }
        { // Union/intersection size limit (in the number of states)
            lblUnionIntersectionSizeLimit = new Label(container, SWT.NONE);
            lblUnionIntersectionSizeLimit.setText("Union/intersection size limit, in the number of states");

            spnUnionIntersectionSizeLimit = new Spinner(container, SWT.BORDER);
            spnUnionIntersectionSizeLimit.setLayoutData(spanColumns(2));
            spnUnionIntersectionSizeLimit.setValues(compareOptions.unionIntersectionSizeLimit, 0, 1000000, 0, 100, 100);
        }
        { // Structural comparison size limit (in the number of states)
            lblStructuralCompareSizeLimit = new Label(container, SWT.NONE);
            lblStructuralCompareSizeLimit.setText("Structural comparison size limit, in the number of states");

            spnStructuralCompareSizeLimit = new Spinner(container, SWT.BORDER);
            spnStructuralCompareSizeLimit.setLayoutData(spanColumns(2));
            spnStructuralCompareSizeLimit.setValues(compareOptions.structuralCompareSizeLimit, 0, 1000000, 0, 100, 100);
        }

        return area;
    }

    /**
     * Handle OK button press.
     */
    @Override
    protected void okPressed() {
        compareOptions.outputPath = Paths.get(txtOutputPath.getText()).toAbsolutePath().normalize();
        compareOptions.applyPostprocessing = btnApplyPostprocessing.getSelection();
        compareOptions.colorScheme = Arrays.stream(HslColorScheme.values())
                .filter(cs -> cs.description.equals(cmbColorScheme.getText())).findFirst().get();
        compareOptions.modelType = Arrays.stream(ModelType.values())
                .filter(cs -> cs.description.equals(cmbModelType.getText())).findFirst().get();
        compareOptions.cmiCompareMode = Arrays.stream(CmiCompareMode.values())
                .filter(cs -> cs.description.equals(cmbCmiCompareMode.getText())).findFirst().get();
        compareOptions.compareAlgorithm = Arrays.stream(CompareAlgorithm.values())
                .filter(a -> a.getDescription().equals(cmbCompareAlgorithm.getText())).findFirst().get();
        compareOptions.svgGenerationTimeout = spnSvgGenerationTimeout.getSelection();
        compareOptions.unionIntersectionSizeLimit = spnUnionIntersectionSizeLimit.getSelection();
        compareOptions.structuralCompareSizeLimit = spnStructuralCompareSizeLimit.getSelection();

        super.okPressed();
    }

    private GridData spanColumns(int spanColums) {
        GridData ret = new GridData(SWT.FILL, SWT.TOP, true, false);
        ret.horizontalSpan = spanColums;
        return ret;
    }
}
