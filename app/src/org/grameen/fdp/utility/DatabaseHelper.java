package org.grameen.fdp.utility;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import org.grameen.fdp.BuildConfig;
import org.grameen.fdp.application.FdpApplication;
import org.grameen.fdp.object.ActivitiesPlusInputs;
import org.grameen.fdp.object.Calculation;
import org.grameen.fdp.object.ComplexCalculation;
import org.grameen.fdp.object.Country;
import org.grameen.fdp.object.Form;
import org.grameen.fdp.object.HistoricalData;
import org.grameen.fdp.object.Input;
import org.grameen.fdp.object.LaborDaysLaborCost;
import org.grameen.fdp.object.Logic;
import org.grameen.fdp.object.Monitoring;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.object.RealFarmer;
import org.grameen.fdp.object.RealPlot;
import org.grameen.fdp.object.Recommendation;
import org.grameen.fdp.object.RecommendationsPlusActivity;
import org.grameen.fdp.object.SkipLogic;
import org.grameen.fdp.object.SuppliesCost;
import org.grameen.fdp.object.Village;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static org.grameen.fdp.utility.DatabaseDataClass.*;


/**
 * Created by aangjnr on 24/06/2017.
 */

public class DatabaseHelper extends SQLiteOpenHelper {


    static final String TAG = DatabaseHelper.class.getSimpleName();
    static final String DB_NAME = "fdp.db";
    static final int DB_VERSION = 50;

    private static DatabaseHelper instance;
    Context _context;
    SharedPreferences prefs;
    private SQLiteDatabase db;


    private DatabaseHelper(Context ctx) { //
        super(ctx, DB_NAME, null, DB_VERSION);
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        db = this.getWritableDatabase();
        _context = ctx;


    }


    public int backupDatabase(String oldPath, String newPath) throws IOException {

        // Close the SQLiteOpenHelper so it will commit the created empty
        // database to internal storage.
        close();
        File newDb = new File(newPath);
        File oldDb = new File(oldPath);

        if (!oldDb.exists())
            return 0;


        if (!newDb.exists()) {
            FileOutputStream out = new FileOutputStream(newDb);
            out.write(0);
            out.close();
        }

        if (newDb.exists()) {
            FileUtils.copyFile(new FileInputStream(newDb), new FileOutputStream(oldDb));
            // Access the copied database so SQLiteHelper will cache it and mark
            // it as created.
            getWritableDatabase().close();
            return 1;
        }

        return -1;
    }


    public int exportDB(String oldPath, String newPath) {

        int value;

        File currentDB = new File(oldPath);

        File backupDB = new File(newPath);

        if (!currentDB.exists())
            return 0;

        FileChannel source;
        FileChannel destination;

        try {
            source = new FileInputStream(currentDB).getChannel();
            destination = new FileOutputStream(backupDB).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();

            value = 1;
        } catch (IOException e) {
            e.printStackTrace();
            value = -1;
        }

        return value;
    }









