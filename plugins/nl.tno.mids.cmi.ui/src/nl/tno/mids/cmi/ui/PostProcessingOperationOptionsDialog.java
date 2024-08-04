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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

import com.google.common.base.Preconditions;

import nl.tno.mids.cmi.postprocessing.PostProcessingFilterMode;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProviders;
import nl.tno.mids.cmi.ui.postprocessing.PostProcessingOperationUIProvider;

/**
 * Dialog to configure post processing operation.
 *
 */
public class PostProcessingOperationOptionsDialog extends TitleAreaDialog {
    private final static String TITLE_POSTFIX = "post processing operation";

    private final static String MESSAGE = "Select operation and configure options.";

    private final static int OUTER_COLUMNS = 2;

    private final static int INNER_COLUMNS = 3;

    private final String titlePrefix;

    // Operations UI.
    private org.eclipse.swt.widgets.List lstOperation;

    // Information per operation.
    private List<String> operationFormalNames = new ArrayList<>();

    private List<Composite> operationContainers = new ArrayList<>();

    private List<Runnable> operationUiEnablementUpdaters = new ArrayList<>();

    private List<Supplier<String>> operationValidators = new ArrayList<>();

    private List<Supplier<PostProcessingOperationOptions>> operationOptionsCreators = new ArrayList<>();

    private List<Class<? extends PostProcessingOperationOptions>> operationOptionsClasses = new ArrayList<>();

    private List<Consumer<PostProcessingOperationOptions>> operationOptionsAppliers = new ArrayList<>();

    private List<Boolean> operationSupportsComponentFiltering = new ArrayList<>();

    // Information for common options.
    private List<Runnable> commonOptionsUiEnablementUpdaters = new ArrayList<>();

    private List<Function<Boolean, String>> commonOptionsValidators = new ArrayList<>();

    private List<Consumer<PostProcessingOperationOptions>> commonOptionsAppliers = new ArrayList<>();

    private List<Consumer<PostProcessingOperationOptions>> commonOptionsSetters = new ArrayList<>();

    // Filtering.
    private Label lblFilterMode;

    private Combo cmbFilterMode;

    private Label lblFilterPattern;

    private Text txtFilterPattern;

    // Resulting options.
    private PostProcessingOperationOptions options;

    /**
     * @param parentShell Parent container for this dialog.
     * @param titlePrefix Prefix for dialog title.
     */
    public PostProcessingOperationOptionsDialog(Shell parentShell, String titlePrefix) {
        super(parentShell);
        this.titlePrefix = titlePrefix;
    }

