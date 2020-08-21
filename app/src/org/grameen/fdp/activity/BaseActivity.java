package org.grameen.fdp.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.ApiVersionStrings;

import org.grameen.fdp.R;
import org.grameen.fdp.application.FdpApplication;
import org.grameen.fdp.fragment.LogoutDialogFragment;
import org.grameen.fdp.fragment.MonitoringFormFragment;
import org.grameen.fdp.fragment.MyFormFragment;
import org.grameen.fdp.object.Form;
import org.grameen.fdp.object.Logic;
import org.grameen.fdp.object.Monitoring;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.object.RealFarmer;
import org.grameen.fdp.object.RealPlot;
import org.grameen.fdp.object.SkipLogic;
import org.grameen.fdp.utility.Callbacks;
import org.grameen.fdp.utility.Constants;
import org.grameen.fdp.utility.CustomToast;
import org.grameen.fdp.utility.DatabaseHelper;
import org.grameen.fdp.utility.DateUtil;
import org.grameen.fdp.utility.MySingleton;
import org.grameen.fdp.utility.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import static org.grameen.fdp.application.FdpApplication.ROOT_DIR;

/**
 * Created by aangjnr on 08/11/2017.
 */

public class BaseActivity extends AppCompatActivity {


    public static Callbacks.NetworkActivityCompleteListener networkActivityCompleteListener;

    static String ISO_CODE;

    int COUNT = 0;

    static int SELECTED_FORM_INDEX;
    int SYNC_STATUS = Constants.SYNC_STATUS_NO_SYNC;
    String message = "";
    String title = "";
    private List<RealFarmer> UN_SYNCED_FARMERS;
    private static final String DOUBLE_LINE = "==============================================================================";
    private static final String SINGLE_LINE = "------------------------------------------------------------------------------";
    public List<Form> FORMS;
    String date;
    public static ProgressDialog progressDialog;
    SharedPreferences prefs;
    public DatabaseHelper databaseHelper;
    ScriptEngine engine;
    static String TAG = "BASE ACTIVITY";
    private String apiVersion;
    Boolean IS_TRANSLATION = false;
    public boolean IS_MONITIRING_MODE;
    String type = Constants.FORM_DIAGNOSTIC;
    public String correspondingMessage;

    public static void showError(Context context, Exception e) {
        Toast toast = Toast.makeText(context,
                context.getString(
                        SalesforceSDKManager.getInstance().
                                getSalesforceR().stringGenericError(),
                        e.toString()),
                Toast.LENGTH_LONG);
        toast.show();
    }

    public static ProgressDialog showProgress(Context context, String title, String message, Boolean cancelable) {

        ProgressDialog progressDialog = new ProgressDialog(context, R.style.DialogTheme);
        progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(cancelable);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
        return progressDialog;


    }

    public static void printRequestInfo(long nanoDuration, int characterLength, int statusCode) {
        System.out.println(SINGLE_LINE);
        System.out.println("Time (ms): " + nanoDuration / 1000000);
        System.out.println("Size (chars): " + characterLength);
        System.out.println("Status code: " + statusCode);
    }

    public static void printException(Exception e) {
        System.out.println("Error: " + e.getClass().getSimpleName());
        System.out.println(e.getMessage());
        e.printStackTrace();

    }

