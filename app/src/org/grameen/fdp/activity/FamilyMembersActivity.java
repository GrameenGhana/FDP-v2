package org.grameen.fdp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.evrencoskun.tableview.TableView;
import com.google.gson.Gson;

import org.grameen.fdp.R;
import org.grameen.fdp.adapter.FineTableViewAdapter;
import org.grameen.fdp.object.Cell;
import org.grameen.fdp.object.ColumnHeader;
import org.grameen.fdp.object.Form;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.object.RealFarmer;
import org.grameen.fdp.object.RowHeader;
import org.grameen.fdp.utility.Callbacks;
import org.grameen.fdp.utility.Constants;
import org.grameen.fdp.utility.CustomToast;
import org.grameen.fdp.utility.TableViewListener;
import org.grameen.fdp.viewholder.CellViewHolder;
import org.grameen.fdp.viewholder.CheckBoxViewHolder;
import org.grameen.fdp.viewholder.SpinnerViewHolder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;


/**
 * Created by aangjnr on 08/02/2018.
 */

public class FamilyMembersActivity extends BaseActivity implements Callbacks.UpdateJsonArray{
    static  int COLUMN_SIZE;
    static int ROW_SIZE = 1;
    private List<RowHeader> mRowHeaderList;
    private List<ColumnHeader> mColumnHeaderList;
    private List<List<Cell>> mCellList;
    JSONObject ALL_ANSWERS_JSON_OBJECT;
    private List<List<Question>> mQuestionsList;
    String formLabel = Constants.FAMILY_MEMBERS;
    TableView tableView;
    Question familyMembersQuestion;
    int SCROLL_POSITION = 3;
    RealFarmer farmer;
    List<Question> INITIALIZED_QUESTIONS;
    Button save;
    JSONArray jsonArray = new JSONArray();
    static JSONArray oldValuesArray ;
    String nameIdInFamilyMembersTable = "";
    private boolean monitoringMode = false;
    private FineTableViewAdapter mTableViewAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_members);

        Intent intent = getIntent();
        type = intent.getStringExtra("type");
        tableView = findViewById(R.id.table_view);

        monitoringMode = Objects.equals(prefs.getString("flag", ""), Constants.MONITORING);

        save = findViewById(R.id.save);
        farmer = new Gson().fromJson(getIntent().getStringExtra("farmer"), RealFarmer.class);
        if(monitoringMode) {
            //In monitoring mode but have a diagnostic type form in monitoring, disable save button
            if (type.equalsIgnoreCase(Constants.FORM_DIAGNOSTIC))
                save.setVisibility(View.GONE);
        }

        nameIdInFamilyMembersTable = databaseHelper.getQuestionIdByTranslationName("Name");
        INITIALIZED_QUESTIONS = databaseHelper.getSpecificSetOfQuestions(Constants.FAMILY_MEMBERS);
        COLUMN_SIZE = INITIALIZED_QUESTIONS.size();

        findViewById(R.id.scrollRight).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SCROLL_POSITION >= COLUMN_SIZE + 1) {
                    tableView.scrollToColumnPosition(COLUMN_SIZE - 1);

                    SCROLL_POSITION = 0;
                } else {
                    tableView.scrollToColumnPosition(SCROLL_POSITION);
                    SCROLL_POSITION += 3;
                }
            }
        });

        findViewById(R.id.scrollLeft).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SCROLL_POSITION > 3) {
                    SCROLL_POSITION -= 3;
                    tableView.scrollToColumnPosition(SCROLL_POSITION);
                } else
                    tableView.scrollToColumnPosition(0);
            }
        });

        try {
            ROW_SIZE = Integer.parseInt(getIntent().getStringExtra("familyMembers"));
        }catch(Exception e){
            e.printStackTrace();
            ROW_SIZE = 1;
        }

        initData();


        for(int i = 0; i < ROW_SIZE; i++){

            JSONObject jsonObject = new JSONObject();
            try {
                jsonArray.put(i, jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        if (farmer != null) {
            setToolbar(getResources(R.string.family_members));

            TextView name = findViewById(R.id.name);
            TextView code = findViewById(R.id.code);

            name.setText(farmer.getFarmerName());
            code.setText(farmer.getCode());
            String familyMembersArray = "[]";

            try {
                familyMembersQuestion = databaseHelper.getQuestionByTranslation("Family members");
                ALL_ANSWERS_JSON_OBJECT = new JSONObject(databaseHelper.getAllAnswersJson(farmer.getId()));
                familyMembersArray = ALL_ANSWERS_JSON_OBJECT.getString(familyMembersQuestion.getId());
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.i("FM ACTIVITY", "FAMILY MEMBERS OLD ARRAY IS " + familyMembersArray);

            try {

                if (familyMembersArray != null && !familyMembersArray.equalsIgnoreCase("null") && !familyMembersArray.equalsIgnoreCase(""))
                    oldValuesArray = new JSONArray(familyMembersArray);
                else
                    oldValuesArray = new JSONArray();

            } catch (JSONException e) {
                e.printStackTrace();
                oldValuesArray = new JSONArray();
            }

            if (oldValuesArray.length() == ROW_SIZE)
                jsonArray = oldValuesArray;

            mTableViewAdapter = new FineTableViewAdapter(this, INITIALIZED_QUESTIONS);
            tableView.setAdapter(mTableViewAdapter);
            tableView.setTableViewListener(new TableViewListener());

            loadData();

            if (!monitoringMode) {
                if (farmer.getHasSubmitted().equalsIgnoreCase(Constants.YES) && farmer.getSyncStatus() == 1) {
                    save.setVisibility(View.GONE);
                }
            }

        }

        onBackClicked();

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    String idForIncome = databaseHelper.getQuestionIdByTranslationName("How much income contribute in a year");
                    if (idForIncome == null || idForIncome.equalsIgnoreCase("null"))
                        idForIncome = databaseHelper.getQuestionIdByTranslationName("How much income contributed in a year from work outside own farm");


                    StringBuilder stringBuilder = new StringBuilder();

                    for (int i = 0; i < ROW_SIZE; i++){

                        String value = "";

                        try {
                            value = jsonArray.getJSONObject(i).get(idForIncome).toString();

                        } catch (JSONException e) {
                            e.printStackTrace();
                            value = "0";
                        }

                        stringBuilder.append(value).append("+");

                    }

                    stringBuilder.append("0");
                    Question q = databaseHelper.getQuestionByTranslation("Total Family Income");
                    String socioJson = databaseHelper.getAllAnswersJson(farmer.getId());

                    JSONObject socioJsonObject = new JSONObject();
                    try {

                        if(socioJson != null && !socioJson.equalsIgnoreCase("null") && !socioJson.equalsIgnoreCase("")) {
                            socioJsonObject = new JSONObject(socioJson);
                            if (socioJsonObject.has(q.getId()))
                                socioJsonObject.remove(q.getId());
                                socioJsonObject.put(q.getId(), applyCalculation(stringBuilder.toString()));
                        }else{

                            socioJsonObject = new JSONObject();
                            socioJsonObject.put(q.getId(), "0.0");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                databaseHelper.editAllAnswersJson(farmer.getId(), socioJsonObject);
                    JSONObject newJson = new JSONObject();
                    try {
                        newJson.put(familyMembersQuestion.getId(), jsonArray);
                    } catch (JSONException | NullPointerException e) {
                        e.printStackTrace();

                        CustomToast.makeToast(FamilyMembersActivity.this, "Family members id supposed to be in Farmer Profile not found!", Toast.LENGTH_LONG).show();
                    }

                if (databaseHelper.editAllAnswersJson(farmer.getId(), newJson)) {
                    CustomToast.makeToast(FamilyMembersActivity.this, getResources(R.string.data_saved), Toast.LENGTH_SHORT).show();
                    loadNextForm();
                    } else
                    CustomToast.makeToast(FamilyMembersActivity.this, getResources(R.string.data_saved), Toast.LENGTH_SHORT).show();

            }
        });

        SpinnerViewHolder.UpdateJsonArrayListener(this);
        CellViewHolder.UpdateJsonArrayListener(this);
        CheckBoxViewHolder.UpdateJsonArrayListener(this);
    }


    private void loadData() {
        List<RowHeader> rowHeaders = getRowHeaderList();
        List<ColumnHeader> columnHeaders = getColumnHeaderList(); //getRandomColumnHeaderList(); //
        List<List<Cell>> cellList =  getCellList(); //getCellListForSortingTest();

        mRowHeaderList.addAll(rowHeaders);
        mColumnHeaderList.addAll(columnHeaders);

        //mCellList.addAll(cellList);
/*
         for (int i = 0; i < ROW_SIZE; i++) {
             mCellList.add(getCells(i));
         }*/


        Log.i(TAG, "########  CELL LIST SIZE IS " + mCellList.size());

        Log.i(TAG, "########  ROW SIZE IS STILL " + ROW_SIZE);

         for (int i = 0; i < ROW_SIZE; i++) {
             Log.i(TAG, "########  GETTING CELLS LIST FOR ROW " + i);

             mCellList.get(i).addAll(cellList.get(i));
        }

        // Load all data


        Log.i(TAG, "HEADERS SIZE IS " + mColumnHeaderList.size());
        Log.i(TAG, "ROWS HEADERS SIZE " + mRowHeaderList.size());
        Log.i(TAG, "DATA ARRAY LIST SIZE IS " + mCellList.size() + " OF ROW SIZE " + ROW_SIZE);


        // Load all data
        mTableViewAdapter.setAllItems(mColumnHeaderList, mRowHeaderList, mCellList);

    }

    private List<List<Cell>> getCellList() {
        List<List<Cell>> list = new ArrayList<>();
        for (int i = 0; i < ROW_SIZE; i++) {

            Log.i(TAG, "ROW INDEX " + i);

            List<Cell> cellList = new ArrayList<>();
            for (int j = 0; j < COLUMN_SIZE; j++) {

                Log.i(TAG, "COLUMN INDEX " + j);

                Question q = mQuestionsList.get(i).get(j);

                //q.setMax_value__c(i + "");

                String value = getValue(i, q.getId());

                if (value != null)
                    q.setDefault_value__c(value);


                Cell cell = new Cell(q.getId(), q);
                cellList.add(cell);
            }
            list.add(cellList);



        }

        Log.i(TAG, "########  CELL LIST SIZE IS " + list.size());

        return list;
    }

    private List<RowHeader> getRowHeaderList() {
        List<RowHeader> list = new ArrayList<>();
        for (int i = 0; i < ROW_SIZE; i++) {

            int rowNumber = i + 1;

            RowHeader header = new RowHeader(String.valueOf(i), String.valueOf(rowNumber));
            list.add(header);
        }

        return list;
    }

    private List<ColumnHeader> getColumnHeaderList() {
        List<ColumnHeader> list = new ArrayList<>();

        for (int i = 0; i < COLUMN_SIZE; i++) {


            if(prefs.getBoolean("toggleTranslation", false)) {
                String title = INITIALIZED_QUESTIONS.get(i).getTranslation__c();
                String helperText;
                if (INITIALIZED_QUESTIONS.get(i).getHelp_Text__c() == null)
                    helperText = "";
                else
                    helperText = INITIALIZED_QUESTIONS.get(i).getHelp_Text__c();


                ColumnHeader header = new ColumnHeader(String.valueOf(i), title, helperText);
                list.add(header);

            }else{

                String helperText;
                if (INITIALIZED_QUESTIONS.get(i).getHelp_Text__c() == null)
                    helperText = "--";
                else
                    helperText = INITIALIZED_QUESTIONS.get(i).getHelp_Text__c();

                     String title = INITIALIZED_QUESTIONS.get(i).getCaption__c();
                ColumnHeader header = new ColumnHeader(String.valueOf(i), title, helperText);
                    list.add(header);


            }


        }

        return list;
    }

    private void initData() {
        mRowHeaderList = new ArrayList<>();
        mColumnHeaderList = new ArrayList<>();
        mCellList = new ArrayList<>();
        mQuestionsList = new ArrayList<>();

        for (int i = 0; i < ROW_SIZE; i++) {
            mCellList.add(new ArrayList<Cell>());
        }

        for (int i = 0; i < ROW_SIZE; i++) {
            mQuestionsList.add(INITIALIZED_QUESTIONS);
        }

        Log.i(TAG, "********* ON INITIALIZATION ......... mCELL LIST SIZE IS " + mCellList.size());


    }

    @Override
    public void onItemValueChanged(int index, String uid, String value) {
        try {
            JSONObject object = jsonArray.getJSONObject(index);
            if(object != null){
                if(object.has(uid)){
                    object.remove(uid);
                    object.put(uid, value);
                    Log.i(TAG, "VALUE UPDATED WITH " + object.get(uid).toString());
                }else
                    object.put(uid, value);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static String getValue(int index, String uid){

        Log.i(TAG, "GETTING OLD VALUE FOE INDEX " + index);

        String value = null;

        try {
             JSONObject object = oldValuesArray.getJSONObject(index);

            if(object.has(uid))
                value = object.getString(uid);

            else value = null;


        } catch (JSONException e) {
            //e.printStackTrace();

            Log.i(TAG, "*** EXCEPTION *** " + e.getMessage());


            value = null;
        }
        Log.i(TAG, "*** VALUE IS  *** " + value);

        return value;
    }


    void loadNextForm() {
        if (IS_MONITIRING_MODE)
            FORMS = databaseHelper.getAllMonitoringForms();

        if (SELECTED_FORM_INDEX < FORMS.size()) {
            for (int i = 0; i < FORMS.size(); i++) {

                if (FORMS.get(i).getName().equalsIgnoreCase(formLabel)) {
                    SELECTED_FORM_INDEX += 1;
                    break;
                }
            }
            if (SELECTED_FORM_INDEX == FORMS.size())

                goToFarmerDetails();

            else {
                Form form = FORMS.get(SELECTED_FORM_INDEX);

                if (IS_MONITIRING_MODE) {

                    if (form.getType().equalsIgnoreCase(Constants.DIAGNOSTIC))
                        goToFarmerDetails();
                    else
                        goToNextForm(form);
                } else
                    formLabel = form.getName();
                goToNextForm(form);
            }
        } else goToFarmerDetails();
    }

    void goToFarmerDetails() {
        SELECTED_FORM_INDEX = 0;

        Intent intent = new Intent(this, FarmerDetailsActivity.class);
        intent.putExtra("farmer", new Gson().toJson(farmer));
        intent.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    void goToNextForm(Form form) {
        Intent intent = new Intent(this, Add_EditFarmerDetailsActivity.class);
        intent.putExtra("farmer", new Gson().toJson(farmer));
        intent.putExtra("flag", "edit");
        intent.putExtra("type", form.getType());
        intent.putExtra("formLabel", form.getName());

        intent.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }
}
