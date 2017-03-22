package adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.outsauce.R;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import models.Job;

/**
 * Created by Lucien on 4/25/2016.
 */
public class JobAdapter extends RecyclerView.Adapter<JobAdapter.ViewHolder> {

    private LayoutInflater layoutInflater;
    private ArrayList<Job> jobList;
    private Context context;
    private clickListener clickListener;

    public JobAdapter(Context context) {
        this.jobList = new ArrayList<>();
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
    }

    public void setListener(clickListener listener) {
        this.clickListener = listener;
    }

    public void setJobList(ArrayList<Job> jobList) {
        if(!jobList.isEmpty())
            this.jobList = jobList;
        else
            this.jobList = new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.job_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Job job = jobList.get(position);
        String base64 = job.getProfile_picture();
        if (base64 != null && !base64.equals("")) {
            byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            holder.profile_image.setImageBitmap(decodedByte);
        } else {
            holder.profile_image.setImageDrawable(context.getResources().getDrawable(R.drawable.profile_default));
        }
        String first_name = job.getFirst_name();
        String last_name = job.getLast_name();
        String job_title = job.getJob_title();
        String job_description = job.getJob_description();
        String job_status = job.getJob_status();

        String job_due_date = job.getJob_date_created();
        DateFormat incoming_format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss aa", Locale.ENGLISH);
        SimpleDateFormat date_format = new SimpleDateFormat("dd MMM", Locale.ENGLISH);
        try {
            Date date = incoming_format.parse(job_due_date);
            holder.jobRowDate.setText(date_format.format(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        holder.rowNameText.setText(first_name + " " + last_name);
        holder.rowTitleText.setText(job_title);
        holder.rowDescriptionText.setText(job_description);
        holder.statusResultText.setText(job_status);

        if (job_status.equals("Pending")) {
            holder.statusResultText.setTextColor(context.getResources().getColor(R.color.orange));
        }
        if (job_status.equals("Declined")) {
            holder.statusResultText.setTextColor(context.getResources().getColor(R.color.errorRed));
        }
        if (job_status.equals("Accepted")) {
            holder.statusResultText.setTextColor(context.getResources().getColor(R.color.green));
        }
        if (job_status.equals("Done")) {
            holder.statusResultText.setTextColor(context.getResources().getColor(R.color.darkgrey));
        }
    }

    @Override
    public int getItemCount() {
        //Log.e("JobAdapter", "jobListSize: "+ jobList.size());
        return jobList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView rowNameText, rowTitleText, rowDescriptionText, statusResultText, jobRowDate;
        private ImageView profile_image;

        public ViewHolder(View itemView) {
            super(itemView);
            profile_image = (ImageView) itemView.findViewById(R.id.profile_image);
            rowNameText = (TextView) itemView.findViewById(R.id.rowNameText);
            rowTitleText = (TextView) itemView.findViewById(R.id.rowTitleText);
            statusResultText = (TextView) itemView.findViewById(R.id.statusResultText);
            rowDescriptionText = (TextView) itemView.findViewById(R.id.rowDescriptionText);
            jobRowDate = (TextView) itemView.findViewById(R.id.jobRowDate);
            Typeface Roboto_Regular = Typeface.createFromAsset(context.getAssets(), "Roboto-Regular.ttf");
            rowNameText.setTypeface(Roboto_Regular);
            rowTitleText.setTypeface(Roboto_Regular);
            rowDescriptionText.setTypeface(Roboto_Regular);
            statusResultText.setTypeface(Roboto_Regular);
            jobRowDate.setTypeface(Roboto_Regular);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (clickListener != null) {
                clickListener.itemClicked(getPosition());
            }
        }
    }

    public interface clickListener {
        void itemClicked(int position);
    }
}
