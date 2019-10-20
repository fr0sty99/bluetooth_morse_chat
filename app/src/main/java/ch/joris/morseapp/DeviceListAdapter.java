package ch.joris.morseapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceListAdapter extends ArrayAdapter<DeviceListItem> {
    private Context context;
    private int resourceId;

    static class ViewHolder{
        TextView name;
        TextView address;
    }

    public DeviceListAdapter(Context context, int resource, ArrayList<DeviceListItem> object) {
        super(context, resource, object);
        this.context = context;
        this.resourceId = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        String deviceName = getItem(position).getDeviceName();
        String deviceAddress = getItem(position).getDeviceAddress();

        DeviceListItem listItem = new DeviceListItem(deviceName, deviceAddress);

        final View result;
        ViewHolder holder;

        if(convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(resourceId, parent, false);
            holder = new ViewHolder();

            holder.address = convertView.findViewById(R.id.textView_deviceAddress);
            holder.name = convertView.findViewById(R.id.textView_deviceName);

            result = convertView;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
            result = convertView;
        }

        // TODO:
        //  Animation animation = AnimationUtils.loadAnimation(context, )
        //  result.startAnimation();

        holder.address.setText(listItem.getDeviceAddress());
        holder.name.setText(listItem.getDeviceName());

        return convertView;
    }
}
