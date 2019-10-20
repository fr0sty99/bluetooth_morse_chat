package ch.joris.morseapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MessageArrayAdapter extends ArrayAdapter<MessageListItem> {
    private Context context;
    private int resourceId;

    static class ViewHolder {
        TextView id;
        TextView time;
    }

    public MessageArrayAdapter(@NonNull Context context, int resource) {
        super(context, resource);
        this.context = context;
        this.resourceId = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        String messageId = getItem(position).getId();
        String messageTime = getItem(position).getMessageTime();

        DeviceListItem listItem = new DeviceListItem(messageId, messageTime);

        final View result;
        MessageArrayAdapter.ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(resourceId, parent, false);
            holder = new MessageArrayAdapter.ViewHolder();

            holder.id = convertView.findViewById(R.id.textView_deviceAddress);
            holder.time = convertView.findViewById(R.id.textView_deviceName);

            result = convertView;
            convertView.setTag(holder);
        } else {
            holder = (MessageArrayAdapter.ViewHolder) convertView.getTag();
            result = convertView;
        }

        // TODO:
        //  Animation animation = AnimationUtils.loadAnimation(context, )
        //  result.startAnimation();

        holder.id.setText(listItem.getDeviceAddress());
        holder.time.setText(listItem.getDeviceName());

        return convertView;
    }
}
