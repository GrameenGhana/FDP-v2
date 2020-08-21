package org.grameen.fdp.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.jaredrummler.materialspinner.MaterialSpinner;

import org.grameen.fdp.R;
import org.grameen.fdp.fragment.MyFormFragment;
import org.grameen.fdp.object.Form;
import org.grameen.fdp.object.Question;
import org.grameen.fdp.object.RealFarmer;
import org.grameen.fdp.utility.Constants;
import org.grameen.fdp.utility.CustomToast;
import org.grameen.fdp.utility.DateUtil;
import org.grameen.fdp.utility.ImageUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static org.grameen.fdp.application.FdpApplication.ROOT_DIR;

/**
 * Created by aangjnr on 09/11/2017.
 */

public class Add_EditFarmerDetailsActivity extends BaseActivity{

    public static int CAMERA_INTENT = 90;
    public static int PERMISSION_CAMERA = 11;

    static String TAG = Add_EditFarmerDetailsActivity.class.getSimpleName();
    String IMAGE_URL = "";
    Uri URI;

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
    String formLabel = "";
    String BASE64_STRING = "";

    Question educationLevelQuestion;
    Question genderQuestion;

    MyFormFragment formFragment;
    String[] educationLevels = {"Primary", "Secondary", "Tertiary", "Professional Course", "Other"};
    String[] genders = {"Male", "Female"};
    private boolean newDataSaved = false;
    FragmentManager fm;


