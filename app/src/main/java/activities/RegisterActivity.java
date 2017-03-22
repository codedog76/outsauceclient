package activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import adapters.ExpertiseInputAdapter;
import other.ObscuredSharedPreferences;
import com.example.outsauce.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class RegisterActivity extends AppCompatActivity implements ExpertiseInputAdapter.clickListener {
    //// TODO: 4/20/2016 implement expertise
    private Toolbar toolbar;
    private EditText emailText, passwordText1, passwordText2, firstnameText, lastnameText, coursenameText, yearofstudyText, contactnumberText, expertiseText;
    private Button expertiseButton;
    private LinearLayout emailTextLayout, passwordText1Layout, passwordText2Layout, firstnameTextLayout, lastnameTextLayout, coursenameTextLayout,
            yearofstudyTextLayout, contactnumberTextLayout, expertiseLayout;
    private TextInputLayout emailTextInputLayout, passwordTextInput1Layout, passwordTextInput2Layout, firstnameTextInputLayout, lastnameTextInputLayout, coursenameTextInputLayout,
            yearofstudyTextInputLayout, contactnumberTextInputLayout, expertiseTextInputLayout;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private AppBarLayout appBarLayout;
    private RequestQueue requestQueue;
    private ProgressDialog progressDialog;
    private FloatingActionButton imageFab;
    private static final String REGISTER_URL_STRING = "http://opensouce.csdev.nmmu.ac.za/api/users/register";
    private static final String LOGIN_URL_STRING = "http://opensouce.csdev.nmmu.ac.za/api/users/login";
    private SharedPreferences prefs;
    private static int RESULT_LOAD_IMAGE = 1;
    private ArrayList<String> listItems;
    private ExpertiseInputAdapter adapter;
    private String encodedImage = null;
    private String encodedImageBig = null;
    private String picturePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        //assign views
        assignViews();
        //assign action bar
        assignActionBar();
        //assign fonts
        assignFonts();
        //improve editText selection
        improveEditTextSelection();
        //assign progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading..");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        //assign new request queue for volley
        requestQueue = Volley.newRequestQueue(this);
        //custom encryption wrapper for sharedpreferences
        prefs = new ObscuredSharedPreferences(this, this.getSharedPreferences("SECURE", Context.MODE_PRIVATE));
        imageFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create intent to Open Image applications like Gallery, Google Photos
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                // Start the Intent
                startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE);
            }
        });
        //expertise
        listItems = new ArrayList<String>();
        // Create the adapter to convert the array to views
        adapter = new ExpertiseInputAdapter(this, listItems);
        adapter.setListener(this);
        // Attach the adapter to a ListView
        ListView listView = (ListView) findViewById(R.id.exerptiseList);
        listView.setAdapter(adapter);
        expertiseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItems();
            }
        });
    }

    public void addItems() {
        if(validateExpertiseInput())
        {
            listItems.add(expertiseText.getText().toString());
            adapter.notifyDataSetChanged();
            expertiseText.getText().clear();
            expertiseText.clearFocus();
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void itemClicked(int position) {
        listItems.remove(position);
        adapter.notifyDataSetChanged();
    }

    private boolean validateExpertiseInput() {
        if (expertiseText.getText().length() == 0) {
            return false;
        }
        return true;
    }

    private boolean validateExpertise() {
        if (listItems.size() == 0) {
            expertiseText.setError("At least one expertise required");
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            picturePath = cursor.getString(columnIndex);
            cursor.close();
            ImageView imageView = (ImageView) findViewById(R.id.profilePicture);
            Bitmap bitmapSmall = ShrinkBitmap(picturePath, 130, 130);
            Bitmap bitmapBig = ShrinkBitmap(picturePath, 600, 600);
            imageView.setImageBitmap(bitmapBig);
            ByteArrayOutputStream byteArrayOutputStreamSmall = new ByteArrayOutputStream();
            bitmapSmall.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStreamSmall);
            ByteArrayOutputStream byteArrayOutputStreamBig = new ByteArrayOutputStream();
            bitmapBig.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStreamBig);
            byte[] byteArraySmall = byteArrayOutputStreamSmall.toByteArray();
            byte[] byteArrayBig = byteArrayOutputStreamBig.toByteArray();
            encodedImage = Base64.encodeToString(byteArraySmall, Base64.DEFAULT);
            encodedImageBig = Base64.encodeToString(byteArrayBig, Base64.DEFAULT);
        }
    }

    public Bitmap ShrinkBitmap(String file, int width, int height) {
        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(file, bmpFactoryOptions);

        int heightRatio = (int) Math.ceil(bmpFactoryOptions.outHeight / (float) height);
        int widthRatio = (int) Math.ceil(bmpFactoryOptions.outWidth / (float) width);

        if(heightRatio > 1 || widthRatio > 1)
        {
            if(heightRatio > widthRatio)
            {
                bmpFactoryOptions.inSampleSize = heightRatio;
            }
            else
            {
                bmpFactoryOptions.inSampleSize = widthRatio;
            }
        }

        bmpFactoryOptions.inJustDecodeBounds = false;
        bitmap = BitmapFactory.decodeFile(file, bmpFactoryOptions);
        return bitmap;
    }

    public void registerJSONRequest() {
        if (validateText()) {
            //create a jsonObject to send data to web api
            JSONObject jsonObject = createRegisterJsonObject();
            progressDialog.show();
            JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, REGISTER_URL_STRING, jsonObject,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                Boolean success = false;
                                String errorMessage = "";
                                if (validJSONObject("success", response)) {
                                    success = response.getBoolean("success");
                                    if (validJSONObject("message", response)) {
                                        errorMessage = response.getString("message");
                                        Log.e("server response", errorMessage);
                                    }
                                }
                                if (success) {
                                    progressDialog.hide();
                                    loginJSONRequest();
                                } else {
                                    progressDialog.hide();
                                    if (errorMessage.equals("Email already exists")) {
                                        emailText.setError("Email already used");
                                    }
                                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
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
                            //possibly check if succeeded
                            progressDialog.hide();
                            handleVolleyError(ex);
                        }
                    }
            );
            int socketTimeout = 15000;
            RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
            objectRequest.setRetryPolicy(policy);
            requestQueue.add(objectRequest);
        }
    }

    private void loginJSONRequest() {
        //validate email and password
        if (validateText()) {
            //create a jsonObject to send data to web api
            JSONObject jsonObject = createLoginJsonObject();
            progressDialog.show();
            JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, LOGIN_URL_STRING, jsonObject,
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
                                    startActivity(intent);
                                } else {
                                    progressDialog.hide();
                                    Toast.makeText(getApplicationContext(), "Login details incorrect", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
    }

    private void improveEditTextSelection() {
        emailTextLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focusEditText(emailText);
            }
        });
        passwordText1Layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focusEditText(passwordText1);
            }
        });
        passwordText2Layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focusEditText(passwordText2);
            }
        });
        firstnameTextLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focusEditText(firstnameText);
            }
        });
        lastnameTextLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focusEditText(lastnameText);
            }
        });
        coursenameTextLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focusEditText(coursenameText);
            }
        });
        yearofstudyTextLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focusEditText(yearofstudyText);
            }
        });
        contactnumberTextLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focusEditText(contactnumberText);
            }
        });
        expertiseLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                focusEditText(expertiseText);
            }
        });
    }

    private void focusEditText(EditText editText) {
        editText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    private void assignFonts() {
        Typeface Roboto_Regular = Typeface.createFromAsset(getAssets(), "Roboto-Regular.ttf");
        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(R.color.light_grey));
        emailText.setTypeface(Roboto_Regular);
        passwordText1.setTypeface(Roboto_Regular);
        passwordText2.setTypeface(Roboto_Regular);
        firstnameText.setTypeface(Roboto_Regular);
        lastnameText.setTypeface(Roboto_Regular);
        coursenameText.setTypeface(Roboto_Regular);
        yearofstudyText.setTypeface(Roboto_Regular);
        contactnumberText.setTypeface(Roboto_Regular);
        expertiseText.setTypeface(Roboto_Regular);
        passwordTextInput2Layout.setTypeface(Roboto_Regular);
        emailTextInputLayout.setTypeface(Roboto_Regular);
        passwordTextInput1Layout.setTypeface(Roboto_Regular);
        passwordTextInput2Layout.setTypeface(Roboto_Regular);
        firstnameTextInputLayout.setTypeface(Roboto_Regular);
        lastnameTextInputLayout.setTypeface(Roboto_Regular);
        coursenameTextInputLayout.setTypeface(Roboto_Regular);
        yearofstudyTextInputLayout.setTypeface(Roboto_Regular);
        contactnumberTextInputLayout.setTypeface(Roboto_Regular);
        expertiseTextInputLayout.setTypeface(Roboto_Regular);
    }

    private void loginPreferences(String email_address, String first_name, String last_name, String profile_picture, String user_status) {
        prefs.edit().putBoolean("user_logged_in", true).apply();
        prefs.edit().putString("email_address", email_address).apply();
        prefs.edit().putString("first_name", first_name).apply();
        prefs.edit().putString("last_name", last_name).apply();
        prefs.edit().putString("user_status", user_status).apply();
        prefs.edit().putString("profile_picture", profile_picture).apply();
    }

    private JSONObject createLoginJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("email_address", emailText.getText().toString());
            jsonObject.put("password", passwordText1.getText().toString());
        } catch (JSONException e) {
            if (e.getMessage() != null) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("JSONException", e.getMessage());
            }
        }
        return jsonObject;
    }

    private JSONObject createRegisterJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("email_address", emailText.getText().toString());
            jsonObject.put("password", passwordText1.getText().toString());
            jsonObject.put("first_name", firstnameText.getText().toString());
            jsonObject.put("last_name", lastnameText.getText().toString());
            jsonObject.put("year_of_study", yearofstudyText.getText().toString());
            jsonObject.put("course_name", coursenameText.getText().toString());
            jsonObject.put("contact_number", contactnumberText.getText().toString());
            JSONArray array = new JSONArray();
            for(int x = 0; x < listItems.size(); x++)
            {
                JSONObject object = new JSONObject();
                object.put("skill", listItems.get(x));
                array.put(object);
            }
            jsonObject.put("skills", array);
            if(encodedImage!=null)
                jsonObject.put("profile_picture", encodedImage);
            if(encodedImageBig!=null)
                jsonObject.put("profile_picture_big", encodedImageBig);
        } catch (JSONException e) {
            if (e.getMessage() != null) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("JSONException", e.getMessage());
            }
        }
        return jsonObject;
    }

    private void assignActionBar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Registration");
        //back button
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void assignViews() {
        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbarLayout);
        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        imageFab = (FloatingActionButton) findViewById(R.id.imageFab);
        emailText = (EditText) findViewById(R.id.emailText);
        emailText.clearFocus();
        passwordText1 = (EditText) findViewById(R.id.passwordText1);
        passwordText1.clearFocus();
        passwordText2 = (EditText) findViewById(R.id.passwordText2);
        passwordText2.clearFocus();
        firstnameText = (EditText) findViewById(R.id.firstnameText);
        firstnameText.clearFocus();
        lastnameText = (EditText) findViewById(R.id.lastnameText);
        lastnameText.clearFocus();
        coursenameText = (EditText) findViewById(R.id.courseText);
        coursenameText.clearFocus();
        yearofstudyText = (EditText) findViewById(R.id.yearofstudyText);
        yearofstudyText.clearFocus();
        contactnumberText = (EditText) findViewById(R.id.contactnumberText);
        contactnumberText.clearFocus();
        expertiseText = (EditText) findViewById(R.id.expertiseText);
        expertiseText.clearFocus();
        passwordTextInput2Layout = (TextInputLayout)findViewById(R.id.passwordTextInput2Layout);
        emailTextInputLayout  = (TextInputLayout)findViewById(R.id.emailTextInputLayout);
        passwordTextInput1Layout = (TextInputLayout)findViewById(R.id.passwordTextInput1Layout);
        passwordTextInput2Layout = (TextInputLayout)findViewById(R.id.passwordTextInput2Layout);
        firstnameTextInputLayout = (TextInputLayout)findViewById(R.id.firstnameTextInputLayout);
        lastnameTextInputLayout = (TextInputLayout)findViewById(R.id.lastnameTextInputLayout);
        coursenameTextInputLayout = (TextInputLayout)findViewById(R.id.coursenameTextInputLayout);
        yearofstudyTextInputLayout = (TextInputLayout)findViewById(R.id.yearofstudyTextInputLayout);
        contactnumberTextInputLayout = (TextInputLayout)findViewById(R.id.contactnumberTextInputLayout);
        expertiseTextInputLayout = (TextInputLayout)findViewById(R.id.expertiseTextInputLayout);
        expertiseButton = (Button) findViewById(R.id.expertiseButton);
        emailText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    appBarLayout.setExpanded(false, true);
                }
            }
        });
        emailText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.length() > 0)
                    emailText.setAlpha(1.00f);
                else
                    emailText.setAlpha(0.54f);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        passwordText1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    appBarLayout.setExpanded(false, true);
                }
            }
        });
        passwordText1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.length() > 0)
                    passwordText1.setAlpha(1.00f);
                else
                    passwordText1.setAlpha(0.54f);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        passwordText2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    appBarLayout.setExpanded(false, true);
                }
            }
        });
        passwordText2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.length() > 0)
                    passwordText2.setAlpha(1.00f);
                else
                    passwordText2.setAlpha(0.54f);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        firstnameText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    appBarLayout.setExpanded(false, true);
                }
            }
        });
        firstnameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.length() > 0)
                    firstnameText.setAlpha(1.00f);
                else
                    firstnameText.setAlpha(0.54f);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        lastnameText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    appBarLayout.setExpanded(false, true);
                }
            }
        });
        lastnameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.length() > 0)
                    lastnameText.setAlpha(1.00f);
                else
                    lastnameText.setAlpha(0.54f);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        coursenameText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    appBarLayout.setExpanded(false, true);
                }
            }
        });
        coursenameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.length() > 0)
                    coursenameText.setAlpha(1.00f);
                else
                    coursenameText.setAlpha(0.54f);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        yearofstudyText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    appBarLayout.setExpanded(false, true);
                }
            }
        });
        yearofstudyText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.length() > 0)
                    yearofstudyText.setAlpha(1.00f);
                else
                    yearofstudyText.setAlpha(0.54f);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        contactnumberText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    appBarLayout.setExpanded(false, true);
                }
            }
        });
        contactnumberText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.length() > 0)
                    contactnumberText.setAlpha(1.00f);
                else
                    contactnumberText.setAlpha(0.54f);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        expertiseText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    appBarLayout.setExpanded(false, true);
                }
            }
        });
        expertiseText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.length() > 0)
                    expertiseText.setAlpha(1.00f);
                else
                    expertiseText.setAlpha(0.54f);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        emailTextLayout = (LinearLayout) findViewById(R.id.emailTextLayout);
        passwordText1Layout = (LinearLayout) findViewById(R.id.passwordText1Layout);
        passwordText2Layout = (LinearLayout) findViewById(R.id.passwordText2Layout);
        firstnameTextLayout = (LinearLayout) findViewById(R.id.firstnameTextLayout);
        lastnameTextLayout = (LinearLayout) findViewById(R.id.lastnameTextLayout);
        coursenameTextLayout = (LinearLayout) findViewById(R.id.courseTextLayout);
        yearofstudyTextLayout = (LinearLayout) findViewById(R.id.yearofstudyTextLayout);
        contactnumberTextLayout = (LinearLayout) findViewById(R.id.contactnumberTextLayout);
        expertiseLayout = (LinearLayout) findViewById(R.id.expertiseTextLayout);
    }

    private Boolean validJSONObject(String key, JSONObject jsonObject) {
        if (!jsonObject.isNull(key) && jsonObject.has(key)) {
            return true;
        } else {
            Toast.makeText(getApplicationContext(), "Invalid JSONObject", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private Boolean validateText() {
        Boolean validateEmail = validateEmail();
        Boolean validatePasswords = validatePasswords();
        Boolean validateFirstname = validateFirstname();
        Boolean validateLastname = validateLastname();
        Boolean validateCoursename = validateCoursename();
        Boolean validateYearofstudy = validateYearofstudy();
        Boolean validateContactnumber = validateContactnumber();
        Boolean validateExpertise = validateExpertise();

        if (validateEmail && validatePasswords && validateFirstname && validateLastname && validateCoursename && validateYearofstudy && validateContactnumber&& validateExpertise) {
            return true;
        }
        return false;
    }

    private Boolean validateFirstname() {
        if (firstnameText.getText().length() == 0) {
            firstnameText.setError("First name is required");
            return false;
        }
        return true;
    }

    private Boolean validateLastname() {
        if (lastnameText.getText().length() == 0) {
            lastnameText.setError("Last name is required");
            return false;
        }
        return true;
    }

    private Boolean validateCoursename() {
        if (coursenameText.getText().length() == 0) {
            coursenameText.setError("Course name is required");
            return false;
        }
        return true;
    }

    private Boolean validateYearofstudy() {
        if (yearofstudyText.getText().length() == 0) {
            yearofstudyText.setError("Year of study is required");
            return false;
        }
        return true;
    }

    private Boolean validateContactnumber() {
        /*if (contactnumberText.getText().length() == 0) {
            contactnumberText.setError("Contact number is required");
            return false;
        }*/
        return true;
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

    private Boolean validatePasswords() {
        if ((passwordText1.getText().length() == 0) && (passwordText2.getText().length() == 0)) {
            passwordText1.setError("Password is required");
            passwordText2.setError("Password is required");
            return false;
        } else {
            if (!passwordText1.getText().toString().equals(passwordText2.getText().toString())) {
                passwordText2.setError("Passwords don't match");
                return false;
            } else {
                return true;
            }
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
        finish();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_register, menu);
        MenuItem item = menu.findItem(R.id.doneButton);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (isNetworkAvailable()) {
                    registerJSONRequest();
                    return false;
                } else {
                    Toast.makeText(getApplicationContext(), "No internet connection detected", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
