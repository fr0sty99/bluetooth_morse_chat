package ch.joris.morseapp.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static ch.joris.morseapp.Bluetooth.Constants.DEVICE_NAME;
import static ch.joris.morseapp.Bluetooth.Constants.MESSAGE_DEVICE_NAME;
import static ch.joris.morseapp.Bluetooth.Constants.MESSAGE_READ;
import static ch.joris.morseapp.Bluetooth.Constants.MESSAGE_STATE_CHANGE;
import static ch.joris.morseapp.Bluetooth.Constants.MESSAGE_TOAST;
import static ch.joris.morseapp.Bluetooth.Constants.MESSAGE_WRITE;
import static ch.joris.morseapp.Bluetooth.Constants.STATE_CONNECTED;
import static ch.joris.morseapp.Bluetooth.Constants.STATE_CONNECTING;
import static ch.joris.morseapp.Bluetooth.Constants.STATE_LISTEN;
import static ch.joris.morseapp.Bluetooth.Constants.STATE_NONE;
import static ch.joris.morseapp.Bluetooth.Constants.TOAST;

public class BluetoothService {
    // Constants
    private static final String TAG = "BluetoothService";   // Debug
    private static final String NAME_SECURE = "BluetoothChatSecure";    // Name of the SDP record when creating server socket
    private static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");  // Unique UUID for this application

    // Member Variables
    private BluetoothAdapter adapter;
    private Handler handler;
    private AcceptThread secureAcceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private int state;
    private int newState;

    // prepare new bluetooth connection
    public BluetoothService(Handler handler) {
        adapter = BluetoothAdapter.getDefaultAdapter();
        state = STATE_NONE;
        newState = state;
        this.handler = handler;
    }

    // To start Bluetooth Service, kill all Threads and start AcceptThread to begin a session in listening (server) Mode
    public synchronized void start() {
        // Cancel any thread attempting to make a connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (secureAcceptThread == null) {
            secureAcceptThread = new AcceptThread(true);
            secureAcceptThread.start();
        }

        // Update UI title
        updateUserInterfaceTitle();
    }

    // updates ActionBar Title according to app state
    private synchronized void updateUserInterfaceTitle() {
        state = getState();
        Log.d(TAG, "updateUserInterfaceTitle() " + newState + " -> " + state);
        newState = state;

        // give handler new state, so UI Activity can update
        handler.obtainMessage(MESSAGE_STATE_CHANGE, newState, -1).sendToTarget();
    }

    public synchronized int getState() {
        return state;
    }

    // Start the connectThread to initiate a connection to a remote device
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (state == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Start the thread to connect with the given device
        connectThread = new ConnectThread(device);
        connectThread.start();

        // Update UI title
        updateUserInterfaceTitle();
    }

    // Starts the ConnectedThread after receiving the socket and deivce from ConnectThread, to begin managing a Bluetooth connection
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (secureAcceptThread != null) {
            secureAcceptThread.cancel();
            secureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket, socketType);
        connectedThread.start();

        // give handler the name of the newly connected device, to let the UI Activity update
        Message msg = handler.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());
        msg.setData(bundle);
        handler.sendMessage(msg);

        // Update UI title
        updateUserInterfaceTitle();
    }

    // stops all threads
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (secureAcceptThread != null) {
            secureAcceptThread.cancel();
            secureAcceptThread = null;
        }

        /*  not used
        if (insecureAcceptThread != null) {
            insecureAcceptThread.cancel();
            insecureAcceptThread = null;
        }
        */

        state = STATE_NONE;

        // Update UI title
        updateUserInterfaceTitle();
    }

    // Write to ConnectedThread (unsynchronized)
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;

        // TODO: what is this needed for
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (state != STATE_CONNECTED) return;
            r = connectedThread;
        }

        // Perform unsynchronized write
        r.write(out);
    }

    // Indicate that the connection attempt failed and notify the UI Activity.
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = handler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Unable to connect device");
        msg.setData(bundle);
        handler.sendMessage(msg);

        state = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    // Indicate that the connection was lost and notify the UI Activity
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = handler.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(TOAST, "Device connection was lost");
        msg.setData(bundle);
        handler.sendMessage(msg);

        state = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmpSocket = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                tmpSocket = adapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);

             /*   if (secure) {    // always secure
                } else {
                    // ... i.e the communication may be vulnerable to Man In the Middle attacks.
                    // doesnt use encryption or authentication
                    tmpSocket = adapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                } */

            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmpSocket;
            state = STATE_LISTEN;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType +
                    "BEGIN AcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket;

            // Listen to the server socket if we're not connected
            while (state != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (state) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);
        }

        public void cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }

    // runs while attempting to make an outgoing connection with a device. It runs straight through: the connection either succeeds or fails
    private class ConnectThread extends Thread {
        private final BluetoothSocket btSocket;
        private final BluetoothDevice btDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device) {
            btDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: Secure create() failed", e);
            }
            btSocket = tmp;
            state = STATE_CONNECTING;
        }

        public void run() {
            Log.i(TAG, "BEGIN connectThread SocketType:" + mSocketType);
            setName("connectThread" + mSocketType);

            // cancel discovery because it will slow down a connection
            adapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                btSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the connectThread because we're done
            synchronized (BluetoothService.this) {
                connectThread = null;
            }

            // Start the connected thread
            connected(btSocket, btDevice, mSocketType);
        }

        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            state = STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG, "BEGIN connectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (state == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                handler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

}
