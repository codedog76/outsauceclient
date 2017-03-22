package adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.outsauce.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import models.User;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Lucien on 4/12/2016.
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private LayoutInflater layoutInflater;
    private List<User> userList = Collections.emptyList();
    private Context context;
    private clickListener clickListener;
    private String query;

    public UserAdapter(Context context) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
    }

    public void setListener(clickListener listener) {
        this.clickListener = listener;
    }

    public void setUserList(ArrayList<User> userList, String query) {
        this.userList = userList;
        this.query = query;
        notifyDataSetChanged();
    }

    public void clearUserList()
    {
        this.userList.clear();
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.user_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        User user = userList.get(position);
        String base64 = user.getProfile_picture();
        if (base64 != null && !base64.equals("")) {
            byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            holder.profile_image.setImageBitmap(decodedByte);
        } else {
            holder.profile_image.setImageDrawable(context.getResources().getDrawable(R.drawable.profile_default));
        }
        String user_status = user.getUser_status();
        if(user_status.equals("Available"))
        {
            holder.profile_image.setBorderColor(context.getResources().getColor(R.color.green));
        }
        if(user_status.equals("Busy"))
        {
            holder.profile_image.setBorderColor(context.getResources().getColor(R.color.orange));
        }
        if(user_status.equals("Unavailable"))
        {
            holder.profile_image.setBorderColor(context.getResources().getColor(R.color.errorRed));
        }
        ArrayList<String> skills = user.getSkills();
        String skillsText = "";
        if(query!=null)
        {
            for(int x = 0; x < skills.size(); x++)
            {
                String skillTemp = skills.get(x);
                if(skillTemp.toLowerCase().equals(query.toLowerCase())&&query!=null)
                    skillTemp = "<b>"+skillTemp+"</b>";
                if(x!=(skills.size()-1))
                    skillsText += skillTemp+", ";
                else
                    skillsText += skillTemp;
            }
            String email_address = user.getEmail_address();
            if(email_address.toLowerCase().equals(query.toLowerCase())&&query!=null)
                email_address = "<b>"+email_address+"</b>";
            String first_name = user.getFirst_name();
            if(first_name.toLowerCase().equals(query.toLowerCase())&&query!=null)
                first_name = "<b>"+first_name+"</b>";
            String last_name = user.getLast_name();
            if(last_name.toLowerCase().equals(query.toLowerCase())&&query!=null)
                last_name = "<b>"+last_name+"</b>";
            holder.rowSkillsText.setText(Html.fromHtml(skillsText));
            holder.rowNameText.setText(Html.fromHtml(first_name + " " + last_name));
            holder.rowEmailText.setText(Html.fromHtml(email_address));
            holder.favouriteCountText.setText(user.getFavourite_count());
        }
        else
        {
            for(int x = 0; x < skills.size(); x++)
            {
                String skillTemp = skills.get(x);
                if(x!=(skills.size()-1))
                    skillsText += skillTemp+", ";
                else
                    skillsText += skillTemp;
            }
            String email_address = user.getEmail_address();
            String first_name = user.getFirst_name();
            String last_name = user.getLast_name();
            holder.rowSkillsText.setText(skillsText);
            holder.rowNameText.setText(first_name + " " + last_name);
            holder.rowEmailText.setText(email_address);
            holder.favouriteCountText.setText(user.getFavourite_count());
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView rowNameText, rowEmailText, rowSkillsText, favouriteCountText;
        private CircleImageView profile_image;

        public ViewHolder(View itemView) {
            super(itemView);
            profile_image = (CircleImageView) itemView.findViewById(R.id.profile_image);
            rowNameText = (TextView) itemView.findViewById(R.id.rowNameText);
            rowEmailText = (TextView) itemView.findViewById(R.id.rowEmailText);
            rowSkillsText = (TextView) itemView.findViewById(R.id.rowSkillsText);
            favouriteCountText = (TextView) itemView.findViewById(R.id.favouriteCountText);
            Typeface Roboto_Regular = Typeface.createFromAsset(context.getAssets(), "Roboto-Regular.ttf");
            rowEmailText.setTypeface(Roboto_Regular);
            rowEmailText.setTypeface(Roboto_Regular);
            rowSkillsText.setTypeface(Roboto_Regular);
            favouriteCountText.setTypeface(Roboto_Regular);
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
