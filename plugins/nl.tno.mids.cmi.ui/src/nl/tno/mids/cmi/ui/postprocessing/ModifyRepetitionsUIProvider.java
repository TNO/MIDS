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

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Spinner;

import com.google.common.base.Preconditions;

import nl.tno.mids.cif.extensions.mrr.cif.MrrToCifMode;
import nl.tno.mids.cmi.postprocessing.operations.ModifyRepetitions;
import nl.tno.mids.cmi.postprocessing.operations.ModifyRepetitionsOptions;
import nl.tno.mids.cmi.postprocessing.operations.ModifyRepetitionsProvider;

public class ModifyRepetitionsUIProvider extends
        PostProcessingOperationUIProvider<ModifyRepetitions, ModifyRepetitionsOptions, ModifyRepetitionsProvider>
{
    private Button btnRepetitionsInfinite;

    private Spinner spnRepetitionsLowerThreshold;

    private Spinner spnRepetitionsUpperThreshold;

    private Spinner spnRepetitionsMaxRepeats;

    private Combo cmbRepetitionsMode;

    @Override
    public ModifyRepetitionsProvider getOperationProvider() {
        return new ModifyRepetitionsProvider();
    }

    @Override
    public void addUI(Composite container, Runnable validate) {
        container.setLayout(createGridLayout(1));

        {
            Group group = createGroup(container);
            group.setText("Select repetitions to process (by threshold)");
            group.setLayout(createGridLayout(3));

            createLabel(group, "Lower bound:");
            spnRepetitionsLowerThreshold = createSpinner(group);
            spnRepetitionsLowerThreshold.setValues(0, 0, Integer.MAX_VALUE, 0, 1, 10);
            createLabel(group, "(0 for no lower bound)");

            createLabel(group, "Upper bound:");
            spnRepetitionsUpperThreshold = createSpinner(group);
            spnRepetitionsUpperThreshold.setValues(0, 0, Integer.MAX_VALUE, 0, 1, 10);
            createLabel(group, "(0 for no upper bound)");
        }

        {
            Group group = createGroup(container);
            group.setText("Modifications to perform");
            group.setLayout(createGridLayout(1));

            Composite btnGroup = createComposite(group);
            btnGroup.setLayout(createGridLayout(2));
            btnRepetitionsInfinite = new Button(btnGroup, SWT.CHECK);
            btnRepetitionsInfinite.setText("Make repetitions infinite");

            Composite spnGroup = createComposite(group);
            spnGroup.setLayout(createGridLayout(3));
            createLabel(spnGroup, "Restrict number of repetitions to at most:", null);
            spnRepetitionsMaxRepeats = createSpinner(spnGroup);
            spnRepetitionsMaxRepeats.setValues(0, 0, Integer.MAX_VALUE, 0, 1, 10);
            createLabel(spnGroup, "(0 to disable)", null);

            Composite cmbGroup = createComposite(group);
            cmbGroup.setLayout(createGridLayout(2));
            createLabel(cmbGroup, "Edges:");
            cmbRepetitionsMode = new Combo(cmbGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            cmbRepetitionsMode
                    .setItems(Arrays.stream(MrrToCifMode.values()).map(x -> x.description).toArray(String[]::new));
            cmbRepetitionsMode.setText(MrrToCifMode.PLAIN.description);
        }

        // Add all behavior to the dialog controls.
        spnRepetitionsLowerThreshold.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> validate.run()));
        spnRepetitionsMaxRepeats.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> validate.run()));
        spnRepetitionsUpperThreshold.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> validate.run()));

        cmbRepetitionsMode.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> validate.run()));

        btnRepetitionsInfinite.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            updateUIEnablement();
            spnRepetitionsMaxRepeats.setSelection(0);
            validate.run();
        }));
    }

    @Override
    public void updateUIEnablement() {
        spnRepetitionsMaxRepeats.setEnabled(!btnRepetitionsInfinite.getSelection());
    }

    @Override
    public String validate() {
        if (spnRepetitionsUpperThreshold.getSelection() > 0
                && spnRepetitionsLowerThreshold.getSelection() > spnRepetitionsUpperThreshold.getSelection())
        {
            return "Upper bound must be equal or higher than lower bound.";
        } else if (!btnRepetitionsInfinite.getSelection() && spnRepetitionsMaxRepeats.getSelection() == 0
                && cmbRepetitionsMode.getText().equals(MrrToCifMode.PLAIN.description))
        {
            return "Current settings don't impose any modifications";
        } else {
            return null;
        }
    }

    @Override
    public Class<ModifyRepetitionsOptions> getOptionsClass() {
        return ModifyRepetitionsOptions.class;
    }

    @Override
    public ModifyRepetitionsOptions createOptions() {
        ModifyRepetitionsOptions options = new ModifyRepetitionsOptions();
        options.setLowerThreshold(spnRepetitionsLowerThreshold.getSelection());
        options.setUpperThreshold(spnRepetitionsUpperThreshold.getSelection());
        options.setMakeInfinite(btnRepetitionsInfinite.getSelection());
        options.setMaxRepeats(spnRepetitionsMaxRepeats.getSelection());
        MrrToCifMode mrrToCifMode = null;
        for (MrrToCifMode mode: MrrToCifMode.values()) {
            if (mode.description.equals(cmbRepetitionsMode.getText())) {
                mrrToCifMode = mode;
            }
        }
        Preconditions.checkNotNull(mrrToCifMode);
        options.setMode(mrrToCifMode);
        return options;
    }

    @Override
    public void applyOptionsInternal(ModifyRepetitionsOptions options) {
        btnRepetitionsInfinite.setSelection(options.makeInfinite);
        spnRepetitionsLowerThreshold.setSelection(options.lowerThreshold);
        spnRepetitionsUpperThreshold.setSelection(options.upperThreshold);
        spnRepetitionsMaxRepeats.setSelection(options.maxRepeats);
        cmbRepetitionsMode.setText(options.mode.description);
    }
}