    public static synchronized DatabaseHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new DatabaseHelper(ctx.getApplicationContext());

        }
        return instance;
    }


    public void beginTransaction() {
        db.beginTransaction();
    }

    public void endTransaction(boolean success) {
        if (success) {
            db.setTransactionSuccessful();
        }
        db.endTransaction();
    }


    @Override
    public void onCreate(SQLiteDatabase db) {


        db.execSQL(CREATE_COUNTRIES_TABLE);
        db.execSQL(CREATE_QUESTIONS_TABLE);
        db.execSQL(CREATE_FARMER_TABLE);
        db.execSQL(CREATE_PLOTS_TABLE);
        db.execSQL(CREATE_FORMS_TABLE);
        db.execSQL(CREATE_VILLAGES_TABLE);
        db.execSQL(CREATE_SKIP_LOGIC_TABLE);
        db.execSQL(CREATE_CALCULATIONS_TABLE);
        db.execSQL(CREATE_RECOMMENDATIONS_TABLE);
        db.execSQL(CREATE_LOGIC_TABLE);
        db.execSQL(CREATE_RECOMMENDATION_PLUS_ACTIVITIES_TABLE);
        db.execSQL(CREATE_ACTIVITIES_PLUS_INPUTS_TABLE);
        db.execSQL(CREATE_INPUTS_TABLE);
        db.execSQL(CREATE_MONITORING_TABLE);
        db.execSQL(CREATE_COMPLEX_CALCULATIONS_TABLE);


    }






    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


        //deleteAllTables();
        //onCreate(db);

        if (BuildConfig.DEBUG && (oldVersion < newVersion)) {
            Log.i(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>> SQLITE CREATING HISTORICAL TABLE");
            //db.execSQL(DROP_HISTORICAL_DATA_TABLE);


            db.execSQL(CREATE_HISTORICAL_DATA_TABLE);

        }

    }


    public boolean deleteAllTables() {

        Log.i(TAG, "************* DELETING ALL TABLES! ************");

        try {
            Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

            List<String> tables = new ArrayList<>();

            while (c.moveToNext()) {
                tables.add(c.getString(0));
            }

            for (String table : tables) {

                if (table.startsWith("sqlite_")) {
                    continue;
                }

                String dropQuery = "DROP TABLE IF EXISTS " + table;
                db.execSQL(dropQuery);

                Log.i(TAG, "************* TABLE : " + table);

            }

            c.close();

            onCreate(db);

            return true;
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }


    }

    public boolean addAQuestion(Question question) {
        try {

            ContentValues contentValues = new ContentValues();
            contentValues.put(LAST_MODIFIED_DATE, question.getLastModifiedDate());

            contentValues.put(QUESTION_ID, question.getId());
            contentValues.put(QUESTION_NAME, question.getName());

            contentValues.put(QUESTION_CAPTION, question.getCaption__c());

            try {
                if (question.getDefault_value__c() != null && !question.getDefault_value__c().equalsIgnoreCase("null") && !question.getDefault_value__c().equalsIgnoreCase(""))
                    contentValues.put(QUESTION_DEFAULT_VALUE, question.getDefault_value__c());
                else {
                    if (question.getType__c() != null)

                        if (question.getType__c().equalsIgnoreCase(Constants.TYPE_SELECTABLE))
                            contentValues.put(QUESTION_DEFAULT_VALUE, "-select-");
                        else if (question.getType__c().equalsIgnoreCase(Constants.TYPE_CHECKBOX))
                            contentValues.put(QUESTION_DEFAULT_VALUE, "-choose-");
                        else if (question.getType__c().equalsIgnoreCase(Constants.TYPE_NUMBER))
                            contentValues.put(QUESTION_DEFAULT_VALUE, "0");
                        else if (question.getType__c().equalsIgnoreCase(Constants.TYPE_NUMBER_DECIMAL))
                            contentValues.put(QUESTION_DEFAULT_VALUE, "0.00");
                        else if (question.getType__c().equalsIgnoreCase(Constants.TYPE_TEXT))
                            contentValues.put(QUESTION_DEFAULT_VALUE, "--");

                        else
                            contentValues.put(QUESTION_DEFAULT_VALUE, "");


                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
            contentValues.put(DISPLAY_ORDER, question.getDisplay_Order__c());
            contentValues.put(QUESTION_HIDE, question.getHide__c());

            contentValues.put(QUESTION_ERROR_TEXT, question.getError_text__c());
            contentValues.put(QUESTION_HELPER_TEXT, question.getHelp_Text__c());
            contentValues.put(QUESTION_MAXI_VALUE, question.getMax_value__c());
            contentValues.put(QUESTION_MINI_VALUE, question.getMin_value__c());
            contentValues.put(QUESTION_OPTIONS, question.getOptions__c());
            contentValues.put(QUESTION_TYPE, question.getType__c());
            contentValues.put(QUESTION_RELATED_QUESTIONS, question.getRelated_Questions__c());

            contentValues.put(QUESTION_TRANSLATION, question.getTranslation__c());

            contentValues.put(QUESTION_FORM_NAME, question.getForm__r().getName().toLowerCase());
            contentValues.put(QUESTION_FORM_TYPE, question.getForm__r().getType());


            db.insert(QUESTIONS_TABLE, null, contentValues);

            //Log.d(TAG, "QUESTION WITH ID " + question.getId() + ", NAME " + question.getName() + " AND TRANSLATION " + question.getTranslation__c() + " DISPLAY ORDER "+  question.getDisplay_Order__c() + " SHOULD HIDE ?" + question.getHide__c() +" INSERTED");

            if(question.getTranslation__c() != null) {

                if (question.getTranslation__c().equalsIgnoreCase("Tree age")  && question.getName().startsWith("ao_tree_ age")) {
                    PreferenceManager.getDefaultSharedPreferences(_context).edit().putString("tree age", question.getId()).apply();

                    Log.d(TAG, "\n^^^^^^^^^^^^^^^^^^^^   TREE AGE ID FOUND  ^^^^^^^^^^^^^^^^^^^^^^\n\n");
                }else

                if (question.getTranslation__c().equalsIgnoreCase("Number of household members including the farmer")) {
                    PreferenceManager.getDefaultSharedPreferences(_context).edit().putString("no_family_members_id", question.getId()).apply();

                    Log.d(TAG, "\n^^^^^^^^^^^^^^^^^^^^  NO. FAMILY MEMBERS ID FOUND  ^^^^^^^^^^^^^^^^^^^^^^\n\n");
                }else


                if (question.getTranslation__c().equalsIgnoreCase("Plot Assessment")) {
                    PreferenceManager.getDefaultSharedPreferences(_context).edit().putString("plotResult", question.getId()).apply();

                    Log.d(TAG, "\n^^^^^^^^^^^^^^^^^^^^  PLOT RESULT  ^^^^^^^^^^^^^^^^^^^^^^\n\n");
                }else


                if (question.getTranslation__c().equalsIgnoreCase("Observation By")) {
                    PreferenceManager.getDefaultSharedPreferences(_context).edit().putString("observationBy", question.getId()).apply();

                    Log.d(TAG, "\n^^^^^^^^^^^^^^^^^^^^  Observation By  ^^^^^^^^^^^^^^^^^^^^^^\n\n");
                }else


                if (question.getTranslation__c().equalsIgnoreCase("Soil PH") && question.getName().startsWith("monitoring_plot_ph_")) {
                    PreferenceManager.getDefaultSharedPreferences(_context).edit().putString("monitoringPlotPh", question.getId()).apply();

                    Log.d(TAG, "\n^^^^^^^^^^^^^^^^^^^^  monitoringPlotPh  ^^^^^^^^^^^^^^^^^^^^^^\n\n");
                }else

                if (question.getTranslation__c().equalsIgnoreCase("Date of Monitoring")) {
                    PreferenceManager.getDefaultSharedPreferences(_context).edit().putString("monitoringDate", question.getId()).apply();

                    Log.d(TAG, "\n^^^^^^^^^^^^^^^^^^^^  Date of Monitoring  ^^^^^^^^^^^^^^^^^^^^^^\n\n");
                }
                else
                if (question.getTranslation__c().equalsIgnoreCase("What is your total kg production of dry cocoa beans in a crop year or calendar year?")) {
                    PreferenceManager.getDefaultSharedPreferences(_context).edit().putString("totalProduction", question.getId()).apply();

                    Log.d(TAG, "\n^^^^^^^^^^^^^^^^^^^^  TOTAL PRODUCTION  ^^^^^^^^^^^^^^^^^^^^^^\n\n");
                }
                else
                if (question.getTranslation__c().equalsIgnoreCase("Total land ownership or availability size?")) {
                    PreferenceManager.getDefaultSharedPreferences(_context).edit().putString("totalLandSize", question.getId()).apply();

                    Log.d(TAG, "\n^^^^^^^^^^^^^^^^^^^^  TOTAL LAND SIZE  ^^^^^^^^^^^^^^^^^^^^^^\n\n");
                }
                else
                if (question.getTranslation__c().equalsIgnoreCase("Area units")) {
                    PreferenceManager.getDefaultSharedPreferences(_context).edit().putString("totalAreaUnit", question.getId()).apply();

                    Log.d(TAG, "\n^^^^^^^^^^^^^^^^^^^^  AREA UNIT  ^^^^^^^^^^^^^^^^^^^^^^\n\n");
                }else
                if (question.getTranslation__c().equalsIgnoreCase("Weight units")) {
                    PreferenceManager.getDefaultSharedPreferences(_context).edit().putString("totalWeightUnit", question.getId()).apply();

                    Log.d(TAG, "\n^^^^^^^^^^^^^^^^^^^^  WEIGHTS UNIT  ^^^^^^^^^^^^^^^^^^^^^^\n\n");
                }



            }
            addForm(question.getForm__r());


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }


    public boolean deleteQuestion(String id) {

        return db.delete(QUESTIONS_TABLE, QUESTION_ID + " = ? ", new String[]{id}) > 0;

    }

   /* public boolean deleteQuestionsTable() {

        try {


            db.execSQL("DELETE FROM " + QUESTIONS_TABLE);

            Log.i("DATABASE", "QUESTIONS TABLE DELETED");

        //    deleteFormsTable();

            return true;
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }
    }*/

    public Question getQuestion(String id) {

        Question question = null;
        Cursor cursor = null;
        try {
            question = new Question();


            String selectQuery = "SELECT  * FROM " + QUESTIONS_TABLE + " WHERE " +
                    QUESTION_ID + " ='" + id + "'";
            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {


                        Form form = new Form();
                        form.setType(cursor.getString(cursor.getColumnIndex(QUESTION_FORM_TYPE)));
                        form.setName(cursor.getString(cursor.getColumnIndex(QUESTION_FORM_NAME)));
                        question.setId(cursor.getString(cursor.getColumnIndex(QUESTION_ID)));
                        question.setName(cursor.getString(cursor.getColumnIndex(QUESTION_NAME)));

                        question.setCaption__c(cursor.getString(cursor.getColumnIndex(QUESTION_CAPTION)));
                        question.setDisplay_Order__c(cursor.getDouble(cursor.getColumnIndex(DISPLAY_ORDER)));
                        question.setDefault_value__c(cursor.getString(cursor.getColumnIndex(QUESTION_DEFAULT_VALUE)));
                        question.setHide__c(cursor.getString(cursor.getColumnIndex(QUESTION_HIDE)));


                        question.setError_text__c(cursor.getString(cursor.getColumnIndex(QUESTION_ERROR_TEXT)));
                        question.setHelp_Text__c(cursor.getString(cursor.getColumnIndex(QUESTION_HELPER_TEXT)));
                        question.setMax_value__c(cursor.getString(cursor.getColumnIndex(QUESTION_MAXI_VALUE)));
                        question.setMin_value__c(cursor.getString(cursor.getColumnIndex(QUESTION_MINI_VALUE)));
                        question.setOptions__c(cursor.getString(cursor.getColumnIndex(QUESTION_OPTIONS)));
                        question.setType__c(cursor.getString(cursor.getColumnIndex(QUESTION_TYPE)));
                        question.setRelated_Questions__c(cursor.getString(cursor.getColumnIndex(QUESTION_RELATED_QUESTIONS)));

                        question.setTranslation__c(cursor.getString(cursor.getColumnIndex(QUESTION_TRANSLATION)));

                        question.setForm__r(form);


                    } while (cursor.moveToNext());
            }


        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {

            if (cursor != null)
                cursor.close();
        }

        return question;

    }


    public Question getQuestionByName(String id) {

        Question question = null;
        Cursor cursor = null;
        try {
            question = new Question();


            String selectQuery = "SELECT  * FROM " + QUESTIONS_TABLE + " WHERE " +
                    QUESTION_NAME + " ='" + id + "'";
            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {


                        Form form = new Form();
                        form.setType(cursor.getString(cursor.getColumnIndex(QUESTION_FORM_TYPE)));
                        form.setName(cursor.getString(cursor.getColumnIndex(QUESTION_FORM_NAME)));


                        question.setId(cursor.getString(cursor.getColumnIndex(QUESTION_ID)));
                        question.setName(cursor.getString(cursor.getColumnIndex(QUESTION_NAME)));

                        question.setCaption__c(cursor.getString(cursor.getColumnIndex(QUESTION_CAPTION)));
                        question.setDisplay_Order__c(cursor.getDouble(cursor.getColumnIndex(DISPLAY_ORDER)));
                        question.setDefault_value__c(cursor.getString(cursor.getColumnIndex(QUESTION_DEFAULT_VALUE)));
                        question.setHide__c(cursor.getString(cursor.getColumnIndex(QUESTION_HIDE)));
                        question.setError_text__c(cursor.getString(cursor.getColumnIndex(QUESTION_ERROR_TEXT)));
                        question.setHelp_Text__c(cursor.getString(cursor.getColumnIndex(QUESTION_HELPER_TEXT)));
                        question.setMax_value__c(cursor.getString(cursor.getColumnIndex(QUESTION_MAXI_VALUE)));
                        question.setMin_value__c(cursor.getString(cursor.getColumnIndex(QUESTION_MINI_VALUE)));
                        question.setOptions__c(cursor.getString(cursor.getColumnIndex(QUESTION_OPTIONS)));
                        question.setType__c(cursor.getString(cursor.getColumnIndex(QUESTION_TYPE)));
                        question.setRelated_Questions__c(cursor.getString(cursor.getColumnIndex(QUESTION_RELATED_QUESTIONS)));

                        question.setTranslation__c(cursor.getString(cursor.getColumnIndex(QUESTION_TRANSLATION)));

                        question.setForm__r(form);


                    } while (cursor.moveToNext());

                Log.i("------ QUERY ------", "QUESTION ID FOR " + question.getName() + " IS " + question.getId());

            }


        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {

            if (cursor != null)
                cursor.close();
        }

        return question;

    }



    public Question getQuestionByTranslation(String s) {

        Question question = null;
        Cursor cursor = null;
        try {


            String selectQuery = "SELECT  * FROM " + QUESTIONS_TABLE + " WHERE " +
                    QUESTION_TRANSLATION + " ='" + s + "'";
            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                question = new Question();

                if (cursor.moveToFirst())

                    do {



                        Form form = new Form();
                        form.setType(cursor.getString(cursor.getColumnIndex(QUESTION_FORM_TYPE)));
                        form.setName(cursor.getString(cursor.getColumnIndex(QUESTION_FORM_NAME)));


                        question.setId(cursor.getString(cursor.getColumnIndex(QUESTION_ID)));
                        question.setName(cursor.getString(cursor.getColumnIndex(QUESTION_NAME)));

                        question.setCaption__c(cursor.getString(cursor.getColumnIndex(QUESTION_CAPTION)));
                        question.setDisplay_Order__c(cursor.getDouble(cursor.getColumnIndex(DISPLAY_ORDER)));
                        question.setDefault_value__c(cursor.getString(cursor.getColumnIndex(QUESTION_DEFAULT_VALUE)));
                        question.setHide__c(cursor.getString(cursor.getColumnIndex(QUESTION_HIDE)));
                        question.setError_text__c(cursor.getString(cursor.getColumnIndex(QUESTION_ERROR_TEXT)));
                        question.setHelp_Text__c(cursor.getString(cursor.getColumnIndex(QUESTION_HELPER_TEXT)));
                        question.setMax_value__c(cursor.getString(cursor.getColumnIndex(QUESTION_MAXI_VALUE)));
                        question.setMin_value__c(cursor.getString(cursor.getColumnIndex(QUESTION_MINI_VALUE)));
                        question.setOptions__c(cursor.getString(cursor.getColumnIndex(QUESTION_OPTIONS)));
                        question.setType__c(cursor.getString(cursor.getColumnIndex(QUESTION_TYPE)));
                        question.setRelated_Questions__c(cursor.getString(cursor.getColumnIndex(QUESTION_RELATED_QUESTIONS)));

                        question.setTranslation__c(cursor.getString(cursor.getColumnIndex(QUESTION_TRANSLATION)));

                        question.setForm__r(form);


                    } while (cursor.moveToNext());

                Log.i("------ QUERY ------", "QUESTION ID FOR " + question.getName() + " IS " + question.getId());

            }


        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {

            if (cursor != null)
                cursor.close();
        }

        return question;

    }


    public String getQuestionId(String name) {

        String id = "null";

        Cursor cursor = null;
        try {

            String selectQuery = "SELECT  * FROM " + QUESTIONS_TABLE + " WHERE " +
                    QUESTION_NAME + " ='" + name + "'";
            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        id = cursor.getString(cursor.getColumnIndex(QUESTION_ID));

                    } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            if (cursor != null)
                cursor.close();
        }


        Log.i("------ QUERY ------", "QUESTION ID FOR " + name + " IS " + id);


        return id;

    }

    public String getQuestionIdByTranslationName(String name) {

        String id = "null";

        Cursor cursor = null;
        try {

            String selectQuery = "SELECT  * FROM " + QUESTIONS_TABLE + " WHERE " +
                    QUESTION_TRANSLATION + " ='" + name + "'";
            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        id = cursor.getString(cursor.getColumnIndex(QUESTION_ID));

                    } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            if (cursor != null)
                cursor.close();
        }


        Log.i("------ QUERY ------", "QUESTION ID FOR " + name + " IS " + id);


        return id;

    }


    public String getQuestionIdByTranslationNameAndFormType(String name, String formType) {

        String id = "null";

        Cursor cursor = null;
        try {

            String selectQuery = "SELECT  * FROM " + QUESTIONS_TABLE + " WHERE " +
                    QUESTION_TRANSLATION + " ='" + name + "' AND " + QUESTION_FORM_NAME + " = '" + formType + "'";
            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        id = cursor.getString(cursor.getColumnIndex(QUESTION_ID));

                    } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            if (cursor != null)
                cursor.close();
        }


        Log.i("------ QUERY ------", "QUESTION ID FOR " + name + " IS " + id);


        return id;

    }


    public List<Question> getSpecificSetOfQuestions(String questionFormName) {


        List<Question> questions = new ArrayList<>();
        Cursor cursor = null;

        try {



            String selectQuery = "SELECT  * FROM " + QUESTIONS_TABLE + " WHERE " +
                    QUESTION_FORM_NAME + " ='" + questionFormName.toLowerCase() + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);


            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {


                        Question question = new Question();
                        Form form = new Form();
                        form.setType(cursor.getString(cursor.getColumnIndex(QUESTION_FORM_TYPE)));
                        form.setName(cursor.getString(cursor.getColumnIndex(QUESTION_FORM_NAME)));


                        question.setId(cursor.getString(cursor.getColumnIndex(QUESTION_ID)));
                        question.setName(cursor.getString(cursor.getColumnIndex(QUESTION_NAME)));

                        question.setTranslation__c(cursor.getString(cursor.getColumnIndex(QUESTION_TRANSLATION)));

                        question.setDisplay_Order__c(cursor.getDouble(cursor.getColumnIndex(DISPLAY_ORDER)));
                        question.setDefault_value__c(cursor.getString(cursor.getColumnIndex(QUESTION_DEFAULT_VALUE)));
                        question.setHide__c(cursor.getString(cursor.getColumnIndex(QUESTION_HIDE)));

                        question.setCaption__c(cursor.getString(cursor.getColumnIndex(QUESTION_CAPTION)));
                        question.setError_text__c(cursor.getString(cursor.getColumnIndex(QUESTION_ERROR_TEXT)));
                        question.setHelp_Text__c(cursor.getString(cursor.getColumnIndex(QUESTION_HELPER_TEXT)));
                        question.setMax_value__c(cursor.getString(cursor.getColumnIndex(QUESTION_MAXI_VALUE)));
                        question.setMin_value__c(cursor.getString(cursor.getColumnIndex(QUESTION_MINI_VALUE)));
                        question.setOptions__c(cursor.getString(cursor.getColumnIndex(QUESTION_OPTIONS)));
                        question.setType__c(cursor.getString(cursor.getColumnIndex(QUESTION_TYPE)));
                        question.setRelated_Questions__c(cursor.getString(cursor.getColumnIndex(QUESTION_RELATED_QUESTIONS)));

                        question.setForm__r(form);


                        //Log.i(TAG, "QUESTION : id " + question.getId() + " Name " + question.getName() + " Type " + question.getType__c() + " Caption " + question.getCaption__c() + " Translation " + question.getTranslation__c() + " Related Questions " + question.getRelated_Questions__c() + " Options " + question.getOptions__c() + " Form label " + question.getForm__r().getName());


                        questions.add(question);


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return questions;

        } finally {

            if (cursor != null)
                cursor.close();
        }


        //Sort out questions data by display order

            Collections.sort(questions, new Comparator<Question>() {
                @Override
                public int compare(Question o, Question t1) {

                    Double value1 = 0.00, value2 = 0.00;


                    try {

                        value1 = o.getDisplay_Order__c();
                        value2 = t1.getDisplay_Order__c();

                        Log.i(TAG, "V1 = " + value1 + " AND V2 " + value2);

                        return value1.compareTo(value2);

                    } catch (Exception e) {
                        e.printStackTrace();

                        return 1;

                    }


                }
            });


        Log.i(TAG, "SORTED OUT WITH SIZE " + questions.size());

        return questions;

    }


    public boolean addNewFarmer(RealFarmer realFarmer) {
        try {

            if (realFarmer.getAnswersJson() == null)
                realFarmer.setAnswersJson(new JSONObject().toString());


            if (realFarmer.getHasSubmitted() == null)
                realFarmer.setHasSubmitted(Constants.NO);

            ContentValues contentValues = new ContentValues();
            contentValues.put(FARMER_ID, realFarmer.getId());
            contentValues.put(FARMER_NAME, realFarmer.getFarmerName());
            contentValues.put(FARMER_CODE, realFarmer.getCode());
            contentValues.put(FARMER_VILLAGE, realFarmer.getVillage());
            contentValues.put(FARMER_GENDER, realFarmer.getGender());
            contentValues.put(FARMER_BIRTHYEAR, realFarmer.getBirthYear());
            contentValues.put(FARMER_IMAGE_URL, realFarmer.getImageUrl());
            contentValues.put(ANSWERS_JSON, realFarmer.getAnswersJson());

            contentValues.put(FARMER_EDUCATION, realFarmer.getEducationLevel());

            if(realFarmer.getFirstVisitDate() != null)
            contentValues.put(FARMER_FIRST_VISIT_DATE, realFarmer.getFirstVisitDate());

            contentValues.put(LAST_MODIFIED_DATE, realFarmer.getLastModifiedDate());


            contentValues.put(FARMER_LAST_VISIT_DATE, realFarmer.getLastVisitDate());
            contentValues.put(FARMER_LAND_AREA, realFarmer.getLandArea());
            contentValues.put(FARMER_SYNC_STATUS, realFarmer.getSyncStatus());
            contentValues.put(HAS_REGISTERED, realFarmer.getHasSubmitted());

            db.insert(FARMER_TABLE, null, contentValues);


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    public boolean deleteFarmer(String id) {

        for (RealPlot realPlot : getAllFarmerPlots(id)) {
            deletePlot(realPlot.getId());
            for (Monitoring monitoring : getAllPlotMonitoring(realPlot.getId()))
                deletePlotMonitoring(monitoring.getId());
        }

        return db.delete(FARMER_TABLE, FARMER_ID + " = ? ", new String[]{id}) > 0;

    }


    public boolean setFarmerAsSynced(String id) {
        try {

            ContentValues contentValues = new ContentValues();

            contentValues.put(FARMER_SYNC_STATUS, 1);
            contentValues.put(FARMER_LAST_VISIT_DATE, DateUtil.getFormattedDateMMDDYYYYhhmmaa());


            db.update(FARMER_TABLE, contentValues, FARMER_ID + "= ?", new String[]{id});

            System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");
            Log.d(TAG, "FARMER WITH ID " + id + " SYNC STATUS UPDATED WITH VALUE " + 1);
            System.out.println("\n^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }


    public boolean setFarmerAsUnSynced(String id) {
        try {

            ContentValues contentValues = new ContentValues();

            contentValues.put(FARMER_SYNC_STATUS, 0);


            db.update(FARMER_TABLE, contentValues, FARMER_ID + "= ?", new String[]{id});

            System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");
            Log.d(TAG, "FARMER WITH ID " + id + " SYNC STATUS UPDATED WITH VALUE " + 1);
            System.out.println("\n^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");

            prefs.edit().putBoolean("refreshMainActivity", true).apply();


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }


    public boolean setAgreementSubmitted(String id) {
        try {

            ContentValues contentValues = new ContentValues();

            contentValues.put(HAS_REGISTERED, Constants.YES);


            db.update(FARMER_TABLE, contentValues, FARMER_ID + "= ?", new String[]{id});

            System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");
            Log.d(TAG, "FARMER WITH ID " + id + " FDP STATUS UPDATED WITH VALUE Yes");
            System.out.println("\n^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }


    public boolean setAllFarmersAsSynced() {
        try {

            ContentValues contentValues = new ContentValues();

            contentValues.put(FARMER_SYNC_STATUS, 1);
            contentValues.put(FARMER_LAST_VISIT_DATE, DateUtil.getFormattedDateMMDDYYYYhhmmaa());


            db.update(FARMER_TABLE, contentValues, FARMER_SYNC_STATUS + "= ?", new String[]{String.valueOf(0)});
            Log.d(TAG, "ALL FARMERS SYNC STATUS UPDATED WITH VALUE " + 1);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean setAllFarmersAsUnSynced() {
        try {

            ContentValues contentValues = new ContentValues();

            contentValues.put(FARMER_SYNC_STATUS, 0);

            db.update(FARMER_TABLE, contentValues, FARMER_SYNC_STATUS + "= ?", new String[]{String.valueOf(0)});
            Log.d(TAG, "ALL FARMERS SYNC STATUS UPDATED WITH VALUE " + 0);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }



    public List<RealFarmer> getAllFarmersBasicInfoAccordingToVillage(String villageName) {

        List<RealFarmer> realFarmers = null;
        Cursor cursor = null;

        try {

            realFarmers = new ArrayList<>();

            String selectQuery = "SELECT  * FROM " + FARMER_TABLE + " WHERE " +
                    FARMER_VILLAGE + " ='" + villageName + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {


                        RealFarmer realFarmer = new RealFarmer();

                        realFarmer.set_ID(cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)));
                        realFarmer.setId(cursor.getString(cursor.getColumnIndex(FARMER_ID)));
                        realFarmer.setFarmerName(cursor.getString(cursor.getColumnIndex(FARMER_NAME)));
                        realFarmer.setCode(cursor.getString(cursor.getColumnIndex(FARMER_CODE)));
                        realFarmer.setVillage(cursor.getString(cursor.getColumnIndex(FARMER_VILLAGE)));
                        realFarmer.setGender(cursor.getString(cursor.getColumnIndex(FARMER_GENDER)));
                        realFarmer.setBirthYear(cursor.getString(cursor.getColumnIndex(FARMER_BIRTHYEAR)));
                        realFarmer.setEducationLevel(cursor.getString(cursor.getColumnIndex(FARMER_EDUCATION)));
                        realFarmer.setFirstVisitDate(cursor.getString(cursor.getColumnIndex(FARMER_FIRST_VISIT_DATE)));
                        realFarmer.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));

                        realFarmer.setLastVisitDate(cursor.getString(cursor.getColumnIndex(FARMER_LAST_VISIT_DATE)));
                        realFarmer.setLandArea(cursor.getString(cursor.getColumnIndex(FARMER_LAND_AREA)));
                        realFarmer.setImageUrl(cursor.getString(cursor.getColumnIndex(FARMER_IMAGE_URL)));
                        realFarmer.setHasSubmitted(cursor.getString(cursor.getColumnIndex(HAS_REGISTERED)));
                        realFarmer.setSyncStatus(cursor.getInt(cursor.getColumnIndex(FARMER_SYNC_STATUS)));

                        Log.i(TAG, "RealFarmer found with CODE " + realFarmer.getCode());

                        realFarmers.add(realFarmer);


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }
        return sortFarmersByNameInAscendingOrder(realFarmers);


    }


    public List<RealFarmer> getAllFarmers() {

        List<RealFarmer> realFarmers = null;
        Cursor cursor = null;

        try {

            realFarmers = new ArrayList<>();

            String selectQuery = "SELECT  * FROM " + FARMER_TABLE;

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {


                        RealFarmer realFarmer = new RealFarmer();

                        realFarmer.setId(cursor.getString(cursor.getColumnIndex(FARMER_ID)));
                        realFarmer.setFarmerName(cursor.getString(cursor.getColumnIndex(FARMER_NAME)));
                        realFarmer.setCode(cursor.getString(cursor.getColumnIndex(FARMER_CODE)));
                        realFarmer.setVillage(cursor.getString(cursor.getColumnIndex(FARMER_VILLAGE)));
                        realFarmer.setGender(cursor.getString(cursor.getColumnIndex(FARMER_GENDER)));
                        realFarmer.setBirthYear(cursor.getString(cursor.getColumnIndex(FARMER_BIRTHYEAR)));
                        realFarmer.setEducationLevel(cursor.getString(cursor.getColumnIndex(FARMER_EDUCATION)));
                        realFarmer.setFirstVisitDate(cursor.getString(cursor.getColumnIndex(FARMER_FIRST_VISIT_DATE)));
                        realFarmer.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));

                        realFarmer.setAnswersJson(cursor.getString(cursor.getColumnIndex(ANSWERS_JSON)));
                        realFarmer.setLastVisitDate(cursor.getString(cursor.getColumnIndex(FARMER_LAST_VISIT_DATE)));
                        realFarmer.setLandArea(cursor.getString(cursor.getColumnIndex(FARMER_LAND_AREA)));
                        realFarmer.setImageUrl(cursor.getString(cursor.getColumnIndex(FARMER_IMAGE_URL)));
                        realFarmer.setHasSubmitted(cursor.getString(cursor.getColumnIndex(HAS_REGISTERED)));
                        realFarmer.setSyncStatus(cursor.getInt(cursor.getColumnIndex(FARMER_SYNC_STATUS)));

                        //Log.i(TAG, "RealFarmer found with CODE " + realFarmer.getCode());

                        realFarmers.add(realFarmer);


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return realFarmers;


    }


    public List<RealFarmer> getAllUnsyncedFarmers() {

        List<RealFarmer> realFarmers = null;
        Cursor cursor = null;

        try {

            realFarmers = new ArrayList<>();

            String selectQuery = "SELECT  * FROM " + FARMER_TABLE + " WHERE " + FARMER_SYNC_STATUS + " = '" + 0 + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {


                        RealFarmer realFarmer = new RealFarmer();

                        realFarmer.setId(cursor.getString(cursor.getColumnIndex(FARMER_ID)));
                        realFarmer.setFarmerName(cursor.getString(cursor.getColumnIndex(FARMER_NAME)));
                        realFarmer.setCode(cursor.getString(cursor.getColumnIndex(FARMER_CODE)));
                        realFarmer.setVillage(cursor.getString(cursor.getColumnIndex(FARMER_VILLAGE)));
                        realFarmer.setGender(cursor.getString(cursor.getColumnIndex(FARMER_GENDER)));
                        realFarmer.setBirthYear(cursor.getString(cursor.getColumnIndex(FARMER_BIRTHYEAR)));
                        realFarmer.setEducationLevel(cursor.getString(cursor.getColumnIndex(FARMER_EDUCATION)));
                        realFarmer.setFirstVisitDate(cursor.getString(cursor.getColumnIndex(FARMER_FIRST_VISIT_DATE)));
                        realFarmer.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));

                        realFarmer.setAnswersJson(cursor.getString(cursor.getColumnIndex(ANSWERS_JSON)));
                        realFarmer.setLastVisitDate(cursor.getString(cursor.getColumnIndex(FARMER_LAST_VISIT_DATE)));
                        realFarmer.setLandArea(cursor.getString(cursor.getColumnIndex(FARMER_LAND_AREA)));
                        realFarmer.setImageUrl(cursor.getString(cursor.getColumnIndex(FARMER_IMAGE_URL)));
                        realFarmer.setHasSubmitted(cursor.getString(cursor.getColumnIndex(HAS_REGISTERED)));
                        realFarmer.setSyncStatus(cursor.getInt(cursor.getColumnIndex(FARMER_SYNC_STATUS)));

                        //Log.i(TAG, "RealFarmer found with CODE " + realFarmer.getCode());

                        realFarmers.add(realFarmer);


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return realFarmers;


    }


    public RealFarmer getFarmerBasicInfo(String id) {

        Cursor cursor = null;
        RealFarmer realFarmer = null;

        try {

            String selectQuery = "SELECT  * FROM " + FARMER_TABLE + " WHERE " +
                    FARMER_ID + " ='" + id + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        realFarmer = new RealFarmer();

                        realFarmer.setId(cursor.getString(cursor.getColumnIndex(FARMER_ID)));
                        realFarmer.setFarmerName(cursor.getString(cursor.getColumnIndex(FARMER_NAME)));
                        realFarmer.setCode(cursor.getString(cursor.getColumnIndex(FARMER_CODE)));
                        realFarmer.setVillage(cursor.getString(cursor.getColumnIndex(FARMER_VILLAGE)));
                        realFarmer.setGender(cursor.getString(cursor.getColumnIndex(FARMER_GENDER)));
                        realFarmer.setBirthYear(cursor.getString(cursor.getColumnIndex(FARMER_BIRTHYEAR)));
                        realFarmer.setEducationLevel(cursor.getString(cursor.getColumnIndex(FARMER_EDUCATION)));
                        realFarmer.setFirstVisitDate(cursor.getString(cursor.getColumnIndex(FARMER_FIRST_VISIT_DATE)));
                        realFarmer.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));
                        realFarmer.setAnswersJson(cursor.getString(cursor.getColumnIndex(ANSWERS_JSON)));

                        realFarmer.setLastVisitDate(cursor.getString(cursor.getColumnIndex(FARMER_LAST_VISIT_DATE)));
                        realFarmer.setLandArea(cursor.getString(cursor.getColumnIndex(FARMER_LAND_AREA)));
                        realFarmer.setImageUrl(cursor.getString(cursor.getColumnIndex(FARMER_IMAGE_URL)));

                        realFarmer.setHasSubmitted(cursor.getString(cursor.getColumnIndex(HAS_REGISTERED)));
                        realFarmer.setSyncStatus(cursor.getInt(cursor.getColumnIndex(FARMER_SYNC_STATUS)));



                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return realFarmer;


    }


    public boolean editFarmerBasicInfo(RealFarmer realFarmer) {

        try {

            ContentValues contentValues = new ContentValues();
            contentValues.put(FARMER_ID, realFarmer.getId());
            contentValues.put(FARMER_NAME, realFarmer.getFarmerName());
            contentValues.put(FARMER_CODE, realFarmer.getCode());
            contentValues.put(FARMER_VILLAGE, realFarmer.getVillage());
            contentValues.put(FARMER_GENDER, realFarmer.getGender());
            contentValues.put(FARMER_BIRTHYEAR, realFarmer.getBirthYear());
            contentValues.put(FARMER_IMAGE_URL, realFarmer.getImageUrl());
            contentValues.put(HAS_REGISTERED, realFarmer.getHasSubmitted());


            contentValues.put(FARMER_EDUCATION, realFarmer.getEducationLevel());
            contentValues.put(FARMER_LAST_VISIT_DATE, realFarmer.getLastVisitDate());
            contentValues.put(LAST_MODIFIED_DATE, realFarmer.getLastModifiedDate());
            contentValues.put(FARMER_LAND_AREA, realFarmer.getLandArea());
            contentValues.put(FARMER_SYNC_STATUS, realFarmer.getSyncStatus());


            db.update(FARMER_TABLE, contentValues, FARMER_ID + "= ?", new String[]{realFarmer.getId()});



        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;


    }


    public Boolean editAllAnswersJson(String id, JSONObject newJson) {


        try {
            JSONObject jsonObject;

            String jsonStringValue = getAllAnswersJson(id);
            if(jsonStringValue != null) {

                jsonObject = new JSONObject(jsonStringValue);

            }else jsonObject = new JSONObject();


            try {

                Iterator i1 = newJson.keys();
                while (i1.hasNext()) {
                    String tmp_key = (String) i1.next();

                    if (jsonObject.has(tmp_key))
                        jsonObject.remove(tmp_key);

                        jsonObject.put(tmp_key, newJson.getString(tmp_key));


                }
                Log.d("P & L ACTIVITY", "ADDING TO MAIN JSON OBJECT");

            } catch (JSONException e) {
                e.printStackTrace();
                Log.d("P & L ACTIVITY", "####### JSON ERROR" + e.getMessage());

            }

            ContentValues contentValues = new ContentValues();
            contentValues.put(ANSWERS_JSON, jsonObject.toString());
            db.update(FARMER_TABLE, contentValues, FARMER_ID + "= ?", new String[]{id});
            Log.d(TAG, "DATA\t" + jsonObject.toString() + "ADDED\n FOR FARMER WITH id " + id);


            setFarmerAsUnSynced(id);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;


    }

    public Boolean editAnswerToQuestion(String farmerId, String id, String newValue) {

        try {
            JSONObject jsonObject;

            String jsonStringValue = getAllAnswersJson(farmerId);
            if(jsonStringValue != null) {

                jsonObject = new JSONObject(jsonStringValue);

            }else jsonObject = new JSONObject();

            try {
                if (jsonObject.has(id))
                        jsonObject.remove(id);


                jsonObject.put(id, newValue);

                Log.d("DB", "ADDING " + newValue + " TO MAIN JSON OBJECT");

            } catch (JSONException e) {
                e.printStackTrace();
                Log.d("DB", "####### JSON ERROR" + e.getMessage());

            }

            ContentValues contentValues = new ContentValues();
            contentValues.put(ANSWERS_JSON, jsonObject.toString());
            db.update(FARMER_TABLE, contentValues, FARMER_ID + "= ?", new String[]{farmerId});
            //Log.d(TAG, "DATA\t" + jsonObject.toString() + "ADDED\n FOR FARMER WITH ID " + farmerId);

            setFarmerAsUnSynced(farmerId);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public String getAllAnswersJson(String id) {

        Cursor cursor = null;
        String data = "";

        try {
            String selectQuery = "SELECT  * FROM " + FARMER_TABLE + " WHERE " +
                    FARMER_ID + " ='" + id + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {

                        data = cursor.getString(cursor.getColumnIndex(ANSWERS_JSON));

                        Log.i(TAG, "DATA VALUE IS " + data);

                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return data;


    }

    public boolean editFarmerPlotAnswers(RealPlot plot) {


        try {

            ContentValues contentValues = new ContentValues();


            contentValues.put(PLOT_NAME, plot.getName());
            contentValues.put(PLOT_ANSWERS_JSON, plot.getPlotInformationJson());


            db.update(PLOTS_TABLE, contentValues, FARMER_CODE + "  = ? AND " + PLOT_ID + " = ?", new String[]{plot.getFarmerCode(), plot.getId()});


            Log.d(TAG, "DATA\n" + plot.getPlotInformationJson() + " \n ADDED FOR FARMER WITH CODE " + plot.getFarmerCode() + " AND ID " + plot.getId());

            setFarmerAsUnSynced(plot.getFarmerCode());

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;


    }

    public boolean editFarmerPlotRecommendationId(String farmerCode, String plotId, String gapsRecId_plotRecId) {

        //Todo plot recommedndation

        Question plotRecommendation = getQuestionByTranslation("Plot recommendation");

        if (plotRecommendation != null)
            editPlotAORJson(plotId, plotRecommendation.getId(), gapsRecId_plotRecId.split(",")[1]);


        try {

            ContentValues contentValues = new ContentValues();


            contentValues.put(RECOMMENDATION_ID, gapsRecId_plotRecId);


            db.update(PLOTS_TABLE, contentValues, FARMER_CODE + "  = ? AND " + PLOT_ID + " = ?", new String[]{farmerCode, plotId});


            Log.d(TAG, "DATA RECOMMENDATION ID " + gapsRecId_plotRecId + " APPLIED FOR " + farmerCode + " AND ID " + plotId);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;


    }


    public String getSystemTime() {
        return String.valueOf(System.currentTimeMillis());

    }


    public String getDate() {
        return new SimpleDateFormat("MMM d", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
    }




      boolean addForm(Form form) {

        try {

            if (!doesFormLabelExist(form.getName())) {

                if (!containsString(form.getName().toLowerCase())) {

                    ContentValues contentValues = new ContentValues();
                    contentValues.put(FORM_TYPE, form.getType());
                    contentValues.put(LAST_MODIFIED_DATE, form.getLastModifiedDate());
                    contentValues.put(FORM_NAME, form.getName().toLowerCase());
                    contentValues.put(DISPLAY_NAME, form.getDiaplayName());
                    contentValues.put(DISPLAY_ORDER, form.getDiaplayOrder());
                    contentValues.put(TRANSLATION, form.getTranslation());



                    db.insert(FORMS_TABLE, null, contentValues);

                    Log.d(TAG, "FORM LABEL WITH NAME " + form.getName() + "  AND TYPE " + form.getType() + " INSERTED");


                }


            } else {
                Log.d(TAG, "FORM LABEL WITH NAME " + form.getName() + " ALREADY EXISTS!");

            }


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    private boolean containsString(String testString) {
        List<String> formList = new ArrayList<>();
        formList.add(Constants.ADOPTION_OBSERVATION_RESULTS);
        formList.add(Constants.ADOPTION_OBSERVATIONS);
        formList.add(Constants.PLOT_RESULTS);
        formList.add(Constants.ADDITIONAL_INTERVENTION);
        formList.add(Constants.AGGREGATE_ECONOMIC_RESULTS);
        formList.add(Constants.AGGREGATE_ECONOMIC_RESULTS);
        formList.add(Constants.PLOT_INFORMATION);
        formList.add(Constants.AO_MONITORING);
        formList.add(Constants.AO_MONITORING_RESULT);
        formList.add(Constants.MONITORING_PLOT_INFORMATION);
        formList.add(Constants.COMPETENCE_MONITORING);
        formList.add(Constants.FAILURE_MONITORING);
        formList.add(Constants.OTHER);
        formList.add(Constants.MONITORING);

        return formList.contains(testString);
    }



    public List<Form> getAllDiagnosticForms() {
        List<Form> forms = null;
        Cursor cursor = null;
        try {
            forms = new ArrayList<>();
            String selectQuery = "SELECT  * FROM " + FORMS_TABLE + " WHERE " + FORM_TYPE + " = '" + "Diagnostic' OR "
                    + FORM_TYPE + " = '" + "Diaganostic_Monitoring'";
            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {
                if (cursor.moveToFirst())
                    do {
                        Form form = new Form();
                        form.setId(String.valueOf(cursor.getInt(cursor.getColumnIndex(BaseColumns._ID))));

                        form.setType(cursor.getString(cursor.getColumnIndex(FORM_TYPE)));
                        form.setName(cursor.getString(cursor.getColumnIndex(FORM_NAME)));
                        form.setDiaplayName(cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)));
                        form.setDiaplayOrder(cursor.getDouble(cursor.getColumnIndex(DISPLAY_ORDER)));

                        form.setTranslation(cursor.getString(cursor.getColumnIndex(TRANSLATION)));
                        form.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));
                        Log.i(TAG, "Form found with value " + form.getName());
                        forms.add(form);
                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }
        return sortFormsInAscendingOrder(forms);

    }



    public List<Form> getAllMonitoringForms() {

        List<Form> forms = null;
        Cursor cursor = null;

        try {

            forms = new ArrayList<>();

            String selectQuery = "SELECT  * FROM " + FORMS_TABLE + " WHERE " + FORM_TYPE + " = '" + "Monitoring' OR "
                    + FORM_TYPE + " = '" + "Diaganostic_Monitoring'";
            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {

                        Form form = new Form();
                        form.setId(String.valueOf(cursor.getInt(cursor.getColumnIndex(BaseColumns._ID))));

                        form.setType(cursor.getString(cursor.getColumnIndex(FORM_TYPE)));
                        form.setName(cursor.getString(cursor.getColumnIndex(FORM_NAME)));
                        form.setDiaplayName(cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)));
                        form.setDiaplayOrder(cursor.getDouble(cursor.getColumnIndex(DISPLAY_ORDER)));
                        form.setTranslation(cursor.getString(cursor.getColumnIndex(TRANSLATION)));

                        form.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));


                        forms.add(form);


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return sortFormsInAscendingOrder(forms);

    }


    public List<Form> getAllForms() {

        List<Form> forms = null;
        Cursor cursor = null;

        try {

            forms = new ArrayList<>();

            String selectQuery = "SELECT  * FROM " + FORMS_TABLE;
            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {

                        Form form = new Form();
                        form.setId(String.valueOf(cursor.getInt(cursor.getColumnIndex(BaseColumns._ID))));

                        form.setType(cursor.getString(cursor.getColumnIndex(FORM_TYPE)));
                        form.setName(cursor.getString(cursor.getColumnIndex(FORM_NAME)));
                        form.setDiaplayOrder(cursor.getDouble(cursor.getColumnIndex(DISPLAY_ORDER)));

                        form.setDiaplayName(cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)));
                        form.setTranslation(cursor.getString(cursor.getColumnIndex(TRANSLATION)));

                        form.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));

                        //Log.i(TAG, "Form found with value " + form.getName());

                        forms.add(form);


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return sortFormsInAscendingOrder(forms);

    }

    boolean doesFormLabelExist(String formLabel) {

        SQLiteDatabase db = this.getReadableDatabase();
        return DatabaseUtils.queryNumEntries(db, FORMS_TABLE, FORM_NAME + " = ? ",
                new String[]{formLabel.toLowerCase()}) > 0;

    }


    public Form getFormBasedOnName(String name) {

        Cursor cursor = null;
        Form form = null;

        try {

            String selectQuery = "SELECT  * FROM " + FORMS_TABLE + " WHERE " + FORM_NAME + "='" + name + "'";
            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {

                        form = new Form();
                        form.setId(String.valueOf(cursor.getInt(cursor.getColumnIndex(BaseColumns._ID))));
                        form.setType(cursor.getString(cursor.getColumnIndex(FORM_TYPE)));
                        form.setName(cursor.getString(cursor.getColumnIndex(FORM_NAME)));
                        form.setDiaplayName(cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)));
                        form.setDiaplayOrder(cursor.getDouble(cursor.getColumnIndex(DISPLAY_ORDER)));

                        form.setTranslation(cursor.getString(cursor.getColumnIndex(TRANSLATION)));
                        form.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));


                    } while (cursor.moveToNext());
            } else {

                form = new Form();
                form.setDiaplayName(name);
                form.setTranslation(name);
                form.setName(name);

            }
        } catch (Exception e) {
            e.printStackTrace();
            form = new Form();
            form.setDiaplayName(name);
            form.setTranslation(name);
            form.setName(name);

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return form;


    }


    public Form getFormBasedOnId(String id) {

        Cursor cursor = null;
        Form form = null;

        try {

            String selectQuery = "SELECT  * FROM " + FORMS_TABLE + " WHERE " + BaseColumns._ID + "='" + id + "'";
            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {

                        form = new Form();
                        form.setId(String.valueOf(cursor.getInt(cursor.getColumnIndex(BaseColumns._ID))));

                        form.setType(cursor.getString(cursor.getColumnIndex(FORM_TYPE)));
                        form.setName(cursor.getString(cursor.getColumnIndex(FORM_NAME)));
                        form.setDiaplayName(cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)));
                        form.setDiaplayOrder(cursor.getDouble(cursor.getColumnIndex(DISPLAY_ORDER)));

                        form.setTranslation(cursor.getString(cursor.getColumnIndex(TRANSLATION)));
                        form.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return form;


    }



    public boolean addVillage(Village village) {

        try {

            if (!doesVillageExist(village.getId())) {

                ContentValues contentValues = new ContentValues();
                contentValues.put(LAST_MODIFIED_DATE, village.getLastModifiedDate());
                contentValues.put(ID, village.getId());
                contentValues.put(VILLAGE_NAME, village.getName());
                contentValues.put(VILLAGE_DISTRICT, village.getDistrict());


                db.insert(VILLAGES_TABLE, null, contentValues);

                Log.d(TAG, "VILLAGE WITH NAME " + village.getName() + " INSERTED");

            } else {
                Log.d(TAG, "VILLAGE WITH NAME " + village.getName() + " ALREADY EXISTS!");

            }


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    public boolean deleteVillage(String villageId) {

        return db.delete(VILLAGES_TABLE, VILLAGE_ID + " = ? ", new String[]{villageId}) > 0;

    }

    public String getVillageId(String name) {

        String id = "";
        Cursor cursor = null;

        try {


            String selectQuery = "SELECT  * FROM " + VILLAGES_TABLE + " WHERE " + VILLAGE_NAME + " ='" + name + "'";
            Log.i("QUERY", selectQuery);

            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())


                    id = cursor.getString(cursor.getColumnIndex(VILLAGE_ID));

                Log.i(TAG, "Village found with id " + id);


            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return id;


    }

    public String getVillageName(String id) {

        String name = "";
        Cursor cursor = null;

        try {


            String selectQuery = "SELECT  * FROM " + VILLAGES_TABLE + " WHERE " + VILLAGE_ID + " ='" + id + "'";
            Log.i("QUERY", selectQuery);

            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())


                    name = cursor.getString(cursor.getColumnIndex(VILLAGE_NAME));

                Log.i(TAG, "Village found with name " + name);


            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return name;


    }

    public List<Village> getAllVillages() {

        List<Village> villages;
        Cursor cursor = null;

        try {

            villages = new ArrayList<>();

            String selectQuery = "SELECT  * FROM " + VILLAGES_TABLE;
            Log.i("QUERY", selectQuery);

            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        Village village = new Village();
                        village.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));
                        village.setId(cursor.getString(cursor.getColumnIndex(VILLAGE_ID)));
                        village.setName(cursor.getString(cursor.getColumnIndex(VILLAGE_NAME)));
                        village.setDistrict(cursor.getString(cursor.getColumnIndex(VILLAGE_DISTRICT)));


                        villages.add(village);


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return sortVillagesInAscendingOrder(villages);


    }

    public List<String> getAllVillageNames() {
        List<String> villages;
        Cursor cursor = null;
        try {
            villages = new ArrayList<>();
            String selectQuery = "SELECT  * FROM " + VILLAGES_TABLE;
            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);
            if (cursor != null && cursor.getCount() > 0) {
                if (cursor.moveToFirst())
                    do {
                        villages.add(cursor.getString(cursor.getColumnIndex(VILLAGE_NAME)));
                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return sortVillagesByNameInAscendingOrder(villages);


    }


    public boolean addCountry(Country country) {

        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(ID, country.getId());
            contentValues.put(COUNTRY_NAME, country.getName());
            contentValues.put(COUNTRY_ISO_CODE, country.getIsoCode());
            contentValues.put(COUNTRY_CURRENCY, country.getCurrency());

            contentValues.put(COUNTRY_DRY_GATE_PRICE, country.getAverageGatePrice());

            db.insert(COUNTRIES_TABLE, null, contentValues);



        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }


    public boolean deleteCountriesTable() {

        try {


            db.execSQL("DELETE FROM " + COUNTRIES_TABLE);


            return true;
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }
    }

    public List<Country> getAllCountries() {

        List<Country> countries;
        Cursor cursor = null;

        try {

            countries = new ArrayList<>();

            String selectQuery = "SELECT  * FROM " + COUNTRIES_TABLE;
            Log.i("QUERY", selectQuery);

            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {


                        Country country = new Country();
                        country.setId(cursor.getString(cursor.getColumnIndex(COUNTRY_ID)));
                        country.setName(cursor.getString(cursor.getColumnIndex(COUNTRY_NAME)));
                        country.setIsoCode(cursor.getString(cursor.getColumnIndex(COUNTRY_ISO_CODE)));
                        country.setCurrency(cursor.getString(cursor.getColumnIndex(COUNTRY_CURRENCY)));
                        country.setAverageGatePrice(cursor.getString(cursor.getColumnIndex(COUNTRY_DRY_GATE_PRICE)));



                        countries.add(country);


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return countries;


    }


    boolean doesVillageExist(String id) {

        SQLiteDatabase db = this.getReadableDatabase();
        return DatabaseUtils.queryNumEntries(db, VILLAGES_TABLE, VILLAGE_ID + " = ? ",
                new String[]{id}) > 0;

    }



    public boolean hasUnsyncedFarmerData(int status) {

        SQLiteDatabase db = this.getReadableDatabase();
        return DatabaseUtils.queryNumEntries(db, FARMER_TABLE, FARMER_SYNC_STATUS + " = ? ",
                new String[]{String.valueOf(status)}) > 0;

    }


    public boolean addNewPlot(RealPlot realPlot) {
        try {

            ContentValues contentValues = new ContentValues();
            contentValues.put(FARMER_CODE, realPlot.getFarmerCode());
            contentValues.put(PLOT_ID, realPlot.getId());
            contentValues.put(PLOT_NAME, realPlot.getName());

            contentValues.put(PLOT_ANSWERS_JSON, realPlot.getPlotInformationJson());
            contentValues.put(RECOMMENDATION_ID, realPlot.getRecommendationId());
            contentValues.put(PLOT_GPS_POINTS, realPlot.getGpsPoints());

            contentValues.put(START_YEAR, realPlot.getStartYear());


            db.insert(PLOTS_TABLE, null, contentValues);



        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }


    public boolean editPlotStartYear(String plotId, int newStartYearValue) {
        try {

            //Todo plot_intervention_year

            Question interventionStartYear = getQuestionByTranslation("Intervention start year");
            editPlotAORJson(plotId, interventionStartYear.getId(), String.valueOf(newStartYearValue));


            ContentValues contentValues = new ContentValues();
            contentValues.put(START_YEAR, newStartYearValue);

            db.update(PLOTS_TABLE, contentValues, ID + "= ?", new String[]{plotId});
            Log.i(TAG, "PLOT START YEAR UPDATED! " + "NEW VALUE IS " + newStartYearValue);


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }


    public boolean editPlotGPS(String plotId, String newValue) {
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(PLOT_GPS_POINTS, newValue);
            db.update(PLOTS_TABLE, contentValues, ID + "= ?", new String[]{plotId});
            Log.i(TAG, "PLOT GPS POINTS! " + "NEW VALUE IS " + newValue);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }


    Boolean editPlotAORJson(String plotId, String questionId, String newValue) {
            Log.i(TAG, "PLOT INFO JSON");

        String jsonStringValue = null;

        Cursor cursor = null;
        try{

            String selectQuery = "SELECT  " + PLOT_ANSWERS_JSON + " FROM " + PLOTS_TABLE + " WHERE " + PLOT_ID + " ='" + plotId + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {
                if (cursor.moveToFirst())

                    jsonStringValue = cursor.getString(cursor.getColumnIndex(PLOT_ANSWERS_JSON));

            }
        }catch(Exception e){

            e.printStackTrace();

        }finally {
            if(cursor != null)
                cursor.close();
        }


        try {
            JSONObject jsonObject ;
            if(jsonStringValue != null) {

                Log.d(TAG, "OLD AOR VALUE IS " + jsonStringValue);


                jsonObject = new JSONObject(jsonStringValue);

                    if (jsonObject.has(questionId)) {
                        jsonObject.remove(questionId);
                        jsonObject.put(questionId, newValue);
                    } else {
                        jsonObject.put(questionId, newValue);
                    }
                    Log.d(TAG, "ADDING " + newValue + " TO OLD AOR JSON OBJECT");


                ContentValues contentValues = new ContentValues();

                contentValues.put(PLOT_ANSWERS_JSON, jsonObject.toString());
                db.update(PLOTS_TABLE, contentValues, ID + "= ?", new String[]{plotId});
                Log.d(TAG, "DATA\t" + jsonObject.toString() + "ADDED\n FOR PLOT WITH ID " + plotId);

            }else return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        return true;
    }



    public boolean deletePlot(String id) {

        return db.delete(PLOTS_TABLE, PLOT_ID + " = ? ", new String[]{id}) > 0;

    }

    public List<RealPlot> getAllFarmerPlots(String farmerId) {

        List<RealPlot> realPlots = null;
        Cursor cursor = null;

        try {

            realPlots = new ArrayList<>();

            String selectQuery = "SELECT  * FROM " + PLOTS_TABLE + " WHERE " +
                    FARMER_CODE + " ='" + farmerId + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {


                        RealPlot realPlot = new RealPlot();

                        realPlot.setId(cursor.getString(cursor.getColumnIndex(PLOT_ID)));
                        realPlot.setName(cursor.getString(cursor.getColumnIndex(PLOT_NAME)));

                        realPlot.setFarmerCode(cursor.getString(cursor.getColumnIndex(FARMER_CODE)));
                        realPlot.setPlotInformationJson(cursor.getString(cursor.getColumnIndex(PLOT_ANSWERS_JSON)));
                        realPlot.setRecommendationId(cursor.getString(cursor.getColumnIndex(RECOMMENDATION_ID)));
                        realPlot.setGpsPoints(cursor.getString(cursor.getColumnIndex(PLOT_GPS_POINTS)));

                        realPlot.setStartYear(cursor.getInt(cursor.getColumnIndex(START_YEAR)));



                        realPlots.add(realPlot);


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return realPlots;


    }


    public RealPlot getFarmerPlot(String id, String farmerId) {

        RealPlot realPlot = null;
        Cursor cursor = null;

        try {

            String selectQuery = "SELECT  * FROM " + PLOTS_TABLE + " WHERE " +
                    FARMER_CODE + " ='" + farmerId + "' AND " + PLOT_ID + " ='" + id + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {


                        realPlot = new RealPlot();

                        realPlot.setId(cursor.getString(cursor.getColumnIndex(PLOT_ID)));
                        realPlot.setName(cursor.getString(cursor.getColumnIndex(PLOT_NAME)));

                        realPlot.setFarmerCode(cursor.getString(cursor.getColumnIndex(FARMER_CODE)));
                        realPlot.setPlotInformationJson(cursor.getString(cursor.getColumnIndex(PLOT_ANSWERS_JSON)));

                        realPlot.setGpsPoints(cursor.getString(cursor.getColumnIndex(PLOT_GPS_POINTS)));
                        realPlot.setStartYear(cursor.getInt(cursor.getColumnIndex(START_YEAR)));

                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return realPlot;


    }

    public boolean addSkipLogic(SkipLogic skipLogic) {
        try {

            ContentValues contentValues = new ContentValues();
            contentValues.put(ID, skipLogic.getId());
            contentValues.put(SKIP_LOGIC_NAME, skipLogic.getName());
            contentValues.put(SKIP_LOGIC_QUESTION_ID, skipLogic.getQuestionId());
            contentValues.put(SKIP_LOGIC_ANSWER_VALUE, skipLogic.getAnswerValue());
            contentValues.put(SKIP_LOGIC_QUESTION_AFFECTED_ID, skipLogic.getQuestionShowHide());
            contentValues.put(SKIP_LOGIC_OPERATOR, skipLogic.getLogicalOperator());
            contentValues.put(SKIP_LOGIC_ACTION_TAKEN, skipLogic.getActionToBeTaken());
            contentValues.put(LAST_MODIFIED_DATE, skipLogic.getLastModifiedDate());


            db.insert(SKIP_LOGIC_TABLE, null, contentValues);


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    public boolean deleteSkipLogic(String id) {

        return db.delete(SKIP_LOGIC_TABLE, ID + " = ? ", new String[]{id}) > 0;

    }

 /*   public boolean deleteSkipLogicsTable() {

        try {


            db.execSQL("DELETE FROM " + SKIP_LOGIC_TABLE);

            Log.i("DATABASE", "SKIP_LOGIC_TABLE TABLE DELETED");

            return true;
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }
    }*/

    public List<SkipLogic> doesQuestionHaveSkipLogics(String questionId) {

        List<SkipLogic> skipLogics = null;

        Cursor cursor = null;

        try {

            skipLogics = new ArrayList<>();

            String selectQuery = "SELECT  * FROM " + SKIP_LOGIC_TABLE + " WHERE " +
                    SKIP_LOGIC_QUESTION_ID + " ='" + questionId + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        SkipLogic skipLogic = new SkipLogic();
                        skipLogic.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        skipLogic.setName(cursor.getString(cursor.getColumnIndex(SKIP_LOGIC_NAME)));
                        skipLogic.setQuestionId(cursor.getString(cursor.getColumnIndex(SKIP_LOGIC_QUESTION_ID)));
                        skipLogic.setAnswerValue(cursor.getString(cursor.getColumnIndex(SKIP_LOGIC_ANSWER_VALUE)));
                        skipLogic.setQuestionShowHide(cursor.getString(cursor.getColumnIndex(SKIP_LOGIC_QUESTION_AFFECTED_ID)));
                        skipLogic.setLogicalOperator(cursor.getString(cursor.getColumnIndex(SKIP_LOGIC_OPERATOR)));
                        skipLogic.setActionToBeTaken(cursor.getString(cursor.getColumnIndex(SKIP_LOGIC_ACTION_TAKEN)));
                        skipLogic.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));

                        skipLogics.add(skipLogic);


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return skipLogics;


    }


    public SkipLogic  doesQuestionHaveSkipLogic(String questionId) {


        SkipLogic skipLogic = null;
        Cursor cursor = null;

        try {


            String selectQuery = "SELECT  * FROM " + SKIP_LOGIC_TABLE + " WHERE " +
                    SKIP_LOGIC_QUESTION_AFFECTED_ID + " ='" + questionId + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {skipLogic = new SkipLogic();
                        skipLogic.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        skipLogic.setName(cursor.getString(cursor.getColumnIndex(SKIP_LOGIC_NAME)));
                        skipLogic.setQuestionId(cursor.getString(cursor.getColumnIndex(SKIP_LOGIC_QUESTION_ID)));
                        skipLogic.setAnswerValue(cursor.getString(cursor.getColumnIndex(SKIP_LOGIC_ANSWER_VALUE)));
                        skipLogic.setQuestionShowHide(cursor.getString(cursor.getColumnIndex(SKIP_LOGIC_QUESTION_AFFECTED_ID)));
                        skipLogic.setLogicalOperator(cursor.getString(cursor.getColumnIndex(SKIP_LOGIC_OPERATOR)));
                        skipLogic.setActionToBeTaken(cursor.getString(cursor.getColumnIndex(SKIP_LOGIC_ACTION_TAKEN)));
                        skipLogic.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return skipLogic;


    }





    public boolean addCalculation(Calculation calculation) {
        try {

            ContentValues contentValues = new ContentValues();
            contentValues.put(ID, calculation.getId());
            contentValues.put(CALCULATIONS_NAME, calculation.getName());
            contentValues.put(CALCULATIONS_Q1, calculation.getQuestion1());
            contentValues.put(CALCULATIONS_Q2, calculation.getQuestion2());
            contentValues.put(CALCULATIONS_Q3, calculation.getQuestion3());
            contentValues.put(CALCULATIONS_Q4, calculation.getQuestion4());
            contentValues.put(CALCULATIONS_OP1, calculation.getOperator1());
            contentValues.put(CALCULATIONS_OP2, calculation.getOperator2());
            contentValues.put(CALCULATIONS_OP3, calculation.getOperator3());
            contentValues.put(HIERARCHY, calculation.getHierarchy());
            contentValues.put(CALCULATIONS_RESULT_QUESTION, calculation.getResultQuestion());
            contentValues.put(LAST_MODIFIED_DATE, calculation.getLastModifiedDate());


            db.insert(CALCULATIONS_TABLE, null, contentValues);


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }


    public boolean deleteCalculation(String id) {

        return db.delete(CALCULATIONS_TABLE, ID + " = ? ", new String[]{id}) > 0;

    }

   /* public boolean deleteCalculationsTable() {

        try {


            db.execSQL("DELETE FROM " + CALCULATIONS_TABLE);

            Log.i("DATABASE", "CALCULATIONS TABLE DELETED");

            return true;
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }
    }*/

    public Calculation getCalculation(String id) {

        Calculation calculation = null;
        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + CALCULATIONS_TABLE + " WHERE " +
                    CALCULATIONS_RESULT_QUESTION + " ='" + id + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        calculation = new Calculation();


                        calculation.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        calculation.setName(cursor.getString(cursor.getColumnIndex(CALCULATIONS_NAME)));
                        calculation.setQuestion1(cursor.getString(cursor.getColumnIndex(CALCULATIONS_Q1)));
                        calculation.setQuestion2(cursor.getString(cursor.getColumnIndex(CALCULATIONS_Q2)));
                        calculation.setQuestion3(cursor.getString(cursor.getColumnIndex(CALCULATIONS_Q3)));
                        calculation.setQuestion4(cursor.getString(cursor.getColumnIndex(CALCULATIONS_Q4)));
                        calculation.setOperator1(cursor.getString(cursor.getColumnIndex(CALCULATIONS_OP1)));
                        calculation.setOperator2(cursor.getString(cursor.getColumnIndex(CALCULATIONS_OP2)));
                        calculation.setOperator3(cursor.getString(cursor.getColumnIndex(CALCULATIONS_OP3)));
                        calculation.setHierarchy(cursor.getInt(cursor.getColumnIndex(HIERARCHY)));
                        calculation.setResultQuestion(cursor.getString(cursor.getColumnIndex(CALCULATIONS_RESULT_QUESTION)));
                        calculation.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));
/*


                        Log.i(TAG, "CALCULATION WITH ID " + id + " \nDATA IS \n\n");
                        Log.i(TAG, "NAME  =  " + calculation.getName() + " \n");
                        Log.i(TAG, "QUESTION1  =  " + calculation.getQuestion1() + " \n");
                        Log.i(TAG, "QUESTION2  =  " + calculation.getQuestion2() + " \n");
                        Log.i(TAG, "QUESTION3  =  " + calculation.getQuestion3() + " \n");
                        Log.i(TAG, "QUESTION4  =  " + calculation.getQuestion4() + " \n");
                        Log.i(TAG, "OP1  =  " + calculation.getOperator1() + " \n");
                        Log.i(TAG, "OP2  =  " + calculation.getOperator2() + " \n");
                        Log.i(TAG, "OP3  =  " + calculation.getOperator3() + " \n");
                        Log.i(TAG, "RESULT QUESTION  =  " + calculation.getResultQuestion() + " \n");
*/


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return calculation;


    }


    public List<Calculation> getAllCalculations() {

        List<Calculation> calculationList = new ArrayList<>();


        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + CALCULATIONS_TABLE + "";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        Calculation calculation = new Calculation();


                        calculation.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        calculation.setName(cursor.getString(cursor.getColumnIndex(CALCULATIONS_NAME)));
                        calculation.setQuestion1(cursor.getString(cursor.getColumnIndex(CALCULATIONS_Q1)));
                        calculation.setQuestion2(cursor.getString(cursor.getColumnIndex(CALCULATIONS_Q2)));
                        calculation.setQuestion3(cursor.getString(cursor.getColumnIndex(CALCULATIONS_Q3)));
                        calculation.setQuestion4(cursor.getString(cursor.getColumnIndex(CALCULATIONS_Q4)));
                        calculation.setOperator1(cursor.getString(cursor.getColumnIndex(CALCULATIONS_OP1)));
                        calculation.setOperator2(cursor.getString(cursor.getColumnIndex(CALCULATIONS_OP2)));
                        calculation.setOperator3(cursor.getString(cursor.getColumnIndex(CALCULATIONS_OP3)));
                        calculation.setHierarchy(cursor.getInt(cursor.getColumnIndex(HIERARCHY)));
                        calculation.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));
                        calculation.setResultQuestion(cursor.getString(cursor.getColumnIndex(CALCULATIONS_RESULT_QUESTION)));

/*

                        Log.i(TAG, "CALCULATION WITH ID " + calculation.getId() + " \nDATA IS \n\n");
                        Log.i(TAG, "NAME  =  " + calculation.getName() + " \n");
                        Log.i(TAG, "QUESTION1  =  " + calculation.getQuestion1() + " \n");
                        Log.i(TAG, "QUESTION2  =  " + calculation.getQuestion2() + " \n");
                        Log.i(TAG, "QUESTION3  =  " + calculation.getQuestion3() + " \n");
                        Log.i(TAG, "QUESTION4  =  " + calculation.getQuestion4() + " \n");
                        Log.i(TAG, "OP1  =  " + calculation.getOperator1() + " \n");
                        Log.i(TAG, "OP2  =  " + calculation.getOperator2() + " \n");
                        Log.i(TAG, "OP3  =  " + calculation.getOperator3() + " \n");
                        Log.i(TAG, "RESULT QUESTION  =  " + calculation.getResultQuestion() + " \n");
*/


                        calculationList.add(calculation);


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return calculationList;

    }


    public List<Calculation> sortCalculations() {

        List<Calculation> recommendations = getAllCalculations();

        if (recommendations != null)

            Collections.sort(recommendations, new Comparator<Calculation>() {
                @Override
                public int compare(Calculation o, Calculation t1) {
                    Integer value1;
                    Integer value2;
                    try {

                        value1 =  o.getHierarchy();
                        value2 =  t1.getHierarchy();

                        Log.i(TAG, "V1 = " + value1 + " AND V2 " + value2);

                        return value1.compareTo(value2);

                    } catch (Exception e) {
                        e.printStackTrace();
                        return 1;
                    }
                }
            });


        // Log.i(TAG, "SORTED OUT WITH SIZE " + recommendations.size());


        return recommendations;

    }


    public boolean addRecommendation(Recommendation recommendation) {
        try {

            ContentValues contentValues = new ContentValues();
            contentValues.put(LAST_MODIFIED_DATE, recommendation.getLastModifiedDate());

            contentValues.put(ID, recommendation.getId());
            contentValues.put(RECOMMENDATIONS_NAME, recommendation.getName());
            contentValues.put(RECOMMENDATIONS_CONDITION, recommendation.getCondition());
            contentValues.put(RECOMMENDATIONS_DESCRIPTION, recommendation.getDescription());
            contentValues.put(RECOMMENDATIONS_HIERARCHY, recommendation.getHierarchy());
            contentValues.put(RECOMMENDATIONS_COST0, recommendation.getCost0());
            contentValues.put(RECOMMENDATIONS_COST_QUESTIONS0, recommendation.getCostQuestions());

            contentValues.put(RECOMMENDATIONS_INCOME0, recommendation.getIncome0());
            contentValues.put(RECOMMENDATIONS_INCOME1, recommendation.getIncome1());
            contentValues.put(RECOMMENDATIONS_INCOME2, recommendation.getIncome2());
            contentValues.put(RECOMMENDATIONS_INCOME3, recommendation.getIncome3());
            contentValues.put(RECOMMENDATIONS_INCOME4, recommendation.getIncome4());
            contentValues.put(RECOMMENDATIONS_INCOME5, recommendation.getIncome5());
            contentValues.put(RECOMMENDATIONS_INCOME6, recommendation.getIncome6());
            contentValues.put(RECOMMENDATIONS_INCOME7, recommendation.getIncome7());

            contentValues.put(TRANSLATION, recommendation.getTranslation());

            contentValues.put(RECOMMENDATIONS_LOGIC, recommendation.getLogicId());
            contentValues.put(RECOMMENDATIONS_RELATED_1, recommendation.getRelatedOne());
            contentValues.put(RECOMMENDATIONS_RELATED_2, recommendation.getRelatedTwo());
            contentValues.put(RECOMMENDATIONS_YEAR_BACK_TO_GAPS, recommendation.getYearBackToGAPs());
            contentValues.put(RECOMMENDATIONS_QUESTIONS_INVOLVED, recommendation.getQuestionsInvolved());


            db.insert(RECOMMENDATIONS_TABLE, null, contentValues);


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    public boolean deleteRecommendation(String id) {

        return db.delete(RECOMMENDATIONS_TABLE, ID + " = ? ", new String[]{id}) > 0;

    }


    public Recommendation getRecommendation(String id) {

        Recommendation recommendation = null;
        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + RECOMMENDATIONS_TABLE + " WHERE " +
                    ID + " ='" + id + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        recommendation = new Recommendation();

                        recommendation.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));

                        recommendation.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        recommendation.setName(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_NAME)));
                        recommendation.setCondition(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_CONDITION)));
                        recommendation.setDescription(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_DESCRIPTION)));
                        recommendation.setHierarchy(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_HIERARCHY)));

                        recommendation.setCost0(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_COST0)));
                        recommendation.setCostQuestions(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_COST_QUESTIONS0)));


                        recommendation.setIncome0(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME0)));
                        recommendation.setIncome1(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME1)));
                        recommendation.setIncome2(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME2)));
                        recommendation.setIncome3(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME3)));
                        recommendation.setIncome4(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME4)));
                        recommendation.setIncome5(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME5)));
                        recommendation.setIncome6(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME6)));
                        recommendation.setIncome7(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME7)));
                        recommendation.setTranslation(cursor.getString(cursor.getColumnIndex(TRANSLATION)));
                        recommendation.setLogicId(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_LOGIC)));
                        recommendation.setRelatedOne(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_RELATED_1)));
                        recommendation.setRelatedTwo(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_RELATED_2)));
                        recommendation.setYearBackToGAPs(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_YEAR_BACK_TO_GAPS)));
                        recommendation.setQuestionsInvolved(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_QUESTIONS_INVOLVED)));

