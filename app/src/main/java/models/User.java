package models;

import java.io.Serializable;
import java.util.ArrayList;

public class User implements Serializable {
    private String email_address, first_name, last_name, profile_picture, favourite_count, user_status;
    private ArrayList<String> skills;

    public User(String email_address, String first_name, String last_name, String profile_picture, ArrayList<String> skills, String favourite_count, String user_status)
    {
        this.email_address = email_address;
        this.first_name = first_name;
        this.last_name = last_name;
        this.profile_picture = profile_picture;
        this.skills = skills;
        this.favourite_count = favourite_count;
        this.user_status = user_status;
    }

    public String getFirst_name() {
        return first_name;
    }

    public String getLast_name() {
        return this.last_name;
    }

    public String getEmail_address() {
        return this.email_address;
    }

    public String getProfile_picture() { return this.profile_picture; }

    public ArrayList<String> getSkills() { return this.skills; }

    public String getFavourite_count() { return this.favourite_count; }

    public String getUser_status() { return this.user_status; }
}