    public static void createNoMediaFile() {
        FileOutputStream out = null;

        try {

            File ROOT = new File(ROOT_DIR);

            if (!ROOT.exists())
                if (ROOT.mkdirs()) {
                    Log.i(TAG, "ROOT dir file created!  " + ROOT);
                }

            File thumbnailsDir = new File(ROOT + File.separator + ".thumbnails");
            if (!thumbnailsDir.exists())
                if (thumbnailsDir.mkdirs()) {

                    Log.i(TAG, "Thumbnails dir file created!  " + thumbnailsDir);
                }


            File file = new File(ROOT + File.separator, ".nomedia");
            if (!file.exists()) {
                out = new FileOutputStream(file);
                out.write(0);
                out.close();


                Log.i(TAG, "No media file created!  " + file);
            } else {
                Log.i(TAG, "No media already exists!!!!!!  " + file);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String generateThumbnail(String path, String destinationName) {
        File file = new File(path);

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();

        bitmapOptions.inJustDecodeBounds = true; // obtain the size of the image, without loading it in memory
        BitmapFactory.decodeFile(file.getAbsolutePath(), bitmapOptions);

        int desiredWidth = 400;
        int desiredHeight = 300;

        float widthScale = (float) bitmapOptions.outWidth / desiredWidth;
        float heightScale = (float) bitmapOptions.outHeight / desiredHeight;

        float scale = Math.min(widthScale, heightScale);

        int sampleSize = 1;
        while (sampleSize < scale) {
            sampleSize *= 2;
        }
        bitmapOptions.inSampleSize = sampleSize; // this value must be a power of 2,
        // this is why you can not have an image scaled as you would like
        bitmapOptions.inJustDecodeBounds = false; // now we want to load the image

        Bitmap thumbnail = BitmapFactory.decodeFile(String.valueOf(file), bitmapOptions);

        File thumbnailFile = new File(ROOT_DIR + File.separator + ".thumbnails", destinationName + ".jpg");

        FileOutputStream fos = null;


        try {
            fos = new FileOutputStream(thumbnailFile);
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 70, fos);

            Log.i(TAG, "Saving thumbnail " + thumbnailFile);
            return thumbnailFile.getAbsolutePath();

        } catch (FileNotFoundException e) {
            e.printStackTrace();

            thumbnail.recycle();
            return "null";

        } finally {
            // Not really required
            try {

                thumbnail.recycle();
                if (fos != null) {
                    fos.flush();
                    fos.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static String saveImage(Bitmap imageBitmap, String destinationName) {

        OutputStream fOut = null;
        File dir = new File(ROOT_DIR + File.separator + "Farmer Images");
        if (!dir.exists()) Log.i(TAG, "Is DIR created?  " + dir.mkdirs());
        Log.i(TAG, "Destination path is " + dir);

        File save_image = new File(dir, destinationName + ".jpg");
        try {
            fOut = new FileOutputStream(save_image);
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            Log.i(TAG, "Saving edited screenshot with name" + save_image);
            return save_image.getAbsolutePath();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "null";
        } finally {
            // Not really required
            try {

                if (fOut != null) {
                    fOut.flush();
                    fOut.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNoMediaFile();
        Intent intent = getIntent();
        setContentView(R.layout.activity_edit_farmer_details);
        fm = getSupportFragmentManager();

        TextView farmer_name_text = findViewById(R.id.farmer_name_text);
        Question temp = databaseHelper.getQuestionByTranslation("Farmer Name");
        if(temp != null)
            farmer_name_text.setText((prefs.getBoolean("toggleTranslation", false)) ? temp.getTranslation__c() : temp.getCaption__c());

        TextView farmer_code_text = findViewById(R.id.farmer_code_text);
        temp = databaseHelper.getQuestionByTranslation("Farmer Code");
        if(temp != null)
            farmer_code_text.setText((prefs.getBoolean("toggleTranslation", false)) ? temp.getTranslation__c() : temp.getCaption__c());

        TextView village_text = findViewById(R.id.village_text);
        temp = databaseHelper.getQuestionByTranslation("Village");
        if(temp != null)
            village_text.setText((prefs.getBoolean("toggleTranslation", false)) ? temp.getTranslation__c() : temp.getCaption__c());


        TextView educational_level_text = findViewById(R.id.education_level_text);
        educationLevelQuestion = databaseHelper.getQuestionByTranslation("Highest education level graduated");
        if (educationLevelQuestion != null)
            educational_level_text.setText((prefs.getBoolean("toggleTranslation", false)) ? educationLevelQuestion.getTranslation__c() : educationLevelQuestion.getCaption__c());


        TextView birthyear_text = findViewById(R.id.birth_year_text);
        temp = databaseHelper.getQuestionByTranslation("Birthday Year");
        if(temp != null)
            birthyear_text.setText((prefs.getBoolean("toggleTranslation", false)) ? temp.getTranslation__c() : temp.getCaption__c());

        TextView gender_text = findViewById(R.id.gender_text);
        genderQuestion = databaseHelper.getQuestionByTranslation("Gender");
        if (genderQuestion != null)
            gender_text.setText((prefs.getBoolean("toggleTranslation", false)) ? genderQuestion.getTranslation__c() : genderQuestion.getCaption__c());

        cancel = (Button) findViewById(R.id.back);
        save = (Button) findViewById(R.id.save);

        farmerName = findViewById(R.id.farmerName);
        farmerName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                if (editable.length() < 3) {
                    save.setEnabled(false);

                    farmerName.setError(getResources(R.string.full_name_error));

                } else save.setEnabled(true);
                farmerName.setError(null);
            }
        });


        farmerCode = findViewById(R.id.farmerCode);
        birthYearEdittext = findViewById(R.id.birthdayYearEdittext);

        takePhoto = (TextView) findViewById(R.id.takeImage);
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCameraIntent();
            }
        });


        initials = (TextView) findViewById(R.id.initials);

        villageSpinner = (MaterialSpinner) findViewById(R.id.villageSpinner);
        villageSpinner.setItems(databaseHelper.getAllVillageNames());

        try {
            villageSpinner.setSelectedIndex(0);
            village = villageSpinner.getItems().get(0).toString();

        } catch (Exception e) {
            e.printStackTrace();

            CustomToast.makeToast(this, "Missing villages data!", Toast.LENGTH_LONG).show();
        }


        villageSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {
                village = item.toString();

                Log.i(TAG, "^^^^^^^^^^ ON VILLAGE ITEM SELECTED " + village);


            }
        });

        educationLevelSpinner = (MaterialSpinner) findViewById(R.id.educationLevelSpinner);

        try {
            educationLevelSpinner.setItems(educationLevelQuestion.formatQuestionOptions());


        }catch(Exception e){e.printStackTrace();

        educationLevelSpinner.setItems(educationLevels);
        }


        education = educationLevelSpinner.getItems().get(0).toString();


        educationLevelSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {
                education = item.toString();
                Log.i(TAG, "^^^^^^^^^^ ON EDUCATION ITEM SELECTED " + education);
            }
        });