/*

                        Log.i(TAG, "RECOMMENDATION WITH ID " + recommendation.getId() + " \nDATA IS \n\n");
                        Log.i(TAG, "NAME  =  " + recommendation.getName() + " \n");
                        Log.i(TAG, "CONDITION  =  " + recommendation.getCondition() + " \n");
                        Log.i(TAG, "DESCRIPTION  =  " + recommendation.getDescription() + " \n");
                        Log.i(TAG, "HIERARCHY  =  " + recommendation.getHierarchy() + " \n");
                        Log.i(TAG, "INCOME0  =  " + recommendation.getIncome0() + " \n");
                        Log.i(TAG, "INCOME1  =  " + recommendation.getIncome1() + " \n");
                        Log.i(TAG, "INCOME2  =  " + recommendation.getIncome2() + " \n");
                        Log.i(TAG, "INCOME3  =  " + recommendation.getIncome3() + " \n");
                        Log.i(TAG, "INCOME4  =  " + recommendation.getIncome4() + " \n");
                        Log.i(TAG, "INCOME5  =  " + recommendation.getIncome5() + " \n");
                        Log.i(TAG, "INCOME6  =  " + recommendation.getIncome6() + " \n");
                        Log.i(TAG, "INCOME7  =  " + recommendation.getIncome7() + " \n");
                        Log.i(TAG, "LOGIC  =  " + recommendation.getLogicId() + " \n");
                        Log.i(TAG, "TRANSLATION  =  " + recommendation.getTranslation() + " \n");

                        Log.i(TAG, "RELATED_1  =  " + recommendation.getRelatedOne() + " \n");
                        Log.i(TAG, "RELATED_2  =  " + recommendation.getRelatedTwo() + " \n");
                        Log.i(TAG, "YEAR_BACK_TO_GAPS  =  " + recommendation.getYearBackToGAPs() + " \n");
                        Log.i(TAG, "QUESTIONS INVOLVED  =  " + recommendation.getQuestionsInvolved() + " \n");
*/



                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return recommendation;


    }

    public List<Recommendation> getAllRecommendations() {

        List<Recommendation> recommendations = new ArrayList<>();

        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + RECOMMENDATIONS_TABLE;

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        Recommendation recommendation = new Recommendation();

                        recommendation.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));

                        recommendation.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        recommendation.setName(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_NAME)));
                        recommendation.setCondition(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_CONDITION)));
                        recommendation.setDescription(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_DESCRIPTION)));
                        recommendation.setHierarchy(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_HIERARCHY)));
                        recommendation.setCost0(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_COST0)));
                        recommendation.setCostQuestions(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_COST_QUESTIONS0)));

                        recommendation.setIncome0(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME0)));
                        recommendation.setIncome1(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME1)));
                        recommendation.setIncome2(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME2)));
                        recommendation.setIncome3(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME3)));
                        recommendation.setIncome4(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME4)));
                        recommendation.setIncome5(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME5)));
                        recommendation.setIncome6(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME6)));
                        recommendation.setIncome7(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME7)));

                        recommendation.setTranslation(cursor.getString(cursor.getColumnIndex(TRANSLATION)));
                        recommendation.setLogicId(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_LOGIC)));
                        recommendation.setRelatedOne(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_RELATED_1)));
                        recommendation.setRelatedTwo(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_RELATED_2)));
                        recommendation.setYearBackToGAPs(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_YEAR_BACK_TO_GAPS)));
                        recommendation.setQuestionsInvolved(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_QUESTIONS_INVOLVED)));


                     /*   Log.i(TAG, "RECOMMENDATION WITH ID " + recommendation.getId() + " \nDATA IS \n\n");
                        Log.i(TAG, "NAME  =  " + recommendation.getName() + " \n");
                        Log.i(TAG, "CONDITION  =  " + recommendation.getCondition() + " \n");
                        Log.i(TAG, "DESCRIPTION  =  " + recommendation.getDescription() + " \n");
                        Log.i(TAG, "HIERARCHY  =  " + recommendation.getHierarchy() + " \n");
                        Log.i(TAG, "INCOME0  =  " + recommendation.getIncome0() + " \n");
                        Log.i(TAG, "INCOME1  =  " + recommendation.getIncome1() + " \n");
                        Log.i(TAG, "INCOME2  =  " + recommendation.getIncome2() + " \n");
                        Log.i(TAG, "INCOME3  =  " + recommendation.getIncome3() + " \n");
                        Log.i(TAG, "INCOME4  =  " + recommendation.getIncome4() + " \n");
                        Log.i(TAG, "INCOME5  =  " + recommendation.getIncome5() + " \n");
                        Log.i(TAG, "INCOME6  =  " + recommendation.getIncome6() + " \n");
                        Log.i(TAG, "INCOME7  =  " + recommendation.getIncome7() + " \n");

                        Log.i(TAG, "LOGIC  =  " + recommendation.getLogicId() + " \n");
                        Log.i(TAG, "RELATED_1  =  " + recommendation.getRelatedOne() + " \n");
                        Log.i(TAG, "RELATED_2  =  " + recommendation.getRelatedTwo() + " \n");
                        Log.i(TAG, "YEAR_BACK_TO_GAPS  =  " + recommendation.getYearBackToGAPs() + " \n");
                        Log.i(TAG, "QUESTIONS INVOLVED  =  " + recommendation.getQuestionsInvolved() + " \n");

*/
                        recommendations.add(recommendation);

                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return recommendations;

    }



    public Recommendation getRecommendationBasedOnName(String name) {

        Recommendation recommendation = null;
        Cursor cursor = null;


        try {
            String selectQuery = "SELECT  * FROM " + RECOMMENDATIONS_TABLE + "  WHERE " + RECOMMENDATIONS_NAME + " ='" + name + "'";



            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        recommendation = new Recommendation();

                        recommendation.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));

                        recommendation.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        recommendation.setName(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_NAME)));
                        recommendation.setCondition(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_CONDITION)));
                        recommendation.setDescription(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_DESCRIPTION)));
                        recommendation.setHierarchy(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_HIERARCHY)));
                        recommendation.setCost0(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_COST0)));
                        recommendation.setCostQuestions(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_COST_QUESTIONS0)));

                        recommendation.setIncome0(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME0)));
                        recommendation.setIncome1(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME1)));
                        recommendation.setIncome2(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME2)));
                        recommendation.setIncome3(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME3)));
                        recommendation.setIncome4(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME4)));
                        recommendation.setIncome5(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME5)));
                        recommendation.setIncome6(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME6)));
                        recommendation.setIncome7(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME7)));

                        recommendation.setTranslation(cursor.getString(cursor.getColumnIndex(TRANSLATION)));

                        recommendation.setLogicId(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_LOGIC)));
                        recommendation.setRelatedOne(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_RELATED_1)));
                        recommendation.setRelatedTwo(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_RELATED_2)));
                        recommendation.setYearBackToGAPs(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_YEAR_BACK_TO_GAPS)));
                        recommendation.setQuestionsInvolved(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_QUESTIONS_INVOLVED)));

