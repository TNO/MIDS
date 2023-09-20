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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Spinner;

import nl.tno.mids.cmi.postprocessing.PostProcessingOperation;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;

public abstract class PostProcessingOperationUIProvider<T extends PostProcessingOperation<U>,
        U extends PostProcessingOperationOptions, V extends PostProcessingOperationProvider<T, U>>
{
    protected final static int INNER_COLUMNS = 3;

    public abstract V getOperationProvider();

    public abstract void addUI(Composite container, Runnable validate);

    public abstract void updateUIEnablement();

    public abstract String validate();

    public abstract Class<U> getOptionsClass();

    public abstract U createOptions();

    @SuppressWarnings("unchecked")
    public void applyOptions(PostProcessingOperationOptions options) {
        applyOptionsInternal((U)options);
    }

    protected abstract void applyOptionsInternal(U options);

    protected GridData allColumn() {
        GridData ret = new GridData(SWT.FILL, SWT.TOP, true, false);
        ret.horizontalSpan = INNER_COLUMNS;
        return ret;
    }

    protected Label createWideLabel(Composite container, String txt) {
        return createLabel(container, txt, allColumn());
    }

    protected Label createLabel(Composite container, String txt) {
        return createLabel(container, txt, null);
    }

    protected Label createLabel(Composite container, String txt, GridData layout) {
        Label lbl = new Label(container, SWT.NONE);
        lbl.setText(txt);
        if (layout != null) {
            lbl.setLayoutData(layout);
        }
        return lbl;
    }

    protected Layout createGridLayout(int numColumns) {
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = numColumns;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        return gridLayout;
    }

    protected Spinner createSpinner(Composite parent) {
        return new Spinner(parent, SWT.BORDER);
    }

    protected Group createGroup(Composite container) {
        Group group = new Group(container, SWT.NONE);
        return group;
    }

    protected Composite createComposite(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        return composite;
    }
}
