package nl.tno.mids.cmi.postprocessing;

import com.google.common.base.Preconditions;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("all")
public abstract class PostProcessingOperationProvider<T extends PostProcessingOperation<U>, U extends PostProcessingOperationOptions> {
  /**
   * Returns the formal name of the post-processing operation. Must be unique among post-processing operations.
   * Used for e.g. serialization.
   */
  public String getOperationFormalName() {
    return PostProcessingOperationProvider.getOperationFormalName(this.getOperationClass());
  }
  
  /**
   * Returns the human readable name of the post-processing operation.
   */
  public abstract String getOperationReadableName();
  
  /**
   * Returns a short description (one line) of the post-processing operation.
   */
  public abstract String getOperationDescription();
  
  /**
   * Returns {@code true} if the operation will use the filtered component list, {@code false} otherwise.
   */
  public abstract boolean supportsFilteredComponentsAsInput();
  
  /**
   * Returns the class of the operation.
   */
  public abstract Class<T> getOperationClass();
  
  /**
   * Returns the class of the operation's options.
   */
  public abstract Class<U> getOperationOptionsClass();
  
  /**
   * Returns an instance of the operation.
   * 
   * @param options The options for the operation.
   * @return The operation.
   */
  public abstract T getOperation(final U options);
  
  /**
   * Returns the options for the operation based on comma-separated values.
   * 
   * @param options The options for the operation as comma-separated string.
   * @return The options.
   */
  public abstract U getOptions(final String options);
  
  /**
   * Returns the options for the operation in the syntax of the arguments for post-processing operations of the
   * command line option value.
   * 
   * @param options The options for the operation.
   * @return The options for the operation as comma-separated string.
   */
  public abstract String writeOptions(final U options);
  
  /**
   * Returns an instance of the operation.
   * 
   * @param options The options for the operation.
   * @return The operation.
   */
  public T getOperationFromRawOptions(final PostProcessingOperationOptions options) {
    return this.getOperation(((U) options));
  }
  
  public static String getOperationFormalName(final Class<?> operationClass) {
    Preconditions.checkArgument(PostProcessingOperation.class.isAssignableFrom(operationClass));
    String name = operationClass.getSimpleName();
    name = StringUtils.uncapitalize(name);
    return name;
  }
  
  /**
   * Create a user-friendly representation of an enum value.
   * 
   * @param value Enumeration value string.
   * @return {@code value} in lowercase and with {@code _} replaced by {@code -}.
   */
  protected static String displayEnumValue(final String value) {
    return value.replace("_", "-").toLowerCase(Locale.US);
  }
  
  /**
   * Normalize the string representation of an enumeration value.
   * 
   * @param value Enumeration value string.
   * @return {@code value} trimmed, in uppercase and with {@code -} replaced by {@code _}.
   */
  public static String normalizeEnumValue(final String value) {
    return value.trim().replace("-", "_").toUpperCase(Locale.US);
  }
}
