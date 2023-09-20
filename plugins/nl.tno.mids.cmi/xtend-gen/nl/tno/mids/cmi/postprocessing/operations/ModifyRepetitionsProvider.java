package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.List;
import nl.tno.mids.cif.extensions.mrr.cif.MrrToCifMode;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;

@SuppressWarnings("all")
public class ModifyRepetitionsProvider extends PostProcessingOperationProvider<ModifyRepetitions, ModifyRepetitionsOptions> {
  @Override
  public String getOperationReadableName() {
    return "Modify repetitions";
  }
  
  @Override
  public String getOperationDescription() {
    return "Change the repetition count of detected repetitions and/or represent them differently.";
  }
  
  @Override
  public Class<ModifyRepetitions> getOperationClass() {
    return ModifyRepetitions.class;
  }
  
  @Override
  public Class<ModifyRepetitionsOptions> getOperationOptionsClass() {
    return ModifyRepetitionsOptions.class;
  }
  
  @Override
  public ModifyRepetitions getOperation(final ModifyRepetitionsOptions options) {
    return new ModifyRepetitions(options);
  }
  
  @Override
  public ModifyRepetitionsOptions getOptions(final String args) {
    final ModifyRepetitionsOptions options = new ModifyRepetitionsOptions();
    final List<String> argList = Arrays.<String>asList(args.split(","));
    int _size = argList.size();
    boolean _equals = (_size == 5);
    Preconditions.checkArgument(_equals, ("Invalid arguments for ModifyRepetitions operation: " + args));
    options.mode = MrrToCifMode.valueOf(PostProcessingOperationProvider.normalizeEnumValue(argList.get(0)));
    options.lowerThreshold = (Integer.valueOf(argList.get(1).trim())).intValue();
    options.upperThreshold = (Integer.valueOf(argList.get(2).trim())).intValue();
    options.makeInfinite = (Boolean.valueOf(argList.get(3).trim())).booleanValue();
    options.maxRepeats = (Integer.valueOf(argList.get(4).trim())).intValue();
    return options;
  }
  
  @Override
  public String writeOptions(final ModifyRepetitionsOptions options) {
    String _displayEnumValue = PostProcessingOperationProvider.displayEnumValue(options.mode.toString());
    String _plus = (_displayEnumValue + ",");
    String _plus_1 = (_plus + Integer.valueOf(options.lowerThreshold));
    String _plus_2 = (_plus_1 + ",");
    String _plus_3 = (_plus_2 + Integer.valueOf(options.upperThreshold));
    String _plus_4 = (_plus_3 + 
      ",");
    String _plus_5 = (_plus_4 + Boolean.valueOf(options.makeInfinite));
    String _plus_6 = (_plus_5 + ",");
    return (_plus_6 + Integer.valueOf(options.maxRepeats));
  }
  
  @Override
  public boolean supportsFilteredComponentsAsInput() {
    return true;
  }
}