/*

                        Log.i(TAG, "RECOMMENDATION WITH ID " + recommendation.getId() + " \nDATA IS \n\n");
                        Log.i(TAG, "NAME  =  " + recommendation.getName() + " \n");
                        Log.i(TAG, "CONDITION  =  " + recommendation.getCondition() + " \n");
                        Log.i(TAG, "DESCRIPTION  =  " + recommendation.getDescription() + " \n");
                        Log.i(TAG, "HIERARCHY  =  " + recommendation.getHierarchy() + " \n");
                        Log.i(TAG, "INCOME0  =  " + recommendation.getIncome0() + " \n");
                        Log.i(TAG, "INCOME1  =  " + recommendation.getIncome1() + " \n");
                        Log.i(TAG, "INCOME2  =  " + recommendation.getIncome2() + " \n");
                        Log.i(TAG, "INCOME3  =  " + recommendation.getIncome3() + " \n");
                        Log.i(TAG, "INCOME4  =  " + recommendation.getIncome4() + " \n");
                        Log.i(TAG, "INCOME5  =  " + recommendation.getIncome5() + " \n");
                        Log.i(TAG, "INCOME6  =  " + recommendation.getIncome6() + " \n");
                        Log.i(TAG, "INCOME7  =  " + recommendation.getIncome7() + " \n");
                        Log.i(TAG, "LOGIC  =  " + recommendation.getLogicId() + " \n");
                        Log.i(TAG, "RELATED_1  =  " + recommendation.getRelatedOne() + " \n");
                        Log.i(TAG, "RELATED_2  =  " + recommendation.getRelatedTwo() + " \n");
                        Log.i(TAG, "YEAR_BACK_TO_GAPS  =  " + recommendation.getYearBackToGAPs() + " \n");
*/

                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return recommendation;
    }


    public Recommendation getRecommendationBasedOnTranslationName(String name) {

        Recommendation recommendation = null;
        Cursor cursor = null;


        try {
            String selectQuery = "SELECT  * FROM " + RECOMMENDATIONS_TABLE + "  WHERE " + TRANSLATION + " ='" + name + "'";


            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        recommendation = new Recommendation();

                        recommendation.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));

                        recommendation.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        recommendation.setName(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_NAME)));
                        recommendation.setCondition(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_CONDITION)));
                        recommendation.setDescription(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_DESCRIPTION)));
                        recommendation.setHierarchy(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_HIERARCHY)));
                        recommendation.setCost0(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_COST0)));
                        recommendation.setCostQuestions(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_COST_QUESTIONS0)));

                        recommendation.setIncome0(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME0)));
                        recommendation.setIncome1(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME1)));
                        recommendation.setIncome2(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME2)));
                        recommendation.setIncome3(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME3)));
                        recommendation.setIncome4(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME4)));
                        recommendation.setIncome5(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME5)));
                        recommendation.setIncome6(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME6)));
                        recommendation.setIncome7(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_INCOME7)));

                        recommendation.setTranslation(cursor.getString(cursor.getColumnIndex(TRANSLATION)));

                        recommendation.setLogicId(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_LOGIC)));
                        recommendation.setRelatedOne(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_RELATED_1)));
                        recommendation.setRelatedTwo(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_RELATED_2)));
                        recommendation.setYearBackToGAPs(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_YEAR_BACK_TO_GAPS)));
                        recommendation.setQuestionsInvolved(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_QUESTIONS_INVOLVED)));