    public static void printHeader(Object obj) {
        System.out.println(DOUBLE_LINE);
        System.out.println(obj.toString());
        System.out.println(SINGLE_LINE);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.right_slide, R.anim.slide_out_left);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);


        ISO_CODE = prefs.getString("ISO", "");

        IS_MONITIRING_MODE = prefs.getString("flag", "").equalsIgnoreCase(Constants.MONITORING);
        if (IS_MONITIRING_MODE)
            setTheme(R.style.AppTheme_Monitoring);
        else
            setTheme(R.style.AppTheme);


        IS_TRANSLATION = prefs.getBoolean("toggleTranslation", false);



        apiVersion = ApiVersionStrings.getVersionNumber(this);

        databaseHelper = DatabaseHelper.getInstance(this);


        date = DateUtil.getFormattedDateMMDDYYYY();


        if (IS_MONITIRING_MODE)
            FORMS = databaseHelper.getAllForms();
        else
            FORMS = databaseHelper.getAllDiagnosticForms();


    }

    Toolbar setToolbar(String title) {

        Toolbar toolbar = null;
        try {
            toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);


            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_keyboard_arrow_left_white_24dp);


            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }

        return toolbar;

    }

    public void showAlertDialog(Boolean cancelable, @Nullable String title, @Nullable String message,
                                @Nullable DialogInterface.OnClickListener onPositiveButtonClickListener,
                                @NonNull String positiveText,
                                @Nullable DialogInterface.OnClickListener onNegativeButtonClickListener,
                                @NonNull String negativeText, @Nullable int icon_drawable) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppDialog);
        builder.setTitle(title);
        builder.setCancelable(cancelable);


        if (icon_drawable != 0) builder.setIcon(icon_drawable);
        builder.setMessage(message);

        if (onPositiveButtonClickListener != null)
            builder.setPositiveButton(positiveText, onPositiveButtonClickListener);
        if (onNegativeButtonClickListener != null)
            builder.setNegativeButton(negativeText, onNegativeButtonClickListener);
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // I do not want this...
                // Home as up button is to navigate to Home-Activity not previous acitivity
                super.onBackPressed();
                return true;
        }


        return super.onOptionsItemSelected(item);


    }

    void showSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    void hideSoftKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        }
    }

    public void logOut(final AppCompatActivity context) {

        LogoutDialogFragment logoutDialogFragment = new LogoutDialogFragment();
        logoutDialogFragment.show(getSupportFragmentManager(), "logoutDialog");


    }

    void onBackClicked() {

        try {

            findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    onBackPressed();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void onBackClicked(View v) {

        try {
            onBackPressed();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    boolean hasPermissions(Context context, String permission) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null) {

            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {


                }
                return false;
            }

        }
        return true;
    }

    void loadDynamicView(MyFormFragment formFragment1, int layout1) {

        //Todo add parameter to load data from the database, if is in editing mode else display default forms with their resp values
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .add(layout1, formFragment1, formFragment1.getClass().getSimpleName())
                //.add(layout2, formFragment2, formFragment2.getClass().getSimpleName())
                .commit();


    }

    void loadDynamicView(MonitoringFormFragment formFragment1, int layout1) {

        //Todo add parameter to load data from the database, if is in editing mode else display default forms with their resp values


        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .add(layout1, formFragment1, formFragment1.getClass().getSimpleName())
                //.add(layout2, formFragment2, formFragment2.getClass().getSimpleName())
                .commit();


    }

    String calculate(String equation) throws ScriptException {
        Double value = 0.0;
        try {
            value = (Double) new ScriptEngineManager().getEngineByName("rhino").eval(equation.trim().replace(",", ""));
        } catch (Exception e) {
            e.printStackTrace();
            value = 0.0;
        }

        return String.valueOf(value);
    }

    Double calculateDouble(String equation) {
        Log.i(TAG, "Evaluating " + equation);
        Double value;
        try {
            value = (Double) new ScriptEngineManager().getEngineByName("rhino").eval(equation.trim().replace(",", ""));
        } catch (Exception e) {
            e.printStackTrace();
            value = null;
        }

        return value;
    }

    Boolean compareValues(SkipLogic sl, String newValue) {

        engine = new ScriptEngineManager().getEngineByName("rhino");

        String equation = sl.getAnswerValue() + sl.getLogicalOperator() + String.valueOf(newValue);

        Log.i("BASE ACTIVITY", "Equation is " + equation);

        boolean value = false;

        try {
            value = (Boolean) engine.eval(equation);

        } catch (ScriptException | NumberFormatException e) {
            System.out.println("******* EXCEPTION ****** " + e.getMessage());

            if (sl.getLogicalOperator().equalsIgnoreCase("=="))
                value = sl.getAnswerValue().equalsIgnoreCase(newValue);
            else value = !sl.getAnswerValue().equalsIgnoreCase(newValue);

        } finally {
            System.out.println(equation + " = " + value);
        }
        return value;
    }


    Boolean compareValues(Logic logic, JSONObject ALL_MONITORING_VALUES_JSON) {

        Boolean value = null;
        String inputValue = getValue(logic.getQUESTION(), ALL_MONITORING_VALUES_JSON);
        Log.i(TAG, "EQUATION IS    ");


        Log.i(TAG, inputValue + "  " + logic.getLOGICAL_OPERATOR() + "  " + logic.getVALUE() + "\n\n");


        if (inputValue != null) {

            try {
                Double.parseDouble(inputValue.trim());

                value = (Boolean) engine.eval(inputValue + logic.getLOGICAL_OPERATOR() + logic.getVALUE());

                return value;

            } catch (ScriptException | NumberFormatException e) {

                System.out.println("**********  EXCEPTION ******************" + e.getMessage());


                if (logic.getLOGICAL_OPERATOR().equalsIgnoreCase("=="))
                    value = inputValue.equalsIgnoreCase(logic.getVALUE());
                else value = !inputValue.equalsIgnoreCase(logic.getVALUE());


                return value;
            } finally {
                System.out.println("*****************************LOGIC VALUE IS: " + value);

            }
        } else return null;


    }


    Boolean getLogicValue(Logic logic, JSONObject JSON_OBJECT) {


        if (logic == null)
            return null;

        else {


            if (logic.getValue1() != null && !logic.getValue1().equalsIgnoreCase("null") && !logic.getValue1().isEmpty()) {


                Log.i(TAG, "<<<<<<<<<<  LOGIC NAME IS = " + logic.getName() + ">>>>>>>>>>>>>>\n");


                if (logic.getQuestion10() != null && !logic.getQuestion10().equals("null")) {


                    Log.i(TAG, "LOGIC WITH 10 QUESTIONS WITH QUESTION VALUE = " + logic.getQuestion10() + "\n\n");

                    if (logic.getQuestion10().equals(""))
                        return null;


                    Logic l1 = new Logic();
                    l1.setQUESTION(logic.getQuestion1());
                    l1.setLOGICAL_OPERATOR(logic.getLogicalOperator1());
                    l1.setVALUE(logic.getValue1());


                    Logic l2 = new Logic();
                    l2.setQUESTION(logic.getQuestion2());
                    l2.setLOGICAL_OPERATOR(logic.getLogicalOperator2());
                    l2.setVALUE(logic.getValue2());


                    Logic l3 = new Logic();
                    l3.setQUESTION(logic.getQuestion3());
                    l3.setLOGICAL_OPERATOR(logic.getLogicalOperator3());
                    l3.setVALUE(logic.getValue3());


                    Logic l4 = new Logic();
                    l4.setQUESTION(logic.getQuestion4());
                    l4.setLOGICAL_OPERATOR(logic.getLogicalOperator4());
                    l4.setVALUE(logic.getValue4());


                    Logic l5 = new Logic();
                    l5.setQUESTION(logic.getQuestion5());
                    l5.setLOGICAL_OPERATOR(logic.getLogicalOperator5());
                    l5.setVALUE(logic.getValue5());


                    Logic l6 = new Logic();
                    l6.setQUESTION(logic.getQuestion6());
                    l6.setLOGICAL_OPERATOR(logic.getLogicalOperator6());
                    l6.setVALUE(logic.getValue6());


                    Logic l7 = new Logic();
                    l7.setQUESTION(logic.getQuestion7());
                    l7.setLOGICAL_OPERATOR(logic.getLogicalOperator7());
                    l7.setVALUE(logic.getValue7());


                    Logic l8 = new Logic();
                    l8.setQUESTION(logic.getQuestion8());
                    l8.setLOGICAL_OPERATOR(logic.getLogicalOperator8());
                    l8.setVALUE(logic.getValue8());


                    Logic l9 = new Logic();
                    l9.setQUESTION(logic.getQuestion9());
                    l9.setLOGICAL_OPERATOR(logic.getLogicalOperator9());
                    l9.setVALUE(logic.getValue9());


                    Logic l10 = new Logic();
                    l10.setQUESTION(logic.getQuestion10());
                    l10.setLOGICAL_OPERATOR(logic.getLogicalOperator10());
                    l10.setVALUE(logic.getValue10());


                    Boolean value;

                    try {

                        String equation = ((((((((compareValues(l1, JSON_OBJECT)
                                + logic.getQuestionLogicOperation1() + compareValues(l2, JSON_OBJECT))
                                + logic.getQuestionLogicOperation2() + compareValues(l3, JSON_OBJECT))
                                + logic.getQuestionLogicOperation3() + compareValues(l4, JSON_OBJECT))
                                + logic.getQuestionLogicOperation4() + compareValues(l5, JSON_OBJECT))
                                + logic.getQuestionLogicOperation5() + compareValues(l6, JSON_OBJECT))
                                + logic.getQuestionLogicOperation6() + compareValues(l7, JSON_OBJECT))
                                + logic.getQuestionLogicOperation7() + compareValues(l8, JSON_OBJECT))
                                + logic.getQuestionLogicOperation8() + compareValues(l9, JSON_OBJECT))
                                + logic.getQuestionLogicOperation9() + compareValues(l10, JSON_OBJECT);


                        System.out.println("  EQUATION  =  " + equation);


                        value = (Boolean) engine.eval(equation);

                        System.out.println("COMPARE QUESTION VALUES Object value: " + value + "\n");

                    } catch (ScriptException e) {
                        e.printStackTrace();

                        value = null;
                    }


                    return value;


                } else if (logic.getQuestion9() != null && !logic.getQuestion9().equals("null")) {


                    Log.i(TAG, "LOGIC WITH 9 QUESTIONS!\n\n");


                    Logic l1 = new Logic();
                    l1.setQUESTION(logic.getQuestion1());
                    l1.setLOGICAL_OPERATOR(logic.getLogicalOperator1());
                    l1.setVALUE(logic.getValue1());


                    Logic l2 = new Logic();
                    l2.setQUESTION(logic.getQuestion2());
                    l2.setLOGICAL_OPERATOR(logic.getLogicalOperator2());
                    l2.setVALUE(logic.getValue2());


                    Logic l3 = new Logic();
                    l3.setQUESTION(logic.getQuestion3());
                    l3.setLOGICAL_OPERATOR(logic.getLogicalOperator3());
                    l3.setVALUE(logic.getValue3());


                    Logic l4 = new Logic();
                    l4.setQUESTION(logic.getQuestion4());
                    l4.setLOGICAL_OPERATOR(logic.getLogicalOperator4());
                    l4.setVALUE(logic.getValue4());


                    Logic l5 = new Logic();
                    l5.setQUESTION(logic.getQuestion5());
                    l5.setLOGICAL_OPERATOR(logic.getLogicalOperator5());
                    l5.setVALUE(logic.getValue5());


                    Logic l6 = new Logic();
                    l6.setQUESTION(logic.getQuestion6());
                    l6.setLOGICAL_OPERATOR(logic.getLogicalOperator6());
                    l6.setVALUE(logic.getValue6());


                    Logic l7 = new Logic();
                    l7.setQUESTION(logic.getQuestion7());
                    l7.setLOGICAL_OPERATOR(logic.getLogicalOperator7());
                    l7.setVALUE(logic.getValue7());


                    Logic l8 = new Logic();
                    l8.setQUESTION(logic.getQuestion8());
                    l8.setLOGICAL_OPERATOR(logic.getLogicalOperator8());
                    l8.setVALUE(logic.getValue8());


                    Logic l9 = new Logic();
                    l9.setQUESTION(logic.getQuestion9());
                    l9.setLOGICAL_OPERATOR(logic.getLogicalOperator9());
                    l9.setVALUE(logic.getValue9());


                    Boolean value;

                    try {


                        String equation = ((((((((compareValues(l1, JSON_OBJECT)
                                + logic.getQuestionLogicOperation1() + compareValues(l2, JSON_OBJECT))
                                + logic.getQuestionLogicOperation2() + compareValues(l3, JSON_OBJECT))
                                + logic.getQuestionLogicOperation3() + compareValues(l4, JSON_OBJECT))
                                + logic.getQuestionLogicOperation4() + compareValues(l5, JSON_OBJECT))
                                + logic.getQuestionLogicOperation5() + compareValues(l6, JSON_OBJECT))
                                + logic.getQuestionLogicOperation6() + compareValues(l7, JSON_OBJECT))
                                + logic.getQuestionLogicOperation7() + compareValues(l8, JSON_OBJECT))
                                + logic.getQuestionLogicOperation8() + compareValues(l9, JSON_OBJECT));


                        System.out.println("  EQUATION  =  " + equation);


                        value = (Boolean) engine.eval(equation);

                        System.out.println("COMPARE QUESTION VALUES Object value: " + value);

                    } catch (ScriptException e) {
                        e.printStackTrace();

                        value = null;
                    }


                    return value;


                } else if (logic.getQuestion8() != null && !logic.getQuestion8().equals("null")) {


                    Log.i(TAG, "LOGIC WITH 8 QUESTIONS!\n\n");


                    Logic l1 = new Logic();
                    l1.setQUESTION(logic.getQuestion1());
                    l1.setLOGICAL_OPERATOR(logic.getLogicalOperator1());
                    l1.setVALUE(logic.getValue1());


                    Logic l2 = new Logic();
                    l2.setQUESTION(logic.getQuestion2());
                    l2.setLOGICAL_OPERATOR(logic.getLogicalOperator2());
                    l2.setVALUE(logic.getValue2());


                    Logic l3 = new Logic();
                    l3.setQUESTION(logic.getQuestion3());
                    l3.setLOGICAL_OPERATOR(logic.getLogicalOperator3());
                    l3.setVALUE(logic.getValue3());


                    Logic l4 = new Logic();
                    l4.setQUESTION(logic.getQuestion4());
                    l4.setLOGICAL_OPERATOR(logic.getLogicalOperator4());
                    l4.setVALUE(logic.getValue4());


                    Logic l5 = new Logic();
                    l5.setQUESTION(logic.getQuestion5());
                    l5.setLOGICAL_OPERATOR(logic.getLogicalOperator5());
                    l5.setVALUE(logic.getValue5());


                    Logic l6 = new Logic();
                    l6.setQUESTION(logic.getQuestion6());
                    l6.setLOGICAL_OPERATOR(logic.getLogicalOperator6());
                    l6.setVALUE(logic.getValue6());


                    Logic l7 = new Logic();
                    l7.setQUESTION(logic.getQuestion7());
                    l7.setLOGICAL_OPERATOR(logic.getLogicalOperator7());
                    l7.setVALUE(logic.getValue7());


                    Logic l8 = new Logic();
                    l8.setQUESTION(logic.getQuestion8());
                    l8.setLOGICAL_OPERATOR(logic.getLogicalOperator8());
                    l8.setVALUE(logic.getValue8());


                    Boolean value;

                    try {


                        String equation = (
                                ((((((compareValues(l1, JSON_OBJECT)
                                        + logic.getQuestionLogicOperation1() + compareValues(l2, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation2() + compareValues(l3, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation3() + compareValues(l4, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation4() + compareValues(l5, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation5() + compareValues(l6, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation6() + compareValues(l7, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation7() + compareValues(l8, JSON_OBJECT)
                        );


                        System.out.println("  EQUATION  =  " + equation);


                        value = (Boolean) engine.eval(equation);

                        System.out.println("COMPARE QUESTION VALUES Object value: " + value);

                    } catch (ScriptException e) {
                        e.printStackTrace();

                        value = null;
                    }


                    return value;

                } else if (logic.getQuestion7() != null && !logic.getQuestion7().equals("null")) {


                    Log.i(TAG, "LOGIC WITH 7 QUESTIONS!\n\n");


                    Logic l1 = new Logic();
                    l1.setQUESTION(logic.getQuestion1());
                    l1.setLOGICAL_OPERATOR(logic.getLogicalOperator1());
                    l1.setVALUE(logic.getValue1());


                    Logic l2 = new Logic();
                    l2.setQUESTION(logic.getQuestion2());
                    l2.setLOGICAL_OPERATOR(logic.getLogicalOperator2());
                    l2.setVALUE(logic.getValue2());


                    Logic l3 = new Logic();
                    l3.setQUESTION(logic.getQuestion3());
                    l3.setLOGICAL_OPERATOR(logic.getLogicalOperator3());
                    l3.setVALUE(logic.getValue3());


                    Logic l4 = new Logic();
                    l4.setQUESTION(logic.getQuestion4());
                    l4.setLOGICAL_OPERATOR(logic.getLogicalOperator4());
                    l4.setVALUE(logic.getValue4());


                    Logic l5 = new Logic();
                    l5.setQUESTION(logic.getQuestion5());
                    l5.setLOGICAL_OPERATOR(logic.getLogicalOperator5());
                    l5.setVALUE(logic.getValue5());


                    Logic l6 = new Logic();
                    l6.setQUESTION(logic.getQuestion6());
                    l6.setLOGICAL_OPERATOR(logic.getLogicalOperator6());
                    l6.setVALUE(logic.getValue6());


                    Logic l7 = new Logic();
                    l7.setQUESTION(logic.getQuestion7());
                    l7.setLOGICAL_OPERATOR(logic.getLogicalOperator7());
                    l7.setVALUE(logic.getValue7());


                    Boolean value;

                    try {


                        String equation = (
                                (((((compareValues(l1, JSON_OBJECT)
                                        + logic.getQuestionLogicOperation1() + compareValues(l2, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation2() + compareValues(l3, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation3() + compareValues(l4, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation4() + compareValues(l5, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation5() + compareValues(l6, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation6() + compareValues(l7, JSON_OBJECT)
                        );


                        System.out.println("  EQUATION  =  " + equation);


                        value = (Boolean) engine.eval(equation);

                        System.out.println("COMPARE QUESTION VALUES Object value: " + value);

                    } catch (ScriptException e) {
                        e.printStackTrace();

                        value = null;
                    }


                    return value;


                } else if (logic.getQuestion6() != null && !logic.getQuestion6().equals("null")) {


                    Log.i(TAG, "LOGIC WITH 6 QUESTIONS!\n\n");


                    Logic l1 = new Logic();
                    l1.setQUESTION(logic.getQuestion1());
                    l1.setLOGICAL_OPERATOR(logic.getLogicalOperator1());
                    l1.setVALUE(logic.getValue1());


                    Logic l2 = new Logic();
                    l2.setQUESTION(logic.getQuestion2());
                    l2.setLOGICAL_OPERATOR(logic.getLogicalOperator2());
                    l2.setVALUE(logic.getValue2());


                    Logic l3 = new Logic();
                    l3.setQUESTION(logic.getQuestion3());
                    l3.setLOGICAL_OPERATOR(logic.getLogicalOperator3());
                    l3.setVALUE(logic.getValue3());


                    Logic l4 = new Logic();
                    l4.setQUESTION(logic.getQuestion4());
                    l4.setLOGICAL_OPERATOR(logic.getLogicalOperator4());
                    l4.setVALUE(logic.getValue4());


                    Logic l5 = new Logic();
                    l5.setQUESTION(logic.getQuestion5());
                    l5.setLOGICAL_OPERATOR(logic.getLogicalOperator5());
                    l5.setVALUE(logic.getValue5());


                    Logic l6 = new Logic();
                    l6.setQUESTION(logic.getQuestion6());
                    l6.setLOGICAL_OPERATOR(logic.getLogicalOperator6());
                    l6.setVALUE(logic.getValue6());


                    Boolean value;

                    try {


                        String equation = (
                                ((((compareValues(l1, JSON_OBJECT)
                                        + logic.getQuestionLogicOperation1() + compareValues(l2, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation2() + compareValues(l3, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation3() + compareValues(l4, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation4() + compareValues(l5, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation5() + compareValues(l6, JSON_OBJECT)
                        );


                        System.out.println("  EQUATION  =  " + equation);

                        value = (Boolean) engine.eval(equation);

                        System.out.println("COMPARE QUESTION VALUES Object value: " + value);

                    } catch (ScriptException e) {
                        e.printStackTrace();

                        value = null;
                    }


                    return value;


                } else if (logic.getQuestion5() != null && !logic.getQuestion5().equals("null")) {

                    Log.i(TAG, "LOGIC WITH 5 QUESTIONS!\n\n");


                    Logic l1 = new Logic();
                    l1.setQUESTION(logic.getQuestion1());
                    l1.setLOGICAL_OPERATOR(logic.getLogicalOperator1());
                    l1.setVALUE(logic.getValue1());


                    Logic l2 = new Logic();
                    l2.setQUESTION(logic.getQuestion2());
                    l2.setLOGICAL_OPERATOR(logic.getLogicalOperator2());
                    l2.setVALUE(logic.getValue2());


                    Logic l3 = new Logic();
                    l3.setQUESTION(logic.getQuestion3());
                    l3.setLOGICAL_OPERATOR(logic.getLogicalOperator3());
                    l3.setVALUE(logic.getValue3());


                    Logic l4 = new Logic();
                    l4.setQUESTION(logic.getQuestion4());
                    l4.setLOGICAL_OPERATOR(logic.getLogicalOperator4());
                    l4.setVALUE(logic.getValue4());


                    Logic l5 = new Logic();
                    l5.setQUESTION(logic.getQuestion5());
                    l5.setLOGICAL_OPERATOR(logic.getLogicalOperator5());
                    l5.setVALUE(logic.getValue5());


                    Boolean value;

                    try {


                        String equation = (
                                (((compareValues(l1, JSON_OBJECT)
                                        + logic.getQuestionLogicOperation1() + compareValues(l2, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation2() + compareValues(l3, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation3() + compareValues(l4, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation4() + compareValues(l5, JSON_OBJECT)
                        );


                        System.out.println("  EQUATION  =  " + equation);

                        value = (Boolean) engine.eval(equation);


                        System.out.println("COMPARE QUESTION VALUES Object value: " + value);

                    } catch (ScriptException e) {
                        e.printStackTrace();

                        value = null;
                    }


                    return value;


                } else if (logic.getQuestion4() != null && !logic.getQuestion4().equals("null")) {

                    Log.i(TAG, "LOGIC WITH 4 QUESTIONS!\n\n");


                    Logic l1 = new Logic();
                    l1.setQUESTION(logic.getQuestion1());
                    l1.setLOGICAL_OPERATOR(logic.getLogicalOperator1());
                    l1.setVALUE(logic.getValue1());


                    Logic l2 = new Logic();
                    l2.setQUESTION(logic.getQuestion2());
                    l2.setLOGICAL_OPERATOR(logic.getLogicalOperator2());
                    l2.setVALUE(logic.getValue2());


                    Logic l3 = new Logic();
                    l3.setQUESTION(logic.getQuestion3());
                    l3.setLOGICAL_OPERATOR(logic.getLogicalOperator3());
                    l3.setVALUE(logic.getValue3());


                    Logic l4 = new Logic();
                    l4.setQUESTION(logic.getQuestion4());
                    l4.setLOGICAL_OPERATOR(logic.getLogicalOperator4());
                    l4.setVALUE(logic.getValue4());


                    Boolean value;

                    try {


                        String equation = (
                                ((compareValues(l1, JSON_OBJECT)
                                        + logic.getQuestionLogicOperation1() + compareValues(l2, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation2() + compareValues(l3, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation3() + compareValues(l4, JSON_OBJECT)
                        );


                        System.out.println("  EQUATION  =  " + equation);

                        value = (Boolean) engine.eval(equation);


                        System.out.println("COMPARE QUESTION VALUES Object value: " + value);

                    } catch (ScriptException e) {
                        e.printStackTrace();

                        value = null;
                    }


                    return value;


                } else if (logic.getQuestion3() != null && !logic.getQuestion3().equals("null")) {


                    Log.i(TAG, "LOGIC WITH 3 QUESTIONS!\n\n");


                    Logic l1 = new Logic();
                    l1.setQUESTION(logic.getQuestion1());
                    l1.setLOGICAL_OPERATOR(logic.getLogicalOperator1());
                    l1.setVALUE(logic.getValue1());


                    Logic l2 = new Logic();
                    l2.setQUESTION(logic.getQuestion2());
                    l2.setLOGICAL_OPERATOR(logic.getLogicalOperator2());
                    l2.setVALUE(logic.getValue2());


                    Logic l3 = new Logic();
                    l3.setQUESTION(logic.getQuestion3());
                    l3.setLOGICAL_OPERATOR(logic.getLogicalOperator3());
                    l3.setVALUE(logic.getValue3());


                    Boolean value;

                    try {


                        String equation = (
                                (compareValues(l1, JSON_OBJECT)
                                        + logic.getQuestionLogicOperation1() + compareValues(l2, JSON_OBJECT))
                                        + logic.getQuestionLogicOperation2() + compareValues(l3, JSON_OBJECT)
                        );


                        System.out.println("  EQUATION  =  " + equation);

                        value = (Boolean) engine.eval(equation);


                        System.out.println("COMPARE QUESTION VALUES Object value: " + value);

                    } catch (ScriptException e) {
                        e.printStackTrace();

                        value = null;
                    }


                    return value;


                } else if (logic.getQuestion2() != null && !logic.getQuestion2().equals("null")) {

                    Log.i(TAG, "LOGIC WITH 2 QUESTIONS!\n\n");


                    Logic l1 = new Logic();
                    l1.setQUESTION(logic.getQuestion1());
                    l1.setLOGICAL_OPERATOR(logic.getLogicalOperator1());
                    l1.setVALUE(logic.getValue1());


                    Logic l2 = new Logic();
                    l2.setQUESTION(logic.getQuestion2());
                    l2.setLOGICAL_OPERATOR(logic.getLogicalOperator2());
                    l2.setVALUE(logic.getValue2());


                    Boolean value;

                    try {

                        String equation = (compareValues(l1, JSON_OBJECT)
                                + logic.getQuestionLogicOperation1() + compareValues(l2, JSON_OBJECT));


                        System.out.println("  EQUATION  =  " + equation);

                        value = (Boolean) engine.eval(equation);


                        System.out.println("COMPARE QUESTION VALUES Object value: " + value);

                    } catch (ScriptException e) {
                        e.printStackTrace();

                        value = null;
                    }


                    return value;


                } else if (logic.getQuestion1() != null && !logic.getQuestion1().equals("null")) {

                    Log.i(TAG, "LOGIC WITH 1 QUESTIONS!\n\n");

                    Logic l = new Logic();
                    l.setQUESTION(logic.getQuestion1());
                    l.setLOGICAL_OPERATOR(logic.getLogicalOperator1());
                    l.setVALUE(logic.getValue1());

                    return compareValues(l, JSON_OBJECT);

                } else
                    return null;


            } else
                return getSpecialImplementationLogicValue(logic, JSON_OBJECT);

        }
    }


    Boolean getSpecialImplementationLogicValue(Logic logic, JSONObject JSON_OBJECT) {


        if (logic == null)
            return null;

        else {

            Log.i(TAG, "<<<<<<<<<< SPECIAL LOGIC NAME IS = " + logic.getName() + ">>>>>>>>>>>>>>\n");


            if (logic.getQuestion10() != null && !logic.getQuestion10().equals("null")) {


                Log.i(TAG, "SPECIAL LOGIC WITH 10 QUESTIONS WITH QUESTION VALUE = " + logic.getQuestion10() + "\n\n");

                if (logic.getQuestion10().equals(""))
                    return null;

                Boolean value;

                try {

                    String equation = ((((((((getValue(logic.getQuestion1(), JSON_OBJECT)
                            + logic.getQuestionLogicOperation1() + getValue(logic.getQuestion2(), JSON_OBJECT))
                            + logic.getQuestionLogicOperation2() + getValue(logic.getQuestion3(), JSON_OBJECT))
                            + logic.getQuestionLogicOperation3() + getValue(logic.getQuestion4(), JSON_OBJECT))
                            + logic.getQuestionLogicOperation4() + getValue(logic.getQuestion5(), JSON_OBJECT))
                            + logic.getQuestionLogicOperation5() + getValue(logic.getQuestion6(), JSON_OBJECT))
                            + logic.getQuestionLogicOperation6() + getValue(logic.getQuestion7(), JSON_OBJECT))
                            + logic.getQuestionLogicOperation7() + getValue(logic.getQuestion8(), JSON_OBJECT))
                            + logic.getQuestionLogicOperation8() + getValue(logic.getQuestion9(), JSON_OBJECT))
                            + logic.getQuestionLogicOperation9() + getValue(logic.getQuestion10(), JSON_OBJECT);


                    System.out.println(" EQUATION  =  " + equation);


                    value = (Boolean) engine.eval(equation);

                    System.out.println("COMPARE QUESTION VALUES Object value: " + value + "\n");

                } catch (ScriptException e) {
                    e.printStackTrace();

                    value = null;
                }


                return value;


            } else if (logic.getQuestion9() != null && !logic.getQuestion9().equals("null")) {


                Log.i(TAG, "SPECIAL LOGIC WITH 9 QUESTIONS!\n\n");


                Boolean value;

                try {


                    String equation = ((((((((getValue(logic.getQuestion1(), JSON_OBJECT)
                            + logic.getQuestionLogicOperation1() + getValue(logic.getQuestion2(), JSON_OBJECT))
                            + logic.getQuestionLogicOperation2() + getValue(logic.getQuestion3(), JSON_OBJECT))
                            + logic.getQuestionLogicOperation3() + getValue(logic.getQuestion4(), JSON_OBJECT))
                            + logic.getQuestionLogicOperation4() + getValue(logic.getQuestion5(), JSON_OBJECT))
                            + logic.getQuestionLogicOperation5() + getValue(logic.getQuestion6(), JSON_OBJECT))
                            + logic.getQuestionLogicOperation6() + getValue(logic.getQuestion7(), JSON_OBJECT))
                            + logic.getQuestionLogicOperation7() + getValue(logic.getQuestion8(), JSON_OBJECT))
                            + logic.getQuestionLogicOperation8() + getValue(logic.getQuestion9(), JSON_OBJECT));


                    System.out.println("  EQUATION  =  " + equation);


                    value = (Boolean) engine.eval(equation);

                    System.out.println("COMPARE QUESTION VALUES Object value: " + value);

                } catch (ScriptException e) {
                    e.printStackTrace();

                    value = null;
                }


                return value;


            } else if (logic.getQuestion8() != null && !logic.getQuestion8().equals("null")) {

                Log.i(TAG, " SPECIAL LOGIC WITH 8 QUESTIONS!\n\n");


                Boolean value;

                try {


                    String equation = (
                            ((((((getValue(logic.getQuestion1(), JSON_OBJECT)
                                    + logic.getQuestionLogicOperation1() + getValue(logic.getQuestion2(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation2() + getValue(logic.getQuestion3(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation3() + getValue(logic.getQuestion4(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation4() + getValue(logic.getQuestion5(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation5() + getValue(logic.getQuestion6(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation6() + getValue(logic.getQuestion7(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation7() + getValue(logic.getQuestion8(), JSON_OBJECT)
                    );


                    System.out.println("  EQUATION  =  " + equation);


                    value = (Boolean) engine.eval(equation);

                    System.out.println("COMPARE QUESTION VALUES Object value: " + value);

                } catch (ScriptException e) {
                    e.printStackTrace();

                    value = null;
                }


                return value;

            } else if (logic.getQuestion7() != null && !logic.getQuestion7().equals("null")) {


                Log.i(TAG, " SPECIAL LOGIC WITH 7 QUESTIONS!\n\n");


                Boolean value;

                try {


                    String equation = (
                            (((((getValue(logic.getQuestion1(), JSON_OBJECT)
                                    + logic.getQuestionLogicOperation1() + getValue(logic.getQuestion2(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation2() + getValue(logic.getQuestion3(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation3() + getValue(logic.getQuestion4(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation4() + getValue(logic.getQuestion5(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation5() + getValue(logic.getQuestion6(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation6() + getValue(logic.getQuestion7(), JSON_OBJECT)
                    );


                    System.out.println("  EQUATION  =  " + equation);


                    value = (Boolean) engine.eval(equation);

                    System.out.println("COMPARE QUESTION VALUES Object value: " + value);

                } catch (ScriptException e) {
                    e.printStackTrace();

                    value = null;
                }


                return value;


            } else if (logic.getQuestion6() != null && !logic.getQuestion6().equals("null")) {


                Log.i(TAG, "LOGIC SPECIAL WITH 6 QUESTIONS!\n\n");


                Boolean value;

                try {


                    String equation = (
                            ((((getValue(logic.getQuestion1(), JSON_OBJECT)
                                    + logic.getQuestionLogicOperation1() + getValue(logic.getQuestion2(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation2() + getValue(logic.getQuestion3(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation3() + getValue(logic.getQuestion4(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation4() + getValue(logic.getQuestion5(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation5() + getValue(logic.getQuestion6(), JSON_OBJECT)
                    );


                    System.out.println("  EQUATION  =  " + equation);

                    value = (Boolean) engine.eval(equation);

                    System.out.println("COMPARE QUESTION VALUES Object value: " + value);

                } catch (ScriptException e) {
                    e.printStackTrace();

                    value = null;
                }


                return value;


            } else if (logic.getQuestion5() != null && !logic.getQuestion5().equals("null")) {

                Log.i(TAG, "SPECIAL LOGIC WITH 5 QUESTIONS!\n\n");


                Boolean value;

                try {


                    String equation = (
                            (((getValue(logic.getQuestion1(), JSON_OBJECT)
                                    + logic.getQuestionLogicOperation1() + getValue(logic.getQuestion2(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation2() + getValue(logic.getQuestion3(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation3() + getValue(logic.getQuestion4(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation4() + getValue(logic.getQuestion5(), JSON_OBJECT)
                    );


                    System.out.println("  EQUATION  =  " + equation);

                    value = (Boolean) engine.eval(equation);


                    System.out.println("COMPARE QUESTION VALUES Object value: " + value);

                } catch (ScriptException e) {
                    e.printStackTrace();

                    value = null;
                }


                return value;


            } else if (logic.getQuestion4() != null && !logic.getQuestion4().equals("null")) {

                Log.i(TAG, "SPECIAL LOGIC WITH 4 QUESTIONS!\n\n");


                Boolean value;

                try {


                    String equation = (
                            ((getValue(logic.getQuestion1(), JSON_OBJECT)
                                    + logic.getQuestionLogicOperation1() + getValue(logic.getQuestion2(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation2() + getValue(logic.getQuestion3(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation3() + getValue(logic.getQuestion4(), JSON_OBJECT)
                    );


                    System.out.println("  EQUATION  =  " + equation);

                    value = (Boolean) engine.eval(equation);


                    System.out.println("COMPARE QUESTION VALUES Object value: " + value);

                } catch (ScriptException e) {
                    e.printStackTrace();

                    value = null;
                }


                return value;


            } else if (logic.getQuestion3() != null && !logic.getQuestion3().equals("null")) {


                Log.i(TAG, " SPECIAL LOGIC WITH 3 QUESTIONS!\n\n");


                Boolean value;

                try {


                    String equation = (
                            (getValue(logic.getQuestion1(), JSON_OBJECT)
                                    + logic.getQuestionLogicOperation1() + getValue(logic.getQuestion2(), JSON_OBJECT))
                                    + logic.getQuestionLogicOperation2() + getValue(logic.getQuestion3(), JSON_OBJECT)
                    );


                    System.out.println("  EQUATION  =  " + equation);

                    value = (Boolean) engine.eval(equation);


                    System.out.println("COMPARE QUESTION VALUES Object value: " + value);

                } catch (ScriptException e) {
                    e.printStackTrace();

                    value = null;
                }


                return value;


            } else if (logic.getQuestion2() != null && !logic.getQuestion2().equals("null")) {

                Log.i(TAG, " SPECIAL LOGIC WITH 2 QUESTIONS!\n\n");


                Boolean value;

                try {

                    String equation = (getValue(logic.getQuestion1(), JSON_OBJECT)
                            + logic.getQuestionLogicOperation1() + getValue(logic.getQuestion2(), JSON_OBJECT));


                    System.out.println("  EQUATION  =  " + equation);

                    value = (Boolean) engine.eval(equation);


                    System.out.println("COMPARE QUESTION VALUES Object value: " + value);

                } catch (ScriptException e) {
                    e.printStackTrace();

                    value = null;
                }


                return value;


            } else
                return null;


        }
    }


    Boolean compareAndEvaluateCascadedLogics(Logic logic, JSONObject ALL_DATA_JSON) {


        StringBuilder equation = new StringBuilder();

        String equationValue = "";


        Log.i("\n" + TAG, "------ CASCADED LOGICS  ------- \n");

        //level 1
        Log.i(TAG, " ------  LEVEL 1  ------- ");
        Boolean value1 = getLogicValue(logic, ALL_DATA_JSON);
        Log.i(TAG, "\n");
        Log.i(TAG, " Value  " + value1);
        Log.i(TAG, "\n");


        //level 2
        Log.i(TAG, " ------  LEVEL 2  ------- ");
        Log.i(TAG, "\n");


        Logic logic2 = databaseHelper.getLogic(logic.getParentLogic());
        Boolean value2 = getLogicValue(logic2, ALL_DATA_JSON);
        Log.i(TAG, "\n");
        Log.i(TAG, " Value  " + value2);
        Log.i(TAG, "\n");


        //level 3
        Log.i(TAG, " ------  LEVEL 3  ------- ");
        Log.i(TAG, "\n");

        Logic logic3 = databaseHelper.getLogic(logic2.getParentLogic());
        Boolean value3 = getLogicValue(logic3, ALL_DATA_JSON);

        Log.i(TAG, "\n");

        Log.i(TAG, " Value  " + value3);
        Log.i(TAG, "\n");


        //level 4
        Log.i(TAG, " ------  LEVEL 4  ------- ");
        Log.i(TAG, "\n");


        Logic logic4 = databaseHelper.getLogic(logic3.getParentLogic());
        Boolean value4 = getLogicValue(logic4, ALL_DATA_JSON);
        Log.i(TAG, "\n");

        Log.i(TAG, " Value  " + value4);
        Log.i(TAG, "\n");


        //level 5
        Log.i(TAG, " ------  LEVEL 5  ------- ");
        Log.i(TAG, "\n");


        Logic logic5 = databaseHelper.getLogic(logic4.getParentLogic());
        Boolean value5 = getLogicValue(logic5, ALL_DATA_JSON);
        Log.i(TAG, "\n");

        Log.i(TAG, " Value  " + value5);
        Log.i(TAG, "\n");


        equation.append(value1)
                .append(logic.getParentLogicalOperator())
                .append(value2).append(logic2.getParentLogicalOperator())
                .append(value3).append(logic3.getParentLogicalOperator())
                .append(value4).append(logic4.getParentLogicalOperator())
                .append(value5);


        equationValue = equation.toString().replace("null", "");


        Log.i(TAG, "STRING BUILDER EQUATION IS " + equation.toString());
        Log.i(TAG, "FILTERED EQUATION IS " + equationValue);


        Boolean value = null;


        try {

            value = (Boolean) engine.eval(equationValue);


        } catch (ScriptException e) {
            System.out.println("*****  Exception  *****   " + e.getMessage());


        } finally {
            System.out.println("************  LOGIC VALUE IS: " + value);

        }
        return value;


    }


    String applyCalculation(String equation) {

        System.out.println("EQUATION IS " + equation);

        String calculatedValue = "0";
        try {

            calculatedValue = calculate(equation);


        } catch (ScriptException e) {
            e.printStackTrace();
        }

        System.out.println("####### NEW VALUE IS " + calculatedValue);

        return calculatedValue;

    }


    public static class SpacesGridItemDecoration extends RecyclerView.ItemDecoration {
        private final int mSpace;

        public SpacesGridItemDecoration(int space) {
            this.mSpace = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {


            outRect.bottom = mSpace;
            // Add top margin only for the first item to avoid double space between items
            if (parent.getChildAdapterPosition(view) == 0 || parent.getChildAdapterPosition(view) == 1 || parent.getChildAdapterPosition(view) == 2) {
                outRect.top = mSpace * 2;
            } else {

                outRect.top = mSpace;

            }

            if (parent.getChildAdapterPosition(view) % 3 == 0) {
                outRect.right = mSpace * 2;
                outRect.left = mSpace;

            } else if (parent.getChildAdapterPosition(view) % 3 == 2) {
                outRect.left = mSpace * 2;
                outRect.right = mSpace;

            }

        }

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


    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }


    public boolean checkIfFarmProductionCorresponds(String farmerId) {

        Question estProdQue = databaseHelper.getQuestionByTranslation("Estimated production size");

        Double totalFarmProduction = null;

        String totalUnit = "--";

        try {
            JSONObject farmingEconomicProfileJson = new JSONObject(databaseHelper.getAllAnswersJson(farmerId));
            totalFarmProduction = round(Double.parseDouble(farmingEconomicProfileJson.get(prefs.getString("totalProduction", "")).toString().replace(",", "")), 2);
            totalUnit = farmingEconomicProfileJson.getString(prefs.getString("totalWeightUnit", ""));

        } catch (Exception e) {
            e.printStackTrace();
        }

        StringBuilder stringBuilder = new StringBuilder();

        List<RealPlot> plots = databaseHelper.getAllFarmerPlots(farmerId);


        for (RealPlot plot : plots) {
            try {

                JSONObject jsonObject = new JSONObject(plot.getPlotInformationJson());

                String estimatedProductions = "0";

                if (jsonObject.has(estProdQue.getId()) && !jsonObject.getString(estProdQue.getId()).trim().isEmpty())
                    estimatedProductions = jsonObject.getString(estProdQue.getId());

                stringBuilder.append(estimatedProductions).append("+");

            } catch (Exception e) {
                e.printStackTrace();

                stringBuilder.append("0").append("+");
            }

        }


        stringBuilder.append("0");

        Double totalPlotsProduction = round(calculateDouble(stringBuilder.toString()), 2);

        //Log.i(TAG, "$$$$$$$$$$$$$    TOTAL UNITS " + totalUnit + " AND TOTAL WEIGHTS " + stringUnitBuilder.toString());

        Log.i(TAG, "$$$$$$$$$$$$$    TOTAL FARM PROD " + totalFarmProduction + " AND TOTAL PLOTS PROD " + totalPlotsProduction);

        // if (stringUnitBuilder.toString().toLowerCase().contains(totalUnit.toLowerCase())) {
        if (totalFarmProduction != null) {
                if (totalPlotsProduction > totalFarmProduction) {
                    CustomToast.makeToast(this, getResources(R.string.error_total_plot_estimated_production), Toast.LENGTH_LONG).show();
                    return false;

                } else if (totalPlotsProduction < totalFarmProduction) {
                    CustomToast.makeToast(this, getResources(R.string.error_total_plot_estimated_production), Toast.LENGTH_LONG).show();
                    return false;

                } else return true;

            } else {
                CustomToast.makeToast(this, getResources(R.string.error_missing_estimated_production), Toast.LENGTH_LONG).show();

                return false;
            }

      /*  } else {
            CustomToast.makeToast(this, getResources(R.string.error_invalid_units), Toast.LENGTH_LONG).show();
            return false;
        }*/


    }

    public boolean checkIfFarmSizeCorresponds(String farmerCode, JSONObject ALL_FARMER_ANSWERS_JSON) {

        Question farmSizeForCocoaQuestion = databaseHelper.getQuestionByTranslation("How many hectares/acres are used for cocoa?");
        if (farmSizeForCocoaQuestion == null) {
            CustomToast.makeToast(this, "Missing question \"How many hectares/acres are used for cocoa?\" in SF", Toast.LENGTH_LONG).show();
            farmSizeForCocoaQuestion = new Question();
            farmSizeForCocoaQuestion.setId(prefs.getString("totalLandSize", ""));

        }
        Question plotSizeQue = databaseHelper.getQuestionByTranslation("Plot Size");


        Boolean booleanValue = true;

        Double farmAcre = null;
        //String totalUnit = "null";

        try {


            farmAcre = round(Double.parseDouble(ALL_FARMER_ANSWERS_JSON.get(farmSizeForCocoaQuestion.getId()).toString().replace(",", "")), 2);

            // totalUnit = farmingEconomicProfileJson.getString(prefs.getString("totalAreaUnit", ""));


        } catch (Exception e) {
            e.printStackTrace();
        }

        StringBuilder stringBuilder = new StringBuilder();
        // StringBuilder stringUnitBuilder = new StringBuilder();


        List<RealPlot> plots = databaseHelper.getAllFarmerPlots(farmerCode);

        for (RealPlot plot : plots) {

            try {
                Log.i(TAG, "******* PLOT NAME " + plot.getName() + " AND INFO JSON " + plot.getPlotInformationJson());


                String value = "0";
                JSONObject jsonObject = new JSONObject(plot.getPlotInformationJson());

                if (jsonObject.has(plotSizeQue.getId()) && !jsonObject.getString(plotSizeQue.getId()).trim().isEmpty()) {
                    value = jsonObject.getString(plotSizeQue.getId());

                }

                stringBuilder.append(value).append("+");


            } catch (JSONException e) {
                e.printStackTrace();

                stringBuilder.append("0").append("+");

            }

        }
        stringBuilder.append("0");

        Double totalSizes = round(calculateDouble(stringBuilder.toString()), 2);







        Log.i(TAG, "$$$$$$$$$$$$$    TOTAL SIZES " + farmAcre + " AND TOTAL PLOTS SIZES " + totalSizes);

        if (farmAcre != null) {
            if (totalSizes > farmAcre) {
                booleanValue = false;

                correspondingMessage = getResources(R.string.ensure_farm_acre_equal);
                CustomToast.makeToast(this, correspondingMessage, Toast.LENGTH_LONG).show();

            } else if (totalSizes < farmAcre) {

                booleanValue = false;
                correspondingMessage = getResources(R.string.ensure_farm_acre_equal);
                CustomToast.makeToast(this, correspondingMessage, Toast.LENGTH_LONG).show();

            }
        } else {

            correspondingMessage = getResources(R.string.fill_in_plot_size_values);
            booleanValue = false;
            CustomToast.makeToast(this, correspondingMessage, Toast.LENGTH_LONG).show();

        }


        return booleanValue;


    }

    public String getResources(int resource) {
        return getString(resource);
    }

    public String parseIfFormula(String formula, JSONObject jsonObject) {

        String operator = "+";

        if (formula.contains("+")) operator = "+";
        else if (formula.contains("*")) operator = "*";
        else if (formula.contains("-")) operator = "-";
        else if (formula.contains("/")) operator = "/";

        Log.i(TAG, "OPERATOR IS IS " + operator);


        StringBuilder parsedFormula = new StringBuilder();

        Double finalValue = -1.0;

        try {

            List<String> questionNames = new ArrayList<>();
            List<String> valueToCompare = new ArrayList<>();
            List<String> trueValues = new ArrayList<>();
            List<String> falseValues = new ArrayList<>();


            String[] sections = formula.split("[-+*/]");
            Log.i(TAG, "FORMULA SECTIONS SIZE IS " + sections.length);

            for (int i = 0; i < sections.length; i++) {

                sections[i] = sections[i].replace("IF", "");
                sections[i] = sections[i].replace("(", "");
                sections[i] = sections[i].replace(")", "");
                sections[i] = sections[i].replace(" ", "");


                //Log.i(TAG, "AFTER CLEANSING " + sections[i]);

                String[] discard = sections[i].split("==");
                questionNames.add(discard[0]);
                //Log.i(TAG, "QUESTION NAME " + discard[0]);


                String[] values = discard[1].split(",");

                valueToCompare.add(values[0]);
                //Log.i(TAG, "VALUE TO COMPARE " + values[0]);

                trueValues.add(values[1]);
                //Log.i(TAG, "TRUE VALUE " + values[1]);

                falseValues.add(values[2]);
                // Log.i(TAG, "FALSE VALUE " + values[2]);


            }

            // Log.i(TAG, "QUESTION NAMES SIZE = " + questionNames.size());
            // Log.i(TAG, "VALUES SIZE = " + valueToCompare.size());
            //  Log.i(TAG, "TRUE VALUES SIZE = " + trueValues.size());
            //  Log.i(TAG, "FALSE VALUES SIZE = " + falseValues.size());


            for (int i = 0; i < sections.length; i++) {

                String value = getValue(databaseHelper.getQuestionByName(questionNames.get(i)).getId(), jsonObject);
                if (!value.equals("--")) {

                    if (value.equalsIgnoreCase(valueToCompare.get(i))) {

                        parsedFormula.append(trueValues.get(i)).append(operator);

                    } else
                        parsedFormula.append(falseValues.get(i)).append(operator);

                } else parsedFormula.append("-1").append(operator);
            }

            parsedFormula.append("0");


            engine = new ScriptEngineManager().getEngineByName("rhino");
            finalValue = (Double) engine.eval(parsedFormula.toString());

            Log.i(TAG, "PARSED FORMULA IS " + parsedFormula.toString() + " = " + finalValue);

        } catch (Exception e) {
            e.printStackTrace();

        }

        return String.valueOf(finalValue.intValue());
    }

    String getValue(String id, JSONObject jsonObject) {
        String value = null;
        try {

            value = jsonObject.get(id).toString();
        } catch (JSONException ignore) {
            Log.i(TAG, ignore.getMessage());
            value = "--";
        }


        return value;
    }

    public void syncFarmerData(final Context context) {


        if (Utils.checkInternetConnection(context)) {

            UN_SYNCED_FARMERS = databaseHelper.getAllUnsyncedFarmers();

            if (UN_SYNCED_FARMERS != null && UN_SYNCED_FARMERS.size() > 0) {

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    sendAllFarmersDataToServer2(context);

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
                    syncFarmerData(context);

                }
            });

            builder.setNegativeButton(getResources(R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();

                }
            });

            builder.show();


        }




    }

    void sendAllFarmersDataToServer2(final Context context) {


        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                progressDialog = showProgress(context, "Syncing farmer data", "Please wait", false);

            }
        });


        final RealFarmer farmer = UN_SYNCED_FARMERS.get(COUNT);


        String body = buildAllFarmersJson2(farmer).toString().replace("\\", "");
        largeLog("SEND FARMER ANSWERS DATA BASE ACT", body);

        String url;

        try {
            url = FdpApplication.END_POINT + FdpApplication.REQUEST_TYPE_FARMER + URLEncoder.encode(body, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            url = FdpApplication.END_POINT + FdpApplication.REQUEST_TYPE_FARMER + body;
        }

        largeLog("URL IS ", url);


        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        System.out.println("**********************************\n");
                        Log.i(TAG, "UNFILTERED RESPONSE " + response);
                        System.out.println("\n**********************************\n");

                        final String resp = filterResponseFromServer(response);

                        Log.i(TAG, "FILTERED RESPONSE " + resp);
                        System.out.println("\n**********************************\n");


                        String submission, farmerId;

                        try {
                            JSONObject jsonObject = new JSONObject(resp);

                            submission = jsonObject.getString(Constants.SUBMISSION);
                            farmerId = jsonObject.getString(Constants.FARMER_ID);

                            System.out.println("**********************************\n");
                            System.out.println("SECOND TRY ------ SENDING PLOTS INFO!");
                            System.out.println("**********************************\n");


                            String body = buildAllFarmerPlotsJson(farmer, submission, farmerId).toString().replace("\\", "");
                            largeLog("SEND PLOTS DATA ", body);

                            String url;

                            try {
                                url = FdpApplication.END_POINT + FdpApplication.REQUEST_TYPE_PLOT + URLEncoder.encode(body, "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                                url = FdpApplication.END_POINT + FdpApplication.REQUEST_TYPE_PLOT + body;
                            }

                            largeLog("PLOTS URL IS ", url);


                            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {


                                    System.out.println("**********************************\n");
                                    Log.i(TAG, "AFTER SENDING PLOTS UNFILTERED RESPONSE " + response);
                                    System.out.println("\n**********************************\n");

                                    final String resp = filterResponseFromServer(response);

                                    Log.i(TAG, "FILTERED RESPONSE " + resp);
                                    System.out.println("\n**********************************\n");

                                    String responseCode;

                                    JSONObject jsonObject;
                                    try {
                                        jsonObject = new JSONObject(resp);
                                        responseCode = String.valueOf(jsonObject.get(Constants.RESPONSE_CODE));


                                        Log.i(TAG, "RESPONSE CODE IS " + responseCode);


                                        if (responseCode != null)
                                            if (responseCode.equalsIgnoreCase(Constants.RESPONSE_SUCCESS))
                                                databaseHelper.setFarmerAsSynced(farmer.getId());

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }


                                    moveToNextFarmer(context);


                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {

                                    System.out.println(DOUBLE_LINE);
                                    Log.i(TAG, error.getMessage() + "");
                                    System.out.println(DOUBLE_LINE);


                                    moveToNextFarmer(context);


                                }
                            });

                            MySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(DOUBLE_LINE);
                Log.i(TAG, error.getMessage() + "");
                System.out.println(DOUBLE_LINE);


                moveToNextFarmer(context);


            }
        });

        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);


    }

    void moveToNextFarmer(Context context) {

        COUNT++;

        if (COUNT < UN_SYNCED_FARMERS.size())
            sendAllFarmersDataToServer2(context);
        else
            showSyncCompleteDialog(context);

    }

    void showSyncCompleteDialog(final Context context) {
        COUNT = 0;

        title = getResources(R.string.sync_in_complete);
        message = "Error syncing data! Please retry.";


        UN_SYNCED_FARMERS = databaseHelper.getAllUnsyncedFarmers();

        if (UN_SYNCED_FARMERS != null)
            if (UN_SYNCED_FARMERS.size() > 0) {
                SYNC_STATUS = Constants.SYNC_STATUS_PARTIAL_SYNC;

                title = getResources(R.string.sync_in_complete);
                message = "Some data did not sync. Please click on retry to sync remaining data.";

            } else if (UN_SYNCED_FARMERS.size() == 0) {
                SYNC_STATUS = Constants.SYNC_STATUS_COMPLETE;

                title = getResources(R.string.sync_complete);
                message = getResources(R.string.all_data_synced);

            }


        runOnUiThread(new Runnable() {


            @Override
            public void run() {


                if (progressDialog != null)
                    progressDialog.dismiss();

                AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppDialog);
                builder.setTitle(title);
                builder.setCancelable(true);
                builder.setMessage(message);
                builder.setPositiveButton(getResources(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();

                        if (networkActivityCompleteListener != null)
                            networkActivityCompleteListener.taskComplete(SYNC_STATUS);

                    }
                });

                if (SYNC_STATUS == Constants.SYNC_STATUS_NO_SYNC || SYNC_STATUS == Constants.SYNC_STATUS_PARTIAL_SYNC)
                    builder.setNeutralButton(getResources(R.string.retry), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();

                            sendAllFarmersDataToServer2(context);

                        }
                    });

                builder.show();

            }


        });


    }

    void sendDataToServer2(final Context context, final RealFarmer farmer) {


        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                progressDialog = showProgress(context, "Syncing farmer " + farmer.getFarmerName() + "'s data", "Please wait", false);

            }
        });


        String body = buildAllFarmersJson2(farmer).toString().replace("\\", "");
        largeLog("SEND FARMER ANSWERS DATA BASE ACT", body);

        String url;

        try {
            url = FdpApplication.END_POINT + FdpApplication.REQUEST_TYPE_FARMER + URLEncoder.encode(body, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            url = FdpApplication.END_POINT + FdpApplication.REQUEST_TYPE_FARMER + body;
        }

        largeLog("URL IS ", url);


        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        System.out.println("**********************************\n");
                        Log.i(TAG, "UNFILTERED RESPONSE " + response);
                        System.out.println("\n**********************************\n");

                        final String resp = filterResponseFromServer(response);

                        Log.i(TAG, "FILTERED RESPONSE " + resp);
                        System.out.println("\n**********************************\n");


                        String submission, farmerId;

                        try {
                            JSONObject jsonObject = new JSONObject(resp);

                            submission = jsonObject.getString(Constants.SUBMISSION);
                            farmerId = jsonObject.getString(Constants.FARMER_ID);

                            System.out.println("**********************************\n");
                            System.out.println("SECOND TRY ------ SENDING PLOTS INFO!");
                            System.out.println("**********************************\n");


                            String body = buildAllFarmerPlotsJson(farmer, submission, farmerId).toString().replace("\\", "");
                            largeLog("SEND PLOTS DATA ", body);

                            String url;

                            try {
                                url = FdpApplication.END_POINT + FdpApplication.REQUEST_TYPE_PLOT + URLEncoder.encode(body, "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                                url = FdpApplication.END_POINT + FdpApplication.REQUEST_TYPE_PLOT + body;
                            }

                            largeLog("PLOTS URL IS ", url);


                            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {


                                    System.out.println("**********************************\n");
                                    Log.i(TAG, "AFTER SENDING PLOTS UNFILTERED RESPONSE " + response);
                                    System.out.println("\n**********************************\n");

                                    final String resp = filterResponseFromServer(response);

                                    Log.i(TAG, "FILTERED RESPONSE " + resp);
                                    System.out.println("\n**********************************\n");

                                    String responseCode;

                                    JSONObject jsonObject;
                                    try {
                                        jsonObject = new JSONObject(resp);
                                        responseCode = String.valueOf(jsonObject.get(Constants.RESPONSE_CODE));


                                        Log.i(TAG, "RESPONSE CODE IS " + responseCode);


                                        if (responseCode != null)
                                            if (responseCode.equalsIgnoreCase(Constants.RESPONSE_SUCCESS))
                                                databaseHelper.setFarmerAsSynced(farmer.getId());

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }


                                    showSyncCompleteDialog(context, farmer.getId());


                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {

                                    System.out.println(DOUBLE_LINE);
                                    Log.i(TAG, error.getMessage() + "");
                                    System.out.println(DOUBLE_LINE);


                                    showSyncCompleteDialog(context, farmer.getId());


                                }
                            });

                            MySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(DOUBLE_LINE);
                Log.i(TAG, error.getMessage() + "");
                System.out.println(DOUBLE_LINE);


                showSyncCompleteDialog(context, farmer.getId());


            }
        });

        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(stringRequest);


    }

    void showSyncCompleteDialog(final Context context, String farmerId) {
        final RealFarmer farmer = databaseHelper.getFarmerBasicInfo(farmerId);


        title = getResources(R.string.sync_in_complete);
        message = "Error syncing data! Please retry.";


        if (farmer != null)
            if (farmer.getSyncStatus() == 0) {

                SYNC_STATUS = Constants.SYNC_STATUS_PARTIAL_SYNC;

                title = getResources(R.string.sync_in_complete);
                message = "Some data did not sync. Please click on retry to sync remaining data.";

            } else if (farmer.getSyncStatus() == 1) {
                SYNC_STATUS = Constants.SYNC_STATUS_COMPLETE;

                title = getResources(R.string.sync_complete);
                message = getResources(R.string.all_data_synced);

            }


        runOnUiThread(new Runnable() {


            @Override
            public void run() {


                if (progressDialog != null)
                    progressDialog.dismiss();

                AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppDialog);
                builder.setTitle(title);
                builder.setCancelable(true);
                builder.setMessage(message);
                builder.setPositiveButton(getResources(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();

                        if (networkActivityCompleteListener != null)
                            networkActivityCompleteListener.taskComplete(SYNC_STATUS);

                    }
                });

                if (SYNC_STATUS == Constants.SYNC_STATUS_NO_SYNC || SYNC_STATUS == Constants.SYNC_STATUS_PARTIAL_SYNC)
                    builder.setNeutralButton(getResources(R.string.retry), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();

                            sendDataToServer2(context, farmer);

                        }
                    });

                builder.show();

            }


        });


    }

    JSONArray formatFarmerPlotsJsonStructure(String farmerCode, String date) {

        JSONArray plotsArray = new JSONArray();


        List<RealPlot> plots = databaseHelper.getAllFarmerPlots(farmerCode);

        if (plots != null && plots.size() > 0) {

            for (RealPlot plot : plots) {

                JSONObject plotObject = new JSONObject();
                try {
                    plotObject.put("plotName", plot.getName());
                    plotObject.put("plotAge", 0.0);
                    plotObject.put("plotArea", 0.0);
                    plotObject.put("latitude", 0.0);
                    plotObject.put("longitude", 0.0);
                    plotObject.put("monitoring", formatPlotMonitoringJsonStructure(plot.getId()));
                    plotObject.put("answers", formatAnswersJsonStructure(farmerCode, date, plot.getPlotInformationJson()));


                    plotsArray.put(plotObject);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }
        return plotsArray;
    }

    JSONArray formatAnswersJsonStructure(String farmerCode, String date, String answersObjectString) {

        JSONArray refinedArray = new JSONArray();

        try {

            JSONObject answersObject = new JSONObject(answersObjectString);

            Iterator i1 = answersObject.keys();


            while (i1.hasNext()) {

                String tmp_key = (String) i1.next();

                JSONObject disposableObject = new JSONObject();
                disposableObject.put("answer", answersObject.getString(tmp_key));
                disposableObject.put("question", tmp_key);
                disposableObject.put("qdate", date);
                disposableObject.put("farmerid", farmerCode);


                refinedArray.put(disposableObject);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return refinedArray;

    }

    JSONArray formatPlotMonitoringJsonStructure(String plotId) {

        JSONArray monitoringArray = new JSONArray();


        List<Monitoring> monitoringList = databaseHelper.getAllPlotMonitoring(plotId);

        if (monitoringList != null && monitoringList.size() > 0) {

            for (Monitoring monitoring : monitoringList) {

                JSONObject monitoringObject = new JSONObject();
                try {
                    monitoringObject.put("name", monitoring.getName());
                    monitoringObject.put("year", " ");
                    monitoringObject.put("result", " ");
                    monitoringObject.put("answers", formatMonitoringAnswersJsonStructure(monitoring.getJson()));

                    monitoringArray.put(monitoringObject);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }
        return monitoringArray;
    }

    JSONArray formatMonitoringAnswersJsonStructure(String answersObjectString) {

        JSONArray refinedArray = new JSONArray();
        String placeHolderQuestionId = databaseHelper.getQuestionIdByTranslationName("Place holder question");

        try {

            JSONObject answersObject = new JSONObject(answersObjectString);

            Iterator i1 = answersObject.keys();


            while (i1.hasNext()) {

                try {

                    JSONObject disposableObject = new JSONObject();


                    String competenceId;
                    String reasonForFailureId;

                    String tmp_key = (String) i1.next();
                    Question q = databaseHelper.getQuestion(tmp_key);
                    if (q != null) {

                        if (q.getRelated_Questions__c() != null) {


                            String values[] = q.getRelated_Questions__c().split(",");
                            String competenceName = values[0];
                            String reasonForFailureName = values[1];

                            competenceId = databaseHelper.getQuestionByName(competenceName).getId();
                            if (competenceId == null) competenceId = "";
                            reasonForFailureId = databaseHelper.getQuestionByName(reasonForFailureName).getId();
                            if (reasonForFailureId == null) reasonForFailureId = "";


                            disposableObject.put("answer", answersObject.getString(tmp_key));
                            disposableObject.put("question", tmp_key);

                            if (answersObject.has(competenceId)) {
                                disposableObject.put("competence", competenceId);
                                disposableObject.put("competenceAnswer", answersObject.get(competenceId));
                            } else {

                                disposableObject.put("competence", competenceId);
                                disposableObject.put("competenceAnswer", "-select-");

                            }

                            if (answersObject.has(reasonForFailureId)) {
                                disposableObject.put("reasonForFailure", reasonForFailureId);
                                disposableObject.put("reasonForFailureAnswer", answersObject.get(reasonForFailureId));
                            } else {

                                disposableObject.put("reasonForFailure", reasonForFailureId);
                                disposableObject.put("reasonForFailureAnswer", "-select-");


                            }
                        } else {

                            disposableObject.put("answer", answersObject.getString(tmp_key));
                            disposableObject.put("question", tmp_key);
                            disposableObject.put("competence", placeHolderQuestionId);
                            disposableObject.put("competenceAnswer", "none");
                            disposableObject.put("reasonForFailure", placeHolderQuestionId);
                            disposableObject.put("reasonForFailureAnswer", "none");

                        }

                    }

                    refinedArray.put(disposableObject);

                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return refinedArray;

    }

    public static void largeLog(String tag, String content) {

        if (content.length() > 4000) {
            Log.d(tag, content.substring(0, 4000));
            largeLog(tag, content.substring(4000));
        } else {
            Log.d(tag, content);
        }


    }


    String filterResponseFromServer(String response) {

        response = response.replaceAll("\\s+", "").trim();
        response = response.substring(response.indexOf("'") + 1, response.indexOf("');"));

        return response;

    }


    public JSONObject buildAllFarmersJson2(RealFarmer farmer) {


        String familyMembersId = prefs.getString("no_family_members_id", null);


        String lastSyncDate = prefs.getString("lastSync", "");

        JSONObject farmers = new JSONObject();

        JSONArray jsonArray = new JSONArray();

        Double noFamilyMembers = 0.0;

        try {

            JSONArray FARMER_ANSWERS_JSON_ARRAY = formatAnswersJsonStructure(farmer.getCode(), date, farmer.getAnswersJson());
            //JSONArray PLOTS_JSON_ARRAY = formatFarmerPlotsJsonStructure(farmer.getCode(), date);

            JSONObject answerJsonObject = new JSONObject();
            answerJsonObject.put("birthday", "10/10/" + farmer.getBirthYear());
            answerJsonObject.put("fullname", farmer.getFarmerName());
            answerJsonObject.put("farmercode", farmer.getCode());
            answerJsonObject.put("gender", farmer.getGender());
            answerJsonObject.put("village", farmer.getVillage());
            answerJsonObject.put("start", farmer.getFirstVisitDate());
            answerJsonObject.put("endSurvey", "01/02/2018 12:23 PM");
            answerJsonObject.put("educationalLevel", farmer.getEducationLevel());
            answerJsonObject.put("familyMembers", 1.0);
            answerJsonObject.put("latitude", 0.0);
            answerJsonObject.put("longitude", 0.0);
            answerJsonObject.put("houseAddress", "");
            answerJsonObject.put("lengthOfRelationship", 1);
            answerJsonObject.put("phoneNumber", 1234567890);
            answerJsonObject.put("farmerGroup", "");
            answerJsonObject.put("reasonForRetreat", "");
            answerJsonObject.put("registrationDate", "01/02/2018");
            answerJsonObject.put("status", "");
            answerJsonObject.put("surveyor", prefs.getString(Constants.USER_UID, null));
            answerJsonObject.put("answers", FARMER_ANSWERS_JSON_ARRAY);
            answerJsonObject.put("plots", new JSONArray());

            jsonArray.put(answerJsonObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }


        try {
            farmers.put("farmers", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        // largeLog("SEND DATA BASE ACTIVITY", farmers.toString());

        return farmers;
    }


    public JSONObject buildAllFarmerPlotsJson(RealFarmer farmer, String submission, String farmerCode) {


        String familyMembersId = prefs.getString("no_family_members_id", null);


        String lastSyncDate = prefs.getString("lastSync", "");

        JSONObject farmers = new JSONObject();

        JSONArray jsonArray = new JSONArray();

        Double noFamilyMembers = 0.0;

        try {

            //JSONArray FARMER_ANSWERS_JSON_ARRAY = formatAnswersJsonStructure(farmer.getCode(), date, farmer.getAnswersJson());
            JSONArray PLOTS_JSON_ARRAY = formatFarmerPlotsJsonStructure(farmer.getCode(), date);

            JSONObject answerJsonObject = new JSONObject();
            answerJsonObject.put("birthday", "10/10/" + farmer.getBirthYear());
            answerJsonObject.put("fullname", farmer.getFarmerName());
            answerJsonObject.put("farmercode", farmerCode);
            answerJsonObject.put("gender", farmer.getGender());
            answerJsonObject.put("village", farmer.getVillage());
            answerJsonObject.put("start", farmer.getFirstVisitDate());
            answerJsonObject.put("endSurvey", "01/02/2018 12:23 PM");
            answerJsonObject.put("educationalLevel", farmer.getEducationLevel());
            answerJsonObject.put("familyMembers", 1.0);
            answerJsonObject.put("latitude", 0.0);
            answerJsonObject.put("longitude", 0.0);
            answerJsonObject.put("houseAddress", "");
            answerJsonObject.put("lengthOfRelationship", 1);
            answerJsonObject.put("phoneNumber", 1234567890);
            answerJsonObject.put("farmerGroup", "");
            answerJsonObject.put("reasonForRetreat", "");
            answerJsonObject.put("registrationDate", "01/02/2018");
            answerJsonObject.put("status", submission);
            answerJsonObject.put("surveyor", prefs.getString(Constants.USER_UID, null));
//            answerJsonObject.put("answers", new JSONArray());
            answerJsonObject.put("plots", PLOTS_JSON_ARRAY);

            jsonArray.put(answerJsonObject);

        } catch (JSONException e) {
            e.printStackTrace();
        }


        try {
            farmers.put("farmers", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        // largeLog("SEND DATA BASE ACTIVITY", farmers.toString());

        return farmers;
    }


    boolean checkIfFarmerFdpStatusFormFilled(String farmerId) {

        boolean value = false;
        Question agreeWithPlanQuestion = databaseHelper.getQuestionByTranslation("Agree with plan");

        if (agreeWithPlanQuestion != null) {


            JSONObject answersJson;

            String json = databaseHelper.getAllAnswersJson(farmerId);

            try {
                answersJson = new JSONObject(json);
            } catch (JSONException e) {
                e.printStackTrace();
                answersJson = new JSONObject();
            }


            if (answersJson.has(agreeWithPlanQuestion.getId())) {

                String answer;
                try {
                    answer = answersJson.getString(agreeWithPlanQuestion.getId());
                } catch (JSONException e) {
                    e.printStackTrace();
                    answer = null;
                }


                if (answer != null && !answer.equalsIgnoreCase("-select-"))
                    value = true;

            }


        } else {
            CustomToast.makeToast(this, "DEBUG: Agree with plan question is missing!", Toast.LENGTH_LONG).show();
            return false;

        }

        return value;

    }


    public void goBack(@Nullable View v) {
        onBackPressed();
    }


    String captureScreenshot(View v, String activityName) {
        String fileLocation = null;

        String dir = ROOT_DIR + "/screenCaptures/";

        File file = new File(dir);
        if (!file.exists()) file.mkdirs();


        try {
            // image naming and path  to include sd card  appending name you choose for file
            fileLocation = dir + activityName + ".jpg";


            // create bitmap screen capture
            v.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v.getDrawingCache());
            v.setDrawingCacheEnabled(false);

            File imageFile = new File(fileLocation);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();

        } catch (Throwable e) {
            // Several error may come out with file handling or DOM
            e.printStackTrace();
        }
        return fileLocation;
    }



}
