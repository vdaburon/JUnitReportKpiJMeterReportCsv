package io.github.vdaburon.jmeter.utils.reportkpi;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.apache.commons.csv.CSVRecord;
import org.w3c.dom.Document;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

public class JUnitReportFromJMReportCsv {

    private static final Logger LOGGER = Logger.getLogger(JUnitReportFromJMReportCsv.class.getName());
    public static final int K_RETURN_OK = 0;
    public static final int K_RETURN_KO = 1;
    public static final String K_JUNIT_XML_FILE_DEFAULT = "jmeter-junit-plugin-jmreport.xml";
    public static final String K_CVS_JM_REPORT_OPT = "csvJMReport";
    public static final String K_CSV_LABEL_COLUMN_NAME_OPT = "csvLabelColumnName";
    public static final String K_KPI_FILE_OPT = "kpiFile";
    public static final String K_JUNIT_XML_FILE_OPT = "junitFile";
    public static final String K_EXIT_RETURN_ON_FAIL_OPT = "exitReturnOnFail";


    // column name for the kpi csv file
    public static final String K_CSV_COL_NAME_KPI = "name_kpi";
    public static final String K_CSV_COL_METRIC_CSV_COLUM_NAME = "metric_csv_column_name";
    public static final String K_CSV_COL_LABEL_REGEX = "label_regex";
    public static final String K_CSV_COL_COMPARATOR = "comparator";
    public static final String K_CSV_COL_THREASHOLD = "threshold";


    // column name Label in jmeter csv report
    public static final String K_CSV_JMREPORT_COL_LABEL_DEFAULT = "Label";

    public static final int K_FAIL_MESSAGE_SIZE_MAX = 1024;

    public static void main(String[] args) {
        long startTimeMs = System.currentTimeMillis();

        Options options = createOptions();
        Properties parseProperties = null;

        try {
            parseProperties = parseOption(options, args);
        } catch (ParseException ex) {
            helpUsage(options);
            System.exit(K_RETURN_KO);
        }
        int exitReturn = K_RETURN_KO;

        String csvJmeterReport = "NOT SET";
        String csvLabelColumnName = K_CSV_JMREPORT_COL_LABEL_DEFAULT;
        String kpiFile = "NOT SET";
        String junitFile = K_JUNIT_XML_FILE_DEFAULT;
        boolean exitOnFailKpi = false;

        String sTmp;
        sTmp = (String) parseProperties.get(K_CVS_JM_REPORT_OPT);
        if (sTmp != null) {
            csvJmeterReport = sTmp;
        }

        sTmp = (String) parseProperties.get(K_CSV_LABEL_COLUMN_NAME_OPT);
        if (sTmp != null) {
            csvLabelColumnName = sTmp;
        }

        sTmp = (String) parseProperties.get(K_KPI_FILE_OPT);
        if (sTmp != null) {
            kpiFile = sTmp;
        }

        sTmp = (String) parseProperties.get(K_JUNIT_XML_FILE_OPT);
        if (sTmp != null) {
            junitFile = sTmp;
        }

        sTmp = (String) parseProperties.get(K_EXIT_RETURN_ON_FAIL_OPT);
        if (sTmp != null) {
            exitOnFailKpi = Boolean.parseBoolean(sTmp);
            LOGGER.fine("exitOnFailKpi:" + exitOnFailKpi);
        }
        boolean isKpiFail = false;
        LOGGER.info("Parameters CLI:" + parseProperties);
        try {
            isKpiFail = analyseCsvJMReportWithKpiRules(csvJmeterReport,csvLabelColumnName, kpiFile, junitFile);
            LOGGER.info("isKpiFail=" + isKpiFail);
        } catch (Exception ex) {
            LOGGER.warning(ex.toString());
            exitReturn = K_RETURN_KO;
        }
        if (exitOnFailKpi && isKpiFail) {
            // at least one kpi rule failure => exit 1
            exitReturn = K_RETURN_KO;
            LOGGER.info("exitOnFailKpi=" + exitOnFailKpi + " and isKpiFail=" + isKpiFail + " set program exit=" + exitReturn);
        } else {
            exitReturn = K_RETURN_OK;
        }
        long endTimeMs = System.currentTimeMillis();
        LOGGER.info("Duration ms=" + (endTimeMs - startTimeMs));
        LOGGER.info("End main (exit " + exitReturn + ")");

        System.exit(exitReturn);
    }

