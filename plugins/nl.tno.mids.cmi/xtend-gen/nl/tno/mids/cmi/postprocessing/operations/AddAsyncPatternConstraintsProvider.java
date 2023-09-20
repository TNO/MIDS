package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Preconditions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;

@SuppressWarnings("all")
public class AddAsyncPatternConstraintsProvider extends PostProcessingOperationProvider<AddAsyncPatternConstraints, AddAsyncPatternConstraintsOptions> {
  @Override
  public String getOperationReadableName() {
    return "Add asynchronous pattern constraints";
  }
  
  @Override
  public String getOperationDescription() {
    return "Add constraints to the models to enforce asynchronous patterns (e.g. requests/replies).";
  }
  
  @Override
  public Class<AddAsyncPatternConstraints> getOperationClass() {
    return AddAsyncPatternConstraints.class;
  }
  
  @Override
  public Class<AddAsyncPatternConstraintsOptions> getOperationOptionsClass() {
    return AddAsyncPatternConstraintsOptions.class;
  }
  
  @Override
  public AddAsyncPatternConstraints getOperation(final AddAsyncPatternConstraintsOptions options) {
    return new AddAsyncPatternConstraints(options);
  }
  
  @Override
  public AddAsyncPatternConstraintsOptions getOptions(final String args) {
    Preconditions.checkArgument(args.isEmpty(), ("Invalid arguments for AddAsyncPatternConstraints operation: " + args));
    return new AddAsyncPatternConstraintsOptions();
  }
  
  @Override
  public String writeOptions(final AddAsyncPatternConstraintsOptions options) {
    return "";
  }
  
  @Override
  public boolean supportsFilteredComponentsAsInput() {
    return true;
  }
}
