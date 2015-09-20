package ca.davidvuong.atise;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;

public class UsersAdapter extends ArrayAdapter<Row> {
    public UsersAdapter(Context context, ArrayList<Row> rows) {
        super(context, 0, rows);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Row row = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item, parent, false);
        }
        // Lookup view for data population
        TextView item_name = (TextView) convertView.findViewById(R.id.item_name);
        TextView item_price = (TextView) convertView.findViewById(R.id.item_price);
        ImageView item_pic = (ImageView) convertView.findViewById(R.id.Image);
        // Populate the data into the template view using the data object
        item_name.setText(row.name);
        item_price.setText(row.price);
        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.displayImage(row.url,item_pic);

        // Return the completed view to render on screen
        return convertView;
    }
}