/*

                        Log.i(TAG, "RECOMMENDATION WITH ID " + recommendation.getId() + " \nDATA IS \n\n");
                        Log.i(TAG, "NAME  =  " + recommendation.getName() + " \n");
                        Log.i(TAG, "CONDITION  =  " + recommendation.getCondition() + " \n");
                        Log.i(TAG, "DESCRIPTION  =  " + recommendation.getDescription() + " \n");
                        Log.i(TAG, "HIERARCHY  =  " + recommendation.getHierarchy() + " \n");
                        Log.i(TAG, "INCOME0  =  " + recommendation.getIncome0() + " \n");
                        Log.i(TAG, "INCOME1  =  " + recommendation.getIncome1() + " \n");
                        Log.i(TAG, "INCOME2  =  " + recommendation.getIncome2() + " \n");
                        Log.i(TAG, "INCOME3  =  " + recommendation.getIncome3() + " \n");
                        Log.i(TAG, "INCOME4  =  " + recommendation.getIncome4() + " \n");
                        Log.i(TAG, "INCOME5  =  " + recommendation.getIncome5() + " \n");
                        Log.i(TAG, "INCOME6  =  " + recommendation.getIncome6() + " \n");
                        Log.i(TAG, "INCOME7  =  " + recommendation.getIncome7() + " \n");
                        Log.i(TAG, "LOGIC  =  " + recommendation.getLogicId() + " \n");
                        Log.i(TAG, "RELATED_1  =  " + recommendation.getRelatedOne() + " \n");
                        Log.i(TAG, "RELATED_2  =  " + recommendation.getRelatedTwo() + " \n");
                        Log.i(TAG, "YEAR_BACK_TO_GAPS  =  " + recommendation.getYearBackToGAPs() + " \n");
*/

                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return recommendation;
    }



    public List<Recommendation> sortRecommendations(List<Recommendation> recommendations) {

        if (recommendations != null)

            Collections.sort(recommendations, new Comparator<Recommendation>() {
                @Override
                public int compare(Recommendation o, Recommendation t1) {

                    Integer value1 = 0, value2 = 0;


                    try {

                        value1 = Integer.parseInt(o.getHierarchy());
                        value2 = Integer.parseInt(t1.getHierarchy());

                        return value1.compareTo(value2);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return -1;

                }
            });

        return recommendations;

    }


    List<Village> sortVillagesInAscendingOrder(List<Village> objects) {

        if (objects != null)

            Collections.sort(objects, new Comparator<Village>() {
                @Override
                public int compare(Village o, Village t1) {

                    try {
                        return o.getName().compareTo(t1.getName());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return -1;

                }
            });

        return objects;

    }

    List<String> sortVillagesByNameInAscendingOrder(List<String> objects) {

        if (objects != null)

            Collections.sort(objects, new Comparator<String>() {
                @Override
                public int compare(String o, String t1) {

                    try {
                        return o.compareTo(t1);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return -1;

                }
            });

        return objects;

    }


    List<Form> sortFormsInAscendingOrder(List<Form> objects) {

        if (objects != null)

            Collections.sort(objects, new Comparator<Form>() {
                @Override
                public int compare(Form o, Form t1) {

                    try {
                        return o.getDiaplayOrder().compareTo(t1.getDiaplayOrder());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return -1;

                }
            });

        return objects;

    }


    List<RealFarmer> sortFarmersByNameInAscendingOrder(List<RealFarmer> objects) {

        if (objects != null)

            Collections.sort(objects, new Comparator<RealFarmer>() {
                @Override
                public int compare(RealFarmer o, RealFarmer t1) {
                    try {
                        return o.getFarmerName().compareTo(t1.getFarmerName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return -1;
                }
            });

        return objects;

    }


    public boolean addRecommendationPlusAcivity(RecommendationsPlusActivity recommendation) {
        try {

            ContentValues contentValues = new ContentValues();
            contentValues.put(LAST_MODIFIED_DATE, recommendation.getLastModifiedDate());

            contentValues.put(ID, recommendation.getId());
            contentValues.put(SEASONAL, recommendation.getSeasonal());

            contentValues.put(RECOMMENDATIONS_PLUS_ACTIVITIES_NAME, recommendation.getName());
            contentValues.put(RECOMMENDATIONS_PLUS_ACTIVITIES_ACTIVITY_ID, recommendation.getActivityId());
            contentValues.put(RECOMMENDATIONS_PLUS_ACTIVITIES_ACTIVITY_NAME, recommendation.getActivityName());

            contentValues.put(RECOMMENDATIONS_PLUS_ACTIVITIES_LABOR_COST, recommendation.getLaborCost());
            contentValues.put(RECOMMENDATIONS_PLUS_ACTIVITIES_LABOR_DAYS_NEEDED, recommendation.getLaborDaysNeeded());
            contentValues.put(RECOMMENDATIONS_PLUS_ACTIVITIES_MONTH, recommendation.getMonth());
            contentValues.put(RECOMMENDATIONS_PLUS_ACTIVITIES_YEAR, recommendation.getYear());
            contentValues.put(RECOMMENDATIONS_PLUS_ACTIVITIES_SUPPLIES_COST, recommendation.getSuppliesCost());
            contentValues.put(RECOMMENDATIONS_PLUS_ACTIVITIES_RECOMMENDATION_ID, recommendation.getRecommendationId());

            db.insert(RECOMMENDATIONS_PLUS_ACTIVITIES_TABLE, null, contentValues);

            Log.i(TAG, "NAME  =  " + recommendation.getName() + " WITH SEASONAL " + recommendation.getSeasonal());



        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    public boolean deleteRecommendationPlusAcivity(String id) {

        return db.delete(RECOMMENDATIONS_PLUS_ACTIVITIES_TABLE, ID + " = ? ", new String[]{id}) > 0;

    }


    public RecommendationsPlusActivity getRecommendationPlusAcivity(String id) {

        RecommendationsPlusActivity recommendation = null;
        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + RECOMMENDATIONS_PLUS_ACTIVITIES_TABLE + " WHERE " +
                    ID + " ='" + id + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        recommendation = new RecommendationsPlusActivity();

                        recommendation.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));

                        recommendation.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        recommendation.setActivityId(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_ACTIVITY_ID)));
                        recommendation.setName(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_NAME)));

                        recommendation.setLaborCost(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_LABOR_COST)));
                        recommendation.setLaborDaysNeeded(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_LABOR_DAYS_NEEDED)));
                        recommendation.setMonth(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_MONTH)));
                        recommendation.setYear(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_YEAR)));
                        recommendation.setSuppliesCost(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_SUPPLIES_COST)));
                        recommendation.setRecommendationId(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_RECOMMENDATION_ID)));

