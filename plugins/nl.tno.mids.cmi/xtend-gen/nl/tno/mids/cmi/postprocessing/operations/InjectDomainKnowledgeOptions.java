package nl.tno.mids.cmi.postprocessing.operations;

import java.util.Locale;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.xbase.lib.Pure;

@Accessors
@SuppressWarnings("all")
public class InjectDomainKnowledgeOptions extends PostProcessingOperationOptions {
  /**
   * The path to the model to inject.
   */
  public String modelPath;
  
  /**
   * The operator to use for the injection.
   */
  public InjectDomainKnowledgeOperator operator;
  
  @Override
  public void validate() throws IllegalStateException {
  }
  
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append(PostProcessingOperationProvider.getOperationFormalName(InjectDomainKnowledge.class));
    builder.append(" ");
    builder.append(this.operator.name().toLowerCase(Locale.US));
    builder.append(" ");
    builder.append(this.modelPath);
    builder.append(" ");
    builder.append(super.toString());
    return builder.toString();
  }
  
  @Pure
  public String getModelPath() {
    return this.modelPath;
  }
  
  public void setModelPath(final String modelPath) {
    this.modelPath = modelPath;
  }
  
  @Pure
  public InjectDomainKnowledgeOperator getOperator() {
    return this.operator;
  }
  
  public void setOperator(final InjectDomainKnowledgeOperator operator) {
    this.operator = operator;
  }
}
