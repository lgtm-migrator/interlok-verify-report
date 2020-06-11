package com.adaptris.labs.verify;

import com.adaptris.labs.verify.report.sonar.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CreateVerifyReport {

  private final Options options;
  private final Options helpOnlyOptions;

  private static final String HELP_ARG = "help";
  private static final String REPORT_FILE_ARG = "reportFile";
  private static final String OUTPUT_FILE_ARG = "outputFile";
  private static final String ENGINE_ID_ARG = "engineId";
  private static final String RULE_ID_PREFIX_ARG = "ruleIdPrefix";
  private static final String LOCATION_FILE_PATH_ARG = "locationFilePath";
  private static final String INC_ONLY_LINES_STARTING = "includeOnlyLinesStarting";
  private static final String REMOVE_LINE_STARTING_STRING = "removeIncludeOnlyLineFilter";

  private static final String ENGINE_ID_DEFAULT = "interlokVerify";
  private static final String RULE_ID_PREFIX_DEFAULT = "rule";
  private static final String LOCATION_FILE_PATH_DEFAULT = "./src/main/interlok/config/adapter.xml";
  private static final String INC_ONLY_LINES_STARTING_DEFAULT = "";
  private static final String REMOVE_LINE_STARTING_STRING_DEFAULT = "true";

  CreateVerifyReport(){
    options = new Options();
    Option help = new Option("h", HELP_ARG, false, "Displays this..");
    options.addOption(help);
    options.addRequiredOption("f", REPORT_FILE_ARG, true, "(required) The verify report file");
    options.addRequiredOption("o", OUTPUT_FILE_ARG, true, "(required) The output file");
    options.addOption("e", ENGINE_ID_ARG, true, String.format("The engine id (default: %s)", ENGINE_ID_DEFAULT));
    options.addOption("r", RULE_ID_PREFIX_ARG, true, String.format("The rule id prefix (default: %s)", RULE_ID_PREFIX_DEFAULT));
    options.addOption("l", LOCATION_FILE_PATH_ARG, true, String.format("The location file path (default: %s)", LOCATION_FILE_PATH_DEFAULT));
    options.addOption("i", INC_ONLY_LINES_STARTING, true, String.format("Include only lines starting with input (default: %s)", INC_ONLY_LINES_STARTING_DEFAULT));
    options.addOption("s", REMOVE_LINE_STARTING_STRING, true, String.format("Remove include only line filter (default: %s)", REMOVE_LINE_STARTING_STRING_DEFAULT));

    helpOnlyOptions = new Options();
    helpOnlyOptions.addOption(help);
  }

  public static void main(String[] args) throws Exception {
    CreateVerifyReport report = new CreateVerifyReport();
    report.run(args);
  }

  void run(String[] args) throws IOException, ParseException {
    ArgumentWrapper argumentWrapper = parseArguments(args);
    if(argumentWrapper != null) {
      ObjectMapper mapper = new ObjectMapper();
      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      Issues issues = createIssues(argumentWrapper, readFile(argumentWrapper.getReportFile(), StandardCharsets.UTF_8));
      mapper.writerWithDefaultPrettyPrinter().writeValue(new File(argumentWrapper.getOutputFile()), issues);
    }
  }

  ArgumentWrapper parseArguments(String[] args) throws ParseException {
    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine helpCommandLine = parser.parse(helpOnlyOptions, args, true);
      if(helpCommandLine.hasOption(HELP_ARG)){
        usage();
        return null;
      }
      CommandLine line = parser.parse(options, args);
      return new ArgumentWrapper(
        line.getOptionValue(ENGINE_ID_ARG, ENGINE_ID_DEFAULT),
        line.getOptionValue(RULE_ID_PREFIX_ARG, RULE_ID_PREFIX_DEFAULT),
        line.getOptionValue(LOCATION_FILE_PATH_ARG, LOCATION_FILE_PATH_DEFAULT),
        line.getOptionValue(REPORT_FILE_ARG),
        line.getOptionValue(OUTPUT_FILE_ARG),
        line.getOptionValue(INC_ONLY_LINES_STARTING, INC_ONLY_LINES_STARTING_DEFAULT),
        Boolean.parseBoolean(line.getOptionValue(REMOVE_LINE_STARTING_STRING, REMOVE_LINE_STARTING_STRING_DEFAULT))
      );
    } catch (ParseException e) {
      usage();
      throw e;
    }
  }

  Issues createIssues(ArgumentWrapper argumentWrapper, String report) {
    List<Issue> issueList = new ArrayList<>();
    try(Scanner scanner = new Scanner(report)) {
      int i = 1;
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (line.startsWith(argumentWrapper.getIncludeOnlyLinesStarting())) {
          issueList.add(new Issue(
            argumentWrapper.getEngineId(),
            String.format("%s%s", argumentWrapper.getRuleIdPrefix(), i++),
            Severity.INFO,
            Type.CODE_SMELL,
            new Location(
              argumentWrapper.getRemoveIncludeOnlyLineFilter() ? line.substring(argumentWrapper.getIncludeOnlyLinesStarting().length()) : line,
              argumentWrapper.getLocationFilePath())
          ));
        }
      }
    }
    return new Issues(issueList);
  }

  private void usage(){
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("interlok-verify-report", options);
  }

  private String readFile(String path, Charset encoding) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }

  @AllArgsConstructor
  static class ArgumentWrapper {

    @Getter
    private final String engineId;

    @Getter
    private final String ruleIdPrefix;

    @Getter
    private final String locationFilePath;

    @Getter
    private final String reportFile;

    @Getter
    private final String outputFile;

    @Getter
    private final String includeOnlyLinesStarting;

    @Getter
    private final boolean removeIncludeOnlyLineFilter;
  }

}
