package models;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class Job implements Serializable {
    private String email_address, first_name, last_name, job_title, job_description, job_status, profile_picture, job_id, job_due_date, job_date_created, job_date_modified;

    public Job(String email_address, String first_name, String last_name, String job_id, String job_title, String job_description,
               String job_status, String profile_picture, String job_due_date, String job_date_created, String job_date_modified)
    {
        this.email_address = email_address;
        this.first_name = first_name;
        this.last_name = last_name;
        this.job_id = job_id;
        this.job_title = job_title;
        this.job_description = job_description;
        this.job_status = job_status;
        this.profile_picture = profile_picture;
        this.job_due_date = job_due_date;
        this.job_date_created = job_date_created;
        this.job_date_modified = job_date_modified;
    }

    public String getEmail_address() { return this.email_address; }

    public String getFirst_name() { return this.first_name; }

    public String getLast_name() { return this.last_name; }

    public String getJob_id() { return this.job_id; }

    public String getJob_title() { return this.job_title; }

    public String getJob_description() { return this.job_description; }

    public String getJob_status() { return this.job_status; }

    public String getProfile_picture() { return this.profile_picture; }

    public String getJob_due_date() { return this.job_due_date; }

    public String getJob_date_created() { return this.job_date_created; }

    public String getJob_date_modified() { return this.job_date_modified; }
}
