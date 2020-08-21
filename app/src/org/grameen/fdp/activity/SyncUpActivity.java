package org.grameen.fdp.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.salesforce.androidsdk.rest.ApiVersionStrings;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.ui.SalesforceActivity;

import org.grameen.fdp.R;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.object.RealFarmer;
import org.grameen.fdp.object.RealPlot;
import org.grameen.fdp.syncObjects.Answer;
import org.grameen.fdp.syncObjects.Farmer;
import org.grameen.fdp.syncObjects.Monitoring;
import org.grameen.fdp.syncObjects.MonitoringAnswer;
import org.grameen.fdp.syncObjects.Plot;
import org.grameen.fdp.syncObjects.Submission;
import org.grameen.fdp.utility.Callbacks;
import org.grameen.fdp.utility.Constants;
import org.grameen.fdp.utility.CustomToast;
import org.grameen.fdp.utility.DatabaseHelper;
import org.grameen.fdp.utility.DateUtil;
import org.grameen.fdp.utility.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static java.sql.DriverManager.println;

/**
 * Created by aangjnr on 11/12/2017.
 */

public class SyncUpActivity extends SalesforceActivity {

    int TOTAL_SIZE = 0;
    boolean SUBMIT_AGREEMENT = false;
    private List<RealFarmer> UN_SYNCED_FARMERS;

    AlertDialog.Builder builder;
    boolean BREAK_OUT_OF_LOOP = false;
    String nextRecordsUrl = "";
    RestRequest queryMore;
    Boolean done = null;
    //RestRequest restRequest;
    ObjectMapper oMapper;

    final int MAX_RETRY = 3;
    int RETRY_COUNT = 0;

    int NEXT_BATCH = 0;
    int COUNT = 0;
    int BEGIN_INDEX = 0;
    final int BATCH = 100;
    int SYNC_STATUS = Constants.SYNC_STATUS_NO_SYNC;
    String message = "";
    String title = "";
    String SUBMISSION_ID;
    String USER_ID;
    RestClient restClient = null;
    ProgressDialog progressDialog;
    String TAG = "DataDownloadActivity";
    DatabaseHelper databaseHelper;
    long start = 0;
    String ISO_CODE = "";
    Context context;
    Boolean syncInitialData;
    String API_VERSION;
    Boolean exception = false;
    Question farmResultsQuestion;
    List<Object> ALL_FARMER_LIST = new ArrayList<>();
    List<Object> ALL_FARMERS_PLOT_LIST = new ArrayList<>();
    List<Object> ALL_FARMER_ANSWERS = new ArrayList<>();
    List<Object> ALL_PLOT_ANSWERS = new ArrayList<>();
    List<Object> ALL_MONITORING_LIST = new ArrayList<>();
    List<Object> ALL_MONITORING_ANSWERS_LIST = new ArrayList<>();

    private RealFarmer FARMER;
    SharedPreferences preferences;

    public static Callbacks.NetworkActivityCompleteListener networkActivityCompleteListener;

    public static void onNetworkActivityComplete(Callbacks.NetworkActivityCompleteListener listener) {
        networkActivityCompleteListener = listener;
    }

    public static void removeOnNetworkActivityComplete() {
        networkActivityCompleteListener = null;
    }






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        databaseHelper = DatabaseHelper.getInstance(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        farmResultsQuestion = databaseHelper.getQuestionByTranslation("Farm result");

        Log.i(TAG, "Farm result question " + new Gson().toJson(farmResultsQuestion));

        syncInitialData = getIntent().getBooleanExtra("initialData", false);
        SUBMIT_AGREEMENT = getIntent().getBooleanExtra("submitAgreement", false);

        context = this;

        ISO_CODE = PreferenceManager.getDefaultSharedPreferences(this).getString("ISO", "");

        oMapper = new ObjectMapper();

        API_VERSION = ApiVersionStrings.getVersionNumber(this);


    }

