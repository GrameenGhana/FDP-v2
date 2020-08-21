package org.grameen.fdp.activity;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.jaredrummler.materialspinner.MaterialSpinner;

import org.grameen.fdp.R;
import org.grameen.fdp.adapter.ReviewTablePagerAdapter;
import org.grameen.fdp.object.HistoricalTableViewData;
import org.grameen.fdp.object.PlotMonitoringTableData;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.object.RealFarmer;
import org.grameen.fdp.object.RealPlot;
import org.grameen.fdp.utility.Constants;
import org.grameen.fdp.utility.CustomToast;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.codecrafters.tableview.TableView;

/**
 * Created by aangjnr on 08/02/2018.
 */

public class PlotsReviewActivity extends BaseActivity {
    Boolean hasFamilyMembersData = false;
    RealFarmer farmer;
    TextView farmerName;
    TextView farmerCode;
    TextView plotName;
    List<Question> ALL_PLOT_QUESTIONS;
    List<PlotMonitoringTableData> plotMonitoringTableDataList;
    List<RealPlot> PLOTS_LIST;
    JSONObject ALL_FAMRER_ANSWERS;
    ReviewTablePagerAdapter plotMonitoringTablePagerAdapter;

    String TAG = PlotsReviewActivity.class.getSimpleName();
    TableView generalAOTableView;
    ViewPager viewPager;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow(); // in Activity's onCreate() for instance
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.activity_review_page);


        prefs.edit().putBoolean("refreshViewPager", false).apply();
        farmer = databaseHelper.getFarmerBasicInfo(getIntent().getStringExtra("farmerCode"));


        if (farmer != null) {
            ALL_PLOT_QUESTIONS = new ArrayList<>();

            try {
                ALL_FAMRER_ANSWERS = new JSONObject(farmer.getAnswersJson());
            } catch (JSONException e) {
                e.printStackTrace();
                ALL_FAMRER_ANSWERS = new JSONObject();
            }


            ALL_PLOT_QUESTIONS.addAll(databaseHelper.getSpecificSetOfQuestions(Constants.PLOT_INFORMATION));
            ALL_PLOT_QUESTIONS.addAll(databaseHelper.getSpecificSetOfQuestions(Constants.ADOPTION_OBSERVATIONS));
            ALL_PLOT_QUESTIONS.addAll(databaseHelper.getSpecificSetOfQuestions(Constants.AO_MONITORING_RESULT));
            ALL_PLOT_QUESTIONS.addAll(databaseHelper.getSpecificSetOfQuestions(Constants.ADDITIONAL_INTERVENTION));


            viewPager = findViewById(R.id.view_pager);
            viewPager.setClipToPadding(false);
            viewPager.setPadding(0, 0, 130, 0);
            viewPager.setPageMargin(60);

            farmerName = findViewById(R.id.name);
            farmerCode = findViewById(R.id.code);
            plotName = findViewById(R.id.plotName);

            farmerName.setText(farmer.getFarmerName());
            farmerCode.setText(farmer.getCode());


            PLOTS_LIST = databaseHelper.getAllFarmerPlots(farmer.getId());



            setUpViewPager();


            MaterialSpinner labourSpinner = findViewById(R.id.farmer_hire_labour_spinner);
            labourSpinner.setItems("-select-", "Yes", "No");
            labourSpinner.setSelectedIndex(0);
            MaterialSpinner labourType = findViewById(R.id.labour_type_spinner);
            labourType.setItems("-select-", "Full", "Seasonal");
            labourType.setSelectedIndex(0);


            final Question labourQuestion = databaseHelper.getQuestionByTranslation("Labour");
            final Question labourTypeQuestion = databaseHelper.getQuestionByTranslation("Labour type");


            if (labourQuestion != null) {
                if (ALL_FAMRER_ANSWERS.has(labourQuestion.getId()))
                    try {
                        labourSpinner.setText(ALL_FAMRER_ANSWERS.getString(labourQuestion.getId()));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                labourSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {

                        databaseHelper.editAnswerToQuestion(farmer.getId(), labourQuestion.getId(), item.toString());

                    }
                });

            } else
                CustomToast.makeToast(this, "Labour Question is missing. Labour value wont be saved.", Toast.LENGTH_LONG).show();


            if (labourTypeQuestion != null) {
                if (ALL_FAMRER_ANSWERS.has(labourTypeQuestion.getId()))
                    try {
                        labourType.setText(ALL_FAMRER_ANSWERS.getString(labourTypeQuestion.getId()));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                labourType.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {

                        databaseHelper.editAnswerToQuestion(farmer.getId(), labourTypeQuestion.getId(), item.toString());


                    }
                });

            } else
                CustomToast.makeToast(this, "Labour Question is missing. Labour value wont be saved.", Toast.LENGTH_LONG).show();


            if (farmer.getHasSubmitted().equalsIgnoreCase(Constants.YES) && farmer.getSyncStatus() == 1) {

                findViewById(R.id.save).setVisibility(View.GONE);
                labourSpinner.setEnabled(false);
                labourType.setEnabled(false);


            }


        }

        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        onBackClicked();


    }


    void setUpViewPager() {

        plotMonitoringTableDataList = new ArrayList<>();

        updateTableData();

        plotMonitoringTablePagerAdapter = new ReviewTablePagerAdapter(this, plotMonitoringTableDataList);


        if (plotMonitoringTableDataList.size() > 0) {
            viewPager.setAdapter(plotMonitoringTablePagerAdapter);


            //titleTextView.setText("Edit" + plotMonitoringTableDataList.get(0).getTitle());



           /* viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {



                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });*/

            findViewById(R.id.noData).setVisibility(View.GONE);

        } else findViewById(R.id.noData).setVisibility(View.VISIBLE);


    }


    String getValue(String id, JSONObject jsonObject) {

        String defVal = "--";
        try {
            defVal = jsonObject.get(id).toString();
            return defVal;

        } catch (JSONException e) {

            Log.i("FORM FRAGMENT", "####### EXCEPTION ##########" + e.getMessage());

            return defVal;
        }
    }


    @Override
    public void onResume() {
        super.onResume();


    }


    void updateTableData() {

        if (PLOTS_LIST != null && PLOTS_LIST.size() > 0) {

            findViewById(R.id.noData).setVisibility(View.GONE);


            for (RealPlot plot : PLOTS_LIST) {


                List<HistoricalTableViewData> historicalTableViewDataList = new ArrayList<>();

                JSONObject jsonObject;
                try {
                    jsonObject = new JSONObject(plot.getPlotInformationJson());
                } catch (JSONException e) {
                    e.printStackTrace();
                    jsonObject = new JSONObject();
                }


                Question plotSizeQuestion = databaseHelper.getQuestionByTranslation("Plot Size");
                if (plotSizeQuestion != null)
                    historicalTableViewDataList.add(new HistoricalTableViewData((IS_TRANSLATION) ? plotSizeQuestion.getTranslation__c() : plotSizeQuestion.getCaption__c(), getValue(plotSizeQuestion.getId(), jsonObject), "", "", null));


                Question estProd = databaseHelper.getQuestionByTranslation("Estimated production size");
                if (estProd != null)
                    historicalTableViewDataList.add(new HistoricalTableViewData((IS_TRANSLATION) ? estProd.getTranslation__c() : estProd.getCaption__c(), getValue(estProd.getId(), jsonObject), "", "", null));


                Question limeNeeded = databaseHelper.getQuestionByTranslation("Lime need");
                if (limeNeeded != null)
                    historicalTableViewDataList.add(new HistoricalTableViewData((IS_TRANSLATION) ? limeNeeded.getTranslation__c() : limeNeeded.getCaption__c(), getValue(limeNeeded.getId(), jsonObject), "", "", null));


                Question plotRec = databaseHelper.getQuestionByTranslation("Plot recommendation");
                if (plotRec != null) {
                    String recName;
                    try {


                        recName = (IS_TRANSLATION) ? databaseHelper.getRecommendation(getValue(plotRec.getId(), jsonObject)).getName() : databaseHelper.getRecommendation(getValue(plotRec.getId(), jsonObject)).getTranslation();
                    } catch (Exception e) {
                        e.printStackTrace();
                        recName = "--";
                    }

                    historicalTableViewDataList.add(new HistoricalTableViewData((IS_TRANSLATION) ? plotRec.getTranslation__c() : plotRec.getCaption__c(), recName, "", "", null));

                }

                for (Question q : ALL_PLOT_QUESTIONS) {

                    if (!q.getHide__c().equalsIgnoreCase(Constants.HIDE)) {


                        historicalTableViewDataList.add(new HistoricalTableViewData((IS_TRANSLATION) ? q.getTranslation__c() : q.getCaption__c(), getValue(q.getId(), jsonObject), "", "", null));


                    }
                }


                PlotMonitoringTableData p = new PlotMonitoringTableData(plot.getName(), historicalTableViewDataList);


                plotMonitoringTableDataList.add(p);

            }

        } else
            findViewById(R.id.noData).setVisibility(View.VISIBLE);


    }


}
