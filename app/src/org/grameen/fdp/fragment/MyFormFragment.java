package org.grameen.fdp.fragment;

import android.animation.Animator;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.grameen.fdp.activity.PlotDetailsActivity;
import org.grameen.fdp.object.Calculation;
import org.grameen.fdp.object.Form;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.object.RealPlot;
import org.grameen.fdp.object.SkipLogic;
import org.grameen.fdp.utility.ButtonController;
import org.grameen.fdp.utility.CheckBoxController;
import org.grameen.fdp.utility.Constants;
import org.grameen.fdp.utility.CustomToast;
import org.grameen.fdp.utility.DatabaseHelper;
import org.grameen.fdp.utility.DatePickerController;
import org.grameen.fdp.utility.EditTextController;
import org.grameen.fdp.utility.MyFormController;
import org.grameen.fdp.utility.MyFormSectionController;
import org.grameen.fdp.utility.PhotoButtonController;
import org.grameen.fdp.utility.SelectionController;
import org.grameen.fdp.utility.TimePickerController;
import org.json.JSONException;
import org.json.JSONObject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Created by aangjnr on 01/12/2017.
 */


public class MyFormFragment extends FormFragment {


    List<Question> questions = new ArrayList<>();
    MyFormSectionController formSectionController;
    DatabaseHelper databaseHelper;
    String formName = "";
    String farmerCode = "";
    boolean shouldLoadOldValues = false;
    boolean isEnabled;

    String TAG = "MYFORMFRAGMENT";

    MyFormSectionController PLOT_INFORMATION_CONTROLLER;

    MyFormSectionController AO_SECTION_CONTROLLER;
    MyFormSectionController AO_RESULTS_SECTION_CONTROLLER;
    MyFormSectionController ADDITIONAL_INTERVENTION_CONTROLLER;

    JSONObject ALL_ANSWERS_JSON_OBJECT;
    /*JSONObject AO_JSON_OBJECT;
    JSONObject AOR_JSON_OBJECT;
    JSONObject AI_JSON_OBJECT;
    JSONObject PLOT_INFO_JSON_OBJECT;*/


    List<Question> plotInfoQuestions = new ArrayList<>();

    List<Question> aoQuestions;
    List<Question> aoResultsQuestions;
    List<Question> additionalInterventionQuestions;
    ScriptEngine engine;
    String CURRENT_PLOT_ID;
    RealPlot PLOT;
    Form form;


    public MyFormFragment() {

    }

    public static MyFormFragment newInstance(String formName, boolean shouldLoadOldValues, @Nullable String farmerCode, boolean disableFormControlller) {


        MyFormFragment formFragment = new MyFormFragment();

        Bundle bundle = new Bundle();
        bundle.putString("FORM_NAME", formName);
        bundle.putBoolean("loadValues", shouldLoadOldValues);
        bundle.putBoolean("disable", disableFormControlller);

        bundle.putString("code", farmerCode);

        formFragment.setArguments(bundle);
        return formFragment;


    }

    @Override
    public void onAttach(Context context) {

        formName = getArguments().getString("FORM_NAME");
        shouldLoadOldValues = getArguments().getBoolean("loadValues");
        isEnabled = getArguments().getBoolean("disable");

        farmerCode = getArguments().getString("code");
        databaseHelper = DatabaseHelper.getInstance(context);


        super.onAttach(context);
    }

