package nl.tno.mids.cmi.postprocessing.status;

import java.util.List;
import nl.tno.mids.cmi.postprocessing.PostProcessingValidationResult;
import org.eclipse.escet.common.java.Lists;

@SuppressWarnings("all")
public class PostProcessingStatus {
  /**
   * Whether the model contains data, e.g. variables, guards and updates ({@code true}) or not ({@code false}).
   */
  final boolean data;
  
  /**
   * Whether the model contains tau events ({@code true}) or not ({@code false}).
   */
  final boolean tau;
  
  public PostProcessingStatus(final boolean data, final boolean tau) {
    this.data = data;
    this.tau = tau;
  }
  
  /**
   * This method validates that the given {@link PostProcessingSubset} can be applied to the receiving {@link PostProcessingStatus}.
   * The method will return a list of messages from {@link PostProcessingValidationResult} when failing. If valid,
   * the method will return an empty list, i.e. no issues.
   * 
   * @param preConditionSubset
   *            The postProcessingSubset to apply to the receiver
   * @return messages
   *            A list of messages. Empty in case of no issues.
   */
  public List<String> validate(final PostProcessingPreconditionSubset preConditionSubset) {
    final List<String> messages = Lists.<String>list();
    if ((this.dataIsPresent() && preConditionSubset.dataIsNotAllowed())) {
      messages.add(PostProcessingValidationResult.DATA_NOT_ALLOWED.message);
    }
    if ((this.dataIsNotPresent() && preConditionSubset.dataIsRequired())) {
      messages.add(PostProcessingValidationResult.DATA_REQUIRED.message);
    }
    if ((this.tauIsPresent() && preConditionSubset.tauIsNotAllowed())) {
      messages.add(PostProcessingValidationResult.TAU_NOT_ALLOWED.message);
    }
    if ((this.tauIsNotPresent() && preConditionSubset.tauIsRequired())) {
      messages.add(PostProcessingValidationResult.TAU_REQUIRED.message);
    }
    return messages;
  }
  
  public boolean dataIsPresent() {
    return this.data;
  }
  
  public boolean dataIsNotPresent() {
    return (!this.data);
  }
  
  public boolean tauIsPresent() {
    return this.tau;
  }
  
  public boolean tauIsNotPresent() {
    return (!this.tau);
  }
}
