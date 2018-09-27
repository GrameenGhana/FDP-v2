package org.grameen.fdp.activity;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.evrencoskun.tableview.filter.FilterType;
import com.google.gson.Gson;
import com.wooplr.spotlight.SpotlightConfig;
import com.wooplr.spotlight.utils.SpotlightSequence;

import org.grameen.fdp.R;
import org.grameen.fdp.adapter.CustomSuggestionsAdapter;
import org.grameen.fdp.adapter.FarmerListAdapter;
import org.grameen.fdp.fragment.FarmerListFragment;
import org.grameen.fdp.object.RealFarmer;
import org.grameen.fdp.object.Village;
import org.grameen.fdp.utility.Callbacks;
import org.grameen.fdp.utility.Constants;
import org.grameen.fdp.utility.CustomToast;
import org.grameen.fdp.utility.DatabaseHelper;
import org.grameen.fdp.utility.DateUtil;
import org.grameen.fdp.utility.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import br.com.mauker.materialsearchview.MaterialSearchView;

/**
 * Created by aangjnr on 08/11/2017.
 */


public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener,
        Callbacks.NetworkActivityCompleteListener {

    Switch toggleTranslation;
    ViewPager viewPager;
    String TAG = "MainActivity";
    MaterialSearchView searchBar;
    GridLayoutManager productsGridLayoutManager;
    DatabaseHelper databaseHelper;
    TextView tabOIndicatorText;
    SharedPreferences prefs;
    int dotsCount;
    ImageView[] dots;
    FarmerPagerAdapter viewPagerAdapter;
    List<Village> villages;
    List<String> actualVillageList = new ArrayList<>();


    private DrawerLayout drawer;
    private boolean IS_FAB_VISIBLE = false;
    private LinearLayout pager_indicator;
    ImageView hamburger;
    RelativeLayout searchLayout;

    private List<RealFarmer> TOTAL_FARMER_LIST;



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hamburger = findViewById(R.id.hamburger);
        searchLayout = findViewById(R.id.toolbar);



        prefs = PreferenceManager.getDefaultSharedPreferences(this);


        databaseHelper = DatabaseHelper.getInstance(this);
        pager_indicator = (LinearLayout) findViewById(R.id.viewPagerCountDots);

        toggleTranslation = findViewById(R.id.translation_switch);

        if(prefs.getBoolean("toggleTranslation", false))
            toggleTranslation.setChecked(true);


        Log.d("ACTION TYPE", prefs.getString("flag", ""));
        if (prefs.getString("flag", "").equals(Constants.MONITORING)) {

            //Todo add the rest of the views to hide

            findViewById(R.id.add_farmer).setVisibility(View.GONE);

        }

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        viewPager = (ViewPager) findViewById(R.id.view_pager);




        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);
        searchBar = findViewById(R.id.search_view);


        searchLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBar.openSearch();
            }
        });


        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        // customSuggestionsAdapter = new CustomSuggestionsAdapter(inflater);

        TOTAL_FARMER_LIST = databaseHelper.getAllFarmers();



        findViewById(R.id.add_farmer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (databaseHelper.getAllForms().size() > 0) {

                    startActivity(new Intent(MainActivity.this, Add_EditFarmerDetailsActivity.class));
                } else {

                    if (Utils.checkInternetConnection(MainActivity.this)) {

                        DataDownloadActivity.onNetworkActivityComplete(MainActivity.this);
                        startActivity(new Intent(MainActivity.this, DataDownloadActivity.class));
                    } else {
                        CustomToast.makeToast(MainActivity.this, getResources(R.string.no_internet_connection_available), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        findViewById(R.id.sync).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(MainActivity.this, "Sync", Toast.LENGTH_SHORT).show();

                if (Utils.checkInternetConnection(MainActivity.this)) {
                    DataDownloadActivity.onNetworkActivityComplete(MainActivity.this);
                    SyncDownActivity.onNetworkActivityComplete(MainActivity.this);

                    startActivity(new Intent(MainActivity.this, DataDownloadActivity.class));
                } else {
                    CustomToast.makeToast(MainActivity.this, getResources(R.string.no_internet_connection_available), Toast.LENGTH_LONG).show();

                }
            }
        });





        tabOIndicatorText = (TextView) findViewById(R.id.tab_indicator_text);


        //Todo obtain categories/Groups according to a filter flag and populate the viewpager accordingly

        setUpAdatper();


        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {


                startIntro();

            }
        }, 1000);




        findViewById(R.id.translationLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                drawer.closeDrawers();
                toggleTranslation.setChecked(!toggleTranslation.isChecked());
                prefs.edit().putBoolean("toggleTranslation", toggleTranslation.isChecked()).apply();
                Log.i(TAG, "Translation is " + toggleTranslation.isChecked());


                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {


                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();

                    }
                }, 500);


            }
        });


        searchBar.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                searchBar.addSuggestions(generateSearchFilter(query));

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchBar.addSuggestions(generateSearchFilter(newText));
                return true;
            }
        });

        searchBar.setSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewOpened() {
                findViewById(R.id.card_view).setAlpha(0f);
                // Do something once the view is open.
            }

            @Override
            public void onSearchViewClosed() {

                findViewById(R.id.card_view).animate().alpha(1f).setDuration(1000).start();
            }
        });


        searchBar.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Do something when the suggestion list is clicked.
                String suggestion = searchBar.getSuggestionAtPosition(position);
                //searchBar.setQuery(suggestion, true);

                searchBar.closeSearch();

                Intent intent = new Intent(MainActivity.this, FarmerDetailsActivity.class);
                intent.putExtra("farmer", new Gson().toJson(databaseHelper.getFarmerBasicInfo(suggestion.split(Pattern.quote("|"))[1].trim())));
                startActivity(intent);

            }
        });


        hamburger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.openDrawer(Gravity.START);
            }
        });


        onBackClicked();

    }


    void setUpAdatper() {

        villages = databaseHelper.getAllVillages();
        viewPagerAdapter = new FarmerPagerAdapter(getSupportFragmentManager());

        for (Village village : villages) {
            List<RealFarmer> farmerList = databaseHelper.getAllFarmersBasicInfoAccordingToVillage(village.getName());

            if (farmerList != null && farmerList.size() != 0) {
                viewPagerAdapter.addFrag(FarmerListFragment.newInstance(village.getName()));
                actualVillageList.add(village.getName());
            }
        }

        viewPager.setAdapter(viewPagerAdapter);
        if (viewPagerAdapter.getCount() != 0) {
            findViewById(R.id.placeHolder).setVisibility(View.GONE);

            setUiPageViewController();

        } else {
            findViewById(R.id.placeHolder).setVisibility(View.VISIBLE);
        }

        viewPager.setCurrentItem(0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "ON START!");
    }


    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "ON STOP!");


    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        final int id = item.getItemId();

        drawer.closeDrawer(GravityCompat.START);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

        switch (id) {


            case R.id.sync:

                if (Utils.checkInternetConnection(MainActivity.this)) {

                    DataDownloadActivity.onNetworkActivityComplete(MainActivity.this);
                    startActivity(new Intent(MainActivity.this, DataDownloadActivity.class));

                }

                else {
                    CustomToast.makeToast(MainActivity.this, getResources(R.string.no_internet_connection_available), Toast.LENGTH_LONG).show();
                }


                break;

            case R.id.logout:

                logOut(MainActivity.this);

                break;

            case R.id.sync_farmer:

                SyncUpActivity.onNetworkActivityComplete(MainActivity.this);

                startActivity(new Intent(MainActivity.this, SyncUpActivity.class));

                break;

            case R.id.download_farmer_data:

                SyncDownActivity.onNetworkActivityComplete(MainActivity.this);

                startActivity(new Intent(MainActivity.this, SyncDownActivity.class));

                break;


    /*
            case R.id.homepage:


                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();



                startActivity(new Intent( this, LandingPageActivity.class));


                break;*/


        }

            }
        }, 500);


        return true;
    }



    @Override
    public void onResume() {
        super.onResume();
        Log.i("MAIN ACTIVITY", "ON RESUME!");

        if (prefs.getBoolean("shouldRestartMainActivity", false)) {
            prefs.edit().putBoolean("shouldRestartMainActivity", false).apply();
            Intent intent = new Intent(this, MainActivity.class);
            overridePendingTransition(R.anim.fade_in, 0);
            startActivity(intent);
            finish();

        } else if (prefs.getBoolean("refreshMainActivity", false)) {
            prefs.edit().putBoolean("refreshMainActivity", false).apply();
            setUpAdatper();
        }



    }


    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (searchBar.isOpen()) {
            searchBar.closeSearch();
        } else {
            super.onBackPressed();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        searchBar.clearSuggestions();
    }


