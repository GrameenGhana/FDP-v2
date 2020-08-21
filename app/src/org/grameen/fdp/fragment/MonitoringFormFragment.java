package org.grameen.fdp.fragment;

import android.animation.Animator;
import android.content.Context;
import android.content.SharedPreferences;
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

import org.grameen.fdp.R;
import org.grameen.fdp.activity.PlotDetailsActivity;
import org.grameen.fdp.object.Calculation;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.object.SkipLogic;
import org.grameen.fdp.utility.ButtonController;
import org.grameen.fdp.utility.Callbacks;
import org.grameen.fdp.utility.CheckBoxController;
import org.grameen.fdp.utility.Constants;
import org.grameen.fdp.utility.CustomToast;
import org.grameen.fdp.utility.DatabaseHelper;
import org.grameen.fdp.utility.DatePickerController;
import org.grameen.fdp.utility.EditTextController;
import org.grameen.fdp.utility.MonitoringCustomController;
import org.grameen.fdp.utility.MonitoringFormSectionController;
import org.grameen.fdp.utility.MyFormController;
import org.grameen.fdp.utility.MyFormSectionController;
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


public class MonitoringFormFragment extends FormFragment {


    List<Question> questions = new ArrayList<>();
    MyFormSectionController formSectionController;
    DatabaseHelper databaseHelper;
    static Callbacks.AnItemSelectedListener anItemSelectedListener;
    boolean shouldLoadOldValues = false;
    boolean isEnabled;
    String TAG = "MYFORMFRAGMENT";
    String VALUES_JSON_STRING = "";
    MyFormSectionController MONITORING_PLOT_INFO_SECTION_CONTROLLER;
    List<Question> monitoringPlotInfoQuestions;

    ScriptEngine engine;


    public MonitoringFormFragment() {

    }

    public static MonitoringFormFragment newInstance( boolean shouldLoadOldValues, String jsonString, boolean disableFormControlller) {


        MonitoringFormFragment formFragment = new MonitoringFormFragment();

        Bundle bundle = new Bundle();
        bundle.putBoolean("loadValues", shouldLoadOldValues);
        bundle.putBoolean("disable", disableFormControlller);
        bundle.putString("json", jsonString);




        formFragment.setArguments(bundle);
        return formFragment;


    }

    @Override
    public void onAttach(Context context) {

        if(getArguments() != null) {
            shouldLoadOldValues = getArguments().getBoolean("loadValues", false);
            isEnabled = getArguments().getBoolean("disable", true);
            VALUES_JSON_STRING = getArguments().getString("json");

        }
        databaseHelper = DatabaseHelper.getInstance(context);


        super.onAttach(context);
    }

    @Override
    public void initForm(MyFormController controller) {

        Context context = getContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());


        MONITORING_PLOT_INFO_SECTION_CONTROLLER = new MyFormSectionController(getContext(), getString(R.string.info_plot_monitoring),
                getString(R.string.info_plot_monitoring));


        // Add new Questions with data model of Question.class
        // The Question data model takes 2 or more parameters based on the type value

        monitoringPlotInfoQuestions = databaseHelper.getSpecificSetOfQuestions("Monitoring Plot Information");

            if (!shouldLoadOldValues) {
                Log.d("MYFORMFRAG", "NO DEFAULT VALUES TO LOAD");


                loadQuestions(context, monitoringPlotInfoQuestions, MONITORING_PLOT_INFO_SECTION_CONTROLLER);




            } else {
                Log.d("MYFORMFRAG", "LOAD DEFAULT VALUES");


                JSONObject jsonObject;

                if (VALUES_JSON_STRING != null && !VALUES_JSON_STRING.isEmpty()) {

                    try {
                        jsonObject = new JSONObject(VALUES_JSON_STRING);

                        loadQuestionsWithValues(context, monitoringPlotInfoQuestions, jsonObject, MONITORING_PLOT_INFO_SECTION_CONTROLLER);


                    } catch (JSONException e) {
                        e.printStackTrace();
                        loadQuestions(context, monitoringPlotInfoQuestions, MONITORING_PLOT_INFO_SECTION_CONTROLLER);


                    }
                } else {
                    loadQuestions(context, monitoringPlotInfoQuestions, MONITORING_PLOT_INFO_SECTION_CONTROLLER);

                }






////////////////////////////////////////////////////////////

              
            }

            //Todo get AO Json Data from DB





        if (MONITORING_PLOT_INFO_SECTION_CONTROLLER != null)
                controller.addSection(MONITORING_PLOT_INFO_SECTION_CONTROLLER);
            

 
////////////////////////////////////////


