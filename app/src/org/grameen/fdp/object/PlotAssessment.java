package org.grameen.fdp.object;

/**
 * Created by aangjnr on 15/02/2018.
 */

public class PlotAssessment {

    String plotId;
    String plotName;
    String results;
    int color;

    public PlotAssessment(String name, String result){this.plotName = name; this.results = result;}


    public void setColor(int color) {
        this.color = color;
    }

    public void setPlotName(String plotName) {
        this.plotName = plotName;
    }

    public void setResults(String results) {
        this.results = results;
    }


    public String getResults() {
        return results;
    }

    public int getColor() {
        return color;
    }

    public String getPlotName() {
        return plotName;
    }



    int getColor(String v){

        //if(v.equalsIgnoreCase(""))

        return 0;
    }


}
