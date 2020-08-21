package org.grameen.fdp.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.grameen.fdp.R;
import org.grameen.fdp.adapter.HistoricalTableHeaderAdapter;
import org.grameen.fdp.adapter.PlotMonitoringTablePagerAdapter;
import org.grameen.fdp.adapter.PlotMonitoringTableViewAdapter;
import org.grameen.fdp.object.HistoricalTableViewData;
import org.grameen.fdp.object.Monitoring;
import org.grameen.fdp.object.PlotMonitoringTableData;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.object.RealFarmer;
import org.grameen.fdp.object.RealPlot;
import org.grameen.fdp.object.Recommendation;
import org.grameen.fdp.utility.Constants;
import org.grameen.fdp.utility.CustomToast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.codecrafters.tableview.TableView;
import de.codecrafters.tableview.model.TableColumnWeightModel;

/**
 * Created by aangjnr on 08/02/2018.
 */

public class PlotMonitoringActivity extends BaseActivity {


    Boolean hasFamilyMembersData = false;
    RealFarmer farmer;
    Button sync;
    Button editMonitoring;
    Button addMonitoring;
    TextView farmerName;
    TextView farmerCode;
    TextView plotName;
    List<Question> AO_QUESTIONS;
    List<Question> MONITORING_AO_QUESTIONS;
    List<PlotMonitoringTableData> plotMonitoringTableDataList;

    JSONObject AO_JSON_OBJECT;
    PlotMonitoringTablePagerAdapter plotMonitoringTablePagerAdapter;

    String TAG = PlotMonitoringActivity.class.getSimpleName();




    RealPlot PLOT;
    Integer selectedYear;

    TableView generalAOTableView;
    TextView titleTextView;

    Integer SELECTED_MONITORING_ID;

