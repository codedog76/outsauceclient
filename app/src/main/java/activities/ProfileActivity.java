package activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.android.volley.toolbox.Volley;

import other.ObscuredSharedPreferences;

import com.example.outsauce.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView contactNumberMainTextView, contactNumberSecondaryTextView, emailAddressMainTextView, emailAddressSecondaryTextView, studyMainTextView, studySecondaryTextView,
            favouriteCountMainTextView, favouriteCountSecondaryTextView, dateCreatedMainTextView, dateCreatedSecondaryTextView, statusResultText;
    private LinearLayout contactNumberLayout, emailAddressLayout, contentLayout, expertiseLayout, statusLayout;
    private View contactNumberLayoutDiv;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private FloatingActionButton favouriteFab, jobFab;
    private String email_address1, email_address2;
    private ProgressDialog progressDialog;
    private RequestQueue requestQueue;
    private CoordinatorLayout profileCoordinatorLayout;
    private NestedScrollView nestedScrollView;
    private SharedPreferences prefs;
    private Boolean favouritesChanged = false;
    private ImageView profile_image;
    private Boolean favourite = false;
    private ProgressBar progressBar;
    private static final String GET_USER_URL_STRING = "http://opensouce.csdev.nmmu.ac.za/api/users/user";
    private static final String NOT_LOGGED_GET_USER_URL_STRING = "http://opensouce.csdev.nmmu.ac.za/api/users/useroffline";
    private static final String REMOVE_FAV_USER_URL_STRING = "http://opensouce.csdev.nmmu.ac.za/api/users/removefavourite";
    private static final String ADD_FAV_USER_URL_STRING = "http://opensouce.csdev.nmmu.ac.za/api/users/addfavourite";
    private static final String GET_USER_PROFILE_PICTURE_URL_STRING = "http://opensouce.csdev.nmmu.ac.za/api/users/bigpicture";
    private static final String GET_USER_REPORT_URL_STRING = "http://opensouce.csdev.nmmu.ac.za/api/users/report";
    private int favouriteCount;
    private String contact_number = null;
    private String first_name = null;
    private String last_name = null;
    private String email_address = null;
    private String user_status = null;
    private Boolean logged = false;
    private Boolean fromMail = false;
    private Boolean fromSearch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        //assign views
        assignViews();
        //assign action bar
        assignActionBar();
        //assign fonts
        assignFonts();
        //custom encryption wrapper for sharedpreferences
        prefs = new ObscuredSharedPreferences(this, this.getSharedPreferences("SECURE", Context.MODE_PRIVATE));
        //assign progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading..");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        //fetch bundle
        getBundle();
        //fetch prefs
        logged = prefs.getBoolean("user_logged_in", false);
        if (logged)
            email_address1 = prefs.getString("email_address", null);

        nestedScrollView.setVisibility(View.GONE);

        requestQueue = Volley.newRequestQueue(this);
        favouriteFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (favourite) {
                    removeFavouriteJSONRequest();
                } else {
                    addFavouriteJSONRequest();
                }
                favouritesChanged = true;
            }
        });
        jobFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), NewJobActivity.class);
                Bundle b = new Bundle();
                b.putString("email_address1", email_address1);
                b.putString("first_name", first_name);
                b.putString("last_name", last_name);
                b.putString("user_status", user_status);
                b.putString("email_address2", email_address2);
                intent.putExtras(b);
                startActivity(intent);
            }
        });
        if (logged) {
            userProfileJSONRequest(GET_USER_URL_STRING);
        } else {
            userProfileJSONRequest(NOT_LOGGED_GET_USER_URL_STRING);
            favouriteFab.setVisibility(View.GONE);
            jobFab.setVisibility(View.GONE);
        }

        userProfilePictureJSONRequest();
        contactNumberLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (contact_number != null && !contact_number.equals("")) {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + contact_number));
                    startActivity(callIntent);
                } else {
                    Snackbar.make(profileCoordinatorLayout, "Invalid phone number", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
                return false;
            }
        });
        emailAddressLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", email_address, null));
                startActivity(Intent.createChooser(emailIntent, "Send email..."));
                return false;
            }
        });
    }

    private void getBundle() {
        Bundle b = getIntent().getExtras();
        email_address2 = b.getString("email_address2", null);
        fromMail = b.getBoolean("fromMail", false);
        if (fromMail) {
            jobFab.setVisibility(View.GONE);
        }
        fromSearch = b.getBoolean("fromSearch", false);
    }

    private void setFavouriteStar() {
        if (favourite) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                favouriteFab.setImageDrawable(getResources().getDrawable(R.drawable.ic_star_white_48dp, this.getTheme()));
            } else {
                favouriteFab.setImageDrawable(getResources().getDrawable(R.drawable.ic_star_white_48dp));
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                favouriteFab.setImageDrawable(getResources().getDrawable(R.drawable.ic_star_border_white_48dp, this.getTheme()));
            } else {
                favouriteFab.setImageDrawable(getResources().getDrawable(R.drawable.ic_star_border_white_48dp));
            }
        }
    }

    private void addFavouriteJSONRequest() {
        JSONObject jsonObject = addFavouriteCreateJSONObject();
        progressDialog.show();
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, ADD_FAV_USER_URL_STRING, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (validJSONObject("value1", response)) {
                            addFavouriteParseJSONRequest(response);
                            progressDialog.dismiss();
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Error occured", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError ex) {
                        progressDialog.dismiss();
                        handleVolleyError(ex);
                    }
                }
        );
        requestQueue.add(objectRequest);
    }

    private JSONObject addFavouriteCreateJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("value1", email_address1);
            jsonObject.put("value2", email_address2);
        } catch (JSONException e) {
            if (e.getMessage() != null) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("JSONException", e.getMessage());
            }
        }
        return jsonObject;
    }

    private void addFavouriteParseJSONRequest(JSONObject response) {
        try {
            if (response.getString("value1").equals("done")) {
                favourite = true;
                setFavouriteStar();
                Snackbar.make(profileCoordinatorLayout, "Added as a favourite", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                favouriteCount++;
                favouriteCountMainTextView.setText(Integer.toString(favouriteCount));
            } else {
                favourite = false;
                setFavouriteStar();
                Snackbar.make(profileCoordinatorLayout, "Failed to add user to favourites", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        } catch (JSONException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void removeFavouriteJSONRequest() {
        JSONObject jsonObject = removeFavouriteCreateJSONObject();
        progressDialog.show();
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, REMOVE_FAV_USER_URL_STRING, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (validJSONObject("value1", response)) {
                            removeFavouriteParseJSONRequest(response);
                            progressDialog.dismiss();
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Error occured", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError ex) {
                        progressDialog.dismiss();
                        handleVolleyError(ex);
                    }
                }
        );
        requestQueue.add(objectRequest);
    }

    private JSONObject removeFavouriteCreateJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("value1", email_address1);
            jsonObject.put("value2", email_address2);
        } catch (JSONException e) {
            if (e.getMessage() != null) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("JSONException", e.getMessage());
            }
        }
        return jsonObject;
    }

    private void removeFavouriteParseJSONRequest(JSONObject response) {
        try {
            if (response.getString("value1").equals("done")) {
                favourite = false;
                setFavouriteStar();
                Snackbar.make(profileCoordinatorLayout, "Removed as a favourite", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                favouriteCount--;
                favouriteCountMainTextView.setText(Integer.toString(favouriteCount));
            } else {
                favourite = true;
                setFavouriteStar();
                Snackbar.make(profileCoordinatorLayout, "Failed to remove user from favourites", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        } catch (JSONException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void userProfileJSONRequest(String URL) {
        JSONObject jsonObject = userProfileCreateJSONObject();
        progressDialog.show();
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, URL, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (validJSONObject("email_address", response)) {
                            userProfileParseJSONRequest(response);
                            progressDialog.dismiss();
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "User details not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError ex) {
                        progressDialog.dismiss();
                        handleVolleyError(ex);
                        finish();
                    }
                }
        );
        int socketTimeout = 10000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        objectRequest.setRetryPolicy(policy);
        requestQueue.add(objectRequest);
    }

    private JSONObject userProfileCreateJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (logged)
                jsonObject.put("value1", email_address1);
            jsonObject.put("value2", email_address2);
        } catch (JSONException e) {
            if (e.getMessage() != null) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("JSONException", e.getMessage());
            }
        }
        return jsonObject;
    }

    private void userProfileParseJSONRequest(JSONObject response) {
        try {
            first_name = response.getString("first_name");
            last_name = response.getString("last_name");
            collapsingToolbarLayout.setTitle(first_name + " " + last_name);
            if (logged) {
                if (response.getString("is_favourite").equals("1")) {
                    favourite = true;
                    setFavouriteStar();
                } else {
                    favourite = false;
                    setFavouriteStar();
                }
            }
            JSONArray skillsArray = response.getJSONArray("skills");
            for (int a = 0; a < skillsArray.length(); a++) {
                JSONObject obj = skillsArray.getJSONObject(a);
                String tempSkill = obj.getString("skill");
                addItems(tempSkill);
            }
            contact_number = response.getString("contact_number");
            if (contact_number != null && !contact_number.equals("")) {
                contactNumberMainTextView.setText(contact_number);
            } else {
                contactNumberLayout.setVisibility(View.GONE);
                contactNumberLayoutDiv.setVisibility(View.GONE);
            }
            email_address = response.getString("email_address");
            if (email_address != null && !email_address.equals("")) {
                emailAddressMainTextView.setText(email_address);
            }
            user_status = response.getString("user_status");
            if (user_status.equals("Available")) {
                statusLayout.setBackgroundColor(getResources().getColor(R.color.green));
                statusResultText.setText("Available");
            }
            if (user_status.equals("Busy")) {
                statusLayout.setBackgroundColor(getResources().getColor(R.color.orange));
                statusResultText.setText(user_status);
            }
            if (user_status.equals("Unavailable")) {
                statusLayout.setBackgroundColor(getResources().getColor(R.color.errorRed));
                statusResultText.setText("Unavailable");
                jobFab.setVisibility(View.GONE);
            }
            studyMainTextView.setText(response.getString("course_name"));
            studySecondaryTextView.setText(response.getString("year_of_study"));
            String currentFavouriteCount = response.getString("favourite_count");
            favouriteCount = Integer.valueOf(currentFavouriteCount);
            favouriteCountMainTextView.setText(currentFavouriteCount);
            favouriteCountSecondaryTextView.setText("Favourite Count");
            String startDateString = response.getString("date_created");
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
            SimpleDateFormat format2 = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
            String newDateString = "";
            Date startDate;
            try {
                startDate = format1.parse(startDateString);
                newDateString = format2.format(startDate);
            } catch (ParseException e) {
                Log.e("ParseException", e.getMessage());
            }
            dateCreatedMainTextView.setText(newDateString);
            dateCreatedSecondaryTextView.setText("Date Created");
            int user_report = response.getInt("user_report");
            if(user_report >= 5){
                favouriteFab.setVisibility(View.GONE);
                jobFab.setVisibility(View.GONE);
                statusLayout.setBackgroundColor(getResources().getColor(R.color.errorRed));
                statusResultText.setText("Disabled");
                jobFab.setVisibility(View.GONE);
                showReportedWarningDialog();
            }
            nestedScrollView.setVisibility(View.VISIBLE);
        } catch (JSONException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showReportedWarningDialog() {
        new AlertDialog.Builder(this)
                .setMessage("This user has been disabled due to high reports")
                .setPositiveButton("Ok", null).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
    }

    private void userProfilePictureJSONRequest() {
        JSONObject jsonObject = userProfilePictureCreateJSONObject();
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, GET_USER_PROFILE_PICTURE_URL_STRING, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (validJSONObject("value1", response)) {
                            userProfilePictureParseJSONRequest(response);
                            progressBar.setVisibility(View.INVISIBLE);
                        } else {
                            progressBar.setVisibility(View.INVISIBLE);
                            setDefaultProfilePicture();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError ex) {
                        handleVolleyError(ex);
                    }
                }
        );
        int socketTimeout = 10000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        objectRequest.setRetryPolicy(policy);
        requestQueue.add(objectRequest);
    }

    private JSONObject userProfilePictureCreateJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("value1", email_address2);
        } catch (JSONException e) {
            if (e.getMessage() != null) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("JSONException", e.getMessage());
            }
        }
        return jsonObject;
    }

    private void userProfilePictureParseJSONRequest(JSONObject response) {
        try {
            String base64 = response.getString("value1");
            if (base64 != null && !base64.equals("")) {
                byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                profile_image.setImageBitmap(decodedByte);
            } else {
                setDefaultProfilePicture();
            }
        } catch (JSONException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setDefaultProfilePicture() {
        profile_image.setBackground(this.getResources().getDrawable(R.drawable.avatar_male));
    }

    public void addItems(String item) {
        TextView rowTextView = new TextView(this);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, 0, 0, 3); // llp.setMargins(left, top, right, bottom);
        rowTextView.setLayoutParams(llp);
        rowTextView.setText(item);
        expertiseLayout.addView(rowTextView);
    }

    private Boolean validJSONObject(String key, JSONObject jsonObject) {
        return !jsonObject.isNull(key) && jsonObject.has(key);
    }

    private void assignViews() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        profile_image = (ImageView) findViewById(R.id.profilePicture);
        contactNumberMainTextView = (TextView) findViewById(R.id.contactNumberMainTextView);
        contactNumberSecondaryTextView = (TextView) findViewById(R.id.contactNumberSecondaryTextView);
        emailAddressMainTextView = (TextView) findViewById(R.id.emailAddressMainTextView);
        emailAddressSecondaryTextView = (TextView) findViewById(R.id.emailAddressSecondaryTextView);
        studyMainTextView = (TextView) findViewById(R.id.studyMainTextView);
        studySecondaryTextView = (TextView) findViewById(R.id.studySecondaryTextView);
        favouriteCountMainTextView = (TextView) findViewById(R.id.favouriteCountMainTextView);
        favouriteCountSecondaryTextView = (TextView) findViewById(R.id.favouriteCountSecondaryTextView);
        dateCreatedMainTextView = (TextView) findViewById(R.id.dateCreatedMainTextView);
        dateCreatedSecondaryTextView = (TextView) findViewById(R.id.dateCreatedSecondaryTextView);
        profileCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.profileCoordinatorLayout);
        favouriteFab = (FloatingActionButton) findViewById(R.id.favouriteFab);
        jobFab = (FloatingActionButton) findViewById(R.id.jobFab);
        contactNumberLayout = (LinearLayout) findViewById(R.id.contactNumberLayout);
        emailAddressLayout = (LinearLayout) findViewById(R.id.emailAddressLayout);
        expertiseLayout = (LinearLayout) findViewById(R.id.expertiseLayout);
        contentLayout = (LinearLayout) findViewById(R.id.contentLayout);
        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        contactNumberLayoutDiv = findViewById(R.id.contactNumberLayoutDiv);
        nestedScrollView = (NestedScrollView) findViewById(R.id.nestedScrollView);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        statusLayout = (LinearLayout) findViewById(R.id.statusLayout);
        statusResultText = (TextView) findViewById(R.id.statusResultText);
    }

    private void assignFonts() {
        Typeface Roboto_Regular = Typeface.createFromAsset(getAssets(), "Roboto-Regular.ttf");
        contactNumberMainTextView.setTypeface(Roboto_Regular);
        contactNumberSecondaryTextView.setTypeface(Roboto_Regular);
        emailAddressMainTextView.setTypeface(Roboto_Regular);
        emailAddressSecondaryTextView.setTypeface(Roboto_Regular);
        studyMainTextView.setTypeface(Roboto_Regular);
        studySecondaryTextView.setTypeface(Roboto_Regular);
        favouriteCountMainTextView.setTypeface(Roboto_Regular);
        favouriteCountSecondaryTextView.setTypeface(Roboto_Regular);
        dateCreatedMainTextView.setTypeface(Roboto_Regular);
        dateCreatedSecondaryTextView.setTypeface(Roboto_Regular);

    }

    private void assignActionBar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void handleVolleyError(VolleyError ex) {
        if (ex instanceof TimeoutError || ex instanceof NoConnectionError) {
            Toast.makeText(getApplicationContext(), "No internet access or server is down", Toast.LENGTH_SHORT).show();
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        MenuItem item = menu.findItem(R.id.report_user);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (logged)
                    showReportDialog();
                else
                    showLoginDialog();
                return false;
            }
        });
        return true;
    }

    private void showReportDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to report this user?")
                        //.setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        userReportJSONRequest();
                    }
                })
                .setNegativeButton("No", null).show();
    }

    private void userReportJSONRequest() {
        JSONObject jsonObject = userReportCreateJSONObject();
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, GET_USER_REPORT_URL_STRING, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (validJSONObject("value1", response)) {
                            userReportParseJSONRequest(response);
                        } else {
                            Snackbar.make(profileCoordinatorLayout, "Report Failed", Snackbar.LENGTH_SHORT)
                                    .setAction("Action", null).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError ex) {
                        handleVolleyError(ex);
                    }
                }
        );
        int socketTimeout = 10000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        objectRequest.setRetryPolicy(policy);
        requestQueue.add(objectRequest);
    }

    private JSONObject userReportCreateJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("value1", email_address2);
        } catch (JSONException e) {
            if (e.getMessage() != null) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("JSONException", e.getMessage());
            }
        }
        return jsonObject;
    }

    private void userReportParseJSONRequest(JSONObject response) {
        try {
            String value1 = response.getString("value1");
            if (value1.equals("success")) {
                Snackbar.make(profileCoordinatorLayout, "Report Succeeded", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                favouritesChanged = true;
            }
            if (value1.equals("failed")) {
                Snackbar.make(profileCoordinatorLayout, "Report Failed", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        } catch (JSONException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoginDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmation")
                .setMessage("Please log in to report someone?")
                        //.setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void goBack() {
        if (fromMail) {
            finish();
        } else {
            if (fromSearch && !favouritesChanged) {
                finish();
            } else {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                Bundle b = new Bundle();
                if (favouritesChanged) {
                    b.putBoolean("refresh", true);
                }
                b.putString("fragment", "favourites");
                intent.putExtras(b);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onBackPressed() {
        goBack();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                goBack();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