        genderSpinner = (MaterialSpinner) findViewById(R.id.genderSpinner);
        try {
            genderSpinner.setItems(genderQuestion.formatQuestionOptions());


        }catch(Exception e) {
            e.printStackTrace();

            genderSpinner.setItems(genders);
        }
        gender = genderSpinner.getItems().get(0).toString();

        genderSpinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, Object item) {

                gender = item.toString();

                Log.i(TAG, "^^^^^^^^^^ ON GENDER ITEM SELECTED " + gender);

            }
        });


        circleImageView = (CircleImageView) findViewById(R.id.photo);

        farmer = new Gson().fromJson(getIntent().getStringExtra("farmer"), RealFarmer.class);
        formLabel = intent.getStringExtra("formLabel");
        type = intent.getStringExtra("type");



        Log.d("ACTION TYPE", prefs.getString("flag", ""));
        if (IS_MONITIRING_MODE) {

            //Todo hide views, load farmer details with tag
            Toolbar toolbar = setToolbar("View Farmer Details");

            if(type.equalsIgnoreCase(Constants.FORM_DIAGNOSTIC))
            save.setVisibility(View.GONE);


            cancel.setText("BACK");
            farmerName.setEnabled(false);
            farmerCode.setEnabled(false);
            villageSpinner.setEnabled(false);
            educationLevelSpinner.setEnabled(false);
            genderSpinner.setEnabled(false);
            birthYearEdittext.setEnabled(false);
            takePhoto.setVisibility(View.GONE);

            setUpViews();

            formFragment = MyFormFragment.newInstance(formLabel, true, farmer.getId(), true);


        } else {
            //Todo load farmer details

            if (intent.getStringExtra("flag") != null && intent.getStringExtra("flag").equals("edit")) {

                Toolbar toolbar = setToolbar(getResources(R.string.edit_farmer_details));

                isEditMode = true;
                newFarmer = false;

                setUpViews();

                if (farmer.getHasSubmitted().equalsIgnoreCase(Constants.YES) && farmer.getSyncStatus() == 1) {
                    save.setVisibility(View.GONE);
                    cancel.setText("BACK");
                    farmerName.setEnabled(false);
                    farmerCode.setEnabled(false);
                    villageSpinner.setEnabled(false);
                    educationLevelSpinner.setEnabled(false);
                    genderSpinner.setEnabled(false);
                    birthYearEdittext.setEnabled(false);
                    takePhoto.setVisibility(View.GONE);
                    formFragment = MyFormFragment.newInstance(formLabel, true, farmer.getId(), true);
                } else
                    formFragment = MyFormFragment.newInstance(formLabel, true, farmer.getId(), false);


            } else {

                isEditMode = true;
                newFarmer = true;

                //Todo load ui views with tag default farmer profile
                Toolbar toolbar = setToolbar(getResources(R.string.add_farmer_details));
                formLabel = Constants.FARMER_PROFILE;
                formFragment = MyFormFragment.newInstance(formLabel, false, null, false);


                farmerCode.setText(getSaltString());
                save.setEnabled(false);

            }
        }


        loadDynamicView(formLabel);
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

        if (farmer != null && farmer.getHasSubmitted().equalsIgnoreCase(Constants.YES) && farmer.getSyncStatus() == 1)
            finish();
        else if (!newDataSaved && isEditMode) {
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

    void loadDynamicView(String formName) {

        //Todo add parameter to load data from the database, if is in editing mode else display default forms with their resp values

        try {
           // if (fm.getFragments().contains(formFragment)) {

                fm.beginTransaction()
                        .replace(R.id.dynamicLayout, formFragment, formName)
                        .addToBackStack(null)
                        .commit();
                //Log.i(TAG, "^^^^^^^^^   FRAGMENT FOUND! RECYCLING....  ^^^^^^^^^^^^^^^");

//            }else{
//
//
//                fm.beginTransaction()
//                        .add(R.id.dynamicLayout, formFragment, formFragment.getClass().getSimpleName())
//                        .commit();
//
//                Log.i(TAG, "^^^^^^^^^  NEW FRAGMENT ....  ^^^^^^^^^^^^^^^");
//
//            }

        }catch(Exception e){e.printStackTrace();}





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

        prefs.edit().putBoolean("shouldRestartMainActivity", true).apply();

        if (newFarmer) {

            String date = DateUtil.getFormattedDateMMDDYYYYhhmmaa();
            //Todo add new farmer, save details finish this activity and go back to farmer details activity

            RealFarmer _farmer = new RealFarmer();
            _farmer.setId(String.valueOf(System.currentTimeMillis()));
            _farmer.setFarmerName(farmerName.getText().toString());
            _farmer.setCode(farmerCode.getText().toString());
            _farmer.setVillage(village);
            _farmer.setEducationLevel(education);
            _farmer.setBirthYear(birthYearEdittext.getText().toString());
            _farmer.setGender(gender);
            _farmer.setImageUrl(IMAGE_URL);
            _farmer.setFirstVisitDate(date);
            _farmer.setLastVisitDate(date);
            _farmer.setLastModifiedDate(date);
            _farmer.setSyncStatus(0);
            _farmer.setImageUrl(BASE64_STRING);

            if (databaseHelper.addNewFarmer(_farmer)) {

                databaseHelper.editAllAnswersJson(_farmer.getId(), newJsonValue);

                newDataSaved = true;

                if (shouldLoadNextActivity) {
                    CustomToast.makeToast(Add_EditFarmerDetailsActivity.this, getResources(R.string.new_farmer_added), Toast.LENGTH_SHORT).show();

                    /*Intent intent = new Intent(Add_EditFarmerDetailsActivity.this, FarmerDetailsActivity.class);
                    intent.putExtra("farmer", new Gson().toJson(_farmer));
                    intent.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);

                    finish();*/

                    farmer = _farmer;

                    loadNextForm();



                }

            } else
                CustomToast.makeToast(Add_EditFarmerDetailsActivity.this, getResources(R.string.new_farmer_not_added), Toast.LENGTH_SHORT).show();


        } else {  //Todo save details finish this activity and go back to farmer details activity


            farmer.setFarmerName(farmerName.getText().toString());
            farmer.setCode(farmerCode.getText().toString());
            farmer.setVillage(village);
            farmer.setEducationLevel(education);
            farmer.setBirthYear(birthYearEdittext.getText().toString());
            farmer.setGender(gender);
            farmer.setImageUrl(BASE64_STRING);
            //farmer.setFirstVisitDate(null);
            farmer.setLastVisitDate(farmer.getLastVisitDate());
            farmer.setLastModifiedDate(DateUtil.getFormattedDateMMDDYYYYhhmmaa());
            farmer.setSyncStatus(0);



            if (databaseHelper.editFarmerBasicInfo(farmer)) {
                //String value = databaseHelper.getAllAnswersJson(formType, farmer.getCode());

                //if (value == null) value = "empty";


                if (databaseHelper.editAllAnswersJson(farmer.getId(), newJsonValue)) {

                    newDataSaved = true;

                    if (shouldLoadNextActivity) {
                        CustomToast.makeToast(Add_EditFarmerDetailsActivity.this, getResources(R.string.farmer_updated), Toast.LENGTH_SHORT).show();

                       /* Intent intent = new Intent(Add_EditFarmerDetailsActivity.this, FarmerDetailsActivity.class);
                        intent.putExtra("farmer", new Gson().toJson(farmer));
                        intent.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();*/

                       loadNextForm();
                    }

                } else
                    CustomToast.makeToast(Add_EditFarmerDetailsActivity.this, getResources(R.string.could_not_save) + formLabel + getResources(R.string.try_again_suffix), Toast.LENGTH_SHORT).show();


            } else
                CustomToast.makeToast(Add_EditFarmerDetailsActivity.this, getResources(R.string.could_not_save) + formLabel + getResources(R.string.try_again_suffix), Toast.LENGTH_SHORT).show();


        }

        save.setEnabled(true);

        prefs.edit().putBoolean("shouldRestartMainActivity", true).apply();


    }

    void setUpViews() {

        farmerName.setText(farmer.getFarmerName());
        farmerCode.setText(farmer.getCode());

        if (farmer.getBirthYear() != null && !farmer.getBirthYear().isEmpty())
            birthYearEdittext.setText(farmer.getBirthYear());
        else
            birthYearEdittext.setText("0000");



        village = farmer.getVillage();
        villageSpinner.setText(village.toUpperCase());

        education = farmer.getEducationLevel();
        educationLevelSpinner.setText(education);

        gender = farmer.getGender();
        genderSpinner.setText(gender);


        BASE64_STRING = farmer.getImageUrl();
        if (farmer.getImageUrl() != null && !farmer.getImageUrl().equals("")) {
            try {
                circleImageView.setImageBitmap(ImageUtil.base64ToBitmap(BASE64_STRING));

            } catch (Exception ignored) {
            }

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

    void startCameraIntent() {

        File photo;

        Intent takePictureIntent;
        prefs.edit().putBoolean("shouldSave", false).apply();

        if (!hasPermissions(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(Add_EditFarmerDetailsActivity.this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
        } else {


            takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            try {
                // place where to store camera taken picture
                photo = this.createTemporaryFile("picture", ".jpg");
                photo.delete();
                //URI = Uri.fromFile(photo);
                URI = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".org.grameen.fdp.provider", photo);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, URI);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);


            } catch (Exception e) {
                Log.v(TAG, "Can't create file to take picture!");
                //Toast.makeText(activity, "Please check SD card! Image shot is impossible!", 10000);
            }




            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

                Log.d(TAG, "Starting camera intent");

                startActivityForResult(takePictureIntent, CAMERA_INTENT);

            }


        }
    }

    private File createTemporaryFile(String part, String ext) throws Exception {

        File dir = new File(ROOT_DIR + File.separator + ".temp/");
        if (!dir.exists()) Log.i(TAG, "Is DIR created?  " + dir.mkdirs());
        Log.i(TAG, "Destination path is " + dir);


        return File.createTempFile(part, ext, dir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {


            Log.d(TAG, "RESULT CODE = " + resultCode + " REQUEST CODE " + requestCode);


            if (resultCode == RESULT_OK) {

                Log.d(TAG, " REQUEST CODE IS OK");

                if (requestCode == CAMERA_INTENT) {


                    Log.d(TAG, "CAMERA INTENT");

/*
                    Bundle extras = data.getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");


                    //String farmerImage = "file://" + saveImage(imageBitmap, farmerCode.getText().toString());


                    //IMAGE_URL = farmerImage;

                    //Log.d(TAG, "FARMER IMAGE URL IS " + farmerImage);

                    BASE64_STRING = ImageUtil.bitmapToBase64(imageBitmap);*/

                    /*this.getContentResolver().notifyChange(URI, null);
                    ContentResolver cr = this.getContentResolver();*/
                    Bitmap bitmap = null;
                    try {

                        //bitmap =  android.provider.MediaStore.Images.Media.getBitmap(cr, URI);
                        bitmap = ImageUtil.handleSamplingAndRotationBitmap(Add_EditFarmerDetailsActivity.this, URI);
                        BASE64_STRING = ImageUtil.bitmapToBase64(bitmap);

                        if (circleImageView != null) {

                            circleImageView.setImageBitmap(bitmap);
                            //Picasso.with(this).load(URI).resize(400, 300).into(circleImageView);
                            initials.setVisibility(View.GONE);

                        } else {

                            Log.d(TAG, "Image view is null");

                        }

                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Failed to load", e);
                    }





                }


            } else CustomToast.makeToast(this, "Something went wrong", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            CustomToast.makeToast(this, "Something went wrong", Toast.LENGTH_LONG)
                    .show();
        }

        prefs.edit().putBoolean("shouldSave", true).apply();


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permission, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permission, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // CustomToast.makeToast(this, permission[0], Toast.LENGTH_SHORT).show();

            if (requestCode == PERMISSION_CAMERA) {
                startCameraIntent();
            }

        } else {
            showAlertDialog(true, getResources(R.string.permission_required), getResources(R.string.camera_permission_rationale) , new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    startCameraIntent();
                }
            }, getResources(R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }, getResources(R.string.no), 0);
        }
    }

    protected String getSaltString() {
        String UUID = prefs.getString(Constants.USER_UID, "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
        StringBuilder salt = new StringBuilder();
        Random r = new Random(System.currentTimeMillis());
        while (salt.length() < 8) { // length of the random string.
            int index = (int) (r.nextFloat() * UUID.length());
            salt.append(UUID.charAt(index));
        }

        return salt.toString().toUpperCase();

    }

    private File getOutputMediaFile() {
        File mediaStorageDir = new File(ROOT_DIR);

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }


        return new File(ROOT_DIR,
                "temp" + ".jpg");
    }

    void loadNextForm(){
        if (IS_MONITIRING_MODE)
            FORMS = databaseHelper.getAllMonitoringForms();

        if(SELECTED_FORM_INDEX < FORMS.size()) {
            for (int i = 0; i < FORMS.size(); i++) {
                if (FORMS.get(i).getName().equalsIgnoreCase(formLabel)) {
                    SELECTED_FORM_INDEX += 1;
                    break;
                }
            }


            if (SELECTED_FORM_INDEX == FORMS.size()) {
                SELECTED_FORM_INDEX = 0;

                Intent intent = new Intent(Add_EditFarmerDetailsActivity.this, FarmerDetailsActivity.class);
                intent.putExtra("farmer", new Gson().toJson(farmer));
                intent.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();


            } else {


                JSONObject ALL_FARMER_ANSWERS_JSON;
                try {
                    ALL_FARMER_ANSWERS_JSON = new JSONObject(databaseHelper.getAllAnswersJson(farmer.getId()));
                } catch (JSONException e) {
                    e.printStackTrace();
                    ALL_FARMER_ANSWERS_JSON = new JSONObject();
                }


                if (FORMS.get(SELECTED_FORM_INDEX).getName().equalsIgnoreCase(Constants.FAMILY_MEMBERS)) {
                    // SELECTED_FORM_INDEX += 1;

                    String familyMemnersKey = prefs.getString("no_family_members_id", null);
                    Log.i(TAG, "FAMILY MEMBERS KEY " + prefs.getString("no_family_members_id", "null"));
                    String familyMembers;


                    try {
                        familyMembers = ALL_FARMER_ANSWERS_JSON.getString(familyMemnersKey);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        familyMembers = "1";
                    }


                    Log.i(TAG, "FAMILY MEMBERS KEY " + prefs.getString("no_family_members_id", "null"));

                    if (familyMembers.equalsIgnoreCase("null")) {
                        Log.i(TAG, "FAMILY MEMBERS OF FARMER NULL");

                        //Please fill out Socio-Economic data first

                        showAlertDialog(false, getResources(R.string.fill_data), getResources(R.string.enter_data_rationale) + farmer.getFarmerName() + getResources(R.string.before_proceed_suffux), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();

                            }
                        }, getResources(R.string.ok), null, "", 0);

                    } else {

                        Intent intent = new Intent(this, FamilyMembersActivity.class);
                        intent.putExtra("farmer", new Gson().toJson(farmer));
                        intent.putExtra("familyMembers", familyMembers);
                        startActivity(intent);

                    }


                } else {
                    Form form = FORMS.get(SELECTED_FORM_INDEX);

                    if (IS_MONITIRING_MODE)
                        if (form.getType().equalsIgnoreCase(Constants.DIAGNOSTIC)) {
                            formLabel = form.getName();

                            loadNextForm();

                        } else
                            goToNextForm(form);

                    else
                        goToNextForm(form);


                }

            }
        }
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

        Log.i(TAG, "^^^^^^^^^^    SELECTED INDEX " + SELECTED_FORM_INDEX);

        Log.i(TAG, "^^^^^^^^^^    FORM NAME IS  " + form.getName());

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
