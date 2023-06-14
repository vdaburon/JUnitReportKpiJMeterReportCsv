package io.github.vdaburon.jmeter.utils.reportkpi;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.template.TemplateException;
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

    // System.exit return
    public static final int K_RETURN_OK = 0;
    public static final int K_RETURN_KO = 1;

    // Defaut JUnit XML Report file
    public static final String K_JUNIT_XML_FILE_DEFAULT = "TEST-jmeter-junit-plugin-jmreport.xml";

    // OPTions for Command Line Parameters
    public static final String K_CVS_JM_REPORT_OPT = "csvJMReport";
    public static final String K_CSV_LABEL_COLUMN_NAME_OPT = "csvLabelColumnName";
    public static final String K_KPI_FILE_OPT = "kpiFile";
    public static final String K_JUNIT_XML_FILE_OPT = "junitFile";
    public static final String K_OUT_HTML_FILE_OPT = "htmlOutFile";
    public static final String K_OUT_DIV_HTML_FILE_OPT = "divHtmlOutFile";
    public static final String K_OUT_CSV_FILE_OPT = "csvOutFile";
    public static final String K_OUT_JSON_FILE_OPT = "jsonOutFile";
    public static final String K_EXIT_RETURN_ON_FAIL_OPT = "exitReturnOnFail";


    // column name for the kpi csv file
    public static final String K_CSV_COL_NAME_KPI = "name_kpi";
    public static final String K_CSV_COL_METRIC_CSV_COLUM_NAME = "metric_csv_column_name";
    public static final String K_CSV_COL_LABEL_REGEX = "label_regex";
    public static final String K_CSV_COL_COMPARATOR = "comparator";
    public static final String K_CSV_COL_THREASHOLD = "threshold";

    // Column name for Html or CSV out file
    public static final String K_CSV_COL_OUT_RESULT = "result";
    public static final String K_CSV_COL_OUT_FAIL_MSG = "fail_msg";

    private static final String K_NOT_SET = "NOT SET";

    // Column name Label in jmeter csv report
    public static final String K_CSV_JMREPORT_COL_LABEL_DEFAULT = "Label";

    // Freemarker Html Template
    public static final String K_FREEMARKER_HTML_TEMPLATE_DIRECTORY = "/templates_freemarker";
    public static final String K_FREEMARKER_HTML_TEMPLATE = "template_html_result.ftl";
    public static final String K_FREEMARKER_DIV_HTML_TEMPLATE = "template_div_result.ftl";

    public static final int K_TYPE_HTML_TEMPLATE = 1;
    public static final int K_TYPE_DIV_HTML_TEMPLATE = 2;
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

        String csvJmeterReport = K_NOT_SET;
        String csvLabelColumnName = K_CSV_JMREPORT_COL_LABEL_DEFAULT;
        String kpiFile = K_NOT_SET;
        String junitFile = K_JUNIT_XML_FILE_DEFAULT;
        String htmlFile = K_NOT_SET;
        String divHtmlFile = K_NOT_SET;
        String csvFile = K_NOT_SET;
        String jsonFile = K_NOT_SET;

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

        sTmp = (String) parseProperties.get(K_OUT_HTML_FILE_OPT);
        if (sTmp != null && sTmp.length() > 1) {
            htmlFile = sTmp;
        }

        sTmp = (String) parseProperties.get(K_OUT_DIV_HTML_FILE_OPT);
        if (sTmp != null && sTmp.length() > 1) {
            divHtmlFile = sTmp;
        }

        sTmp = (String) parseProperties.get(K_OUT_CSV_FILE_OPT);
        if (sTmp != null && sTmp.length() > 1) {
            csvFile = sTmp;
        }

        sTmp = (String) parseProperties.get(K_OUT_JSON_FILE_OPT);
        if (sTmp != null && sTmp.length() > 1) {
            jsonFile = sTmp;
        }

        sTmp = (String) parseProperties.get(K_EXIT_RETURN_ON_FAIL_OPT);
        if (sTmp != null) {
            exitOnFailKpi = Boolean.parseBoolean(sTmp);
            LOGGER.fine("exitOnFailKpi:" + exitOnFailKpi);
        }
        boolean isKpiFail = false;
        LOGGER.info("Parameters CLI:" + parseProperties);

        try {
            isKpiFail = analyseCsvJMReportWithKpiRules(csvJmeterReport, csvLabelColumnName, kpiFile, junitFile, htmlFile, divHtmlFile, csvFile, jsonFile);
            LOGGER.info("isKpiFail=" + isKpiFail);
        } catch (Exception ex) {
            LOGGER.warning(stackTraceInString(ex));
            isKpiFail = true;
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
     *
     * @param csvJmeterReport    the JMeter Report CSV format
     * @param csvLabelColumnName the Label Column Name (default : Label)
     * @param kpiFile            the kpi contains kpi declaration
     * @param junitFile          the JUnit XML out file to create
     * @param htmlFile           the Html out file result optional
     * @param divHtmlFile        partial Div Html out file result optional
     * @param csvFile            the Cvs out file result optional
     * @param jsonFile           the Json out file result optional
     * @return is Fail true or false, a kpi is fail or not
     * @throws IOException                  file exception
     * @throws ParserConfigurationException error reading csv file
     * @throws TransformerException         error writing JUnit XML file
     */
    private static boolean analyseCsvJMReportWithKpiRules(String csvJmeterReport, String csvLabelColumnName, String kpiFile, String junitFile, String htmlFile, String divHtmlFile, String csvFile, String jsonFile) throws IOException, ParserConfigurationException, TransformerException, TemplateException {
        boolean isFail = false;
        List<CSVRecord> csvJMReportLines = UtilsCsvFile.readCsvFile(csvJmeterReport);
        List<CSVRecord> csvKpiLines = UtilsCsvFile.readCsvFile(kpiFile);
        GlobalResult globalResult = new GlobalResult();
        List<CheckKpiResult> checkKpiResults = new ArrayList<>();
        globalResult.setCheckKpiResults(checkKpiResults);
        globalResult.setCsvJmeterReport(csvJmeterReport);
        globalResult.setKpiFile(kpiFile);

        int nbFailed = 0;
        Document document = UtilsJUnitXml.createJUnitRootDocument();
        for (int i = 0; i < csvKpiLines.size(); i++) {
            CSVRecord recordKpiLine = csvKpiLines.get(i);
            if (recordKpiLine.size() < 3) {
                continue;
            }
            CheckKpiResult checkKpiResult = verifyKpi(recordKpiLine, csvJMReportLines, csvLabelColumnName);
            if (checkKpiResult.isKpiFail()) {
                isFail = true;
                nbFailed++;
                String className = checkKpiResult.getMetricCsvColumnName() + " (" + checkKpiResult.getLabelRegex() + ") " + checkKpiResult.getComparator() + " " + checkKpiResult.getThreshold();
                UtilsJUnitXml.addTestCaseFailure(document, checkKpiResult.getNameKpi(), className, checkKpiResult.getFailMessage());
            } else {
                String className = checkKpiResult.getMetricCsvColumnName() + " (" + checkKpiResult.getLabelRegex() + ") " + checkKpiResult.getComparator() + " " + checkKpiResult.getThreshold();
                UtilsJUnitXml.addTestCaseOk(document, checkKpiResult.getNameKpi(), className);
            }
            globalResult.getCheckKpiResults().add(checkKpiResult);
        }
        globalResult.setNumberOfKpis(csvKpiLines.size());
        globalResult.setNumberFailed(nbFailed);

        LOGGER.info("Write junitFile=" + junitFile);
        UtilsJUnitXml.saveXmFile(document, junitFile);
        if (!K_NOT_SET.equals(htmlFile)) {
            LOGGER.info("Write html file=" + htmlFile);
            UtilsHtml.saveHtmlFile(globalResult, htmlFile, K_TYPE_HTML_TEMPLATE);
        }

        if (!K_NOT_SET.equals(divHtmlFile)) {
            LOGGER.info("Write Div Html file=" + divHtmlFile);
            UtilsHtml.saveHtmlFile(globalResult, divHtmlFile, K_TYPE_DIV_HTML_TEMPLATE);
        }

        if (!K_NOT_SET.equals(csvFile)) {
            LOGGER.info("Write csv file=" + csvFile);
            UtilsCsvFile.saveCsvFile(globalResult, csvFile);
        }

        if (!K_NOT_SET.equals(jsonFile)) {
            LOGGER.info("Write json file=" + jsonFile);
            UtilsJsonFile.saveJsonFile(globalResult, jsonFile);
        }
        return isFail;
    }

    /**
     * verify one kpi for lines in csv JMeter Report
     *
     * @param recordKpiLine      a kpi line to verify
     * @param csvJMReportLines   all lines in JMeter Report
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
        checkKpiResult.setFailMessage("");

        Pattern patternRegex = Pattern.compile(labelRegex);

        boolean isFailKpi = false;
        boolean isFirstFail = true;
        for (int i = 0; i < csvJMReportLines.size(); i++) {
            CSVRecord recordJMReportLine = csvJMReportLines.get(i);
            String label = recordJMReportLine.get(csvLabelColumnName);
            Matcher matcherRegex = patternRegex.matcher(label);
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
                                String failMessage = "Actual value " + dMetric + " exceeds or equals threshold " + dThreshold + " for samples matching \"" + labelRegex + "\"; fail label(s) \"" + label + "\""; // Actual value 2908,480000 exceeds threshold 2500,000000 for samples matching "@SC01_P03_DUMMY"
                                checkKpiResult.setKpiFail(true);
                                checkKpiResult.setFailMessage(failMessage);
                            } else {
                                String failMessage = checkKpiResult.getFailMessage();
                                failMessage = concatFailMessage(label, failMessage);
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
                                String failMessage = "Actual value " + dMetric + " exceeds threshold " + dThreshold + " for samples matching \"" + labelRegex + "\"; fail label(s) \"" + label + "\"";
                                checkKpiResult.setKpiFail(true);
                                checkKpiResult.setFailMessage(failMessage);
                            } else {
                                String failMessage = checkKpiResult.getFailMessage();
                                failMessage = concatFailMessage(label, failMessage);
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
                                String failMessage = "Actual value " + dMetric + " is less or equals then threshold " + dThreshold + " for samples matching \"" + labelRegex + "\"; fail label(s) \"" + label + "\"";
                                checkKpiResult.setKpiFail(true);
                                checkKpiResult.setFailMessage(failMessage);
                            } else {
                                String failMessage = checkKpiResult.getFailMessage();
                                failMessage = concatFailMessage(label, failMessage);
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
                                String failMessage = "Actual value " + dMetric + "is less then threshold " + dThreshold + " for samples matching \"" + labelRegex + "\"; fail label(s) \"" + label + "\"";
                                checkKpiResult.setKpiFail(true);
                                checkKpiResult.setFailMessage(failMessage);
                            } else {
                                String failMessage = checkKpiResult.getFailMessage();
                                failMessage = concatFailMessage(label, failMessage);
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

    private static String concatFailMessage(String label, String failMessage) {
        if ((failMessage.length() + label.length()) < K_FAIL_MESSAGE_SIZE_MAX) {
            failMessage += ", \"" + label + "\"";
        } else {
            if (!failMessage.endsWith(" ...")) {
                failMessage += " ...";
            }
        }
        return failMessage;
    }

    private static String stackTraceInString(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
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
                + K_CSV_LABEL_COLUMN_NAME_OPT + " Label -" + K_KPI_FILE_OPT + " kpi_check.csv -" + K_JUNIT_XML_FILE_OPT + " junit.xml -" +
                K_OUT_HTML_FILE_OPT + " result.html -" + K_OUT_DIV_HTML_FILE_OPT + " div_result.html -" + K_OUT_CSV_FILE_OPT + " result.csv -" + K_OUT_JSON_FILE_OPT + " result.json -" + K_EXIT_RETURN_ON_FAIL_OPT + " false\n";
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

        if (line.hasOption(K_OUT_HTML_FILE_OPT)) {
            properties.setProperty(K_OUT_HTML_FILE_OPT, line.getOptionValue(K_OUT_HTML_FILE_OPT));
        }

        if (line.hasOption(K_OUT_DIV_HTML_FILE_OPT)) {
            properties.setProperty(K_OUT_DIV_HTML_FILE_OPT, line.getOptionValue(K_OUT_DIV_HTML_FILE_OPT));
        }

        if (line.hasOption(K_OUT_CSV_FILE_OPT)) {
            properties.setProperty(K_OUT_CSV_FILE_OPT, line.getOptionValue(K_OUT_CSV_FILE_OPT));
        }

        if (line.hasOption(K_OUT_JSON_FILE_OPT)) {
            properties.setProperty(K_OUT_JSON_FILE_OPT, line.getOptionValue(K_OUT_JSON_FILE_OPT));
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
                .desc("JMeter report csv file (E.g: summary.csv or aggregate.csv or synthesis.csv)")
                .build();
        options.addOption(csvJmeterReportFileOpt);

        Option csvLabelColumnNameOpt = Option.builder(K_CSV_LABEL_COLUMN_NAME_OPT).argName(K_CSV_LABEL_COLUMN_NAME_OPT)
                .hasArg(true)
                .required(false)
                .desc("Label Column Name in CSV JMeter Report (Default: " + K_CSV_JMREPORT_COL_LABEL_DEFAULT + ")")
                .build();
        options.addOption(csvLabelColumnNameOpt);

        Option kpiFileOpt = Option.builder(K_KPI_FILE_OPT).argName(K_KPI_FILE_OPT)
                .hasArg(true)
                .required(true)
                .desc("KPI file contains rule to check (E.g: kpi.csv)")
                .build();
        options.addOption(kpiFileOpt);

        Option junitXmlOutOpt = Option.builder(K_JUNIT_XML_FILE_OPT).argName(K_JUNIT_XML_FILE_OPT)
                .hasArg(true)
                .required(false)
                .desc("JUnit XML file name out (Always created, default: " + K_JUNIT_XML_FILE_DEFAULT + ")")
                .build();
        options.addOption(junitXmlOutOpt);

        Option htmlOutOpt = Option.builder(K_OUT_HTML_FILE_OPT).argName(K_OUT_HTML_FILE_OPT)
                .hasArg(true)
                .required(false)
                .desc("Html out file result optional (E.g: result.html)")
                .build();
        options.addOption(htmlOutOpt);

        Option divHtmlOutOpt = Option.builder(K_OUT_DIV_HTML_FILE_OPT).argName(K_OUT_DIV_HTML_FILE_OPT)
                .hasArg(true)
                .required(false)
                .desc("Div Partial Html Page out file result optional (E.g: div_result.html), to include in an another HTML Page")
                .build();
        options.addOption(divHtmlOutOpt);

        Option csvOutOpt = Option.builder(K_OUT_CSV_FILE_OPT).argName(K_OUT_CSV_FILE_OPT)
                .hasArg(true)
                .required(false)
                .desc("Csv out file result optional (E.g: result.csv)")
                .build();
        options.addOption(csvOutOpt);

        Option jsonOutOpt = Option.builder(K_OUT_JSON_FILE_OPT).argName(K_OUT_JSON_FILE_OPT)
                .hasArg(true)
                .required(false)
                .desc("Json out file result optional (E.g: result.json)")
                .build();
        options.addOption(jsonOutOpt);

        Option exitReturnOnFailOpt = Option.builder(K_EXIT_RETURN_ON_FAIL_OPT).argName(K_EXIT_RETURN_ON_FAIL_OPT)
                .hasArg(true)
                .required(false)
                .desc("If true then when kpi fail then create JUnit XML file and program return exit 1 (KO); If false (Default) then create JUnit XML File and exit 0 (OK)")
                .build();
        options.addOption(exitReturnOnFailOpt);

        return options;
    }
}
