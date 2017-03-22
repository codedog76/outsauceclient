package service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ArrayAdapter;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import activities.JobActivity;
import activities.MainActivity;
import fragments.JobOutboxFragment;
import models.Job;
import other.ObscuredSharedPreferences;
import other.VolleySingleton;

public class OutSauceService extends Service {
    //job inbox variables
    private static final String JOB_INBOX_FRAGMENT_URL_STRING = "http://opensouce.csdev.nmmu.ac.za/api/users/jobinbox"; //job inbox json url
    private String JOB_INBOX_FILENAME = "_INBOX_CACHE.srl";
    private final Handler jobInboxHandler = new Handler();

    //job outbox variables
    private static final String JOB_OUTBOX_FRAGMENT_URL_STRING = "http://opensouce.csdev.nmmu.ac.za/api/users/joboutbox"; //job outbox json url
    private String JOB_OUTBOX_FILENAME = "_OUTBOX_CACHE.srl";
    private final Handler jobOutboxHandler = new Handler();

    //general
    private SharedPreferences prefs;
    private RequestQueue requestQueue;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
    private IBinder mBinder = new LocalBinder();
    LocalBroadcastManager broadcaster;

    private int REFRESH_INTERVAL = 20 * 1000;

    //current user data
    private String user_email_address;
    private Boolean user_logged_in;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getUserDataFromPrefs();
        requestQueue = VolleySingleton.getInstance(this.getApplicationContext()).getRequestQueue();
        startJobInboxThread();
        startJobOutboxThread();
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        broadcaster = LocalBroadcastManager.getInstance(this);
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public OutSauceService getServerInstance() {
            return OutSauceService.this;
        }
    }

    private void getUserDataFromPrefs() {
        prefs = new ObscuredSharedPreferences(this, this.getSharedPreferences("SECURE", Context.MODE_PRIVATE));
        user_logged_in = prefs.getBoolean("user_logged_in", false);
        if (user_logged_in) {
            user_email_address = prefs.getString("email_address", null);
        }
    }

    public String getJobInboxLastRefreshed() {
        String user_last_refreshed_job_inbox = prefs.getString("last_refreshed_job_inbox", null);
        if (user_last_refreshed_job_inbox == null) {
            String string = "2000-01-01 00:00:00";
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            try {
                prefs.edit().putString("last_refreshed_job_inbox", sdf.format(format.parse(string))).apply();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            user_last_refreshed_job_inbox = prefs.getString("last_refreshed_job_inbox", null);
        }
        return user_last_refreshed_job_inbox;
    }

    public String getJobOutboxLastRefreshed() {
        String user_last_refreshed_job_outbox = prefs.getString("last_refreshed_job_outbox", null);
        if (user_last_refreshed_job_outbox == null) {
            String string = "2000-01-01 00:00:00";
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            try {
                prefs.edit().putString("last_refreshed_job_outbox", sdf.format(format.parse(string))).apply();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            user_last_refreshed_job_outbox = prefs.getString("last_refreshed_job_outbox", null);
        }
        return user_last_refreshed_job_outbox;
    }

    public void setJobInboxDataPrefs() {
        String user_last_refreshed_job_inbox = sdf.format(new Date());
        prefs.edit().putString("last_refreshed_job_inbox", user_last_refreshed_job_inbox).apply();
    }

    public void setJobOutboxDataPrefs() {
        String user_last_refreshed_job_outbox = sdf.format(new Date());
        prefs.edit().putString("last_refreshed_job_outbox", user_last_refreshed_job_outbox).apply();
    }

    private void startJobInboxThread() {
        jobInboxHandler.removeCallbacks(jobInboxRunnable);
        jobInboxHandler.postDelayed(jobInboxRunnable, 1000); // 1 second
    }

    private void startJobOutboxThread() {
        jobOutboxHandler.removeCallbacks(jobOutboxRunnable);
        jobOutboxHandler.postDelayed(jobOutboxRunnable, 1000); // 1 second
    }

    private Runnable jobInboxRunnable = new Runnable() {
        @Override
        public void run() {
            if (user_logged_in) {
                jobInboxFragmentJSONRequest();
            } else {
                Log.e("jobInboxRunnable", "not logged in");
            }
            jobInboxHandler.postDelayed(this, REFRESH_INTERVAL); //20 seconds
        }
    };

    private Runnable jobOutboxRunnable = new Runnable() {
        @Override
        public void run() {
            if (user_logged_in) {
                jobOutboxFragmentJSONRequest();
            } else {
                Log.e("jobOutboxRunnable", "not logged in");
            }
            jobOutboxHandler.postDelayed(this, REFRESH_INTERVAL); //20 seconds
        }
    };

    private ArrayList<Job> readJobInboxFromCache() {
        ArrayList<Job> returnList = new ArrayList<>();
        ObjectInputStream input;
        try {
            input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(), "") + File.separator + user_email_address + JOB_INBOX_FILENAME)));
            returnList = (ArrayList<Job>) input.readObject();
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
    }

    private ArrayList<Job> readJobOutboxFromCache() {
        ArrayList<Job> returnList = new ArrayList<>();
        ObjectInputStream input;
        try {
            input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(), "") + File.separator + user_email_address + JOB_OUTBOX_FILENAME)));
            returnList = (ArrayList<Job>) input.readObject();
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
    }

    private void writeJobInboxToCache(ArrayList<Job> jobs) {
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), "") + File.separator + user_email_address + JOB_INBOX_FILENAME, false));
            out.writeObject(jobs);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeJobOutboxToCache(ArrayList<Job> jobs) {
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(new File(getFilesDir(), "") + File.separator + user_email_address + JOB_OUTBOX_FILENAME, false));
            out.writeObject(jobs);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void jobInboxFragmentJSONRequest() {
        JSONObject jsonObject = jobInboxFragmentCreateJSONObject();
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, JOB_INBOX_FRAGMENT_URL_STRING, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (validJSONObject("Jobs", response)) {
                            jobInboxParseJSONResponse(response);
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

    private JSONObject jobInboxFragmentCreateJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (user_email_address != null)
                jsonObject.put("email_address", user_email_address);
            String user_last_refreshed_job_inbox = getJobInboxLastRefreshed();
            if (user_last_refreshed_job_inbox != null)
                jsonObject.put("date_last_refreshed", user_last_refreshed_job_inbox);
        } catch (JSONException e) {
            if (e.getMessage() != null) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("JSONException", e.getMessage());
            }
        }
        return jsonObject;
    }

    private void jobInboxParseJSONResponse(JSONObject response) {
        try {
            ArrayList<Job> parsedList = new ArrayList<>();
            JSONArray jsonArray = response.getJSONArray("Jobs");
            for (int x = 0; x < jsonArray.length(); x++) {
                JSONObject jsonObject = jsonArray.getJSONObject(x);
                String email_address = jsonObject.getString("email_address_sender");
                String first_name = jsonObject.getString("sender_first_name");
                String last_name = jsonObject.getString("sender_last_name");
                String job_id = jsonObject.getString("job_id");
                String job_title = jsonObject.getString("job_title");
                String job_description = jsonObject.getString("job_description");
                String job_status = jsonObject.getString("job_status");
                String profile_picture = jsonObject.getString("profile_picture");
                String job_due_date = jsonObject.getString("job_due_date");
                String job_date_created = jsonObject.getString("job_date_created");
                String job_date_modified = jsonObject.getString("job_date_modified");
                parsedList.add(new Job(email_address, first_name, last_name, job_id, job_title, job_description, job_status, profile_picture, job_due_date, job_date_created, job_date_modified));
            }
            compareInboxData(parsedList);
        } catch (JSONException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("JSONException", e.getMessage());
        }
    }

    private void jobOutboxFragmentJSONRequest() {
        JSONObject jsonObject = jobOutboxFragmentCreateJSONObject();
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.POST, JOB_OUTBOX_FRAGMENT_URL_STRING, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (validJSONObject("Jobs", response)) {
                            jobOutboxParseJSONResponse(response);
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
    } //done

    private JSONObject jobOutboxFragmentCreateJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (user_email_address != null)
                jsonObject.put("email_address", user_email_address);
            String user_last_refreshed_job_inbox = getJobOutboxLastRefreshed();
            if (user_last_refreshed_job_inbox != null)
                jsonObject.put("date_last_refreshed", user_last_refreshed_job_inbox);
        } catch (JSONException e) {
            if (e.getMessage() != null) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("JSONException", e.getMessage());
            }
        }
        return jsonObject;
    } //done

    private void jobOutboxParseJSONResponse(JSONObject response) {
        try {
            ArrayList<Job> parsedList = new ArrayList<>();
            JSONArray jsonArray = response.getJSONArray("Jobs");
            for (int x = 0; x < jsonArray.length(); x++) {
                JSONObject jsonObject = jsonArray.getJSONObject(x);
                String email_address = jsonObject.getString("email_address_receiver");
                String first_name = jsonObject.getString("receiver_first_name");
                String last_name = jsonObject.getString("receiver_last_name");
                String job_id = jsonObject.getString("job_id");
                String job_title = jsonObject.getString("job_title");
                String job_description = jsonObject.getString("job_description");
                String job_status = jsonObject.getString("job_status");
                String profile_picture = jsonObject.getString("profile_picture");
                String job_due_date = jsonObject.getString("job_due_date");
                String job_date_created = jsonObject.getString("job_date_created");
                String job_date_modified = jsonObject.getString("job_date_modified");
                parsedList.add(new Job(email_address, first_name, last_name, job_id, job_title, job_description, job_status, profile_picture, job_due_date, job_date_created, job_date_modified));
            }
            compareOutboxData(parsedList);
        } catch (JSONException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("JSONException", e.getMessage());
        }
    } //done

    private void compareInboxData(ArrayList<Job> externalList) {
        ArrayList<Job> localList = readJobInboxFromCache();
        Boolean changeMade = false;
        if (localList != null && externalList != null) {
            for (Job jobExternal : externalList) {
                Job jobLocal = compareJobId(jobExternal.getJob_id(), localList);
                if (jobLocal == null) {
                    localList.add(jobExternal);
                    changeMade = true;
                    newJobInboxNotification(jobExternal);
                } else {
                    if (!compareJobStatus(jobLocal, jobExternal)) {
                        localList = replaceLocalList(localList, jobExternal);
                        changeMade = true;
                    }
                }
            }
            if (changeMade) {
                writeJobInboxToCache(localList);
                addAllJobInboxView(localList);
            }
            setJobInboxDataPrefs();
        }
    }

    private void compareOutboxData(ArrayList<Job> externalList) {
        ArrayList<Job> localList = readJobOutboxFromCache();
        Boolean changeMade = false;
        if (localList != null && externalList != null) {
            for (Job jobExternal : externalList) {
                Job jobLocal = compareJobId(jobExternal.getJob_id(), localList);
                if (jobLocal == null) {
                    localList.add(jobExternal);
                    changeMade = true;
                } else {
                    if (!compareJobStatus(jobLocal, jobExternal)) {
                        localList = replaceLocalList(localList, jobExternal);
                        changeMade = true;
                        changedJobInboxNotification(jobExternal);
                    }
                }
            }
            if (changeMade) {
                writeJobOutboxToCache(localList);
                addAllJobOutboxView(localList);
            }
            setJobOutboxDataPrefs();
        }
    }

    private ArrayList<Job> replaceLocalList(ArrayList<Job> localList, Job jobExternal) {
        for (int x = 0; x < localList.size(); x++) {
            Job jobLocal = localList.get(x);
            if (jobExternal.getJob_id().equals(jobLocal.getJob_id())) {
                localList.set(x, jobExternal);
            }
        }
        return localList;
    }

    public void readFromDiskJobInbox() {
        addAllJobInboxView(readJobInboxFromCache());
    }

    public void readFromDatabaseJobInbox() {
        jobInboxFragmentJSONRequest();
    }

    public void readFromDiskJobOutbox() {
        addAllJobOutboxView(readJobOutboxFromCache());
    }

    public void readFromDatabaseJobOutbox() {
        jobOutboxFragmentJSONRequest();
    }

    public void addAllJobInboxView(ArrayList<Job> jobs) {
        Intent intent = new Intent("addAllInboxReceiver");
        intent.putExtra("jobInboxList", jobs);
        if (broadcaster != null)
            broadcaster.sendBroadcast(intent);
    }

    public void addAllJobOutboxView(ArrayList<Job> jobs) {
        Intent intent = new Intent("addAllOutboxReceiver");
        intent.putExtra("jobOutboxList", jobs);
        if (broadcaster != null)
            broadcaster.sendBroadcast(intent);
    }

    private void newJobInboxNotification(Job jobExternal) {
        Intent intent = new Intent(this, JobActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Bundle b = new Bundle();
        b.putBoolean("notification", true);
        b.putString("from", "inbox");
        b.putSerializable("job", jobExternal);
        intent.putExtras(b);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notification.setSound(alarmSound);
        notification.setTicker("New Job Received");
        notification.setSmallIcon(android.R.drawable.ic_menu_report_image);
        notification.setContentTitle(jobExternal.getJob_title());
        notification.setContentText(jobExternal.getJob_description());
        notification.setContentIntent(pi);
        notification.setGroup("New Job");
        notification.setAutoCancel(true);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification.build());
    }

    private void changedJobInboxNotification(Job jobExternal) {
        Intent intent = new Intent(this, JobActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Bundle b = new Bundle();
        b.putBoolean("notification", true);
        b.putString("from", "outbox");
        b.putSerializable("job", jobExternal);
        intent.putExtras(b);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notification.setSound(alarmSound);
        notification.setTicker("Job was updated");
        notification.setSmallIcon(android.R.drawable.ic_menu_report_image);
        notification.setContentTitle(jobExternal.getJob_title());
        String job_status = jobExternal.getJob_status();
        if (job_status.equals("Accepted") || job_status.equals("Declined")) {
            notification.setContentText(jobExternal.getFirst_name() + " " + jobExternal.getLast_name() + " has " + job_status + " this job");
        }
        if (job_status.equals("Done")) {
            notification.setContentText(jobExternal.getFirst_name() + " " + jobExternal.getLast_name() + " has marked this job as done");
        }
        notification.setContentIntent(pi);
        notification.setGroup("Changed Job");
        notification.setAutoCancel(true);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification.build());
    }

    private Boolean compareJobStatus(Job jobLocal, Job jobExternal) {
        return jobLocal.getJob_status().equals(jobExternal.getJob_status());
    }

    private Job compareJobId(String job_id_external, ArrayList<Job> localList) {
        for (Job jobLocal : localList) {
            if (jobLocal.getJob_id().equals(job_id_external)) {
                return jobLocal;
            }
        }
        return null;
    }

    private Boolean validJSONObject(String key, JSONObject jsonObject) {
        return !jsonObject.isNull(key) && jsonObject.has(key);
    }

    private void handleVolleyError(VolleyError ex) {
        if (ex instanceof TimeoutError || ex instanceof NoConnectionError) {
            Log.e("VolleyError", "No internet access or server is down.");
        } else if (ex instanceof AuthFailureError) {
            Log.e("VolleyError", "Authentication error");
        } else if (ex instanceof ServerError) {
            Log.e("VolleyError", "Server error");
        } else if (ex instanceof NetworkError) {
            Log.e("VolleyError", "Network error");
        } else if (ex instanceof ParseError) {
            Log.e("VolleyError", "Parse error");
        }
        if (ex.getMessage() != null)
            Log.e("VolleyError", ex.getMessage());
    }
}
