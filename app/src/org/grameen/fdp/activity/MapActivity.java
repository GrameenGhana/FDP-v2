package org.grameen.fdp.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.gson.Gson;
import com.google.maps.android.SphericalUtil;

import org.grameen.fdp.R;
import org.grameen.fdp.adapter.PointsListAdapter;
import org.grameen.fdp.object.Plot;
import org.grameen.fdp.object.RealPlot;
import org.grameen.fdp.utility.Constants;
import org.grameen.fdp.utility.CustomToast;
import org.grameen.fdp.utility.PermissionsUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.codetail.animation.ViewAnimationUtils;

/**
 * Created by aangjnr on 09/11/2017.
 */

public class MapActivity extends BaseActivity {
    public static Float DEFAULT_ZOOM = 17.0f;
    ProgressDialog progressDialog;
    String TAG = getClass().getSimpleName();
    Button addPoint;
    Button calculateArea;
    List<LatLng> latLngs = new ArrayList<>();
    RealPlot plot;
    RecyclerView recyclerView;
    boolean GpsStatus = false;
    LocationManager locationManager;
    boolean hasCalculated = false;
    PointsListAdapter mAdapter;
    Double AREA_OF_PLOT;
    android.location.LocationListener locationListener;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setIndeterminate(true);

        recyclerView = findViewById(R.id.recycler_view);

        plot = new Gson().fromJson(getIntent().getStringExtra("plot"), RealPlot.class);

        Toolbar toolbar;
        if (plot != null) {
            toolbar = setToolbar(plot.getName() +" " + getResources(R.string.title_area_calc));
        } else {
            toolbar = setToolbar("Plot GPS Area Calculation");
        }

        Log.i(TAG, "PLOT POINTS " + plot.getGpsPoints());

        if (plot.getGpsPoints() != null && !plot.getGpsPoints().equalsIgnoreCase("")) {
            try {
                String llgs[] = plot.getGpsPoints().split("_");
                for (String llg : llgs) {
                    String values[] = llg.split(",");
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
                //latLngs.remove(position);

                if(latLngs.size() > 2)
                    calculateArea.setEnabled(true);
                    else
                        calculateArea.setEnabled(false);

                if(latLngs.size() > 0) {
                    if(findViewById(R.id.placeHolder).getVisibility() == View.VISIBLE)
                        findViewById(R.id.placeHolder).setVisibility(View.GONE);

                }else{
                    if(findViewById(R.id.placeHolder).getVisibility() == View.GONE)
                        findViewById(R.id.placeHolder).setVisibility(View.VISIBLE);
                }
            }
        });


        calculateArea = (Button) findViewById(R.id.calculate);
        calculateArea.setEnabled(false);

        calculateArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (latLngs != null && latLngs.size() > 2) {
                if (!hasCalculated) {
                    computeAreaInSquareMeters();
                }else{
                    if (progressDialog.isShowing())
                        progressDialog.dismiss();

                    Double inHectares = convertToHectres(AREA_OF_PLOT);
                    Double inAcres = convertToAcres(AREA_OF_PLOT);

                    String message = "Area in Hectares is " + new DecimalFormat("0.00").format(inHectares) +
                            "\nArea in Acres is " + new DecimalFormat("0.00").format(inAcres) +
                            "\nArea in Square Meters is " + new DecimalFormat("0.00").format(AREA_OF_PLOT);

                    showAlertDialog(false, "Area of plot " + plot.getName(), message, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                    }, getResources(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            StringBuilder builder = new StringBuilder();
                            //Todo save latLngs
                            for (LatLng latLng : latLngs) {

                                if (!Objects.equals(latLng, latLngs.get(latLngs.size() - 1)))
                                    builder.append(latLng.latitude).append(",").append(latLng.longitude).append("_");
                                else
                                    builder.append(latLng.latitude).append(",").append(latLng.longitude);
                            }

                            Log.i(TAG, "STRING ARRAY OF LatLngs = " + builder);
                            dialog.dismiss();

                            if (databaseHelper.editPlotGPS(plot.getId(), builder.toString())) {
                                CustomToast.makeToast(MapActivity.this, getResources(R.string.new_data_updated), Toast.LENGTH_LONG).show();
                            } else {
                                CustomToast.makeToast(MapActivity.this, getResources(R.string.data_not_saved), Toast.LENGTH_LONG).show();
                            }
                        }
                    }, getResources(R.string.save), 0);
                        hasCalculated = true;
                }
                } else
                    CustomToast.makeToast(MapActivity.this, "Please add 3 or more points to calculate the area of " + plot.getName(), Toast.LENGTH_LONG).show();
            }
        });

        addPoint = (Button) findViewById(R.id.addPoint);
        addPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                progressDialog.show();
                addPoint.setEnabled(false);
                getCurrentLocation();
            }
        });
        onBackClicked();

        locationListener = new android.location.LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {
                Log.i(TAG, "^^^^^^^^^^ LOCATION CHANGED ^^^^^^^^^^^^");


                String msg = "Updated Location " + "\n" +
                        "Latitude : " + Double.toString(location.getLatitude()) + "\n" +
                        "Longitude : " + Double.toString(location.getLongitude()) + "\n" +
                        "Accuracy : " + location.getAccuracy() + " meters ";
                //"Altitude : "+mCurrentLocation.getAltitude()+" high ";
                //Toast.makeText(CustomerMapActivity.this, msg, Toast.LENGTH_LONG).show();

                if (progressDialog.isShowing())
                    progressDialog.dismiss();

                showAlertDialog(true, "Are you sure?", msg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        LatLng newLL = new LatLng(location.getLatitude(), location.getLongitude());
                        mAdapter.addPoint(newLL);
                        addPoint.setEnabled(true);
                        progressDialog.dismiss();
                        hasCalculated = false;

                        if (latLngs.size() > 0) {
                            if (findViewById(R.id.placeHolder).getVisibility() == View.VISIBLE)
                                findViewById(R.id.placeHolder).setVisibility(View.GONE);
                        }
                        if (latLngs.size() > 2) {
                            calculateArea.setEnabled(true);

                        } else {
                            calculateArea.setEnabled(false);
                        }
                    }
                }, "YES, CONTINUE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }, "NO, CANCEL", 0);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
                Log.i(TAG, "^^^^^^^^^^ PROVIDER ENABLED ^^^^^^^^^^^^");
            }

            @Override
            public void onProviderDisabled(String s) {
                Log.i(TAG, "^^^^^^^^^^ PROVIDER DISABLED ^^^^^^^^^^^^");
            }
        };
    }

    void computeAreaInSquareMeters() {
        if (progressDialog.isShowing())
            progressDialog.dismiss();

            AREA_OF_PLOT = SphericalUtil.computeArea(latLngs);
            Log.d(TAG, "computeAreaInSquareMeters " + AREA_OF_PLOT);

        Double inHectares = convertToHectres(AREA_OF_PLOT);
        Double inAcres = convertToAcres(AREA_OF_PLOT);


        String message = "Area in Hectares is " + new DecimalFormat("0.00").format(inHectares) +
                "\nArea in Acres is " + new DecimalFormat("0.00").format(inAcres) +
                "\nArea in Square Meters is " + new DecimalFormat("0.00").format(AREA_OF_PLOT);


        showAlertDialog(false, "Area of plot " + plot.getName(), message, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    dialogInterface.dismiss();


                }
        }, getResources(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                StringBuilder builder = new StringBuilder();

                //Todo save latLngs
                for (LatLng latLng : latLngs) {

                    if (!Objects.equals(latLng, latLngs.get(latLngs.size() - 1)))
                        builder.append(latLng.latitude).append(",").append(latLng.longitude).append("_");
                    else
                        builder.append(latLng.latitude).append(",").append(latLng.longitude);

                }


                Log.i(TAG, "STRING ARRAY OF LatLngs = " + builder);

                dialog.dismiss();

                if (databaseHelper.editPlotGPS(plot.getId(), builder.toString())) {

                    CustomToast.makeToast(MapActivity.this, getResources(R.string.new_data_updated), Toast.LENGTH_LONG).show();

                } else {

                    CustomToast.makeToast(MapActivity.this, getResources(R.string.data_not_saved), Toast.LENGTH_LONG).show();

                }


            }
        }, getResources(R.string.save), 0);


            hasCalculated = true;


    }

    double convertToHectres(Double valueInSquareMetres) {
        return valueInSquareMetres / 10000;
    }

    double convertToAcres(Double valueInSquareMetres) {
        return valueInSquareMetres / 4040.856;
    }

    private void getCurrentLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        GpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (GpsStatus) {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setPowerRequirement(Criteria.POWER_HIGH);
            criteria.setAltitudeRequired(true);
            criteria.setBearingRequired(false);
            criteria.setSpeedRequired(false);
            criteria.setCostAllowed(true);
            criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
            criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);

            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            // This is the Best And IMPORTANT part
            final Looper looper =  Looper.getMainLooper();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                if (locationManager != null) {
                    locationManager.requestSingleUpdate(criteria, locationListener, looper);
                }
            }
        } else {
            CustomToast.makeToast(this, "Please Enable GPS First", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    protected void onStop() {

        StringBuilder builder = new StringBuilder();

        //Todo save latLngs
        for (LatLng latLng : latLngs) {

            if (!Objects.equals(latLng, latLngs.get(latLngs.size() - 1)))
                builder.append(latLng.latitude).append(",").append(latLng.longitude).append("_");
            else
                builder.append(latLng.latitude).append(",").append(latLng.longitude);

        }

        if (databaseHelper.editPlotGPS(plot.getId(), builder.toString())) {

            CustomToast.makeToast(MapActivity.this, getResources(R.string.new_data_updated), Toast.LENGTH_LONG).show();

        } else {

            CustomToast.makeToast(MapActivity.this, getResources(R.string.data_not_saved), Toast.LENGTH_LONG).show();

        }

        plot.setGpsPoints(builder.toString());

        super.onStop();
    }


    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, PlotDetailsActivity.class);
        intent.putExtra("plot", new Gson().toJson(plot));
        startActivity(intent);
        supportFinishAfterTransition();
    }
}
