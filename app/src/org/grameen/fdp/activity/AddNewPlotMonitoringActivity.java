package org.grameen.fdp.activity;

import android.animation.Animator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.grameen.fdp.R;
import org.grameen.fdp.fragment.MonitoringFormFragment;
import org.grameen.fdp.fragment.MyFormFragment;
import org.grameen.fdp.object.ComplexCalculation;
import org.grameen.fdp.object.Logic;
import org.grameen.fdp.object.Monitoring;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.object.RealPlot;
import org.grameen.fdp.object.Recommendation;
import org.grameen.fdp.object.SkipLogic;
import org.grameen.fdp.utility.Callbacks;
import org.grameen.fdp.utility.Constants;
import org.grameen.fdp.utility.CustomToast;
import org.grameen.fdp.utility.MyFormController;
import org.json.JSONException;
import org.json.JSONObject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Created by aangjnr on 09/11/2017.
 */

public class AddNewPlotMonitoringActivity extends BaseActivity implements Callbacks.AnItemSelectedListener {

    String TAG = "ADD NEW MONITORING ACTIVITY";
    RealPlot plot;
    MonitoringFormFragment plotMonitoringAOFragment;
    TextView plotName;
    TextView plotSize;
    TextView ph;
    TextView estimatedProductionSize;
    TextView recommendedIntervention;


    ProgressBar recommendationProgress;
    

    private List<Recommendation> recommendations;

    List<Question> aoMonitoringQuestions;

    List <View> ALL_VIEWS_LIST = new ArrayList<>();
    Boolean IS_NEW_MONITORING;
    String selectedYear = "";
    int SELECTED_MONITORING_ID;
    String MONITORIMNG_ID = "";
    private Toolbar toolbar;

    ViewGroup monitoringAOLayout;
    List<View> COMPETENCE_VIEWS = new ArrayList<>();

