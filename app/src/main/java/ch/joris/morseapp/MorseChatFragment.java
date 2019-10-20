package ch.joris.morseapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import ch.joris.morseapp.Bluetooth.BluetoothService;
import ch.joris.morseapp.Bluetooth.Constants;

import static ch.joris.morseapp.Bluetooth.Constants.STATE_CONNECTED;
import static ch.joris.morseapp.Bluetooth.Constants.STATE_CONNECTING;
import static ch.joris.morseapp.Bluetooth.Constants.STATE_LISTEN;
import static ch.joris.morseapp.Bluetooth.Constants.STATE_NONE;

public class MorseChatFragment extends Fragment {
    private BluetoothService chatService;
    private Vibrator vibrator;
    private String deviceAddress;
    private BluetoothAdapter btAdapter;
    private ListView sentMessagesListView;
    private ListView receivedMessagesListView;
    private TextView chatTitleTextView;
    private MessageArrayAdapter receivedMessagesArrayAdapter;
    private MessageArrayAdapter sentMessagesArrayAdapter;
    private String connectedDeviceName;

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        bundle = getArguments();

        vibrator = (Vibrator) getActivity().getSystemService(Service.VIBRATOR_SERVICE);

        deviceAddress = bundle.getString("device_address");
        BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);

        chatService = new BluetoothService(handler);
        chatService.connect(device);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View parent =  inflater.inflate(R.layout.fragment_conversationview, container, false);

        sentMessagesListView = parent.findViewById(R.id.sentMessagesListView);
        sentMessagesListView.setAdapter(sentMessagesArrayAdapter);
        receivedMessagesListView = parent.findViewById(R.id.receivedMessagesListView);
        receivedMessagesListView.setAdapter(receivedMessagesArrayAdapter);
        chatTitleTextView = parent.findViewById(R.id.chatTitleTextView);

        return parent;
    }


    // The Handler that gets information back from the BluetoothChatService
    // TODO: check out what a handlerleak is and understand how this handler gets data from the service
    @SuppressLint("HandlerLeak")
    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Activity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case STATE_CONNECTED:
                            setChatTitleTextView("Connected To: " + connectedDeviceName);
                            receivedMessagesArrayAdapter.clear();
                            sentMessagesArrayAdapter.clear();
                            break;
                        case STATE_CONNECTING:
                            setChatTitleTextView("Connecting");
                            break;
                        case STATE_LISTEN:
                        case STATE_NONE:
                            setChatTitleTextView("Not Connected");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuffer = (byte[]) msg.obj;
                    // construct a MorsePattern from the buffer
                    MorsePattern writePattern = deserializeMorsePattern(writeBuffer);
                    conversationArrayAdapter.add(String.valueOf(patternList.indexOf(writePattern)));
                    patternList.add(writePattern);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a MorsePattern from the bytes in the buffer
                    MorsePattern pattern = deserializeMorsePattern(readBuf);
                    if (!patternList.contains(pattern)) {
                        patternList.add(pattern);
                    }
                    conversationArrayAdapter.add(String.valueOf(patternList.indexOf(pattern) + 1)); // + 1 cuz its an index
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    connectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to " + connectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    // Updates the status on the action bar.
    private void setChatTitleTextView(String title) {
        chatTitleTextView.setText(title);
    }

    // convert MorsePattern to ByteArray
    public byte[] serializeMorsePattern(MorsePattern pattern) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(pattern.getPattern().size() * 8); // 1 long takes 8 bytes
        LongBuffer longBuffer = byteBuffer.asLongBuffer();
        longBuffer.clear();

        ArrayList<Long> patternList = pattern.getPattern();
        long[] data = new long[patternList.size()];
        for(int i = 0; i < patternList.size(); i++)  {
            data[i] = patternList.get(i);
        }

        longBuffer.put(data);

        return byteBuffer.array();
    }

    // convert Byte Array to long Array
    private static long[] toLongArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        LongBuffer lb = buffer.asLongBuffer();

        long[] longArray = new long[lb.limit()];
        lb.get(longArray);

        return longArray;
    }

    // convert ByteArray to MorsePattern
    public MorsePattern deserializeMorsePattern(byte[] bytes) {
        long[] patternArray = toLongArray(bytes);
        ArrayList<Long> patternList = new ArrayList<>();
        for (long l : patternArray) {
            patternList.add(l);
        }
        return new MorsePattern(patternList);
    }

}
