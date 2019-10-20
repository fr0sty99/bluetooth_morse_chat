package ch.joris.morseapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {
    private BluetoothAdapter btAdapater;
    private final int REQUEST_ENABLE_BT = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        btAdapater = BluetoothAdapter.getDefaultAdapter();

        // if btAdapter is null, Bluetooth is not supported
        if (btAdapater == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }

        setContentView(R.layout.activity_main);

    }

    private void setupMorseCHat() {
        // if (savedInstanceState == null) { ??
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        supportFragmentManager.beginTransaction().add(R.id.fragmentContainer, new ConnectDeviceFragment()).commit();

    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is off, request to enable.
        // after enabling, setupChat will be called in onActivityResult
        if (!btAdapater.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise setup morse chat session
        } else {
            setupMorseCHat();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                // bluetooth enabled, setup morse chat session
                setupMorseCHat();
            } else {
                // User didnt enable Bluetooth or error occurred
                Toast.makeText(this, "Bluetooth is crucial for this app...",
                        Toast.LENGTH_SHORT).show();
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }

        }
    }
}
