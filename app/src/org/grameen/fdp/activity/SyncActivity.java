package org.grameen.fdp.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.salesforce.androidsdk.rest.ApiVersionStrings;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.ui.SalesforceActivity;

import org.grameen.fdp.R;
import org.grameen.fdp.object.ActivitiesPlusInputs;
import org.grameen.fdp.object.Calculation;
import org.grameen.fdp.object.ComplexCalculation;
import org.grameen.fdp.object.Country;
import org.grameen.fdp.object.Input;
import org.grameen.fdp.object.Logic;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.object.Recommendation;
import org.grameen.fdp.object.RecommendationsPlusActivity;
import org.grameen.fdp.object.SkipLogic;
import org.grameen.fdp.object.Village;
import org.grameen.fdp.utility.CustomToast;
import org.grameen.fdp.utility.DatabaseDataClass;
import org.grameen.fdp.utility.DatabaseHelper;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.List;

import static java.sql.DriverManager.println;

/**
 * Created by aangjnr on 11/12/2017.
 */

public class SyncActivity extends SalesforceActivity {

    String nextRecordsUrl = "";
    RestRequest queryMore;
    Boolean done = null;
    RestRequest restRequest;


    RestClient restClient = null;
    ProgressDialog progressDialog;
    String TAG = "SyncActivity";

    DatabaseHelper databaseHelper;
    long start = 0;

    String ISO_CODE = "";
    Context context;

    Boolean syncInitialData;

    Boolean exception = false;
    int totalSize = 0;
    int batchSize = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        databaseHelper = DatabaseHelper.getInstance(this);

        syncInitialData = getIntent().getBooleanExtra("initialData", false);

        context = this;

        ISO_CODE = PreferenceManager.getDefaultSharedPreferences(this).getString("ISO", "");