    @Override
    public void initForm(MyFormController controller) {

        Context context = getContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        CURRENT_PLOT_ID = preferences.getString("currentPlotId", null);





        // Add new Questions with data model of Question.class
        // The Question data model takes 2 or more parameters based on the type value


        if (formName.equals(Constants.ADOPTION_OBSERVATIONS)) {

            form = databaseHelper.getFormBasedOnName(Constants.ADOPTION_OBSERVATIONS);
            AO_SECTION_CONTROLLER = new MyFormSectionController(context, (preferences.getBoolean("toggleTranslation", false)) ? form.getTranslation() : form.getDiaplayName());

            form = databaseHelper.getFormBasedOnName(Constants.PLOT_INFORMATION);
            PLOT_INFORMATION_CONTROLLER = new MyFormSectionController(context, (preferences.getBoolean("toggleTranslation", false)) ? form.getTranslation() : form.getDiaplayName());

            if (!shouldLoadOldValues) {
                Log.d("MY FORM FRAG", "NO DEFAULT VALUES TO LOAD");


                plotInfoQuestions.addAll(databaseHelper.getSpecificSetOfQuestions(Constants.PLOT_INFORMATION));
                loadQuestions(context, plotInfoQuestions, PLOT_INFORMATION_CONTROLLER);


                aoQuestions = databaseHelper.getSpecificSetOfQuestions(Constants.ADOPTION_OBSERVATIONS);
                loadQuestions(context, aoQuestions, AO_SECTION_CONTROLLER);


                questions.addAll(plotInfoQuestions);
                questions.addAll(aoQuestions);


                if (getActivity() instanceof PlotDetailsActivity) {

                    form = databaseHelper.getFormBasedOnName(Constants.ADOPTION_OBSERVATION_RESULTS);
                    AO_RESULTS_SECTION_CONTROLLER = new MyFormSectionController(context, (preferences.getBoolean("toggleTranslation", false)) ? form.getTranslation() : form.getDiaplayName());

                    aoResultsQuestions = databaseHelper.getSpecificSetOfQuestions(Constants.ADOPTION_OBSERVATION_RESULTS);
                    loadQuestions(context, aoResultsQuestions, AO_RESULTS_SECTION_CONTROLLER);


                    form = databaseHelper.getFormBasedOnName(Constants.ADDITIONAL_INTERVENTION);
                    ADDITIONAL_INTERVENTION_CONTROLLER = new MyFormSectionController(context, (preferences.getBoolean("toggleTranslation", false)) ? form.getTranslation() : form.getDiaplayName());

                    additionalInterventionQuestions = databaseHelper.getSpecificSetOfQuestions(Constants.ADDITIONAL_INTERVENTION);
                    loadQuestions(context, additionalInterventionQuestions, ADDITIONAL_INTERVENTION_CONTROLLER);


                    questions.addAll(aoResultsQuestions);
                    questions.addAll(additionalInterventionQuestions);


                }


            } else {
                Log.d("MY FORM FRAG", "LOAD DEFAULT VALUES");

/////////////////////////////////////////

                String[]ids  = farmerCode.split("_");
                PLOT = databaseHelper.getFarmerPlot(ids[1], ids[0]);

                Log.i(TAG, "PLOT INFO = " + PLOT.getPlotInformationJson());


                try{

                    ALL_ANSWERS_JSON_OBJECT = new JSONObject(PLOT.getPlotInformationJson());
                }catch(Exception e){

                    e.printStackTrace();
                }

                plotInfoQuestions.addAll(databaseHelper.getSpecificSetOfQuestions(Constants.PLOT_INFORMATION));
                loadQuestionsWithValues(context, plotInfoQuestions, PLOT_INFORMATION_CONTROLLER);

                aoQuestions = databaseHelper.getSpecificSetOfQuestions(Constants.ADOPTION_OBSERVATIONS);
                loadQuestionsWithValues(context, aoQuestions, AO_SECTION_CONTROLLER);


                questions.addAll(plotInfoQuestions);
                questions.addAll(aoQuestions);

            }
            if (getActivity() instanceof PlotDetailsActivity) {


                Log.i("MY FORM FRAG", "LOAD DEFAULT VALUES");


                form = databaseHelper.getFormBasedOnName(Constants.ADOPTION_OBSERVATION_RESULTS);
                AO_RESULTS_SECTION_CONTROLLER = new MyFormSectionController(context, (preferences.getBoolean("toggleTranslation", false)) ? form.getTranslation() : form.getDiaplayName());
                aoResultsQuestions = databaseHelper.getSpecificSetOfQuestions(Constants.ADOPTION_OBSERVATION_RESULTS);
                loadQuestionsWithValues(context, aoResultsQuestions, AO_RESULTS_SECTION_CONTROLLER);


                form = databaseHelper.getFormBasedOnName(Constants.ADDITIONAL_INTERVENTION);
                ADDITIONAL_INTERVENTION_CONTROLLER = new MyFormSectionController(context, (preferences.getBoolean("toggleTranslation", false)) ? form.getTranslation() : form.getDiaplayName());
                additionalInterventionQuestions = databaseHelper.getSpecificSetOfQuestions(Constants.ADDITIONAL_INTERVENTION);
                loadQuestionsWithValues(context, additionalInterventionQuestions, ADDITIONAL_INTERVENTION_CONTROLLER);


                questions.addAll(aoResultsQuestions);
                questions.addAll(additionalInterventionQuestions);


            }

            if (PLOT_INFORMATION_CONTROLLER != null)
                controller.addSection(PLOT_INFORMATION_CONTROLLER);

            if (AO_SECTION_CONTROLLER != null)
                controller.addSection(AO_SECTION_CONTROLLER);

            if (AO_RESULTS_SECTION_CONTROLLER != null)
                controller.addSection(AO_RESULTS_SECTION_CONTROLLER);

            if (ADDITIONAL_INTERVENTION_CONTROLLER != null)
                controller.addSection(ADDITIONAL_INTERVENTION_CONTROLLER);


////////////////////////////////////////

        } else {

            try {
                ALL_ANSWERS_JSON_OBJECT = new JSONObject(databaseHelper.getAllAnswersJson(farmerCode));
            } catch (JSONException e) {
                e.printStackTrace();

                ALL_ANSWERS_JSON_OBJECT = new JSONObject();
            }


            questions = databaseHelper.getSpecificSetOfQuestions(formName);
            if (questions != null) {
                Log.d("FORM FRAGMENT", "QUESTIONS SIZE IS " + questions.size());

            } else {
                Log.d("FORM FRAGMENT", "QUESTIONS SIZE IS NULLLLL");
            }

            form = databaseHelper.getFormBasedOnName(formName);
            formSectionController = new MyFormSectionController(getContext(), (preferences.getBoolean("toggleTranslation", false)) ? form.getTranslation() : form.getDiaplayName());


            if (!shouldLoadOldValues) {

                Log.d("MYFORMFRAG", "NO DEFAULT VALUES TO LOAD");
                loadQuestions(context, questions, formSectionController);


            } else {

                Log.d("MYFORMFRAG", "LOAD DEFAULT VALUES");

                loadQuestionsWithValues(context, questions, formSectionController);

            }


            controller.addSection(formSectionController);

        }



    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.i(TAG, "\n\n\n************************************ ON ACTIVITY CREATED" + "\n\n\n************************************");


        if (shouldLoadOldValues)
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    initiateSkipLogicsAndHideViews(questions, getFormController());

                }
            }, 500);


    }


    // This method iterates through the questions list, append their respective answers and
    // parses the list into a JSON string

    public boolean validate() throws JSONException {
        getFormController().resetValidationErrors();
        if (getFormController().isValidInput()) {

            //Send data to server here after getting JSON string

            //Toast.makeText(getContext(), getAllAnswersInJSON(), Toast.LENGTH_LONG).show();

            CustomToast.makeToast(getActivity(), getAllAnswersInJSON(), Toast.LENGTH_LONG).show();

        } else {

            // Whoaaaaaaa! There were some invalid inputs
            getFormController().showValidationErrors();

        }
        return true;
    }

    public String getAllAnswersInJSON() {


        JSONObject jsonObject = new JSONObject();

        for (Question q : questions) {


            try {
                jsonObject.put(q.getId(), getModel().getValue(q.getId()));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        return jsonObject.toString();


    }

    public JSONObject getAllAnswersInJSONObject() {


        JSONObject jsonObject = new JSONObject();

        for (Question q : questions) {


            try {
                jsonObject.put(q.getId(), getModel().getValue(q.getId()));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        return jsonObject;


    }













    void loadQuestions(Context context, List<Question> questions, MyFormSectionController formSectionController) {


        for (final Question q : questions) {


            if(q.getHide__c().equalsIgnoreCase("false")){


                if(q.getForm__r().getType().equalsIgnoreCase("monitoring")) isEnabled = false;

                Log.d("MY FORM FRAG ", "TYPE IS " + q.getType__c());
            switch (q.getType__c().toLowerCase()) {

                case Constants.TYPE_TEXT:
                    formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), q.getDefault_value__c(), true, InputType.TYPE_CLASS_TEXT, !isEnabled, q.getHelp_Text__c()));

                    break;
                case Constants.TYPE_NUMBER:
                    formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), q.getDefault_value__c(), true, InputType.TYPE_CLASS_NUMBER, !isEnabled, q.getHelp_Text__c()));

                    break;

                case Constants.TYPE_NUMBER_DECIMAL:
                    formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), q.getDefault_value__c(), true, InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL, !isEnabled, q.getHelp_Text__c()));

                    break;

                case Constants.TYPE_SELECTABLE:
                    formSectionController.addElement(new SelectionController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), true, q.getDefault_value__c(), q.formatQuestionOptions(), true, !isEnabled, q.getHelp_Text__c()));

                    break;
                case Constants.TYPE_MULTI_SELECTABLE:
                    formSectionController.addElement(new CheckBoxController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), true, q.formatQuestionOptions(), true, !isEnabled));

                    break;

                case Constants.TYPE_TIMEPICKER:
                    formSectionController.addElement(new TimePickerController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c()));

                    break;
                case Constants.TYPE_DATEPICKER:
                    formSectionController.addElement(new DatePickerController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c()));
                    break;

                case Constants.TYPE_MATH_FORMULA:
                    formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), q.getDefault_value__c(), true, InputType.TYPE_CLASS_TEXT, false, q.getHelp_Text__c()));
                    applyCalculation(databaseHelper.getCalculation(q.getId()));

                    break;

                case Constants.TYPE_LOGIC_FORMULA:
                    formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), q.getDefault_value__c(), true, InputType.TYPE_CLASS_TEXT, false, q.getHelp_Text__c()));
                    break;


                case Constants.TYPE_LOCATION:
                    formSectionController.addElement(new ButtonController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {

                            Log.i(TAG, "^^^^^^^^^^ LOCATION CHANGED ^^^^^^^^^^^^");

                            Log.i(TAG, "lat:" + location.getLatitude() + " lon:" + location.getLongitude());

                            getModel().setValue(q.getId(), location.getLatitude() + ", " + location.getLongitude());


                        }

                        @Override
                        public void onStatusChanged(String s, int i, Bundle bundle) {

                        }

                        @Override
                        public void onProviderEnabled(String s) {
                            Log.i(TAG, "^^^^^^^^^^ PROVIDER ENABLED ^^^^^^^^^^^^");

                        }

                        @Override
                        public void onProviderDisabled(String s) {
                            Log.i(TAG, "^^^^^^^^^^ PROVIDER DISABLED ^^^^^^^^^^^^");


                        }
                    }, !isEnabled));
                    break;


                case Constants.TYPE_PHOTO:
                    formSectionController.addElement(new PhotoButtonController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try{

                                startCameraIntent(q.getId());

                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }, !isEnabled));
                    break;
            }
            }


            setUpSkipLogics(q);

        }


    }

    void loadQuestionsWithValues(Context context, List<Question> questions, MyFormSectionController formSectionController) {

        for (final Question q : questions) {

            if(q.getHide__c().equalsIgnoreCase("false")) {


                if (q.getForm__r().getType().equalsIgnoreCase("monitoring"))
                    isEnabled = false;

                Log.d("MYFORMFRAG ", "TYPE IS " + q.getType__c());

                String storedValue;
                storedValue = getValue(q);
                if (storedValue.equalsIgnoreCase(""))
                    storedValue = q.getDefault_value__c();


                switch (q.getType__c().toLowerCase()) {
                    case Constants.TYPE_TEXT:
                        formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), storedValue, true, InputType.TYPE_CLASS_TEXT, !isEnabled, q.getHelp_Text__c()));
                        //getValue(q);

                        break;
                    case Constants.TYPE_NUMBER:
                        formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), storedValue, true, InputType.TYPE_CLASS_NUMBER, !isEnabled, q.getHelp_Text__c()));
                        //getValue(q);

                        break;

                    case Constants.TYPE_NUMBER_DECIMAL:
                        formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), storedValue, true, InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL, !isEnabled, q.getHelp_Text__c()));
                        //getValue(q);

                        break;

                    case Constants.TYPE_SELECTABLE:
                        formSectionController.addElement(new SelectionController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), true, storedValue, q.formatQuestionOptions(), true, !isEnabled, q.getHelp_Text__c()));
                        //getValue(q);

                        break;

                    case Constants.TYPE_MULTI_SELECTABLE:
                        formSectionController.addElement(new CheckBoxController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), true, q.formatQuestionOptions(), true, !isEnabled));
                        //getValue(q, jsonObject);

                        break;

                    case Constants.TYPE_TIMEPICKER:
                        formSectionController.addElement(new TimePickerController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c()));
                        //getValue(q);

                        break;
                    case Constants.TYPE_DATEPICKER:
                        formSectionController.addElement(new DatePickerController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c()));
                        //getValue(q);

                        break;

                    case Constants.TYPE_MATH_FORMULA:
                        formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), storedValue, true, InputType.TYPE_CLASS_TEXT, false));
                        //getValue(q);
                        applyCalculation(databaseHelper.getCalculation(q.getId()));

                        break;

                    case Constants.TYPE_LOGIC_FORMULA:
                        formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), storedValue, true, InputType.TYPE_CLASS_TEXT, false));
                        //getValue(q);
                        break;


                    case Constants.TYPE_LOCATION:
                        formSectionController.addElement(new ButtonController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), new LocationListener() {
                            @Override
                            public void onLocationChanged(Location location) {

                                Log.i(TAG, "^^^^^^^^^^ LOCATION CHANGED ^^^^^^^^^^^^");

                                Log.i(TAG, "lat:" + location.getLatitude() + " lon:" + location.getLongitude());

                                getModel().setValue(q.getId(), location.getLatitude() + ", " + location.getLongitude());

                            }

                            @Override
                            public void onStatusChanged(String s, int i, Bundle bundle) {


                            }

                            @Override
                            public void onProviderEnabled(String s) {
                                Log.i(TAG, "^^^^^^^^^^ PROVIDER ENABLED ^^^^^^^^^^^^");

                            }

                            @Override
                            public void onProviderDisabled(String s) {
                                Log.i(TAG, "^^^^^^^^^^ PROVIDER DISABLED ^^^^^^^^^^^^");


                            }
                        }, !isEnabled));
                        //getValue(q);
                        break;


                    case Constants.TYPE_PHOTO:
                        formSectionController.addElement(new PhotoButtonController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                try {

                                    startCameraIntent(q.getId());

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }


                            }
                        }, !isEnabled));
                        getValue(q);
                        break;

                }

            }




            setUpSkipLogics(q);

        }

    }

    String getValue(Question q) {

        String defVal = "";
        try {
            defVal = ALL_ANSWERS_JSON_OBJECT.get(q.getId()).toString();
            getModel().setValue(q.getId(), defVal);
            return defVal;

        } catch (JSONException e) {

            Log.i("FORM FRAGMENT", "####### NO STORED VALUE EXCEPTION ##########" + e.getMessage());

            return defVal;
        }

    }




    void setUpSkipLogics(Question q) {


        final List<SkipLogic> skipLogics = databaseHelper.doesQuestionHaveSkipLogics(q.getId());

        if (skipLogics != null && skipLogics.size() > 0) {

            final String caption = q.getCaption__c();

            getModel().addPropertyChangeListener(q.getId(), new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent event) {

                    Log.i("PROPERTY CHANGE ", " FOR QUESTION " + caption + "Value was: " + event.getOldValue() + ", now: " + event.getNewValue());

                    for (SkipLogic sl : skipLogics) {


                        try {
                            if (compareValues(sl, String.valueOf(event.getNewValue()))) {


                                if (sl.getActionToBeTaken().equalsIgnoreCase(Constants.HIDE))
                                    getFormController().getElement(sl.getQuestionShowHide()).getView().setVisibility(View.GONE);

                                else
                                    getFormController().getElement(sl.getQuestionShowHide()).getView().setVisibility(View.VISIBLE);

                            } else {

                                if (sl.getActionToBeTaken().equalsIgnoreCase(Constants.HIDE))
                                    getFormController().getElement(sl.getQuestionShowHide()).getView().setVisibility(View.VISIBLE);
                                else
                                    getFormController().getElement(sl.getQuestionShowHide()).getView().setVisibility(View.GONE);
                            }


                        } catch (Exception ignored) {
                        }


                    }
                }
            });


        }
    }

    void initiateSkipLogicsAndHideViews(List<Question> questions, final MyFormController formController) {

        Log.i(TAG, "QUESTIONS SIZE IS " + questions.size());


        for (Question q : questions) {

            final List<SkipLogic> skipLogics = databaseHelper.doesQuestionHaveSkipLogics(q.getId());

            if (skipLogics != null && skipLogics.size() > 0) {


                final String caption = q.getCaption__c();

                Log.i(TAG, "Size of Skip Logic is " + skipLogics.size());


                for (final SkipLogic sl : skipLogics) {


                    try {

                        if (compareValues(sl, formController.getModel().getValue(sl.getQuestionId()).toString())) {

                            Log.i(TAG, "COMPARING VALUES EVALUATED TO " + true);

                            if (sl.getActionToBeTaken().equalsIgnoreCase(Constants.HIDE)) {

                                formController.getElement(sl.getQuestionShowHide()).getView().animate().alpha(0f).setDuration(500).setListener(new Animator.AnimatorListener() {
                                    @Override
                                    public void onAnimationStart(Animator animator) {

                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animator) {
                                        formController.getElement(sl.getQuestionShowHide()).getView().setVisibility(View.GONE);
                                        formController.getElement(sl.getQuestionShowHide()).getView().setAlpha(1);

                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animator) {

                                    }

                                    @Override
                                    public void onAnimationRepeat(Animator animator) {

                                    }
                                })
                                        .start();


                            } else {
                                formController.getElement(sl.getQuestionShowHide()).getView().animate().alpha(1f).setDuration(500).setListener(new Animator.AnimatorListener() {
                                    @Override
                                    public void onAnimationStart(Animator animator) {

                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animator) {
                                        formController.getElement(sl.getQuestionShowHide()).getView().setVisibility(View.VISIBLE);
                                        formController.getElement(sl.getQuestionShowHide()).getView().clearAnimation();

                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animator) {

                                    }

                                    @Override
                                    public void onAnimationRepeat(Animator animator) {

                                    }
                                })
                                        .start();

                            }

                        } else {

                            Log.i(TAG, "COMPARING VALUES EVALUATED TO " + false);

                            if (sl.getActionToBeTaken().equalsIgnoreCase(Constants.HIDE)) {

                                formController.getElement(sl.getQuestionShowHide()).getView().animate().alpha(1f).setDuration(500)
                                        .setListener(new Animator.AnimatorListener() {
                                            @Override
                                            public void onAnimationStart(Animator animator) {

                                            }

                                            @Override
                                            public void onAnimationEnd(Animator animator) {
                                                formController.getElement(sl.getQuestionShowHide()).getView().setVisibility(View.VISIBLE);
                                                formController.getElement(sl.getQuestionShowHide()).getView().clearAnimation();

                                            }

                                            @Override
                                            public void onAnimationCancel(Animator animator) {

                                            }

                                            @Override
                                            public void onAnimationRepeat(Animator animator) {

                                            }
                                        })
                                        .start();
                            } else {
                                formController.getElement(sl.getQuestionShowHide()).getView().animate().alpha(0f).setDuration(500).setListener(new Animator.AnimatorListener() {
                                    @Override
                                    public void onAnimationStart(Animator animator) {

                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animator) {
                                        formController.getElement(sl.getQuestionShowHide()).getView().setVisibility(View.GONE);
                                        formController.getElement(sl.getQuestionShowHide()).getView().setAlpha(1);

                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animator) {

                                    }

                                    @Override
                                    public void onAnimationRepeat(Animator animator) {

                                    }
                                })
                                        .start();

                            }
                        }


                    } catch (Exception ignored) {
                        ignored.printStackTrace();
                        Log.i(TAG, "INITIALIZING SKIP LOGIC " + ignored.getMessage());

                    }


                }
            }


        }
    }

    void applyCalculation(final Calculation calculation) {
        engine = new ScriptEngineManager().getEngineByName("rhino");


        if (calculation.getQuestion4() != null && !calculation.getQuestion4().equals("null")) {

            System.out.println("***************************** CALCULATION WITH 4 DATA VALUES");

            addPropertyChangeListener(calculation, calculation.getQuestion1());
            addPropertyChangeListener(calculation, calculation.getQuestion2());
            addPropertyChangeListener(calculation, calculation.getQuestion3());
            addPropertyChangeListener(calculation, calculation.getQuestion4());


        } else if (calculation.getQuestion3() != null && !calculation.getQuestion3().equals("null")) {

            System.out.println("***************************** CALCULATION WITH 3 DATA VALUES");

            addPropertyChangeListener(calculation, calculation.getQuestion1());
            addPropertyChangeListener(calculation, calculation.getQuestion2());
            addPropertyChangeListener(calculation, calculation.getQuestion3());


        } else if (calculation.getQuestion2() != null && !calculation.getQuestion2().equals("null")) {

            System.out.println("***************************** CALCULATION WITH 2 DATA VALUES");

            addPropertyChangeListener(calculation, calculation.getQuestion1());
            addPropertyChangeListener(calculation, calculation.getQuestion2());

        }


    }

    void addPropertyChangeListener(final Calculation calculation, String id) {


            getModel().addPropertyChangeListener(id, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

                    String equation = getModel().getValue(calculation.getQuestion1()) + calculation.getOperator1()
                            + getModel().getValue(calculation.getQuestion2()) + calculation.getOperator2()
                            + getModel().getValue(calculation.getQuestion3()) + calculation.getOperator3()
                            + getModel().getValue(calculation.getQuestion4());

                    equation = equation.replace("null", "").replace(",", "");

                    System.out.println("####### PROPERTY CHANGE LISTENER FIRED ------ NO COMPLEX CALC");
                    System.out.println("EQUATION IS " + equation);

                    String newValue = "0.00";
                    try {

                        newValue = calculate(equation);

                        getModel().setValue(calculation.getResultQuestion(), newValue);

                    } catch (ScriptException e) {
                        e.printStackTrace();

                        getModel().setValue(calculation.getResultQuestion(), newValue);

                    }

                    System.out.println("####### NEW VALUE IS " + newValue);

                }
            });



    }

    String calculate(String equation) throws ScriptException {

        Double value = (Double) new ScriptEngineManager().getEngineByName("rhino").eval(equation.trim());
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat formatter = (DecimalFormat) nf;
        formatter.applyPattern("#,###,###.##");


        return (formatter.format(value));
    }

    Boolean compareValues(SkipLogic sl, String newValue) {

        engine = new ScriptEngineManager().getEngineByName("rhino");

        String equation = sl.getAnswerValue() + sl.getLogicalOperator() + String.valueOf(newValue);

        Log.i(TAG, "Equation is " + equation);

        boolean value = false;
        try {
            value = (Boolean) engine.eval(equation);

        } catch (ScriptException | NumberFormatException e) {
            System.out.println("******* EXCEPTION ****** " + e.getMessage());

            value = sl.getAnswerValue().equalsIgnoreCase(newValue);

        } finally {
            System.out.println(equation + " = " + value);
        }
        return value;
    }

    public List<Question> getQuestions() {

        return questions;
    }





}