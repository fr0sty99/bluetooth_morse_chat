package ch.joris.morseapp;

public class DeviceListItem {
    private String deviceName;
    private String deviceAddress;

    public DeviceListItem(String deviceName, String deviceAddress) {
        this.deviceAddress = deviceAddress;
        this.deviceName = deviceName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

}
