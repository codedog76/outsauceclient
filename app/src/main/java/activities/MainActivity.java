package activities;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.outsauce.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Collections;

import de.hdodenhof.circleimageview.CircleImageView;
import fragments.FavouritesFragment;
import fragments.JobInboxFragment;
import fragments.JobOutboxFragment;
import fragments.NavigationDrawerFragment;
import fragments.SearchFragment;
import models.Job;
import models.User;
import other.ObscuredSharedPreferences;
import other.VolleySingleton;
import service.OutSauceService;

public class MainActivity extends AppCompatActivity implements FavouritesFragment.refreshInterface, SearchFragment.refreshInterface,
        JobInboxFragment.refreshInterface, JobOutboxFragment.refreshInterface, NavigationDrawerFragment.drawerListener {
    //current logged in user variables
    private boolean user_logged_in;
    private String user_email_address, user_first_name, user_last_name, user_status, profile_picture;
    //other activity variables
    private boolean mIsInForegroundMode;
    private boolean mBounded;
    private boolean firstLaunch = true;
    private OutSauceService mServer;
    private ProgressDialog progressDialog;
    private SharedPreferences prefs;
    private RequestQueue requestQueue;
    private static final String USER_STATUS_URL_STRING = "http://opensouce.csdev.nmmu.ac.za/api/users/userstatus"; //status change json url
    //navigation fragment global variables
    private NavigationDrawerFragment drawerFragment;
    //views
    private Button signinButton;
    private Toolbar toolbar;
    private TextView navSearchTextView, navFavouritesTextView, navJobInboxTextView, navJobOutboxTextView, navLogOutTextView, fullnameText, fullnameEmailText, availableTextView,
            unavailableTextView, busyTextView, statusDropTextView;
    private ImageView navSearchImageView, navFavouritesImageView, navJobInboxImageView, navJobOutboxImageView, dropImageView, statusImageView;
    private CircleImageView profile_image;
    private LinearLayout signinLayout, signoutLayout, searchLayout, navigationalLayout, favouritesLayout, jobinboxLayout, joboutboxlayout, navListLayout, accountLayout, searchLayoutBack,
            favouritesLayoutBack, jobinboxLayoutBack, joboutboxLayoutBack, topNavBarLayout, statusDropLayout, statusListLayout;
    private RelativeLayout mainActivityLayout;
    private SearchView sv;
    //search fragment global variables
    private String query;
    private Boolean searchFragmentSelected = false;
    private static final String SEARCH_FRAGMENT_URL_STRING = "http://opensouce.csdev.nmmu.ac.za/api/users/search"; //search all json url
    private SearchFragment searchFragment;
    private ArrayList<User> searchFragmentList; //local storage for search list
    //favourites fragment global variables
    private String FAVOURITES_FILENAME = "_FAVOURITES_CACHE.srl";
    private Boolean favouritesFragmentSelected = false;
    private static final String FAVOURITES_FRAGMENT_URL_STRING = "http://opensouce.csdev.nmmu.ac.za/api/users/favourites"; //favourites json url
    private FavouritesFragment favouritesFragment;
    private ArrayList<User> favouritesFragmentList; //local storage for favourites list
    private FavouritesFragmentListener favouritesFragmentListener;
    //job inbox fragment global variables
    private Boolean jobInboxFragmentSelected = false;
    private JobInboxFragment jobInboxFragment;
    private ArrayList<Job> jobInboxFragmentList; //local storage for job inbox list
    private JobInboxFragmentListener jobInboxFragmentListener;
    //job outbox fragment global variables
    private Boolean jobOutboxFragmentSelected = false;
    private JobOutboxFragment jobOutboxFragment;
    private ArrayList<Job> jobOutboxFragmentList; //local storage for job outbox list
    private JobOutboxFragmentListener jobOutboxFragmentListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        assignViews();
        assignFonts();
        assignActionBar();
        assignProgressDialog();
        getUserDataFromPrefs();
        assignNavigationDrawer();
        requestQueue = VolleySingleton.getInstance(this.getApplicationContext()).getRequestQueue();
        //create new favourites list cache //replace with local sql database
        searchFragmentList = new ArrayList<>();
        favouritesFragmentList = new ArrayList<>();
        jobInboxFragmentList = new ArrayList<>();
        jobOutboxFragmentList = new ArrayList<>();
        assignListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startService(new Intent(getBaseContext(), OutSauceService.class));
        Intent mIntent = new Intent(this, OutSauceService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver((addAllInboxReceiver),
                new IntentFilter("addAllInboxReceiver")
        );
        LocalBroadcastManager.getInstance(this).registerReceiver((addAllOutboxReceiver),
                new IntentFilter("addAllOutboxReceiver")
        );
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(addAllInboxReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(addAllOutboxReceiver);
        if (mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
        progressDialog.dismiss();
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsInForegroundMode = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsInForegroundMode = true;
    }

    private void assignViews() {
        fullnameText = (TextView) findViewById(R.id.fullnameText);
        fullnameEmailText = (TextView) findViewById(R.id.fullnameEmailText);
        navSearchTextView = (TextView) findViewById(R.id.navSearchTextView);
        navFavouritesTextView = (TextView) findViewById(R.id.navFavouritesTextView);
        navJobInboxTextView = (TextView) findViewById(R.id.navJobInboxTextView);
        navJobOutboxTextView = (TextView) findViewById(R.id.navJobOutboxTextView);
        navLogOutTextView = (TextView) findViewById(R.id.navLogOutTextView);
        availableTextView = (TextView) findViewById(R.id.availableTextView);
        busyTextView = (TextView) findViewById(R.id.busyTextView);
        unavailableTextView = (TextView) findViewById(R.id.unavailableTextView);
        statusDropTextView = (TextView) findViewById(R.id.statusDropTextView);
        profile_image = (CircleImageView) findViewById(R.id.profile_image);
        navSearchImageView = (ImageView) findViewById(R.id.navSearchImageView);
        navFavouritesImageView = (ImageView) findViewById(R.id.navFavouritesImageView);
        navJobInboxImageView = (ImageView) findViewById(R.id.navJobInboxImageView);
        navJobOutboxImageView = (ImageView) findViewById(R.id.navJobOutboxImageView);
        dropImageView = (ImageView) findViewById(R.id.dropImageView);
        statusImageView = (ImageView) findViewById(R.id.statusImageView);
        signinButton = (Button) findViewById(R.id.signinButton);
        mainActivityLayout = (RelativeLayout) findViewById(R.id.mainActivityLayout);
        searchLayout = (LinearLayout) findViewById(R.id.searchLayout);
        favouritesLayout = (LinearLayout) findViewById(R.id.favouriteLayout);
        jobinboxLayout = (LinearLayout) findViewById(R.id.jobinboxLayout);
        joboutboxlayout = (LinearLayout) findViewById(R.id.joboutboxLayout);
        signoutLayout = (LinearLayout) findViewById(R.id.signoutLayout);
        signinLayout = (LinearLayout) findViewById(R.id.signinLayout);
        navigationalLayout = (LinearLayout) findViewById(R.id.navigationalLayout);
        navListLayout = (LinearLayout) findViewById(R.id.navListLayout);
        accountLayout = (LinearLayout) findViewById(R.id.availableLayout);
        searchLayoutBack = (LinearLayout) findViewById(R.id.searchLayoutBack);
        favouritesLayoutBack = (LinearLayout) findViewById(R.id.favouriteLayoutBack);
        jobinboxLayoutBack = (LinearLayout) findViewById(R.id.jobinboxLayoutBack);
        joboutboxLayoutBack = (LinearLayout) findViewById(R.id.joboutboxLayoutBack);
        topNavBarLayout = (LinearLayout) findViewById(R.id.topNavBarLayout);
        statusDropLayout = (LinearLayout) findViewById(R.id.statusDropLayout);
        statusListLayout = (LinearLayout) findViewById(R.id.statusListLayout);
        toolbar = (Toolbar) findViewById(R.id.app_actionbar);
    }

    private void assignFonts() {
        Typeface Roboto_Medium = Typeface.createFromAsset(getAssets(), "Roboto-Medium.ttf");
        Typeface Roboto_Regular = Typeface.createFromAsset(getAssets(), "Roboto-Regular.ttf");
        fullnameText.setTypeface(Roboto_Medium);
        fullnameEmailText.setTypeface(Roboto_Regular);
        navSearchTextView.setTypeface(Roboto_Regular);
        navFavouritesTextView.setTypeface(Roboto_Regular);
        navJobInboxTextView.setTypeface(Roboto_Regular);
        navJobOutboxTextView.setTypeface(Roboto_Regular);
        navLogOutTextView.setTypeface(Roboto_Regular);
        availableTextView.setTypeface(Roboto_Regular);
        unavailableTextView.setTypeface(Roboto_Regular);
        signinButton.setTypeface(Roboto_Medium);
    }

    private void assignActionBar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void assignProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading..");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
    }

    private void getUserDataFromPrefs() {
        prefs = new ObscuredSharedPreferences(this, this.getSharedPreferences("SECURE", Context.MODE_PRIVATE));
        user_logged_in = prefs.getBoolean("user_logged_in", false);
        if (user_logged_in) {
            user_email_address = prefs.getString("email_address", null);
            user_first_name = prefs.getString("first_name", null);
            user_last_name = prefs.getString("last_name", null);
            user_status = prefs.getString("user_status", null);
            profile_picture = prefs.getString("profile_picture", null);
        }
    }

    private void assignNavigationDrawer() {
        drawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp((DrawerLayout) findViewById(R.id.drawer_layout), toolbar);
        drawerFragment.setListener(this);
        if (user_logged_in) {
            navigationalLayout.setVisibility(View.VISIBLE);
            signinLayout.setVisibility(View.GONE);
            fullnameText.setText(user_first_name + " " + user_last_name);
            fullnameEmailText.setText(user_email_address);
            if (profile_picture != null && !profile_picture.equals("")) {
                byte[] decodedString = Base64.decode(profile_picture, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                profile_image.setImageBitmap(decodedByte);
            }
            if (user_status != null) {
                if (user_status.equals("Available")) {
                    profile_image.setBorderColor(this.getResources().getColor(R.color.green));
                    busyTextView.setTextColor(this.getResources().getColor(R.color.black));
                    unavailableTextView.setTextColor(this.getResources().getColor(R.color.black));
                    availableTextView.setTextColor(this.getResources().getColor(R.color.green));

                }
                if (user_status.equals("Busy")) {
                    profile_image.setBorderColor(this.getResources().getColor(R.color.orange));
                    busyTextView.setTextColor(this.getResources().getColor(R.color.orange));
                    unavailableTextView.setTextColor(this.getResources().getColor(R.color.black));
                    availableTextView.setTextColor(this.getResources().getColor(R.color.black));
                }
                if (user_status.equals("Unavailable")) {
                    profile_image.setBorderColor(this.getResources().getColor(R.color.errorRed));
                    busyTextView.setTextColor(this.getResources().getColor(R.color.black));
                    unavailableTextView.setTextColor(this.getResources().getColor(R.color.errorRed));
                    availableTextView.setTextColor(this.getResources().getColor(R.color.black));
                }
            }
        } else {
            navigationalLayout.setVisibility(View.GONE);
            signinLayout.setVisibility(View.VISIBLE);
        }
    }

    private void getBundle() {
        if (searchFragmentSelected) {
            Log.e("SearchFragment", "selected");
            return;
        }
        Bundle b = getIntent().getExtras();
        if (b != null) {
            String fragmentToLoad = b.getString("fragment", null);
            int position = b.getInt("position", -1);
            Boolean refresh = b.getBoolean("refresh", false);
            if (fragmentToLoad != null) {
                if (fragmentToLoad.equals("search")) {
                    loadSearchFragment(refresh, null);
                }
                if (fragmentToLoad.equals("favourites")) {
                    loadFavouritesFragment(refresh);
                    if (position != -1) {
                        getJobInboxFragmentListener().setPosition(position);
                    }
                }
                if (fragmentToLoad.equals("inbox")) {
                    loadJobInboxFragment(refresh);
                    if (position != -1) {
                        getJobInboxFragmentListener().setPosition(position);
                    }
                }
                if (fragmentToLoad.equals("outbox")) {
                    loadJobOutBoxFragment(refresh);
                    if (position != -1) {
                        getJobOutboxFragmentListener().setPosition(position);
                    }
                }
            }
        } else {
            if (firstLaunch) {
                loadSearchFragment(false, null);
                firstLaunch = false;
            }
        }
    }

    private BroadcastReceiver addAllInboxReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<Job> jobs = (ArrayList<Job>) intent.getSerializableExtra("jobInboxList");
            if (jobInboxFragment != null && mIsInForegroundMode && jobInboxFragmentSelected) {
                String tag = jobInboxFragment.getClass().toString();
                jobInboxFragment = (JobInboxFragment) getSupportFragmentManager().findFragmentByTag(tag);
                Collections.reverse(jobs);
                jobInboxFragment.refreshList(jobs);
            }
        }
    };

    private BroadcastReceiver addAllOutboxReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<Job> jobs = (ArrayList<Job>) intent.getSerializableExtra("jobOutboxList");
            if (jobOutboxFragment != null && mIsInForegroundMode && jobOutboxFragmentSelected) {
                String tag = jobOutboxFragment.getClass().toString();
                jobOutboxFragment = (JobOutboxFragment) getSupportFragmentManager().findFragmentByTag(tag);
                Collections.reverse(jobs);
                jobOutboxFragment.refreshList(jobs);
            }
        }
    };

    private void assignListeners() { //initialize navigation drawer listeners
        signinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            }
        });
        signoutLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDialog();
            }
        });
        searchLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerFragment.close();
                loadSearchFragment(false, null);
            }
        });
        favouritesLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerFragment.close();
                loadFavouritesFragment(false);
            }
        });
        jobinboxLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerFragment.close();
                loadJobInboxFragment(false);
            }
        });
        joboutboxlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerFragment.close();
                loadJobOutBoxFragment(false);
            }
        });
        topNavBarLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (navListLayout.getVisibility() == View.VISIBLE) {
                    navListLayout.setVisibility(View.GONE);
                    accountLayout.setVisibility(View.VISIBLE);
                    dropImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_arrow_drop_up_white_48dp));
                } else {
                    accountLayout.setVisibility(View.GONE);
                    navListLayout.setVisibility(View.VISIBLE);
                    dropImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_arrow_drop_down_white_48dp));
                }
            }
        });
        availableTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                statusChangeJSONRequest("Available");
            }
        });
        busyTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                statusChangeJSONRequest("Busy");
            }
        });
        unavailableTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                statusChangeJSONRequest("Unavailable");
            }
        });
        statusDropLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (statusListLayout.getVisibility() == View.GONE) {
                    statusDropTextView.setTextColor(getResources().getColor(R.color.dark_primary_color));
                    statusListLayout.setVisibility(View.VISIBLE);
                    statusImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_arrow_drop_up_black_48dp));
                } else {
                    statusDropTextView.setTextColor(getResources().getColor(R.color.primary_text));
                    statusListLayout.setVisibility(View.GONE);
                    statusImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_arrow_drop_down_black_48dp));
                }
            }
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)

                .setMessage("Are you sure you want to log out?")
                        //.setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("No", null).show();
    }

    private void loadSearchFragment(Boolean loadData, String query) {
        if (searchFragment == null)
            searchFragment = new SearchFragment();
        replaceFragment(searchFragment);
        if (loadData && query != null) {
            searchFragmentList = new ArrayList<>();
            searchFragmentJSONRequest(query);
        }
        setSelectedFragment(searchLayout);
    }

    private void loadFavouritesFragment(Boolean loadData) {
        if (favouritesFragment == null)
            favouritesFragment = new FavouritesFragment();
        replaceFragment(favouritesFragment);
        if (loadData) {
            favouritesFragmentList.clear();
            favouritesFragmentJSONRequest(true);
        } else {
            File file = getFileStreamPath(user_email_address + FAVOURITES_FILENAME);
            if (file.exists()) {
                favouritesFragmentList = readFavouritesFromCache();
                String tag = favouritesFragment.getClass().toString();
                favouritesFragment = (FavouritesFragment) getSupportFragmentManager().findFragmentByTag(tag);
                favouritesFragment.refreshList(favouritesFragmentList);
            } else {
                favouritesFragmentList.clear();
                favouritesFragmentJSONRequest(true);
            }
        }
        setSelectedFragment(favouritesLayout);
    } //done

    private void loadJobInboxFragment(Boolean loadData) {
        if (jobInboxFragment == null)
            jobInboxFragment = new JobInboxFragment();
        replaceFragment(jobInboxFragment);
        if (loadData) {
            Log.e("JobInbox", "loadData");
            mServer.readFromDatabaseJobInbox();
        } else {
            Log.e("JobInbox", "loadDisk");
            mServer.readFromDiskJobInbox();
        }
        setSelectedFragment(jobinboxLayout);
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (searchFragmentSelected) {
            Log.e("SearchFragment", "selected");
            return;
        }
        setIntent(intent);
        Bundle b = getIntent().getExtras();
        if (b != null) {
            String fragmentToLoad = b.getString("fragment", null);
            int position = b.getInt("position", -1);
            Boolean refresh = b.getBoolean("refresh", false);
            if (fragmentToLoad != null) {
                if (fragmentToLoad.equals("search")) {
                    loadSearchFragment(refresh, null);
                }
                if (fragmentToLoad.equals("favourites")) {
                    loadFavouritesFragment(refresh);
                    if (position != -1) {
                        getJobInboxFragmentListener().setPosition(position);
                    }
                }
                if (fragmentToLoad.equals("inbox")) {
                    loadJobInboxFragment(refresh);
                    if (position != -1) {
                        getJobInboxFragmentListener().setPosition(position);
                    }
                }
                if (fragmentToLoad.equals("outbox")) {
                    loadJobOutBoxFragment(refresh);
                    if (position != -1) {
                        getJobOutboxFragmentListener().setPosition(position);
                    }
                }
            }
        } else {
            if (firstLaunch) {
                loadSearchFragment(false, null);
                firstLaunch = false;
            }
        }
    }

    private void loadJobOutBoxFragment(Boolean loadData) {
        if (jobOutboxFragment == null)
            jobOutboxFragment = new JobOutboxFragment();
        replaceFragment(jobOutboxFragment);
        if (loadData) {
            Log.e("JobOutBox", "loadData");
            mServer.readFromDatabaseJobOutbox();
        } else {
            Log.e("JobOutBox", "loadDisk");
            mServer.readFromDiskJobOutbox();
        }
        setSelectedFragment(joboutboxlayout);
    }

    private void statusChangeJSONRequest(String status) {
        JSONObject jsonObject = statusChangeCreateJSONObject(status);
        progressDialog.show();
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, USER_STATUS_URL_STRING, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (validJSONObject("value1", response)) {
                            statusChangeParseJSONRequest(response);
                        } else {
                            progressDialog.hide();
                            Toast.makeText(MainActivity.this, "Unable to change your status", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError ex) {
                        loadSearchFragment(false, null);
                        progressDialog.hide();
                        handleVolleyError(ex);
                    }
                }
        );
        int socketTimeout = 1000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        objectRequest.setRetryPolicy(policy);
        requestQueue.add(objectRequest);
    } //done

    private JSONObject statusChangeCreateJSONObject(String status) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("value1", status);
            String email_address = prefs.getString("email_address", null);
            if (email_address != null)
                jsonObject.put("value2", email_address);
        } catch (JSONException e) {
            if (e.getMessage() != null) {
                progressDialog.hide();
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("JSONException", e.getMessage());
            }
        }
        return jsonObject;
    } //done

    private void statusChangeParseJSONRequest(JSONObject response) {
        try {
            String temp = response.getString("value1");
            if (!temp.equals("failed")) {
                if (temp.equals("Available")) {
                    progressDialog.hide();
                    availableTextView.setTextColor(getResources().getColor(R.color.green));
                    profile_image.setBorderColor(this.getResources().getColor(R.color.green));
                    busyTextView.setTextColor(getResources().getColor(R.color.black));
                    unavailableTextView.setTextColor(getResources().getColor(R.color.black));
                }
                if (temp.equals("Busy")) {
                    progressDialog.hide();
                    busyTextView.setTextColor(getResources().getColor(R.color.orange));
                    profile_image.setBorderColor(this.getResources().getColor(R.color.orange));
                    availableTextView.setTextColor(getResources().getColor(R.color.black));
                    unavailableTextView.setTextColor(getResources().getColor(R.color.black));
                }
                if (temp.equals("Unavailable")) {
                    progressDialog.hide();
                    unavailableTextView.setTextColor(this.getResources().getColor(R.color.errorRed));
                    profile_image.setBorderColor(this.getResources().getColor(R.color.errorRed));
                    busyTextView.setTextColor(getResources().getColor(R.color.black));
                    availableTextView.setTextColor(this.getResources().getColor(R.color.black));
                }
            } else {
                progressDialog.hide();
                Toast.makeText(MainActivity.this, "Unable to change your status", Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            progressDialog.hide();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("JSONException", e.getMessage());
        }
    } //done

    private void searchFragmentJSONRequest(String query) {
        this.query = query;
        JSONObject jsonObject = searchFragmentCreateJSONObject();
        progressDialog.show();
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, SEARCH_FRAGMENT_URL_STRING, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (validJSONObject("Users", response)) {
                            searchFragmentParseJSONRequest(response);
                        } else {
                            String tag = searchFragment.getClass().toString();
                            clearQuery();
                            searchFragmentList = new ArrayList<>();
                            searchFragment = (SearchFragment) getSupportFragmentManager().findFragmentByTag(tag);
                            searchFragment.clearList();
                            progressDialog.hide();
                            Snackbar.make(mainActivityLayout, "Nothing was found", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError ex) {
                        loadSearchFragment(false, null);
                        progressDialog.hide();
                        handleVolleyError(ex);
                    }
                }
        );
        int socketTimeout = 10000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        objectRequest.setRetryPolicy(policy);
        requestQueue.add(objectRequest);
    } //done

    private JSONObject searchFragmentCreateJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("value1", query);
            String email_address = prefs.getString("email_address", null);
            if (email_address != null)
                jsonObject.put("value2", email_address);
            else
                jsonObject.put("value2", "");
        } catch (JSONException e) {
            if (e.getMessage() != null) {
                progressDialog.hide();
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("JSONException", e.getMessage());
            }
        }
        return jsonObject;
    } //done

    private void searchFragmentParseJSONRequest(JSONObject response) {
        try {
            JSONArray jsonArray = response.getJSONArray("Users");
            for (int x = 0; x < jsonArray.length(); x++) {
                JSONObject tempObject = jsonArray.getJSONObject(x);
                String email_address = tempObject.getString("email_address");
                String first_name = tempObject.getString("first_name");
                String last_name = tempObject.getString("last_name");
                String profile_picture = tempObject.getString("profile_picture");
                String user_status = tempObject.getString("user_status");
                JSONArray skillsArray = tempObject.getJSONArray("skills");
                ArrayList<String> skills = new ArrayList<>();
                for (int a = 0; a < skillsArray.length(); a++) {
                    JSONObject tempObject2 = skillsArray.getJSONObject(a);
                    String tempSkill = tempObject2.getString("skill");
                    if (tempSkill.toLowerCase().equals(query.toLowerCase()))
                        skills.add(0, tempSkill);
                    else
                        skills.add(tempSkill);
                }
                String favourite_count = tempObject.getString("favourite_count");
                searchFragmentList.add(new User(email_address, first_name, last_name, profile_picture, skills, favourite_count, user_status));
            }
            if (searchFragmentList.size() != 0) {
                String tag = searchFragment.getClass().toString();
                searchFragment = (SearchFragment) getSupportFragmentManager().findFragmentByTag(tag);
                searchFragment.refreshList(searchFragmentList, query);
            }
            progressDialog.hide();
        } catch (JSONException e) {
            progressDialog.hide();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("JSONException", e.getMessage());
        }
    } //done

    private void favouritesFragmentJSONRequest(final Boolean write) {
        JSONObject jsonObject = favouritesFragmentCreateJSONObject();
        progressDialog.show();
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, FAVOURITES_FRAGMENT_URL_STRING, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (validJSONObject("Users", response)) {
                            favouritesFragmentParseJSONResponse(response, write);
                        } else {
                            if (validJSONObject("value1", response)) {
                                try {
                                    String value1 = response.getString("value1");
                                    if (value1.equals("empty")) {
                                        favouritesFragmentList = new ArrayList<>();
                                        String tag = favouritesFragment.getClass().toString();
                                        favouritesFragment = (FavouritesFragment) getSupportFragmentManager().findFragmentByTag(tag);
                                        favouritesFragment.refreshList(favouritesFragmentList);
                                        writeFavouritesToCache(favouritesFragmentList);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            progressDialog.hide();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError ex) {
                        loadFavouritesFragment(false);
                        progressDialog.hide();
                        handleVolleyError(ex);
                    }
                }
        );
        int socketTimeout = 10000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        objectRequest.setRetryPolicy(policy);
        requestQueue.add(objectRequest);
    } //done

    private JSONObject favouritesFragmentCreateJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            String email_address = prefs.getString("email_address", null);
            if (email_address != null)
                jsonObject.put("email_address", email_address);
        } catch (JSONException e) {
            if (e.getMessage() != null) {
                progressDialog.hide();
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("JSONException", e.getMessage());
            }
        }
        return jsonObject;
    } //done

    private void favouritesFragmentParseJSONResponse(JSONObject response, Boolean write) {
        try {
            JSONArray jsonArray = response.getJSONArray("Users");
            for (int x = 0; x < jsonArray.length(); x++) {
                JSONObject temp = jsonArray.getJSONObject(x);
                String email_address = temp.getString("email_address");
                String first_name = temp.getString("first_name");
                String last_name = temp.getString("last_name");
                String profile_picture = temp.getString("profile_picture");
                String user_status = temp.getString("user_status");
                JSONArray skillsArray = temp.getJSONArray("skills");
                ArrayList<String> skills = new ArrayList<>();
                for (int a = 0; a < skillsArray.length(); a++) {
                    JSONObject temp2 = skillsArray.getJSONObject(a);
                    skills.add(temp2.getString("skill"));
                }
                Collections.sort(skills);
                String favourite_count = temp.getString("favourite_count");
                favouritesFragmentList.add(new User(email_address, first_name, last_name, profile_picture, skills, favourite_count, user_status));
            }
            String tag = favouritesFragment.getClass().toString();
            favouritesFragment = (FavouritesFragment) getSupportFragmentManager().findFragmentByTag(tag);
            favouritesFragment.refreshList(favouritesFragmentList);
            if (write)
                writeFavouritesToCache(favouritesFragmentList);
            progressDialog.hide();
        } catch (JSONException e) {
            progressDialog.hide();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("JSONException", e.getMessage());
        }
    } //done

    private void writeFavouritesToCache(ArrayList<User> users) {
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), "") + File.separator + user_email_address + FAVOURITES_FILENAME));
            out.writeObject(users);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    } //done

    private ArrayList<User> readFavouritesFromCache() {
        ArrayList<User> returnList = new ArrayList<>();
        ObjectInputStream input;
        try {
            input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(), "") + File.separator + user_email_address + FAVOURITES_FILENAME)));
            returnList = (ArrayList<User>) input.readObject();
            input.close();
        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return returnList;
    } //done

    private Boolean validJSONObject(String key, JSONObject jsonObject) {
        return !jsonObject.isNull(key) && jsonObject.has(key);
    } //done

    private void handleVolleyError(VolleyError ex) {
        progressDialog.hide();
        if (ex instanceof TimeoutError || ex instanceof NoConnectionError) {
            Toast.makeText(getApplicationContext(), "No internet access or server is down.", Toast.LENGTH_SHORT).show();
        } else if (ex instanceof AuthFailureError) {
            Toast.makeText(getApplicationContext(), "Authentication error", Toast.LENGTH_SHORT).show();
        } else if (ex instanceof ServerError) {
            Toast.makeText(getApplicationContext(), "Server error", Toast.LENGTH_SHORT).show();
        } else if (ex instanceof NetworkError) {
            Toast.makeText(getApplicationContext(), "Network error", Toast.LENGTH_SHORT).show();
        } else if (ex instanceof ParseError) {
            Toast.makeText(getApplicationContext(), "Parse error", Toast.LENGTH_SHORT).show();
        }
        if (ex.getMessage() != null)
            Log.e("VolleyError", ex.getMessage());
    } //done

    private void replaceFragment(Fragment fragment) {
        String tag = fragment.getClass().toString();
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment, tag);
            fragmentTransaction.addToBackStack(tag);
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            fragmentTransaction.commit();
            getSupportFragmentManager().executePendingTransactions();
        } catch (Exception ex) {
            Log.e("FragmentException", ex.getMessage());
        }
    } //done

    @Override
    public void refreshFragment() {
        if (user_logged_in) {
            if (searchFragmentSelected) {
                loadSearchFragment(true, query);
            }
            if (favouritesFragmentSelected) {
                loadFavouritesFragment(true);
            }
            if (jobInboxFragmentSelected) {
                loadJobInboxFragment(true);
            }
            if (jobOutboxFragmentSelected) {
                loadJobOutBoxFragment(true);
            }
        } else
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onBackPressed() {
        if (drawerFragment.isVisible()) {
            drawerFragment.close();
        } else {
            if (!searchFragmentSelected) {
                loadSearchFragment(false, null);
            } else {
                finish();
            }
        }
    } //done


    private void setSelectedFragment(LinearLayout layout) {
        if (layout.equals(searchLayout)) {
            searchLayoutBack.setBackgroundColor(getResources().getColor(R.color.light_grey));
            setTitle("Search All");
            searchFragmentSelected = true;
            navSearchImageView.setAlpha(1.0f);
            navSearchTextView.setAlpha(1.0f);
        } else {
            searchLayoutBack.setBackgroundColor(getResources().getColor(R.color.white));
            searchFragmentSelected = false;
            navSearchImageView.setAlpha(0.54f);
            navSearchTextView.setAlpha(0.87f);
        }
        if (layout.equals(favouritesLayout)) {
            favouritesLayoutBack.setBackgroundColor(getResources().getColor(R.color.light_grey));
            setTitle("Favourites");
            favouritesFragmentSelected = true;
            navFavouritesImageView.setAlpha(1.0f);
            navFavouritesTextView.setAlpha(1.0f);
        } else {
            favouritesLayoutBack.setBackgroundColor(getResources().getColor(R.color.white));
            favouritesFragmentSelected = false;
            navFavouritesImageView.setAlpha(0.54f);
            navFavouritesTextView.setAlpha(0.87f);
        }
        if (layout.equals(jobinboxLayout)) {
            jobinboxLayoutBack.setBackgroundColor(getResources().getColor(R.color.light_grey));
            setTitle("Job Inbox");
            jobInboxFragmentSelected = true;
            navJobInboxImageView.setAlpha(1.0f);
            navJobInboxTextView.setAlpha(1.0f);
        } else {
            jobinboxLayoutBack.setBackgroundColor(getResources().getColor(R.color.white));
            jobInboxFragmentSelected = false;
            navJobInboxImageView.setAlpha(0.54f);
            navJobInboxTextView.setAlpha(0.87f);
        }
        if (layout.equals(joboutboxlayout)) {
            joboutboxLayoutBack.setBackgroundColor(getResources().getColor(R.color.light_grey));
            setTitle("Job Outbox");
            jobOutboxFragmentSelected = true;
            navJobOutboxImageView.setAlpha(1.0f);
            navJobOutboxTextView.setAlpha(1.0f);
        } else {
            joboutboxLayoutBack.setBackgroundColor(getResources().getColor(R.color.white));
            jobOutboxFragmentSelected = false;
            navJobOutboxImageView.setAlpha(0.54f);
            navJobOutboxTextView.setAlpha(0.87f);
        }
    } //done

    @Override
    public void drawerClosed() {
        if (accountLayout.getVisibility() == View.VISIBLE) {
            accountLayout.setVisibility(View.GONE);
            navListLayout.setVisibility(View.VISIBLE);
            dropImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_arrow_drop_down_white_48dp));
            statusListLayout.setVisibility(View.GONE);
            statusImageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_arrow_drop_down_black_48dp));

        }
    } //done

    @Override
    public void drawerOpened() {
        if (getWindow().getDecorView().getRootView() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getRootView().getWindowToken(), 0);
        }
    } //done

    public interface FavouritesFragmentListener {
        void onSearch(String string);
    }

    public FavouritesFragmentListener getFavouritesFragmentListener() {
        return favouritesFragmentListener;
    }

    public void setFavouritesFragmentRefreshListener(FavouritesFragmentListener favouriteFragmentListener) {
        this.favouritesFragmentListener = favouriteFragmentListener;
    }

    public interface JobInboxFragmentListener {
        void onSearch(String string);

        void setPosition(int pos);
    }

    public JobInboxFragmentListener getJobInboxFragmentListener() {
        return jobInboxFragmentListener;
    }

    public void setJobInboxFragmentListener(JobInboxFragmentListener jobInboxFragmentListener) {
        this.jobInboxFragmentListener = jobInboxFragmentListener;
    }

    public interface JobOutboxFragmentListener {
        void onSearch(String string);

        void setPosition(int pos);
    }

    public JobOutboxFragmentListener getJobOutboxFragmentListener() {
        return jobOutboxFragmentListener;
    }

    public void setJobOutboxFragmentListener(JobOutboxFragmentListener jobOutboxFragmentListener) {
        this.jobOutboxFragmentListener = jobOutboxFragmentListener;
    }

    private void clearQuery() {
        query = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        MenuItem item = menu.findItem(R.id.searchButton);
        sv = new SearchView(this.getSupportActionBar().getThemedContext());
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW | MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        MenuItemCompat.setActionView(item, sv);
        sv.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        MenuItemCompat.setOnActionExpandListener(item, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                if (searchFragmentSelected) {
                    clearQuery();
                    searchFragmentList = new ArrayList<>();
                    String tag = searchFragment.getClass().toString();
                    searchFragment = (SearchFragment) getSupportFragmentManager().findFragmentByTag(tag);
                    searchFragment.clearList();
                    if (getWindow().getDecorView().getRootView() != null) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getWindow().getDecorView().getRootView().getWindowToken(), 0);
                    }
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }
        });
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (searchFragmentSelected) {
                    sv.clearFocus();
                    loadSearchFragment(true, query);
                    if (getWindow().getDecorView().getRootView() != null) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getWindow().getDecorView().getRootView().getWindowToken(), 0);
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if (favouritesFragmentSelected && favouritesFragmentList.size() != 0) {
                    getFavouritesFragmentListener().onSearch(query);
                }
                if (jobInboxFragmentSelected && jobInboxFragmentList.size() != 0) {
                    getJobInboxFragmentListener().onSearch(query);
                }
                if (jobOutboxFragmentSelected && jobOutboxFragmentList.size() != 0) {
                    getJobOutboxFragmentListener().onSearch(query);
                }
                if (searchFragmentSelected) {
                    if (query.length() == 0) {
                        clearQuery();
                        searchFragmentList = new ArrayList<>();
                        String tag = searchFragment.getClass().toString();
                        searchFragment = (SearchFragment) getSupportFragmentManager().findFragmentByTag(tag);
                        searchFragment.clearList();
                    }
                }
                return false;
            }
        });

        return true;
    } //done

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }


    ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceDisconnected(ComponentName name) {
            mBounded = false;
            mServer = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            mBounded = true;
            OutSauceService.LocalBinder mLocalBinder = (OutSauceService.LocalBinder) service;
            mServer = mLocalBinder.getServerInstance();
            getBundle();
        }
    };
}
