package ch.joris.morseapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

public class ConnectDeviceFragment extends Fragment {
    private BroadcastReceiver receiver;
    public static String EXTRA_DEVICE_ADDRESS = "device_address"; // const for Intent
    private BluetoothAdapter btAdapter;
    private DeviceListAdapter deviceAdapter;
    private ArrayList<DeviceListItem> deviceList;
    private ListView deviceListView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init and populate list
        deviceList = new ArrayList<>();
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 1) {
            for (BluetoothDevice device : pairedDevices) {
               // TODO: test if we can only display phones instead of all devices if(device.getType())
                deviceList.add(new DeviceListItem(device.getName(), device.getAddress()));
            }
        }

        deviceAdapter = new DeviceListAdapter(getContext(), R.layout.layout_devicelistitem, deviceList);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        final View parentView = inflater.inflate(R.layout.fragment_connectdevice, container, false);

        deviceListView = parentView.findViewById(R.id.deviceList);
        deviceListView.setAdapter(deviceAdapter);
        deviceListView.setOnItemClickListener((new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                TextView deviceAddressTextView = parent.findViewById(R.id.textView_deviceAddress);
                String address = deviceAddressTextView.getText().toString();

                Bundle bundle = new Bundle();
                bundle.putString(EXTRA_DEVICE_ADDRESS, address);

                MorseChatFragment morseChatFragment = new MorseChatFragment();
                morseChatFragment.setArguments(bundle);

                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, morseChatFragment).commit();
            }
        }));

        return parentView;
    }

}
