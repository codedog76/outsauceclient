package activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class NewJobActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView rowNameText, rowEmailText, timeText, dateText;
    private EditText titleText, descriptionText;
    private String email_address1, email_address2, first_name, last_name, user_status;
    private LinearLayout timeLayout, dateLayout;
    private ProgressDialog progressDialog;
    private static final String NEW_JOB_URL_STRING = "http://opensouce.csdev.nmmu.ac.za/api/users/newjob";
    private static final String GET_USER_PROFILE_PICTURE_URL_STRING = "http://opensouce.csdev.nmmu.ac.za/api/users/smallpicture";
    private RequestQueue requestQueue;
    private CircleImageView profile_image;
    private AlertDialog timeDialog, dateDialog;
    private View timeView, dateView;
    private SimpleDateFormat time_format = new SimpleDateFormat("hh:mm aa", Locale.ENGLISH);
    private SimpleDateFormat date_format = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
    private String currentSetTime;
    private String currentSetDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_new);
        //assign views
        assignViews();
        //assign fonts
        assignFonts();
        //assign action bar
        assignActionBar();
        getBundle();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading..");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        requestQueue = Volley.newRequestQueue(this);
        assignTimeDate();
        assignUserView();
    }

    private void assignTimeDate() {

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        currentSetTime = time_format.format(cal.getTime());
        timeText.setText(currentSetTime);
        currentSetDate = date_format.format(cal.getTime());
        dateText.setText(currentSetDate);
        final TimePicker timePicker = (TimePicker) timeView.findViewById(R.id.time_picker);
        int hour=cal.get(Calendar.HOUR_OF_DAY);
        int min=cal.get(Calendar.MINUTE);
        timePicker.setCurrentHour(hour);
        timePicker.setCurrentMinute(min);

        final DatePicker datePicker = (DatePicker) dateView.findViewById(R.id.date_picker);
        int year=cal.get(Calendar.YEAR);
        int month=cal.get(Calendar.MONTH);
        int day=cal.get(Calendar.DAY_OF_MONTH);
        datePicker.updateDate(year, month, day);

        timeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeDialog.setView(timeView);
                timeDialog.show();
            }
        });
        dateLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dateDialog.setView(dateView);
                dateDialog.show();
            }
        });
        timeView.findViewById(R.id.time_set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar calendar = new GregorianCalendar(datePicker.getYear(),
                        datePicker.getMonth(),
                        datePicker.getDayOfMonth(),
                        timePicker.getCurrentHour(),
                        timePicker.getCurrentMinute());
                currentSetTime = time_format.format(calendar.getTime());
                timeText.setText(currentSetTime);
                timeDialog.dismiss();
            }
        });
        timeView.findViewById(R.id.time_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timeDialog.dismiss();
            }
        });
        dateView.findViewById(R.id.date_set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Calendar calendar = new GregorianCalendar(datePicker.getYear(),
                        datePicker.getMonth(),
                        datePicker.getDayOfMonth(),
                        timePicker.getCurrentHour(),
                        timePicker.getCurrentMinute());
                currentSetDate = date_format.format(calendar.getTime());
                dateText.setText(currentSetDate);
                dateDialog.dismiss();
            }
        });
        dateView.findViewById(R.id.date_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dateDialog.dismiss();
            }
        });
    }

    private void assignUserView() {
        rowNameText.setText(first_name + " " + last_name);
        rowEmailText.setText(email_address2);
        if (user_status.equals("Available")) {
            profile_image.setBorderColor(getResources().getColor(R.color.green));
        }
        if (user_status.equals("Busy")) {
            profile_image.setBorderColor(getResources().getColor(R.color.orange));
        }
        if (user_status.equals("Unavailable")) {
            profile_image.setBorderColor(getResources().getColor(R.color.errorRed));
        }
        userProfilePictureJSONRequest();
    }

    private void getBundle() {
        Bundle b = getIntent().getExtras();
        email_address1 = b.getString("email_address1");
        first_name = b.getString("first_name");
        last_name = b.getString("last_name");
        user_status = b.getString("user_status");
        email_address2 = b.getString("email_address2");
    }

    private void userProfilePictureJSONRequest() {
        JSONObject jsonObject = userProfilePictureCreateJSONObject();
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, GET_USER_PROFILE_PICTURE_URL_STRING, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (validJSONObject("value1", response)) {
                            userProfilePictureParseJSONRequest(response);
                        } else {
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
            Log.e("SmallPictureRequest", email_address2);
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

    private void newJobJSONRequest() {
        JSONObject jsonObject = newJobCreateJSONObject();
        progressDialog.show();
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, NEW_JOB_URL_STRING, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (validJSONObject("value1", response)) {
                            newJobParseJSONRequest(response);
                        } else {
                            progressDialog.hide();

                            try {
                                String errorMessage = response.getString("error");
                                Log.e("newJobJSONRequest Error", errorMessage);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(getApplicationContext(), "Message failed to send", Toast.LENGTH_SHORT).show();
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
        requestQueue.add(objectRequest);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
    }


    private JSONObject newJobCreateJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("email_address_sender", email_address1);
            jsonObject.put("email_address_receiver", email_address2);
            jsonObject.put("job_title", titleText.getText().toString());
            jsonObject.put("job_description", descriptionText.getText().toString());
            Log.e("currentDateTime", currentSetTime + " " + currentSetDate);
            jsonObject.put("job_due_date", currentSetTime+" "+currentSetDate);
        } catch (JSONException e) {
            if (e.getMessage() != null) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("JSONException", e.getMessage());
            }
        }
        return jsonObject;
    }

    private void newJobParseJSONRequest(JSONObject response) {
        try {
            if (response.getString("value1").equals("done")) {
                progressDialog.hide();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                Bundle b = new Bundle();
                b.putBoolean("refresh", true);
                b.putString("fragment", "outbox");
                intent.putExtras(b);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                progressDialog.hide();
                Toast.makeText(getApplicationContext(), "Message failed to send", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            progressDialog.hide();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Boolean validJSONObject(String key, JSONObject jsonObject) {
        return !jsonObject.isNull(key) && jsonObject.has(key);
    }

    private void setDefaultProfilePicture() {
        profile_image.setImageDrawable(getResources().getDrawable(R.drawable.profile_default));
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

    private void assignActionBar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Post Job");
    }

    private void assignViews() {
        titleText = (EditText) findViewById(R.id.titleText);
        descriptionText = (EditText) findViewById(R.id.descriptionText);
        rowNameText = (TextView) findViewById(R.id.rowNameText);
        rowEmailText = (TextView) findViewById(R.id.rowEmailText);
        toolbar = (Toolbar) findViewById(R.id.app_actionbar);
        profile_image = (CircleImageView) findViewById(R.id.profile_image);
        timeLayout = (LinearLayout) findViewById(R.id.timeLayout);
        dateLayout = (LinearLayout) findViewById(R.id.dateLayout);
        timeText = (TextView)findViewById(R.id.timeText);
        dateText = (TextView)findViewById(R.id.dateText);

        timeDialog = new AlertDialog.Builder(this).create();
        dateDialog = new AlertDialog.Builder(this).create();
        dateView = View.inflate(this, R.layout.date_dialog, null);
        timeView = View.inflate(this, R.layout.time_dialog, null);
    }

    private Boolean validateText() {
        Boolean validateTitle = validateTitleText();
        Boolean validateDescr = validateDescriptionText();
        if (validateTitle && validateDescr)
            return true;
        else
            return false;

    }

    private Boolean validateTitleText() {
        if (titleText.getText().length() == 0) {
            titleText.setError("A title is required");
            return false;
        }
        return true;
    }

    private Boolean validateDescriptionText() {
        if (descriptionText.getText().length() == 0) {
            descriptionText.setError("A description is required");
            return false;
        }
        return true;
    }

    private void assignFonts() {
        Typeface Roboto_Regular = Typeface.createFromAsset(getAssets(), "Roboto-Regular.ttf");
        titleText.setTypeface(Roboto_Regular);
        descriptionText.setTypeface(Roboto_Regular);
        rowNameText.setTypeface(Roboto_Regular);
        rowEmailText.setTypeface(Roboto_Regular);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_job_new, menu);
        MenuItem item = menu.findItem(R.id.doneButton);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (validateText()) {
                    newJobJSONRequest();
                }
                return false;
            }
        });
        return true;
    }

    @Override
    public void onBackPressed() {
        showLogoutDialog();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                showLogoutDialog();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Are you want to discard this new job?")
                        //.setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        finish();
                    }
                })
                .setNegativeButton("No", null).show();
    }
}
