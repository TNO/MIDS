package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Preconditions;
import java.util.Locale;
import nl.tno.mids.cif.extensions.mrr.cif.MrrToCifMode;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.xbase.lib.Pure;

@Accessors
@SuppressWarnings("all")
public class ModifyRepetitionsOptions extends PostProcessingOperationOptions {
  /**
   * Whether to make repetitions infinite.
   */
  public boolean makeInfinite;
  
  /**
   * Restrict number of repetitions, if number of repetitions at least this value. {@code 0} for no lower bound.
   * Must not be enabled if {@link #makeInfinite} is enabled.
   * Must not be enabled if {@link #maxRepeats} is disabled.
   */
  public int lowerThreshold;
  
  /**
   * Restrict number of repetitions, if number of repetitions at most this value. {@code 0} for no upper bound.
   * Must not be enabled if {@link #makeInfinite} is enabled.
   * Must not be enabled if {@link #maxRepeats} is disabled.
   */
  public int upperThreshold;
  
  /**
   * Restrict number of repetitions to this value. {@code 0} to disable.
   * Must not be enabled if {@link #makeInfinite} is enabled.
   */
  public int maxRepeats;
  
  /**
   * How to modify the repetitions.
   */
  public MrrToCifMode mode = MrrToCifMode.PLAIN;
  
  @Override
  public void validate() throws IllegalStateException {
    Preconditions.checkState((!((this.upperThreshold > 0) && (this.lowerThreshold > this.upperThreshold))));
    Preconditions.checkState((!((this.maxRepeats > 0) && this.makeInfinite)));
  }
  
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append(PostProcessingOperationProvider.getOperationFormalName(ModifyRepetitions.class));
    builder.append(" ");
    builder.append(this.mode.toString().toLowerCase(Locale.US));
    if (this.makeInfinite) {
      builder.append(" makeInfinite");
    } else {
      if ((this.maxRepeats > 0)) {
        builder.append(" maxRepeats ");
        builder.append(this.maxRepeats);
        if ((this.lowerThreshold > 0)) {
          builder.append(" lowerThreshold ");
          builder.append(this.lowerThreshold);
        }
        if ((this.upperThreshold > 0)) {
          builder.append(" upperThreshold ");
          builder.append(this.upperThreshold);
        }
      }
    }
    builder.append(" ");
    builder.append(super.toString());
    return builder.toString();
  }
  
  @Pure
  public boolean isMakeInfinite() {
    return this.makeInfinite;
  }
  
  public void setMakeInfinite(final boolean makeInfinite) {
    this.makeInfinite = makeInfinite;
  }
  
  @Pure
  public int getLowerThreshold() {
    return this.lowerThreshold;
  }
  
  public void setLowerThreshold(final int lowerThreshold) {
    this.lowerThreshold = lowerThreshold;
  }
  
  @Pure
  public int getUpperThreshold() {
    return this.upperThreshold;
  }
  
  public void setUpperThreshold(final int upperThreshold) {
    this.upperThreshold = upperThreshold;
  }
  
  @Pure
  public int getMaxRepeats() {
    return this.maxRepeats;
  }
  
  public void setMaxRepeats(final int maxRepeats) {
    this.maxRepeats = maxRepeats;
  }
  
  @Pure
  public MrrToCifMode getMode() {
    return this.mode;
  }
  
  public void setMode(final MrrToCifMode mode) {
    this.mode = mode;
  }
}