    @Override
    public void onResume(RestClient client) {


        restClient = client;

        FARMER = new Gson().fromJson(getIntent().getStringExtra("farmer"), RealFarmer.class);


        if (FARMER == null)
            UN_SYNCED_FARMERS = databaseHelper.getAllUnsyncedFarmers();
        else {
            UN_SYNCED_FARMERS = new ArrayList<>();
            UN_SYNCED_FARMERS.add(FARMER);


        }


        try {
            RestClient.ClientInfo ci = restClient.getClientInfo();
            USER_ID = ci.userId;

            System.out.println("*****************************************************************");
            System.out.println();
            Log.i(TAG, "USER ID IS " + USER_ID);
            System.out.println();
            System.out.println("*****************************************************************");

            PreferenceManager.getDefaultSharedPreferences(this).edit().putString(Constants.USER_UID, USER_ID).apply();


        } catch (Exception e) {
            e.printStackTrace();
        }


        if (Utils.checkInternetConnection(context)) {

            if (UN_SYNCED_FARMERS != null && UN_SYNCED_FARMERS.size() > 0) {

                progressDialog = BaseActivity.showProgress(context, getResources(R.string.sending_data), getResources(R.string.please_wait), false);


                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {


                        sendFarmerDataToServer();


                    }
                });


                thread.start();
            } else {

                AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppDialog);
                builder.setTitle(getResources(R.string.no_new_data));
                builder.setCancelable(true);
                builder.setMessage(getResources(R.string.no_farmers));
                builder.setPositiveButton(getResources(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        finish();

                    }
                });
                builder.show();


            }
        } else {

            AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppDialog);
            builder.setTitle(getResources(R.string.network_error));
            builder.setCancelable(true);
            builder.setMessage(getResources(R.string.network_error_rational));
            builder.setPositiveButton(getResources(R.string.retry), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    startActivity(new Intent(SyncUpActivity.this, SyncUpActivity.class));
                    finish();

                }
            });

            builder.setNegativeButton(getResources(R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    finish();

                }
            });

            builder.show();


        }

    }

    private void prepareData() {

        Date date = DateUtil.formatDateMMDDYYYY();
        String placeHolderQuestionId = databaseHelper.getQuestionIdByTranslationName("Place holder question");


        for (RealFarmer realFarmer : UN_SYNCED_FARMERS) {

            Farmer farmer = new Farmer();
            farmer.setSubmission__c(SUBMISSION_ID);
            farmer.setExternal_id__c(realFarmer.getId());
            farmer.setName(realFarmer.getCode());
            farmer.setFullName__c(realFarmer.getFarmerName());
            farmer.setGender__c(realFarmer.getGender());
            farmer.setEducationalLevel__c(realFarmer.getEducationLevel());
            farmer.setGender__c(realFarmer.getGender());
            farmer.setBirthday__c(realFarmer.getBirthYear());
            farmer.setLastModifiedDate__c(realFarmer.getLastModifiedDate());
            farmer.setLastVisitDate__c(realFarmer.getLastVisitDate());
            farmer.setVillage__c(databaseHelper.getVillageId(toCamelCase(realFarmer.getVillage())));
            farmer.setSubmitAgreement__c(realFarmer.getHasSubmitted());

            ALL_FARMER_LIST.add(farmer);

            JSONObject farmerAnswersJsonObject;

            try {
                farmerAnswersJsonObject = new JSONObject(realFarmer.getAnswersJson());
            } catch (JSONException e) {
                e.printStackTrace();
                farmerAnswersJsonObject = new JSONObject();
            }


            Iterator i1 = farmerAnswersJsonObject.keys();


            while (i1.hasNext()) {


                String tmp_key = (String) i1.next();

                Answer farmerAnswer = new Answer();
                try {
                    farmerAnswer.setAnswer__c(farmerAnswersJsonObject.getString(tmp_key));
                } catch (JSONException e) {
                    e.printStackTrace();
                    farmerAnswer.setAnswer__c("");
                }
                farmerAnswer.setSubmission__c(SUBMISSION_ID);
                farmerAnswer.setDate__c(date);
                farmerAnswer.setExternal_id__c(realFarmer.getId());
                farmerAnswer.setFarmer_ID__c(realFarmer.getCode());
                farmerAnswer.setQuestion__c(tmp_key);

                ALL_FARMER_ANSWERS.add(farmerAnswer);

            }


            List<RealPlot> realPlots = databaseHelper.getAllFarmerPlots(realFarmer.getId());

            if (realPlots != null && realPlots.size() > 0)
                for (RealPlot realPlot : realPlots) {

                    Plot plot = new Plot();
                    plot.setRecommendation_id__c(realPlot.getRecommendationId());
                    plot.setFarmer_code__c(realFarmer.getId());
                    plot.setPlot_name__c(realPlot.getName());
                    plot.setPlot_area_coordinates__c(realPlot.getGpsPoints());
                    plot.setExternal_id__c(realPlot.getId());
                    plot.setStart_year__c(realPlot.getStartYear());
                    plot.setSubmission__c(SUBMISSION_ID);


                    ALL_FARMERS_PLOT_LIST.add(plot);


                    JSONObject plotAnswersJsonObject;

                    try {
                        plotAnswersJsonObject = new JSONObject(realPlot.getPlotInformationJson());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        plotAnswersJsonObject = new JSONObject();
                    }


                    Iterator j1 = plotAnswersJsonObject.keys();


                    while (j1.hasNext()) {


                        String tmp_key = (String) j1.next();

                        Answer plotAnswer = new Answer();
                        try {
                            plotAnswer.setAnswer__c(plotAnswersJsonObject.getString(tmp_key));
                        } catch (JSONException e) {
                            e.printStackTrace();
                            plotAnswer.setAnswer__c("");
                        }

                        plotAnswer.setDate__c(date);
                        plotAnswer.setExternal_id__c(realPlot.getId());
                        plotAnswer.setFarmer_ID__c(realFarmer.getCode());
                        plotAnswer.setQuestion__c(tmp_key);
                        plotAnswer.setSubmission__c(SUBMISSION_ID);

                        ALL_PLOT_ANSWERS.add(plotAnswer);


                    }


                    List<org.grameen.fdp.object.Monitoring> realMonitoringList = databaseHelper.getAllPlotMonitoring(realPlot.getId());

                    if (realMonitoringList != null && realMonitoringList.size() > 0)

                        for (org.grameen.fdp.object.Monitoring realMonitoring : realMonitoringList) {


                            Monitoring monitoring = new Monitoring();
                            monitoring.setSubmission__c(SUBMISSION_ID);
                            monitoring.setExternal_id__c(realMonitoring.getId());
                            monitoring.setMonitoringName__c(realMonitoring.getName());
                            monitoring.setPlot_External_id__c(realMonitoring.getPlotId());
                            monitoring.setResult__c("");
                            monitoring.setYear__c(realMonitoring.getYear());

                            ALL_MONITORING_LIST.add(monitoring);

                            JSONObject monirotingAnswerJsonObject;

                            try {
                                monirotingAnswerJsonObject = new JSONObject(realMonitoring.getJson());
                            } catch (JSONException e) {
                                e.printStackTrace();
                                monirotingAnswerJsonObject = new JSONObject();
                            }


                            Iterator k1 = monirotingAnswerJsonObject.keys();


                            while (k1.hasNext()) {

                                String tmp_key = (String) k1.next();

                                MonitoringAnswer monitoringAnswer = new MonitoringAnswer();
                                monitoringAnswer.setSubmission__c(SUBMISSION_ID);
                                try {
                                    monitoringAnswer.setAnswer__c(monirotingAnswerJsonObject.getString(tmp_key));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    monitoringAnswer.setAnswer__c("");
                                }
                                monitoringAnswer.setExternal_id__c(realMonitoring.getId());
                                monitoringAnswer.setQuestion__c(tmp_key);

                                String competenceId;
                                String reasonForFailureId;


                                Question q = databaseHelper.getQuestion(tmp_key);
                                if (q != null) {

                                    if (q.getRelated_Questions__c() != null) {


                                        String values[] = q.getRelated_Questions__c().split(",");
                                        String competenceName = values[0];
                                        String reasonForFailureName = values[1];

                                        competenceId = databaseHelper.getQuestionByName(competenceName).getId();
                                        reasonForFailureId = databaseHelper.getQuestionByName(reasonForFailureName).getId();

                                        monitoringAnswer.setCompetence__c(competenceId);

                                        try {
                                            monitoringAnswer.setCompetence_Answer__c(monirotingAnswerJsonObject.getString(competenceId));
                                        } catch (JSONException ignore) {
                                            monitoringAnswer.setCompetence_Answer__c("");
                                        }

                                        monitoringAnswer.setReasons_For_Failure__c(reasonForFailureId);

                                        try {
                                            monitoringAnswer.setReasons_for_failure_answer__c(monirotingAnswerJsonObject.getString(reasonForFailureId));
                                        } catch (JSONException ignore) {
                                            monitoringAnswer.setReasons_for_failure_answer__c("");
                                        }


                                    } else {

                                        monitoringAnswer.setCompetence__c(placeHolderQuestionId);
                                        monitoringAnswer.setCompetence_Answer__c("");

                                        monitoringAnswer.setReasons_For_Failure__c(placeHolderQuestionId);
                                        monitoringAnswer.setReasons_for_failure_answer__c("");
                                    }
                                }

                                ALL_MONITORING_ANSWERS_LIST.add(monitoringAnswer);
                            }

                        }

                }


        }

    }


    private void prepareDataForSIngleFarmer(RealFarmer realFarmer) {
        Date date = DateUtil.formatDateMMDDYYYY();
        String placeHolderQuestionId = databaseHelper.getQuestionIdByTranslationName("Place holder question");

        Farmer farmer = new Farmer();
        farmer.setSubmission__c(SUBMISSION_ID);
        farmer.setExternal_id__c(realFarmer.getId());
        farmer.setName(realFarmer.getCode());
        farmer.setFullName__c(realFarmer.getFarmerName());
        farmer.setGender__c(realFarmer.getGender());
        farmer.setEducationalLevel__c(realFarmer.getEducationLevel());
        farmer.setGender__c(realFarmer.getGender());
        farmer.setBirthday__c(realFarmer.getBirthYear());
        farmer.setPhotoUrl__c(realFarmer.getImageUrl());
        farmer.setFirstVisitDate__c(realFarmer.getFirstVisitDate());
        farmer.setLastModifiedDate__c(realFarmer.getLastModifiedDate());
        farmer.setLastVisitDate__c(realFarmer.getLastVisitDate());
        farmer.setVillage__c(databaseHelper.getVillageId(toCamelCase(realFarmer.getVillage())));
        farmer.setSubmitAgreement__c(realFarmer.getHasSubmitted());

        ALL_FARMER_LIST.add(farmer);
        JSONObject farmerAnswersJsonObject;
        try {
            farmerAnswersJsonObject = new JSONObject(realFarmer.getAnswersJson());
            //Add farm results answer
            if(farmerAnswersJsonObject.has(farmResultsQuestion.getId()))
            farmerAnswersJsonObject.remove(farmResultsQuestion.getId());
            farmerAnswersJsonObject.put(farmResultsQuestion.getId(), preferences.getString(realFarmer.getCode() + "_farm_results", "N/A"));
        } catch (JSONException e) {
            e.printStackTrace();
            farmerAnswersJsonObject = new JSONObject();
        }

        Iterator i1 = farmerAnswersJsonObject.keys();
        while (i1.hasNext()) {
            String tmp_key = (String) i1.next();
            Answer farmerAnswer = new Answer();
            try {
                farmerAnswer.setAnswer__c(farmerAnswersJsonObject.getString(tmp_key));
            } catch (JSONException e) {
                e.printStackTrace();
                farmerAnswer.setAnswer__c("");
            }
            farmerAnswer.setSubmission__c(SUBMISSION_ID);
            farmerAnswer.setDate__c(date);
            farmerAnswer.setExternal_id__c(realFarmer.getId());
            farmerAnswer.setFarmer_ID__c(realFarmer.getCode());
            farmerAnswer.setQuestion__c(tmp_key);
            ALL_FARMER_ANSWERS.add(farmerAnswer);
        }

        List<RealPlot> realPlots = databaseHelper.getAllFarmerPlots(realFarmer.getId());

        if (realPlots != null && realPlots.size() > 0)
            for (RealPlot realPlot : realPlots) {

                Plot plot = new Plot();
                plot.setRecommendation_id__c(realPlot.getRecommendationId());
                plot.setFarmer_code__c(realFarmer.getId());
                plot.setPlot_name__c(realPlot.getName());
                plot.setPlot_area_coordinates__c(realPlot.getGpsPoints());
                plot.setExternal_id__c(realPlot.getId());
                plot.setStart_year__c(realPlot.getStartYear());
                plot.setSubmission__c(SUBMISSION_ID);


                ALL_FARMERS_PLOT_LIST.add(plot);


                JSONObject plotAnswersJsonObject;

                try {
                    plotAnswersJsonObject = new JSONObject(realPlot.getPlotInformationJson());
                } catch (JSONException e) {
                    e.printStackTrace();
                    plotAnswersJsonObject = new JSONObject();
                }


                Iterator j1 = plotAnswersJsonObject.keys();


                while (j1.hasNext()) {


                    String tmp_key = (String) j1.next();

                    Answer plotAnswer = new Answer();
                    try {
                        plotAnswer.setAnswer__c(plotAnswersJsonObject.getString(tmp_key));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        plotAnswer.setAnswer__c("");
                    }

                    plotAnswer.setDate__c(date);
                    plotAnswer.setExternal_id__c(realPlot.getId());
                    plotAnswer.setFarmer_ID__c(realFarmer.getCode());
                    plotAnswer.setQuestion__c(tmp_key);
                    plotAnswer.setSubmission__c(SUBMISSION_ID);

                    ALL_PLOT_ANSWERS.add(plotAnswer);


                }


                List<org.grameen.fdp.object.Monitoring> realMonitoringList = databaseHelper.getAllPlotMonitoring(realPlot.getId());

                if (realMonitoringList != null && realMonitoringList.size() > 0)

                    for (org.grameen.fdp.object.Monitoring realMonitoring : realMonitoringList) {


                        Monitoring monitoring = new Monitoring();
                        monitoring.setSubmission__c(SUBMISSION_ID);
                        monitoring.setExternal_id__c(realMonitoring.getId());
                        monitoring.setMonitoringName__c(realMonitoring.getName());
                        monitoring.setPlot_External_id__c(realMonitoring.getPlotId());
                        monitoring.setResult__c("");
                        monitoring.setYear__c(realMonitoring.getYear());

                        ALL_MONITORING_LIST.add(monitoring);

                        JSONObject monirotingAnswerJsonObject;

                        try {
                            monirotingAnswerJsonObject = new JSONObject(realMonitoring.getJson());
                        } catch (JSONException e) {
                            e.printStackTrace();
                            monirotingAnswerJsonObject = new JSONObject();
                        }


                        Iterator k1 = monirotingAnswerJsonObject.keys();


                        while (k1.hasNext()) {

                            String tmp_key = (String) k1.next();

                            MonitoringAnswer monitoringAnswer = new MonitoringAnswer();
                            monitoringAnswer.setSubmission__c(SUBMISSION_ID);
                            try {
                                monitoringAnswer.setAnswer__c(monirotingAnswerJsonObject.getString(tmp_key));
                            } catch (JSONException e) {
                                e.printStackTrace();
                                monitoringAnswer.setAnswer__c("");
                            }
                            monitoringAnswer.setExternal_id__c(realMonitoring.getId());
                            monitoringAnswer.setQuestion__c(tmp_key);

                            String competenceId;
                            String reasonForFailureId;


                            Question q = databaseHelper.getQuestion(tmp_key);
                            if (q != null) {

                                if (q.getRelated_Questions__c() != null) {


                                    String values[] = q.getRelated_Questions__c().split(",");
                                    String competenceName = values[0];
                                    String reasonForFailureName = values[1];

                                    competenceId = databaseHelper.getQuestionByName(competenceName).getId();
                                    reasonForFailureId = databaseHelper.getQuestionByName(reasonForFailureName).getId();

                                    monitoringAnswer.setCompetence__c(competenceId);

                                    try {
                                        monitoringAnswer.setCompetence_Answer__c(monirotingAnswerJsonObject.getString(competenceId));
                                    } catch (JSONException ignore) {
                                        monitoringAnswer.setCompetence_Answer__c("");
                                    }

                                    monitoringAnswer.setReasons_For_Failure__c(reasonForFailureId);

                                    try {
                                        monitoringAnswer.setReasons_for_failure_answer__c(monirotingAnswerJsonObject.getString(reasonForFailureId));
                                    } catch (JSONException ignore) {
                                        monitoringAnswer.setReasons_for_failure_answer__c("");
                                    }


                                } else {

                                    monitoringAnswer.setCompetence__c(placeHolderQuestionId);
                                    monitoringAnswer.setCompetence_Answer__c("");

                                    monitoringAnswer.setReasons_For_Failure__c(placeHolderQuestionId);
                                    monitoringAnswer.setReasons_for_failure_answer__c("");
                                }
                            }

                            ALL_MONITORING_ANSWERS_LIST.add(monitoringAnswer);
                        }

                    }

            }


        System.out.println("******************************");
        System.out.println();
        System.out.println();

        Log.i(TAG, "ALL FARMERS LIST SIZE IS " + ALL_FARMER_LIST.size());
        Log.i(TAG, "ALL FARMERS ANSWERS LIST SIZE IS " + ALL_FARMER_ANSWERS.size());

        Log.i(TAG, "ALL PLOTS LIST SIZE IS " + ALL_FARMERS_PLOT_LIST.size());
        Log.i(TAG, "ALL PLOT ANSWERS LIST SIZE IS " + ALL_PLOT_ANSWERS.size());

        Log.i(TAG, "ALL MONITORING LIST SIZE IS " + ALL_MONITORING_LIST.size());
        Log.i(TAG, "ALL MONITORING ANSWER LIST SIZE IS " + ALL_MONITORING_ANSWERS_LIST.size());


        System.out.println();
        System.out.println();
        System.out.println("******************************");


        sendDataOneAtATime(realFarmer);
    }




    private void sendRequest(RestRequest request, RestClient.AsyncRequestCallback asyncRequestCallback) {
        println("####  SEND REQUEST  ####");
        BaseActivity.printHeader(request);
        try {
            sendFromUIThread(request, asyncRequestCallback);
        } catch (Exception e) {
            BaseActivity.printException(e);
            CustomToast.makeToast(getApplicationContext(), getResources(R.string.connection_error), Toast.LENGTH_LONG).show();

        }
    }

    private void sendFromUIThread(RestRequest restRequest, RestClient.AsyncRequestCallback asyncRequestCallback) throws UnsupportedEncodingException {
        System.out.println("\n\n");
        //BaseActivity.printHeader(restRequest);

        start = System.nanoTime();


        restClient.sendAsync(restRequest, asyncRequestCallback);

    }


    public String getResources(int resource) {
        return getString(resource);

    }


    @SuppressWarnings("unchecked")
    HashMap<String, Object> parseObject(Object o) {

        return oMapper.convertValue(o, HashMap.class);

    }


    void sendFarmerDataToServer() {

        int count = 0;
        int totalSize = UN_SYNCED_FARMERS.size();


        for (RealFarmer realFarmer : UN_SYNCED_FARMERS) {

            if (BREAK_OUT_OF_LOOP)
                break;

            count++;

            message = "Uploading " + count + " out of " + totalSize;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    progressDialog.setTitle(message);
                }
            });


            Submission submission = new Submission();
            submission.setStart__c(DateUtil.stringToDateMMDDYYYYhhmma(realFarmer.getFirstVisitDate()));
            submission.setEnd__c(DateUtil.stringToDateMMDDYYYYhhmma(realFarmer.getLastModifiedDate()));
            submission.setExternal_id__c(String.valueOf(System.currentTimeMillis()));
            submission.setCountry_iso__c(ISO_CODE);
            submission.setSurveyor__c(USER_ID);


            RestRequest createSubmissionRequest = null;
            try {
                createSubmissionRequest = RestRequest.getRequestForCreate(API_VERSION, "fpd_Submission__c", parseObject(submission));
            } catch (IOException e) {
                e.printStackTrace();
            }


            RestResponse response;
            JSONObject responseJson;
            try {
                response = restClient.sendSync(createSubmissionRequest);
                Log.i(TAG, "RESPONSE CODE = " + response.getStatusCode());
                try {

                    responseJson = response.asJSONObject();
                    Log.i(TAG, "RESPONSE BODY = " + responseJson);
                } catch (JSONException e) {
                    e.printStackTrace();
                    responseJson = new JSONObject();
                }

                if (response.isSuccess()) {

                    try {
                        SUBMISSION_ID = responseJson.getString("id");

                        prepareDataForSIngleFarmer(realFarmer);


                    } catch (JSONException e) {
                        e.printStackTrace();
                        SUBMISSION_ID = null;

                        showErrorDialog();

                    }

                } else {

                    showErrorDialog();

                }


            } catch (IOException e) {
                e.printStackTrace();
                Log.i(TAG, "RESPONSE BODY is NULL");

                progressDialog.dismiss();
                finish();
                CustomToast.makeToast(context, "Please try again", Toast.LENGTH_LONG).show();

            }

        }

        if (!BREAK_OUT_OF_LOOP) {

            showSyncCompleteDialog(context);

        } else {
            showErrorDialog();
        }

    }


    void sendDataOneAtATime(RealFarmer realFarmer) {

        if (!BREAK_OUT_OF_LOOP) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage("Sending farmers data...");
                }
            });

            COUNT = ALL_FARMER_LIST.size();
            BEGIN_INDEX = 0;
            TOTAL_SIZE = COUNT;
            BEGIN_INDEX = 0;

            NEXT_BATCH = 0;
            if (COUNT > 0) {

                while (COUNT > BATCH) {
                    NEXT_BATCH += BATCH;

                    sendDataForCreate("fdp_farmer_answer__c", ALL_FARMER_LIST.subList(BEGIN_INDEX, NEXT_BATCH));
                }

                do {
                    sendDataForCreate("fdp_farmer_answer__c", ALL_FARMER_LIST.subList(BEGIN_INDEX, TOTAL_SIZE));
                } while (RETRY_COUNT > 0);

            }

        }


        //Send farmer answers data first in 200 objects per request

        if (!BREAK_OUT_OF_LOOP) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage("Sending farmers answers data...");
                }
            });
            COUNT = ALL_FARMER_ANSWERS.size();

            TOTAL_SIZE = COUNT;
            BEGIN_INDEX = 0;
            NEXT_BATCH = 0;
            if (COUNT > 0) {

                while (COUNT > BATCH) {
                    NEXT_BATCH += BATCH;
                    sendDataForCreate("fpd_Answer__c", ALL_FARMER_ANSWERS.subList(BEGIN_INDEX, NEXT_BATCH));
                }
                do {
                    sendDataForCreate("fpd_Answer__c", ALL_FARMER_ANSWERS.subList(BEGIN_INDEX, TOTAL_SIZE));
                } while (RETRY_COUNT > 0);

            }
        }


        //Send farmer plots data first in 200 objects per request

        if (!BREAK_OUT_OF_LOOP) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage("Sending plots data...");
                }
            });

            COUNT = ALL_FARMERS_PLOT_LIST.size();
            TOTAL_SIZE = COUNT;
            BEGIN_INDEX = 0;
            NEXT_BATCH = 0;
            if (COUNT > 0) {

                while (COUNT > BATCH) {
                    NEXT_BATCH += BATCH;

                    sendDataForCreate("fpd_Plot_answer__c", ALL_FARMERS_PLOT_LIST.subList(BEGIN_INDEX, NEXT_BATCH));
                }
                do {
                    sendDataForCreate("fpd_Plot_answer__c", ALL_FARMERS_PLOT_LIST.subList(BEGIN_INDEX, TOTAL_SIZE));
                } while (RETRY_COUNT > 0);

            }
        }


        //Send plot answers data first in 200 objects per request


        if (!BREAK_OUT_OF_LOOP) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage("Sending plot answers data...");
                }
            });

            COUNT = ALL_PLOT_ANSWERS.size();
            TOTAL_SIZE = COUNT;
            BEGIN_INDEX = 0;

            NEXT_BATCH = 0;
            if (COUNT > 0) {

                while (COUNT > BATCH) {
                    NEXT_BATCH += BATCH;


                    sendDataForCreate("fpd_Answer__c", ALL_PLOT_ANSWERS.subList(BEGIN_INDEX, NEXT_BATCH));
                }
                do {
                    sendDataForCreate("fpd_Answer__c", ALL_PLOT_ANSWERS.subList(BEGIN_INDEX, TOTAL_SIZE));
                } while (RETRY_COUNT > 0);

            }
        }

        //Send monitoring data first in 200 objects per request

        if (!BREAK_OUT_OF_LOOP) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage("Sending monitoring information data...");
                }
            });
            COUNT = ALL_MONITORING_LIST.size();
            TOTAL_SIZE = COUNT;
            BEGIN_INDEX = 0;

            NEXT_BATCH = 0;
            if (COUNT > 0) {


                while (COUNT > BATCH) {
                    NEXT_BATCH += BATCH;

                    sendDataForCreate("fdp_Monitoring__c", ALL_MONITORING_LIST.subList(BEGIN_INDEX, NEXT_BATCH));
                }

                do {
                    sendDataForCreate("fdp_Monitoring__c", ALL_MONITORING_LIST.subList(BEGIN_INDEX, TOTAL_SIZE));
                }
                while (RETRY_COUNT > 0);


            }

        }


        //Send monitoring answers data first in 200 objects per request


        if (!BREAK_OUT_OF_LOOP) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage("Sending last bit of data...");
                }
            });

            COUNT = ALL_MONITORING_ANSWERS_LIST.size();

            TOTAL_SIZE = COUNT;
            BEGIN_INDEX = 0;

            NEXT_BATCH = 0;
            if (COUNT > 0) {


                while (COUNT > BATCH) {
                    NEXT_BATCH += BATCH;
                    sendDataForCreate("fdp_Monitoring_Answer__c", ALL_MONITORING_ANSWERS_LIST.subList(BEGIN_INDEX, NEXT_BATCH));
                }
                do {
                    sendDataForCreate("fdp_Monitoring_Answer__c", ALL_MONITORING_ANSWERS_LIST.subList(BEGIN_INDEX, TOTAL_SIZE));
                } while (RETRY_COUNT > 0);

            }

        }


        if (!BREAK_OUT_OF_LOOP)
            databaseHelper.setFarmerAsSynced(realFarmer.getId());

        ALL_FARMER_LIST.clear();
        ALL_FARMERS_PLOT_LIST.clear();
        ALL_FARMER_ANSWERS.clear();
        ALL_PLOT_ANSWERS.clear();
        ALL_MONITORING_LIST.clear();
        ALL_MONITORING_ANSWERS_LIST.clear();


    }


    void sendDataForCreate(String objectType, List<Object> objectList) {


        int count = 0;
        int counter;


        List<RestRequest.SObjectTree> objectTrees = new ArrayList<>();

        for (Object o : objectList) {

            counter = count++;
            String refId = "ref" + counter;

            RestRequest.SObjectTree sObjectTree = new RestRequest.SObjectTree(objectType, null, refId, parseObject(o), null);
            objectTrees.add(sObjectTree);

        }


        RestRequest restRequest = null;
        try {
            restRequest = RestRequest.getRequestForSObjectTree(API_VERSION, objectType, objectTrees);
            Log.i(TAG, "REQUEST PATH = " + restRequest.getPath());

        } catch (JSONException e) {
            e.printStackTrace();

            Log.i(TAG, "REQUEST = " + null);

        }


        RestResponse response = null;
        try {
            response = restClient.sendSync(restRequest);
            Log.i(TAG, "RESPONSE CODE = " + response.getStatusCode());
            try {
                Log.i(TAG, "RESPONSE CODE = " + response.asJSONObject());
            } catch (JSONException e) {
                e.printStackTrace();

            }

            if (response.isSuccess()) {
                BEGIN_INDEX += BATCH;

                RETRY_COUNT = 0;

                COUNT -= BATCH;

            } else {

                RETRY_COUNT++;

                System.out.println("***********  " + objectType + "*************");
                System.out.println();
                Log.i(TAG, "ERROR! RETRY " + RETRY_COUNT);
                System.out.println();
                System.out.println("************************");


                if (RETRY_COUNT == MAX_RETRY) {

                    RETRY_COUNT = 0;
                    BREAK_OUT_OF_LOOP = true;

                }

            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "RESPONSE BODY is NULL");

            RETRY_COUNT++;
            if (RETRY_COUNT == MAX_RETRY) {

                BREAK_OUT_OF_LOOP = true;

            }


        }


    }


    void sendDataInBatches() {

        //Send farmer data first in 200 objects per request

        if (!BREAK_OUT_OF_LOOP) {

           /* runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage("Sending farmers data...");
                }
            });*/

            COUNT = ALL_FARMER_LIST.size();
            BEGIN_INDEX = 0;

            if (COUNT > 0) {

                while (COUNT > BATCH) {
                    sendDataForCreate("fdp_farmer_answer__c", ALL_FARMER_LIST.subList(BEGIN_INDEX, BATCH));
                }

                do {
                    sendDataForCreate("fdp_farmer_answer__c", ALL_FARMER_LIST.subList(BEGIN_INDEX, COUNT));
                } while (RETRY_COUNT > 0);

            }

        }

        //Send farmer answers data first in 200 objects per request

        if (!BREAK_OUT_OF_LOOP) {
           /* runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage("Sending farmers answers data...");
                }
            });*/
            COUNT = ALL_FARMER_ANSWERS.size();

            BEGIN_INDEX = 0;

            if (COUNT > 0) {

                while (COUNT > BATCH) {
                    sendDataForCreate("fpd_Answer__c", ALL_FARMER_ANSWERS.subList(BEGIN_INDEX, BATCH));
                }
                do {
                    sendDataForCreate("fpd_Answer__c", ALL_FARMER_ANSWERS.subList(BEGIN_INDEX, COUNT));
                } while (RETRY_COUNT > 0);

            }
        }


        //Send farmer plots data first in 200 objects per request

        if (!BREAK_OUT_OF_LOOP) {
           /* runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage("Sending plots data...");
                }
            });*/

            COUNT = ALL_FARMERS_PLOT_LIST.size();
            BEGIN_INDEX = 0;

            if (COUNT > 0) {

                while (COUNT > BATCH) {
                    sendDataForCreate("fpd_Plot_answer__c", ALL_FARMERS_PLOT_LIST.subList(BEGIN_INDEX, BATCH));
                }
                do {
                    sendDataForCreate("fpd_Plot_answer__c", ALL_FARMERS_PLOT_LIST.subList(BEGIN_INDEX, COUNT));
                } while (RETRY_COUNT > 0);

            }
        }


        //Send plot answers data first in 200 objects per request


        if (!BREAK_OUT_OF_LOOP) {
           /* runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage("Sending plot answers data...");
                }
            });
*/
            COUNT = ALL_PLOT_ANSWERS.size();
            BEGIN_INDEX = 0;

            if (COUNT > 0) {

                while (COUNT > BATCH) {
                    sendDataForCreate("fpd_Answer__c", ALL_PLOT_ANSWERS.subList(BEGIN_INDEX, BATCH));
                }
                do {
                    sendDataForCreate("fpd_Answer__c", ALL_PLOT_ANSWERS.subList(BEGIN_INDEX, COUNT));
                } while (RETRY_COUNT > 0);

            }
        }

        //Send monitoring data first in 200 objects per request

        if (!BREAK_OUT_OF_LOOP) {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    progressDialog.setMessage("Sending monitoring information data...");
//                }
//            });
            COUNT = ALL_MONITORING_LIST.size();
            BEGIN_INDEX = 0;

            if (COUNT > 0) {

                while (COUNT > BATCH) {
                    sendDataForCreate("fdp_Monitoring__c", ALL_MONITORING_LIST.subList(BEGIN_INDEX, BATCH));
                }

                do {
                    sendDataForCreate("fdp_Monitoring__c", ALL_MONITORING_LIST.subList(BEGIN_INDEX, COUNT));
                } while (RETRY_COUNT > 0);

            }

        }


        //Send monitoring answers data first in 200 objects per request


        if (!BREAK_OUT_OF_LOOP) {
          /*  runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage("Sending last bit of data...");
                }
            });*/
            COUNT = ALL_MONITORING_ANSWERS_LIST.size();
            BEGIN_INDEX = 0;


            if (COUNT > 0) {

                while (COUNT > BATCH) {
                    sendDataForCreate("fdp_Monitoring_Answer__c", ALL_MONITORING_ANSWERS_LIST.subList(BEGIN_INDEX, BATCH));
                }
                do {
                    sendDataForCreate("fdp_Monitoring_Answer__c", ALL_MONITORING_ANSWERS_LIST.subList(BEGIN_INDEX, COUNT));
                } while (RETRY_COUNT > 0);

            }

        }



/*
        if(!BREAK_OUT_OF_LOOP) {


            showSyncCompleteDialog(context);

        }else {
            showErrorDialog();
        }*/


    }


    public String toCamelCase(String value) {

        if (value == null || value.equals("null")) return "";
        else {

            if (Character.isUpperCase(value.codePointAt(0)))
                return value;

            else
                return (value.substring(0, 1).toUpperCase() + value.substring(1, value.length()).toLowerCase());

        }
    }


    void showErrorDialog() {


        RETRY_COUNT = 0;

        if (builder == null)

            runOnUiThread(new Runnable() {
                @Override
                public void run() {


                    if (progressDialog != null)
                        progressDialog.dismiss();


                    builder = new AlertDialog.Builder(context, R.style.AppDialog);
                    builder.setTitle("Error reaching server");
                    builder.setCancelable(true);
                    builder.setMessage("An error has occurred. Retry?");
                    builder.setPositiveButton(getResources(R.string.retry), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            startActivity(new Intent(SyncUpActivity.this, SyncUpActivity.class));
                            finish();

                        }
                    });

                    builder.setNegativeButton(getResources(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            finish();

                        }
                    });

                    builder.show();


                }
            });


    }



    void showSyncCompleteDialog(final Context context) {
        COUNT = 0;
        RETRY_COUNT = 0;
        BEGIN_INDEX = 0;



        message = getResources(R.string.all_data_synced);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("lastSync", DateUtil.getFormattedDateMMDDYYYYhhmmaa()).apply();


        if (SUBMIT_AGREEMENT) {
            message = getResources(R.string.agreement_submitted_and_data_synced);
            databaseHelper.setAgreementSubmitted(FARMER.getId());

            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("refreshMainActivity", true).apply();
        }


        runOnUiThread(new Runnable() {


            @Override
            public void run() {


                if (progressDialog != null)
                    progressDialog.dismiss();


                builder = new AlertDialog.Builder(context, R.style.AppDialog);
                builder.setTitle(title);
                builder.setCancelable(true);
                builder.setMessage(message);
                builder.setPositiveButton(getResources(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("lastSync", DateUtil.getFormattedDateMMDDYYYYhhmmaa()).apply();

                        if (networkActivityCompleteListener != null)
                            networkActivityCompleteListener.taskComplete(SYNC_STATUS);

                        finish();

                    }
                });

                   /* if (FARMER == null)
                        if (SYNC_STATUS == Constants.SYNC_STATUS_NO_SYNC || SYNC_STATUS == Constants.SYNC_STATUS_PARTIAL_SYNC)
                            builder.setNeutralButton(getResources(R.string.retry), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();

                                    sendDataInBatches();

                                }
                            });*/

                builder.show();


            }


        });


    }




}














