    JSONObject ALL_MONITORING_VALUES_JSON;
    JSONObject PLOT_ANSWERS_JSON;
    Iterator i1;
    private JSONObject FARMING_ECONOMIC_PROFILE_JSON;
    private Question plotNameQue;
    private Question plotSizeQue;
    private Question estProdQue;
    private Question soilPhQue;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_plot_monitoring);


        try {
            plotNameQue = databaseHelper.getQuestionByTranslation("Plot Name");
            plotSizeQue = databaseHelper.getQuestionByTranslation("Plot Size");

            estProdQue = databaseHelper.getQuestionByTranslation("Estimated production size");
            soilPhQue = databaseHelper.getQuestionByTranslation("Soil PH");

        } catch (Exception e) {
            e.printStackTrace();
            CustomToast.makeToast(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }


        engine = new ScriptEngineManager().getEngineByName("rhino");

        monitoringAOLayout = findViewById(R.id.monitoringAOLayout);
        // monitoringAOLayout.setVisibility(View.INVISIBLE);


        plotName = (TextView) findViewById(R.id.plotName);
        plotSize = (TextView) findViewById(R.id.landSize);
        ph = (TextView) findViewById(R.id.ph);
        estimatedProductionSize = (TextView) findViewById(R.id.estimatedProductionSize);

        recommendedIntervention = findViewById(R.id.recommended_intervention);
        recommendationProgress = findViewById(R.id.recommendationProgress);

        aoMonitoringQuestions = databaseHelper.getSpecificSetOfQuestions(Constants.AO_MONITORING);

        plot = new Gson().fromJson(getIntent().getStringExtra("plot"), RealPlot.class);

        MonitoringFormFragment.setMyItemSelectedListener(this);


        if (plot != null) {
            plotName.setText(plot.getName());

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

            String plotInfo = plot.getPlotInformationJson();

            Log.i(TAG, "PLOT INFO VALUES =  " + plotInfo);

            try {

                String size, estProd;

                PLOT_ANSWERS_JSON = new JSONObject(plotInfo);

                size = PLOT_ANSWERS_JSON.getString(plotSizeQue.getId()) + " " + sizeUnit;
                estProd = PLOT_ANSWERS_JSON.getString(estProdQue.getId()) + " " + estProdUnit;

                plotSize.setText(size);
                estimatedProductionSize.setText(estProd);


            } catch (JSONException e) {
                e.printStackTrace();
                PLOT_ANSWERS_JSON = new JSONObject();


            }


            i1 = PLOT_ANSWERS_JSON.keys();



            String[] GAPS_PLOT_RECOMMENDATION_IDS = plot.getRecommendationId().split(",");

            Recommendation GAPS_RECOMENDATION_FOR_START_YEAR = databaseHelper.getRecommendation(GAPS_PLOT_RECOMMENDATION_IDS[0]);
            Recommendation PLOT_RECOMMENDATION = databaseHelper.getRecommendation(GAPS_PLOT_RECOMMENDATION_IDS[1]);


            String value;

            /*if(!PLOT_RECOMMENDATION.getName().equalsIgnoreCase(GAPS_RECOMENDATION_FOR_START_YEAR.getName()))
             value = PLOT_RECOMMENDATION.getName() + ", " + GAPS_RECOMENDATION_FOR_START_YEAR.getName();
            else*/
                value = PLOT_RECOMMENDATION.getName();


            value = (prefs.getBoolean("toggleTranslation", false)) ? PLOT_RECOMMENDATION.getName() : PLOT_RECOMMENDATION.getTranslation();

            recommendedIntervention.setText(value);

            IS_NEW_MONITORING = getIntent().getBooleanExtra("isNewMonitoring", false);
            selectedYear = String.valueOf(getIntent().getIntExtra("year", -1));

            monitoringAOLayout.setAlpha(0f);





            if(IS_NEW_MONITORING){

                toolbar = setToolbar(getResources(R.string.add_plot_monitoring));

                  SELECTED_MONITORING_ID = databaseHelper.getAllPlotMonitoringForYear(plot.getId(), selectedYear).size() + 1;

                ALL_MONITORING_VALUES_JSON = new JSONObject();


                Question monitoringYearQuestion = databaseHelper.getQuestionByTranslation("Monitoring year");
                try {

                    if (ALL_MONITORING_VALUES_JSON.has(monitoringYearQuestion.getId()))
                        ALL_MONITORING_VALUES_JSON.remove(monitoringYearQuestion.getId());

                    ALL_MONITORING_VALUES_JSON.put(monitoringYearQuestion.getId(), selectedYear);
                } catch (JSONException e) {
                    e.printStackTrace();
                }



           plotMonitoringAOFragment = MonitoringFormFragment.newInstance(false,null, false);



            }else {
                MONITORIMNG_ID = getIntent().getStringExtra("monitoringId");
                SELECTED_MONITORING_ID = getIntent().getIntExtra("selectedMonitoring", -1);
                toolbar = setToolbar(getResources(R.string.edit_plot_monitoring) + " " + SELECTED_MONITORING_ID);



                //Todo get ALL MONITORING JSON OBJECT FROM DB, send to fragment
                Monitoring monitoring = databaseHelper.getPlotMonitoring(MONITORIMNG_ID, plot.getId(), selectedYear);


                try {
                    ALL_MONITORING_VALUES_JSON  = new JSONObject(monitoring.getJson());

                } catch (JSONException e) {
                    e.printStackTrace();
                }


                plotMonitoringAOFragment = MonitoringFormFragment.newInstance(true, ALL_MONITORING_VALUES_JSON.toString(), false);


            }


            String tmp_key;

            while (i1.hasNext()) {
                tmp_key = (String) i1.next();
                try {
                    ALL_MONITORING_VALUES_JSON.put(tmp_key, PLOT_ANSWERS_JSON.get(tmp_key));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("ALL VALUES WITH PLOT ANSWERS \n" + ALL_MONITORING_VALUES_JSON);

            loadDynamicView(plotMonitoringAOFragment, R.id.aosLayout);


            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (final Question q : aoMonitoringQuestions) {
                        q.setOptions__c(q.getOptions__c() + ",Select");

                        addViewsDynamically(q);

                    }

                    monitoringAOLayout.animate().alpha(1f).setInterpolator(new LinearInterpolator())
                            .setListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationStart(Animator animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    if (!IS_NEW_MONITORING)
                                        initiateSkipLogicsAndHideViews(aoMonitoringQuestions);
                                }

                                @Override
                                public void onAnimationCancel(Animator animation) {

                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {

                                }
                            }).start();


                }
            }, 200);

        }


        findViewById(R.id.saveButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(saveData()) {

                    int year = Integer.parseInt(selectedYear);
                    int oldYear = prefs.getInt(plot.getFarmerCode(), -1);

                    if (!Objects.equals(oldYear, year)) {

                        if (year > oldYear) {
                            prefs.edit().putInt(plot.getFarmerCode(), year).apply();
                        }
                    }

                    //Todo go to plot activity, reload vp

                    prefs.edit().putBoolean("refreshViewPager", true).apply();
                    finish();

                }
            }
        });


        onBackClicked();


    }

