package io.github.vdaburon.jmeter.utils.reportkpi;

import java.util.List;

public class GlobalResult {
    private String csvJmeterReport;
    private String kpiFile;
    private int numberOfKpis;
    private int numberFailed;
    List checkKpiResults;

    public String getCsvJmeterReport() {
        return csvJmeterReport;
    }

    public void setCsvJmeterReport(String csvJmeterReport) {
        this.csvJmeterReport = csvJmeterReport;
    }

    public String getKpiFile() {
        return kpiFile;
    }

    public void setKpiFile(String kpiFile) {
        this.kpiFile = kpiFile;
    }

    public int getNumberOfKpis() {
        return numberOfKpis;
    }

    public void setNumberOfKpis(int numberOfKpis) {
        this.numberOfKpis = numberOfKpis;
    }

    public int getNumberFailed() {
        return numberFailed;
    }

    public void setNumberFailed(int numberFailed) {
        this.numberFailed = numberFailed;
    }

    public List getCheckKpiResults() {
        return checkKpiResults;
    }

    public void setCheckKpiResults(List checkKpiResults) {
        this.checkKpiResults = checkKpiResults;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GlobalResult{");
        sb.append("csvJmeterReport='").append(csvJmeterReport).append('\'');
        sb.append(", kpiFile='").append(kpiFile).append('\'');
        sb.append(", numberOfKpis=").append(numberOfKpis);
        sb.append(", numberFailed=").append(numberFailed);
        sb.append(", checkKpiResults=").append(checkKpiResults);
        sb.append('}');
        return sb.toString();
    }
}
