package org.grameen.fdp.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.jaredrummler.materialspinner.MaterialSpinner;

import org.grameen.fdp.BuildConfig;
import org.grameen.fdp.R;
import org.grameen.fdp.adapter.MyTableHearderAdapter;
import org.grameen.fdp.adapter.MyTableViewAdapter;
import org.grameen.fdp.object.Calculation;
import org.grameen.fdp.object.ComplexCalculation;
import org.grameen.fdp.object.Data;
import org.grameen.fdp.object.LaborDaysLaborCost;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.object.RealFarmer;
import org.grameen.fdp.object.RealPlot;
import org.grameen.fdp.object.Recommendation;
import org.grameen.fdp.object.SkipLogic;
import org.grameen.fdp.object.SuppliesCost;
import org.grameen.fdp.utility.Callbacks;
import org.grameen.fdp.utility.Constants;
import org.grameen.fdp.utility.CustomToast;
import org.grameen.fdp.utility.DateUtil;
import org.grameen.fdp.utility.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import de.codecrafters.tableview.TableView;

import static org.grameen.fdp.utility.Constants.AGGREGATE_ECONOMIC_RESULTS;
import static org.grameen.fdp.utility.Constants.BUTTON_VIEW;
import static org.grameen.fdp.utility.Constants.TAG_OTHER_TEXT_VIEW;
import static org.grameen.fdp.utility.Constants.TAG_RESULTS;
import static org.grameen.fdp.utility.Constants.TAG_TITLE_TEXT_VIEW;
import static org.grameen.fdp.utility.Constants.TAG_VIEW;
import static org.grameen.fdp.utility.Constants.TYPE_TEXT;

/**
 * Created by aangjnr on 20/12/2017.
 */
public class PandLActivity extends BaseActivity implements Callbacks.NetworkActivityCompleteListener {

