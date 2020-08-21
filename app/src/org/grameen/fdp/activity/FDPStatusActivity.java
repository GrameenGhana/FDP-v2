package org.grameen.fdp.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.jaredrummler.materialspinner.MaterialSpinner;
import com.squareup.picasso.Picasso;

import org.grameen.fdp.R;
import org.grameen.fdp.fragment.MyFormFragment;
import org.grameen.fdp.object.RealFarmer;
import org.grameen.fdp.utility.Constants;
import org.grameen.fdp.utility.CustomToast;
import org.json.JSONObject;

import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by aangjnr on 09/11/2017.
 */

public class FDPStatusActivity extends BaseActivity {
    EditText farmerName;
    EditText farmerCode;
    TextView takePhoto;
    MaterialSpinner villageSpinner;
    MaterialSpinner educationLevelSpinner;
    MaterialSpinner genderSpinner;
    String gender = "";
    String village = "";
    String education = "";
    EditText birthYearEdittext;
    CircleImageView circleImageView;
    TextView initials;
    RealFarmer farmer;
    boolean isEditMode = false;
    boolean newFarmer = false;
    Button cancel;
    Button save;
    String formType = "";
    MyFormFragment formFragment;
    String[] educationLevels = {"Primary", "Secondary", "Tertiary", "Professional Course", "Other"};
    String[] genders = {"Male", "Female"};
    private boolean newDataSaved = false;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Intent intent = getIntent();
        setContentView(R.layout.activity_edit_farmer_details);


        cancel = (Button) findViewById(R.id.back);
        save = (Button) findViewById(R.id.save);

        farmerName = findViewById(R.id.farmerName);

        farmerCode = findViewById(R.id.farmerCode);
        birthYearEdittext = findViewById(R.id.birthdayYearEdittext);

        takePhoto = (TextView) findViewById(R.id.takeImage);
        initials = (TextView) findViewById(R.id.initials);
        villageSpinner = (MaterialSpinner) findViewById(R.id.villageSpinner);
//        villageSpinner.setItems(databaseHelper.getAllVillageNames());






        educationLevelSpinner = (MaterialSpinner) findViewById(R.id.educationLevelSpinner);



        genderSpinner = (MaterialSpinner) findViewById(R.id.genderSpinner);


        circleImageView = (CircleImageView) findViewById(R.id.photo);

        farmer = new Gson().fromJson(getIntent().getStringExtra("farmer"), RealFarmer.class);
        formType = Constants.FDP_STATUS;


        farmerName.setEnabled(false);
        farmerCode.setEnabled(false);
        villageSpinner.setEnabled(false);
        educationLevelSpinner.setEnabled(false);
        genderSpinner.setEnabled(false);
        birthYearEdittext.setEnabled(false);
        takePhoto.setVisibility(View.GONE);


        Log.d("ACTION TYPE", prefs.getString("flag", ""));
        if (prefs.getString("flag", "").equals(Constants.MONITORING)) {

            //Todo hide views, load farmer details with tag
            Toolbar toolbar = setToolbar("View Farmer Details");
            //save.setVisibility(View.GONE);
            cancel.setText(getResources(R.string.back));
            setUpViews();

            formFragment = MyFormFragment.newInstance(formType, true, farmer.getId(), true);


        } else {

            //Todo load farmer details with tag


                Toolbar toolbar = setToolbar(getResources(R.string.fdp_status));
                isEditMode = true;
                newFarmer = false;

                setUpViews();
            formFragment = MyFormFragment.newInstance(formType, true, farmer.getId(), false);


        }


        loadDynamicView(formType);
        onBackClicked();
    }

    @Override
    protected void onPause() {


        Log.i(TAG, "ON PAUSE");

        if (prefs.getBoolean("shouldSave", false))

            if (!newDataSaved && isEditMode) {
                Log.i(TAG, "SAVING NEW DATA NOW!!!");

                if (!farmerName.getText().toString().equals(""))
                    saveOrUpdateData(false);

            }

        super.onPause();


    }

    @Override
    public void onBackPressed() {


        Log.i(TAG, "ON BACK PRESSED");

        if (!newDataSaved && isEditMode) {
            Log.i(TAG, "NEW DATA NOT SAVED, SAVING NEW DATA NOW!!!");


            if (!farmerName.getText().toString().equals("")) {

                showAlertDialog(true, getResources(R.string.save_data), getResources(R.string.save_data_explanation), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        prefs.edit().putBoolean("shouldRestartActivity", true).apply();

                        dialogInterface.dismiss();

                        saveOrUpdateData(false);
                        finish();


                    }
                }, getResources(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        newDataSaved = true;
                        prefs.edit().putBoolean("shouldRestartActivity", false).apply();


                        dialogInterface.dismiss();
                        finish();

                    }
                }, getResources(R.string.no), 0);


            } else finish();


        } else {
            finish();
        }

    }

    void loadDynamicView(final String formType) {

        //Todo add parameter to load data from the database, if is in editing mode else display default forms with their resp values


        FragmentManager fm = getSupportFragmentManager();

        fm.beginTransaction()
                .add(R.id.dynamicLayout, formFragment, formFragment.getClass().getSimpleName())
                .commit();


        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                saveOrUpdateData(true);


            }
        });


    }

    void saveOrUpdateData(boolean shouldLoadNextActivity) {

        save.setEnabled(false);
        JSONObject newJsonValue = formFragment.getAllAnswersInJSONObject();

        Log.d(TAG, "JSON VALUE IS + \n" + newJsonValue + "\n");


        if (databaseHelper.editAllAnswersJson(farmer.getId(), newJsonValue)) {

                    newDataSaved = true;

                    CustomToast.makeToast(FDPStatusActivity.this, "Farmer data updated successfully!", Toast.LENGTH_SHORT).show();

                    finish();

                } else
                    CustomToast.makeToast(FDPStatusActivity.this, "Could not save farmer " + formType + ". Please try again", Toast.LENGTH_SHORT).show();





        save.setEnabled(true);



    }

    void setUpViews() {

        farmerName.setText(farmer.getFarmerName());
        farmerCode.setText(farmer.getCode());
        birthYearEdittext.setText(farmer.getBirthYear());


        village = farmer.getVillage();
        villageSpinner.setText(village.toUpperCase());

        education = farmer.getEducationLevel();
        educationLevelSpinner.setText(education);

        gender = farmer.getGender();
        genderSpinner.setText(gender);




        if (farmer.getImageUrl() != null && !farmer.getImageUrl().equals("")) {
            Picasso.with(this).load(farmer.getImageUrl()).resize(200, 200).into(circleImageView);

        } else {


            try {
                String[] valueArray = farmer.getFarmerName().split(" ");
                String value = valueArray[0].substring(0, 1) + valueArray[1].substring(0, 1);
                initials.setText(value);

            } catch (Exception e) {

                if (!farmer.getFarmerName().trim().isEmpty())
                    initials.setText(farmer.getFarmerName().substring(0, 1));


            }


            int[] mColors = getResources().getIntArray(R.array.recommendations_colors);

            int randomColor = mColors[new Random().nextInt(mColors.length)];

            GradientDrawable drawable = new GradientDrawable();
            drawable.setCornerRadius(1000);
            drawable.setColor(randomColor);
            circleImageView.setBackground(drawable);
        }


    }




}
