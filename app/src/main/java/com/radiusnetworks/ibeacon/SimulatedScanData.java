package com.radiusnetworks.ibeacon;

import java.util.ArrayList;
import java.util.List;

public class SimulatedScanData {
    public static List<IBeacon> iBeacons;
    /*
     * You may simulate detection of iBeacons by creating a class like this in your project in the com.radiusnetworks.ibeacon package.
     * Each iBeacon in the list will be detected by the system exactly once in each scan cycle.
     * This is especially useful for when you are testing in an Emulator or on a device without BluetoothLE capability.
     *
     * Note that by default in this demo, this is disabled.  change the line below from false to true to enable it.
     */
    public static boolean USE_SIMULATED_IBEACONS = true;

    static {
        iBeacons = new ArrayList<IBeacon>();

        if (USE_SIMULATED_IBEACONS) {
            IBeacon iBeacon1 = new IBeacon("DF7E1C79-43E9-44FF-886F-1D1F7DA6997A".toLowerCase(),
                    1, 1, -55, -55);
          //  IBeacon iBeacon2 = new IBeacon("DF7E1C79-43E9-44FF-886F-1D1F7DA6997B".toLowerCase(),
           //         1, 2, -55, -55);
            IBeacon iBeacon3 = new IBeacon("DF7E1C79-43E9-44FF-886F-1D1F7DA6997C".toLowerCase(),
                    1, 3, -55, -55);
            IBeacon iBeacon4 = new IBeacon("DF7E1C79-43E9-44FF-886F-1D1F7DA6997D".toLowerCase(),
                    1, 4, -55, -55);
            iBeacons.add(iBeacon1);
           // iBeacons.add(iBeacon2);
            iBeacons.add(iBeacon3);
            iBeacons.add(iBeacon4);
        }
    }
}
