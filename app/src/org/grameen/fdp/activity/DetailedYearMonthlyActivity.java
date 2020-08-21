package org.grameen.fdp.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.grameen.fdp.BuildConfig;
import org.grameen.fdp.R;
import org.grameen.fdp.adapter.DetailedYearTableHearderAdapter;
import org.grameen.fdp.adapter.DetailedYearTableViewAdapter;
import org.grameen.fdp.object.Data;
import org.grameen.fdp.object.HistoricalTableViewData;
import org.grameen.fdp.object.RealFarmer;
import org.grameen.fdp.object.RealPlot;
import org.grameen.fdp.object.RecommendationsPlusActivity;
import org.grameen.fdp.utility.CustomToast;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngineManager;

import de.codecrafters.tableview.TableView;

import static org.grameen.fdp.utility.Constants.TAG_OTHER_TEXT_VIEW;
import static org.grameen.fdp.utility.Constants.TAG_TITLE_TEXT_VIEW;

/**
 * Created by aangjnr on 27/01/2018.
 */

public class DetailedYearMonthlyActivity extends BaseActivity {

    String TAG = "DETAILED ACTIVITY";
    RealFarmer farmer;
    TextView farmerName;
    TextView farmerCode;
    TableView tableView;
    DetailedYearTableViewAdapter myTableViewAdapter;
    TextView currency;
    Boolean DID_LABOUR = false;
    String LABOUR_TYPE;
    int year;
    JSONObject PLOT_SIZES_IN_HA = new JSONObject();

    List<RealPlot> plotList;
    List<Data> TABLE_DATA_LIST = new ArrayList<>();

