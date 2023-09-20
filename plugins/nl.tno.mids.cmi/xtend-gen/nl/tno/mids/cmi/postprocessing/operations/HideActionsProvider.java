package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Preconditions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;

@SuppressWarnings("all")
public class HideActionsProvider extends PostProcessingOperationProvider<HideActions, HideActionsOptions> {
  @Override
  public String getOperationReadableName() {
    return "Hide actions";
  }
  
  @Override
  public String getOperationDescription() {
    return "Hide actions matching the given pattern (regular expression).";
  }
  
  @Override
  public Class<HideActions> getOperationClass() {
    return HideActions.class;
  }
  
  @Override
  public Class<HideActionsOptions> getOperationOptionsClass() {
    return HideActionsOptions.class;
  }
  
  @Override
  public HideActions getOperation(final HideActionsOptions options) {
    return new HideActions(options);
  }
  
  @Override
  public HideActionsOptions getOptions(final String args) {
    boolean _isEmpty = args.trim().isEmpty();
    boolean _not = (!_isEmpty);
    Preconditions.checkArgument(_not, ("Invalid arguments for HideActions operation: " + args));
    final HideActionsOptions options = new HideActionsOptions();
    options.pattern = args.trim();
    return options;
  }
  
  @Override
  public String writeOptions(final HideActionsOptions options) {
    return options.pattern;
  }
  
  @Override
  public boolean supportsFilteredComponentsAsInput() {
    return true;
  }
}