    ViewPager viewPager;
    private List<Monitoring> monitoringList;
    Boolean loadTranslation;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow(); // in Activity's onCreate() for instance
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.activity_plot_monitoring_view);

        sync = findViewById(R.id.sync);
        addMonitoring = findViewById(R.id.add_monitoring);
        titleTextView = findViewById(R.id.title_view);

        editMonitoring = findViewById(R.id.edit_monitoring);
        prefs.edit().putBoolean("refreshViewPager", false).apply();


        farmer = new Gson().fromJson(getIntent().getStringExtra("farmer"), RealFarmer.class);
        PLOT = new Gson().fromJson(getIntent().getStringExtra("plot"), RealPlot.class);
        selectedYear = getIntent().getIntExtra("selectedYear", 0);



        if (farmer != null) {


            loadTranslation = prefs.getBoolean("toggleTranslation", false);


            viewPager = findViewById(R.id.view_pager);
            viewPager.setClipToPadding(false);
            viewPager.setPadding(100, 0, 100, 0);
            viewPager.setPageMargin(40);

            farmerName = findViewById(R.id.name);
            farmerCode = findViewById(R.id.code);
            plotName = findViewById(R.id.plotName);

            farmerName.setText(farmer.getFarmerName());
            farmerCode.setText(farmer.getCode());



            String name_of_plot = PLOT.getName() + " Year " + selectedYear;
            plotName.setText(name_of_plot);



            if(PLOT.getRecommendationId() != null) {

                try {
                    AO_JSON_OBJECT = new JSONObject(PLOT.getPlotInformationJson());
                } catch (JSONException e) {
                    e.printStackTrace();
                    AO_JSON_OBJECT = new JSONObject();

                }

                AO_QUESTIONS = databaseHelper.getSpecificSetOfQuestions(Constants.ADOPTION_OBSERVATIONS);

                MONITORING_AO_QUESTIONS = databaseHelper.getSpecificSetOfQuestions(Constants.AO_MONITORING);

                setGeneralAOTableViewData();



                addMonitoring.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Todo go to add new monitoring
                        Intent intent = new Intent(PlotMonitoringActivity.this, AddNewPlotMonitoringActivity.class);
                        intent.putExtra("isNewMonitoring", true);
                        intent.putExtra("plot", new Gson().toJson(PLOT));
                        intent.putExtra("year", selectedYear);

                        startActivity(intent);


                    }
                });


                editMonitoring.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Intent intent = new Intent(PlotMonitoringActivity.this, AddNewPlotMonitoringActivity.class);
                        intent.putExtra("isNewMonitoring", false);
                        intent.putExtra("plot", new Gson().toJson(PLOT));
                        intent.putExtra("year", selectedYear);
                        intent.putExtra("selectedMonitoring", SELECTED_MONITORING_ID + 1);

                        intent.putExtra("monitoringId", monitoringList.get(SELECTED_MONITORING_ID).getId());
                        startActivity(intent);

                    }
                });


            }else {

                findViewById(R.id.noData).setVisibility(View.VISIBLE);
                sync.setEnabled(false);
                addMonitoring.setEnabled(false);


                addMonitoring.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        showAlertDialog(true, getResources(R.string.incomplete_ao_monitoring), getResources(R.string.incomplete_ao_rational) + PLOT.getName(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                dialogInterface.dismiss();

                            }
                        }, getResources(R.string.ok), null, "", 0);


                    }
                });
            }


            findViewById(R.id.sync).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (farmer.getSyncStatus() != Constants.SYNC_OK) {

                        Intent intent = new Intent(PlotMonitoringActivity.this, SyncUpActivity.class);
                        intent.putExtra("farmer", new Gson().toJson(farmer));
                        startActivity(intent);
                    } else
                        CustomToast.makeToast(PlotMonitoringActivity.this, getResources(R.string.farmer) + " " + farmer.getFarmerName() + getResources(R.string.apostrophe_s) + getResources(R.string.data_already_synced), Toast.LENGTH_LONG).show();


                }
            });


        }

        onBackClicked();


    }





    void setGeneralAOTableViewData(){

        String[] GAPS_PLOT_RECOMMENDATION_IDS = PLOT.getRecommendationId().split(",");
        Recommendation GAPS_RECOMENDATION_FOR_START_YEAR = databaseHelper.getRecommendation(GAPS_PLOT_RECOMMENDATION_IDS[0]);
        Recommendation PLOT_RECOMMENDATION = databaseHelper.getRecommendation(GAPS_PLOT_RECOMMENDATION_IDS[1]);


        String value = (loadTranslation) ? PLOT_RECOMMENDATION.getName() : PLOT_RECOMMENDATION.getTranslation();
        //+ " + " + GAPS_RECOMENDATION_FOR_START_YEAR.getName();


        String[] TABLE_HEADERS = {value, getResources(R.string.diagnostic)};

        generalAOTableView = findViewById(R.id.general_ao_tableView);

/*        TableColumnPxWidthModel columnModel = new TableColumnPxWidthModel(2, 150);
        columnModel.setColumnWidth(0, 180);
        columnModel.setColumnWidth(1, 120);*/
        TableColumnWeightModel columnModel = new TableColumnWeightModel(generalAOTableView.getColumnCount());
        columnModel.setColumnWeight(0, 4);
        columnModel.setColumnWeight(1, 1);
        generalAOTableView.setColumnModel(columnModel);


        HistoricalTableHeaderAdapter headerAdapter = new HistoricalTableHeaderAdapter(PlotMonitoringActivity.this, TABLE_HEADERS);
        generalAOTableView.setHeaderAdapter(headerAdapter);
      /*  headerAdapter.setHeaderClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i(TAG, "Header clicked at " + v.getTag());





            }
        });*/


        List<HistoricalTableViewData> ADOPTION_OBSERVATIONS = new ArrayList<>();
      for(Question q : AO_QUESTIONS){
          //Todo get results
          ADOPTION_OBSERVATIONS.add(new HistoricalTableViewData((loadTranslation) ? q.getTranslation__c() : q.getCaption__c(), getValue(q.getId(), AO_JSON_OBJECT), "--", "--", null));
      }


        PlotMonitoringTableViewAdapter plotMonitoringTableViewAdapter = new PlotMonitoringTableViewAdapter(this, ADOPTION_OBSERVATIONS, generalAOTableView);
        generalAOTableView.setDataAdapter(plotMonitoringTableViewAdapter);


        setUpViewPager();



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

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {


        if(prefs.getBoolean("refreshViewPager", false)){

            Log.i(TAG, "On RESUME");

            if(findViewById(R.id.noData).getVisibility() == View.VISIBLE)
                setUpViewPager();
            else
                updateTableData();


            if(plotMonitoringTablePagerAdapter != null) {
                plotMonitoringTablePagerAdapter.setData(plotMonitoringTableDataList);
                plotMonitoringTablePagerAdapter.notifyDataSetChanged();


                viewPager.setCurrentItem(SELECTED_MONITORING_ID, true);


            }

        }

            }
        }, 300);


    }


    void setUpViewPager() {

        plotMonitoringTableDataList = new ArrayList<>();

        updateTableData();

        plotMonitoringTablePagerAdapter = new PlotMonitoringTablePagerAdapter(this, plotMonitoringTableDataList);


        if (plotMonitoringTableDataList.size() > 0) {
            viewPager.setAdapter(plotMonitoringTablePagerAdapter);


            titleTextView.setText(plotMonitoringTableDataList.get(0).getTitle());


            SELECTED_MONITORING_ID = 0;

            viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {


                    SELECTED_MONITORING_ID = position;
                    titleTextView.setText(plotMonitoringTableDataList.get(position).getTitle());


                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });

            findViewById(R.id.noData).setVisibility(View.GONE);
            editMonitoring.setVisibility(View.VISIBLE);

        } else {
            findViewById(R.id.noData).setVisibility(View.VISIBLE);
            editMonitoring.setVisibility(View.GONE);
        }


    }


    void updateTableData(){

        plotMonitoringTableDataList = new ArrayList<>();

        monitoringList = databaseHelper.getAllPlotMonitoringForYear(PLOT.getId(), String.valueOf(selectedYear));

        for(Monitoring monitoring : monitoringList) {
            List<HistoricalTableViewData> historicalTableViewDataList = new ArrayList<>();

            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(monitoring.getJson());
            } catch (JSONException e) {
                e.printStackTrace();
                jsonObject = new JSONObject();
            }


            for (Question q : MONITORING_AO_QUESTIONS) {

                if (q.getRelated_Questions__c() != null) {
                    String[] ids = q.getRelated_Questions__c().split(",");

                    //Todo get results
                    historicalTableViewDataList.add(new HistoricalTableViewData("", getValue(q.getId(), jsonObject), getValue(databaseHelper.getQuestionId(ids[0]), jsonObject), getValue(databaseHelper.getQuestionId(ids[1]), jsonObject), null));

                }
            }

            String date = "--";

            try {

                String dateValue = getValue(prefs.getString("monitoringDate", ""), jsonObject);
                if(dateValue.length() > 20) {
                    date = dateValue.substring(0, 19);
                    historicalTableViewDataList.add(new HistoricalTableViewData("", "", getResources(R.string.date), date, Constants.TAG_RESULTS));

                }
                else {
                    historicalTableViewDataList.add(new HistoricalTableViewData("", "", getResources(R.string.date), dateValue, Constants.TAG_RESULTS));
                }

            }catch(Exception e){
                e.printStackTrace();

                historicalTableViewDataList.add(new HistoricalTableViewData("", "", getResources(R.string.date), date, Constants.TAG_RESULTS));

            }


            for(Question q : databaseHelper.getSpecificSetOfQuestions(Constants.AO_MONITORING_RESULT)){

                if(q.getType__c().equalsIgnoreCase(Constants.TYPE_LOGIC_FORMULA))
                    historicalTableViewDataList.add(new HistoricalTableViewData("", "", q.getCaption__c(), getValue(q.getId(), jsonObject), Constants.TAG_RESULTS));


            }


            PlotMonitoringTableData p = new PlotMonitoringTableData(monitoring.getName(), historicalTableViewDataList);


            plotMonitoringTableDataList.add(p);

        }
    }


}