        questions.addAll(monitoringPlotInfoQuestions);


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
            }, 3000);


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

            if (!q.getHide__c().equalsIgnoreCase("true")) {

                Log.d("MYFORMFRAG ", "TYPE IS " + q.getType__c());
                switch (q.getType__c().toLowerCase()) {

                    case Constants.TYPE_TEXT:
                        formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), q.getDefault_value__c(), true, InputType.TYPE_CLASS_TEXT, !isEnabled));

                        break;
                    case Constants.TYPE_NUMBER:
                        formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), q.getDefault_value__c(), true, InputType.TYPE_CLASS_NUMBER, !isEnabled));

                        break;

                    case Constants.TYPE_NUMBER_DECIMAL:
                        formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), q.getDefault_value__c(), true, InputType.TYPE_NUMBER_FLAG_DECIMAL, !isEnabled));

                        break;

                    case Constants.TYPE_SELECTABLE:
                        formSectionController.addElement(new SelectionController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), true, q.getDefault_value__c(), q.formatQuestionOptions(), true, !isEnabled, q.getHelp_Text__c()));
                        addPropertyChange(q);
                        break;
                    case Constants.TYPE_MULTI_SELECTABLE:
                        formSectionController.addElement(new CheckBoxController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), true, q.formatQuestionOptions(), true, !isEnabled));

                        break;

               /* case Constants.TYPE_CHECKBOX:
                    formSectionController.addElement(new CheckBoxController(context, q.getId(), q.getCaption__c(), true, null, true, !IS_CONTROLLER_ENABLED));

                    break;*/
                    case Constants.TYPE_TIMEPICKER:
                        formSectionController.addElement(new TimePickerController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c()));

                        break;
                    case Constants.TYPE_DATEPICKER:
                        formSectionController.addElement(new DatePickerController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c()));
                        break;

                    case Constants.TYPE_MATH_FORMULA:
                        formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), q.getDefault_value__c(), true, InputType.TYPE_CLASS_TEXT, false));
                        applyCalculation(databaseHelper.getCalculation(q.getId()));

                        break;

                    case Constants.TYPE_LOGIC_FORMULA:
                        formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), q.getDefault_value__c(), true, InputType.TYPE_CLASS_TEXT, false));
                        break;


                }
            }
            //setUpSkipLogics(q);
        }

    }

    private void addPropertyChange(final Question q) {




        getModel().addPropertyChangeListener(q.getId(), new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {

                Log.i("PROPERTY CHANGE ", " FOR QUESTION " + q.getCaption__c() + " Value was: " + event.getOldValue() + ", now: " + event.getNewValue());



                if(q.getTranslation__c().equalsIgnoreCase("Observation By")){
                    if(anItemSelectedListener != null)
                    anItemSelectedListener.onItemSelected(event.getNewValue().toString());
                }

            }
        });








    }

    void loadQuestionsWithValues(Context context, List<Question> questions, JSONObject jsonObject, MyFormSectionController formSectionController) {

        for (final Question q : questions) {

            if(!q.getHide__c().equalsIgnoreCase("true")){

                Log.d("MYFORMFRAG ", "TYPE IS " + q.getType__c());

                String storedValue;
                storedValue =  getValue(q, jsonObject);
                if(storedValue.equalsIgnoreCase(""))
                    storedValue = q.getDefault_value__c();


                switch (q.getType__c().toLowerCase()) {

                case Constants.TYPE_TEXT:
                    formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), storedValue, true, InputType.TYPE_CLASS_TEXT, !isEnabled, q.getHelp_Text__c()));

                    break;
                case Constants.TYPE_NUMBER:
                    formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), storedValue, true, InputType.TYPE_CLASS_NUMBER, !isEnabled));

                    break;

                case Constants.TYPE_NUMBER_DECIMAL:
                    formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), storedValue, true, InputType.TYPE_NUMBER_FLAG_DECIMAL, !isEnabled));

                    break;

                case Constants.TYPE_SELECTABLE:
                    formSectionController.addElement(new SelectionController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), true, storedValue, q.formatQuestionOptions(), true, !isEnabled, q.getHelp_Text__c()));
                     addPropertyChange(q);

                    break;

                case Constants.TYPE_MULTI_SELECTABLE:
                    formSectionController.addElement(new CheckBoxController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), true, q.formatQuestionOptions(), true, !isEnabled));

                    break;
/*
                case Constants.TYPE_CHECKBOX:
                    formSectionController.addElement(new CheckBoxController(context, q.getId(), q.getCaption__c(), true, null, true, !IS_CONTROLLER_ENABLED));
                    getValue(q, jsonObject);

                    break;*/
                case Constants.TYPE_TIMEPICKER:
                    formSectionController.addElement(new TimePickerController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c()));

                    break;
                case Constants.TYPE_DATEPICKER:
                    formSectionController.addElement(new DatePickerController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c()));

                    break;

                case Constants.TYPE_MATH_FORMULA:
                    formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), storedValue, true, InputType.TYPE_CLASS_TEXT, false));
                     applyCalculation(databaseHelper.getCalculation(q.getId()));

                    break;

                case Constants.TYPE_LOGIC_FORMULA:
                    formSectionController.addElement(new EditTextController(context, q.getId(), q.getName(), (preferences.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c(), storedValue, true, InputType.TYPE_CLASS_TEXT, false));
                     break;
            }
            }
        }
    }

    public static void setMyItemSelectedListener(Callbacks.AnItemSelectedListener listener){

         anItemSelectedListener = listener;

    }


    String getValue(Question q, JSONObject jsonObject) {

        String defVal = "";
        try {
            defVal = jsonObject.get(q.getId()).toString();
            getModel().setValue(q.getId(), defVal);
            return defVal;

        } catch (JSONException e) {

            Log.i("FORM FRAGMENT", "####### EXCEPTION ##########" + e.getMessage());

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

            String value;


            if(q.getTranslation__c() != null && q.getTranslation__c().equalsIgnoreCase("Observation By")){

                try {

                    value = getModel().getValue(q.getId()).toString();

                }catch(Exception e){
                    e.printStackTrace();
                    value = "Coach";
                }

                if(value != null && anItemSelectedListener != null)
                    anItemSelectedListener.onItemSelected(value);


            }



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

                System.out.println("####### PROPERTY CHANGE LISTENER FIRED");
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



    List<List<String>> getAllOptions (Question q){

        List<List<String>> optionsList = new ArrayList<>();


        String values[] = q.getRelated_Questions__c().split(",");

        String competenceId = values[0];
        String reasonForFailureId = values[1];


        optionsList.add(q.formatQuestionOptions());

        optionsList.add(databaseHelper.getQuestionByName(competenceId).formatQuestionOptions());
        optionsList.add(databaseHelper.getQuestionByName(reasonForFailureId).formatQuestionOptions());



        return optionsList;

    }



}