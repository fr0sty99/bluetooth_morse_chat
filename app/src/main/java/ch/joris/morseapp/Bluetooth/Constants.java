package ch.joris.morseapp.Bluetooth;

public interface Constants {
    // Key names received from the BluetoothChatService Handler
    String DEVICE_NAME = "device_name";
    String TOAST = "toast";

    int STATE_NONE = 0;       // doing nothing
    int STATE_LISTEN = 1;     // listening for incoming connections
    int STATE_CONNECTING = 2; // initiating an outgoing connection
    int STATE_CONNECTED = 3;  // connected to a remote device

    // Message types sent from the BluetoothChatService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_DEVICE_NAME = 4;
    int MESSAGE_TOAST = 5;
}