        progressDialog = BaseActivity.showProgress(context, getResources(R.string.getting_data), getResources(R.string.please_wait), false);


    }

    @Override
    public void onResume(RestClient client) {


        restClient = client;


        if (syncInitialData) {


            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    refreshCountriesDataFromServer();

                }
            });

            thread.start();


        } else {


            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {


                    refreshQuestionsDataFromServer();

                }
            });

            thread.start();

        }


    }


    @Override
    public void onResume() {

        super.onResume();

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
        BaseActivity.printHeader(restRequest);

        start = System.nanoTime();


        restClient.sendAsync(restRequest, asyncRequestCallback);

    }


    void refreshQuestionsDataFromServer() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setMessage(getResources(R.string.update_questions_data));
            }
        });


        String soql =
                "select id, Name, Caption__c, Default_value__c, Display_Order__c, Error_text__c, Hide__c, Help_Text__c, Max_value__c, Min_value__c, Options__c, Type__c, Translation__c, Related_questions__c, Form__r.Name,  Form__r.Id, Form__r.Type__c from fpd_question__c where form__r.Country__r.ISO_code__c = '" + ISO_CODE + "'";

        RestRequest restRequest = null;
        try {
            restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);


            RestClient.AsyncRequestCallback asyncRequestCallback = new RestClient.AsyncRequestCallback() {
                @Override
                public void onSuccess(RestRequest request, final RestResponse result) {


                    result.consumeQuietly(); // consume before going back to main thread

                    // consume before going back to main thread

                    if (result.isSuccess()) {
                        Log.d(TAG, "THE RESULT IS " + "A SUCCESS");


                        try {
                            int size = 0;
                            size = result.asString().length();
                            long duration = System.nanoTime() - start;
                            int statusCode = result.getStatusCode();
                            BaseActivity.printRequestInfo(duration, size, statusCode);

                            Log.d(TAG, "THE RESULT IS ");
                            System.out.println(result.asJSONObject());


                            Gson gson = new Gson();
                            Type listType = new TypeToken<List<Question>>() {
                            }.getType();

                            JSONArray jsonArray = result.asJSONObject().getJSONArray("records");

                            List<Question> questions = (List<Question>) gson.fromJson(String.valueOf(jsonArray), listType);

                            Log.d(TAG, "SIZE OF QUESTIONS ARRAY IS " + questions.size());

                            if (databaseHelper.deleteQuestionsTable()) {

                                databaseHelper.deleteFormsTable();
                                for (Question q : questions) {

                                    databaseHelper.addAQuestion(q);

                                }

                                Log.d(TAG, "QUESTIONS HAVE BEEN UPDATED! TOTAL SIZE IS " + databaseHelper.queryNumberOfEntries(DatabaseDataClass.QUESTIONS_TABLE));
                                Log.d(TAG, "FORMS HAVE BEEN UPDATED! TOTAL SIZE IS " + databaseHelper.queryNumberOfEntries(DatabaseDataClass.FORMS_TABLE));

                                // CustomToast.makeToast(getApplicationContext(), "All data has been downloaded successfully!", Toast.LENGTH_LONG).show();


                            } else {

                                //CustomToast.makeToast(MainActivity.this, "Couldn't de", Toast.LENGTH_LONG).show();


                                Log.d(TAG, "QUESTIONS TABLE WASN'T DELETED ");


                            }


                            refreshCountriesDataFromServer();


                        } catch (IOException | JSONException e) {
                            BaseActivity.printException(e);
                            CustomToast.makeToast(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

                            progressDialog.dismiss();
                            finish();

                        }
                    } else {

                        Log.d(TAG, "THE RESULT IS A FAILURE");

                        AlertDialog.Builder builder = new AlertDialog.Builder(SyncActivity.this, R.style.AppDialog);
                        builder.setTitle(getResources(R.string.could_not_connect));
                        builder.setCancelable(false);
                        builder.setMessage(getResources(R.string.could_not_connect_rationale));
                        builder.setPositiveButton(getResources(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                finish();


                            }
                        });


                    }


                }

                @Override
                public void onError(final Exception exception) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {


                            progressDialog.dismiss();

                            Toast.makeText(getApplicationContext(), getResources(R.string.connection_error), Toast.LENGTH_LONG).show();


                            BaseActivity.printHeader("Could not build query request");
                            BaseActivity.printException(exception);


                            AlertDialog.Builder builder = new AlertDialog.Builder(SyncActivity.this, R.style.AppDialog);
                            builder.setTitle(getResources(R.string.could_not_connect));
                            builder.setCancelable(false);
                            builder.setMessage(getResources(R.string.could_not_connect_rationale));
                            builder.setPositiveButton(getResources(R.string.ok), new DialogInterface.OnClickListener() {
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
            };


            sendRequest(restRequest, asyncRequestCallback);


        } catch (UnsupportedEncodingException e) {
            CustomToast.makeToast(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

            progressDialog.dismiss();
            e.printStackTrace();
            finish();


        }


    }


    void refreshCountriesDataFromServer() {

      /*  runOnUiThread(new Runnable() {
            @Override
            public void run() {

                progressDialog.setMessage("Obtaining Country data");

            }
        });*/


        String soql = "select Id, Name, ISO_code__c, currencySign__c, Avg_Price_Kg__c from fpd_country__c ";

        RestRequest restRequest = null;
        try {
            restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);


            RestClient.AsyncRequestCallback asyncRequestCallback = new RestClient.AsyncRequestCallback() {
                @Override
                public void onSuccess(RestRequest request, final RestResponse result) {
                    result.consumeQuietly(); // consume before going back to main thread


                    if (result.isSuccess()) {
                        Log.d(TAG, "THE RESULT IS " + "A SUCCESS");


                        try {
                            int size = 0;
                            size = result.asString().length();
                            long duration = System.nanoTime() - start;
                            int statusCode = result.getStatusCode();
                            BaseActivity.printRequestInfo(duration, size, statusCode);

                            Log.d(TAG, "THE RESULT IS ");
                            System.out.println(result.asJSONObject());


                            Gson gson = new Gson();
                            Type listType = new TypeToken<List<Country>>() {
                            }.getType();

                            JSONArray jsonArray = result.asJSONObject().getJSONArray("records");

                            List<Country> countries = (List<Country>) gson.fromJson(String.valueOf(jsonArray), listType);

                            Log.d(TAG, "SIZE OF Country ARRAY IS " + countries.size());

                            if (databaseHelper.deleteCountriesTable()) {

                                for (Country v : countries) {

                                    databaseHelper.addCountry(v);

                                }

                                Log.d(TAG, "Country HAVE BEEN UPDATED! TOTAL SIZE IS " + countries.size());


                            } else {

                                Log.d(TAG, "Country TABLE WASN'T DELETED ");

                            }

                            if (syncInitialData) {

                                PreferenceManager.getDefaultSharedPreferences(SyncActivity.this).edit().putBoolean("initialData", false).apply();

                                progressDialog.dismiss();
                                finish();


                            } else {

                                refreshVillagesDataFromServer();

                            }


                        } catch (IOException | JSONException e) {
                            BaseActivity.printException(e);
                            CustomToast.makeToast(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

                            progressDialog.dismiss();
                            finish();

                        }
                    } else Log.d(TAG, "THE RESULT IS " + "A FAILURE");


                }

                @Override
                public void onError(final Exception exception) {


                    progressDialog.dismiss();

                    CustomToast.makeToast(getApplicationContext(), getResources(R.string.connection_error), Toast.LENGTH_LONG).show();


                    BaseActivity.printHeader("Could not build query request");
                    BaseActivity.printException(exception);

                    finish();


                }
            };


            sendRequest(restRequest, asyncRequestCallback);


        } catch (UnsupportedEncodingException e) {
            CustomToast.makeToast(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

            progressDialog.dismiss();
            e.printStackTrace();
            finish();


        }


    }


    void refreshVillagesDataFromServer() {


        String soql = "select Id, Name, district__c  from fpd_village__c  where district__r.Province_Region__r.Country__r.ISO_code__c ='" + ISO_CODE + "'";

        RestRequest restRequest = null;
        try {
            restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);


            RestClient.AsyncRequestCallback asyncRequestCallback = new RestClient.AsyncRequestCallback() {
                @Override
                public void onSuccess(RestRequest request, final RestResponse result) {
                    result.consumeQuietly(); // consume before going back to main thread


                    if (result.isSuccess()) {
                        Log.d(TAG, "THE RESULT IS " + "A SUCCESS");


                        try {
                            int size = 0;
                            size = result.asString().length();
                            long duration = System.nanoTime() - start;
                            int statusCode = result.getStatusCode();
                            BaseActivity.printRequestInfo(duration, size, statusCode);

                            Log.d(TAG, "THE RESULT IS ");
                            System.out.println(result.asJSONObject());


                            Gson gson = new Gson();
                            Type listType = new TypeToken<List<Village>>() {
                            }.getType();

                            JSONArray jsonArray = result.asJSONObject().getJSONArray("records");

                            List<Village> villages = (List<Village>) gson.fromJson(String.valueOf(jsonArray), listType);

                            Log.d(TAG, "SIZE OF VILLAGES ARRAY IS " + villages.size());

                            if (databaseHelper.deleteVillagesTable()) {

                                for (Village v : villages) {

                                    databaseHelper.addVillage(v);

                                }


                                Log.d(TAG, "VILLAGES HAVE BEEN UPDATED! TOTAL SIZE IS " + villages.size());


                            } else {

                                //CustomToast.makeToast(MainActivity.this, "Couldn't de", Toast.LENGTH_LONG).show();


                                Log.d(TAG, "VILLAGES TABLE WASN'T DELETED ");

                            }


                            refreshSkipLogicDataFromServer();


                        } catch (IOException | JSONException e) {
                            BaseActivity.printException(e);
                            CustomToast.makeToast(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

                            progressDialog.dismiss();
                            finish();

                        }
                    } else Log.d(TAG, "THE RESULT IS " + "A FAILURE");


                }

                @Override
                public void onError(final Exception exception) {


                    progressDialog.dismiss();

                    CustomToast.makeToast(getApplicationContext(), getResources(R.string.connection_error), Toast.LENGTH_LONG).show();


                    BaseActivity.printHeader("Could not build query request");
                    BaseActivity.printException(exception);

                    finish();


                }
            };


            sendRequest(restRequest, asyncRequestCallback);


        } catch (UnsupportedEncodingException e) {
            CustomToast.makeToast(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

            progressDialog.dismiss();
            e.printStackTrace();
            finish();


        }


    }


    void refreshSkipLogicDataFromServer() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                progressDialog.setMessage(getResources(R.string.getting_skip_logic));

            }
        });


        String soql = "select Id, Name, Question__c,  Question_1__c, Hide__c, Question_value__c, Logical_Operator__c from fpd_Skip_Logic__c where Contry_code__c ='" + ISO_CODE + "'";

        RestRequest restRequest = null;
        try {
            restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);


            RestClient.AsyncRequestCallback asyncRequestCallback = new RestClient.AsyncRequestCallback() {
                @Override
                public void onSuccess(RestRequest request, final RestResponse result) {
                    result.consumeQuietly(); // consume before going back to main thread


                    if (result.isSuccess()) {
                        Log.d(TAG, "THE RESULT IS " + "A SUCCESS");


                        try {
                            int size = 0;
                            size = result.asString().length();
                            long duration = System.nanoTime() - start;
                            int statusCode = result.getStatusCode();
                            BaseActivity.printRequestInfo(duration, size, statusCode);

                            Log.d(TAG, "THE RESULT IS ");
                            System.out.println(result.asJSONObject());


                            Gson gson = new Gson();
                            Type listType = new TypeToken<List<SkipLogic>>() {
                            }.getType();

                            JSONArray jsonArray = result.asJSONObject().getJSONArray("records");

                            List<SkipLogic> skipLogics = (List<SkipLogic>) gson.fromJson(String.valueOf(jsonArray), listType);

                            Log.d(TAG, "SIZE OF SKIP LOGIC ARRAY IS " + skipLogics.size());

                            if (databaseHelper.deleteSkipLogicsTable()) {

                                for (SkipLogic q : skipLogics) {

                                    databaseHelper.addSkipLogic(q);

                                }


                                Log.d(TAG, "SKIP LOGICS HAVE BEEN UPDATED! TOTAL SIZE IS " + skipLogics.size());


                            } else {

                                //CustomToast.makeToast(MainActivity.this, "Couldn't de", Toast.LENGTH_LONG).show();


                                Log.d(TAG, "SKIP LOGIC TABLE WASN'T DELETED ");

                            }


                            refreshRecommendationsDataFromServer();


                        } catch (IOException | JSONException e) {
                            BaseActivity.printException(e);
                            CustomToast.makeToast(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

                            progressDialog.dismiss();
                            finish();

                        }
                    } else Log.d(TAG, "THE RESULT IS " + "A FAILURE");


                }

                @Override
                public void onError(final Exception exception) {


                    progressDialog.dismiss();

                    CustomToast.makeToast(getApplicationContext(), getResources(R.string.connection_error), Toast.LENGTH_LONG).show();


                    BaseActivity.printHeader("Could not build query request");
                    BaseActivity.printException(exception);

                    finish();


                }
            };


            sendRequest(restRequest, asyncRequestCallback);


        } catch (UnsupportedEncodingException e) {
            CustomToast.makeToast(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

            progressDialog.dismiss();
            e.printStackTrace();
            finish();


        }


    }


    void refreshRecommendationsDataFromServer() {


        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                progressDialog.setMessage(getResources(R.string.getting_recommendations));

            }
        });

        String soql = "select Id, Name, Condition__c, description__c, Hierarchy__c, Cost_Year_0__c, Cost_questions__c, Income_Year_0__c, Income_Year_1__c, Income_Year_2__c, Income_Year_3__c, Income_Year_4__c, Income_Year_5__c, Income_Year_6__c, " +
                "Income_Year_7__c, Logic__c, Related_1__c, Related_2__c, Income_questions__c, Year_back_to_Gaps__c from fpd_recommendation__c where Contry_code__c ='" + ISO_CODE + "'";

        RestRequest restRequest = null;
        try {
            restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);


            RestClient.AsyncRequestCallback asyncRequestCallback = new RestClient.AsyncRequestCallback() {
                @Override
                public void onSuccess(RestRequest request, final RestResponse result) {
                    result.consumeQuietly(); // consume before going back to main thread

                    // consume before going back to main thread


                    if (result.isSuccess()) {
                        Log.d(TAG, "THE RESULT IS " + "A SUCCESS");


                        try {
                            int size = 0;
                            size = result.asString().length();
                            long duration = System.nanoTime() - start;
                            int statusCode = result.getStatusCode();
                            BaseActivity.printRequestInfo(duration, size, statusCode);

                            Log.d(TAG, "THE RESULT IS ");
                            System.out.println(result.asJSONObject());


                            Gson gson = new Gson();
                            Type listType = new TypeToken<List<Recommendation>>() {
                            }.getType();

                            JSONArray jsonArray = result.asJSONObject().getJSONArray("records");

                            List<Recommendation> recommendations = (List<Recommendation>) gson.fromJson(String.valueOf(jsonArray), listType);

                            recommendations = databaseHelper.sortRecommendations(recommendations);

                            Log.d(TAG, "SIZE OF RECOMMENDATIONS ARRAY IS " + recommendations.size());

                            if (databaseHelper.deleteRecommendationsTable()) {

                                for (Recommendation q : recommendations) {

                                    databaseHelper.addRecommendation(q);

                                }

                                Log.d(TAG, "RECOMMENDATIONS HAVE BEEN UPDATED! TOTAL SIZE IS " + recommendations.size());

                            } else {

                                //CustomToast.makeToast(MainActivity.this, "Couldn't de", Toast.LENGTH_LONG).show();
                                Log.d(TAG, "RECOMMENDATIONS TABLE WASN'T DELETED ");

                            }


                            refreshRecommendationsPlusActivitiesDataFromServer();


                        } catch (IOException | JSONException e) {
                            BaseActivity.printException(e);
                            CustomToast.makeToast(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

                            progressDialog.dismiss();
                            finish();
                        }
                    } else Log.d(TAG, "THE RESULT IS " + "A FAILURE");


                }

                @Override
                public void onError(final Exception exception) {


                    progressDialog.dismiss();
                    CustomToast.makeToast(getApplicationContext(), getResources(R.string.connection_error), Toast.LENGTH_LONG).show();


                    BaseActivity.printHeader("Could not build query request");
                    BaseActivity.printException(exception);

                    finish();


                }
            };


            sendRequest(restRequest, asyncRequestCallback);


        } catch (UnsupportedEncodingException e) {
            CustomToast.makeToast(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

            progressDialog.dismiss();
            e.printStackTrace();
            finish();


        }


    }


    void refreshRecommendationsPlusActivitiesDataFromServer() {

        done = false;
        totalSize = 0;
        batchSize = 0;
        queryMore = null;
        nextRecordsUrl = null;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                progressDialog.setMessage(getResources(R.string.getting_more));

            }
        });

        String soql = "select Id, Name, Activity__c, Activity_name__c, Labor_cost__c, Labor_days_need__c, Month__c, Year__c, Supplies_cost__c, Recommendation__c from fpd_Recomendation_Activity__c where Recommendation__r.Contry_code__c ='" + ISO_CODE + "'";

        restRequest = null;

        try {
            restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);
            databaseHelper.deleteRecommendationPlusAcivityTable();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        if (restRequest != null)
            getAllRecommendationsPlusActivityData(restRequest);


    }

    void getAllRecommendationsPlusActivityData(final RestRequest restRequest) {


        RestClient.AsyncRequestCallback asyncRequestCallback = new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread

                if (result.isSuccess()) {
                    Log.d(TAG, "THE RESULT IS " + "A SUCCESS");


                    try {
                        done = Boolean.valueOf(result.asJSONObject().getString("done"));
                        totalSize = Integer.parseInt(result.asJSONObject().getString("totalSize"));
                        done = Boolean.valueOf(result.asJSONObject().getString("done"));


                        int size = 0;
                        size = result.asString().length();
                        long duration = System.nanoTime() - start;
                        int statusCode = result.getStatusCode();
                        BaseActivity.printRequestInfo(duration, size, statusCode);

                        Log.d(TAG, "THE RESULT IS ");
                        System.out.println(result.asJSONObject());


                        Gson gson = new Gson();
                        Type listType = new TypeToken<List<RecommendationsPlusActivity>>() {
                        }.getType();

                        JSONArray jsonArray = result.asJSONObject().getJSONArray("records");

                        List<RecommendationsPlusActivity> recommendations = (List<RecommendationsPlusActivity>) gson.fromJson(String.valueOf(jsonArray), listType);

                        Log.d(TAG, "SIZE OF RECOMMENDATIONS_PLUS_ACTIVITY ARRAY IS " + recommendations.size());

                        Log.d(TAG, "TOTAL SIZE IS " + totalSize);

                        batchSize = recommendations.size();
                        Log.d(TAG, "BATCH SIZE IS " + batchSize);


                        for (RecommendationsPlusActivity q : recommendations) {

                            databaseHelper.addRecommendationPlusAcivity(q);

                        }


                        if (!done) {
                            nextRecordsUrl = result.asJSONObject().getString("nextRecordsUrl");
                            getAllRecommendationsPlusActivityData(new RestRequest(RestRequest.RestMethod.GET, nextRecordsUrl));


                        } else {
                            Log.d(TAG, "RECOMMENDATION PLUS ACTIVITY HAVE BEEN UPDATED! TOTAL SIZE IS " + databaseHelper.queryNumberOfEntries(DatabaseDataClass.RECOMMENDATIONS_PLUS_ACTIVITIES_TABLE));
                            refreshActivitiesPlusInputsDataFromServer();

                        }

                    } catch (IOException | JSONException e) {
                        BaseActivity.printException(e);

                        //progressDialog.dismiss();
                        //finish();
                    }
                } else Log.d(TAG, "THE RESULT IS " + "A FAILURE");


            }

            @Override
            public void onError(final Exception exception) {

                progressDialog.dismiss();
                CustomToast.makeToast(getApplicationContext(), getResources(R.string.connection_error), Toast.LENGTH_LONG).show();

                done = true;

                BaseActivity.printHeader("Could not build query request");
                BaseActivity.printException(exception);

                finish();


            }
        };


        sendRequest(restRequest, asyncRequestCallback);


    }


    void refreshActivitiesPlusInputsDataFromServer() {

        done = false;
        totalSize = 0;
        batchSize = 0;
        queryMore = null;
        nextRecordsUrl = null;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                progressDialog.setMessage(getResources(R.string.getting_even_more));

            }
        });

        String soql = "select Id, Name,  Input_type__c, Quantity__c, Input__r.Unit__c, Input__r.Cost__c,   Cost__c,  Recommendation__c,  reco__c from fpd_Activity_Input__c where Contry_code__c ='" + ISO_CODE + "'";

        restRequest = null;

        try {
            restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);
            databaseHelper.deleteActivityPlusInputTable();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        if (restRequest != null)
            getAllActivitiesPlusInputsData(restRequest);


    }

    void getAllActivitiesPlusInputsData(final RestRequest restRequest) {


        RestClient.AsyncRequestCallback asyncRequestCallback = new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread

                if (result.isSuccess()) {
                    Log.d(TAG, "THE RESULT IS " + "A SUCCESS");


                    try {
                        done = Boolean.valueOf(result.asJSONObject().getString("done"));
                        totalSize = Integer.parseInt(result.asJSONObject().getString("totalSize"));
                        done = Boolean.valueOf(result.asJSONObject().getString("done"));


                        int size = 0;
                        size = result.asString().length();
                        long duration = System.nanoTime() - start;
                        int statusCode = result.getStatusCode();
                        BaseActivity.printRequestInfo(duration, size, statusCode);

                        Log.d(TAG, "THE RESULT IS ");
                        System.out.println(result.asJSONObject());


                        Gson gson = new Gson();
                        Type listType = new TypeToken<List<ActivitiesPlusInputs>>() {
                        }.getType();

                        JSONArray jsonArray = result.asJSONObject().getJSONArray("records");

                        List<ActivitiesPlusInputs> activitiesPlusInputs = (List<ActivitiesPlusInputs>) gson.fromJson(String.valueOf(jsonArray), listType);

                        Log.d(TAG, "SIZE OF ACTIVITY_PLUS_INPUTS ARRAY IS " + activitiesPlusInputs.size());

                        Log.d(TAG, "TOTAL SIZE IS " + totalSize);

                        batchSize = activitiesPlusInputs.size();
                        Log.d(TAG, "BATCH SIZE IS " + batchSize);


                        for (ActivitiesPlusInputs q : activitiesPlusInputs) {

                            databaseHelper.addActivityPlusInput(q);

                        }


                        if (!done) {
                            nextRecordsUrl = result.asJSONObject().getString("nextRecordsUrl");
                            getAllActivitiesPlusInputsData(new RestRequest(RestRequest.RestMethod.GET, nextRecordsUrl));


                        } else {
                            Log.d(TAG, "ALL ACTIVITY_PLUS_INPUTS HAVE BEEN UPDATED! TOTAL SIZE IS " + databaseHelper.queryNumberOfEntries(DatabaseDataClass.ACTIVITIY_PLUS_INPUTS_TABLE));


                            refreshInputsDataFromServer();
                        }

                    } catch (IOException | JSONException e) {
                        BaseActivity.printException(e);

                        //progressDialog.dismiss();
                        //finish();
                    }
                } else Log.d(TAG, "THE RESULT IS " + "A FAILURE");


            }

            @Override
            public void onError(final Exception exception) {

                progressDialog.dismiss();
                CustomToast.makeToast(getApplicationContext(), getResources(R.string.connection_error), Toast.LENGTH_LONG).show();

                done = true;

                BaseActivity.printHeader("Could not build query request");
                BaseActivity.printException(exception);

                finish();


            }
        };


        sendRequest(restRequest, asyncRequestCallback);


    }


    void refreshInputsDataFromServer() {

        done = false;
        totalSize = 0;
        batchSize = 0;
        queryMore = null;
        nextRecordsUrl = null;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                progressDialog.setMessage(getResources(R.string.getting_final_data));

            }
        });

        String soql = "select Id, Name,    Cost__c,  Province_Region__c,  Type__c, Unit__c from fpd_Inputs__c where Province_Region__r.Country__r.ISO_code__c ='" + ISO_CODE + "'";

        restRequest = null;

        try {
            restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);
            databaseHelper.deleteInputTable();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        if (restRequest != null)
            getAllInputsData(restRequest);


    }

    void getAllInputsData(final RestRequest restRequest) {


        RestClient.AsyncRequestCallback asyncRequestCallback = new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse result) {
                result.consumeQuietly(); // consume before going back to main thread

                if (result.isSuccess()) {
                    Log.d(TAG, "THE RESULT IS " + "A SUCCESS");


                    try {
                        done = Boolean.valueOf(result.asJSONObject().getString("done"));
                        totalSize = Integer.parseInt(result.asJSONObject().getString("totalSize"));
                        done = Boolean.valueOf(result.asJSONObject().getString("done"));


                        int size = 0;
                        size = result.asString().length();
                        long duration = System.nanoTime() - start;
                        int statusCode = result.getStatusCode();
                        BaseActivity.printRequestInfo(duration, size, statusCode);

                        Log.d(TAG, "THE RESULT IS ");
                        System.out.println(result.asJSONObject());


                        Gson gson = new Gson();
                        Type listType = new TypeToken<List<Input>>() {
                        }.getType();

                        JSONArray jsonArray = result.asJSONObject().getJSONArray("records");

                        List<Input> activitiesPlusInputs = (List<Input>) gson.fromJson(String.valueOf(jsonArray), listType);

                        Log.d(TAG, "SIZE OF INPUTS ARRAY IS " + activitiesPlusInputs.size());

                        Log.d(TAG, "TOTAL SIZE IS " + totalSize);

                        batchSize = activitiesPlusInputs.size();
                        Log.d(TAG, "BATCH SIZE IS " + batchSize);


                        for (Input q : activitiesPlusInputs) {

                            databaseHelper.addInput(q);

                        }


                        if (!done) {
                            nextRecordsUrl = result.asJSONObject().getString("nextRecordsUrl");
                            getAllInputsData(new RestRequest(RestRequest.RestMethod.GET, nextRecordsUrl));


                        } else {
                            Log.d(TAG, "ALL INPUTS HAVE BEEN UPDATED! TOTAL SIZE IS " + databaseHelper.queryNumberOfEntries(DatabaseDataClass.INPUTS_TABLE));


                            refreshLogicDataFromServer();
                        }

                    } catch (IOException | JSONException e) {
                        BaseActivity.printException(e);

                        //progressDialog.dismiss();
                        //finish();
                    }
                } else Log.d(TAG, "THE RESULT IS " + "A FAILURE");


            }

            @Override
            public void onError(final Exception exception) {

                progressDialog.dismiss();
                CustomToast.makeToast(getApplicationContext(), getResources(R.string.connection_error), Toast.LENGTH_LONG).show();

                done = true;

                BaseActivity.printHeader("Could not build query request");
                BaseActivity.printException(exception);

                finish();


            }
        };


        sendRequest(restRequest, asyncRequestCallback);


    }

    void refreshLogicDataFromServer() {


        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                progressDialog.setMessage(getResources(R.string.getting_logic));

            }
        });


        String soql = "select Id, Name, Parent_logic__c, Parent_logical_operator__c, Result__c, Result_questions__c, Logical_operator_1__c, Logical_operator_2__c," +
                " Logical_operator_3__c, Logical_operator_4__c, Logical_operator_5__c, Logical_operator_6__c, Logical_operator_7__c, Logical_operator_8__c, " +
                "Logical_operator_9__c, Logical_operator_10__c, Question_1__c,  Question_2__c,  Question_3__c,  Question_4__c,  Question_5__c,  Question_6__c, " +
                " Question_7__c,  Question_8__c,  Question_9__c,  Question_10__c, Question_logic_operation_1__c, Question_logic_operation_2__c, " +
                "Question_logic_operation_3__c, Question_logic_operation_4__c, Question_logic_operation_5__c, Question_logic_operation_6__c, " +
                "Question_logic_operation_7__c, Question_logic_operation_8__c, Question_logic_operation_9__c," +
                "Value_1__c, Value_2__c, Value_3__c, Value_4__c, Value_5__c, Value_6__c, Value_7__c, Value_8__c, Value_9__c, Value_10__c " +
                "from fpd_Logic__c where Contry_code__c = '" + ISO_CODE + "'";

        RestRequest restRequest = null;
        try {
            restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);


            RestClient.AsyncRequestCallback asyncRequestCallback = new RestClient.AsyncRequestCallback() {
                @Override
                public void onSuccess(RestRequest request, final RestResponse result) {
                    result.consumeQuietly(); // consume before going back to main thread

                    // consume before going back to main thread


                    if (result.isSuccess()) {
                        Log.d(TAG, "THE RESULT IS " + "A SUCCESS");


                        try {
                            int size = 0;
                            size = result.asString().length();
                            long duration = System.nanoTime() - start;
                            int statusCode = result.getStatusCode();
                            BaseActivity.printRequestInfo(duration, size, statusCode);

                            Log.d(TAG, "THE RESULT IS ");
                            System.out.println(result.asJSONObject());


                            Gson gson = new Gson();
                            Type listType = new TypeToken<List<Logic>>() {
                            }.getType();

                            JSONArray jsonArray = result.asJSONObject().getJSONArray("records");

                            List<Logic> logics = (List<Logic>) gson.fromJson(String.valueOf(jsonArray), listType);

                            Log.d(TAG, "SIZE OF LOGIC ARRAY IS " + logics.size());

                            if (databaseHelper.deleteLogicsTable()) {

                                for (Logic q : logics) {

                                    databaseHelper.addLogic(q);

                                }


                                Log.d(TAG, "LOGICS HAVE BEEN UPDATED! TOTAL SIZE IS " + logics.size());


                            } else {

                                Log.d(TAG, "LOGIC TABLE WASN'T DELETED ");

                            }


                            refreshComplexCalculationsDataFromServer();


                        } catch (IOException | JSONException e) {
                            BaseActivity.printException(e);
                            CustomToast.makeToast(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

                            progressDialog.dismiss();
                            finish();

                        }
                    } else Log.d(TAG, "THE RESULT IS " + "A FAILURE");


                }

                @Override
                public void onError(final Exception exception) {


                    progressDialog.dismiss();


                    CustomToast.makeToast(getApplicationContext(), getResources(R.string.connection_error), Toast.LENGTH_LONG).show();


                    BaseActivity.printHeader("Could not build query request");
                    BaseActivity.printException(exception);

                    finish();
                }
            };


            sendRequest(restRequest, asyncRequestCallback);


        } catch (UnsupportedEncodingException e) {
            CustomToast.makeToast(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

            progressDialog.dismiss();
            e.printStackTrace();
            finish();


        }


    }

    void refreshComplexCalculationsDataFromServer() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                progressDialog.setMessage(getResources(R.string.getting_cc));

            }
        });


        String soql = "select Id, Name, Question__c, Condition__c from fdp_complex_calculation__c";

        RestRequest restRequest = null;
        try {
            restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);


            RestClient.AsyncRequestCallback asyncRequestCallback = new RestClient.AsyncRequestCallback() {
                @Override
                public void onSuccess(RestRequest request, final RestResponse result) {
                    result.consumeQuietly(); // consume before going back to main thread

                    // consume before going back to main thread


                    if (result.isSuccess()) {
                        Log.d(TAG, "THE RESULT IS " + "A SUCCESS");


                        try {
                            int size = 0;
                            size = result.asString().length();
                            long duration = System.nanoTime() - start;
                            int statusCode = result.getStatusCode();
                            BaseActivity.printRequestInfo(duration, size, statusCode);

                            Log.d(TAG, "THE RESULT IS ");
                            System.out.println(result.asJSONObject());


                            Gson gson = new Gson();
                            Type listType = new TypeToken<List<ComplexCalculation>>() {
                            }.getType();

                            JSONArray jsonArray = result.asJSONObject().getJSONArray("records");

                            List<ComplexCalculation> calculations = (List<ComplexCalculation>) gson.fromJson(String.valueOf(jsonArray), listType);

                            Log.d(TAG, "SIZE OF CALCULATIONS ARRAY IS " + calculations.size());

                            if (databaseHelper.deleteComplexCalculationTable()) {

                                for (ComplexCalculation calc : calculations) {

                                    databaseHelper.addComplexCalculation(calc);

                                }


                                Log.d(TAG, "COMPLEX CALCULATIONS TABLE HAVE BEEN UPDATED! TOTAL SIZE IS " + calculations.size());





                            } else {

                                Log.d(TAG, "COMPLEX CALCULATIONS TABLE WASN'T DELETED ");

                            }


                            refreshCalculationsDataFromServer();


                        } catch (IOException | JSONException e) {
                            BaseActivity.printException(e);
                            CustomToast.makeToast(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

                            progressDialog.dismiss();
                            finish();

                        }
                    }
                }

                @Override
                public void onError(final Exception exception) {


                    progressDialog.dismiss();

                    CustomToast.makeToast(getApplicationContext(), getResources(R.string.connection_error), Toast.LENGTH_LONG).show();

                    BaseActivity.printHeader("Could not build query request");
                    BaseActivity.printException(exception);

                    finish();

                }
            };

            sendRequest(restRequest, asyncRequestCallback);


        } catch (UnsupportedEncodingException e) {
            CustomToast.makeToast(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

            progressDialog.dismiss();
            e.printStackTrace();
            finish();


        }


    }

    void refreshCalculationsDataFromServer() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                progressDialog.setMessage(getResources(R.string.getting_c));

            }
        });


        String soql = "select Id, Name, Question_1__c,  Operator_1__c, Question_2__c,  Operator_2__c, Question_3__c,  Operator_3__c, Question_4__c, Result_question__c, Calculation_order__c, Contry_code__c from fpd_Calculation__c where Contry_code__c = '" + ISO_CODE + "'";

        RestRequest restRequest = null;
        try {
            restRequest = RestRequest.getRequestForQuery(ApiVersionStrings.getVersionNumber(this), soql);


            RestClient.AsyncRequestCallback asyncRequestCallback = new RestClient.AsyncRequestCallback() {
                @Override
                public void onSuccess(RestRequest request, final RestResponse result) {
                    result.consumeQuietly(); // consume before going back to main thread

                    // consume before going back to main thread


                    if (result.isSuccess()) {
                        Log.d(TAG, "THE RESULT IS " + "A SUCCESS");


                        try {
                            int size = 0;
                            size = result.asString().length();
                            long duration = System.nanoTime() - start;
                            int statusCode = result.getStatusCode();
                            BaseActivity.printRequestInfo(duration, size, statusCode);

                            Log.d(TAG, "THE RESULT IS ");
                            System.out.println(result.asJSONObject());


                            Gson gson = new Gson();
                            Type listType = new TypeToken<List<Calculation>>() {
                            }.getType();

                            JSONArray jsonArray = result.asJSONObject().getJSONArray("records");

                            List<Calculation> calculations = (List<Calculation>) gson.fromJson(String.valueOf(jsonArray), listType);

                            Log.d(TAG, "SIZE OF CALCULATIONS ARRAY IS " + calculations.size());

                            if (databaseHelper.deleteCalculationsTable()) {

                                for (Calculation calc : calculations) {

                                    databaseHelper.addCalculation(calc);

                                }


                                Log.d(TAG, "CALCULATIONS TABLE HAVE BEEN UPDATED! TOTAL SIZE IS " + calculations.size());


                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CustomToast.makeToast(getApplicationContext(), "All data has been downloaded successfully!", Toast.LENGTH_LONG).show();

                                    }
                                });
                            } else {

                                Log.d(TAG, "CALCULATIONS TABLE WASN'T DELETED ");

                            }


                        } catch (IOException | JSONException e) {
                            BaseActivity.printException(e);
                            CustomToast.makeToast(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

                            //progressDialog.dismiss();
                            //finish();

                        } finally {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {


                                    progressDialog.dismiss();

                                    AlertDialog.Builder builder = new AlertDialog.Builder(SyncActivity.this, R.style.AppDialog);
                                    builder.setTitle(getResources(R.string.download_complete));
                                    builder.setCancelable(true);
                                    builder.setMessage(getResources(R.string.all_data_downloaded));
                                    builder.setPositiveButton(getResources(R.string.ok), new DialogInterface.OnClickListener() {
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
                    } else Log.d(TAG, "THE RESULT IS " + "A FAILURE");


                }

                @Override
                public void onError(final Exception exception) {


                    progressDialog.dismiss();

                    CustomToast.makeToast(getApplicationContext(), getResources(R.string.connection_error), Toast.LENGTH_LONG).show();

                    BaseActivity.printHeader("Could not build query request");
                    BaseActivity.printException(exception);

                    finish();

                }
            };

            sendRequest(restRequest, asyncRequestCallback);


        } catch (UnsupportedEncodingException e) {
            CustomToast.makeToast(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

            progressDialog.dismiss();
            e.printStackTrace();
            finish();


        }


    }



    public String getResources(int resource){
        return getString(resource);
    }

}













































