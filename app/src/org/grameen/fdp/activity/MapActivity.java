package org.grameen.fdp.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.maps.android.SphericalUtil;

import org.grameen.fdp.R;
import org.grameen.fdp.adapter.PointsListAdapter;
import org.grameen.fdp.object.RealPlot;
import org.grameen.fdp.utility.CustomToast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by aangjnr on 09/11/2017.
 */
public class MapActivity extends BaseActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    ProgressDialog progressDialog;
    String TAG = getClass().getSimpleName();
    Button addPointButton;
    Button calculateArea;
    int MIN_NO_OF_POINTS = 6;
    List<LatLng> latLngs = new ArrayList<>();
    RealPlot plot;
    RecyclerView recyclerView;
    boolean GpsStatus = false;
    boolean hasCalculated = false;
    PointsListAdapter mAdapter;
    Double AREA_OF_PLOT;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final long UPDATE_INTERVAL = 5000, FASTEST_INTERVAL = 5000; // = 5 seconds
    private GoogleApiClient googleApiClient;
    boolean hasGpsDataBeenSaved = true;
    String action = "";


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setIndeterminate(true);

        recyclerView = findViewById(R.id.recycler_view);

        plot = new Gson().fromJson(getIntent().getStringExtra("plot"), RealPlot.class);
        setToolbar((plot != null) ? plot.getName() +" " + getResources(R.string.title_area_calc) : "Plot GPS Area Calculation");

        if (plot.getGpsPoints() != null && !plot.getGpsPoints().equalsIgnoreCase("")) {
            Log.e(TAG, plot.getGpsPoints());
            try {
                String[] llgs = plot.getGpsPoints().split("_");
                for (String llg : llgs) {
                    String[] values = llg.split(",");
                    latLngs.add(new LatLng(Double.parseDouble(values[0]), Double.parseDouble(values[1])));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (latLngs.size() > 0)
            findViewById(R.id.placeHolder).setVisibility(View.GONE);

        mAdapter = new PointsListAdapter(this, latLngs);
        mAdapter.setHasStableIds(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new PointsListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                mAdapter.removePoint(position);
                if(latLngs.size() > 0) {
                    if(findViewById(R.id.placeHolder).getVisibility() == View.VISIBLE)
                        findViewById(R.id.placeHolder).setVisibility(View.GONE);
                }else{
                    if(findViewById(R.id.placeHolder).getVisibility() == View.GONE)
                        findViewById(R.id.placeHolder).setVisibility(View.VISIBLE);
                }
                hasGpsDataBeenSaved = false;
            }
        });

        calculateArea = findViewById(R.id.calculate);
        calculateArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                action = "calculate area of ";
                if(checkNoGPSPointsAdded())
                    computeAreaInSquareMeters();
            }
        });

        addPointButton = findViewById(R.id.addPoint);
        addPointButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLocationUpdates();
            }
        });

        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action = "save data of ";

                if(checkNoGPSPointsAdded())
                saveGpsPointsData(false);
            }
        });

        onBackClicked();
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            String msg = "Do you want to add this point?" + "\n\n" +
                    "Latitude : " +  location.getLatitude() + "\n" +
                    "Longitude : " + location.getLongitude() + "\n" +
                    "Accuracy : " + location.getAccuracy() + " meters ";

            if (progressDialog.isShowing())
                progressDialog.dismiss();

            showAlertDialog(true, "Location update!", msg, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    LatLng newLL = new LatLng(location.getLatitude(), location.getLongitude());
                    mAdapter.addPoint(newLL);
                    progressDialog.dismiss();
                    hasCalculated = false;

                    if (latLngs.size() > 0) {
                        if (findViewById(R.id.placeHolder).getVisibility() == View.VISIBLE)
                            findViewById(R.id.placeHolder).setVisibility(View.GONE);
                    }
                    hasGpsDataBeenSaved = false;
                }
            }, "ADD POINT", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }, "CANCEL", 0);
            removeLocationListener();
        }
    };

    void computeAreaInSquareMeters() {
        if (progressDialog.isShowing())
            progressDialog.dismiss();
            AREA_OF_PLOT = SphericalUtil.computeArea(latLngs);
            Log.d(TAG, "computeAreaInSquareMeters " + AREA_OF_PLOT);

        Double inHectares = convertToHectares(AREA_OF_PLOT);
        Double inAcres = convertToAcres(AREA_OF_PLOT);
        String message = "Area in Hectares is " + new DecimalFormat("0.00").format(inHectares) +
                "\nArea in Acres is " + new DecimalFormat("0.00").format(inAcres) +
                "\nArea in Square Meters is " + new DecimalFormat("0.00").format(AREA_OF_PLOT);
        showAlertDialog(false, "Area of plot " + plot.getName(), message, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
        }, getResources(R.string.ok), null, "", 0);
            hasCalculated = true;
    }

    boolean checkNoGPSPointsAdded(){
        if(latLngs.size() < MIN_NO_OF_POINTS) {
            CustomToast.makeToast(MapActivity.this, "Please add " + MIN_NO_OF_POINTS + " or more points to " + action  + plot.getName(),
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    double convertToHectares(Double valueInSquareMetres) {
        return valueInSquareMetres / 10000;
    }

    double convertToAcres(Double valueInSquareMetres) {
        return valueInSquareMetres / 4040.856;
    }

    private void getLocationUpdates() {
        progressDialog.show();

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        GpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (GpsStatus)
            startLocationListener();
        else{
            final String action = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
            showAlertDialog(true, "GPS disabled", "Do you want to open GPS settings?", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    startActivity(new Intent(action));
                }
            }, getResources(R.string.yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }, getResources(R.string.no), 0);
        }
    }

    void startLocationListener(){
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            CustomToast.makeToast(this, "You need to enable permissions to display location!", Toast.LENGTH_LONG).show();
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, locationListener);
    }

    void removeLocationListener(){
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, locationListener);
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
            }
            return false;
        }
        return true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();
    }

    void saveGpsPointsData(boolean shouldExit){
        int count = 0;
        StringBuilder builder = new StringBuilder();
        for (LatLng latLng : latLngs) {
            builder.append(latLng.latitude).append(",").append(latLng.longitude);

            if (count != latLngs.size() - 1)
                builder.append("_");
            count += 1;
        }
        plot.setGpsPoints(builder.toString());

        if (databaseHelper.editPlotGPS(plot.getId(), builder.toString())) {
            hasGpsDataBeenSaved = true;
            CustomToast.makeToast(MapActivity.this, getResources(R.string.new_data_updated), Toast.LENGTH_LONG).show();
            if(shouldExit)
                moveToPlotDetailsActivity();
        }
        else
            CustomToast.makeToast(MapActivity.this, getResources(R.string.data_not_saved), Toast.LENGTH_LONG).show();

    }

    void moveToPlotDetailsActivity(){
        Intent intent = new Intent(this, PlotDetailsActivity.class);
        intent.putExtra("plot", new Gson().toJson(plot));
        startActivity(intent);
        supportFinishAfterTransition();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!checkPlayServices()) {
            showAlertDialog(false, "Missing Google Play Services", "You need to install Google Play Services to use the App properly", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.gms")));
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.gms")));
                    }
                }
            }, "INSTALL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            }, "EXIT", 0);
        }
    }

    @Override
    protected void onPause() {
        if(!hasGpsDataBeenSaved)
        saveGpsPointsData(false);
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (googleApiClient != null  &&  googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if(!hasGpsDataBeenSaved)
            showAlertDialog(false, "Save GPS data", "Do you want to save the GPS points?", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if(checkNoGPSPointsAdded())
                        saveGpsPointsData(true);
                    dialogInterface.dismiss();
                }
            }, getResources(R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    moveToPlotDetailsActivity();
                }
            }, "NO", 0);
        else
            moveToPlotDetailsActivity();
    }

}