/*String getAnswerValue(String s) {

        String defVal = "--";

            try {
                defVal = ALL_MONITORING_VALUES_JSON.get(s).toString();

            } catch (JSONException e) {

                Log.i(TAG, e.getMessage());


            }


        return defVal;
    }*/


    void addViewsDynamically(Question q) {


        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.weight = 4;
        labelParams.gravity = Gravity.CENTER;

        View labelView = getViewAtLabel(q);
        linearLayout.addView(labelView, labelParams);

        ALL_VIEWS_LIST.add(labelView);


        LinearLayout.LayoutParams aoParam = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        aoParam.weight = 2;
        aoParam.gravity = Gravity.CENTER;
        aoParam.leftMargin = 10;
        aoParam.rightMargin = 10;

        View AOView = getAOView(q);
        linearLayout.addView(AOView, aoParam);
        ALL_VIEWS_LIST.add(AOView);


        if (q.getRelated_Questions__c() != null) {
            String values[] = q.getRelated_Questions__c().split(",");
            String competenceName = values[0];
            String reasonForFailureName = values[1];


            LinearLayout.LayoutParams competenceParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT);

            competenceParams.weight = 2;
            competenceParams.gravity = Gravity.CENTER;
            competenceParams.leftMargin = 10;
            competenceParams.rightMargin = 10;

            View competenceView = getCompetenceView(databaseHelper.getQuestionByName(competenceName));
            linearLayout.addView(competenceView, competenceParams);

            ALL_VIEWS_LIST.add(competenceView);
            COMPETENCE_VIEWS.add(competenceView);


            LinearLayout.LayoutParams reasonForFailureParam = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            reasonForFailureParam.weight = 3;
            reasonForFailureParam.gravity = Gravity.CENTER;
            reasonForFailureParam.leftMargin = 10;
            reasonForFailureParam.rightMargin = 10;

            View failureView = getReasonForFailureView(databaseHelper.getQuestionByName(reasonForFailureName));
            linearLayout.addView(failureView, reasonForFailureParam);
            ALL_VIEWS_LIST.add(failureView);


            monitoringAOLayout.addView(linearLayout);


        }

    }
    View getViewAtLabel(Question q){
        View view;


        TextView textView = new TextView(this);
        textView.setText((prefs.getBoolean("toggleTranslation", false)) ? q.getTranslation__c() : q.getCaption__c());
        textView.setTextSize(12);
        textView.setTag(q.getId());
        textView.setPadding(10, 10, 10, 10);
        view = textView;
        return view;
    }
    View getAOView(final Question q){

        final View view;

        if(q.getType__c().equalsIgnoreCase(Constants.TYPE_NUMBER)){


            EditText editText = new EditText(this);
            editText.setHint(getValue(q.getId(), ALL_MONITORING_VALUES_JSON));
            editText.setTextSize(12);
            editText.setTag(q.getId());
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.setPadding(10, 10, 10, 10);
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {


                    try {

                        if(ALL_MONITORING_VALUES_JSON.has(q.getId())) {
                            ALL_MONITORING_VALUES_JSON.remove(q.getId());
                            ALL_MONITORING_VALUES_JSON.put(q.getId(), s.toString());

                        }else  ALL_MONITORING_VALUES_JSON.put(q.getId(), s.toString());




                        setUpSkipLogics(q, s.toString());





                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
            view = editText;

        }else {

            final List<String>items = q.formatQuestionOptions();

            final Spinner spinner = new Spinner(this);
            spinner.setPrompt("Select");
            spinner.setTag(q.getId());

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(AddNewPlotMonitoringActivity.this, android.R.layout.simple_spinner_item, items) {
                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);

                    if (position == getCount()) {
                        TextView itemView = ((TextView) view.findViewById(android.R.id.text1));
                        itemView.setText("");
                        itemView.setHint(getItem(getCount()));
                    }
                    return view;
                }

                @Override
                public int getCount() {
                    return super.getCount() - 1; // don't display last item (it's used for the prompt)
                }
            };
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(spinnerAdapter);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {


                    if (pos != items.size() - 1) {
                        try {

                            if (ALL_MONITORING_VALUES_JSON.has(q.getId())) {
                                ALL_MONITORING_VALUES_JSON.remove(q.getId());
                                ALL_MONITORING_VALUES_JSON.put(q.getId(), parent.getSelectedItem().toString());

                            } else
                                ALL_MONITORING_VALUES_JSON.put(q.getId(), parent.getSelectedItem().toString());


                            setUpSkipLogics(q, parent.getSelectedItem().toString());


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Log.i(TAG, "ALL JSON IS " + ALL_MONITORING_VALUES_JSON.toString());
                    }


                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {


                }
            });

            spinner.setSelection(items.size() - 1);
            refresh(spinner, getValue(q.getId(), ALL_MONITORING_VALUES_JSON), items);

            view = spinner;


        }

        return view;
    }
    View getCompetenceView(final Question q){
        View view;
        q.setOptions__c(q.getOptions__c() + ", Select");
        final List<String>items = q.formatQuestionOptions();

        final Spinner spinner = new Spinner(this);
        spinner.setPrompt("Select");
        spinner.setTag(q.getId());

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(AddNewPlotMonitoringActivity.this, android.R.layout.simple_spinner_item, items) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                if (position == getCount()) {
                    TextView itemView = ((TextView) view.findViewById(android.R.id.text1));
                    itemView.setText("");
                    itemView.setHint(getItem(getCount()));
                }
                return view;
            }

            @Override
            public int getCount() {
                return super.getCount() - 1; // don't display last item (it's used for the prompt)
            }
        };
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {


                if (pos != items.size() - 1) {
                    try {

                        if (ALL_MONITORING_VALUES_JSON.has(q.getId())) {
                            ALL_MONITORING_VALUES_JSON.remove(q.getId());
                            ALL_MONITORING_VALUES_JSON.put(q.getId(), parent.getSelectedItem().toString());

                        } else
                            ALL_MONITORING_VALUES_JSON.put(q.getId(), parent.getSelectedItem().toString());

                        setUpSkipLogics(q, parent.getSelectedItem().toString());


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Log.i(TAG, "ALL JSON IS " + ALL_MONITORING_VALUES_JSON.toString());
                }


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {


            }
        });

        spinner.setSelection(items.size() - 1);
        refresh(spinner, getValue(q.getId(), ALL_MONITORING_VALUES_JSON), items);

        view = spinner;

        return view;
    }
    View getReasonForFailureView(final Question q){

        View view;
        q.setOptions__c(q.getOptions__c() + ", Select");

        final List<String>items = q.formatQuestionOptions();

        final Spinner spinner = new Spinner(this);
        spinner.setPrompt("Select");
        spinner.setTag(q.getId());

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(AddNewPlotMonitoringActivity.this, android.R.layout.simple_spinner_item, items) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                if (position == getCount()) {
                    TextView itemView = ((TextView) view.findViewById(android.R.id.text1));
                    itemView.setText("");
                    itemView.setHint(getItem(getCount()));
                }
                return view;
            }

            @Override
            public int getCount() {
                return super.getCount() - 1; // don't display last item (it's used for the prompt)
            }
        };
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {


                if (pos != items.size() - 1) {
                    try {

                        if (ALL_MONITORING_VALUES_JSON.has(q.getId())) {
                            ALL_MONITORING_VALUES_JSON.remove(q.getId());
                            ALL_MONITORING_VALUES_JSON.put(q.getId(), parent.getSelectedItem().toString());

                        } else
                            ALL_MONITORING_VALUES_JSON.put(q.getId(), parent.getSelectedItem().toString());


                        setUpSkipLogics(q, parent.getSelectedItem().toString());



                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Log.i(TAG, "ALL JSON IS " + ALL_MONITORING_VALUES_JSON.toString());
                }



            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {


            }
        });

        spinner.setSelection(items.size() - 1);
        refresh(spinner, getValue(q.getId(), ALL_MONITORING_VALUES_JSON), items);

        view = spinner;

        return view;
    }

    private void refresh(Spinner spinner, @Nullable String defValue, List<String>items) {
         int selectionIndex = items.size() - 1;    // index of last item shows the 'prompt'



        if(defValue != null && !defValue.equals("--") && !defValue.equals("Select"))
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).equals(defValue)) {
                    selectionIndex = i;
                    break;
                }

        }

        spinner.setSelection(selectionIndex);
    }

    public void putDynamicAnswersInMainJson() {

        Log.i(TAG, "&&&&&&&&&&&&&&&   GETTING FARM ASSESSMENT!  &&&&&&&&&&&&&&&&&&");

        for (Question q : plotMonitoringAOFragment.getQuestions()) {

            try {

                if (ALL_MONITORING_VALUES_JSON.has(q.getId()))
                    ALL_MONITORING_VALUES_JSON.remove(q.getId());

                    ALL_MONITORING_VALUES_JSON.put(q.getId(), plotMonitoringAOFragment.getModel().getValue(q.getId()));


            } catch (JSONException e) {
                e.printStackTrace();
            }

        }


        Question monitoringYearQuestion = databaseHelper.getQuestionByTranslation("Monitoring year");
        try {

            if (ALL_MONITORING_VALUES_JSON.has(monitoringYearQuestion.getId()))
                ALL_MONITORING_VALUES_JSON.remove(monitoringYearQuestion.getId());

            ALL_MONITORING_VALUES_JSON.put(monitoringYearQuestion.getId(), selectedYear);
        } catch (JSONException e) {
            e.printStackTrace();
        }






        String valueAfterEvaluation = Constants.NO_MONITORING_PLACE_HOLDER;


        //Apply logic and get monitoring result, insert in json
        List<Question> monitoringResultsQuestions = databaseHelper.getSpecificSetOfQuestions(Constants.AO_MONITORING_RESULT);

        for(Question q : monitoringResultsQuestions){

            if (q.getType__c().equalsIgnoreCase(Constants.TYPE_COMPLEX_CALCULATION)) {

                Log.i(TAG, "&&&&&&&&&&&&&&&   COMPLEX CALCULATION TYPE  &&&&&&&&&&&&&&&&&&");


                ComplexCalculation cc = databaseHelper.getComplexCalculation(q.getId());

                if (cc != null) {
                    valueAfterEvaluation = parseIfFormula(cc.getCondition(), ALL_MONITORING_VALUES_JSON);

                    Log.i(TAG, "COMPLEX CALC " + cc.getName() + " EVALUATED WITH RESULT VALUE " + valueAfterEvaluation);

                    try {

                        if (ALL_MONITORING_VALUES_JSON.has(q.getId()))
                            ALL_MONITORING_VALUES_JSON.remove(q.getId());

                        ALL_MONITORING_VALUES_JSON.put(q.getId(), valueAfterEvaluation);


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }

        }


        for (Question q : monitoringResultsQuestions) {


            if(q.getType__c().equalsIgnoreCase(Constants.TYPE_LOGIC_FORMULA)) {

                Log.i(TAG, "&&&&&&&&&&&&&&&   LOGIC TYPE  &&&&&&&&&&&&&&&&&&");

                List<Logic> logics = databaseHelper.getLogics(q.getId());
                for (Logic logic : logics) {

                    Boolean value = compareAndEvaluateCascadedLogics(logic, ALL_MONITORING_VALUES_JSON);

                    if (value != null && value) {
                        valueAfterEvaluation = logic.getResult();

                        Log.i(TAG, "LOGIC " + logic.getName() + " EVALUATED TO TRUE WITH RESULT VALUE " + valueAfterEvaluation);

                        try {

                            if (ALL_MONITORING_VALUES_JSON.has(q.getId()))
                                ALL_MONITORING_VALUES_JSON.remove(q.getId());

                            ALL_MONITORING_VALUES_JSON.put(q.getId(), valueAfterEvaluation);


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;

                    }
                }

            }

        }


        String tmp_key;

        while (i1.hasNext()) {
            tmp_key = (String) i1.next();
            try {
                if (ALL_MONITORING_VALUES_JSON.has(tmp_key))
                    ALL_MONITORING_VALUES_JSON.remove(tmp_key);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("ALL VALUES WITHOUT PLOT ANSWERS \n" + ALL_MONITORING_VALUES_JSON);


    }

    Boolean saveData(){
        Boolean value = false;

        Log.i(TAG, "TOTAL JSON BEFORE APPLYING LOGIC IS " + ALL_MONITORING_VALUES_JSON.toString());

        putDynamicAnswersInMainJson();

        Log.i(TAG, "TOTAL JSON IS AFTER LOGIC " + ALL_MONITORING_VALUES_JSON.toString());

        if(IS_NEW_MONITORING){

            Log.i(TAG, "^^^^^^^^^^   IS NEW MONITORING  ^^^^^^^^^^^");

            Monitoring monitoring = new Monitoring();
            monitoring.setId(String.valueOf(System.currentTimeMillis()));
            monitoring.setName("Monitoring " + SELECTED_MONITORING_ID);
            monitoring.setPlotId(plot.getId());
            monitoring.setJson(ALL_MONITORING_VALUES_JSON.toString());

            if(databaseHelper.addPlotMonitoring(monitoring, selectedYear)){

                //databaseHelper.editAnswerToQuestion(plot.getFarmerCode(), monitoringYearQuestion.getId(), selectedYear);


                value = true;

                CustomToast.makeToast(this, getResources(R.string.data_saved), Toast.LENGTH_SHORT).show();


            }else CustomToast.makeToast(this, getResources(R.string.data_not_saved), Toast.LENGTH_SHORT).show();


            Log.i(TAG, "^^^^^^^^^^   IS OLD MONITORING  ^^^^^^^^^^^");

        }else{


            if (databaseHelper.editPlotMonitoringJson(MONITORIMNG_ID, plot.getId(), selectedYear, ALL_MONITORING_VALUES_JSON.toString())) {
                value = true;

                CustomToast.makeToast(this, getResources(R.string.data_saved), Toast.LENGTH_SHORT).show();


            }else CustomToast.makeToast(this, getResources(R.string.data_not_saved), Toast.LENGTH_SHORT).show();

        }

        try {
            databaseHelper.setFarmerAsUnSynced(plot.getFarmerCode());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;

    }

    void setUpSkipLogics(Question q, String value) {
        final List<SkipLogic> skipLogics = databaseHelper.doesQuestionHaveSkipLogics(q.getId());

        if (skipLogics != null && skipLogics.size() > 0) {

            Log.i("SKIP LOCIC ", "SIZE FOR QUESTION " + q.getCaption__c() + " = " + skipLogics.size());

            final String caption = q.getCaption__c();
            Boolean skipLogicValue = null;

                    Log.i("PROPERTY CHANGE ", " FOR QUESTION " + caption + "Value is : " + value);

                    for (SkipLogic sl : skipLogics) {


                        try {
                            if (compareValues(sl, String.valueOf(value))) {
                                skipLogicValue = true;


                                if (sl.getActionToBeTaken().equalsIgnoreCase(Constants.HIDE)) {

                                    Log.i("PROPERTY CHANGE ", "Looping... ");

                                    for(int i = 0; i < ALL_VIEWS_LIST.size(); i++){

                                        if(ALL_VIEWS_LIST.get(i).getTag().equals(sl.getQuestionShowHide())) {
                                            ALL_VIEWS_LIST.get(i).setVisibility(View.INVISIBLE);
                                            ALL_VIEWS_LIST.get(i).setEnabled(false);


                                            break;
                                        }
                                    }


                                }

                                else {
                                    for(int i = 0; i < ALL_VIEWS_LIST.size(); i++){

                                        if(ALL_VIEWS_LIST.get(i).getTag().equals(sl.getQuestionShowHide())) {
                                            ALL_VIEWS_LIST.get(i).setVisibility(View.VISIBLE);
                                            ALL_VIEWS_LIST.get(i).setEnabled(true);

                                            break;
                                        }
                                    }

                                }

                            } else {

                                if(skipLogicValue != null && skipLogicValue)
                                    break;


                                for(int i = 0; i < ALL_VIEWS_LIST.size(); i++){

                                    if(ALL_VIEWS_LIST.get(i).getTag().equals(sl.getQuestionShowHide())) {
                                        ALL_VIEWS_LIST.get(i).setVisibility(View.VISIBLE);
                                        ALL_VIEWS_LIST.get(i).setEnabled(true);

                                        break;
                                    }
                                }
                            }


                        } catch (Exception ignored) {
                            //skipLogicValue = false;
                        }

                       /* if(skipLogicValue != null && skipLogicValue)
                            break;*/
                    }


        }
    }

    void initiateSkipLogicsAndHideViews(List<Question> questions) {

        Log.i(TAG, "INITIALIZING SKIP LOGIC >> QUESTIONS SIZE IS " + questions.size());


        for (Question q : questions) {

            final List<SkipLogic> skipLogics = databaseHelper.doesQuestionHaveSkipLogics(q.getId());

            if (skipLogics != null && skipLogics.size() > 0) {


                final String caption = q.getCaption__c();

                Log.i(TAG, "Size of Skip Logic is " + skipLogics.size());


                for (final SkipLogic sl : skipLogics) {


                    try {
                        if (compareValues(sl, getValue(q.getId(), ALL_MONITORING_VALUES_JSON))) {

                            if (sl.getActionToBeTaken().equalsIgnoreCase(Constants.HIDE)) {

                                Log.i("PROPERTY CHANGE ", "Looping... ");

                                for(int i = 0; i < ALL_VIEWS_LIST.size(); i++){

                                    if(ALL_VIEWS_LIST.get(i).getTag().equals(sl.getQuestionShowHide())) {
                                        ALL_VIEWS_LIST.get(i).setVisibility(View.INVISIBLE);
                                        ALL_VIEWS_LIST.get(i).setEnabled(false);
                                        break;
                                    }
                                }
                            } else {
                                for (int i = 0; i < ALL_VIEWS_LIST.size(); i++) {

                                    if (ALL_VIEWS_LIST.get(i).getTag().equals(sl.getQuestionShowHide())) {
                                        ALL_VIEWS_LIST.get(i).setVisibility(View.VISIBLE);
                                        ALL_VIEWS_LIST.get(i).setEnabled(true);

                                        break;
                                    }
                                }
                            }
                        } else {
                            if (sl.getActionToBeTaken().equalsIgnoreCase(Constants.HIDE)) {
                                for(int i = 0; i < ALL_VIEWS_LIST.size(); i++){

                                    if(ALL_VIEWS_LIST.get(i).getTag().equals(sl.getQuestionShowHide())) {
                                        ALL_VIEWS_LIST.get(i).setVisibility(View.VISIBLE);
                                        ALL_VIEWS_LIST.get(i).setEnabled(true);

                                        break;
                                    }
                                }
                            }
                            else {

                                for(int i = 0; i < ALL_VIEWS_LIST.size(); i++){

                                    if(ALL_VIEWS_LIST.get(i).getTag().equals(sl.getQuestionShowHide())) {
                                        ALL_VIEWS_LIST.get(i).setVisibility(View.INVISIBLE);
                                        ALL_VIEWS_LIST.get(i).setEnabled(false);

                                        break;
                                    }
                                }

                            }
                        }


                    } catch (Exception ignored) {
                    }

                }
            }


        }
    }

    @Override
    public void onItemSelected(String item) {
        Log.i(TAG, "*********** &&&&&&&& ***********  SELECTED ITEM IS " + item);

        if(item != null)
            if(item.equalsIgnoreCase("manager")){

                for(int i = 0; i < COMPETENCE_VIEWS.size(); i++){

                        COMPETENCE_VIEWS.get(i).setVisibility(View.VISIBLE);
                        COMPETENCE_VIEWS.get(i).setEnabled(true);

                }


            }else if(item.equalsIgnoreCase("coach")){

                for(int i = 0; i < COMPETENCE_VIEWS.size(); i++){

                    COMPETENCE_VIEWS.get(i).setVisibility(View.INVISIBLE);
                    COMPETENCE_VIEWS.get(i).setEnabled(false);

                }





            }

    }


}
