package ch.joris.morseapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;

public class MessageArrayAdapter extends ArrayAdapter<MessageListItem> {

    public MessageArrayAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }
}
