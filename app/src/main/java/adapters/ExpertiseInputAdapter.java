package adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.outsauce.R;

import java.util.ArrayList;

public class ExpertiseInputAdapter extends ArrayAdapter {

    private clickListener clickListener;
    private Context context;

    public ExpertiseInputAdapter(Context context, ArrayList<String> list) {
        super(context, 0, list);
        this.context = context;
    }

    public void setListener(clickListener listener) {
        this.clickListener = listener;
    }

    @Override
         public View getView(final int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        String text = (String)getItem(position);
        Log.e("expertiseAdapter", text);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.expertise_row_input, parent, false);
        }
        // Lookup view for data population
        TextView expertiseText = (TextView) convertView.findViewById(R.id.expertiseText);
        Typeface Roboto_Regular = Typeface.createFromAsset(context.getAssets(), "Roboto-Regular.ttf");
        expertiseText.setTypeface(Roboto_Regular);
        Button removeButton = (Button) convertView.findViewById(R.id.removeButton);
        // Populate the data into the template view using the data object
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.itemClicked(position);
            }
        });
        expertiseText.setText(text);
        // Return the completed view to render on screen
        return convertView;
    }

    public interface clickListener {
        void itemClicked(int position);
    }
}
