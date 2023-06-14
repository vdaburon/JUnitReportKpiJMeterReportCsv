package io.github.vdaburon.jmeter.utils.reportkpi;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class UtilsCsvFile {
    /**
     * Read all lines in a csv file
     * @param fileIn the csv file name to read
     * @return a ArrayList of CSVRecord contains all lines
     * @throws IOException error when read the CSV file
     */
    public static List<CSVRecord> readCsvFile(String fileIn) throws IOException {
        Reader in = new FileReader(fileIn);
        Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);

        List<CSVRecord> listRecordsBetweenFirstAndLast = new ArrayList();

        for (CSVRecord record : records) {
            listRecordsBetweenFirstAndLast.add(record);
        }
        in.close();
        return listRecordsBetweenFirstAndLast;
    }

    /**
     * Write the KPIs Lines Result in a CSV file (
     * @param globalResult Global KPIs result
     * @param csvFile Csv out file result
     * @throws IOException
     */
    public static void saveCsvFile(GlobalResult globalResult, String csvFile) throws IOException {
        String[] headers = {
                JUnitReportFromJMReportCsv.K_CSV_COL_NAME_KPI,
                JUnitReportFromJMReportCsv.K_CSV_LABEL_COLUMN_NAME_OPT,
                JUnitReportFromJMReportCsv.K_CSV_COL_LABEL_REGEX,
                JUnitReportFromJMReportCsv.K_CSV_COL_COMPARATOR,
                JUnitReportFromJMReportCsv.K_CSV_COL_THREASHOLD,
                JUnitReportFromJMReportCsv.K_CSV_COL_OUT_RESULT,
                JUnitReportFromJMReportCsv.K_CSV_COL_OUT_FAIL_MSG
        };

        FileWriter fileWrite = new FileWriter(csvFile);

        CSVFormat csvFormat = CSVFormat.RFC4180.builder()
                .setHeader(headers)
                .build();
        List checkKpiResults = globalResult.getCheckKpiResults();
        CSVPrinter printer = new CSVPrinter(fileWrite, csvFormat);
        for (int i = 0; i < checkKpiResults.size(); i++) {
            CheckKpiResult checkKpiResult = (CheckKpiResult) checkKpiResults.get(i);
            String sResult = checkKpiResult.isKpiFail()?"fail":"sucess";
            String sFailMessage = checkKpiResult.getFailMessage() != null?checkKpiResult.getFailMessage():"";

            printer.printRecord(checkKpiResult.getNameKpi(), checkKpiResult.getMetricCsvColumnName(), checkKpiResult.getLabelRegex(), checkKpiResult.getComparator(),
                    checkKpiResult.getThreshold(), sResult, sFailMessage);
        }
        printer.close();
    }
}
