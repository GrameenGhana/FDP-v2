package org.grameen.fdp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.grameen.fdp.R;
import org.grameen.fdp.fragment.MyFormFragment;
import org.grameen.fdp.object.Logic;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.object.RealPlot;
import org.grameen.fdp.object.Recommendation;
import org.grameen.fdp.utility.Constants;
import org.grameen.fdp.utility.CustomToast;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Created by aangjnr on 09/11/2017.
 */

public class PlotDetailsActivity extends BaseActivity {

    String TAG = "PLOT DETAILS ACTIVITY";
    RealPlot plot;
    MyFormFragment plotAOFragment;
    TextView plotName;
    TextView plotSize;
    TextView ph;
    TextView estimatedProductionSize;
    TextView recommendedIntervention;
    TextView limeNeeded;
    ProgressBar recommendationProgress;
    boolean isMonitoringMode = false;
    Question plotNameQue;
    Question plotSizeQue;
    Question soilPhQue;
    Question estProdQue;
    private List<Recommendation> recommendations;
    JSONObject ALL_DATA_JSON = new JSONObject();
    private JSONObject FARMING_ECONOMIC_PROFILE_JSON;


    String farmerHasAgreed;
    int syncStatus;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_plot_v2);
        setUpUI();

        Toolbar toolbar = setToolbar(getResources(R.string.plot_info));

        engine = new ScriptEngineManager().getEngineByName("rhino");


        plotName = (TextView) findViewById(R.id.plotName);
        plotSize = (TextView) findViewById(R.id.landSize);
        ph = (TextView) findViewById(R.id.ph);
        estimatedProductionSize = (TextView) findViewById(R.id.estimatedProductionSize);

        recommendedIntervention = findViewById(R.id.recommended_intervention);
        limeNeeded = findViewById(R.id.lime_needed);
        recommendationProgress = findViewById(R.id.recommendationProgress);


        try {
            plotNameQue = databaseHelper.getQuestionByTranslation("Plot Name");
            plotSizeQue = databaseHelper.getQuestionByTranslation("Plot Size");
            estProdQue = databaseHelper.getQuestionByTranslation("Estimated production size");
            soilPhQue = databaseHelper.getQuestionByTranslation("Soil PH");

        } catch (Exception e) {
            e.printStackTrace();
            CustomToast.makeToast(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }





        plot = new Gson().fromJson(getIntent().getStringExtra("plot"), RealPlot.class);
        farmerHasAgreed = getIntent().getStringExtra("hasSubmitted");
        syncStatus = getIntent().getIntExtra("syncStatus", -1);




        if (plot != null) {

            String sizeUnit = "--", estProdUnit = "--";

            String farmingEconomicProfileJson = databaseHelper.getAllAnswersJson(plot.getFarmerCode());

            Log.i(TAG, "FARMING ECO PROFILE " + farmingEconomicProfileJson);

            if (farmingEconomicProfileJson != null)
                try {

                    FARMING_ECONOMIC_PROFILE_JSON = new JSONObject(farmingEconomicProfileJson);
                    sizeUnit = FARMING_ECONOMIC_PROFILE_JSON.get(databaseHelper.getQuestionIdByTranslationName("Area units")).toString();
                    estProdUnit = FARMING_ECONOMIC_PROFILE_JSON.get(databaseHelper.getQuestionIdByTranslationName("Weight units")).toString();

                } catch (JSONException e) {
                    e.printStackTrace();


                }


            plotName.setText(plot.getName());
            String plotInfo = plot.getPlotInformationJson();

            Log.i(TAG, "PLOT INFO VALUES =  " + plotInfo);

            try {

                String size, estProd;

                ALL_DATA_JSON = new JSONObject(plotInfo);

                size = ALL_DATA_JSON.getString(plotSizeQue.getId()) + " " + sizeUnit;
                estProd = ALL_DATA_JSON.getString(estProdQue.getId()) + " " + estProdUnit;

                plotSize.setText(size);
                estimatedProductionSize.setText(estProd);
                ph.setText(ALL_DATA_JSON.get(soilPhQue.getId()).toString());

                String limeNeededValue = ALL_DATA_JSON.get(databaseHelper.getQuestionIdByTranslationName("Lime need")).toString();

                Log.i(TAG, "^^^^^^^^^^^^ LIME NEED ^^^^^^^^^^^^^^^" + limeNeededValue);


                if (limeNeededValue.equalsIgnoreCase("yes")) {
                    limeNeeded.setText(limeNeededValue);
                    limeNeeded.setTextColor(ContextCompat.getColor(this, R.color.cpb_red));

                } else {

                    limeNeeded.setText("No");
                    limeNeeded.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
                }



            } catch (JSONException e) {
                e.printStackTrace();
            }


            Log.i("ACTION TYPE", prefs.getString("flag", ""));
            if (prefs.getString("flag", "").equals(Constants.MONITORING)) {

                //Todo add the rest of the views to hide

                findViewById(R.id.editButton).setVisibility(View.GONE);

                plotAOFragment = MyFormFragment.newInstance(Constants.ADOPTION_OBSERVATIONS, true, plot.getFarmerCode() + "_" + plot.getId(), true);


            } else {

                plotAOFragment = MyFormFragment.newInstance(Constants.ADOPTION_OBSERVATIONS, true, plot.getFarmerCode() + "_" + plot.getId(), true);

            }

            loadDynamicView(plotAOFragment, R.id.aosLayout);


            if (prefs.getBoolean("shouldReapplyRecommendation", true)) {

                recommendedIntervention.setText("");
                recommendationProgress.setVisibility(View.VISIBLE);


                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        loadRecommendation();

                    }
                }, 2000);
            } else {
                Log.i(TAG, "GETTING RECOMMENDATION FROM DB");

                String ids = plot.getRecommendationId();

                if (ids != null)
                    if (!ids.equalsIgnoreCase("null") && !ids.equalsIgnoreCase("empty") && !ids.isEmpty()) {
                        String[] GAPS_PLOT_RECOMMENDATION_IDS = ids.split(",");

                        Recommendation GAPS_RECOMENDATION_FOR_START_YEAR = databaseHelper.getRecommendation(GAPS_PLOT_RECOMMENDATION_IDS[0]);
                        Recommendation PLOT_RECOMMENDATION = databaseHelper.getRecommendation(GAPS_PLOT_RECOMMENDATION_IDS[1]);

                        final String recNames;
                        recNames = (prefs.getBoolean("toggleTranslation", false)) ? PLOT_RECOMMENDATION.getName() : PLOT_RECOMMENDATION.getTranslation();





                       /* if (!GAPS_RECOMENDATION_FOR_START_YEAR.getName().equalsIgnoreCase(PLOT_RECOMMENDATION.getName()))
                            recNames = GAPS_RECOMENDATION_FOR_START_YEAR.getName() + ", " + PLOT_RECOMMENDATION.getName();
                        else
                            recNames = GAPS_RECOMENDATION_FOR_START_YEAR.getName();

*/

                        recommendedIntervention.setText(recNames);
                        recommendedIntervention.setTextColor(ContextCompat.getColor(PlotDetailsActivity.this, R.color.colorAccent));

                    } else recommendedIntervention.setText("--");


            }


        }


        findViewById(R.id.editButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Todo go to add edit plot activity, set plot parameters

                Intent intent = new Intent(PlotDetailsActivity.this, AddNewPlotActivity.class);
                intent.putExtra("flag", "edit");
                intent.putExtra("plot", new Gson().toJson(plot));

                startActivity(intent);
                finish();

            }
        });


        if (farmerHasAgreed != null)
            if (farmerHasAgreed.equalsIgnoreCase(Constants.YES) && syncStatus == 1) {

                findViewById(R.id.editButton).setVisibility(View.GONE);
            }

        onBackClicked();


    }


    void loadRecommendation() {

        Log.i("\n\n" + TAG, "############    REAPPLYING LOGIC TO RECOMMENDATION    ##################\n\n");


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                recommendations = databaseHelper.getAllRecommendations();

                int sizeOfAO = databaseHelper.getSpecificSetOfQuestions(Constants.ADOPTION_OBSERVATIONS).size();
                boolean hasIncompleteData = false;

                Iterator iterator = ALL_DATA_JSON.keys();
                while (iterator.hasNext()){
                    String tmp_key = (String) iterator.next();

                    try {
                        if (ALL_DATA_JSON.getString(tmp_key).equalsIgnoreCase("--")) {
                            hasIncompleteData = true;
                            break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if (ALL_DATA_JSON.length() >= sizeOfAO && !hasIncompleteData) {

                    Log.i(TAG, "START YEAR IS " + plot.getStartYear());

                    Recommendation PLOT_RECOMMENDATION = null;
                    Recommendation GAPS_RECOMENDATION_FOR_START_YEAR = null;


                    for (Recommendation rec : recommendations) {

                        Log.i("\n\n" + TAG, "@@@@@@@@@@@     RECOMMENDATION NAME  " + rec.getName() + "   @@@@@@@@@@@");
                        Log.i(TAG, "\n\n@@@@@@@@@@@     HIERARCHY   " + rec.getHierarchy() + "   @@@@@@@@@@@\n\n");

                        if (!rec.getHierarchy().equals("0")) {


                            Boolean logicResult = compareAndEvaluateCascadedLogics(databaseHelper.getLogic(rec.getLogicId()), ALL_DATA_JSON);

                            if (logicResult != null && logicResult) {


                                if (rec.getName().equalsIgnoreCase("replant") || rec.getName().equalsIgnoreCase("replant + extra soil"))
                                    GAPS_RECOMENDATION_FOR_START_YEAR = databaseHelper.getRecommendationBasedOnName("Minimal GAPs");

                                else if (rec.getName().equalsIgnoreCase("grafting") || rec.getName().equalsIgnoreCase("grafting + extra soil"))
                                    GAPS_RECOMENDATION_FOR_START_YEAR = databaseHelper.getRecommendationBasedOnName("Modest GAPs");
                                else
                                    GAPS_RECOMENDATION_FOR_START_YEAR = databaseHelper.getRecommendationBasedOnName("Maintenance (GAPs)");

                                PLOT_RECOMMENDATION = rec;

                                break;
                            }

                        }
                    }


                    if (GAPS_RECOMENDATION_FOR_START_YEAR == null)
                        GAPS_RECOMENDATION_FOR_START_YEAR = databaseHelper.getRecommendationBasedOnName("Maintenance (GAPs)");

                    if (PLOT_RECOMMENDATION == null)
                        PLOT_RECOMMENDATION = databaseHelper.getRecommendationBasedOnName("Maintenance (GAPs)");


                    Log.i(TAG, "\n\n");
                    Log.i(TAG, "GAPS for this plot is " + GAPS_RECOMENDATION_FOR_START_YEAR.getName());
                    Log.i(TAG, "Plot recommendation is " + PLOT_RECOMMENDATION.getName());


                    Log.i(TAG, "\n\n");

                    if (databaseHelper.editFarmerPlotRecommendationId(
                            plot.getFarmerCode(),
                            plot.getId(),
                            GAPS_RECOMENDATION_FOR_START_YEAR.getId() + "," + PLOT_RECOMMENDATION.getId())
                            ) {

                        databaseHelper.editPlotStartYear(plot.getId(), 1);


                        final String recNames;
                        recNames = (prefs.getBoolean("toggleTranslation", false)) ? PLOT_RECOMMENDATION.getName() : PLOT_RECOMMENDATION.getTranslation();





                       /* if (GAPS_RECOMENDATION_FOR_START_YEAR.getName().equalsIgnoreCase(PLOT_RECOMMENDATION.getName()))
                            recNames = PLOT_RECOMMENDATION.getName();

                        else if(PLOT_RECOMMENDATION.getName().equalsIgnoreCase("no fdp"))
                            recNames = PLOT_RECOMMENDATION.getName();

                        else
                            recNames = GAPS_RECOMENDATION_FOR_START_YEAR.getName() + ", " + PLOT_RECOMMENDATION.getName();
*/




                        prefs.edit().putBoolean("shouldReapplyRecommendation", false).apply();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {


                                recommendationProgress.setVisibility(View.GONE);
                                recommendedIntervention.setText(recNames);
                                recommendedIntervention.setTextColor(ContextCompat.getColor(PlotDetailsActivity.this, R.color.colorAccent));


                            }
                        });
                    }

                } else {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {


                            recommendationProgress.setVisibility(View.GONE);
                            recommendedIntervention.setText(getResources(R.string.fill_out_ao_data));
                            recommendedIntervention.setTextColor(ContextCompat.getColor(PlotDetailsActivity.this, R.color.cpb_red));


                        }
                    });


                }

            }
        });

        thread.start();


    }





    @Override
    public void onResume() {
        super.onResume();
    }


    private void setUpUI() {

        TextView lime_needed_text = findViewById(R.id.lime_needed_text);
        Question temp = databaseHelper.getQuestionByTranslation("Lime need");
        if (temp != null)
            lime_needed_text.setText((prefs.getBoolean("toggleTranslation", false)) ? temp.getTranslation__c() : temp.getCaption__c());


        TextView plot_est_prod_text = findViewById(R.id.plot_est_prod_text);
        temp = databaseHelper.getQuestionByTranslation("Estimated production size");
        if (temp != null)
            plot_est_prod_text.setText((prefs.getBoolean("toggleTranslation", false)) ? temp.getTranslation__c() : temp.getCaption__c());


        TextView plot_soil_ph_text = findViewById(R.id.plot_ph_text);
        temp = databaseHelper.getQuestionByTranslation("Soil PH");
        if (temp != null)
            plot_soil_ph_text.setText((prefs.getBoolean("toggleTranslation", false)) ? temp.getTranslation__c() : temp.getCaption__c());


    }

}
