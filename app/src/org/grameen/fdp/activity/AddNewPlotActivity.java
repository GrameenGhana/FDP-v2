package org.grameen.fdp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Created by aangjnr on 09/11/2017.
 */

public class AddNewPlotActivity extends BaseActivity {

    boolean isEditMode = false;
    boolean newDataSaved = false;
    RealPlot plot;
    //MyFormFragment plotInfoFragment;
    MyFormFragment plotAOFragment;
    String TAG = "Add New Plot Act";

    EditText plotName;
    EditText plotSizeEdittext;
    TextView plotSizeUnit;
    EditText estimatedProductionEdittext;
    TextView estimatedProductionUnit;
    EditText phEdittext;
    Question plotNameQue;
    Question plotSizeQue;
    Question soilPhQue;
    Question estProdQue;

    String plotRenovatedId = "", plotRenovationMadeId = "", plotInterventionAppliedId = "";


    JSONObject ALL_DATA_JSON = new JSONObject();


    /* JSONObject AO_JSON_OBJECT = null;
    JSONObject PLOT_INFO_JSON;*/
    JSONObject ALL_FARMER_ANSWERS_JSON;



    Button save;
    String farmerCode;

    String plotSize = "";
    String estProductionSize = "";


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_a_plot);

        setUpUI();

        prefs.edit().putBoolean("isInPlotInfoActivity", false).apply();

        Toolbar toolbar;

        plotSizeEdittext = findViewById(R.id.plotSizeEdittext);
        plotSizeUnit = findViewById(R.id.plotSizeUnitSpinner);



        estimatedProductionEdittext = findViewById(R.id.estimatedProductionEdittext);
        estimatedProductionUnit = findViewById(R.id.productionSizeUnitSpinner);

        phEdittext = findViewById(R.id.phEdittext);


        save = findViewById(R.id.saveButton);
        save.setEnabled(false);

        plotName = (EditText) findViewById(R.id.plotNameEdittext);
        plotName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                if (editable.length() < 2) {
                    save.setEnabled(false);
                    CustomToast.makeToast(AddNewPlotActivity.this, getResources(R.string.enter_valid_plot_name), Toast.LENGTH_SHORT).show();

                } else save.setEnabled(true);
            }
        });


        Intent intent = getIntent();


        if (intent.getStringExtra("flag") != null && intent.getStringExtra("flag").equals("edit")) {
            isEditMode = true;
            toolbar = setToolbar(getResources(R.string.edit_plot));


            plot = new Gson().fromJson(getIntent().getStringExtra("plot"), RealPlot.class);
            plotName.setText(plot.getName());
            farmerCode = plot.getFarmerCode();


            String plotInfo = plot.getPlotInformationJson();

            try {
                JSONObject jsonObject = new JSONObject(plotInfo);

                String size = jsonObject.getString(plotSizeQue.getId());
                plotSizeEdittext.setText(size.split(" ")[0]);


                String estProd = jsonObject.getString(estProdQue.getId());
                estimatedProductionEdittext.setText(estProd.split(" ")[0]);


                phEdittext.setText(jsonObject.getString(soilPhQue.getId()));

            } catch (JSONException | ArrayIndexOutOfBoundsException | NullPointerException e ) {
                e.printStackTrace();
            }

            String farmingEconomicProfileJson = databaseHelper.getAllAnswersJson(farmerCode);

            Log.i(TAG, "FARMING ECO PROFILE " + farmingEconomicProfileJson);

            if(farmingEconomicProfileJson != null)

                try {

                    ALL_FARMER_ANSWERS_JSON = new JSONObject(farmingEconomicProfileJson);
                    plotSizeUnit.setText(ALL_FARMER_ANSWERS_JSON.get(databaseHelper.getQuestionIdByTranslationName("Area units")).toString());
                    estimatedProductionUnit.setText(ALL_FARMER_ANSWERS_JSON.get(databaseHelper.getQuestionIdByTranslationName("Weight units")).toString());

            } catch (JSONException | ArrayIndexOutOfBoundsException | NullPointerException e ) {
                e.printStackTrace();
                plotSizeUnit.setText("--");
                estimatedProductionUnit.setText("--");

            } else{
                plotSizeUnit.setText("--");
                estimatedProductionUnit.setText("--");
            }


            Log.i(TAG, "WEIGHT " + estimatedProductionUnit.getText().toString());
            Log.i(TAG, "AREA " + plotSizeUnit.getText().toString());


            plotAOFragment = MyFormFragment.newInstance(Constants.ADOPTION_OBSERVATIONS, true, plot.getFarmerCode() + "_" + plot.getId(), false);

            loadDynamicView(plotAOFragment, R.id.aosLayout);
        } else {

            setToolbar(getResources(R.string.add_new_plot));
            farmerCode = getIntent().getStringExtra("farmerCode");

            Log.d(TAG, "FARMER CODE IS " + farmerCode);


            String farmingEconomicProfileJson = databaseHelper.getAllAnswersJson(farmerCode);

            Log.i(TAG, "FARMING ECO PROFILE " + farmingEconomicProfileJson);

            if(farmingEconomicProfileJson != null)
            try {

                ALL_FARMER_ANSWERS_JSON = new JSONObject(farmingEconomicProfileJson);
                plotSizeUnit.setText(ALL_FARMER_ANSWERS_JSON.get(databaseHelper.getQuestionIdByTranslationName("Area units")).toString());
                estimatedProductionUnit.setText(ALL_FARMER_ANSWERS_JSON.get(databaseHelper.getQuestionIdByTranslationName("Weight units")).toString());
                plotSizeEdittext.setText(plotSizeQue.getDefault_value__c());
                estimatedProductionEdittext.setText(estProdQue.getDefault_value__c());
                phEdittext.setText(soilPhQue.getDefault_value__c());


            } catch (JSONException e) {
                e.printStackTrace();

                plotSizeUnit.setText("--");
                estimatedProductionUnit.setText("--");
            }
            else{
                plotSizeUnit.setText("--");
                estimatedProductionUnit.setText("--");
            }
            int noOfPlots = databaseHelper.getAllFarmerPlots(farmerCode).size() + 1;
            String value = "Plot " + noOfPlots;
            plotName.setText(value);
            plotAOFragment = MyFormFragment.newInstance(Constants.ADOPTION_OBSERVATIONS, false, null, false);
            loadDynamicView(plotAOFragment, R.id.aosLayout);
        }


        findViewById(R.id.plot_area_calculation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Todo go to Map Activity
                if (!plotName.getText().toString().isEmpty() || !plotName.getText().toString().equals("")) {

                    saveOrUpdateData(false);
                    // newDataSaved = true;

                    final Intent intent = new Intent(AddNewPlotActivity.this, MapActivity.class);

                   /* if (!isEditMode) {
                        plot = new RealPlot();
                        plot.setId(databaseHelper.getSystemTime());
                        plot.setFarmerCode(farmerCode);
                        plot.setName(plotName.getText().toString());
                    }*/

                    intent.putExtra("plot", new Gson().toJson(plot));

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(intent);
                            finish();
                        }
                    }, 500);
                } else
                    Toast.makeText(AddNewPlotActivity.this, getResources(R.string.provide_plot_name), Toast.LENGTH_SHORT).show();

            }
        });


        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                saveOrUpdateData(true);

            }
        });


        onBackClicked();

    }

    private void setUpUI() {

        TextView plot_name_text = findViewById(R.id.plot_name_text);
        plotNameQue = databaseHelper.getQuestionByTranslation("Plot Name");
        if (plotNameQue != null)
            plot_name_text.setText((prefs.getBoolean("toggleTranslation", false)) ? plotNameQue.getTranslation__c() : plotNameQue.getCaption__c());


        TextView plot_size_text = findViewById(R.id.plot_size_text);
        plotSizeQue = databaseHelper.getQuestionByTranslation("Plot Size");
        if (plotSizeQue != null)
            plot_size_text.setText((prefs.getBoolean("toggleTranslation", false)) ? plotSizeQue.getTranslation__c() : plotSizeQue.getCaption__c());


        TextView plot_est_prod_text = findViewById(R.id.plot_est_prod_text);
        estProdQue = databaseHelper.getQuestionByTranslation("Estimated production size");
        if (estProdQue != null)
            plot_est_prod_text.setText((prefs.getBoolean("toggleTranslation", false)) ? estProdQue.getTranslation__c() : estProdQue.getCaption__c());


        TextView plot_soil_ph_text = findViewById(R.id.plot_ph_text);
        soilPhQue = databaseHelper.getQuestionByTranslation("Soil PH");
        if (soilPhQue != null)
            plot_soil_ph_text.setText((prefs.getBoolean("toggleTranslation", false)) ? soilPhQue.getTranslation__c() : soilPhQue.getCaption__c());


    }


    void saveOrUpdateData(boolean shouldMoveToNextActivity) {
        engine = new ScriptEngineManager().getEngineByName("rhino");

        if (!newDataSaved) {

            save.setEnabled(false);
            ALL_DATA_JSON = plotAOFragment.getAllAnswersInJSONObject();

            List<Question> plotInfoQues = databaseHelper.getSpecificSetOfQuestions(Constants.PLOT_INFORMATION);
            for (Question q : plotInfoQues) {
                if (q.getTranslation__c().equalsIgnoreCase("soil ph")) {

                    Log.d(TAG, "************************** FOUND THE QUESTION ID FOR Soil PH NEEDED!!!!" + q.getId());
                    try {
                        if (ALL_DATA_JSON.has(q.getId()))
                            ALL_DATA_JSON.remove(q.getId());
                        ALL_DATA_JSON.put(q.getId(), phEdittext.getText().toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (q.getTranslation__c().equalsIgnoreCase("Estimated production size")) {
                    Log.d(TAG, "************************** FOUND THE QUESTION ID FOR  ESTIMATED PROD!!!!" + q.getId());
                    try {

                        if (ALL_DATA_JSON.has(q.getId()))
                            ALL_DATA_JSON.remove(q.getId());

                        ALL_DATA_JSON.put(estProdQue.getId(), estimatedProductionEdittext.getText().toString());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (q.getTranslation__c().equalsIgnoreCase("plot size")) {

                    Log.d(TAG, "************************** FOUND THE QUESTION ID FOR PLOT SIZE!!!!" + q.getId());

                    try {
                        if (ALL_DATA_JSON.has(q.getId()))
                            ALL_DATA_JSON.remove(q.getId());
                        ALL_DATA_JSON.put(plotSizeQue.getId(), plotSizeEdittext.getText().toString());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (q.getTranslation__c().equalsIgnoreCase("how was this plot renovated?")) {
                    plotInterventionAppliedId = q.getId();
                } else if (q.getTranslation__c().equalsIgnoreCase("How long ago was the renovation made?")) {
                    plotRenovationMadeId = q.getId();
                } else if (q.getTranslation__c().equalsIgnoreCase("Was this plot recently renovated?")) {
                    plotRenovatedId = q.getId();
                }
            }
             try {

                 ALL_DATA_JSON.put(soilPhQue.getId(), phEdittext.getText().toString());

            } catch (JSONException e) {
                e.printStackTrace();
            }


            Log.d(TAG, "ALL PLOT ANSWERS JSON IS VALUE IS + \n" + ALL_DATA_JSON + "\n");
            List<Question> questionList = new ArrayList<>();
            questionList.addAll(databaseHelper.getSpecificSetOfQuestions(Constants.ADOPTION_OBSERVATION_RESULTS));
            questionList.addAll(databaseHelper.getSpecificSetOfQuestions(Constants.ADDITIONAL_INTERVENTION));


            applyLogicAndGetValue(questionList);


            Log.d(TAG, "ALL PLOT VALUES ON UPDATE + \n" + ALL_DATA_JSON + "\n");

            if (!isEditMode) {
                Log.d(TAG, "FARMER CODE FOR PLOT IS " + farmerCode);

                RealPlot realPlot = new RealPlot();
                realPlot.setId(databaseHelper.getSystemTime());
                realPlot.setName(plotName.getText().toString());
                realPlot.setFarmerCode(farmerCode);
                realPlot.setPlotInformationJson(ALL_DATA_JSON.toString());

                if (databaseHelper.addNewPlot(realPlot)) {
                    newDataSaved = true;
                    plot = realPlot;

                    databaseHelper.setFarmerAsUnSynced(realPlot.getFarmerCode());

                    checkIfFarmProductionCorresponds(farmerCode);

                    checkIfPlotWasRenovatedRecently(plot);

                    checkIfFarmSizeCorresponds(farmerCode, ALL_FARMER_ANSWERS_JSON);

                    CustomToast.makeToast(AddNewPlotActivity.this, getResources(R.string.new_plot_added), Toast.LENGTH_SHORT).show();

                    if (shouldMoveToNextActivity) {
                        Intent intent = new Intent(AddNewPlotActivity.this, PlotDetailsActivity.class);
                        intent.putExtra("plot", new Gson().toJson(realPlot));
                        intent.putExtra("farmerCode", farmerCode);
                        prefs.edit().putString("currentPlotId", plot.getId()).apply();
                        startActivity(intent);
                        finish();
                    }

                } else
                    CustomToast.makeToast(AddNewPlotActivity.this, getResources(R.string.could_not_add_plot), Toast.LENGTH_SHORT).show();


            } else {  //Todo save details finish this activity and go back to farmer details activity

                plot.setName(plotName.getText().toString());
                plot.setPlotInformationJson(ALL_DATA_JSON.toString());
                /*plot.setAdoptionObservationsJson(aoJsonValue);
                plot.setAdoptionObservationResultsJson(AORJson);
                plot.setAdditionalInterventionJson(AIJson);*/


                if (databaseHelper.editFarmerPlotAnswers(plot)) {
                    newDataSaved = true;
                    databaseHelper.setFarmerAsUnSynced(plot.getFarmerCode());
                    checkIfPlotWasRenovatedRecently(plot);
                    CustomToast.makeToast(AddNewPlotActivity.this, getResources(R.string.new_data_updated), Toast.LENGTH_SHORT).show();

                    checkIfFarmProductionCorresponds(farmerCode);

                    checkIfFarmSizeCorresponds(farmerCode, ALL_FARMER_ANSWERS_JSON);

                    if (shouldMoveToNextActivity) {
                        Intent intent = new Intent(AddNewPlotActivity.this, PlotDetailsActivity.class);
                        intent.putExtra("plot", new Gson().toJson(plot));
                        prefs.edit().putString("currentPlotId", plot.getId()).apply();
                        startActivity(intent);
                        finish();
                    }
                } else
                    CustomToast.makeToast(AddNewPlotActivity.this, getResources(R.string.could_not_add_plot), Toast.LENGTH_SHORT).show();
            }
            save.setEnabled(true);


        } else {
            prefs.edit().putBoolean("shouldReapplyRecommendation", false).apply();
            Intent intent = new Intent(AddNewPlotActivity.this, PlotDetailsActivity.class);
            prefs.edit().putString("currentPlotId", plot.getId()).apply();
            intent.putExtra("plot", new Gson().toJson(plot));

            startActivity(intent);
            finish();
        }
    }


    void applyLogicAndGetValue(List<Question> questions) {
        String resultQuestionId = "", logicId = "";
        Boolean result = false;
        try {
            for (Question question : questions) {
                System.out.println("***************************************************************");
                Log.i(question.getId(), question.getCaption__c());

                System.out.println("***************************************************************\n");
                List<Logic> logics = databaseHelper.doesQuestionHaveLogics(question.getId());

                if (logics.size() > 0) {
                    for (Logic logic : logics) {
                        Log.i(TAG, "Logic Name : " + logic.getName());

                        resultQuestionId = logic.getResultQuestions();
                        logicId = logic.getId();
                        result = compareAndEvaluateCascadedLogics(logic, ALL_DATA_JSON);
                        if (result != null) {
                            if (result) {
                                Log.i(TAG, "LOGIC EVALUATED TO TRUE \n");
                                Log.i(TAG, "BREAK OUT OF LOGIC LOOP");

                                try {
                                    if (ALL_DATA_JSON.has(resultQuestionId))
                                        ALL_DATA_JSON.remove(resultQuestionId);
                                    ALL_DATA_JSON.put(resultQuestionId, logic.getResult());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                databaseHelper.editLogicEvaluatedValue(logicId, result.toString());
                                break;
                            }
                        } else Log.i(TAG, "CHILD LOGIC EVALUATED TO NULL \n");
                    }

                    if (result != null && !result) {
                        String defValue = question.getDefault_value__c();
                        Log.i(TAG, "ALL LOGIC FOR " + question.getName() + " EVALUATED TO FALSE DEFAULT VALUE IS " + defValue);
                        try {
                            if (ALL_DATA_JSON.has(resultQuestionId))
                                ALL_DATA_JSON.remove(resultQuestionId);
                            ALL_DATA_JSON.put(resultQuestionId, defValue);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        // }
                    }
                } else Log.i(TAG, "DOES NOT HAVE LOGIC \n\n");
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        // return jsonObject.toString();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "ON PAUSE");
/*
        if (!newDataSaved && save.isEnabled())
            saveOrUpdateData(true);
*/
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (plot != null)
            try {
                plot = databaseHelper.getFarmerPlot(plot.getId(), plot.getFarmerCode());
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    void checkIfPlotWasRenovatedRecently(RealPlot plot) {
        try {
            Question plotRenovatedCorrectlyQuestion = databaseHelper.getQuestionByTranslation("Plot Renovated Correctly?");
            if (plotRenovatedCorrectlyQuestion != null) {
                Recommendation GAPS_RECOMENDATION_FOR_START_YEAR;
                if (ALL_DATA_JSON.get(plotRenovatedCorrectlyQuestion.getId()).toString().equalsIgnoreCase("yes")) {
                    int year = 0;
                    String recommendationName = ALL_DATA_JSON.get(plotInterventionAppliedId).toString();
                    try {
                        year = Integer.parseInt(ALL_DATA_JSON.get(plotRenovationMadeId).toString());
                        if (!recommendationName.equalsIgnoreCase("") && !recommendationName.isEmpty() && !recommendationName.equals("null") && !recommendationName.equals("--")) {
                            if (recommendationName.equalsIgnoreCase("replanting"))
                                GAPS_RECOMENDATION_FOR_START_YEAR = databaseHelper.getRecommendationBasedOnName("Replant");
                            else if (recommendationName.equalsIgnoreCase("grafting"))
                                GAPS_RECOMENDATION_FOR_START_YEAR = databaseHelper.getRecommendationBasedOnName("Grafting");
                            else
                                GAPS_RECOMENDATION_FOR_START_YEAR = null;

                            if (GAPS_RECOMENDATION_FOR_START_YEAR != null) {
                                if (databaseHelper.editFarmerPlotRecommendationId(plot.getFarmerCode(), plot.getId(), GAPS_RECOMENDATION_FOR_START_YEAR.getId() + "," + GAPS_RECOMENDATION_FOR_START_YEAR.getId()))
                                    databaseHelper.editPlotStartYear(plot.getId(), -year);
                                this.plot.setStartYear(-year);
                                this.plot.setRecommendationId(GAPS_RECOMENDATION_FOR_START_YEAR.getId() + "," + GAPS_RECOMENDATION_FOR_START_YEAR.getId());
                                prefs.edit().putBoolean("shouldReapplyRecommendation", false).apply();

                            } else
                                prefs.edit().putBoolean("shouldReapplyRecommendation", true).apply();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        prefs.edit().putBoolean("shouldReapplyRecommendation", false).apply();
                    }
                } else
                    prefs.edit().putBoolean("shouldReapplyRecommendation", true).apply();
            } else {

                CustomToast.makeToast(this, "Missing question \"Was this plot renovated correctly?\"", Toast.LENGTH_LONG).show();


            }
        } catch (JSONException e) {
            e.printStackTrace();
            prefs.edit().putBoolean("shouldReapplyRecommendation", false).apply();

        }
    }


    Boolean compareAndEvaluateCascadedLogics(Logic logic, JSONObject ALL_DATA_JSON) {


        StringBuilder equation = new StringBuilder();

        String equationValue = "";


        Log.i("\n" + TAG, "------ CASCADED LOGICS  ------- \n");

        //level 1
        Log.i(TAG, " ------  LEVEL 1  ------- ");
        Boolean value1 = getLogicValue(logic, ALL_DATA_JSON);
        Log.i(TAG, "\n");
        Log.i(TAG, " Value  " + value1);
        Log.i(TAG, "\n");


        //level 2
        Log.i(TAG, " ------  LEVEL 2  ------- ");
        Log.i(TAG, "\n");


        Logic logic2 = databaseHelper.getLogic(logic.getParentLogic());
        Boolean value2 = getLogicValue(logic2, ALL_DATA_JSON);
        Log.i(TAG, "\n");
        Log.i(TAG, " Value  " + value2);
        Log.i(TAG, "\n");


        //level 3
        Log.i(TAG, " ------  LEVEL 3  ------- ");
        Log.i(TAG, "\n");

        Logic logic3 = databaseHelper.getLogic(logic2.getParentLogic());
        Boolean value3 = getLogicValue(logic3, ALL_DATA_JSON);

        Log.i(TAG, "\n");

        Log.i(TAG, " Value  " + value3);
        Log.i(TAG, "\n");


        //level 4
        Log.i(TAG, " ------  LEVEL 4  ------- ");
        Log.i(TAG, "\n");


        Logic logic4 = databaseHelper.getLogic(logic3.getParentLogic());
        Boolean value4 = getLogicValue(logic4, ALL_DATA_JSON);
        Log.i(TAG, "\n");

        Log.i(TAG, " Value  " + value4);
        Log.i(TAG, "\n");


        //level 5
        Log.i(TAG, " ------  LEVEL 5  ------- ");
        Log.i(TAG, "\n");


        Logic logic5 = databaseHelper.getLogic(logic4.getParentLogic());
        Boolean value5 = getLogicValue(logic5, ALL_DATA_JSON);
        Log.i(TAG, "\n");

        Log.i(TAG, " Value  " + value5);
        Log.i(TAG, "\n");


        equation.append(value1)
                .append(logic.getParentLogicalOperator())
                .append(value2).append(logic2.getParentLogicalOperator())
                .append(value3).append(logic3.getParentLogicalOperator())
                .append(value4).append(logic4.getParentLogicalOperator())
                .append(value5);

        equationValue = equation.toString().replace("null", "");


        Log.i(TAG, "STRING BUILDER EQUATION IS " + equation.toString());
        Log.i(TAG, "FILTERED EQUATION IS " + equationValue);


        Boolean value = null;


        try {

            value = (Boolean) engine.eval(equationValue);


        } catch (ScriptException e) {
            System.out.println("*****  Exception  *****   " + e.getMessage());


        } finally {
            System.out.println("************  LOGIC VALUE IS: " + value);

        }
        return value;


    }






}