/*

                        Log.i(TAG, "RECOMMENDATION + ACTIVITY WITH ID " + recommendation.getId() + " \nDATA IS \n\n");
                        Log.i(TAG, "NAME  =  " + recommendation.getName() + " \n");

                        Log.i(TAG, "AVTIVITY ID  =  " + recommendation.getActivityId() + " \n");
                        Log.i(TAG, "LABOR COST  =  " + recommendation.getLaborCost() + " \n");
                        Log.i(TAG, "LABOUR DAYS NEEDED  =  " + recommendation.getLaborDaysNeeded() + " \n");
                        Log.i(TAG, "MONTH  =  " + recommendation.getMonth() + " \n");
                        Log.i(TAG, "YEAR  =  " + recommendation.getYear() + " \n");
                        Log.i(TAG, "SUPPLIES COST  =  " + recommendation.getSuppliesCost() + " \n");
                        Log.i(TAG, "RECOMMENDATION ID  =  " + recommendation.getRecommendationId() + " \n\n");
*/


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return recommendation;


    }

    public List<RecommendationsPlusActivity> getRecommendationPlusAcivityByRecommendationIdMonthAndYear(String recommendationId, String month, String year) {
        List<RecommendationsPlusActivity> recommendationsPlusActivityList = new ArrayList<>();
        RecommendationsPlusActivity recommendation = null;
        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + RECOMMENDATIONS_PLUS_ACTIVITIES_TABLE + " WHERE "
                    + RECOMMENDATIONS_PLUS_ACTIVITIES_RECOMMENDATION_ID + " ='" + recommendationId + "' AND "
                    + RECOMMENDATIONS_PLUS_ACTIVITIES_MONTH + " ='" + month + "' AND "
                    + RECOMMENDATIONS_PLUS_ACTIVITIES_YEAR + " ='" + year + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        recommendation = new RecommendationsPlusActivity();

                        recommendation.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));
                        recommendation.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        recommendation.setName(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_NAME)));

                        recommendation.setActivityId(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_ACTIVITY_ID)));
                        recommendation.setActivityName(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_ACTIVITY_NAME)));

                        recommendation.setLaborCost(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_LABOR_COST)));
                        recommendation.setLaborDaysNeeded(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_LABOR_DAYS_NEEDED)));
                        recommendation.setMonth(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_MONTH)));
                        recommendation.setYear(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_YEAR)));
                        recommendation.setSuppliesCost(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_SUPPLIES_COST)));
                        recommendation.setRecommendationId(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_RECOMMENDATION_ID)));

/*

                        Log.i(TAG, "RECOMMENDATION + ACTIVITY WITH ID " + recommendation.getId() + " \nDATA IS \n\n");
                        Log.i(TAG, "NAME  =  " + recommendation.getName() + " \n");

                        Log.i(TAG, "AVTIVITY ID  =  " + recommendation.getActivityId() + " \n");
                        Log.i(TAG, "AVTIVITY NAME  =  " + recommendation.getActivityName() + " \n");

                        Log.i(TAG, "LABOR COST  =  " + recommendation.getLaborCost() + " \n");
                        Log.i(TAG, "LABOUR DAYS NEEDED  =  " + recommendation.getLaborDaysNeeded() + " \n");
                        Log.i(TAG, "MONTH  =  " + recommendation.getMonth() + " \n");
                        Log.i(TAG, "YEAR  =  " + recommendation.getYear() + " \n");
                        Log.i(TAG, "SUPPLIES COST  =  " + recommendation.getSuppliesCost() + " \n");
                        Log.i(TAG, "RECOMMENDATION ID  =  " + recommendation.getRecommendationId() + " \n\n");
*/


                        recommendationsPlusActivityList.add(recommendation);

                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return recommendationsPlusActivityList;


    }


    public List<RecommendationsPlusActivity> getSeasonalRecommendationPlusAcivityByRecommendationIdMonthAndYear(String recommendationId, String month, String year, String labourType) {
        List<RecommendationsPlusActivity> recommendationsPlusActivityList = new ArrayList<>();
        RecommendationsPlusActivity recommendation = null;
        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + RECOMMENDATIONS_PLUS_ACTIVITIES_TABLE + " WHERE "
                    + RECOMMENDATIONS_PLUS_ACTIVITIES_RECOMMENDATION_ID + " ='" + recommendationId + "' AND "
                    + SEASONAL + " ='" + labourType + "' AND "
                    + RECOMMENDATIONS_PLUS_ACTIVITIES_MONTH + " ='" + month + "' AND "
                    + RECOMMENDATIONS_PLUS_ACTIVITIES_YEAR + " ='" + year + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        recommendation = new RecommendationsPlusActivity();

                        recommendation.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));
                        recommendation.setSeasonal(cursor.getString(cursor.getColumnIndex(SEASONAL)));

                        recommendation.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        recommendation.setName(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_NAME)));

                        recommendation.setActivityId(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_ACTIVITY_ID)));
                        recommendation.setActivityName(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_ACTIVITY_NAME)));

                        recommendation.setLaborCost(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_LABOR_COST)));
                        recommendation.setLaborDaysNeeded(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_LABOR_DAYS_NEEDED)));
                        recommendation.setMonth(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_MONTH)));
                        recommendation.setYear(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_YEAR)));
                        recommendation.setSuppliesCost(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_SUPPLIES_COST)));
                        recommendation.setRecommendationId(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_RECOMMENDATION_ID)));

/*
                        Log.i(TAG, "RECOMMENDATION + ACTIVITY WITH ID " + recommendation.getId() + " \nDATA IS \n\n");
                        Log.i(TAG, "NAME  =  " + recommendation.getName() + " \n");

                        Log.i(TAG, "AVTIVITY ID  =  " + recommendation.getActivityId() + " \n");
                        Log.i(TAG, "AVTIVITY NAME  =  " + recommendation.getActivityName() + " \n");

                        Log.i(TAG, "LABOR COST  =  " + recommendation.getLaborCost() + " \n");
                        Log.i(TAG, "LABOUR DAYS NEEDED  =  " + recommendation.getLaborDaysNeeded() + " \n");
                        Log.i(TAG, "MONTH  =  " + recommendation.getMonth() + " \n");
                        Log.i(TAG, "YEAR  =  " + recommendation.getYear() + " \n");
                        Log.i(TAG, "SUPPLIES COST  =  " + recommendation.getSuppliesCost() + " \n");
                        Log.i(TAG, "RECOMMENDATION ID  =  " + recommendation.getRecommendationId() + " \n\n");


                     */
                        recommendationsPlusActivityList.add(recommendation);

                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return recommendationsPlusActivityList;


    }

    public List<RecommendationsPlusActivity> getAllRecommendationPlusAcivityByYear(String year) {

        List<RecommendationsPlusActivity> recommendationsPlusActivities = new ArrayList<>();

        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + RECOMMENDATIONS_PLUS_ACTIVITIES_TABLE + " WHERE " +
                    RECOMMENDATIONS_PLUS_ACTIVITIES_YEAR + " ='" + year + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        RecommendationsPlusActivity recommendation = new RecommendationsPlusActivity();

                        recommendation.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));

                        recommendation.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        recommendation.setSeasonal(cursor.getString(cursor.getColumnIndex(SEASONAL)));

                        recommendation.setName(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_NAME)));
                        recommendation.setActivityName(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_ACTIVITY_NAME)));

                        recommendation.setActivityId(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_ACTIVITY_ID)));
                        recommendation.setLaborCost(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_LABOR_COST)));
                        recommendation.setLaborDaysNeeded(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_LABOR_DAYS_NEEDED)));
                        recommendation.setMonth(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_MONTH)));
                        recommendation.setYear(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_YEAR)));
                        recommendation.setSuppliesCost(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_SUPPLIES_COST)));
                        recommendation.setRecommendationId(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_RECOMMENDATION_ID)));

/*

                        Log.i(TAG, "RECOMMENDATION + ACTIVITY WITH ID " + recommendation.getId() + " \nDATA IS \n\n");
                        Log.i(TAG, "NAME  =  " + recommendation.getName() + " \n");

                        Log.i(TAG, "YEAR  =  " + recommendation.getYear() + " \n");

*/

                        recommendationsPlusActivities.add(recommendation);


                    } while (cursor.moveToNext());

            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return recommendationsPlusActivities;


    }


    public List<RecommendationsPlusActivity> getAllRecommendationPlusAcivity() {

        List<RecommendationsPlusActivity> recommendationsPlusActivities = new ArrayList<>();

        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + RECOMMENDATIONS_PLUS_ACTIVITIES_TABLE;

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        RecommendationsPlusActivity recommendation = new RecommendationsPlusActivity();

                        recommendation.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));

                        recommendation.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        recommendation.setSeasonal(cursor.getString(cursor.getColumnIndex(SEASONAL)));

                        recommendation.setName(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_NAME)));
                        recommendation.setActivityId(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_ACTIVITY_ID)));
                        recommendation.setActivityName(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_ACTIVITY_NAME)));
                        recommendation.setLaborCost(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_LABOR_COST)));
                        recommendation.setLaborDaysNeeded(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_LABOR_DAYS_NEEDED)));
                        recommendation.setMonth(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_MONTH)));
                        recommendation.setYear(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_YEAR)));
                        recommendation.setSuppliesCost(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_SUPPLIES_COST)));
                        recommendation.setRecommendationId(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_RECOMMENDATION_ID)));
