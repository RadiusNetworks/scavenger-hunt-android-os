package com.radiusnetworks.beacon;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;
import java.util.List;

public class SimulatedScanData {
    public static List<Beacon> beacons;
    /*
     * You may simulate detection of beacons by creating a class like this in your project in the org.altbeacon.beacon package.
     * Each beacon in the list will be detected by the system exactly once in each scan cycle.
     * This is especially useful for when you are testing in an Emulator or on a device without BluetoothLE capability.
     *
     * Note that by default in this demo, this is disabled.  change the line below from false to true to enable it.
     */
    public static boolean USE_SIMULATED_BEACONS = false;

    static {
        beacons = new ArrayList<Beacon>();

        if (USE_SIMULATED_BEACONS) {
            Beacon.Builder builder1 = new Beacon.Builder();
            builder1.setId1("2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6".toLowerCase());
            builder1.setId2("1");
            builder1.setId3("1");
            builder1.setRssi(-55);
            builder1.setTxPower(-55);
            Beacon beacon1 = builder1.build();

            Beacon.Builder builder2 = new Beacon.Builder();
            builder2.setId1("2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6".toLowerCase());
            builder2.setId2("1");
            builder2.setId3("2");
            builder2.setRssi(-55);
            builder2.setTxPower(-55);
            Beacon beacon2 = builder2.build();

            beacons.add(beacon1);
            beacons.add(beacon2);
        }
    }
}