    /**
     * Analyse the kpi verifications on JMeter report values
     * @param csvJmeterReport the JMeter Report CSV format
     * @param csvLabelColumnName the Label Column Name (default : Label)
     * @param kpiFile the kpi contains kpi declaration
     * @param junitFile the JUnit XML out file to create
     * @return is Fail true or false, a kpi is fail or not
     * @throws IOException file exception
     * @throws ParserConfigurationException error reading csv file
     * @throws TransformerException error writing JUnit XML file
     */
    private static boolean analyseCsvJMReportWithKpiRules(String csvJmeterReport, String csvLabelColumnName, String kpiFile, String junitFile) throws IOException, ParserConfigurationException, TransformerException {
        boolean isFail = false;
        List<CSVRecord> csvJMReportLines = UtilsCsvFile.readCsvFile(csvJmeterReport);
        List<CSVRecord> csvKpiLines = UtilsCsvFile.readCsvFile(kpiFile);

        Document document = UtilsJUnitXml.createJUnitRootDocument();
        for (int i = 0; i < csvKpiLines.size(); i++) {
            CheckKpiResult checkKpiResult = verifyKpi(csvKpiLines.get(i), csvJMReportLines, csvLabelColumnName);
            if (checkKpiResult.isKpiFail()) {
                isFail = true;
                String className = checkKpiResult.getMetricCsvColumnName() + " (" + checkKpiResult.getLabelRegex() + ") " + checkKpiResult.getComparator() + " "  + checkKpiResult.getThreshold();
                UtilsJUnitXml.addTestCaseFailure(document,checkKpiResult.getNameKpi(), className, checkKpiResult.getFailMessage());
            } else {
                String className = checkKpiResult.getMetricCsvColumnName() + " (" + checkKpiResult.getLabelRegex() + ") " + checkKpiResult.getComparator() + " "  + checkKpiResult.getThreshold();
                UtilsJUnitXml.addTestCaseOk(document,checkKpiResult.getNameKpi(), className);
            }
        }
        LOGGER.info("Write junitFile=" + junitFile);
        UtilsJUnitXml.saveXmlInFile(document, junitFile);
        return isFail;
    }

