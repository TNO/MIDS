package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Preconditions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.xbase.lib.Pure;

@Accessors
@SuppressWarnings("all")
public class FilterClientServerInteractionsOptions extends PostProcessingOperationOptions {
  private String componentName1 = "";
  
  private String componentName2 = "";
  
  @Override
  public void validate() throws IllegalStateException {
    boolean _isEmpty = this.componentName1.isEmpty();
    boolean _not = (!_isEmpty);
    Preconditions.checkArgument(_not);
    boolean _isEmpty_1 = this.componentName2.isEmpty();
    boolean _not_1 = (!_isEmpty_1);
    Preconditions.checkArgument(_not_1);
  }
  
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append(PostProcessingOperationProvider.getOperationFormalName(FilterClientServerInteractions.class));
    builder.append(" ");
    builder.append(this.componentName1);
    builder.append(" ");
    builder.append(this.componentName2);
    builder.append(" ");
    builder.append(super.toString());
    return builder.toString();
  }
  
  @Pure
  public String getComponentName1() {
    return this.componentName1;
  }
  
  public void setComponentName1(final String componentName1) {
    this.componentName1 = componentName1;
  }
  
  @Pure
  public String getComponentName2() {
    return this.componentName2;
  }
  
  public void setComponentName2(final String componentName2) {
    this.componentName2 = componentName2;
  }
}
