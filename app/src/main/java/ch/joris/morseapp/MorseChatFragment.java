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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import ch.joris.morseapp.Bluetooth.BluetoothService;
import ch.joris.morseapp.Bluetooth.Constants;

import static ch.joris.morseapp.Bluetooth.Constants.STATE_CONNECTED;
import static ch.joris.morseapp.Bluetooth.Constants.STATE_CONNECTING;
import static ch.joris.morseapp.Bluetooth.Constants.STATE_LISTEN;
import static ch.joris.morseapp.Bluetooth.Constants.STATE_NONE;

public class MorseChatFragment extends Fragment implements View.OnClickListener, View.OnTouchListener {
    private BluetoothService chatService;
    private String deviceAddress;
    private BluetoothAdapter btAdapter;

    private ListView sentMessagesListView;
    private ListView receivedMessagesListView;
    private TextView chatTitleTextView;
    private Button sendButton;
    private Button recordButton;
    private Button morseButton;

    // Morse
    private Vibrator vibrator;
    private Boolean recording = false;
    private int patternStepCount = 0;
    private int id = 0;
    private long startVibrate;
    private long afterVibrate;
    private ArrayList<Long> currentPatternList = new ArrayList<>();
    private ArrayList<MorsePattern> patternList = new ArrayList<>();
    private MorsePattern tmpPattern;

    private MessageArrayAdapter receivedMessagesArrayAdapter;
    private MessageArrayAdapter sentMessagesArrayAdapter;
    private String connectedDeviceName;

    @Override
    public void onStart() {
        super.onStart();
        setupMorseChat();
    }

    public void setupMorseChat() {
        // inititalize arrayAdapter for conversation thread
        receivedMessagesArrayAdapter = new MessageArrayAdapter(getActivity(), R.id.receivedMessagesListView);
        sentMessagesArrayAdapter = new MessageArrayAdapter(getActivity(), R.id.receivedMessagesListView);

        patternList.add(0, new MorsePattern(1, 50L, 50L, 50L, 50L, 50L, 50L));
        patternList.add(1, new MorsePattern(2, 300L, 300L, 300L, 300L, 300L, 300L));
        patternList.add(2, new MorsePattern(3, 180L, 180L, 180L, 180L, 180L, 100L));

        receivedMessagesArrayAdapter.addAll(new MessageListItem("1", String.valueOf(System.currentTimeMillis())), new MessageListItem("2", String.valueOf(System.currentTimeMillis())));
        receivedMessagesListView.setAdapter(receivedMessagesArrayAdapter);

        sentMessagesArrayAdapter.addAll(new MessageListItem("1", String.valueOf(System.currentTimeMillis())), new MessageListItem("2", String.valueOf(System.currentTimeMillis())));
        sentMessagesListView.setAdapter(sentMessagesArrayAdapter);

    // TODO: test and finish this clicklistener
        receivedMessagesListView.setOnItemClickListener(clickListener);
        sentMessagesListView.setOnItemClickListener(clickListener);

        // Initialize the BluetoothChatService to perform bluetooth connections
        chatService = new BluetoothService(handler);
    }

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        bundle = getArguments();

        vibrator = (Vibrator) getActivity().getSystemService(Service.VIBRATOR_SERVICE);

        // we get the device address from ConnectDeviceFragment
        deviceAddress = bundle.getString("device_address");
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);

        chatService = new BluetoothService(handler);
        chatService.connect(device);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.fragment_conversationview, container, false);

        chatTitleTextView = parent.findViewById(R.id.chatTitleTextView);

        sentMessagesListView = parent.findViewById(R.id.sentMessagesListView);
        sentMessagesListView.setAdapter(sentMessagesArrayAdapter);

        receivedMessagesListView = parent.findViewById(R.id.receivedMessagesListView);
        receivedMessagesListView.setAdapter(receivedMessagesArrayAdapter);

        recordButton = parent.findViewById(R.id.recordButton);
        morseButton = parent.findViewById(R.id.morseButton);
        sendButton = parent.findViewById(R.id.sendButton);

        recordButton.setOnClickListener(this);
        morseButton.setOnTouchListener(this);
        sendButton.setOnClickListener(this);

        return parent;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.morseButton) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                afterVibrate = System.currentTimeMillis();
                vibrator.cancel();
                currentPatternList.add(patternStepCount++, (afterVibrate - startVibrate));
            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                startVibrate = System.currentTimeMillis();
                currentPatternList.add(patternStepCount++, (startVibrate - afterVibrate));
                vibrator.vibrate(10000);
            }
        }
        return false;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.recordButton:
                if (!recording) {
                    morseButton.setEnabled(true);
                    recordButton.setText("Stop");
                    afterVibrate = System.currentTimeMillis();
                    recording = true;
                } else {
                    morseButton.setEnabled(false);
                    recordButton.setText("Record");
                    if (currentPatternList.size() > 0) {
                        tmpPattern = new MorsePattern(id++, currentPatternList);
                        sendButton.setEnabled(true);
                    }
                    patternStepCount = 0;
                    recording = false;
                }
                break;
            case R.id.sendButton:
                if (tmpPattern != null) {
                    sendMessage(tmpPattern);
                    sendButton.setEnabled(false);
                }
                break;
        }
    }

    // sends a message
    private void sendMessage(MorsePattern message) {
        if (chatService.getState() != STATE_CONNECTED) {
            Toast.makeText(getActivity(), "Not Connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // get the message bytes and tell the chatService to write
        byte[] messageAsStream = serializeMorsePattern(message);
        chatService.write(messageAsStream);
    }

    final AdapterView.OnItemClickListener clickListener =  new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            MorsePattern patternToPlay = patternList.get(position);
            ArrayList<Long> morsePattern = patternToPlay.getMorsePattern();
            long[] vibrationPattern = new long[morsePattern.size()];
            for (int i = 0; i < morsePattern.size(); i++) {
                vibrationPattern[i] = morsePattern.get(i);
            }

            vibrator.vibrate(vibrationPattern, -1); // -1 no repeat, 0 repeat forever

            Log.d(",,", "PLAYING MORSE WITH ID: " + id);
        }
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
                            setChatTitleTextView("Waiting for Connection Request");
                        case STATE_NONE:
                            setChatTitleTextView("Not Connected");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuffer = (byte[]) msg.obj;
                    // construct a MorsePattern from the buffer
                    MorsePattern writePattern = deserializeMorsePattern(writeBuffer);
                /*    conversationArrayAdapter.add(String.valueOf(patternList.indexOf(writePattern)));
                    patternList.add(writePattern);*/
                    // TODO: find out how read and write messages behave
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a MorsePattern from the bytes in the buffer
                    MorsePattern pattern = deserializeMorsePattern(readBuf);
               /*     if (!patternList.contains(pattern)) {
                        patternList.add(pattern);
                    }
                    conversationArrayAdapter.add(String.valueOf(patternList.indexOf(pattern) + 1)); // + 1 cuz its an index
                   */
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
        for (int i = 0; i < patternList.size(); i++) {
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
