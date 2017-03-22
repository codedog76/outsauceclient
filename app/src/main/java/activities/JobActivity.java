package activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
import com.example.outsauce.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import models.Job;

public class JobActivity extends AppCompatActivity {

    //other user data
    private String email_address, first_name, last_name, profile_picture;


    private Toolbar toolbar;
    private TextView toFromText, rowNameText, rowEmailText, titleText, descriptionText, statusResultText, timeText, dateText;
    private Button pendingAcceptButton, pendingDeclineButton, acceptedYesButton, acceptedNoButton;
    private LinearLayout statusLayout, pendingBar, acceptedBar, userProfileLayout;
    private CircleImageView profile_image;
    private RequestQueue requestQueue;
    private ProgressDialog progressDialog;
    private static final String JOB_STATUS_URL_STRING = "http://opensouce.csdev.nmmu.ac.za/api/users/jobstatus"; //job outbox json url
    private String job_id, job_title, job_description, job_status, from;
    private int position = -1;
    private Boolean changeMade = false;
    private Boolean notification = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job);
        //assign views
        assignViews();
        //assign fonts
        assignFonts();
        //assign action bar
        assignActionBar();
        //assign progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading..");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        //assign new request queue for volley
        requestQueue = Volley.newRequestQueue(this);

        getBundle();
        initializeListeners();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_job, menu);
        return true;
    }

    private void initializeListeners() {
        pendingAcceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPendingAcceptDialog();
            }
        });
        pendingDeclineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPendingDeclineDialog();
            }
        });
        acceptedYesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAcceptedDialog();
            }
        });
        acceptedNoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptedBar.setVisibility(View.GONE);
            }
        });
        userProfileLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                Bundle b = new Bundle();
                b.putBoolean("fromMail", true);
                b.putString("email_address2", email_address);
                intent.putExtras(b);
                startActivity(intent);
            }
        });
    }

    private void showPendingAcceptDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to accept this job?")
                        //.setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        statusChangeJSONRequest("Accepted");
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    private void showPendingDeclineDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to decline this job?")
                        //.setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        statusChangeJSONRequest("Declined");
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    private void showAcceptedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmation")
                .setMessage("Are you sure this job is done?")
                        //.setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        statusChangeJSONRequest("Done");
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    private void statusChangeJSONRequest(String status) {
        JSONObject jsonObject = statusChangeCreateJSONObject(status);
        progressDialog.show();
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, JOB_STATUS_URL_STRING, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (validJSONObject("value1", response)) {
                            statusChangeParseJSONRequest(response);
                            changeMade = true;
                        } else {
                            progressDialog.hide();
                            Toast.makeText(getApplicationContext(), "Unable to change your status", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError ex) {
                        progressDialog.hide();
                        handleVolleyError(ex);
                    }
                }
        );
        int socketTimeout = 1000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        objectRequest.setRetryPolicy(policy);
        requestQueue.add(objectRequest);
    }

    private JSONObject statusChangeCreateJSONObject(String status) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("value1", job_id);
            if (status.equals("Accepted")) {
                jsonObject.put("value2", "Accepted");
            }
            if (status.equals("Declined")) {
                jsonObject.put("value2", "Declined");
            }
            if (status.equals("Done")) {
                jsonObject.put("value2", "Done");
            }
        } catch (JSONException e) {
            if (e.getMessage() != null) {
                progressDialog.hide();
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("JSONException", e.getMessage());
            }
        }
        return jsonObject;
    }

    private void statusChangeParseJSONRequest(JSONObject response) {
        try {
            String temp = response.getString("value1");
            if (!temp.equals("failed")) {
                if (temp.equals("Accepted")) {
                    progressDialog.hide();
                    statusLayout.setBackgroundColor(getResources().getColor(R.color.green));
                    statusResultText.setText("Accepted");
                    job_status = "Accepted";
                    pendingBar.setVisibility(View.GONE);
                    acceptedBar.setVisibility(View.VISIBLE);
                }
                if (temp.equals("Declined")) {
                    progressDialog.hide();
                    statusLayout.setBackgroundColor(getResources().getColor(R.color.errorRed));
                    statusResultText.setText("Declined");
                    job_status = "Declined";
                    pendingBar.setVisibility(View.GONE);
                }
                if (temp.equals("Done")) {
                    progressDialog.hide();
                    statusLayout.setBackgroundColor(getResources().getColor(R.color.darkgrey));
                    statusResultText.setText("Done");
                    job_status = "Done";
                    acceptedBar.setVisibility(View.GONE);
                }
            } else {
                progressDialog.hide();
                Toast.makeText(getApplicationContext(), "Unable to change your status", Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            progressDialog.hide();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("JSONException", e.getMessage());
        }
    }

    private Boolean validJSONObject(String key, JSONObject jsonObject) {
        return !jsonObject.isNull(key) && jsonObject.has(key);
    }

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
    }

    private void assignViews() {
        toFromText = (TextView) findViewById(R.id.toFromText);
        rowNameText = (TextView) findViewById(R.id.rowNameText);
        rowEmailText = (TextView) findViewById(R.id.rowEmailText);
        titleText = (TextView) findViewById(R.id.titleText);
        descriptionText = (TextView) findViewById(R.id.descriptionText);
        statusResultText = (TextView) findViewById(R.id.statusResultText);
        profile_image = (CircleImageView) findViewById(R.id.profile_image);
        statusLayout = (LinearLayout) findViewById(R.id.statusLayout);
        pendingBar = (LinearLayout) findViewById(R.id.pendingBar);
        userProfileLayout = (LinearLayout) findViewById(R.id.userProfileLayout);
        pendingAcceptButton = (Button) findViewById(R.id.pendingAcceptButton);
        pendingDeclineButton = (Button) findViewById(R.id.pendingDeclineButton);
        acceptedYesButton = (Button) findViewById(R.id.acceptedYesButton);
        acceptedNoButton = (Button) findViewById(R.id.acceptedNoButton);
        timeText = (TextView) findViewById(R.id.timeText);
        dateText  = (TextView) findViewById(R.id.dateText);
        pendingBar.setVisibility(View.GONE);
        acceptedBar = (LinearLayout) findViewById(R.id.acceptedBar);
        acceptedBar.setVisibility(View.GONE);
        toolbar = (Toolbar) findViewById(R.id.app_actionbar);
    }

    private void assignFonts() {
        Typeface Roboto_Regular = Typeface.createFromAsset(getAssets(), "Roboto-Regular.ttf");
        toFromText.setTypeface(Roboto_Regular);
        rowNameText.setTypeface(Roboto_Regular);
        rowEmailText.setTypeface(Roboto_Regular);
        titleText.setTypeface(Roboto_Regular);
        descriptionText.setTypeface(Roboto_Regular);
        statusResultText.setTypeface(Roboto_Regular);
    }

    private void getBundle() {
        Bundle b = getIntent().getExtras();
        if (b != null) {
            Job job = (Job) b.getSerializable("job");
            if (job != null) {
                first_name = job.getFirst_name();
                last_name = job.getLast_name();
                email_address = job.getEmail_address();
                String job_due_date = job.getJob_due_date();
                Log.e("job_due_date", job_due_date);
                DateFormat incoming_format = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss aa", Locale.ENGLISH);
                SimpleDateFormat time_format = new SimpleDateFormat("hh:mm aa", Locale.ENGLISH);
                SimpleDateFormat date_format = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
                try {
                    Date date = incoming_format.parse(job_due_date);
                    timeText.setText(time_format.format(date));
                    dateText.setText(date_format.format(date));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                profile_picture = job.getProfile_picture();
                job_id = job.getJob_id();
                job_status = job.getJob_status();
                job_title = job.getJob_title();
                job_description = job.getJob_description();
                rowNameText.setText(first_name + " " + last_name);
                rowEmailText.setText(email_address);
                titleText.setText(job_title);
                descriptionText.setText(job_description);
                if (profile_picture != null && !profile_picture.equals("")) {
                    byte[] decodedString = Base64.decode(profile_picture, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    profile_image.setImageBitmap(decodedByte);
                } else {
                    profile_image.setImageDrawable(getResources().getDrawable(R.drawable.profile_default));
                }
                from = b.getString("from", null);
                notification = b.getBoolean("notification", false);
                position = b.getInt("position", -1);
                if(from!=null){
                    if (from.equals("inbox")) {
                        toFromText.setText("From");
                        if (job_status.equals("Pending")) {
                            statusLayout.setBackgroundColor(getResources().getColor(R.color.orange));
                            statusResultText.setText("Pending");
                            pendingBar.setVisibility(View.VISIBLE);
                        }
                        if (job_status.equals("Accepted")) {
                            statusLayout.setBackgroundColor(getResources().getColor(R.color.green));
                            statusResultText.setText("Accepted");
                            acceptedBar.setVisibility(View.VISIBLE);
                        }
                        if (job_status.equals("Declined")) {
                            statusLayout.setBackgroundColor(getResources().getColor(R.color.errorRed));
                            statusResultText.setText("Declined");
                        }
                        if (job_status.equals("Done")) {
                            statusLayout.setBackgroundColor(getResources().getColor(R.color.darkgrey));
                            statusResultText.setText("Done");
                        }
                    } else {
                        toFromText.setText("To");
                        if (job_status.equals("Pending")) {
                            statusLayout.setBackgroundColor(getResources().getColor(R.color.orange));
                            statusResultText.setText("Pending");
                        }
                        if (job_status.equals("Accepted")) {
                            statusLayout.setBackgroundColor(getResources().getColor(R.color.green));
                            statusResultText.setText("Accepted");
                            acceptedBar.setVisibility(View.VISIBLE);
                        }
                        if (job_status.equals("Declined")) {
                            statusLayout.setBackgroundColor(getResources().getColor(R.color.errorRed));
                            statusResultText.setText("Declined");
                        }
                        if (job_status.equals("Done")) {
                            statusLayout.setBackgroundColor(getResources().getColor(R.color.darkgrey));
                            statusResultText.setText("Done");
                        }
                    }
                }
            } else {
                goBack(from);
            }

        } else {
            goBack(from);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
    }

    private void assignActionBar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void goBack(String toFragment) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        Bundle b = new Bundle();
        if(changeMade) {
            b.putBoolean("refresh", true);
        }
        b.putInt("position", position);
        b.putString("fragment", toFragment);
        intent.putExtras(b);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        goBack(from);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                goBack(from);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