//////////////////////////////////////////////////////////////////////////

    private void setUiPageViewController() {


        if (pager_indicator != null)
            pager_indicator.removeAllViews();

        dotsCount = viewPagerAdapter.getCount();
        dots = new ImageView[dotsCount];

        for (int i = 0; i < dotsCount; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.non_selected_item));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            params.setMargins(4, 0, 4, 0);

            pager_indicator.addView(dots[i], params);
        }

        dots[0].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.selected_item));

        tabOIndicatorText.setText(actualVillageList.get(0));

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int position) {

                //Todo set tab text accordingly

                tabOIndicatorText.setText(actualVillageList.get(position));

                for (int i = 0; i < dotsCount; i++) {
                    dots[i].setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.non_selected_item));
                }

                dots[position].setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.selected_item));


            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });


    }

    void startIntro() {


        if (prefs.getBoolean("addFarmerSpotlight", true)) {

            try {
                prefs.edit().putBoolean("addFarmerSpotlight", false).apply();

                final SpotlightConfig config = new SpotlightConfig();
                config.setDismissOnBackpress(true);
                config.setDismissOnTouch(true);
                config.setHeadingTvColor(ContextCompat.getColor(this, R.color.colorAccent));
                config.setHeadingTvSize(26);
                config.setSubHeadingTvSize(16);
                config.setSubHeadingTvColor(ContextCompat.getColor(this, R.color.black_50));
                config.setFadingTextDuration(400);
                config.setIntroAnimationDuration(200);
                config.setMaskColor(ContextCompat.getColor(this, R.color.white_50));
                config.setLineAndArcColor(ContextCompat.getColor(this, R.color.colorAccent));
                config.setFadingTextDuration(250);
                config.setLineStroke(2);


                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        SpotlightSequence.getInstance(MainActivity.this, config)

                                .addSpotlight(findViewById(R.id.add_farmer), "Add farmer", "Click here to add a new farmer", "addFarmer")

                                .startSequence();
                    }
                }, 400);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }


    @Override
    public void taskComplete(int response) {

        try {
            DataDownloadActivity.removeOnNetworkActivityComplete();
            SyncUpActivity.removeOnNetworkActivityComplete();
            SyncDownActivity.removeOnNetworkActivityComplete();

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        setUpAdatper();

    }



    public class FarmerPagerAdapter extends FragmentPagerAdapter {

        List<Fragment> tags = new ArrayList<>();

        public FarmerPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return tags.size();
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {

            return tags.get(position);

        }


        public void addFrag(Fragment fragment) {
            tags.add(fragment);
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return tags.get(position).getArguments().getString("filterTag");
        }

    }


    ArrayList<String> generateSearchFilter(String query) {

        ArrayList<String> filteredList = new ArrayList<>();

        if (!TextUtils.isEmpty(query)) {
            //searchBar.clearHistory();


            for (RealFarmer f : TOTAL_FARMER_LIST) {
                if (f.getFarmerName().toLowerCase().contains(query.toLowerCase())) {

                    filteredList.add(f.getFarmerName() + " | " + f.getId());

                }
            }
        }

        return filteredList;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MaterialSearchView.REQUEST_VOICE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && matches.size() > 0) {
                String searchWrd = matches.get(0);
                if (!TextUtils.isEmpty(searchWrd)) {

                    searchBar.setQuery(searchWrd, false);
                    searchBar.addSuggestions(generateSearchFilter(searchWrd));


                }
            }

            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }



}