    String CURRENT_SIZE_IN_HA = "1";


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_year);

        engine = new ScriptEngineManager().getEngineByName("rhino");
        currency = findViewById(R.id.currency);
        currency.setText(prefs.getString("currency", ""));


        farmerName = findViewById(R.id.name);
        farmerCode = findViewById(R.id.code);


        farmer = new Gson().fromJson(getIntent().getStringExtra("farmer"), RealFarmer.class);
        year = getIntent().getIntExtra("year", -1);

        Log.i(TAG, "^^^^^^^^ YEAR  ^^^^^^^^^^   " + year);

        DID_LABOUR = getIntent().getBooleanExtra("labour", false);
        LABOUR_TYPE = getIntent().getStringExtra("labourType");

        try {
            PLOT_SIZES_IN_HA = new JSONObject(getIntent().getStringExtra("multiplier"));
        } catch (JSONException e) {
            e.printStackTrace();
        }


        Toolbar toolbar = setToolbar(getResources(R.string.year) + " " + year);

        Log.i(TAG, "^^^^^^^^ LABOUR? " + DID_LABOUR + " TYPE = " + LABOUR_TYPE);


        if (farmer != null) {

            farmerName.setText(farmer.getFarmerName());
            farmerCode.setText(farmer.getCode());


            tableView = findViewById(R.id.tableView);



            tableView.setColumnCount(12);


            String[] TABLE_HEADERS = {getResources(R.string.jan), getResources(R.string.feb), getResources(R.string.mar), getResources(R.string.apr), getResources(R.string.may), getResources(R.string.jun), getResources(R.string.jul),getResources(R.string.aug), getResources(R.string.sep), getResources(R.string.oct), getResources(R.string.nov), getResources(R.string.dec)};
            tableView.setHeaderAdapter(new DetailedYearTableHearderAdapter(DetailedYearMonthlyActivity.this, TABLE_HEADERS));


            plotList = databaseHelper.getAllFarmerPlots(farmer.getId());
            for (RealPlot plot : plotList) {

                try {
                    CURRENT_SIZE_IN_HA = PLOT_SIZES_IN_HA.getString(plot.getId());
                } catch (JSONException e) {
                    e.printStackTrace();
                    CURRENT_SIZE_IN_HA = "1";

                }


                String[] ids = plot.getRecommendationId().split(",");
                String gapsId = ids[0];
                String recommendationId = ids[1];


                int plotYear = plot.getStartYear();
                System.out.println();
                System.out.println("#####################");
                Log.i(TAG, "PLOT YEAR IS " + plotYear);
                Log.i(TAG, "GAPS ID IS " + gapsId);
                Log.i(TAG, "RECOMMENDATION ID IS " + recommendationId);
                System.out.println("#####################");
                System.out.println();


                if (plotYear >= 1) {

                    if (plotYear == 1)
                        getActivitiesSuppliesAndCosts(recommendationId, plot.getName(), year);
                    else {


                        if (year < plotYear)
                            getActivitiesSuppliesAndCosts(gapsId, plot.getName(), 1);
                        else
                            getActivitiesSuppliesAndCosts(recommendationId, plot.getName(), (year - plotYear) + 1);

                    }

                } else {

                    if (year + Math.abs(plotYear) > 7) {

                        getActivitiesSuppliesAndCosts(recommendationId, plot.getName(), 7);

                    } else {

                        getActivitiesSuppliesAndCosts(recommendationId, plot.getName(), year + Math.abs(plotYear));
                    }


                }
            }

            myTableViewAdapter = new DetailedYearTableViewAdapter(DetailedYearMonthlyActivity.this, TABLE_DATA_LIST, tableView);
            tableView.setDataAdapter(myTableViewAdapter);


        }


        onBackClicked();


        if (BuildConfig.DEBUG) {


            findViewById(R.id.print).setVisibility(View.VISIBLE);

            findViewById(R.id.print).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    findViewById(R.id.bottom_buttons).setVisibility(View.GONE);
                    findViewById(R.id.currency_layout).setVisibility(View.GONE);
                    findViewById(R.id.print).setVisibility(View.GONE);


                    progressDialog = showProgress(DetailedYearMonthlyActivity.this, "Initializing", "Please wait...", false);


                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            String fileLocation = captureScreenshot(findViewById(R.id.main_layout), "monthly_activities");

                            progressDialog.dismiss();

                            if (fileLocation != null) {

                                Intent intent = new Intent(DetailedYearMonthlyActivity.this, PrintingActivity.class);
                                intent.putExtra("file_location", fileLocation);
                                startActivity(intent);

                                findViewById(R.id.bottom_buttons).setVisibility(View.VISIBLE);
                                findViewById(R.id.currency_layout).setVisibility(View.VISIBLE);
                                findViewById(R.id.print).setVisibility(View.VISIBLE);


                            } else {
                                CustomToast.makeToast(DetailedYearMonthlyActivity.this, "Error starting the printer service!", Toast.LENGTH_LONG).show();
                            }


                        }
                    }, 2000);

                }
            });


        }











    }


    HistoricalTableViewData getMonthlyData(String id, String month, int year) {

        List<RecommendationsPlusActivity> recommendationsPlusActivities = new ArrayList<>();
        List<RecommendationsPlusActivity> recommendationsPlusActivities2 = new ArrayList<>();

        HistoricalTableViewData data = new HistoricalTableViewData("", "", "");
        try {

           /*

           if(DID_LABOUR)
                if(LABOUR_TYPE.equalsIgnoreCase("seasonal"))
                    recommendationsPlusActivities = databaseHelper.getSeasonalRecommendationPlusAcivityByRecommendationIdMonthAndYear(id, month, year + "", "true");
                else
                    recommendationsPlusActivities = databaseHelper.getRecommendationPlusAcivityByRecommendationIdMonthAndYear(id, month, year + "");
                else

           */

            recommendationsPlusActivities = databaseHelper.getRecommendationPlusAcivityByRecommendationIdMonthAndYear(id, month, year + "");

            recommendationsPlusActivities2 = databaseHelper.getSeasonalRecommendationPlusAcivityByRecommendationIdMonthAndYear(id, month, year + "", "true");


            StringBuilder activities = new StringBuilder() ;
            StringBuilder labourCost = new StringBuilder() ;
            StringBuilder suppliesCost = new StringBuilder();


            for (int i = 0; i < recommendationsPlusActivities.size(); i++) {

                RecommendationsPlusActivity ra = recommendationsPlusActivities.get(i);

                try {

                        if(ra.getActivityName() != null && !ra.getActivityName().equals("null"))
                            if (!activities.toString().toLowerCase().contains(ra.getActivityName().toLowerCase()))
                                activities.append(toCamelCase(ra.getActivityName())).append(", ");

                        suppliesCost.append(ra.getSuppliesCost()).append("+");

                    try {

                        if (DID_LABOUR)
                            if (LABOUR_TYPE.equalsIgnoreCase("seasonal"))
                                labourCost.append(recommendationsPlusActivities2.get(i).getLaborCost()).append("+");
                            else
                                labourCost.append(ra.getLaborCost()).append("+");
                        else
                            labourCost.append("0").append("+");

                    } catch (Exception ignored) {
                        labourCost.append("0").append("+");
                    }

                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            }



            suppliesCost.append("0.0");
            labourCost.append("0.0");

            Log.i(TAG, "CURRENT SIZE IN HA = " + CURRENT_SIZE_IN_HA);


            Log.i(TAG, "Appended Activities are = " + activities.toString());
            Log.i(TAG, "Appended Supplies cost are = " + suppliesCost.toString());
            Log.i(TAG, "Appended labor cost are = " + labourCost.toString());


            data = new HistoricalTableViewData(activities.toString(),
                    applyCalculation("(" + suppliesCost.toString() + ") * " + CURRENT_SIZE_IN_HA),
                    applyCalculation("(" + labourCost.toString() + ") * " + CURRENT_SIZE_IN_HA));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;

    }


    void getActivitiesSuppliesAndCosts(String recommendationId, String plotName, int year) {


        List<HistoricalTableViewData> dataList = new ArrayList<>();


        dataList.add(getMonthlyData(recommendationId, "Jan", year));
        dataList.add(getMonthlyData(recommendationId, "Feb", year));
        dataList.add(getMonthlyData(recommendationId, "Mar", year));
        dataList.add(getMonthlyData(recommendationId, "Apr", year));
        dataList.add(getMonthlyData(recommendationId, "May", year));
        dataList.add(getMonthlyData(recommendationId, "Jun", year));
        dataList.add(getMonthlyData(recommendationId, "Jul", year));
        dataList.add(getMonthlyData(recommendationId, "Aug", year));
        dataList.add(getMonthlyData(recommendationId, "Sep", year));
        dataList.add(getMonthlyData(recommendationId, "Oct", year));
        dataList.add(getMonthlyData(recommendationId, "Nov", year));
        dataList.add(getMonthlyData(recommendationId, "Dec", year));


        List<String> activities = new ArrayList<>();
        List<String> labourCost = new ArrayList<>();
        List<String> suppliesCost = new ArrayList<>();

        for (HistoricalTableViewData data : dataList) {

            activities.add(data.getLabel());
            suppliesCost.add(data.getV1());

            if (DID_LABOUR)
                labourCost.add(data.getV2());


        }


        TABLE_DATA_LIST.add(new Data(plotName, null, TAG_TITLE_TEXT_VIEW));
        // TABLE_DATA_LIST.add(new Data("", null, TAG_OTHER_TEXT_VIEW));


        TABLE_DATA_LIST.add(new Data(getResources(R.string.activities), activities, TAG_OTHER_TEXT_VIEW));
        TABLE_DATA_LIST.add(new Data(getResources(R.string.supplies), suppliesCost, TAG_OTHER_TEXT_VIEW));

        if (DID_LABOUR)
            TABLE_DATA_LIST.add(new Data(getResources(R.string.labour), labourCost, TAG_OTHER_TEXT_VIEW));


    }



}
