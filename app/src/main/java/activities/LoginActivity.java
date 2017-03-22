package activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
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
import other.ObscuredSharedPreferences;
import com.example.outsauce.R;
import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity { //pretty good

    private static final String URL_STRING = "http://opensouce.csdev.nmmu.ac.za/api/users/login";
    private LinearLayout emailTextLayout, passwordTextLayout;
    private EditText emailText, passwordText;
    private Button signinButton, registerButton;
    private RequestQueue requestQueue;
    private ProgressDialog progressDialog;
    private TextView logoTextView;
    private Toolbar toolbar;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //assign views
        assignViews();
        //assign fonts
        assignFonts();
        //improve editText selection
        improveEditTextSelection();
        //assign action bar
        assignActionBar();
        //assign progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading..");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        //assign new request queue for volley
        requestQueue = Volley.newRequestQueue(this);
        //custom encryption wrapper for sharedpreferences
        prefs = new ObscuredSharedPreferences(this, this.getSharedPreferences("SECURE", Context.MODE_PRIVATE));
        //clear stored preferences
        clearPreferences();
        signinButton.setOnClickListener(new View.OnClickListener() {//possibly move to a thread
                                            @Override
                                            public void onClick(View v) {
                                                if (isNetworkAvailable()) {
                                                    loginJSONRequest();
                                                } else {
                                                    Toast.makeText(getApplicationContext(), "No internet connection detected", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }
        );
        passwordText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (isNetworkAvailable()) {
                        loginJSONRequest();
                    } else {
                        Toast.makeText(getApplicationContext(), "No internet connection detected", Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }
        });
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void loginJSONRequest() {
        //validate email and password
        if (validateText()) {
            //create a jsonObject to send data to web api
            JSONObject jsonObject = createJsonObject();
            progressDialog.show();
            JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, URL_STRING, jsonObject,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if (validJSONObject("email_address", response)) {
                                    String email_address = response.getString("email_address");
                                    String first_name = response.getString("first_name");
                                    String last_name = response.getString("last_name");
                                    String profile_picture = response.getString("profile_picture");
                                    String user_status = response.getString("user_status");
                                    loginPreferences(email_address, first_name, last_name, profile_picture, user_status);
                                    progressDialog.hide();
                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    Bundle b = new Bundle();
                                    b.putString("fragment", "search");
                                    intent.putExtras(b);
                                    startActivity(intent);
                                } else {
                                    if(validJSONObject("message", response)){
                                        String message = response.getString("message");
                                        progressDialog.hide();
                                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                                    }

                                }
                            } catch (JSONException e) {
                                progressDialog.hide();
                                Log.e("JSONException", e.getMessage());
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
            int socketTimeout = 10000;
            RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
            objectRequest.setRetryPolicy(policy);
            requestQueue.add(objectRequest);
        }
    }

    private void improveEditTextSelection() {
        emailTextLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focusEditText(emailText);

            }
        });
        passwordTextLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focusEditText(passwordText);
            }
        });
    }

    private void focusEditText(EditText editText) {
        editText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    private void assignActionBar() {
        setSupportActionBar(toolbar);
        //back button
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private JSONObject createJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("email_address", emailText.getText().toString());
            jsonObject.put("password", passwordText.getText().toString());
        } catch (JSONException e) {
            if (e.getMessage() != null) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("JSONException", e.getMessage());
            }
        }
        return jsonObject;
    }

    private void loginPreferences(String email_address, String first_name, String last_name, String profile_picture, String user_status) {
        prefs.edit().putBoolean("user_logged_in", true).apply();
        prefs.edit().putString("first_name", first_name).apply();
        prefs.edit().putString("last_name", last_name).apply();
        prefs.edit().putString("email_address", email_address).apply();
        prefs.edit().putString("profile_picture", profile_picture).apply();
        prefs.edit().putString("user_status", user_status).apply();
    }

    private void clearPreferences() {
        prefs.edit().remove("user_logged_in").apply();
        prefs.edit().remove("first_name").apply();
        prefs.edit().remove("last_name").apply();
        prefs.edit().remove("email_address").apply();
        prefs.edit().remove("profile_picture").apply();
        prefs.edit().remove("user_status").apply();
    }

    private void assignViews() {
        emailTextLayout = (LinearLayout) findViewById(R.id.emailTextLayout);
        passwordTextLayout = (LinearLayout) findViewById(R.id.passwordTextLayout);
        logoTextView = (TextView) findViewById(R.id.logoTextView);
        emailText = (EditText) findViewById(R.id.emailText);
        passwordText = (EditText) findViewById(R.id.passwordText);
        signinButton = (Button) findViewById(R.id.signinButton);
        registerButton = (Button) findViewById(R.id.registerButton);
        toolbar = (Toolbar) findViewById(R.id.app_actionbar);
    }

    private void assignFonts() {
        Typeface Roboto_Medium = Typeface.createFromAsset(getAssets(), "Roboto-Medium.ttf");
        Typeface Roboto_Regular = Typeface.createFromAsset(getAssets(), "Roboto-Regular.ttf");
        emailText.setTypeface(Roboto_Regular);
        passwordText.setTypeface(Roboto_Regular);
        signinButton.setTypeface(Roboto_Medium);
        registerButton.setTypeface(Roboto_Medium);
        logoTextView.setTypeface(Roboto_Regular);
    }

    private Boolean validJSONObject(String key, JSONObject jsonObject) {
        if (!jsonObject.isNull(key) && jsonObject.has(key)) {
            return true;
        } else {
            return false;
        }
    }

    private Boolean validateText() {
        Boolean validateEmail = validateEmail();
        Boolean validatePassword = validatePassword();
        if (validateEmail && validatePassword) {
            return true;
        }
        return false;
    }

    private Boolean validateEmail() {
        if (emailText.getText().length() == 0) {
            emailText.setError("Email address is required");
            return false;
        } else {
            if (!emailText.getText().toString().contains("@nmmu.ac.za")) {
                emailText.setError("Should contain @nmmmu.ac.za");
                return false;
            } else {
                if (emailText.getText().length() <= 11) {
                    emailText.setError("Email address is too short");
                    return false;
                }
            }
            return true;
        }
    }

    private Boolean validatePassword() {
        if (passwordText.getText().length() == 0) {
            passwordText.setError("Password is required");
            return false;
        } else {
            return true;
        }
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

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login_register, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
