package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.List;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;

@SuppressWarnings("all")
public class InjectDomainKnowledgeProvider extends PostProcessingOperationProvider<InjectDomainKnowledge, InjectDomainKnowledgeOptions> {
  @Override
  public String getOperationReadableName() {
    return "Domain knowledge injection";
  }
  
  @Override
  public String getOperationDescription() {
    return "Combine inferred model and domain knowledge CIF model using a specified operator.";
  }
  
  @Override
  public Class<InjectDomainKnowledge> getOperationClass() {
    return InjectDomainKnowledge.class;
  }
  
  @Override
  public Class<InjectDomainKnowledgeOptions> getOperationOptionsClass() {
    return InjectDomainKnowledgeOptions.class;
  }
  
  @Override
  public InjectDomainKnowledge getOperation(final InjectDomainKnowledgeOptions options) {
    return new InjectDomainKnowledge(options);
  }
  
  @Override
  public InjectDomainKnowledgeOptions getOptions(final String args) {
    final InjectDomainKnowledgeOptions options = new InjectDomainKnowledgeOptions();
    final List<String> argList = Arrays.<String>asList(args.split(","));
    int _size = argList.size();
    boolean _equals = (_size == 2);
    Preconditions.checkArgument(_equals, ("Invalid arguments for InjectDomainKnowledge operation: " + args));
    options.operator = InjectDomainKnowledgeOperator.valueOf(PostProcessingOperationProvider.normalizeEnumValue(argList.get(0)));
    options.modelPath = argList.get(1).trim();
    return options;
  }
  
  @Override
  public String writeOptions(final InjectDomainKnowledgeOptions options) {
    String _displayEnumValue = PostProcessingOperationProvider.displayEnumValue(options.operator.toString());
    String _plus = (_displayEnumValue + ",");
    return (_plus + options.modelPath);
  }
  
  @Override
  public boolean supportsFilteredComponentsAsInput() {
    return true;
  }
}
