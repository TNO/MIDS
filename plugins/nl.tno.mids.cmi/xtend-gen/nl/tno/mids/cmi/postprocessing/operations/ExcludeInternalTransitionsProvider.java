package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Preconditions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;

@SuppressWarnings("all")
public class ExcludeInternalTransitionsProvider extends PostProcessingOperationProvider<ExcludeInternalTransitions, ExcludeInternalTransitionsOptions> {
  @Override
  public String getOperationReadableName() {
    return "Exclude internal transitions";
  }
  
  @Override
  public String getOperationDescription() {
    return "Exclude internal/non-communicating transitions where possible.";
  }
  
  @Override
  public Class<ExcludeInternalTransitions> getOperationClass() {
    return ExcludeInternalTransitions.class;
  }
  
  @Override
  public Class<ExcludeInternalTransitionsOptions> getOperationOptionsClass() {
    return ExcludeInternalTransitionsOptions.class;
  }
  
  @Override
  public ExcludeInternalTransitions getOperation(final ExcludeInternalTransitionsOptions options) {
    return new ExcludeInternalTransitions(options);
  }
  
  @Override
  public ExcludeInternalTransitionsOptions getOptions(final String args) {
    Preconditions.checkArgument(args.isEmpty(), ("Invalid arguments for ExcludeInternalTransitions operation: " + args));
    return new ExcludeInternalTransitionsOptions();
  }
  
  @Override
  public String writeOptions(final ExcludeInternalTransitionsOptions options) {
    return "";
  }
  
  @Override
  public boolean supportsFilteredComponentsAsInput() {
    return true;
  }
}