    @Override
    public void create() {
        super.create();
        setTitle(titlePrefix + " " + TITLE_POSTFIX);
        setMessage(MESSAGE);
        validate();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        // Get parent area.
        Composite area = (Composite)super.createDialogArea(parent);

        // Fill parent area with a container for all our content.
        Composite container = createComposite(area);

        GridData containerLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        container.setLayoutData(containerLayoutData);

        GridLayout containerLayout = new GridLayout(OUTER_COLUMNS, false);
        container.setLayout(containerLayout);

        // At the left of the container is the list of operations to choose from.
        lstOperation = new org.eclipse.swt.widgets.List(container, SWT.SINGLE | SWT.BORDER);
        lstOperation.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));

        // At the right of the container are the options, per operation.
        Composite rightContainer = createComposite(container);

        rightContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));

        GridLayout rightLayout = new GridLayout(1, false);
        rightContainer.setLayout(rightLayout);

        // At the top of the right container are the operation specific options.
        Composite rightTopContainer = createComposite(rightContainer);

        rightTopContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));

        StackLayout rightTopContainerLayout = new StackLayout();
        rightTopContainerLayout.marginWidth = 0;
        rightTopContainerLayout.marginHeight = 0;
        rightTopContainer.setLayout(rightTopContainerLayout);

        // Connect left and right parts.
        lstOperation.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // Update right side container to show operation specific options.
                int idx = lstOperation.getSelectionIndex();
                rightTopContainerLayout.topControl = operationContainers.get(idx);

                // Show filtering only if supported.
                lblFilterMode.setVisible(operationSupportsComponentFiltering.get(idx));
                cmbFilterMode.setVisible(operationSupportsComponentFiltering.get(idx));
                lblFilterPattern.setVisible(operationSupportsComponentFiltering.get(idx));
                txtFilterPattern.setVisible(operationSupportsComponentFiltering.get(idx));

                // Re-layout.
                rightTopContainer.layout(true);

                // Re-validate the options.
                validate();
            }
        });

        // Add the operations.
        addOperations(rightTopContainer);

        // Sanity checking for operations information.
        int operationCount = operationFormalNames.size();
        Preconditions.checkState(operationCount == operationContainers.size());
        Preconditions.checkState(operationCount == operationUiEnablementUpdaters.size());
        Preconditions.checkState(operationCount == operationValidators.size());
        Preconditions.checkState(operationCount == operationOptionsCreators.size());
        Preconditions.checkState(operationCount == operationOptionsClasses.size());
        Preconditions.checkState(operationCount == operationOptionsAppliers.size());

        // At the bottom right are the common options that apply to all operations.
        Composite rightBottomContainer = createComposite(rightContainer);

        rightBottomContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));

        GridLayout rightBottomLayout = new GridLayout(1, false);
        rightBottomLayout.marginWidth = 0;
        rightBottomLayout.marginHeight = 0;
        rightBottomContainer.setLayout(rightBottomLayout);

        // Add common options.
        Label separator = new Label(rightBottomContainer, SWT.HORIZONTAL | SWT.SEPARATOR);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        addCommonOptionsForFiltering(rightBottomContainer);

        // Sanity checking for operations information.
        int commonOptionsCount = commonOptionsSetters.size();
        Preconditions.checkState(commonOptionsCount == commonOptionsUiEnablementUpdaters.size());
        Preconditions.checkState(commonOptionsCount == commonOptionsValidators.size());
        Preconditions.checkState(commonOptionsCount == commonOptionsAppliers.size());
        Preconditions.checkState(commonOptionsCount == commonOptionsSetters.size());

        // Select operation that is selected when dialog opens.
        if (options == null) {
            // Select first operation.
            lstOperation.select(0);
        } else {
            // Apply operation options to UI.
            for (int i = 0; i < operationOptionsClasses.size(); i++) {
                Class<?> operationOptionsClass = operationOptionsClasses.get(i);
                if (operationOptionsClass.isInstance(options)) {
                    // Apply operation specific options.
                    operationOptionsAppliers.get(i).accept(options);

                    // Apply common options that apply to all operations.
                    for (int j = 0; j < commonOptionsAppliers.size(); j++) {
                        commonOptionsAppliers.get(j).accept(options);
                    }

                    break;
                }
            }

            // Select operation.
            String optionsOperationFormalName = PostProcessingOperationProviders
                    .getPostProcessingOperationProvider(options).getOperationFormalName();
            int idx = operationFormalNames.indexOf(optionsOperationFormalName);
            lstOperation.select(idx);
            options = null;
        }

        // Update UI for selected operation, to ensure it is shown when dialog opens.
        lstOperation.notifyListeners(SWT.Selection, null);

        // Update UI enablement.
        for (int i = 0; i < operationUiEnablementUpdaters.size(); i++) {
            operationUiEnablementUpdaters.get(i).run();
        }
        for (int i = 0; i < commonOptionsUiEnablementUpdaters.size(); i++) {
            commonOptionsUiEnablementUpdaters.get(i).run();
        }

        // Return UI element.
        return container;
    }

    private List<PostProcessingOperationUIProvider<?, ?, ?>> getUIProviders() {
        // Use extension registry to find registered providers.
        IExtensionRegistry registry = RegistryFactory.getRegistry();
        String extensionPointId = "nl.tno.mids.cmi.ui.postprocessing";
        IConfigurationElement[] extensions = registry.getConfigurationElementsFor(extensionPointId);

        // Get providers.
        List<PostProcessingOperationUIProvider<?, ?, ?>> providers = new ArrayList<>();
        for (IConfigurationElement extension: extensions) {
            // Check for providers extension.
            if (!"provider".equals(extension.getName())) {
                continue;
            }

            // Get OSGi bundle.
            String pluginName = extension.getAttribute("plugin");
            Bundle bundle = Platform.getBundle(pluginName);
            if (bundle == null) {
                throw new RuntimeException("CMI post-processing operation UI provider plugin not found: " + pluginName);
            }

            // Check bundle state.
            int state = bundle.getState();
            boolean stateOk = state == Bundle.RESOLVED || state == Bundle.STARTING || state == Bundle.ACTIVE;
            if (!stateOk) {
                throw new RuntimeException("CMI post-processing operation UI provider plugin in wrong state: "
                        + pluginName + " (state=" + state + ")");
            }

            // Get class loader from bundle.
            BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
            if (bundleWiring == null) {
                throw new RuntimeException(
                        "CMI post-processing operation UI provider plugin has no bundle wiring: " + pluginName);
            }
            ClassLoader classLoader = bundleWiring.getClassLoader();
            if (classLoader == null) {
                throw new RuntimeException(
                        "CMI post-processing operation UI provider plugin has no class loader: " + pluginName);
            }

            // Get class.
            String className = extension.getAttribute("class");
            Class<?> cls;
            try {
                cls = classLoader.loadClass(className);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(
                        "CMI post-processing operation UI provider plugin is missing the required class: " + pluginName
                                + " (class=" + className + ")");
            }

            // Get provider.
            PostProcessingOperationUIProvider<?, ?, ?> provider;
            try {
                provider = (PostProcessingOperationUIProvider<?, ?, ?>)cls.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(
                        "CMI post-processing operation UI provider plugin has class that could not be instantiated: "
                                + pluginName + " (class=" + className + ")");
            }

            // Add provider.
            providers.add(provider);
        }

        return providers;
    }

    private void addOperations(Composite parent) {
        // Note:
        // The post-processing operations should be added, starting with the operation that has the largest container
        // size. This should be taken into account when contributing providers via the extension point. This is a
        // workaround for a known UI issue.

        List<PostProcessingOperationUIProvider<?, ?, ?>> uiProviders = getUIProviders();
        for (PostProcessingOperationUIProvider<?, ?, ?> uiProvider: uiProviders) {
            PostProcessingOperationProvider<?, ?> operationProvider = uiProvider.getOperationProvider();
            Composite operationContainer = addOperation(parent, operationProvider);
            uiProvider.addUI(operationContainer, this::validate);
            operationValidators.add(() -> uiProvider.validate());
            operationUiEnablementUpdaters.add(() -> uiProvider.updateUIEnablement());
            operationOptionsClasses.add(uiProvider.getOptionsClass());
            operationOptionsCreators.add(() -> uiProvider.createOptions());
            operationOptionsAppliers.add(o -> uiProvider.applyOptions(o));
            uiProvider.updateUIEnablement();
        }
    }

    private Composite addOperation(Composite parent, PostProcessingOperationProvider<?, ?> provider) {
        operationFormalNames.add(provider.getOperationFormalName());
        operationSupportsComponentFiltering.add(provider.supportsFilteredComponentsAsInput());
        lstOperation.add(provider.getOperationReadableName());

        Composite operationContainer = createComposite(parent);
        operationContainers.add(operationContainer);
        GridLayout layout = new GridLayout(INNER_COLUMNS, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        operationContainer.setLayout(layout);

        createWideLabel(operationContainer, provider.getOperationDescription());

        return operationContainer;
    }

    private void addCommonOptionsForFiltering(Composite container) {
        // Enablement updater.
        Runnable updateFilterEnablement = () -> {
            // First mode (index 0) is no filtering. Other modes apply filtering.
            txtFilterPattern.setEnabled(cmbFilterMode.getSelectionIndex() != 0);
        };

        // Add UI.
        lblFilterMode = createWideLabel(container, "Component filtering:");

        cmbFilterMode = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        cmbFilterMode.setItems(
                Arrays.stream(PostProcessingFilterMode.values()).map(x -> x.description).toArray(String[]::new));
        cmbFilterMode.setText(PostProcessingFilterMode.NONE.description);
        cmbFilterMode.setLayoutData(allColumn());
        cmbFilterMode.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            updateFilterEnablement.run();
            validate();
        }));

        lblFilterPattern = createLabel(container, "Component filter pattern:", null);
        txtFilterPattern = new Text(container, SWT.BORDER);
        txtFilterPattern.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, true, false));
        txtFilterPattern.addModifyListener(e -> validate());

        // Add UI enablement updater.
        commonOptionsUiEnablementUpdaters.add(updateFilterEnablement);

        // Add validator.
        commonOptionsValidators.add(supportsFiltering -> {
            if (cmbFilterMode.getSelectionIndex() > 0 && supportsFiltering) {
                // Index 0 is no filtering. Higher indexes do perform filtering.
                if (txtFilterPattern.getText().trim().isEmpty()) {
                    return "Enter a component filtering pattern";
                }
                try {
                    Pattern.compile(txtFilterPattern.getText().trim());
                } catch (PatternSyntaxException e) {
                    return "Invalid component filtering regular expression pattern: " + e.toString();
                }
            }
            return null;
        });

        // Add options applier.
        commonOptionsAppliers.add(o -> {
            PostProcessingOperationOptions postProcCommonOptions = (PostProcessingOperationOptions)o;
            if (postProcCommonOptions.getProvider().supportsFilteredComponentsAsInput()) {
                cmbFilterMode.setText(postProcCommonOptions.filterMode.description);
                txtFilterPattern.setText(postProcCommonOptions.filterPattern);
            }
        });

        // Add options setter.
        commonOptionsSetters.add(o -> {
            PostProcessingOperationOptions postProcCommonOptions = (PostProcessingOperationOptions)o;
            if (postProcCommonOptions.getProvider().supportsFilteredComponentsAsInput()) {
                PostProcessingFilterMode filterMode = null;
                for (PostProcessingFilterMode value: PostProcessingFilterMode.values()) {
                    if (value.description.equals(cmbFilterMode.getText())) {
                        filterMode = value;
                    }
                }
                Preconditions.checkNotNull(filterMode);
                o.setFilterMode(filterMode);

                switch (filterMode) {
                    case NONE:
                        o.setFilterPattern("");
                        break;
                    case EXCLUSION:
                    case INCLUSION:
                        o.setFilterPattern(txtFilterPattern.getText());
                        break;
                    default:
                        throw new RuntimeException("Unknown filter mode: " + filterMode);
                }
            }
        });
    }

    private void validate() {
        String errMsg = null;
        if (lstOperation.getSelectionIndex() != -1) {
            errMsg = operationValidators.get(lstOperation.getSelectionIndex()).get();
            if (errMsg == null) {
                for (int i = 0; i < commonOptionsValidators.size(); i++) {
                    errMsg = commonOptionsValidators.get(i)
                            .apply(operationSupportsComponentFiltering.get(lstOperation.getSelectionIndex()));
                    if (errMsg != null) {
                        break;
                    }
                }
            }
        }
        setErrorMessage(errMsg);
        Button okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null) {
            okButton.setEnabled(errMsg == null);
        }
    }

    @Override
    protected void okPressed() {
        // Validate options for the selected operation.
        Preconditions.checkState(operationValidators.get(lstOperation.getSelectionIndex()).get() == null);

        // Validate the common options shared by all operations.
        for (int i = 0; i < commonOptionsValidators.size(); i++) {
            Preconditions.checkState(commonOptionsValidators.get(i)
                    .apply(operationSupportsComponentFiltering.get(lstOperation.getSelectionIndex())) == null);
        }

        // Create options for the selected operation.
        options = operationOptionsCreators.get(lstOperation.getSelectionIndex()).get();

        // Set common options: filtering.
        for (Consumer<PostProcessingOperationOptions> commonOptionsSetter: commonOptionsSetters) {
            commonOptionsSetter.accept(options);
        }

        // Finish the handling of pressing OK.
        super.okPressed();
    }

    private GridData allColumn() {
        GridData ret = new GridData(SWT.FILL, SWT.TOP, true, false);
        ret.horizontalSpan = INNER_COLUMNS;
        return ret;
    }

    /**
     * Configure dialog with given initial option values. Can only be used before the dialog is shown.
     * 
     * @param options Option values to configure dialog.
     */
    public void setOptions(PostProcessingOperationOptions options) {
        this.options = options;
    }

    /**
     * @return Current option values configured in the dialog.
     */
    public PostProcessingOperationOptions getOptions() {
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

    private Composite createComposite(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        return composite;
    }
}
