package org.grameen.fdp.utility;


import android.os.Environment;

import java.io.File;

/**
 * Created by aangjnr on 11/06/2017.
 */

public class Constants {


    public static String DB_NAME = "fdp.db";





    public static final String FORM_DIAGNOSTIC = "diagnostic";
    public static final String FORM_DIAGNOSTIC_MONITORING = "diagnostic_monitoring";

    public static final String FORM_MONITORING = "monitoring";

    public static final String YES = "Yes";
    public static final String NO = "No";

    public static final String RECORD_EDIT = "edit";
    public static final String RECORD_NEW = "new";




    public static final String TYPE_TEXT = "text";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_NUMBER_DECIMAL = "decimal";
    public static final String TYPE_COMPLEX_CALCULATION = "complex calculation";

    public static final String ANSWERS = "fpd_Answer__c";

    public static final String EMPTY_STRING = "";
    public static final String RESPONSE_SUCCESS = "0";
    public static final String RESOPNSE_ERROR = "1";

    public static final String SUBMISSION = "submission";
    public static final String RESPONSE_CODE = "responseCode";
    public static final String FARMER_ID = "farmerId";

    public static final int SYNC_STATUS_COMPLETE = 0;
    public static final int SYNC_STATUS_PARTIAL_SYNC = 1;
    public static final int SYNC_STATUS_NO_SYNC = -1;
    public static final String ID = "Id";

    public static final String NULL_STRING = "null";
    public static final String TYPE_LOCATION = "geolocation";
    public static final String TYPE_PHOTO = "photo";

    public static final String TYPE_SELECTABLE = "single select";
    public static final String TYPE_MULTI_SELECTABLE = "multi select";

    public static final String TYPE_CHECKBOX = "checkbox";
    public static final String TYPE_TIMEPICKER = "timePicker";
    public static final String TYPE_DATEPICKER = "date";

    public static final String TYPE_MATH_FORMULA = "math formula";
    public static final String TYPE_LOGIC_FORMULA = "logic formula";


    public static final String SHOW = "false";
    public static final String HIDE = "true";


    public static final String FARMER_PROFILE = "farmer profile";
    public static final String ADOPTION_OBSERVATION_RESULTS = "adoption observation results";
    public static final String AGGREGATE_ECONOMIC_RESULTS = "agregate economic results";
    public static final String OTHER = "other";
    public static final String ADDITIONAL_INTERVENTION = "additional intervention";
    public static final String ADOPTION_OBSERVATIONS = "adoption observations";
    public static final String PRODUCTIVE_PROFILE = "productive profile";
    public static final String FARMING_ECONOMIC_PROFILE = "farming economic profile";
    public static final String SOCIO_ECONOMIC_PROFILE = "socioeconomic profile";
    public static final String FAMILY_MEMBERS = "family members";
    public static final String PLOT_RESULTS = "plot results";
    public static final String PLOT_INFORMATION = "plot information";
    public static final String AO_MONITORING = "ao monitoring";
    public static final String AO_MONITORING_RESULT = "ao monitoring result";
    public static final String MONITORING_PLOT_INFORMATION = "monitoring plot information";
    public static final String COMPETENCE_MONITORING = "competence monitoring";
    public static final String FAILURE_MONITORING = "failure monitoring";
    public static final String FDP_STATUS = "fdp status";
    public static final String DIAGNOSTIC = " register";
    public static final String MONITORING = "monitoring";
    public static final int PLACE_PICKER_REQUEST = 101;
    public static final int PERMISSION_FINE_LOCATION = 102;
    public static final int PERMISSION_CALL = 222;
    public static final String TAG_TITLE_TEXT_VIEW = "titleTag";
    public static final String BUTTON_VIEW = "buttonTag";
    public static final String TAG_OTHER_TEXT_VIEW = "textTag";
    public static final String TAG_CALCULATION = "calculationTag";
    public static final String TAG_VIEW = "viewTag";
    public static final String TAG_RESULTS = "resultsTag";
    public static String IS_USER_SIGNED_IN = "isUserSignedIn";
    public static String IS_RETAILER_SIGNED_IN = "isUserSignedIn";
    public static String IS_GAME_CENTER_SIGNED_IN = "isUserSignedIn";
    public static String USERS_PHONE = "phoneNo";
    public static String USER_NAME = "userName";
    public static String USER_AGE = "userAge";
    public static String USER_EMAIL = "userEmail";
    public static String USER_UID = "userUid";
    public static String USER_TOKEN = "userToken";
    public static String USER_PHOTO_LOCAL_URL = "userProfilePhotoLocal";
    public static String USER_PHOTO_CLOUD_URL = "userProfilePhotoCloud";
    public static String IS_NIGHT_MODE = "isNightMode";
    public static int LAST_SELECTED_FRAGMENT = 1;
    public static int SYNC_OK = 1;
    public static int SYNC_NOT_OK = 0;

    public static final String NO_MONITORING_PLACE_HOLDER = "Please_fill_out_all_AO_Monitoring_data";


}