    /**
     * verify one kpi for lines in csv JMeter Report
     * @param recordKpiLine a kpi line to verify
     * @param csvJMReportLines all lines in JMeter Report
     * @param csvLabelColumnName the Label Column name in the JMeter Report (usually : Label)
     * @return the result of the kpi verification and the failure message if kpi fail
     */
    private static CheckKpiResult verifyKpi(CSVRecord recordKpiLine, List<CSVRecord> csvJMReportLines, String csvLabelColumnName) {
        CheckKpiResult checkKpiResult = new CheckKpiResult();
        String nameKpi = recordKpiLine.get(K_CSV_COL_NAME_KPI);
        checkKpiResult.setNameKpi(nameKpi.trim());

        String metricCsvColumnName = recordKpiLine.get(K_CSV_COL_METRIC_CSV_COLUM_NAME);
        checkKpiResult.setMetricCsvColumnName(metricCsvColumnName.trim());

        String labelRegex = recordKpiLine.get(K_CSV_COL_LABEL_REGEX);
        checkKpiResult.setLabelRegex(labelRegex);

        String comparator = recordKpiLine.get(K_CSV_COL_COMPARATOR);
        checkKpiResult.setComparator(comparator.trim());

        String threshold = recordKpiLine.get(K_CSV_COL_THREASHOLD);
        checkKpiResult.setThreshold(threshold.trim());

        checkKpiResult.setKpiFail(false);
        checkKpiResult.setFailMessage("NOT SET");

        Pattern patternRegex = Pattern.compile(labelRegex) ;

        boolean isFailKpi = false;
        boolean isFirstFail = true;
        for (int i = 0; i < csvJMReportLines.size(); i++) {
            CSVRecord recordJMReportLine = csvJMReportLines.get(i);
            String label = recordJMReportLine.get(csvLabelColumnName);
            Matcher matcherRegex = patternRegex.matcher(label) ;
            if (matcherRegex.matches()) {
                String sMetric = recordJMReportLine.get(metricCsvColumnName);
                LOGGER.fine("sMetric=<" + sMetric + ">");
                double dMetric = 0;
                if (sMetric.contains("%")) {
                    sMetric = sMetric.replace('%', ' ');
                    dMetric = Double.parseDouble(sMetric);
                    dMetric = dMetric / 100;
                } else {
                    dMetric = Double.parseDouble(sMetric);
                }
                LOGGER.fine("dMetric=<" + dMetric + ">");

                String sThreshold = checkKpiResult.getThreshold();
                sThreshold = sThreshold.replace('%', ' ');
                double dThreshold = Double.parseDouble(sThreshold);

                String sComparator = checkKpiResult.getComparator();
                switch (sComparator) {
                    case "<":
                        if (dMetric < dThreshold) {
                            LOGGER.fine(dMetric + sComparator + dThreshold);
                        } else {
                            isFailKpi = true;
                            if (isFirstFail) {
                                isFirstFail = false;
                                String failMessage = "Actual value " +  dMetric + " exceeds threshold " + dThreshold + " for samples matching \"" + labelRegex + "\"; fail label(s) \"" + label + "\""; // Actual value 2908,480000 exceeds threshold 2500,000000 for samples matching "@SC01_P03_DUMMY"
                                checkKpiResult.setKpiFail(true);
                                checkKpiResult.setFailMessage(failMessage);
                            } else {
                                String failMessage = checkKpiResult.getFailMessage();
                                if ((failMessage.length() + label.length()) < K_FAIL_MESSAGE_SIZE_MAX) {
                                    failMessage += ", \"" + label + "\"";
                                } else {
                                    if (!failMessage.endsWith(" ...")) {
                                        failMessage += " ...";
                                    }
                                }
                                checkKpiResult.setFailMessage(failMessage);
                            }
                        }
                        break;
                    case "<=":
                        if (dMetric <= dThreshold) {
                            LOGGER.fine(dMetric + sComparator + dThreshold);
                        } else {
                            isFailKpi = true;
                            if (isFirstFail) {
                                isFirstFail = false;
                                String failMessage = "Actual value " + dMetric + " exceeds or equals threshold " + dThreshold + " for samples matching \"" + labelRegex + "\"; fail label(s) \"" + label + "\"";
                                checkKpiResult.setKpiFail(true);
                                checkKpiResult.setFailMessage(failMessage);
                            } else {
                                String failMessage = checkKpiResult.getFailMessage();
                                if ((failMessage.length() + label.length()) < K_FAIL_MESSAGE_SIZE_MAX) {
                                    failMessage += ", \"" + label + "\"";
                                } else {
                                    if (!failMessage.endsWith(" ...")) {
                                        failMessage += " ...";
                                    }
                                }
                                checkKpiResult.setFailMessage(failMessage);
                            }
                        }
                        break;
                    case ">":
                        if (dMetric > dThreshold) {
                            LOGGER.fine(dMetric + sComparator + dThreshold);
                        } else {
                            isFailKpi = true;
                            if (isFirstFail) {
                                isFirstFail = false;
                                String failMessage = "Actual value " + dMetric + " is less then threshold " + dThreshold + " for samples matching \"" + labelRegex + "\"; fail label(s) \"" + label + "\"";
                                checkKpiResult.setKpiFail(true);
                                checkKpiResult.setFailMessage(failMessage);
                            } else {
                                String failMessage = checkKpiResult.getFailMessage();
                                if ((failMessage.length() + label.length()) < K_FAIL_MESSAGE_SIZE_MAX) {
                                    failMessage += ", \"" + label + "\"";
                                } else {
                                    if (!failMessage.endsWith(" ...")) {
                                        failMessage += " ...";
                                    }
                                }
                                checkKpiResult.setFailMessage(failMessage);
                            }
                        }
                        break;
                    case ">=":
                        if (dMetric >= dThreshold) {
                            LOGGER.fine(dMetric + sComparator + dThreshold);
                        } else {
                            isFailKpi = true;
                            if (isFirstFail) {
                                isFirstFail = false;
                                String failMessage = "Actual value " + dMetric + "is less or equals threshold " + dThreshold + " for samples matching \"" + labelRegex + "\"; fail label(s) \"" + label + "\"";
                                checkKpiResult.setKpiFail(true);
                                checkKpiResult.setFailMessage(failMessage);
                            } else {
                                String failMessage = checkKpiResult.getFailMessage();
                                if ((failMessage.length() + label.length()) < K_FAIL_MESSAGE_SIZE_MAX) {
                                    failMessage += ", \"" + label + "\"";
                                } else {
                                    if (!failMessage.endsWith(" ...")) {
                                        failMessage += " ...";
                                    }
                                }
                                checkKpiResult.setFailMessage(failMessage);
                            }
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid comparator:" + sComparator);
                }
            }
        }
        return checkKpiResult;
    }

    /**
     * If incorrect parameter or help, display usage
     * @param options  options and cli parameters
     */
    private static void helpUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        String footer = "E.g : java -jar junit-reporter-kpi-from-jmeter-report-csv-<version>-jar-with-dependencies.jar -" + K_CVS_JM_REPORT_OPT + " summary.csv  -" +
                K_KPI_FILE_OPT + " kpi.csv -" + K_EXIT_RETURN_ON_FAIL_OPT + " true\n";
        footer += "or more parameters : java -jar junit-reporter-kpi-from-jmeter-report-csv-<version>-jar-with-dependencies.jar -" + K_CVS_JM_REPORT_OPT + " AggregateReport.csv  -"
                + K_CSV_LABEL_COLUMN_NAME_OPT + " Label -" + K_KPI_FILE_OPT + " kpi_check.csv -" + K_JUNIT_XML_FILE_OPT + " junit.xml -" + K_EXIT_RETURN_ON_FAIL_OPT + " true\n";
        formatter.printHelp(140, JUnitReportFromJMReportCsv.class.getName(),
                JUnitReportFromJMReportCsv.class.getName(), options, footer, true);
    }

