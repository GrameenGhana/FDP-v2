package org.grameen.fdp.object;

import java.util.List;

/**
 * Created by aangjnr on 15/02/2018.
 */

public class FarmResult {
    String caption;
    String status;
    List<PlotAssessment> plotAssessmentList;


    public FarmResult(){}

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setPlotAssessmentList(List<PlotAssessment> plotAssessmentList) {
        this.plotAssessmentList = plotAssessmentList;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public List<PlotAssessment> getPlotAssessmentList() {
        return plotAssessmentList;
    }

    public String getCaption() {
        return caption;
    }
}
