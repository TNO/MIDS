package nl.tno.mids.cmi;

import com.google.common.base.Objects;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import nl.tno.mids.cmi.postprocessing.PostProcessingFilterMode;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProviders;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.xbase.lib.CollectionExtensions;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.ExclusiveRange;
import org.eclipse.xtext.xbase.lib.Pure;

@Accessors
@SuppressWarnings("all")
public class ComponentExtractionOptions implements Cloneable {
  @Accessors
  public static class ComponentExtractionInputOptions implements Cloneable {
    /**
     * Absolute path to input TMSC from which to extract models.
     */
    private Path path;
    
    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    }
    
    @Pure
    public Path getPath() {
      return this.path;
    }
    
    public void setPath(final Path path) {
      this.path = path;
    }
  }
  
  @Accessors
  public static class ComponentExtractionOutputOptions implements Cloneable {
    /**
     * Absolute path of the output folder.
     */
    private Path path;
    
    /**
     * Kind of output produced.
     */
    private OutputMode outputMode = OutputMode.COMPONENTS;
    
    /**
     * Name of first protocol component.
     * 
     * <p>Is {@code null} if and only if {@link #outputMode} is {@link OutputMode#COMPONENTS}.</p>
     */
    private String protocolName1;
    
    /**
     * Name of second protocol component.
     * 
     * <p>Is {@code null} if and only if {@link #outputMode} is {@link OutputMode#COMPONENTS}.</p>
     */
    private String protocolName2;
    
    /**
     * Names of additional components in scope. May be empty.
     * 
     * <p>Is {@code null} if and only if {@link #outputMode} is {@link OutputMode#COMPONENTS}.</p>
     */
    private List<String> scope = CollectionLiterals.<String>newArrayList();
    
    /**
     * Whether to additionally render extracted models as yEd diagrams.
     */
    private boolean saveYed;
    
    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    }
    
    @Pure
    public Path getPath() {
      return this.path;
    }
    
    public void setPath(final Path path) {
      this.path = path;
    }
    
    @Pure
    public OutputMode getOutputMode() {
      return this.outputMode;
    }
    
    public void setOutputMode(final OutputMode outputMode) {
      this.outputMode = outputMode;
    }
    
    @Pure
    public String getProtocolName1() {
      return this.protocolName1;
    }
    
    public void setProtocolName1(final String protocolName1) {
      this.protocolName1 = protocolName1;
    }
    
    @Pure
    public String getProtocolName2() {
      return this.protocolName2;
    }
    
    public void setProtocolName2(final String protocolName2) {
      this.protocolName2 = protocolName2;
    }
    
    @Pure
    public List<String> getScope() {
      return this.scope;
    }
    
    public void setScope(final List<String> scope) {
      this.scope = scope;
    }
    
    @Pure
    public boolean isSaveYed() {
      return this.saveYed;
    }
    
    public void setSaveYed(final boolean saveYed) {
      this.saveYed = saveYed;
    }
  }
  
  @Accessors
  public static class ComponentExtractionPreProcessingOptions implements Cloneable {
    /**
     * Whether to convert events on untraced components to represent synchronously handled functions.
     */
    private boolean untracedHandleSynchronously = true;
    
    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    }
    
    @Pure
    public boolean isUntracedHandleSynchronously() {
      return this.untracedHandleSynchronously;
    }
    
    public void setUntracedHandleSynchronously(final boolean untracedHandleSynchronously) {
      this.untracedHandleSynchronously = untracedHandleSynchronously;
    }
  }
  
  @Accessors
  public static class ComponentExtractionPostProcessingOptions implements Cloneable {
    /**
     * Include components matching the given regular expression, or all components on empty string.
     */
    private String componentsInclusionRegEx = "";
    
    /**
     * Exclude components matching the given regular expression, or no exclusion on empty string.
     */
    private String componentsExclusionRegEx = "";
    
    /**
     * Apply post-processing operations after component extraction.
     */
    private List<PostProcessingOperationOptions> operations = CollectionLiterals.<PostProcessingOperationOptions>newArrayList();
    
    @Override
    public Object clone() {
      try {
        Object _clone = super.clone();
        final ComponentExtractionOptions.ComponentExtractionPostProcessingOptions rslt = ((ComponentExtractionOptions.ComponentExtractionPostProcessingOptions) _clone);
        int _size = rslt.operations.size();
        ExclusiveRange _doubleDotLessThan = new ExclusiveRange(0, _size, true);
        for (final Integer i : _doubleDotLessThan) {
          Object _clone_1 = rslt.operations.get((i).intValue()).clone();
          rslt.operations.set((i).intValue(), ((PostProcessingOperationOptions) _clone_1));
        }
        return rslt;
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    }
    
    @Pure
    public String getComponentsInclusionRegEx() {
      return this.componentsInclusionRegEx;
    }
    
    public void setComponentsInclusionRegEx(final String componentsInclusionRegEx) {
      this.componentsInclusionRegEx = componentsInclusionRegEx;
    }
    
    @Pure
    public String getComponentsExclusionRegEx() {
      return this.componentsExclusionRegEx;
    }
    
    public void setComponentsExclusionRegEx(final String componentsExclusionRegEx) {
      this.componentsExclusionRegEx = componentsExclusionRegEx;
    }
    
    @Pure
    public List<PostProcessingOperationOptions> getOperations() {
      return this.operations;
    }
    
    public void setOperations(final List<PostProcessingOperationOptions> operations) {
      this.operations = operations;
    }
  }
  
  @Accessors
  public static class ComponentExtractionExtractionOptions implements Cloneable {
    /**
     * Synchronize dependent transitions ({@code true}) or keep them unsynchronized ({@code false}).
     */
    private boolean synchronizeDependentTransitions = true;
    
    /**
     * Whether to exact a CIF model per component ({@code true}) or a single CIF model for all components
     * ({@code false}).
     */
    private boolean modelPerComponent = true;
    
    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    }
    
    @Pure
    public boolean isSynchronizeDependentTransitions() {
      return this.synchronizeDependentTransitions;
    }
    
    public void setSynchronizeDependentTransitions(final boolean synchronizeDependentTransitions) {
      this.synchronizeDependentTransitions = synchronizeDependentTransitions;
    }
    
    @Pure
    public boolean isModelPerComponent() {
      return this.modelPerComponent;
    }
    
    public void setModelPerComponent(final boolean modelPerComponent) {
      this.modelPerComponent = modelPerComponent;
    }
  }
  
  private static final String FILE_NAME = "component-extraction-options.txt";
  
  private boolean doHelp = false;
  
  private ComponentExtractionOptions.ComponentExtractionInputOptions input = new ComponentExtractionOptions.ComponentExtractionInputOptions();
  
  private ComponentExtractionOptions.ComponentExtractionOutputOptions output = new ComponentExtractionOptions.ComponentExtractionOutputOptions();
  
  private ComponentExtractionOptions.ComponentExtractionPreProcessingOptions preProcessing = new ComponentExtractionOptions.ComponentExtractionPreProcessingOptions();
  
  private ComponentExtractionOptions.ComponentExtractionExtractionOptions extraction = new ComponentExtractionOptions.ComponentExtractionExtractionOptions();
  
  private ComponentExtractionOptions.ComponentExtractionPostProcessingOptions postProcessing = new ComponentExtractionOptions.ComponentExtractionPostProcessingOptions();
  
  @Override
  public Object clone() {
    try {
      Object _clone = super.clone();
      final ComponentExtractionOptions rslt = ((ComponentExtractionOptions) _clone);
      Object _clone_1 = rslt.input.clone();
      rslt.input = ((ComponentExtractionOptions.ComponentExtractionInputOptions) _clone_1);
      Object _clone_2 = rslt.output.clone();
      rslt.output = ((ComponentExtractionOptions.ComponentExtractionOutputOptions) _clone_2);
      Object _clone_3 = rslt.preProcessing.clone();
      rslt.preProcessing = ((ComponentExtractionOptions.ComponentExtractionPreProcessingOptions) _clone_3);
      Object _clone_4 = rslt.extraction.clone();
      rslt.extraction = ((ComponentExtractionOptions.ComponentExtractionExtractionOptions) _clone_4);
      Object _clone_5 = rslt.postProcessing.clone();
      rslt.postProcessing = ((ComponentExtractionOptions.ComponentExtractionPostProcessingOptions) _clone_5);
      return rslt;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  /**
   * Create {@link ComponentExtractionOptions} instance based on command line arguments.
   * 
   * @param args Command line arguments describing configured options.
   * @return The parsed component extraction options, or {@code null} if the {@code help} argument is present.
   * @throws ParseException In case the provided arguments cannot be parsed.
   * @throws IOException In case of an I/O error.
   */
  public static ComponentExtractionOptions parse(final String[] cmiArgs) throws IOException {
    try {
      final DefaultParser parser = new DefaultParser();
      final Options options = ComponentExtractionOptions.buildOptions();
      final CommandLine line = parser.parse(options, cmiArgs);
      ComponentExtractionOptions extractionOptions = new ComponentExtractionOptions();
      boolean _hasOption = line.hasOption("options-file");
      if (_hasOption) {
        final List<String> configLines = Files.readAllLines(Paths.get(line.getOptionValue("options-file")));
        final String[] configArray = ((String[])Conversions.unwrapArray(configLines, String.class));
        final CommandLine lineFromFile = parser.parse(options, configArray);
        ComponentExtractionOptions.processLine(extractionOptions, lineFromFile);
      }
      ComponentExtractionOptions.processLine(extractionOptions, line);
      if (extractionOptions.doHelp) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("mids-cmi", options, true);
        return null;
      }
      ComponentExtractionOptions.finalize(extractionOptions);
      ComponentExtractionOptions.validate(extractionOptions);
      return extractionOptions;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  /**
   * Write options to a text file.
   * 
   * @param optionsFileOutputPath Path of folder to write output to.
   * @throws IOException In case of an I/O error.
   */
  public Path writeOptionsFile(final Path optionsFileOutputPath) {
    try {
      Path _xblockexpression = null;
      {
        ArrayList<String> lines = new ArrayList<String>();
        lines.add("-input");
        lines.add(FilenameUtils.separatorsToUnix(this.input.path.toString()));
        lines.add("-output");
        lines.add(FilenameUtils.separatorsToUnix(this.output.path.toString()));
        boolean _equals = Objects.equal(this.output.outputMode, OutputMode.PROTOCOL);
        if (_equals) {
          lines.add("-protocol");
          lines.add(((this.output.protocolName1 + ",") + this.output.protocolName2));
          boolean _isEmpty = this.output.scope.isEmpty();
          boolean _not = (!_isEmpty);
          if (_not) {
            lines.add("-protocol-scope");
            lines.add(this.output.scope.stream().collect(Collectors.joining(",")));
          }
        }
        if (this.output.saveYed) {
          lines.add("-yed");
        }
        if ((!this.preProcessing.untracedHandleSynchronously)) {
          lines.add("-no-untraced-synchronous");
        }
        if ((!this.extraction.modelPerComponent)) {
          lines.add("-single-model");
        }
        if ((!this.extraction.synchronizeDependentTransitions)) {
          lines.add("-no-sync-dependent");
        }
        boolean _isEmpty_1 = this.postProcessing.componentsExclusionRegEx.isEmpty();
        boolean _not_1 = (!_isEmpty_1);
        if (_not_1) {
          lines.add("-component-exclusion");
          lines.add(this.postProcessing.componentsExclusionRegEx);
        }
        boolean _isEmpty_2 = this.postProcessing.componentsInclusionRegEx.isEmpty();
        boolean _not_2 = (!_isEmpty_2);
        if (_not_2) {
          lines.add("-component-inclusion");
          lines.add(this.postProcessing.componentsInclusionRegEx);
        }
        for (final PostProcessingOperationOptions operationOptions : this.postProcessing.operations) {
          {
            final PostProcessingOperationProvider<PostProcessingOperation<PostProcessingOperationOptions>, PostProcessingOperationOptions> provider = operationOptions.<PostProcessingOperation<PostProcessingOperationOptions>, PostProcessingOperationOptions>getProvider();
            lines.add("-post-processing");
            final StringBuilder operationStringBuilder = new StringBuilder();
            boolean _notEquals = (!Objects.equal(operationOptions.filterMode, PostProcessingFilterMode.NONE));
            if (_notEquals) {
              operationStringBuilder.append("<");
              operationStringBuilder.append(ComponentExtractionOptions.displayEnumValue(operationOptions.filterMode.toString()));
              operationStringBuilder.append(",");
              operationStringBuilder.append(operationOptions.filterPattern);
              operationStringBuilder.append(">");
            }
            operationStringBuilder.append(provider.getOperationClass().getSimpleName());
            final String ppOptions = provider.writeOptions(operationOptions);
            boolean _isEmpty_3 = ppOptions.isEmpty();
            boolean _not_3 = (!_isEmpty_3);
            if (_not_3) {
              operationStringBuilder.append("(");
              operationStringBuilder.append(ppOptions);
              operationStringBuilder.append(")");
            }
            lines.add(operationStringBuilder.toString());
          }
        }
        final Path optionsFilePath = optionsFileOutputPath.resolve(ComponentExtractionOptions.FILE_NAME);
        Files.createDirectories(optionsFilePath.getParent());
        _xblockexpression = Files.write(optionsFilePath, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
      }
      return _xblockexpression;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  /**
   * Finalize extraction options by adding remaining defaults.
   * 
   * @param extractionOptions Compare options to finalize.
   */
  private static Path finalize(final ComponentExtractionOptions extractionOptions) {
    Path _xifexpression = null;
    if (((extractionOptions.input.path != null) && (extractionOptions.output.path == null))) {
      _xifexpression = extractionOptions.output.path = extractionOptions.input.path.resolveSibling("cmi");
    }
    return _xifexpression;
  }
  
  /**
   * Validate selected component extraction options.
   * 
   * @param extractionOptions Component extraction options to validate.
   */
  private static void validate(final ComponentExtractionOptions extractionOptions) {
    if ((extractionOptions.input.path == null)) {
      throw new RuntimeException("Missing required path to input data.");
    }
    final Path inputPath = extractionOptions.input.path;
    boolean _exists = Files.exists(inputPath);
    boolean _not = (!_exists);
    if (_not) {
      throw new RuntimeException("Input path must refer to an existing file.");
    }
    boolean _isRegularFile = Files.isRegularFile(inputPath);
    boolean _not_1 = (!_isRegularFile);
    if (_not_1) {
      throw new RuntimeException("Input path must refer to an input file, not a folder.");
    }
    final Path outputPath = extractionOptions.output.path;
    if ((Files.exists(outputPath) && (!Files.isDirectory(outputPath)))) {
      throw new RuntimeException("Output path may not refer to an existing file.");
    }
  }
  
  /**
   * Create {@link ComponentExtractionOptions} instance based on command line arguments.
   * 
   * @param extractionOptions The current component extraction options, to be updated.
   * @param line Command line with parsed arguments.
   * @throws ParseException In case the provided arguments cannot be parsed.
   */
  private static void processLine(final ComponentExtractionOptions extractionOptions, final CommandLine line) {
    try {
      boolean _hasOption = line.hasOption("help");
      if (_hasOption) {
        extractionOptions.doHelp = true;
        return;
      }
      boolean _hasOption_1 = line.hasOption("input");
      if (_hasOption_1) {
        extractionOptions.input.path = Paths.get(line.getOptionValue("input")).toAbsolutePath().normalize();
      }
      boolean _hasOption_2 = line.hasOption("output");
      if (_hasOption_2) {
        extractionOptions.output.path = Paths.get(line.getOptionValue("output")).toAbsolutePath().normalize();
      }
      boolean _hasOption_3 = line.hasOption("p");
      if (_hasOption_3) {
        extractionOptions.output.outputMode = OutputMode.PROTOCOL;
        extractionOptions.output.protocolName1 = line.getOptionValues("p")[0];
        extractionOptions.output.protocolName2 = line.getOptionValues("p")[1];
      } else {
        extractionOptions.output.outputMode = OutputMode.COMPONENTS;
      }
      boolean _hasOption_4 = line.hasOption("protocol-scope");
      if (_hasOption_4) {
        CollectionExtensions.<String>addAll(extractionOptions.output.scope, line.getOptionValues("protocol-scope"));
      }
      extractionOptions.output.saveYed = (extractionOptions.output.saveYed || line.hasOption("yed"));
      extractionOptions.preProcessing.untracedHandleSynchronously = (extractionOptions.preProcessing.untracedHandleSynchronously && (!line.hasOption("no-untraced-synchronous")));
      extractionOptions.extraction.synchronizeDependentTransitions = (extractionOptions.extraction.synchronizeDependentTransitions && (!line.hasOption("no-sync-dependent")));
      extractionOptions.extraction.modelPerComponent = (extractionOptions.extraction.modelPerComponent && 
        (!line.hasOption("single-model")));
      extractionOptions.postProcessing.componentsInclusionRegEx = line.getOptionValue("component-inclusion", 
        extractionOptions.postProcessing.componentsInclusionRegEx);
      extractionOptions.postProcessing.componentsExclusionRegEx = line.getOptionValue("component-exclusion", 
        extractionOptions.postProcessing.componentsExclusionRegEx);
      boolean _hasOption_5 = line.hasOption("post-processing");
      if (_hasOption_5) {
        final Pattern postProcessingPattern = Pattern.compile(
          "(\\<(?<filtermode>\\w*),(?<filterpattern>[^\\>]*)\\>)?(?<name>\\w*)(\\((?<args>.*)\\))?");
        final List<String> postProcessings = Arrays.<String>asList(line.getOptionValues("post-processing"));
        for (final String postProcessing : postProcessings) {
          {
            final Matcher match = postProcessingPattern.matcher(postProcessing);
            boolean _matches = match.matches();
            boolean _not = (!_matches);
            if (_not) {
              throw new ParseException(("Post-processing operation does not fit expected pattern: " + postProcessing));
            }
            final PostProcessingOperationProvider<?, ?> operationProvider = PostProcessingOperationProviders.getPostProcessingOperationProvider(match.group("name"));
            if ((operationProvider == null)) {
              String _group = match.group("name");
              String _plus = ("Unknown post-processing operation: " + _group);
              throw new ParseException(_plus);
            }
            final String argsGroup = match.group("args");
            PostProcessingOperationOptions operation = null;
            if ((argsGroup != null)) {
              operation = operationProvider.getOptions(argsGroup);
            } else {
              operation = operationProvider.getOptions("");
            }
            final String filterMode = match.group("filtermode");
            if ((filterMode != null)) {
              operation.filterMode = PostProcessingFilterMode.valueOf(ComponentExtractionOptions.normalizeEnumValue(filterMode));
              operation.filterPattern = match.group("filterpattern");
            }
            extractionOptions.postProcessing.operations.add(operation);
          }
        }
      }
      return;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  /**
   * Construct CLI options.
   * 
   * @return Constructed options object.
   */
  private static Options buildOptions() {
    final Options options = new Options();
    final Option helpOption = Option.builder("h").longOpt("help").desc("Print help information").build();
    options.addOption(helpOption);
    final Option inputTMSCOption = Option.builder("i").longOpt("input").argName("path").hasArg().desc("Input TMSC file").build();
    options.addOption(inputTMSCOption);
    final Option outputOption = Option.builder("o").longOpt("output").argName("path").hasArg().desc(
      "Output folder path").build();
    options.addOption(outputOption);
    final Option optionsFileOption = Option.builder("f").longOpt("options-file").argName("path").hasArg().desc("Options file").build();
    options.addOption(optionsFileOption);
    final Option protocolNameOption = Option.builder("p").longOpt("protocol").argName("names").hasArgs().numberOfArgs(2).valueSeparator(',').desc("Infer protocol between two components").build();
    options.addOption(protocolNameOption);
    final Option protocolScopeOption = Option.builder("ps").longOpt("protocol-scope").argName("names").hasArgs().valueSeparator(',').desc("Additional protocol scope components").build();
    options.addOption(protocolScopeOption);
    final Option saveYedOption = Option.builder("y").longOpt("yed").desc("Save yEd diagrams").build();
    options.addOption(saveYedOption);
    final Option noUntracedHandledSynchronouslyOption = Option.builder("u").longOpt("no-untraced-synchronous").desc(
      "Do not convert events on untraced components to synchronous functions").build();
    options.addOption(noUntracedHandledSynchronouslyOption);
    final Option noSynchronizeDependentOption = Option.builder("d").longOpt("no-sync-dependent").desc(
      "Do not synchronize dependent transitions").build();
    options.addOption(noSynchronizeDependentOption);
    final Option componentInclusionRegexOption = Option.builder("ci").longOpt("component-inclusion").argName("regex").hasArg().desc("Component inclusion regex").build();
    options.addOption(componentInclusionRegexOption);
    final Option componentExclusionRegexOption = Option.builder("ce").longOpt("component-exclusion").argName("regex").hasArg().desc("Component exclusion regex").build();
    options.addOption(componentExclusionRegexOption);
    final Option postProcessingOption = Option.builder("c").longOpt("post-processing").argName("operation").hasArg().desc(
      "Perform post-processing operation").build();
    options.addOption(postProcessingOption);
    final Option singleModelOption = Option.builder("s").longOpt("single-model").desc("Save single model").build();
    options.addOption(singleModelOption);
    return options;
  }
  
  /**
   * Create a user-friendly representation of an enum value.
   * 
   * @param value Enumeration value string.
   * @return {@code value} in lowercase and with {@code _} replaced by {@code -}.
   */
  private static String displayEnumValue(final String value) {
    return value.replace("_", "-").toLowerCase(Locale.US);
  }
  
  /**
   * Normalize the string representation of an enumeration value.
   * 
   * @param value Enumeration value string.
   * @return {@code value} trimmed, in uppercase and with {@code -} replaced by {@code _}.
   */
  private static String normalizeEnumValue(final String value) {
    return value.trim().replace("-", "_").toUpperCase(Locale.US);
  }
  
  @Pure
  public boolean isDoHelp() {
    return this.doHelp;
  }
  
  public void setDoHelp(final boolean doHelp) {
    this.doHelp = doHelp;
  }
  
  @Pure
  public ComponentExtractionOptions.ComponentExtractionInputOptions getInput() {
    return this.input;
  }
  
  public void setInput(final ComponentExtractionOptions.ComponentExtractionInputOptions input) {
    this.input = input;
  }
  
  @Pure
  public ComponentExtractionOptions.ComponentExtractionOutputOptions getOutput() {
    return this.output;
  }
  
  public void setOutput(final ComponentExtractionOptions.ComponentExtractionOutputOptions output) {
    this.output = output;
  }
  
  @Pure
  public ComponentExtractionOptions.ComponentExtractionPreProcessingOptions getPreProcessing() {
    return this.preProcessing;
  }
  
  public void setPreProcessing(final ComponentExtractionOptions.ComponentExtractionPreProcessingOptions preProcessing) {
    this.preProcessing = preProcessing;
  }
  
  @Pure
  public ComponentExtractionOptions.ComponentExtractionExtractionOptions getExtraction() {
    return this.extraction;
  }
  
  public void setExtraction(final ComponentExtractionOptions.ComponentExtractionExtractionOptions extraction) {
    this.extraction = extraction;
  }
  
  @Pure
  public ComponentExtractionOptions.ComponentExtractionPostProcessingOptions getPostProcessing() {
    return this.postProcessing;
  }
  
  public void setPostProcessing(final ComponentExtractionOptions.ComponentExtractionPostProcessingOptions postProcessing) {
    this.postProcessing = postProcessing;
  }
}