    /**
     * Parse options enter in command line interface
     * @param optionsP parameters to parse
     * @param args parameters from cli
     * @return properties saved
     * @throws ParseException parsing error
     * @throws MissingOptionException mandatory parameter not set
     */
    private static Properties parseOption(Options optionsP, String[] args) throws ParseException, MissingOptionException {

        Properties properties = new Properties();

        CommandLineParser parser = new DefaultParser();

        // parse the command line arguments

        CommandLine line = parser.parse(optionsP, args);

        if (line.hasOption("help")) {
            properties.setProperty("help", "help value");
            return properties;
        }

        if (line.hasOption(K_CVS_JM_REPORT_OPT)) {
            properties.setProperty(K_CVS_JM_REPORT_OPT, line.getOptionValue(K_CVS_JM_REPORT_OPT));
        }

        if (line.hasOption(K_CSV_LABEL_COLUMN_NAME_OPT)) {
            properties.setProperty(K_CSV_LABEL_COLUMN_NAME_OPT, line.getOptionValue(K_CSV_LABEL_COLUMN_NAME_OPT));
        }

        if (line.hasOption(K_KPI_FILE_OPT)) {
            properties.setProperty(K_KPI_FILE_OPT, line.getOptionValue(K_KPI_FILE_OPT));
        }

        if (line.hasOption(K_JUNIT_XML_FILE_OPT)) {
            properties.setProperty(K_JUNIT_XML_FILE_OPT, line.getOptionValue(K_JUNIT_XML_FILE_OPT));
        }

        if (line.hasOption(K_EXIT_RETURN_ON_FAIL_OPT)) {
            properties.setProperty(K_EXIT_RETURN_ON_FAIL_OPT, line.getOptionValue(K_EXIT_RETURN_ON_FAIL_OPT));
        }

        return properties;
    }
    /**
     * Options or parameters for the command line interface
     * @return all options
     **/
    private static Options createOptions() {
        Options options = new Options();

        Option helpOpt = Option.builder("help").hasArg(false).desc("Help and show parameters").build();

        options.addOption(helpOpt);

        Option csvJmeterReportFileOpt = Option.builder(K_CVS_JM_REPORT_OPT).argName(K_CVS_JM_REPORT_OPT)
                .hasArg(true)
                .required(true)
                .desc("JMeter report csv file (E.g : summary.csv)")
                .build();
        options.addOption(csvJmeterReportFileOpt);

        Option csvLabelColumnNameOpt = Option.builder(K_CSV_LABEL_COLUMN_NAME_OPT).argName(K_CSV_LABEL_COLUMN_NAME_OPT)
                .hasArg(true)
                .required(false)
                .desc("Label Column Name in CSV JMeter Report (Default : " + K_CSV_JMREPORT_COL_LABEL_DEFAULT + ")")
                .build();
        options.addOption(csvLabelColumnNameOpt);

        Option kpiFileOpt = Option.builder(K_KPI_FILE_OPT).argName(K_KPI_FILE_OPT)
                .hasArg(true)
                .required(true)
                .desc("KPI file contains rule to check (E.g : kpi.csv)")
                .build();
        options.addOption(kpiFileOpt);

        Option junitXmlOutOpt = Option.builder(K_JUNIT_XML_FILE_OPT).argName(K_JUNIT_XML_FILE_OPT)
                .hasArg(true)
                .required(false)
                .desc("junit file name out (Default : " + K_JUNIT_XML_FILE_DEFAULT + ")")
                .build();
        options.addOption(junitXmlOutOpt);

        Option exitReturnOnFailOpt = Option.builder(K_EXIT_RETURN_ON_FAIL_OPT).argName(K_EXIT_RETURN_ON_FAIL_OPT)
                .hasArg(true)
                .required(false)
                .desc("if true then when kpi fail then create JUnit XML file and program return exit 1 (KO); if false [Default] then create JUnit XML File and exit 0 (OK)")
                .build();
        options.addOption(exitReturnOnFailOpt);

        return options;
    }
}
