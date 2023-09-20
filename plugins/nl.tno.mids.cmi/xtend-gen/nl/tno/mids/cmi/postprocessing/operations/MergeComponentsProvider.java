package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Preconditions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;

@SuppressWarnings("all")
public class MergeComponentsProvider extends PostProcessingOperationProvider<MergeComponents, MergeComponentsOptions> {
  @Override
  public String getOperationReadableName() {
    return "Merge components";
  }
  
  @Override
  public String getOperationDescription() {
    return "Merge multiple runtime components, for instance multiple instances of the same executable, into a single runtime component.";
  }
  
  @Override
  public Class<MergeComponents> getOperationClass() {
    return MergeComponents.class;
  }
  
  @Override
  public Class<MergeComponentsOptions> getOperationOptionsClass() {
    return MergeComponentsOptions.class;
  }
  
  @Override
  public MergeComponents getOperation(final MergeComponentsOptions options) {
    return new MergeComponents(options);
  }
  
  @Override
  public MergeComponentsOptions getOptions(final String args) {
    boolean _isEmpty = args.trim().isEmpty();
    boolean _not = (!_isEmpty);
    Preconditions.checkArgument(_not, ("Invalid arguments for MergeComponents operation: " + args));
    final MergeComponentsOptions options = new MergeComponentsOptions();
    options.pattern = args.trim();
    return options;
  }
  
  @Override
  public String writeOptions(final MergeComponentsOptions options) {
    return options.pattern;
  }
  
  @Override
  public boolean supportsFilteredComponentsAsInput() {
    return true;
  }
}