    String startYearId = "nil";
    String CSSV_ID = "nil";
    String CSSV_VALUE = "--";
    Boolean DID_LABOUR = false;
    String LABOUR_TYPE;
    Boolean shouldHideStartYear = null;
    String TAG = "P & L ACTIVITY";
    RealFarmer farmer;
    TextView farmerName;
    TextView farmerCode;
    TableView tableView;
    Button save;
    Button submit;
    TextView fdpStatus;
    TextView currency;
    JSONObject VALUES_JSON_OBJECT;
    ProgressDialog progressDialog;
    List<String> calculationsList;
    List<RealPlot> realPlotList;
    List<Data> TABLE_DATA_LIST ;
    List<String> TOTAL_LABOR_COST;
    List<String> TOTAL_LABOR_DAYS ;
    List<String> TOTAL_MAINTENANCE_COST;
    List<String> TOTAL_GROSS_INCOME_FROM_COCOA;
    List<String> TOTAL_NET_INCOME_FROM_OTHER_CROPS;
    List<String> TOTAL_FARMING_INCOME;
    List<String> TOTAL_NET_INCOME_FROM_OTHER_SOURCES;
    List<String> TOTAL_INCOME;
    List<String> NET_FAMILY_INCOME;
    String TOTAL_FAMILY_EXPENSES = "";
    List<String> TOTAL_INVESTMENT_IN_FDP;
    List<String> TOTAL_P_AND_L_LIST;
    List<StringBuilder> GROSS_COCOA_STRING_BUILDERS = new ArrayList<>();
    List<StringBuilder> MAINTENANCE_COST_STRING_BUILDERS = new ArrayList<>();
    List<StringBuilder> LABOR_DAYS_STRING_BUILDERS = new ArrayList<>();
    List<StringBuilder> LABOR_COST_STRING_BUILDERS = new ArrayList<>();
    MyTableViewAdapter myTableViewAdapter;
    private JSONObject PLOT_ANSWERS_JSON_OBJECT = new JSONObject();
    JSONObject PLOT_SIZES_IN_HA;
    boolean isTranslation;
    static final int MAX_YEARS = 7;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_p_and_l);


        if (BuildConfig.DEBUG) {

            findViewById(R.id.print).setVisibility(View.VISIBLE);

            findViewById(R.id.print).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    findViewById(R.id.bottom_buttons).setVisibility(View.GONE);
                    findViewById(R.id.fdpStatus).setVisibility(View.GONE);
                    findViewById(R.id.currency_layout).setVisibility(View.GONE);


                    progressDialog.setTitle("Initializing");
                    progressDialog.setMessage("Please wait...");
                    progressDialog.show();


                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            String fileLocation = captureScreenshot(findViewById(R.id.main_layout), "pandl");
                            progressDialog.dismiss();
                            if (fileLocation != null) {

                                Intent intent = new Intent(PandLActivity.this, PrintingActivity.class);
                                intent.putExtra("file_location", fileLocation);
                                startActivity(intent);

                                findViewById(R.id.bottom_buttons).setVisibility(View.VISIBLE);
                                findViewById(R.id.fdpStatus).setVisibility(View.VISIBLE);
                                findViewById(R.id.currency_layout).setVisibility(View.VISIBLE);


                            } else {
                                CustomToast.makeToast(PandLActivity.this, "Error starting the printer service!", Toast.LENGTH_LONG).show();
                            }
                        }
                    }, 2000);
                }
            });
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        isTranslation = prefs.getBoolean("toggleTranslation", false);
        engine = new ScriptEngineManager().getEngineByName("rhino");
        save = findViewById(R.id.save);
        submit = findViewById(R.id.submitAgreement);

        startYearId = databaseHelper.getQuestionIdByTranslationName("Start year");
        CSSV_ID = databaseHelper.getQuestionIdByTranslationNameAndFormType("CSSV", Constants.ADOPTION_OBSERVATIONS);

        currency = findViewById(R.id.currency);
        currency.setText(prefs.getString("currency", ""));

        farmerName = findViewById(R.id.name);
        farmerCode = findViewById(R.id.code);

        fdpStatus = findViewById(R.id.fdpStatus);
        fdpStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PandLActivity.this, FDPStatusActivity.class);
                intent.putExtra("farmer", new Gson().toJson(farmer));
                startActivity(intent);
            }
        });

        Log.d("ACTION TYPE", prefs.getString("flag", ""));
        if (prefs.getString("flag", "").equals(Constants.MONITORING)) {

            //Todo add the rest of the views to hide
            findViewById(R.id.save).setVisibility(View.GONE);
            findViewById(R.id.submitAgreement).setVisibility(View.GONE);
        }

        farmer = new Gson().fromJson(getIntent().getStringExtra("farmer"), RealFarmer.class);

        if (farmer != null) {
            farmerName.setText(farmer.getFarmerName());
            farmerCode.setText(farmer.getCode());
            if (farmer.getHasSubmitted().equalsIgnoreCase(Constants.YES) && farmer.getSyncStatus() == 1) {
                submit.setVisibility(View.GONE);
                findViewById(R.id.save).setVisibility(View.GONE);
            }
            getAllFarmerDataValues();

            submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (farmer.getHasSubmitted() == null || farmer.getHasSubmitted().equalsIgnoreCase(Constants.NO)) {
                        if (checkIfFarmerFdpStatusFormFilled(farmer.getId())) {
                            if (Utils.checkInternetConnection(PandLActivity.this)) {
                                showAlertDialog(true, getResources(R.string.caution), getResources(R.string.read_only_rational),
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                // databaseHelper.setAgreementSubmitted(farmer.getId());
                                                dialog.dismiss();
                                                SyncUpActivity.onNetworkActivityComplete(PandLActivity.this);

                                                Intent intent = new Intent(PandLActivity.this, SyncUpActivity.class);
                                                intent.putExtra("farmer", new Gson().toJson(farmer));
                                                intent.putExtra("submitAgreement", true);
                                                startActivity(intent);
                                            }
                                        }, getResources(R.string.ok), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        }, getResources(R.string.cancel), 0);
                            } else {
                                showAlertDialog(false, getResources(R.string.status_submitted), getResources(R.string.no_internet_connection_available) + "\n" + getResources(R.string.you_can_still_make_edits) + farmer.getFarmerName() + getResources(R.string.apostrophe_s) + getResources(R.string.data_before_sync),
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                                databaseHelper.setAgreementSubmitted(farmer.getId());
                                            }
                                        }, getResources(R.string.ok), null, "", 0);
                            }
                        } else {
                            showAlertDialog(true, getResources(R.string.fdp_status_incomplete), getResources(R.string.fill_out_fdp_status) + farmer.getFarmerName(),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    }, getResources(R.string.ok), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            Intent intent = new Intent(PandLActivity.this, FDPStatusActivity.class);
                                            intent.putExtra("farmer", new Gson().toJson(farmer));
                                            startActivity(intent);
                                        }
                                    }, getResources(R.string.go_to_fdp_status_form), 0);
                        }
                    } else if (farmer.getHasSubmitted().equalsIgnoreCase(Constants.YES)) {
                        showAlertDialog(false, getResources(R.string.status_submitted), getResources(R.string.no_more_mods) + farmer.getFarmerName() + getResources(R.string.apostrophe_s) + getResources(R.string.data),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        submit.setVisibility(View.GONE);
                                    }
                                }, getResources(R.string.ok), null, "", 0);
                    }
                }
            });
        }
        tableView = findViewById(R.id.tableView);
        tableView.setColumnCount(9);

        String[] TABLE_HEADERS = getResources().getStringArray(R.array.seven_years);

        MyTableHearderAdapter tableHearderAdapter = new MyTableHearderAdapter(this, TABLE_HEADERS);
        tableView.setHeaderAdapter(tableHearderAdapter);

        tableHearderAdapter.setHeaderClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = Integer.parseInt(view.getTag().toString());
                Intent intent = new Intent(PandLActivity.this, DetailedYearMonthlyActivity.class);
                intent.putExtra("year", position - 1);
                intent.putExtra("farmer", new Gson().toJson(farmer));
                intent.putExtra("labour", DID_LABOUR);
                intent.putExtra("labourType", LABOUR_TYPE);
                intent.putExtra("multiplier", PLOT_SIZES_IN_HA.toString());
                startActivity(intent);
            }
        });

        tableView.setSaveEnabled(true);
        progressDialog = showProgress(this, getResources(R.string.populating_data), getResources(R.string.please_wait), false);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                populateTableData();
            }
        });

        thread.start();
        onBackClicked();
    }


    void populateTableData() {

        TABLE_DATA_LIST = new ArrayList<>();
        TOTAL_LABOR_COST = new ArrayList<>();
        TOTAL_LABOR_DAYS = new ArrayList<>();
        TOTAL_MAINTENANCE_COST = new ArrayList<>();
        TOTAL_GROSS_INCOME_FROM_COCOA = new ArrayList<>();
        TOTAL_NET_INCOME_FROM_OTHER_CROPS = new ArrayList<>();
        TOTAL_FARMING_INCOME = new ArrayList<>();
        TOTAL_NET_INCOME_FROM_OTHER_SOURCES = new ArrayList<>();
        TOTAL_INCOME = new ArrayList<>();
        NET_FAMILY_INCOME = new ArrayList<>();
        TOTAL_INVESTMENT_IN_FDP = new ArrayList<>();
        TOTAL_P_AND_L_LIST = new ArrayList<>();
        GROSS_COCOA_STRING_BUILDERS = new ArrayList<>();
        MAINTENANCE_COST_STRING_BUILDERS = new ArrayList<>()  ;
        LABOR_DAYS_STRING_BUILDERS = new ArrayList<>();
        LABOR_COST_STRING_BUILDERS = new ArrayList<>();

        for(int i = 0; i <= MAX_YEARS; i++){
            GROSS_COCOA_STRING_BUILDERS.add(new StringBuilder());
            MAINTENANCE_COST_STRING_BUILDERS.add(new StringBuilder());
            LABOR_DAYS_STRING_BUILDERS.add(new StringBuilder());
            LABOR_COST_STRING_BUILDERS.add(new StringBuilder());
        }

        PLOT_SIZES_IN_HA = new JSONObject();

        if (farmer != null) {
            realPlotList = databaseHelper.getAllFarmerPlots(farmer.getId());
            for (RealPlot PLOT : realPlotList) {
                shouldHideStartYear = false;
                Log.d("P & L ACTIVITY", "START YEAR IS " + PLOT.getStartYear());
                try {
                    PLOT_ANSWERS_JSON_OBJECT = new JSONObject(PLOT.getPlotInformationJson());
                } catch (Exception e) {
                    e.printStackTrace();
                    PLOT_ANSWERS_JSON_OBJECT = new JSONObject();
                }
                if(PLOT.getStartYear() > 0){
                        loadDataForYear(PLOT, PLOT.getStartYear());

                }else if(PLOT.getStartYear() < 0){
                    loadDataForInterventionMadeYear(PLOT, PLOT.getStartYear());
                }
            }// For Loop ends

            for(int i = 0; i < MAX_YEARS + 1; i++){
                TOTAL_MAINTENANCE_COST.add(applyCalculation(MAINTENANCE_COST_STRING_BUILDERS.get(i).toString() + "0"));
                TOTAL_LABOR_DAYS.add(applyCalculation(LABOR_DAYS_STRING_BUILDERS.get(i).toString() + "0"));
                TOTAL_LABOR_COST.add(applyCalculation(LABOR_COST_STRING_BUILDERS.get(i).toString() + "0"));
                TOTAL_GROSS_INCOME_FROM_COCOA.add(applyCalculation(GROSS_COCOA_STRING_BUILDERS.get(i).toString() + "0.0"));
            }

            if (DID_LABOUR) {
                //TABLE_DATA_LIST.add(new Data("Total Costs" + prefs.getString("labourType", ""), null, TAG_TITLE_TEXT_VIEW));

                TABLE_DATA_LIST.add(new Data(getResources(R.string.total_maintenance_cost), TOTAL_MAINTENANCE_COST, TAG_RESULTS));
                TABLE_DATA_LIST.add(new Data(getResources(R.string.total_labour_days), TOTAL_LABOR_DAYS, TAG_RESULTS));
                TABLE_DATA_LIST.add(new Data(getResources(R.string.total_labour_cost), TOTAL_LABOR_COST, TAG_RESULTS));
            }

            TABLE_DATA_LIST.add(new Data("", null, TAG_OTHER_TEXT_VIEW));
            TABLE_DATA_LIST.add(new Data("", null, TAG_OTHER_TEXT_VIEW));

            Question nifc = databaseHelper.getQuestionByTranslation("Gross income from Cocoa");
            TABLE_DATA_LIST.add(new Data((isTranslation) ? nifc.getTranslation__c() : nifc.getCaption__c(), TOTAL_GROSS_INCOME_FROM_COCOA, TAG_RESULTS));

            for (Calculation calc :  databaseHelper.sortCalculations()) {

                Question q = databaseHelper.getQuestion(calc.getResultQuestion());
                if (q.getForm__r().getName().equalsIgnoreCase(AGGREGATE_ECONOMIC_RESULTS)) {
                    if (q.getTranslation__c().equalsIgnoreCase("Net income from other crops")) {
                        String value = applyCalculationToCalculationObject(calc);
                        calculationsList = new ArrayList<>();

                        for(int i = 0; i < MAX_YEARS + 1; i++)
                            calculationsList.add(value);

                        TOTAL_NET_INCOME_FROM_OTHER_CROPS = calculationsList;
                        TABLE_DATA_LIST.add(new Data((isTranslation) ?  q.getTranslation__c() : q.getCaption__c(), calculationsList, TAG_RESULTS));
                    } else if (q.getTranslation__c().equalsIgnoreCase("Total farming income")) {
                        calculationsList = new ArrayList<>();
                        for(int i = 0; i < MAX_YEARS + 1; i++) {
                            calculationsList.add(applyCalculation(TOTAL_GROSS_INCOME_FROM_COCOA.get(i) + "+" + TOTAL_NET_INCOME_FROM_OTHER_CROPS.get(i)));
                        }
                        TABLE_DATA_LIST.add(new Data((isTranslation) ?  q.getTranslation__c() : q.getCaption__c(), calculationsList, TAG_RESULTS));
                        TOTAL_FARMING_INCOME = calculationsList;
                    } else if (q.getTranslation__c().equalsIgnoreCase("Net income from all other sources")) {
                        String value = applyCalculationToCalculationObject(calc);
                        calculationsList = new ArrayList<>();
                        for(int i = 0; i < MAX_YEARS + 1; i++)
                            calculationsList.add(value);

                        TOTAL_NET_INCOME_FROM_OTHER_SOURCES = calculationsList;
                        TABLE_DATA_LIST.add(new Data((isTranslation) ?  q.getTranslation__c() : q.getCaption__c(), calculationsList, TAG_RESULTS));
                    } else if (q.getTranslation__c().equalsIgnoreCase("Total income")) {
                        calculationsList = new ArrayList<>();

                        for(int i = 0; i < MAX_YEARS + 1; i++)
                            calculationsList.add(applyCalculation(TOTAL_FARMING_INCOME.get(i) + "+" + TOTAL_NET_INCOME_FROM_OTHER_SOURCES.get(i)));

                        TABLE_DATA_LIST.add(new Data((isTranslation) ?  q.getTranslation__c() : q.getCaption__c(), calculationsList, TAG_RESULTS));
                        TOTAL_INCOME = calculationsList;
                    } else if (q.getTranslation__c().equalsIgnoreCase("Total family expenses")) {
                        String value = applyCalculationToCalculationObject(calc);
                        calculationsList = new ArrayList<>();
                        for(int i = 0; i < MAX_YEARS + 1; i++)
                            calculationsList.add(value);

                        TOTAL_FAMILY_EXPENSES = value;
                        TABLE_DATA_LIST.add(new Data((isTranslation) ?  q.getTranslation__c() : q.getCaption__c(), calculationsList, TAG_RESULTS));
                    } else if (q.getTranslation__c().equalsIgnoreCase("Net family income")) {

                        calculationsList = new ArrayList<>();
                        for(int i = 0; i < MAX_YEARS + 1; i++) {
                            calculationsList.add(applyCalculation(TOTAL_INCOME.get(i) + "-" + TOTAL_FAMILY_EXPENSES));
                        }

                        TABLE_DATA_LIST.add(new Data((isTranslation) ?  q.getTranslation__c() : q.getCaption__c(), calculationsList, TAG_RESULTS));
                        NET_FAMILY_INCOME = calculationsList;
                    }
                }
            }

            calculationsList = new ArrayList<>();

            for(int i = 0; i < MAX_YEARS + 1; i++)
                calculationsList.add(applyCalculation(TOTAL_MAINTENANCE_COST.get(i) + "+" + TOTAL_LABOR_COST.get(i)));

            Question investmentInFdpQuestion = databaseHelper.getQuestionByTranslation("Investment in FDP");
            TABLE_DATA_LIST.add(new Data((isTranslation) ?  investmentInFdpQuestion.getTranslation__c() : investmentInFdpQuestion.getCaption__c(), calculationsList, TAG_RESULTS));


            TOTAL_INVESTMENT_IN_FDP = calculationsList;
            calculationsList = new ArrayList<>();
            try {
                for(int i = 0; i < MAX_YEARS + 1; i++) {
                    calculationsList.add(applyCalculation(NET_FAMILY_INCOME.get(i) + "-" + TOTAL_INVESTMENT_IN_FDP.get(i)));
                }
            } catch (Exception ignored) {
                calculationsList = new ArrayList<>();
                for(int i = 0; i < MAX_YEARS + 1; i++) {
                    calculationsList.add(applyCalculation(TOTAL_INCOME.get(i) + "-" + TOTAL_INVESTMENT_IN_FDP.get(i)));
                }
            }

            Question pL = databaseHelper.getQuestionByTranslation("Profit or Loss");
            TABLE_DATA_LIST.add(new Data((isTranslation) ?  pL.getTranslation__c() : pL.getCaption__c(), calculationsList, TAG_RESULTS));
            TOTAL_P_AND_L_LIST = calculationsList;

            TABLE_DATA_LIST.add(new Data("", null, TAG_OTHER_TEXT_VIEW));
            TABLE_DATA_LIST.add(new Data("", null, TAG_OTHER_TEXT_VIEW));

            Question hhs = databaseHelper.getQuestionByTranslation("Household Savings");
            Question pI =  databaseHelper.getQuestionByTranslation("Does the household have any planned investments? If so, how much have they allocated for the investment");

            try {
                TABLE_DATA_LIST.add(new Data((isTranslation) ?  hhs.getTranslation__c() : hhs.getCaption__c(), VALUES_JSON_OBJECT.get(hhs.getId()).toString()));
            } catch (JSONException e) {
                e.printStackTrace();
                TABLE_DATA_LIST.add(new Data((isTranslation) ?  hhs.getTranslation__c() : hhs.getCaption__c(), "0.00"));
            }
             try {
                 TABLE_DATA_LIST.add(new Data(getResources(R.string.planned_investment), VALUES_JSON_OBJECT.get(pI.getId()).toString()));
            } catch (JSONException e) {
                e.printStackTrace();
                 TABLE_DATA_LIST.add(new Data(getResources(R.string.planned_investment), "0.00"));
            }

            TABLE_DATA_LIST.add(new Data("", null, TAG_OTHER_TEXT_VIEW));
            TABLE_DATA_LIST.add(new Data("", null, TAG_OTHER_TEXT_VIEW));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    myTableViewAdapter = new MyTableViewAdapter(PandLActivity.this, TABLE_DATA_LIST, tableView);
                    tableView.setDataAdapter(myTableViewAdapter);
                    myTableViewAdapter.setItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {
                            Log.i(TAG, "Spinner item selected with tag " + view.getTag());
                            Log.i(TAG, "Position is " + position);
                            if (databaseHelper.editPlotStartYear(String.valueOf(view.getTag().toString().split("_")[0]), position + 1)) {
                                progressDialog.setMessage(getResources(R.string.updating_table_data));
                                progressDialog.show();

                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        populateTableData();
                                    }
                                });
                                thread.start();
                            }
                        }
                    });

                    myTableViewAdapter.setClickistener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Recommendation PLOT_REC;
                            Recommendation GAPS_RECOMMENDATION_FOR_START_YEAR;

                            Log.i(TAG, "Button item clicked with tag " + view.getTag());

                            String[] values = view.getTag().toString().split("_");

                            String plotId = values[0];
                            final String RecName = values[1];


                            //if (prefs.getBoolean("toggleTranslation", false))
                                PLOT_REC = databaseHelper.getRecommendationBasedOnName(RecName);

                            if (PLOT_REC == null)
                                PLOT_REC = databaseHelper.getRecommendationBasedOnTranslationName(RecName);


                            if (PLOT_REC != null) {
                                if (PLOT_REC.getName().equalsIgnoreCase("Replant") || PLOT_REC.getName().equalsIgnoreCase("Replant + Extra soil")) {
                                    GAPS_RECOMMENDATION_FOR_START_YEAR = databaseHelper.getRecommendationBasedOnName("Minimal GAPs");
                                } else if (PLOT_REC.getName().equalsIgnoreCase("Grafting") || PLOT_REC.getName().equalsIgnoreCase("Grafting + Extra soil")) {
                                    GAPS_RECOMMENDATION_FOR_START_YEAR = databaseHelper.getRecommendationBasedOnName("Modest GAPs");
                                } else {
                                    GAPS_RECOMMENDATION_FOR_START_YEAR = databaseHelper.getRecommendationBasedOnName("Maintenance (GAPs)");
                                }

                                if (databaseHelper.editFarmerPlotRecommendationId(farmer.getId(), plotId,
                                        GAPS_RECOMMENDATION_FOR_START_YEAR.getId() + "," + PLOT_REC.getId())) {
                                    progressDialog.setMessage(getResources(R.string.updating_table_data));
                                    progressDialog.show();
                                    Thread thread = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            populateTableData();
                                        }
                                    });
                                    thread.start();
                                }
                            } else
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CustomToast.makeToast(PandLActivity.this, "DEBUG: Couldn't find recommendation with name " + RecName, Toast.LENGTH_LONG).show();
                                    }
                                });
                        }
                    });
                    progressDialog.dismiss();
                }
            });
        }
    }

    void getAllFarmerDataValues() {
        final Question labourQuestion = databaseHelper.getQuestionByTranslation("Labour");
        final Question labourTypeQuestion = databaseHelper.getQuestionByTranslation("Labour type");
        String jsonString = databaseHelper.getAllAnswersJson(farmer.getId());

            Log.d("P & L ACTIVITY", "FOUND STRING " + jsonString);

            if (jsonString != null && !jsonString.equals("null") && !jsonString.equals("") && !jsonString.equals("{}") && !jsonString.isEmpty() && !jsonString.equals("empty"))
                try {
                    VALUES_JSON_OBJECT = new JSONObject(jsonString);
                    try {
                        String val = VALUES_JSON_OBJECT.getString(labourQuestion.getId());
                        if (val.equalsIgnoreCase("Yes"))
                            DID_LABOUR = true;
                        LABOUR_TYPE = VALUES_JSON_OBJECT.getString(labourTypeQuestion.getId());
                        // CustomToast.makeToast(this, "Labour? " + val + " LABOUR TYPE = " + LABOUR_TYPE, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d("P & L ACTIVITY", "####### JSON ERROR" + e.getMessage());
                }
    }

    String getAnswerValue(String s) {
        String defVal = "0.0";
        try {
            defVal = VALUES_JSON_OBJECT.get(s).toString();
         } catch (JSONException e) {
            e.printStackTrace();
             try {
                defVal = PLOT_ANSWERS_JSON_OBJECT.get(s).toString();
             } catch (JSONException f) {
             }
        }
         return defVal;
    }

    String replaceStringWithValues(String name, String stringToReplace) {
        String id = databaseHelper.getQuestionId(name);
        //Todo complex calculation Application
        ComplexCalculation complexCalculation = databaseHelper.getComplexCalculation(id);
        if(complexCalculation != null){
            return stringToReplace.replace(name, applyComplexCalculation(complexCalculation));
        }else
        return stringToReplace.replace(name, getAnswerValue(id));
    }

    String applyCalculation(String equation) {
        System.out.println("EQUATION IS " + equation);
        String calculatedValue = "0";
        try {
            calculatedValue = calculate(equation);
        } catch (ScriptException e) {
            e.printStackTrace();
        }

        System.out.println("####### NEW VALUE IS " + calculatedValue);
        return calculatedValue;
    }

    String parseEquation(String unParsedEquation, String questionsInvolved) {
        String parsedEquation = unParsedEquation;
        Log.i(TAG, "\n*********** BEFORE PARSING ********  " + unParsedEquation);

        String[] questionsInvolvedItems = questionsInvolved.trim().split(",");

        for (String aQuestionInvolved : questionsInvolvedItems) {
            Log.i(TAG, "\n*********** REPLACING STRING ********  " + aQuestionInvolved + "\n");
            parsedEquation = replaceStringWithValues(aQuestionInvolved, parsedEquation);
        }

        Log.i(TAG, "############ AFTER PARSING  ############  " + parsedEquation + "\n");
        return parsedEquation;
    }

    String applyCalculationToCalculationObject(final Calculation calculation) {
        engine = new ScriptEngineManager().getEngineByName("rhino");

        String equation = "";

        if (calculation.getQuestion4() != null && !calculation.getQuestion4().equals("null")) {

            System.out.println("***************************** CALCULATION WITH 4 DATA VALUES");
            equation = getAnswerValue(calculation.getQuestion1()) + calculation.getOperator1()
                    + getAnswerValue(calculation.getQuestion2()) + calculation.getOperator2()
                    + getAnswerValue(calculation.getQuestion3()) + calculation.getOperator3()
                    + getAnswerValue(calculation.getQuestion4());


        } else if (calculation.getQuestion3() != null && !calculation.getQuestion3().equals("null")) {

            System.out.println("***************************** CALCULATION WITH 3 DATA VALUES");

            equation = getAnswerValue(calculation.getQuestion1()) + calculation.getOperator1()
                    + getAnswerValue(calculation.getQuestion2()) + calculation.getOperator2()
                    + getAnswerValue(calculation.getQuestion3());


        } else if (calculation.getQuestion2() != null && !calculation.getQuestion2().equals("null")) {

            System.out.println("***************************** CALCULATION WITH 2 DATA VALUES");
            equation = getAnswerValue(calculation.getQuestion1()) + calculation.getOperator1()
                    + getAnswerValue(calculation.getQuestion2());


        } else {
            System.out.println("***************************** CALCULATION WITH 1 DATA VALUES");
            equation = getAnswerValue(calculation.getQuestion1()) + " + 0";


        }

        System.out.println("EQUATION IS " + equation);

        String newValue = "";
        try {

            newValue = calculate(equation);


        } catch (ScriptException e) {
            Log.i("CALCULATION OBJ", e.getMessage());
            newValue = "0.0";
        }

        System.out.println("####### NEW CALC VALUE IS " + newValue);


        return newValue;
    }

    void loadDataForYear(RealPlot PLOT, int CONTROLLING_YEAR) {
        String plotSizeInHaValue = "0.0";
        try {
            Question plotSizeInHaQuestion = databaseHelper.getQuestionByTranslation("Plot area ha");

            //Todo complex calculation Application
            ComplexCalculation complexCalculation = databaseHelper.getComplexCalculation(plotSizeInHaQuestion.getId());
            if (complexCalculation != null) {
                plotSizeInHaValue = applyComplexCalculation(complexCalculation);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        Log.i(TAG, "^^^^^^^^^^^^^^   PLOT SIZE IN HA VALUE IS ^^^^^^^^^^^^^^^^^  " + plotSizeInHaValue);
        try {
            PLOT_SIZES_IN_HA.put(PLOT.getId(), plotSizeInHaValue);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        System.out.println("################################ \n CALCULATING INCOME \n ###################################");

        Log.i(TAG, "loadDataForYear " + CONTROLLING_YEAR);
        int TEMP = CONTROLLING_YEAR;
        int TEMP2 = CONTROLLING_YEAR;

        Log.i(TAG, "TEMP = " + TEMP);
        Log.i(TAG, "TEMP 2 =  " + TEMP2);

        String recommendationId = PLOT.getRecommendationId();
        String[] GAPS_PLOT_RECOMMENDATION_IDS = recommendationId.split(",");

        Recommendation GAPS_RECOMENDATION_FOR_START_YEAR = databaseHelper.getRecommendation(GAPS_PLOT_RECOMMENDATION_IDS[0]);
        Recommendation PLOT_RECOMMENDATION = databaseHelper.getRecommendation(GAPS_PLOT_RECOMMENDATION_IDS[1]);

        List<String> plotIncomes = new ArrayList<>();

        for(int i = 0; i < CONTROLLING_YEAR; i++){
            Log.i(TAG, "\nYEAR " + i);
            plotIncomes.add(applyCalculation(parseEquation(GAPS_RECOMENDATION_FOR_START_YEAR.getIncome0(), GAPS_RECOMENDATION_FOR_START_YEAR.getQuestionsInvolved())));
            GROSS_COCOA_STRING_BUILDERS.get(i).append(plotIncomes.get(i)).append("+");

        }

        int temp = 0;

        for (int i = CONTROLLING_YEAR; i <= MAX_YEARS; i++) {
            temp++;
            switch (temp) {
                case 1:
                    Log.i(TAG, "\nYEAR 1 " + PLOT_RECOMMENDATION.getIncome1());
                    plotIncomes.add(applyCalculation(parseEquation(PLOT_RECOMMENDATION.getIncome1(), PLOT_RECOMMENDATION.getQuestionsInvolved())));
                    GROSS_COCOA_STRING_BUILDERS.get(i).append(plotIncomes.get(i)).append("+");
                    break;
                case 2:
                    Log.i(TAG, "\nYEAR 2 " + PLOT_RECOMMENDATION.getIncome2());
                    plotIncomes.add(applyCalculation(parseEquation(PLOT_RECOMMENDATION.getIncome2(), PLOT_RECOMMENDATION.getQuestionsInvolved())));
                    GROSS_COCOA_STRING_BUILDERS.get(i).append(plotIncomes.get(i)).append("+");
                    break;
                case 3:
                    Log.i(TAG, "\nYEAR 3 " + PLOT_RECOMMENDATION.getIncome3());
                    plotIncomes.add(applyCalculation(parseEquation(PLOT_RECOMMENDATION.getIncome3(), PLOT_RECOMMENDATION.getQuestionsInvolved())));
                    GROSS_COCOA_STRING_BUILDERS.get(i).append(plotIncomes.get(i)).append("+");
                    break;
                case 4:
                    Log.i(TAG, "\nYEAR 4 " + PLOT_RECOMMENDATION.getIncome4());
                    plotIncomes.add(applyCalculation(parseEquation(PLOT_RECOMMENDATION.getIncome4(), PLOT_RECOMMENDATION.getQuestionsInvolved())));
                    GROSS_COCOA_STRING_BUILDERS.get(i).append(plotIncomes.get(i)).append("+");
                    break;
                case 5:
                    Log.i(TAG, "\nYEAR 5 " + PLOT_RECOMMENDATION.getIncome5());
                    plotIncomes.add(applyCalculation(parseEquation(PLOT_RECOMMENDATION.getIncome5(), PLOT_RECOMMENDATION.getQuestionsInvolved())));
                    GROSS_COCOA_STRING_BUILDERS.get(i).append(plotIncomes.get(i)).append("+");
                    break;
                case 6:
                    Log.i(TAG, "\nYEAR 6 " + PLOT_RECOMMENDATION.getIncome6());
                    plotIncomes.add(applyCalculation(parseEquation(PLOT_RECOMMENDATION.getIncome6(), PLOT_RECOMMENDATION.getQuestionsInvolved())));
                    GROSS_COCOA_STRING_BUILDERS.get(i).append(plotIncomes.get(i)).append("+");
                    break;
                case 7:
                    Log.i(TAG, "\nYEAR 7 " + PLOT_RECOMMENDATION.getIncome7());
                    plotIncomes.add(applyCalculation(parseEquation(PLOT_RECOMMENDATION.getIncome7(), PLOT_RECOMMENDATION.getQuestionsInvolved())));
                    GROSS_COCOA_STRING_BUILDERS.get(i).append(plotIncomes.get(i)).append("+");
                    break;
            }
            if(TEMP == MAX_YEARS) break;

            TEMP += 1;

            Log.i(TAG, "^^^^^^^^^^^^^^    TEMP IS NOW " + TEMP);
        }

        String name = (prefs.getBoolean("toggleTranslation", false)) ? PLOT_RECOMMENDATION.getName() : PLOT_RECOMMENDATION.getTranslation();
        TABLE_DATA_LIST.add(new Data(PLOT.getName() + "\n" + name, null, TAG_TITLE_TEXT_VIEW));
        Question plotIncomeQuestion = databaseHelper.getQuestionByTranslation("Plot income");
        TABLE_DATA_LIST.add(new Data((isTranslation) ? plotIncomeQuestion.getTranslation__c() : plotIncomeQuestion.getCaption__c(), plotIncomes, TAG_OTHER_TEXT_VIEW));
        //Todo maintenance cost, labor codt and labor days * plot size in ha


        //This is also known as Supplies cost. The selected year is always with   the (GAPS) recommendation obtained in conjunction with the plot recommendation attained
        List<String> maintenanceCostList = new ArrayList<>();

        List<String> labourCostList = new ArrayList<>();
        List<String> labourDaysList = new ArrayList<>();
        List<String> pandlist = new ArrayList<>();


        System.out.println("################################ \n CALCULATING MAINTENANCE COST \n ###################################");

        LaborDaysLaborCost lls1;
        SuppliesCost supplies;

        System.out.println("################################ \n CALCULATING LABOUR DAYS AND COST FOR YEAR 0 to SELECTED START YEAR (1 = DEFAULT) \n ###################################");


        for(int i = 0; i < CONTROLLING_YEAR; i++){


            if(i == 0){
                Log.i(TAG, "\nYEAR " + i);
                maintenanceCostList.add(applyCalculation(parseEquation(GAPS_RECOMENDATION_FOR_START_YEAR.getCost0(), GAPS_RECOMENDATION_FOR_START_YEAR.getCostQuestions())));
                MAINTENANCE_COST_STRING_BUILDERS.get(i).append(maintenanceCostList.get(i)).append("+");

            }else {

                supplies = databaseHelper.getTotalSuppliesCostByYear("1", GAPS_RECOMENDATION_FOR_START_YEAR.getId());

                maintenanceCostList.add(applyCalculation("(" + supplies.getSuppliesCost() + ") * " + plotSizeInHaValue));
                MAINTENANCE_COST_STRING_BUILDERS.get(i).append(  maintenanceCostList.get(i)).append("+");
            }


            if (DID_LABOUR) {

                if (LABOUR_TYPE.equalsIgnoreCase("full"))
                    lls1 = databaseHelper.getTotalLaborDaysLaborCostByYear("1", GAPS_RECOMENDATION_FOR_START_YEAR.getId());
                else
                    lls1 = databaseHelper.getTotalSeasonalLaborDaysLaborCostByYear("1", GAPS_RECOMENDATION_FOR_START_YEAR.getId(), "true");



                labourCostList.add(applyCalculation( "(" + lls1.getLaborCost() + ") * " + plotSizeInHaValue));
                labourDaysList.add(applyCalculation( "(" + lls1.getLaborDays() + ") * " + plotSizeInHaValue));


            } else {
                labourCostList.add("0.0");
                labourDaysList.add("0.0");

            }

            LABOR_COST_STRING_BUILDERS.get(i).append(labourCostList.get(i)).append("+");
            LABOR_DAYS_STRING_BUILDERS.get(i).append(labourDaysList.get(i)).append("+");

        }
        System.out.println("################################ \n CALCULATING LABOUR DAYS AND COST \n ###################################");


        for(int i = 1; i < MAX_YEARS + 1; i++) {
            supplies = databaseHelper.getTotalSuppliesCostByYear(String.valueOf(i), PLOT_RECOMMENDATION.getId());
            maintenanceCostList.add(applyCalculation("(" + supplies.getSuppliesCost() + ") * " + plotSizeInHaValue));
            if (DID_LABOUR) {

                if (LABOUR_TYPE.equalsIgnoreCase("full"))
                    lls1 = databaseHelper.getTotalLaborDaysLaborCostByYear(String.valueOf(i), PLOT_RECOMMENDATION.getId());
                else
                    lls1 = databaseHelper.getTotalSeasonalLaborDaysLaborCostByYear(String.valueOf(i), PLOT_RECOMMENDATION.getId(), "true");

                        labourCostList.add(applyCalculation( "(" + lls1.getLaborCost() + ") * " + plotSizeInHaValue));
                        labourDaysList.add(applyCalculation( "(" + lls1.getLaborDays() + ") * " + plotSizeInHaValue));
                    } else {
                        labourCostList.add("0.0");
                        labourDaysList.add("0.0");
                    }
                    MAINTENANCE_COST_STRING_BUILDERS.get(TEMP2).append(maintenanceCostList.get(TEMP2)).append("+");
                    LABOR_COST_STRING_BUILDERS.get(TEMP2).append(labourCostList.get(TEMP2)).append("+");
                    LABOR_DAYS_STRING_BUILDERS.get(TEMP2).append(labourDaysList.get(TEMP2)).append("+");

            if(TEMP2 == MAX_YEARS) break;

            TEMP2 += 1;
        }
        for(int i = 0; i < MAX_YEARS + 1; i++){
            pandlist.add(applyCalculation(plotIncomes.get(i) + "-" + "(" + maintenanceCostList.get(i) + "+" + labourCostList.get(i) + ")"));
        }


        Log.i(TAG, "AFTER : TEMP = " + TEMP);
        Log.i(TAG, "AFTER : TEMP 2 =  " + TEMP2);

        Question mcQ = databaseHelper.getQuestionByTranslation("Cost of maintenance/investment");
        TABLE_DATA_LIST.add(new Data((isTranslation) ? mcQ.getTranslation__c() : mcQ.getCaption__c(), maintenanceCostList, TAG_OTHER_TEXT_VIEW));

        Question ldn = databaseHelper.getQuestionByTranslation("Labor needed in man days");
        TABLE_DATA_LIST.add(new Data((isTranslation) ? ldn.getTranslation__c() : ldn.getCaption__c(), labourDaysList, TAG_OTHER_TEXT_VIEW));


        Question lc = databaseHelper.getQuestionByTranslation("Cost of labor");
        TABLE_DATA_LIST.add(new Data((isTranslation) ? lc.getTranslation__c() : lc.getCaption__c(), labourCostList, TAG_OTHER_TEXT_VIEW));

        Question pl = databaseHelper.getQuestionByTranslation("Plot P&L");
        TABLE_DATA_LIST.add(new Data((isTranslation) ? pl.getTranslation__c() : pl.getCaption__c(), pandlist, TAG_OTHER_TEXT_VIEW));


        List<Question> plotResultsQuestions = databaseHelper.getSpecificSetOfQuestions("plot results");
        if(plotResultsQuestions != null){
            for (Question q : plotResultsQuestions){
                if(q.getType__c().equalsIgnoreCase(TYPE_TEXT)){
                    try {

                        SkipLogic skipLogic = databaseHelper.doesQuestionHaveSkipLogic(q.getId());

                        if( skipLogic != null && setupSkipLogicsAndHideViews(skipLogic))
                            TABLE_DATA_LIST.add(new Data((prefs.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), skipLogic.getAnswerValue()));
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
        }


        showOrHideStartYear(PLOT.getId(), PLOT.getStartYear() - 1);


        Question plotRenovatedCorrectlyQuestion = databaseHelper.getQuestionByTranslation("Plot Renovated Correctly?");

        if (plotRenovatedCorrectlyQuestion != null) {
            if (!getAnswerValue(plotRenovatedCorrectlyQuestion.getId()).equalsIgnoreCase("yes"))
                if ((PLOT_RECOMMENDATION.getName().equalsIgnoreCase("Grafting")) ||
                        PLOT_RECOMMENDATION.getName().equalsIgnoreCase("Thinning Out") ||
                        PLOT_RECOMMENDATION.getName().equalsIgnoreCase("Filling In")) {

                    prefs.edit().putString(PLOT.getId(), PLOT_RECOMMENDATION.getName()).apply();

                    TABLE_DATA_LIST.add(new Data(PLOT.getId() + "_Replant", null, BUTTON_VIEW));


                } else if (PLOT_RECOMMENDATION.getName().equalsIgnoreCase("Replant")) {


                    if (prefs.getString(PLOT.getId(), "").equalsIgnoreCase("thinning out"))

                        TABLE_DATA_LIST.add(new Data(PLOT.getId() + "_Thinning out", null, BUTTON_VIEW));

                    else if (prefs.getString(PLOT.getId(), "").equalsIgnoreCase("filling in"))

                        TABLE_DATA_LIST.add(new Data(PLOT.getId() + "_Filling in", null, BUTTON_VIEW));

                    else if (prefs.getString(PLOT.getId(), "").equalsIgnoreCase("grafting"))

                        TABLE_DATA_LIST.add(new Data(PLOT.getId() + "_Grafting", null, BUTTON_VIEW));


                } else if (PLOT_RECOMMENDATION.getName().equalsIgnoreCase("Filling in + Extra Soil")
                        || PLOT_RECOMMENDATION.getName().equalsIgnoreCase("Thinning out + Extra Soil")
                        || PLOT_RECOMMENDATION.getName().equalsIgnoreCase("Grafting + Extra Soil")) {

                    prefs.edit().putString(PLOT.getId(), PLOT_RECOMMENDATION.getName()).apply();
                    TABLE_DATA_LIST.add(new Data(PLOT.getId() + "_Replant + Extra Soil", null, BUTTON_VIEW));


                } else if (PLOT_RECOMMENDATION.getName().equalsIgnoreCase("Replant + Extra Soil")) {


                    if (prefs.getString(PLOT.getId(), "").equalsIgnoreCase("Thinning out + Extra Soil"))

                        TABLE_DATA_LIST.add(new Data(PLOT.getId() + "_Thinning out + Extra Soil", null, BUTTON_VIEW));

                    else if (prefs.getString(PLOT.getId(), "").equalsIgnoreCase("Filling in + Extra Soil"))

                        TABLE_DATA_LIST.add(new Data(PLOT.getId() + "_Filling in + Extra Soil", null, BUTTON_VIEW));
                    else if (prefs.getString(PLOT.getId(), "").equalsIgnoreCase("Grafting + Extra Soil"))

                        TABLE_DATA_LIST.add(new Data(PLOT.getId() + "_Grafting + Extra Soil", null, BUTTON_VIEW));
                }
        }
        TABLE_DATA_LIST.add(new Data("", null, TAG_OTHER_TEXT_VIEW));
    }


    void loadDataForInterventionMadeYear(RealPlot PLOT, int CONTROLLING_YEAR) {
        Log.i(TAG, "loadDataForYear " + CONTROLLING_YEAR);
        String recommendationId = PLOT.getRecommendationId();
        String[] GAPS_PLOT_RECOMMENDATION_IDS = recommendationId.split(",");

        int TEMP = CONTROLLING_YEAR * -1;
        int TEMP2 = CONTROLLING_YEAR * -1;

        Log.i(TAG, "TEMP = " + TEMP);
        //Log.i(TAG, "TEMP2 =  " + TEMP2);

        Recommendation GAPS_RECOMENDATION_FOR_START_YEAR = databaseHelper.getRecommendation(GAPS_PLOT_RECOMMENDATION_IDS[0]);
        Recommendation PLOT_RECOMMENDATION = databaseHelper.getRecommendation(GAPS_PLOT_RECOMMENDATION_IDS[1]);

        List<String> plotIncomes = new ArrayList<>();

        Log.i(TAG, "\nYEAR 0 " + PLOT_RECOMMENDATION.getIncome1());
        plotIncomes.add(applyCalculation(parseEquation(GAPS_RECOMENDATION_FOR_START_YEAR.getIncome0(), GAPS_RECOMENDATION_FOR_START_YEAR.getQuestionsInvolved())));
        GROSS_COCOA_STRING_BUILDERS.get(0).append(plotIncomes.get(0)).append("+");

        for (int i = 1; i <= MAX_YEARS; i++) {
            TEMP += 1;
            switch (TEMP) {
                case 1:
                    Log.i(TAG, "\nYEAR 1 " + PLOT_RECOMMENDATION.getIncome1());
                    plotIncomes.add(applyCalculation(parseEquation(PLOT_RECOMMENDATION.getIncome1(), PLOT_RECOMMENDATION.getQuestionsInvolved())));
                    GROSS_COCOA_STRING_BUILDERS.get(i).append(plotIncomes.get(i)).append("+");

                    break;
                case 2:

                    Log.i(TAG, "\nYEAR 2 " + PLOT_RECOMMENDATION.getIncome2());
                    plotIncomes.add(applyCalculation(parseEquation(PLOT_RECOMMENDATION.getIncome2(), PLOT_RECOMMENDATION.getQuestionsInvolved())));
                    GROSS_COCOA_STRING_BUILDERS.get(i).append(plotIncomes.get(i)).append("+");
                    break;
                case 3:
                    Log.i(TAG, "\nYEAR 3 " + PLOT_RECOMMENDATION.getIncome3());
                    plotIncomes.add(applyCalculation(parseEquation(PLOT_RECOMMENDATION.getIncome3(), PLOT_RECOMMENDATION.getQuestionsInvolved())));
                    GROSS_COCOA_STRING_BUILDERS.get(i).append(plotIncomes.get(i)).append("+");

                    break;
                case 4:
                    Log.i(TAG, "\nYEAR 4 " + PLOT_RECOMMENDATION.getIncome4());
                    plotIncomes.add(applyCalculation(parseEquation(PLOT_RECOMMENDATION.getIncome4(), PLOT_RECOMMENDATION.getQuestionsInvolved())));
                    GROSS_COCOA_STRING_BUILDERS.get(i).append(plotIncomes.get(i)).append("+");

                    break;
                case 5:

                    Log.i(TAG, "\nYEAR 5 " + PLOT_RECOMMENDATION.getIncome5());
                    plotIncomes.add(applyCalculation(parseEquation(PLOT_RECOMMENDATION.getIncome5(), PLOT_RECOMMENDATION.getQuestionsInvolved())));
                    GROSS_COCOA_STRING_BUILDERS.get(i).append(plotIncomes.get(i)).append("+");

                    break;
                case 6:
                    Log.i(TAG, "\nYEAR 6 " + PLOT_RECOMMENDATION.getIncome6());
                    plotIncomes.add(applyCalculation(parseEquation(PLOT_RECOMMENDATION.getIncome6(), PLOT_RECOMMENDATION.getQuestionsInvolved())));
                    GROSS_COCOA_STRING_BUILDERS.get(i).append(plotIncomes.get(i)).append("+");

                    break;
                default:
                        Log.i(TAG, "\nYEAR 7 " + PLOT_RECOMMENDATION.getIncome7());
                        plotIncomes.add(applyCalculation(parseEquation(PLOT_RECOMMENDATION.getIncome7(), PLOT_RECOMMENDATION.getQuestionsInvolved())));
                        GROSS_COCOA_STRING_BUILDERS.get(i).append(plotIncomes.get(i)).append("+");
            }
            //if(TEMP == MAX_YEARS ) break;
            Log.i(TAG, "^^^^^^^^^^^^^^    TEMP IS NOW " + TEMP);

        }

        String name = (prefs.getBoolean("toggleTranslation", false)) ? PLOT_RECOMMENDATION.getName() : PLOT_RECOMMENDATION.getTranslation();

        TABLE_DATA_LIST.add(new Data(PLOT.getName() + "\n" + name + "\nYear " + PLOT.getStartYear(), null, TAG_TITLE_TEXT_VIEW));

        Question plotIncomeQuestion = databaseHelper.getQuestionByTranslation("Plot income");


        TABLE_DATA_LIST.add(new Data((isTranslation) ? plotIncomeQuestion.getTranslation__c() : plotIncomeQuestion.getCaption__c(), plotIncomes, TAG_OTHER_TEXT_VIEW));

        String plotSizeInHaValue = "0.0";
        try {
            Question plotSizeInHaQuestion = databaseHelper.getQuestionByTranslation("Plot area ha");

            //Todo complex calculation Application
            ComplexCalculation complexCalculation = databaseHelper.getComplexCalculation(plotSizeInHaQuestion.getId());
            if (complexCalculation != null) {
                plotSizeInHaValue = applyComplexCalculation(complexCalculation);
            }
        }catch(Exception e){e.printStackTrace();}

        Log.i(TAG, "^^^^^^^^^^^^^^   PLOT SIZE IN HA VALUE IS ^^^^^^^^^^^^^^^^^  " + plotSizeInHaValue);


        try {
            PLOT_SIZES_IN_HA.put(PLOT.getId(), plotSizeInHaValue);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ////////

        //This is also known as Supplies cost. The selected year is always with   the (GAPS) recommendation obtained in conjunction with the plot recommendation attained
        List<String> maintenanceCostList = new ArrayList<>();

        List<String> labourCostList = new ArrayList<>();
        List<String> labourDaysList = new ArrayList<>();
        List<String> pandlist = new ArrayList<>();

        LaborDaysLaborCost lls1;
        SuppliesCost supplies;

        TEMP = 0;

        System.out.println();
        System.out.println("**********************************************");
        System.out.println("MAINTENANCE COST AND LABOUR COST CALCULATIONS");
        System.out.println("**********************************************");
        System.out.println();


        Log.i(TAG, "\nYEAR " + 0);
        maintenanceCostList.add(applyCalculation(parseEquation(GAPS_RECOMENDATION_FOR_START_YEAR.getCost0(), GAPS_RECOMENDATION_FOR_START_YEAR.getCostQuestions())));
        MAINTENANCE_COST_STRING_BUILDERS.get(0).append(maintenanceCostList.get(0)).append("+");


        if (DID_LABOUR) {
            labourCostList.add(applyCalculation("0.0"));
            labourDaysList.add(applyCalculation("0.0"));
        } else {
            labourCostList.add("0.0");
            labourDaysList.add("0.0");
        }

        LABOR_COST_STRING_BUILDERS.get(0).append(labourCostList.get(0)).append("+");
        LABOR_DAYS_STRING_BUILDERS.get(0).append(labourDaysList.get(0)).append("+");


        for (int i = 1; i <= MAX_YEARS; i++) {

            System.out.println("**********************************************");
            System.out.println("TEMP 2 = " + TEMP2 + " and i = " + i);
            System.out.println("**********************************************");

            TEMP = TEMP2 + i;

            if (TEMP <= MAX_YEARS) {

                supplies = databaseHelper.getTotalSuppliesCostByYear(String.valueOf(TEMP), PLOT_RECOMMENDATION.getId());

                maintenanceCostList.add(applyCalculation("(" + supplies.getSuppliesCost() + ") * " + plotSizeInHaValue));
                MAINTENANCE_COST_STRING_BUILDERS.get(i).append(maintenanceCostList.get(i)).append("+");

                if (DID_LABOUR) {

                    if (LABOUR_TYPE.equalsIgnoreCase("full"))
                        lls1 = databaseHelper.getTotalLaborDaysLaborCostByYear(String.valueOf(TEMP), PLOT_RECOMMENDATION.getId());
                    else
                        lls1 = databaseHelper.getTotalSeasonalLaborDaysLaborCostByYear(String.valueOf(TEMP), PLOT_RECOMMENDATION.getId(), "true");

                    labourCostList.add(applyCalculation("(" + lls1.getLaborCost() + ") * " + plotSizeInHaValue));
                    labourDaysList.add(applyCalculation("(" + lls1.getLaborDays() + ") * " + plotSizeInHaValue));
                } else {
                    labourCostList.add("0.0");
                    labourDaysList.add("0.0");
                }

                LABOR_COST_STRING_BUILDERS.get(i).append(labourCostList.get(i)).append("+");
                LABOR_DAYS_STRING_BUILDERS.get(i).append(labourDaysList.get(i)).append("+");

            } else {
                supplies = databaseHelper.getTotalSuppliesCostByYear("7", PLOT_RECOMMENDATION.getId());
                maintenanceCostList.add(applyCalculation("(" + supplies.getSuppliesCost() + ") * " + plotSizeInHaValue));
                if (DID_LABOUR) {
                    if (LABOUR_TYPE.equalsIgnoreCase("full"))
                        lls1 = databaseHelper.getTotalLaborDaysLaborCostByYear("7", PLOT_RECOMMENDATION.getId());
                    else
                        lls1 = databaseHelper.getTotalSeasonalLaborDaysLaborCostByYear("7", PLOT_RECOMMENDATION.getId(), "true");

                    labourCostList.add(applyCalculation("(" + lls1.getLaborCost() + ") * " + plotSizeInHaValue));
                    labourDaysList.add(applyCalculation("(" + lls1.getLaborDays() + ") * " + plotSizeInHaValue));
                } else {
                    labourCostList.add("0.0");
                    labourDaysList.add("0.0");
                }

                MAINTENANCE_COST_STRING_BUILDERS.get(i).append(maintenanceCostList.get(i)).append("+");
                LABOR_COST_STRING_BUILDERS.get(i).append(labourCostList.get(i)).append("+");
                LABOR_DAYS_STRING_BUILDERS.get(i).append(labourDaysList.get(i)).append("+");
            }
        }

        for(int i = 0; i < MAX_YEARS + 1; i++)
            pandlist.add(applyCalculation(plotIncomes.get(i) + "-" + "(" + maintenanceCostList.get(i) + "+" + labourCostList.get(i) + ")"));

        Question mcQ = databaseHelper.getQuestionByTranslation("Cost of maintenance/investment");
        TABLE_DATA_LIST.add(new Data((isTranslation) ? mcQ.getTranslation__c() : mcQ.getCaption__c(), maintenanceCostList, TAG_OTHER_TEXT_VIEW));

        Question ldn = databaseHelper.getQuestionByTranslation("Labor needed in man days");
        TABLE_DATA_LIST.add(new Data((isTranslation) ? ldn.getTranslation__c() : ldn.getCaption__c(), labourDaysList, TAG_OTHER_TEXT_VIEW));

        Question lc = databaseHelper.getQuestionByTranslation("Cost of labor");
        TABLE_DATA_LIST.add(new Data((isTranslation) ? lc.getTranslation__c() : lc.getCaption__c(), labourCostList, TAG_OTHER_TEXT_VIEW));

        Question pl = databaseHelper.getQuestionByTranslation("Plot P&L");
        TABLE_DATA_LIST.add(new Data((isTranslation) ? pl.getTranslation__c() : pl.getCaption__c(), pandlist, TAG_OTHER_TEXT_VIEW));

        List<Question> plotResultsQuestions = databaseHelper.getSpecificSetOfQuestions("plot results");
        if (plotResultsQuestions != null) {
            for (Question q : plotResultsQuestions) {
                if (q.getType__c().equalsIgnoreCase(TYPE_TEXT)) {
                    try {
                        SkipLogic skipLogic = databaseHelper.doesQuestionHaveSkipLogic(q.getId());
                        if (skipLogic != null && setupSkipLogicsAndHideViews(skipLogic))
                            TABLE_DATA_LIST.add(new Data((prefs.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), skipLogic.getAnswerValue()));
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        //showOrHideStartYear(PLOT.getId(), PLOT.getStartYear() - 1);

    /*    if (PLOT_RECOMMENDATION.getName().equalsIgnoreCase("grafting")) {
            if (prefs.getString("ISO", "").equalsIgnoreCase("CIV") || prefs.getString("ISO", "").equalsIgnoreCase("GHA"))
                TABLE_DATA_LIST.add(new Data(PLOT.getId() + "_Replant", null, BUTTON_VIEW));
            else if (prefs.getString("ISO", "").equalsIgnoreCase("IDN")) {
                try {
                     int treeAge = Integer.parseInt(PLOT_ANSWERS_JSON_OBJECT.get(prefs.getString("tree age", "")).toString());
                    Log.i(TAG, "^^^^^^^^ TREE AGE IS  ^^^^^^^^^" + treeAge);
                    if (treeAge >= 21 && treeAge <= 25)
                        TABLE_DATA_LIST.add(new Data(PLOT.getId() + "_Replant", null, BUTTON_VIEW));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else if (PLOT_RECOMMENDATION.getName().equalsIgnoreCase("Thinning Out") ||
                PLOT_RECOMMENDATION.getName().equalsIgnoreCase("Filling In")) {
            prefs.edit().putString(PLOT.getId(), PLOT_RECOMMENDATION.getName()).apply();
            TABLE_DATA_LIST.add(new Data(PLOT.getId() + "_Replant", null, BUTTON_VIEW));
        } else if (PLOT_RECOMMENDATION.getName().equalsIgnoreCase("Replant")) {
            if (prefs.getString(PLOT.getId(), "").equalsIgnoreCase("thinning out"))
                TABLE_DATA_LIST.add(new Data(PLOT.getId() + "_Thinning Out", null, BUTTON_VIEW));
            else if (prefs.getString(PLOT.getId(), "").equalsIgnoreCase("filling in"))
                TABLE_DATA_LIST.add(new Data(PLOT.getId() + "_Filling In", null, BUTTON_VIEW));
        }*/
        TABLE_DATA_LIST.add(new Data("", null, TAG_OTHER_TEXT_VIEW));
    }


    void showOrHideStartYear(String plotId, int year){
        Log.i(TAG, "\n\n$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$\n\n");
        Boolean value  = false;
        CSSV_VALUE = "--";
        if (PLOT_ANSWERS_JSON_OBJECT != null) {
            try {
                CSSV_VALUE = PLOT_ANSWERS_JSON_OBJECT.getString(CSSV_ID);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        List<SkipLogic> skipLogics = databaseHelper.doesQuestionHaveSkipLogics(CSSV_ID);
        if (skipLogics != null && skipLogics.size() > 0) {
            for (SkipLogic sl : skipLogics) {
                try {
                    value = compareValues(sl, CSSV_VALUE) == sl.getActionToBeTaken().equalsIgnoreCase(Constants.HIDE);
                } catch (Exception ignored) {
                }
            }
        }
        if(!value)
            TABLE_DATA_LIST.add(new Data(plotId + "_" + year, null, TAG_VIEW));
    }


   private  boolean setupSkipLogicsAndHideViews(SkipLogic sl) {
        Boolean value = null;
                    try {
                        if (compareValues(sl, getAnswerValue(sl.getQuestionId()))) {
                            Log.i(TAG, "COMPARING VALUES EVALUATED TO " + true);
                            value = sl.getActionToBeTaken().equalsIgnoreCase(Constants.HIDE);
                        }
                    } catch (Exception ignored) {
                        ignored.printStackTrace();
                        Log.i(TAG, "INITIALIZING SKIP LOGIC " + ignored.getMessage());
            }
            return value;
    }


    String applyComplexCalculation(ComplexCalculation complexCalculation) {
        String evaluatedValue = "0.0";
        System.out.println("####### COMPLEX CALCULATION ------ ");
        String formula = complexCalculation.getCondition();

        try {

            String questionName;
            String valueToCompare;
            String trueValueName;
            String falseValueEquation;
            String section = formula;


            section = section.replace("IF", "");
            section = section.replace("(", "");
            section = section.replace(")", "");
            section = section.replace(" ", "");

            String[] discard = section.split("==");

            questionName = discard[0];
            String[] values = discard[1].split(",");
            valueToCompare = values[0];
            trueValueName = values[1];

            falseValueEquation = values[2];
            String value = getAnswerValue(databaseHelper.getQuestionByName(questionName).getId());

            System.out.println("####### COMPLEX CALCULATION ------ VALUE = " + value);
            System.out.println("####### COMPLEX CALCULATION ------ VALUE TO COMPARE = " + valueToCompare);

            if (!value.equals("--")) {
                if (value.equalsIgnoreCase(valueToCompare)) {
                    String trueValueId = databaseHelper.getQuestionByName(trueValueName).getId();
                    evaluatedValue =  getAnswerValue(trueValueId);
                } else {
                    engine = new ScriptEngineManager().getEngineByName("rhino");
                    String operator = "*";

                    if (falseValueEquation.contains("+")) operator = "+";
                    else if (falseValueEquation.contains("*")) operator = "*";
                    else if (falseValueEquation.contains("-")) operator = "-";
                    else if (falseValueEquation.contains("/")) operator = "/";

                    Log.i(TAG, "OPERATOR IS IS " + operator);
                    String[] vals = falseValueEquation.split("[-+*/]");
                    String falseName = vals[0];
                    String constant = vals[1];

                    String falseValueId = databaseHelper.getQuestionByName(falseName).getId();
                    evaluatedValue =  String.valueOf( engine.eval(getAnswerValue(falseValueId) + operator + constant));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("####### COMPLEX CALCULATION ------ EVALUATED VALUE = " + evaluatedValue);
        return  evaluatedValue;
    }


    @Override
    public void taskComplete(int response) {
        Log.i("SYNC TASK COMPLETE", "STATUS = " + response);
        try {
            SyncUpActivity.removeOnNetworkActivityComplete();
} catch (NullPointerException e) {
            e.printStackTrace();
        }
        if (response == Constants.SYNC_STATUS_COMPLETE) {
            farmer.setLastVisitDate(DateUtil.getFormattedDateMMDDYYYYhhmmaa());
            farmer.setHasSubmitted(Constants.YES);
            if (databaseHelper.editFarmerBasicInfo(farmer))
                submit.setVisibility(View.GONE);
        }
    }
}