/*


                        Log.i(TAG, "RECOMMENDATION + ACTIVITY WITH ID " + recommendation.getId() + " \nDATA IS \n\n");
                        Log.i(TAG, "NAME  =  " + recommendation.getName() + " \n");

                        Log.i(TAG, "YEAR  =  " + recommendation.getYear() + " \n");

*/

                        recommendationsPlusActivities.add(recommendation);


                    } while (cursor.moveToNext());

            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return recommendationsPlusActivities;


    }


    private List<LaborDaysLaborCost> getAllLaborDaysLaborCostByYear(String year, String recommendationId) {

        List<LaborDaysLaborCost> laborDaysLaborCost = new ArrayList<>();

        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + RECOMMENDATIONS_PLUS_ACTIVITIES_TABLE + " WHERE "
                    + RECOMMENDATIONS_PLUS_ACTIVITIES_YEAR + " ='" + year + "' AND "
                    + RECOMMENDATIONS_PLUS_ACTIVITIES_RECOMMENDATION_ID + " ='" + recommendationId + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        LaborDaysLaborCost lls = new LaborDaysLaborCost();

                        lls.setLaborCost(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_LABOR_COST)));
                        lls.setLaborDays(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_LABOR_DAYS_NEEDED)));

                        laborDaysLaborCost.add(lls);

                    } while (cursor.moveToNext());

            }
        } catch (Exception e) {
            e.printStackTrace();
            return laborDaysLaborCost;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return laborDaysLaborCost;


    }

    private List<LaborDaysLaborCost> getAllSeasonalLaborDaysLaborCostByYear(String year, String recommendationId, String labourType) {

        List<LaborDaysLaborCost> laborDaysLaborCost = new ArrayList<>();

        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + RECOMMENDATIONS_PLUS_ACTIVITIES_TABLE + " WHERE "
                    + SEASONAL + " ='" + labourType + "' AND "

                    + RECOMMENDATIONS_PLUS_ACTIVITIES_YEAR + " ='" + year + "' AND "
                    + RECOMMENDATIONS_PLUS_ACTIVITIES_RECOMMENDATION_ID + " ='" + recommendationId + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        LaborDaysLaborCost lls = new LaborDaysLaborCost();

                        lls.setLaborCost(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_LABOR_COST)));
                        lls.setLaborDays(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_LABOR_DAYS_NEEDED)));

                        laborDaysLaborCost.add(lls);

                    } while (cursor.moveToNext());

            }
        } catch (Exception e) {
            e.printStackTrace();
            return laborDaysLaborCost;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return laborDaysLaborCost;


    }


    private List<SuppliesCost> getAllSuppliesCostByYear(String year, String recommendationId) {

        List<SuppliesCost> laborDaysLaborCost = new ArrayList<>();

        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + RECOMMENDATIONS_PLUS_ACTIVITIES_TABLE + " WHERE " +
                    RECOMMENDATIONS_PLUS_ACTIVITIES_YEAR + " ='" + year + "' AND " + RECOMMENDATIONS_PLUS_ACTIVITIES_RECOMMENDATION_ID + " ='" + recommendationId + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        SuppliesCost lls = new SuppliesCost();

                        lls.setSuppliesCost(cursor.getString(cursor.getColumnIndex(RECOMMENDATIONS_PLUS_ACTIVITIES_SUPPLIES_COST)));

                        laborDaysLaborCost.add(lls);

                    } while (cursor.moveToNext());

            }
        } catch (Exception e) {
            e.printStackTrace();
            return laborDaysLaborCost;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return laborDaysLaborCost;


    }




    public LaborDaysLaborCost getTotalLaborDaysLaborCostByYear(String year, String recommendationId) {

        LaborDaysLaborCost laborDaysLaborCostSupplies = new LaborDaysLaborCost();
        List<LaborDaysLaborCost> laborDaysLaborCostSuppliesList = getAllLaborDaysLaborCostByYear(year, recommendationId);

        StringBuilder totalLaborDays = new StringBuilder();
        StringBuilder totalLaborCost = new StringBuilder();

        try {


            for (LaborDaysLaborCost lls : laborDaysLaborCostSuppliesList) {

                totalLaborDays.append(lls.getLaborDays()).append("+");
                totalLaborCost.append(lls.getLaborCost()).append("+");

            }

            totalLaborDays.append("0");
            totalLaborCost.append("0");


            laborDaysLaborCostSupplies.setLaborCost(totalLaborCost.toString());
            laborDaysLaborCostSupplies.setLaborDays(totalLaborDays.toString());


        } catch (Exception e) {
            e.printStackTrace();
            return laborDaysLaborCostSupplies;

        }

        return laborDaysLaborCostSupplies;


    }


    public LaborDaysLaborCost getTotalSeasonalLaborDaysLaborCostByYear(String year, String recommendationId, String labourType) {

        LaborDaysLaborCost laborDaysLaborCostSupplies = new LaborDaysLaborCost();
        List<LaborDaysLaborCost> laborDaysLaborCostSuppliesList = getAllSeasonalLaborDaysLaborCostByYear(year, recommendationId, labourType);

        StringBuilder totalLaborDays = new StringBuilder();
        StringBuilder totalLaborCost = new StringBuilder();

        try {


            for (LaborDaysLaborCost lls : laborDaysLaborCostSuppliesList) {

                totalLaborDays.append(lls.getLaborDays()).append("+");
                totalLaborCost.append(lls.getLaborCost()).append("+");

            }

            totalLaborDays.append("0");
            totalLaborCost.append("0");


            laborDaysLaborCostSupplies.setLaborCost(totalLaborCost.toString());
            laborDaysLaborCostSupplies.setLaborDays(totalLaborDays.toString());


        } catch (Exception e) {
            e.printStackTrace();
            return laborDaysLaborCostSupplies;

        }

        return laborDaysLaborCostSupplies;


    }


    public SuppliesCost getTotalSuppliesCostByYear(String year, String recommendationId) {

        SuppliesCost laborDaysLaborCostSupplies = new SuppliesCost();
        List<SuppliesCost> laborDaysLaborCostSuppliesList = getAllSuppliesCostByYear(year, recommendationId);

        StringBuilder totalSuppliesCost = new StringBuilder();

        try {


            for (SuppliesCost lls : laborDaysLaborCostSuppliesList) {

                totalSuppliesCost.append(lls.getSuppliesCost()).append("+");

            }

            totalSuppliesCost.append("0");


            laborDaysLaborCostSupplies.setSuppliesCost(totalSuppliesCost.toString());


        } catch (Exception e) {
            e.printStackTrace();
            return laborDaysLaborCostSupplies;

        }

        return laborDaysLaborCostSupplies;


    }






    public boolean addLogic(Logic logic) {
        try {


            ContentValues contentValues = new ContentValues();
            contentValues.put(LAST_MODIFIED_DATE, logic.getLastModifiedDate());

            contentValues.put(ID, logic.getId());
            contentValues.put(LOGIC_NAME, logic.getName());
            contentValues.put(LOGIC_PARENT_LOGIC, logic.getParentLogic());
            contentValues.put(LOGIC_PARENT_LOGICAL_OPERATOR, logic.getParentLogicalOperator());
            contentValues.put(LOGIC_RESULT, logic.getResult());
            contentValues.put(LOGIC_RESULT_QUESTIONS, logic.getResultQuestions());

            contentValues.put(LOGIC_LO1, logic.getLogicalOperator1());
            contentValues.put(LOGIC_LO2, logic.getLogicalOperator2());
            contentValues.put(LOGIC_LO3, logic.getLogicalOperator3());
            contentValues.put(LOGIC_LO4, logic.getLogicalOperator4());
            contentValues.put(LOGIC_LO5, logic.getLogicalOperator5());
            contentValues.put(LOGIC_LO6, logic.getLogicalOperator6());
            contentValues.put(LOGIC_LO7, logic.getLogicalOperator7());
            contentValues.put(LOGIC_LO8, logic.getLogicalOperator8());
            contentValues.put(LOGIC_LO9, logic.getLogicalOperator9());
            contentValues.put(LOGIC_LO10, logic.getLogicalOperator10());

            contentValues.put(LOGIC_Q1, logic.getQuestion1());
            contentValues.put(LOGIC_Q2, logic.getQuestion2());
            contentValues.put(LOGIC_Q3, logic.getQuestion3());
            contentValues.put(LOGIC_Q4, logic.getQuestion4());
            contentValues.put(LOGIC_Q5, logic.getQuestion5());
            contentValues.put(LOGIC_Q6, logic.getQuestion6());
            contentValues.put(LOGIC_Q7, logic.getQuestion7());
            contentValues.put(LOGIC_Q8, logic.getQuestion8());
            contentValues.put(LOGIC_Q9, logic.getQuestion9());
            contentValues.put(LOGIC_Q10, logic.getQuestion10());

            contentValues.put(LOGIC_QLO1, logic.getQuestionLogicOperation1());
            contentValues.put(LOGIC_QLO2, logic.getQuestionLogicOperation2());
            contentValues.put(LOGIC_QLO3, logic.getQuestionLogicOperation3());
            contentValues.put(LOGIC_QLO4, logic.getQuestionLogicOperation4());
            contentValues.put(LOGIC_QLO5, logic.getQuestionLogicOperation5());
            contentValues.put(LOGIC_QLO6, logic.getQuestionLogicOperation6());
            contentValues.put(LOGIC_QLO7, logic.getQuestionLogicOperation7());
            contentValues.put(LOGIC_QLO8, logic.getQuestionLogicOperation8());
            contentValues.put(LOGIC_QLO9, logic.getQuestionLogicOperation9());
            contentValues.put(LOGIC_QLO10, logic.getQuestionLogicOperation10());


            contentValues.put(LOGIC_V1, logic.getValue1());
            contentValues.put(LOGIC_V2, logic.getValue2());
            contentValues.put(LOGIC_V3, logic.getValue3());
            contentValues.put(LOGIC_V4, logic.getValue4());
            contentValues.put(LOGIC_V5, logic.getValue5());
            contentValues.put(LOGIC_V6, logic.getValue6());
            contentValues.put(LOGIC_V7, logic.getValue7());
            contentValues.put(LOGIC_V8, logic.getValue8());
            contentValues.put(LOGIC_V9, logic.getValue9());
            contentValues.put(LOGIC_V10, logic.getValue10());
            contentValues.put(LOGIC_EVALUATED_VALUE, logic.getEvaluatedValue());


            db.insert(LOGIC_TABLE, null, contentValues);
            Log.d(TAG, "LOGIC WITH ID " + logic.getId() + " INSERTED");

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    public boolean deleteLogic(String id) {

        return db.delete(LOGIC_TABLE, ID + " = ? ", new String[]{id}) > 0;

    }

    /*public boolean deleteLogicsTable() {

        try {


            db.execSQL("DELETE FROM " + LOGIC_TABLE);

            Log.i("DATABASE", "LOGICS TABLE DELETED");

            return true;
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }
    }*/

    public Logic getLogic(String id) {

        Logic logic = new Logic();
        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + LOGIC_TABLE + " WHERE " +
                    ID + " ='" + id + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {

                        logic.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));

                        logic.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        logic.setName(cursor.getString(cursor.getColumnIndex(LOGIC_NAME)));
                        logic.setParentLogic(cursor.getString(cursor.getColumnIndex(LOGIC_PARENT_LOGIC)));
                        logic.setParentLogicalOperator(cursor.getString(cursor.getColumnIndex(LOGIC_PARENT_LOGICAL_OPERATOR)));
                        logic.setResult(cursor.getString(cursor.getColumnIndex(LOGIC_RESULT)));
                        logic.setResultQuestions(cursor.getString(cursor.getColumnIndex(LOGIC_RESULT_QUESTIONS)));

                        logic.setLogicalOperator1(cursor.getString(cursor.getColumnIndex(LOGIC_LO1)));
                        logic.setLogicalOperator2(cursor.getString(cursor.getColumnIndex(LOGIC_LO2)));
                        logic.setLogicalOperator3(cursor.getString(cursor.getColumnIndex(LOGIC_LO3)));
                        logic.setLogicalOperator4(cursor.getString(cursor.getColumnIndex(LOGIC_LO4)));
                        logic.setLogicalOperator5(cursor.getString(cursor.getColumnIndex(LOGIC_LO5)));
                        logic.setLogicalOperator6(cursor.getString(cursor.getColumnIndex(LOGIC_LO6)));
                        logic.setLogicalOperator7(cursor.getString(cursor.getColumnIndex(LOGIC_LO7)));
                        logic.setLogicalOperator8(cursor.getString(cursor.getColumnIndex(LOGIC_LO8)));
                        logic.setLogicalOperator9(cursor.getString(cursor.getColumnIndex(LOGIC_LO9)));
                        logic.setLogicalOperator10(cursor.getString(cursor.getColumnIndex(LOGIC_LO10)));


                        logic.setQuestion1(cursor.getString(cursor.getColumnIndex(LOGIC_Q1)));
                        logic.setQuestion2(cursor.getString(cursor.getColumnIndex(LOGIC_Q2)));
                        logic.setQuestion3(cursor.getString(cursor.getColumnIndex(LOGIC_Q3)));
                        logic.setQuestion4(cursor.getString(cursor.getColumnIndex(LOGIC_Q4)));
                        logic.setQuestion5(cursor.getString(cursor.getColumnIndex(LOGIC_Q5)));
                        logic.setQuestion6(cursor.getString(cursor.getColumnIndex(LOGIC_Q6)));
                        logic.setQuestion7(cursor.getString(cursor.getColumnIndex(LOGIC_Q7)));
                        logic.setQuestion8(cursor.getString(cursor.getColumnIndex(LOGIC_Q8)));
                        logic.setQuestion9(cursor.getString(cursor.getColumnIndex(LOGIC_Q9)));
                        logic.setQuestion10(cursor.getString(cursor.getColumnIndex(LOGIC_Q10)));


                        logic.setQuestionLogicOperation1(cursor.getString(cursor.getColumnIndex(LOGIC_QLO1)));
                        logic.setQuestionLogicOperation2(cursor.getString(cursor.getColumnIndex(LOGIC_QLO2)));
                        logic.setQuestionLogicOperation3(cursor.getString(cursor.getColumnIndex(LOGIC_QLO3)));
                        logic.setQuestionLogicOperation4(cursor.getString(cursor.getColumnIndex(LOGIC_QLO4)));
                        logic.setQuestionLogicOperation5(cursor.getString(cursor.getColumnIndex(LOGIC_QLO5)));
                        logic.setQuestionLogicOperation6(cursor.getString(cursor.getColumnIndex(LOGIC_QLO6)));
                        logic.setQuestionLogicOperation7(cursor.getString(cursor.getColumnIndex(LOGIC_QLO7)));
                        logic.setQuestionLogicOperation8(cursor.getString(cursor.getColumnIndex(LOGIC_QLO8)));
                        logic.setQuestionLogicOperation9(cursor.getString(cursor.getColumnIndex(LOGIC_QLO9)));
                        logic.setQuestionLogicOperation10(cursor.getString(cursor.getColumnIndex(LOGIC_QLO10)));

                        logic.setValue1(cursor.getString(cursor.getColumnIndex(LOGIC_V1)));
                        logic.setValue2(cursor.getString(cursor.getColumnIndex(LOGIC_V2)));
                        logic.setValue3(cursor.getString(cursor.getColumnIndex(LOGIC_V3)));
                        logic.setValue4(cursor.getString(cursor.getColumnIndex(LOGIC_V4)));
                        logic.setValue5(cursor.getString(cursor.getColumnIndex(LOGIC_V5)));
                        logic.setValue6(cursor.getString(cursor.getColumnIndex(LOGIC_V6)));
                        logic.setValue7(cursor.getString(cursor.getColumnIndex(LOGIC_V7)));
                        logic.setValue8(cursor.getString(cursor.getColumnIndex(LOGIC_V8)));
                        logic.setValue9(cursor.getString(cursor.getColumnIndex(LOGIC_V9)));
                        logic.setValue10(cursor.getString(cursor.getColumnIndex(LOGIC_V10)));
                        logic.setEvaluatedValue(cursor.getString(cursor.getColumnIndex(LOGIC_EVALUATED_VALUE)));


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return logic;

    }

    public List<Logic> getLogics(String id) {

        List<Logic> logicList = new ArrayList<>();

        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + LOGIC_TABLE + " WHERE " +
                    LOGIC_RESULT_QUESTIONS + " ='" + id + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        Logic logic = new Logic();

                        logic.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        logic.setName(cursor.getString(cursor.getColumnIndex(LOGIC_NAME)));
                        logic.setParentLogic(cursor.getString(cursor.getColumnIndex(LOGIC_PARENT_LOGIC)));
                        logic.setParentLogicalOperator(cursor.getString(cursor.getColumnIndex(LOGIC_PARENT_LOGICAL_OPERATOR)));
                        logic.setResult(cursor.getString(cursor.getColumnIndex(LOGIC_RESULT)));
                        logic.setResultQuestions(cursor.getString(cursor.getColumnIndex(LOGIC_RESULT_QUESTIONS)));

                        logic.setLogicalOperator1(cursor.getString(cursor.getColumnIndex(LOGIC_LO1)));
                        logic.setLogicalOperator2(cursor.getString(cursor.getColumnIndex(LOGIC_LO2)));
                        logic.setLogicalOperator3(cursor.getString(cursor.getColumnIndex(LOGIC_LO3)));
                        logic.setLogicalOperator4(cursor.getString(cursor.getColumnIndex(LOGIC_LO4)));
                        logic.setLogicalOperator5(cursor.getString(cursor.getColumnIndex(LOGIC_LO5)));
                        logic.setLogicalOperator6(cursor.getString(cursor.getColumnIndex(LOGIC_LO6)));
                        logic.setLogicalOperator7(cursor.getString(cursor.getColumnIndex(LOGIC_LO7)));
                        logic.setLogicalOperator8(cursor.getString(cursor.getColumnIndex(LOGIC_LO8)));
                        logic.setLogicalOperator9(cursor.getString(cursor.getColumnIndex(LOGIC_LO9)));
                        logic.setLogicalOperator10(cursor.getString(cursor.getColumnIndex(LOGIC_LO10)));


                        logic.setQuestion1(cursor.getString(cursor.getColumnIndex(LOGIC_Q1)));
                        logic.setQuestion2(cursor.getString(cursor.getColumnIndex(LOGIC_Q2)));
                        logic.setQuestion3(cursor.getString(cursor.getColumnIndex(LOGIC_Q3)));
                        logic.setQuestion4(cursor.getString(cursor.getColumnIndex(LOGIC_Q4)));
                        logic.setQuestion5(cursor.getString(cursor.getColumnIndex(LOGIC_Q5)));
                        logic.setQuestion6(cursor.getString(cursor.getColumnIndex(LOGIC_Q6)));
                        logic.setQuestion7(cursor.getString(cursor.getColumnIndex(LOGIC_Q7)));
                        logic.setQuestion8(cursor.getString(cursor.getColumnIndex(LOGIC_Q8)));
                        logic.setQuestion9(cursor.getString(cursor.getColumnIndex(LOGIC_Q9)));
                        logic.setQuestion10(cursor.getString(cursor.getColumnIndex(LOGIC_Q10)));


                        logic.setQuestionLogicOperation1(cursor.getString(cursor.getColumnIndex(LOGIC_QLO1)));
                        logic.setQuestionLogicOperation2(cursor.getString(cursor.getColumnIndex(LOGIC_QLO2)));
                        logic.setQuestionLogicOperation3(cursor.getString(cursor.getColumnIndex(LOGIC_QLO3)));
                        logic.setQuestionLogicOperation4(cursor.getString(cursor.getColumnIndex(LOGIC_QLO4)));
                        logic.setQuestionLogicOperation5(cursor.getString(cursor.getColumnIndex(LOGIC_QLO5)));
                        logic.setQuestionLogicOperation6(cursor.getString(cursor.getColumnIndex(LOGIC_QLO6)));
                        logic.setQuestionLogicOperation7(cursor.getString(cursor.getColumnIndex(LOGIC_QLO7)));
                        logic.setQuestionLogicOperation8(cursor.getString(cursor.getColumnIndex(LOGIC_QLO8)));
                        logic.setQuestionLogicOperation9(cursor.getString(cursor.getColumnIndex(LOGIC_QLO9)));
                        logic.setQuestionLogicOperation10(cursor.getString(cursor.getColumnIndex(LOGIC_QLO10)));

                        logic.setValue1(cursor.getString(cursor.getColumnIndex(LOGIC_V1)));
                        logic.setValue2(cursor.getString(cursor.getColumnIndex(LOGIC_V2)));
                        logic.setValue3(cursor.getString(cursor.getColumnIndex(LOGIC_V3)));
                        logic.setValue4(cursor.getString(cursor.getColumnIndex(LOGIC_V4)));
                        logic.setValue5(cursor.getString(cursor.getColumnIndex(LOGIC_V5)));
                        logic.setValue6(cursor.getString(cursor.getColumnIndex(LOGIC_V6)));
                        logic.setValue7(cursor.getString(cursor.getColumnIndex(LOGIC_V7)));
                        logic.setValue8(cursor.getString(cursor.getColumnIndex(LOGIC_V8)));
                        logic.setValue9(cursor.getString(cursor.getColumnIndex(LOGIC_V9)));
                        logic.setValue10(cursor.getString(cursor.getColumnIndex(LOGIC_V10)));
                        logic.setEvaluatedValue(cursor.getString(cursor.getColumnIndex(LOGIC_EVALUATED_VALUE)));

                        logicList.add(logic);

                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return logicList;

    }

    public boolean editLogicEvaluatedValue(String logicId, String newValue) {
        try {

            ContentValues contentValues = new ContentValues();

            contentValues.put(LOGIC_EVALUATED_VALUE, newValue);


            db.update(LOGIC_TABLE, contentValues, ID + "= ?", new String[]{logicId});
            Log.d(TAG, "LOGIC WITH ID " + logicId + " EDITED WITH VALUE " + newValue);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }





    public List<Logic> doesQuestionHaveLogics(String questionId) {



        List<Logic> logics = null;

        Cursor cursor = null;

        try {

            logics = new ArrayList<>();

            String selectQuery = "SELECT  * FROM " + LOGIC_TABLE + " WHERE " +
                    LOGIC_RESULT_QUESTIONS + " ='" + questionId + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        Logic logic = new Logic();
                        logic.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        logic.setName(cursor.getString(cursor.getColumnIndex(LOGIC_NAME)));
                        logic.setParentLogic(cursor.getString(cursor.getColumnIndex(LOGIC_PARENT_LOGIC)));
                        logic.setParentLogicalOperator(cursor.getString(cursor.getColumnIndex(LOGIC_PARENT_LOGICAL_OPERATOR)));
                        logic.setResult(cursor.getString(cursor.getColumnIndex(LOGIC_RESULT)));
                        logic.setResultQuestions(cursor.getString(cursor.getColumnIndex(LOGIC_RESULT_QUESTIONS)));

                        logic.setLogicalOperator1(cursor.getString(cursor.getColumnIndex(LOGIC_LO1)));
                        logic.setLogicalOperator2(cursor.getString(cursor.getColumnIndex(LOGIC_LO2)));
                        logic.setLogicalOperator3(cursor.getString(cursor.getColumnIndex(LOGIC_LO3)));
                        logic.setLogicalOperator4(cursor.getString(cursor.getColumnIndex(LOGIC_LO4)));
                        logic.setLogicalOperator5(cursor.getString(cursor.getColumnIndex(LOGIC_LO5)));
                        logic.setLogicalOperator6(cursor.getString(cursor.getColumnIndex(LOGIC_LO6)));
                        logic.setLogicalOperator7(cursor.getString(cursor.getColumnIndex(LOGIC_LO7)));
                        logic.setLogicalOperator8(cursor.getString(cursor.getColumnIndex(LOGIC_LO8)));
                        logic.setLogicalOperator9(cursor.getString(cursor.getColumnIndex(LOGIC_LO9)));
                        logic.setLogicalOperator10(cursor.getString(cursor.getColumnIndex(LOGIC_LO10)));


                        logic.setQuestion1(cursor.getString(cursor.getColumnIndex(LOGIC_Q1)));
                        logic.setQuestion2(cursor.getString(cursor.getColumnIndex(LOGIC_Q2)));
                        logic.setQuestion3(cursor.getString(cursor.getColumnIndex(LOGIC_Q3)));
                        logic.setQuestion4(cursor.getString(cursor.getColumnIndex(LOGIC_Q4)));
                        logic.setQuestion5(cursor.getString(cursor.getColumnIndex(LOGIC_Q5)));
                        logic.setQuestion6(cursor.getString(cursor.getColumnIndex(LOGIC_Q6)));
                        logic.setQuestion7(cursor.getString(cursor.getColumnIndex(LOGIC_Q7)));
                        logic.setQuestion8(cursor.getString(cursor.getColumnIndex(LOGIC_Q8)));
                        logic.setQuestion9(cursor.getString(cursor.getColumnIndex(LOGIC_Q9)));
                        logic.setQuestion10(cursor.getString(cursor.getColumnIndex(LOGIC_Q10)));


                        logic.setQuestionLogicOperation1(cursor.getString(cursor.getColumnIndex(LOGIC_QLO1)));
                        logic.setQuestionLogicOperation2(cursor.getString(cursor.getColumnIndex(LOGIC_QLO2)));
                        logic.setQuestionLogicOperation3(cursor.getString(cursor.getColumnIndex(LOGIC_QLO3)));
                        logic.setQuestionLogicOperation4(cursor.getString(cursor.getColumnIndex(LOGIC_QLO4)));
                        logic.setQuestionLogicOperation5(cursor.getString(cursor.getColumnIndex(LOGIC_QLO5)));
                        logic.setQuestionLogicOperation6(cursor.getString(cursor.getColumnIndex(LOGIC_QLO6)));
                        logic.setQuestionLogicOperation7(cursor.getString(cursor.getColumnIndex(LOGIC_QLO7)));
                        logic.setQuestionLogicOperation8(cursor.getString(cursor.getColumnIndex(LOGIC_QLO8)));
                        logic.setQuestionLogicOperation9(cursor.getString(cursor.getColumnIndex(LOGIC_QLO9)));
                        logic.setQuestionLogicOperation10(cursor.getString(cursor.getColumnIndex(LOGIC_QLO10)));

                        logic.setValue1(cursor.getString(cursor.getColumnIndex(LOGIC_V1)));
                        logic.setValue2(cursor.getString(cursor.getColumnIndex(LOGIC_V2)));
                        logic.setValue3(cursor.getString(cursor.getColumnIndex(LOGIC_V3)));
                        logic.setValue4(cursor.getString(cursor.getColumnIndex(LOGIC_V4)));
                        logic.setValue5(cursor.getString(cursor.getColumnIndex(LOGIC_V5)));
                        logic.setValue6(cursor.getString(cursor.getColumnIndex(LOGIC_V6)));
                        logic.setValue7(cursor.getString(cursor.getColumnIndex(LOGIC_V7)));
                        logic.setValue8(cursor.getString(cursor.getColumnIndex(LOGIC_V8)));
                        logic.setValue9(cursor.getString(cursor.getColumnIndex(LOGIC_V9)));
                        logic.setValue10(cursor.getString(cursor.getColumnIndex(LOGIC_V10)));
                        logic.setEvaluatedValue(cursor.getString(cursor.getColumnIndex(LOGIC_EVALUATED_VALUE)));


                        Log.i(TAG, "QUESTION WITH ID " + questionId + " HAS A LOGIC WITH NAME " + logic.getName());

                        logics.add(logic);


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return logics;


    }








    public boolean addActivityPlusInput(ActivitiesPlusInputs activitiesPlusInputs) {


        try {

            ContentValues contentValues = new ContentValues();
            contentValues.put(LAST_MODIFIED_DATE, activitiesPlusInputs.getLastModifiedDate());

            contentValues.put(ID, activitiesPlusInputs.getId());
            contentValues.put(ACTIVITIY_PLUS_INPUTS_NAME, activitiesPlusInputs.getName());
            contentValues.put(ACTIVITIY_PLUS_INPUTS_INPUT_TYPE, activitiesPlusInputs.getInputType());
            contentValues.put(ACTIVITIY_PLUS_INPUTS_QUANTITY, activitiesPlusInputs.getQuantity());

            contentValues.put(ACTIVITIY_PLUS_INPUTS_UNIT_COST, activitiesPlusInputs.getInputId());
            contentValues.put(ACTIVITIY_PLUS_INPUTS_TOTAL_COST, activitiesPlusInputs.getTotalCost());
            contentValues.put(ACTIVITIY_PLUS_INPUTS_RECOMMENDATION_ID, activitiesPlusInputs.getRecommendationId());
            contentValues.put(ACTIVITIY_PLUS_INPUTS_RECOMMENDATION_NAME, activitiesPlusInputs.getRecommendationName());

            db.insert(ACTIVITIY_PLUS_INPUTS_TABLE, null, contentValues);
            Log.d(TAG, "ACTIVITY PLUS INPUT WITH ID " + activitiesPlusInputs.getId() + " INSERTED");
/*

            Log.i(TAG, "UNIT COST  =  " + activitiesPlusInputs.getInputId() + " \n");
            Log.i(TAG, "SI UNIT  =  " + activitiesPlusInputs.getInputSIUnit() + " \n");
*/


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    public boolean deleteActivityPlusInput(String id) {

        return db.delete(ACTIVITIY_PLUS_INPUTS_TABLE, ID + " = ? ", new String[]{id}) > 0;

    }

   /* public boolean deleteActivityPlusInputTable() {

        try {

            db.execSQL("DELETE FROM " + ACTIVITIY_PLUS_INPUTS_TABLE);


            Log.i("DATABASE", "ACTIVITIY_PLUS_INPUTS TABLE DELETED");

            return true;
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }
    }*/


    public ActivitiesPlusInputs getActivityPlusInput(String id) {

        ActivitiesPlusInputs activitiesPlusInputs = null;
        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + ACTIVITIY_PLUS_INPUTS_TABLE + " WHERE " +
                    ID + " ='" + id + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        activitiesPlusInputs = new ActivitiesPlusInputs();


                        activitiesPlusInputs.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        activitiesPlusInputs.setName(cursor.getString(cursor.getColumnIndex(ACTIVITIY_PLUS_INPUTS_NAME)));
                        activitiesPlusInputs.setInputType(cursor.getString(cursor.getColumnIndex(ACTIVITIY_PLUS_INPUTS_INPUT_TYPE)));

                        activitiesPlusInputs.setQuantity(cursor.getString(cursor.getColumnIndex(ACTIVITIY_PLUS_INPUTS_QUANTITY)));
                        activitiesPlusInputs.setInputId(cursor.getString(cursor.getColumnIndex(ACTIVITIY_PLUS_INPUTS_UNIT_COST)));
                        activitiesPlusInputs.setTotalCost(cursor.getString(cursor.getColumnIndex(ACTIVITIY_PLUS_INPUTS_TOTAL_COST)));
                        activitiesPlusInputs.setRecommendationId(cursor.getString(cursor.getColumnIndex(ACTIVITIY_PLUS_INPUTS_RECOMMENDATION_ID)));
                        activitiesPlusInputs.setRecommendationName(cursor.getString(cursor.getColumnIndex(ACTIVITIY_PLUS_INPUTS_RECOMMENDATION_NAME)));


                        Log.i(TAG, "ACTIVITY + INPUTS WITH ID " + id + " \nDATA IS \n\n");
                        Log.i(TAG, "NAME  =  " + activitiesPlusInputs.getName() + " \n");


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return activitiesPlusInputs;


    }


    public ActivitiesPlusInputs getActivityPlusInputByRecommendationId(String recommendationId) {

        ActivitiesPlusInputs activitiesPlusInputs = new ActivitiesPlusInputs();

        Cursor cursor = null;

        try {

            String selectQuery = "SELECT  * FROM " + ACTIVITIY_PLUS_INPUTS_TABLE + " WHERE " +
                    ACTIVITIY_PLUS_INPUTS_RECOMMENDATION_ID + " ='" + recommendationId + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {


                        activitiesPlusInputs.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        activitiesPlusInputs.setName(cursor.getString(cursor.getColumnIndex(ACTIVITIY_PLUS_INPUTS_NAME)));
                        activitiesPlusInputs.setInputType(cursor.getString(cursor.getColumnIndex(ACTIVITIY_PLUS_INPUTS_INPUT_TYPE)));
                        activitiesPlusInputs.setQuantity(cursor.getString(cursor.getColumnIndex(ACTIVITIY_PLUS_INPUTS_QUANTITY)));
                        activitiesPlusInputs.setInputId(cursor.getString(cursor.getColumnIndex(ACTIVITIY_PLUS_INPUTS_UNIT_COST)));
                        activitiesPlusInputs.setTotalCost(cursor.getString(cursor.getColumnIndex(ACTIVITIY_PLUS_INPUTS_TOTAL_COST)));
                        activitiesPlusInputs.setRecommendationId(cursor.getString(cursor.getColumnIndex(ACTIVITIY_PLUS_INPUTS_RECOMMENDATION_ID)));
                        activitiesPlusInputs.setRecommendationName(cursor.getString(cursor.getColumnIndex(ACTIVITIY_PLUS_INPUTS_RECOMMENDATION_NAME)));

                     /*   Log.i(TAG, "ACTIVITY + INPUTS WITH ID " + activitiesPlusInputs.getId() + " \nDATA IS \n\n");
                        Log.i(TAG, "NAME  =  " + activitiesPlusInputs.getName() + " \n");
*/

                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return activitiesPlusInputs;


    }


    public boolean addInput(Input activitiesPlusInputs) {
        try {

            ContentValues contentValues = new ContentValues();
            contentValues.put(LAST_MODIFIED_DATE, activitiesPlusInputs.getLastModifiedDate());

            contentValues.put(ID, activitiesPlusInputs.getId());
            contentValues.put(INPUTS_NAME, activitiesPlusInputs.getName());
            contentValues.put(INPUTS_COST, activitiesPlusInputs.getCost());
            contentValues.put(INPUTS_REGION, activitiesPlusInputs.getRegion());
            contentValues.put(INPUTS_INPUT_TYPE, activitiesPlusInputs.getType());
            contentValues.put(INPUTS_SI_UNIT, activitiesPlusInputs.getUnit());

            db.insert(INPUTS_TABLE, null, contentValues);
            Log.d(TAG, "INPUT WITH ID " + activitiesPlusInputs.getId() + " INSERTED");


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    public boolean deleteInput(String id) {

        return db.delete(INPUTS_TABLE, ID + " = ? ", new String[]{id}) > 0;

    }


    public Input getInput(String id) {

        Input activitiesPlusInputs = null;
        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + INPUTS_TABLE + " WHERE " +
                    ID + " ='" + id + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        activitiesPlusInputs = new Input();


                        activitiesPlusInputs.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        activitiesPlusInputs.setName(cursor.getString(cursor.getColumnIndex(INPUTS_NAME)));
                        activitiesPlusInputs.setCost(cursor.getString(cursor.getColumnIndex(INPUTS_COST)));

                        activitiesPlusInputs.setRegion(cursor.getString(cursor.getColumnIndex(INPUTS_REGION)));

                        activitiesPlusInputs.setType(cursor.getString(cursor.getColumnIndex(INPUTS_INPUT_TYPE)));
                        activitiesPlusInputs.setUnit(cursor.getString(cursor.getColumnIndex(INPUTS_SI_UNIT)));


                 /*       Log.i(TAG, "INPUTS WITH ID " + id + " \nDATA IS \n\n");
                        Log.i(TAG, "NAME  =  " + activitiesPlusInputs.getName() + " \n");

*/
                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return activitiesPlusInputs;


    }


    public long queryNumberOfEntries(String tableName) {
        return DatabaseUtils.queryNumEntries(db, tableName);
    }




    public boolean addPlotMonitoring(Monitoring monitoring, String year) {
        try {

            ContentValues contentValues = new ContentValues();
            contentValues.put(ID, monitoring.getId());
            contentValues.put(MONITORING_NAME, monitoring.getName());
            contentValues.put(MONITORING_JSON, monitoring.getJson());
            contentValues.put(MONITORING_PLOT_ID, monitoring.getPlotId());
            contentValues.put(MONITORING_YEAR, year);


            db.insert(MONITORING_TABLE, null, contentValues);
            Log.d(TAG, "INPUT WITH ID " + monitoring.getId() + " INSERTED FOR YEAR " + year);
            return true;


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deletePlotMonitoring(int id, String plotId, String year) {

        return db.delete(MONITORING_TABLE, ID + " = ? AND " + MONITORING_PLOT_ID + " = ? AND " + MONITORING_YEAR + " = ?", new String[]{String.valueOf(id), plotId, year}) > 0;

    }

    public boolean deletePlotMonitoring(String id) {

        return db.delete(MONITORING_TABLE, ID + " = ?", new String[]{String.valueOf(id)}) > 0;

    }

    public void deleteAllPlotMonitoring(String plotId) {

        List<Monitoring> monitoringList = getAllPlotMonitoring(plotId);

        for (Monitoring monitoring : monitoringList) {


            try {

                deletePlotMonitoring(monitoring.getId());
            } catch (Exception e) {
                e.printStackTrace();

            }
        }
    }


    public Monitoring getPlotMonitoring(String id, String plotId, String year) {

        Monitoring monitoring = null;
        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + MONITORING_TABLE + " WHERE " +
                    ID + " ='" + id + "' AND " + MONITORING_YEAR + " = '" + year + "' AND " + MONITORING_PLOT_ID + " = '" + plotId + "'";


            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        monitoring = new Monitoring();


                        monitoring.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        monitoring.setName(cursor.getString(cursor.getColumnIndex(MONITORING_NAME)));
                        monitoring.setYear(cursor.getString(cursor.getColumnIndex(MONITORING_YEAR)));
                        monitoring.setJson(cursor.getString(cursor.getColumnIndex(MONITORING_JSON)));
                        monitoring.setPlotId(cursor.getString(cursor.getColumnIndex(MONITORING_PLOT_ID)));



                        Log.i(TAG, "INPUTS WITH ID " + id + "\t");
                        Log.i(TAG, "NAME  =  " + monitoring.getName() + " \n");


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return monitoring;


    }


    public String getPlotMonitoringJson(int id, String plotId, String year) {

         Cursor cursor = null;
         String value = "";


        try {

            String selectQuery = "SELECT  * FROM " + MONITORING_TABLE + " WHERE " +
                    ID + " ='" + id + "' AND "  + MONITORING_PLOT_ID + " ='" + plotId
                    + "' AND " + MONITORING_YEAR + " = '" + year + "'";



            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {

                        value = cursor.getString(cursor.getColumnIndex(MONITORING_JSON));


                        Log.i(TAG, "Monitoring Json is " + value + "\t");


                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return value;


    }


    public List<Monitoring> getAllPlotMonitoringForYear(String plotId, String year) {

        List<Monitoring> monitoringList = new ArrayList<>();

        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + MONITORING_TABLE + " WHERE " +
                    MONITORING_PLOT_ID + " ='" + plotId + "' AND " + MONITORING_YEAR + " ='" + year + "' ";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        Monitoring monitoring = new Monitoring();


                        monitoring.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        monitoring.setName(cursor.getString(cursor.getColumnIndex(MONITORING_NAME)));
                        monitoring.setJson(cursor.getString(cursor.getColumnIndex(MONITORING_JSON)));
                        monitoring.setYear(cursor.getString(cursor.getColumnIndex(MONITORING_YEAR)));
                        monitoring.setPlotId(cursor.getString(cursor.getColumnIndex(MONITORING_PLOT_ID)));


                        Log.i(TAG, "MONITORING WITH ID " + monitoring.getId() + "\t");
                        Log.i(TAG, "NAME  =  " + monitoring.getName() + " \n");

                        monitoringList.add(monitoring);

                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return monitoringList;


    }


    public List<Monitoring> getAllPlotMonitoring(String plotId) {

        List<Monitoring> monitoringList = new ArrayList<>();

        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + MONITORING_TABLE + " WHERE " +
                    MONITORING_PLOT_ID + " ='" + plotId + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        Monitoring monitoring = new Monitoring();


                        monitoring.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        monitoring.setName(cursor.getString(cursor.getColumnIndex(MONITORING_NAME)));
                        monitoring.setYear(cursor.getString(cursor.getColumnIndex(MONITORING_YEAR)));
                        monitoring.setJson(cursor.getString(cursor.getColumnIndex(MONITORING_JSON)));
                        monitoring.setPlotId(cursor.getString(cursor.getColumnIndex(MONITORING_PLOT_ID)));


                        Log.i(TAG, "MONITORING WITH ID " + monitoring.getId() + "\t");
                        Log.i(TAG, "NAME  =  " + monitoring.getName() + " \n");

                        monitoringList.add(monitoring);

                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return monitoringList;


    }



    public boolean editPlotMonitoringJson(String id, String plotId, String year, String newValue) {
        try {

            ContentValues contentValues = new ContentValues();

            contentValues.put(MONITORING_JSON, newValue);


            db.update(MONITORING_TABLE, contentValues, ID + "= ? AND " + MONITORING_PLOT_ID + "= ? AND " + MONITORING_YEAR + " = ?", new String[]{id, plotId, year});
            Log.d(TAG, "MONITORING DATA EDITED WITH ID " + id + " EDITED WITH VALUE " + newValue);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }


    }


    public boolean addNewHistoricalData(HistoricalData historicalData) {
        try {

            String dateTime = DateUtil.getFormattedDateMMDDYYYYhhmmaa();


            ContentValues contentValues = new ContentValues();
            contentValues.put(ID, historicalData.getId());
            contentValues.put(NAME, historicalData.getName());
            contentValues.put(DATE_TIME, dateTime);
            contentValues.put(LAST_MODIFIED_DATE, dateTime);

            contentValues.put(ANSWERS_JSON, historicalData.getAnswersJson());
            contentValues.put(FORM_ID, historicalData.getFormId());

            db.insert(HISTORICAL_DATA, null, contentValues);
            Log.d(TAG, "HISTORICAL DATA " + new Gson().toJson(historicalData) + " INSERTED");
            return true;


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean editHistoricalData(String id, String newValue) {
        try {

            ContentValues contentValues = new ContentValues();
            contentValues.put(ANSWERS_JSON, newValue);
            contentValues.put(LAST_MODIFIED_DATE, DateUtil.getFormattedDateMMDDYYYYhhmmaa());
            db.update(HISTORICAL_DATA, contentValues, ID + " = ?", new String[]{id});
            Log.d(TAG, "HISTORICAL DATA EDITED WITH ID " + id + " EDITED WITH VALUE " + newValue);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }


    }

    public List<HistoricalData> getAllHistoricalDataForForm(String formId) {

        List<HistoricalData> historicalDataList = new ArrayList<>();

        Cursor cursor = null;


        try {

            String selectQuery = "SELECT  * FROM " + HISTORICAL_DATA + " WHERE " + FORM_ID + " ='" + formId + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        HistoricalData historicalData = new HistoricalData();
                        historicalData.setName("Data " + cursor.getInt(cursor.getColumnIndex(BaseColumns._ID)));
                        historicalData.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        historicalData.setDateTime(cursor.getString(cursor.getColumnIndex(DATE_TIME)));
                        historicalData.setLastModifiedDate(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));
                        historicalData.setAnswersJson(cursor.getString(cursor.getColumnIndex(ANSWERS_JSON)));
                        historicalData.setFormId(cursor.getString(cursor.getColumnIndex(FORM_ID)));
                        historicalDataList.add(historicalData);
                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if (cursor != null)
                cursor.close();
        }
        return historicalDataList;


    }







    public boolean addComplexCalculation(ComplexCalculation complexCalculation) {
        try {

            ContentValues contentValues = new ContentValues();
            contentValues.put(LAST_MODIFIED_DATE, complexCalculation.getLastModifiedDate());

            contentValues.put(ID, complexCalculation.getId());
            contentValues.put(COMPLEX_CALCULATION_NAME, complexCalculation.getName());
            contentValues.put(COMPLEX_CALCULATION_QUESTION_ID, complexCalculation.getQuestionId());
            contentValues.put(COMPLEX_CALCULATION_CONDITION, complexCalculation.getCondition());


            db.insert(COMPLEX_CALCULATIONS_TABLE, null, contentValues);
            Log.d(TAG, "COM_CALC WITH QUESTION ID " + complexCalculation.getQuestionId() + " INSERTED WITH CONDITION " + complexCalculation.getCondition() );


            if(prefs.getString("monitoringToUse", null) == null){

                if(complexCalculation.getCondition().contains("First"))
                    prefs.edit().putString("monitoringToUse", "first").apply();
                else
                    prefs.edit().putString("monitoringToUse", "last").apply();
            }

            return true;


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteComplexCalculation(String id) {

        return db.delete(COMPLEX_CALCULATIONS_TABLE, ID + " = ?", new String[]{id}) > 0;

    }



    public List<ComplexCalculation> getAllComplexCalculation() {

        List<ComplexCalculation> complexCalculationList = new ArrayList<>();
        Cursor cursor = null;
        try {

            String selectQuery = "SELECT  * FROM " + COMPLEX_CALCULATIONS_TABLE;


            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                        ComplexCalculation complexCalculation = new ComplexCalculation();


                        complexCalculation.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        complexCalculation.setName(cursor.getString(cursor.getColumnIndex(COMPLEX_CALCULATION_NAME)));
                        complexCalculation.setQuestionId(cursor.getString(cursor.getColumnIndex(COMPLEX_CALCULATION_QUESTION_ID)));
                        complexCalculation.setCondition(cursor.getString(cursor.getColumnIndex(COMPLEX_CALCULATION_CONDITION)));


                        complexCalculationList.add(complexCalculation);
                        Log.d(TAG, "INPUT WITH ID " + complexCalculation.getId() + " FOUND WITH CONDITION " + complexCalculation.getCondition());

                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return complexCalculationList;


    }



    public ComplexCalculation getComplexCalculation(String id) {

       ComplexCalculation complexCalculation = null;
        Cursor cursor = null;
        try {

            String selectQuery = "SELECT  * FROM " + COMPLEX_CALCULATIONS_TABLE + " WHERE " + COMPLEX_CALCULATION_QUESTION_ID + " = '" + id +"'";


            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToFirst())

                    do {
                         complexCalculation = new ComplexCalculation();


                        complexCalculation.setId(cursor.getString(cursor.getColumnIndex(ID)));
                        complexCalculation.setName(cursor.getString(cursor.getColumnIndex(COMPLEX_CALCULATION_NAME)));
                        complexCalculation.setQuestionId(cursor.getString(cursor.getColumnIndex(COMPLEX_CALCULATION_QUESTION_ID)));
                        complexCalculation.setCondition(cursor.getString(cursor.getColumnIndex(COMPLEX_CALCULATION_CONDITION)));


                         Log.d(TAG, "INPUT WITH ID " + complexCalculation.getId() + " FOUND WITH CONDITION " + complexCalculation.getCondition());

                    } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        } finally {

            if (cursor != null)
                cursor.close();
        }

        return complexCalculation;


    }






    public Boolean isUpdated(String id, String lastUpdated, String TABLE_NAME){

        Boolean value = false;
        Cursor cursor = null;
        try{

            String selectQuery = "SELECT " + LAST_MODIFIED_DATE + " FROM " + TABLE_NAME + " WHERE " + ID + " ='" + id + "'";

            Log.i("QUERY", selectQuery);
            cursor = db.rawQuery(selectQuery, null);

            if (cursor != null && cursor.getCount() > 0) {
                if (cursor.moveToFirst())

                        value = !lastUpdated.equalsIgnoreCase(cursor.getString(cursor.getColumnIndex(LAST_MODIFIED_DATE)));

            }else
                value = null;
        }catch(Exception e){

            value = false;
            e.printStackTrace();

        }finally {
            if(cursor != null)
                cursor.close();
        }

        Log.i("DATABASE" , "LAST UPDATED? " + value);

        return value;
    }






}
