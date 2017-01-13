package com.bluefletch.motorola;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.android.IntentIntegrator;
import com.android.IntentResult;
import com.bluefletch.motorola.scanhelpers.DataWedgeParser;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DataWedgeIntentHandler{

    protected static Object stateLock = new Object();
    protected static boolean hasInitialized = false;

    protected static String TAG = DataWedgeIntentHandler.class.getSimpleName();

    protected Context applicationContext;

    protected static String DEFAULT_ACTION = "com.bluefletch.motorola.datawedge.ACTION";
    protected String dataWedgeAction = DEFAULT_ACTION;

    private static final String ACTION_SOFT_SCAN = "com.motorolasolutions.emdk.datawedge.api.ACTION_SOFTSCANTRIGGER";
    private static final String EXTRA_TOGGLE_SCAN_PARAM = "com.motorolasolutions.emdk.datawedge.api.EXTRA_PARAMETER";
    private static final String TOGGLE_SCAN = "TOGGLE_SCANNING";
    private static final String SOFT_SCAN_INTENT = "com.foodlogiq.mobile.scan";

    /**
     * This function must be called with the intent Action as configured in the DataWedge Application
     **/
    public void setDataWedgeIntentAction(String action) {
        Log.i(TAG, "Setting data wedge intent to " + action);
        if (action == null || "".equals(action)) return;
        this.dataWedgeAction = action;
    }

    public DataWedgeIntentHandler(Context context) {
        TAG = this.getClass().getSimpleName();
        applicationContext = context;
    }

    public void start() {
        Log.i(TAG, "Open called");
        if (hasInitialized) {
            return;
        }
        synchronized (stateLock) {
            if (hasInitialized) {
                return;
            }

            Log.i(TAG, "Register for Datawedge intent: " + dataWedgeAction);

            applicationContext.registerReceiver(dataReceiver, new IntentFilter(dataWedgeAction));

            enableScanner(true);
            hasInitialized = true;
        }
    }

    public void stop() {
        if (!hasInitialized) {
            return;
        }
        synchronized (stateLock) {
            if (!hasInitialized) {
                return;
            }

            Log.i(TAG, "Running close plugin intent");

            enableScanner(false);

            try {
                applicationContext.unregisterReceiver(dataReceiver);
            } catch (Exception ex) {
                Log.e(TAG, "Exception while unregistering data receiver. Was start ever called?", ex);
            }

            hasInitialized = false;
        }
    }

    public boolean hasListeners() {
        return this.scanCallback != null || this.magstripeCallback != null;
    }

    protected void enableScanner(boolean shouldEnable) {
        Intent enableIntent = new Intent("com.motorolasolutions.emdk.datawedge.api.ACTION_SCANNERINPUTPLUGIN");
        enableIntent.putExtra("com.motorolasolutions.emdk.datawedge.api.EXTRA_PARAMETER",
                shouldEnable ? "ENABLE_PLUGIN" : "DISABLE_PLUGIN");

        applicationContext.sendBroadcast(enableIntent);
    }

    public void startScanning(boolean turnOn) {
        synchronized (stateLock) {
            Intent scanOnIntent = new Intent("com.motorolasolutions.emdk.datawedge.api.ACTION_SOFTSCANTRIGGER");
            scanOnIntent.putExtra("com.motorolasolutions.emdk.datawedge.api.EXTRA_PARAMETER",
                    turnOn ? "START_SCANNING" : "STOP_SCANNING");

            applicationContext.sendBroadcast(scanOnIntent);
        }
    }

    public void switchProfile(String profile) {
        synchronized (stateLock) {
            Intent profileIntent = new Intent("com.motorolasolutions.emdk.datawedge.api.ACTION_SETDEFAULTPROFILE");
            profileIntent.putExtra("com.motorolasolutions.emdk.datawedge.api.EXTRA_PROFILENAME", profile);

            applicationContext.sendBroadcast(profileIntent);
        }
    }

    public void handleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null && action.equals(DEFAULT_ACTION)) {
                dataReceiver.onReceive(applicationContext, intent);
            }
        }
    }

    private static String TRACK_PREFIX_FORMAT = "com.motorolasolutions.emdk.datawedge.msr_track%d";
    private static String TRACK_STATUS_FORMAT = "com.motorolasolutions.emdk.datawedge.msr_track%d_status";
    /**
     * Receiver to handle receiving data from intents
     */
    private BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          Log.i(TAG, "Broadcast received with intent: " + intent.getAction());
            parseDataWedgeBarcode(intent);
        }
    };


    public void startLaserScanning() {
        toggleSoftScan();
    }

    public boolean isLaserDevice() {
        if ((new File("/enterprise/device/settings/datawedge/")).isDirectory()) {
            return true;
        } else {
            return false;
        }
    }

    boolean isLaserDeviceAvailable = false;
    String enumerateScanners = "com.motorolasolutions.emdk.datawedge.api.ACTION_ENUMERATESCANNERS";
    String enumeratedList = "com.motorolasolutions.emdk.datawedge.api.ACTION_ENUMERATEDSCANNERLIST";
    String KEY_ENUMERATEDSCANNERLIST = "DataWedgeAPI_KEY_ENUMERATEDSCANNERLIST";

    private BroadcastReceiver enumerateScanner = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(enumeratedList)) {
                Bundle b = intent.getExtras();
                String[] scanner_list = b.getStringArray(KEY_ENUMERATEDSCANNERLIST);
                if (scanner_list != null && scanner_list.length > 0) {
                    isLaserDeviceAvailable = true;
                }
            }
        }
    };

    private ScanCallback<BarcodeScan> scanCallback;

    public void setScanCallback(ScanCallback<BarcodeScan> scanCallback) {
        this.scanCallback = scanCallback;
    }

    private void toggleSoftScan() {
        Intent scanOnIntent = new Intent(ACTION_SOFT_SCAN);
        scanOnIntent.putExtra(EXTRA_TOGGLE_SCAN_PARAM, TOGGLE_SCAN);
        Log.i(TAG, "toggleSoftScan called with intent action: " + scanOnIntent.getAction());
        applicationContext.sendBroadcast(scanOnIntent);
    }

    private void parseDataWedgeBarcode(Intent intent) {
        Log.i(TAG, "Beginning parse of intent: " + intent.toString());
        Bundle extras = intent.getExtras();
        ArrayList<CharSequence> scanContent = extras.getCharSequenceArrayList("com" +
                ".motorolasolutions.emdk.datawedge.decode_data");

        String scanFormat = extras.getString("com.motorolasolutions.emdk.datawedge" +
                ".label_type");
        String scannedString = extras.getString("com.motorolasolutions.emdk.datawedge.data_string");

        if (scanFormat == null) return;

        byte[] bytes = (byte[]) ((List<?>) scanContent).get(0);

        AlertDialog.Builder builder = new AlertDialog.Builder(applicationContext);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface
                .OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        switch (scanFormat) {
            case "LABEL-TYPE-EAN128": {
                DataWedgeParser dataWedgeParser = new DataWedgeParser(bytes);

                String globalTradeItemNumber = dataWedgeParser.getGlobalTradeItemNumber();

                if (globalTradeItemNumber.isEmpty()) {
                    Log.d("Scan status", "No GTIN found in that GS1-128 Barcode");
                }

                String lot = dataWedgeParser.getLot();
                int quantity = dataWedgeParser.getQuantity();
                String useThroughDateString = dataWedgeParser.getUseThroughDate();
                String packedDateString = dataWedgeParser.getPackDate();
                scanCallback.execute(new BarcodeScan(
                        scanFormat,
                        scannedString,
                        globalTradeItemNumber,
                        lot,
                        packedDateString,
                        useThroughDateString,
                        dataWedgeParser.getSerialNumber(),
                        quantity));
                break;
            }
            case "LABEL-TYPE-I2OF5":
                scanCallback.execute(new BarcodeScan(
                        scanFormat,
                        scannedString,
                        scannedString,
                        "ITF-14",
                        "",
                        "",
                        "",
                        1));
                break;
            case "LABEL-TYPE-EAN13":
            case "LABEL-TYPE-UPCE0":
            case "LABEL-TYPE-UPCA":
            case "UPC_E": {
                String paddedGtin = padStringTo14Characters(scannedString);
                scanCallback.execute(new BarcodeScan(
                        scanFormat,
                        scannedString,
                        scannedString,
                        "UPC",
                        "",
                        "",
                        "",
                        1
                ));
                break;
            }
            default: {
                scanCallback.execute(new BarcodeScan(
                        scanFormat,
                        scannedString,
                        scannedString,
                        scanFormat,
                        "",
                        "",
                        "",
                        1
                ));
                break;
            }
        }
    }

    private ScanCallback<List<String>> magstripeCallback;

    public void setMagstripeReadCallback(ScanCallback<List<String>> callback) {
        magstripeCallback = callback;
    }

    /**
     * Helper function to expand UPC/ITF-14/EAN13 codes to a 14 digit GTIN.
     *
     * @param contents string of variable length digits to be preceded by 0's until  the string
     *                 is 14 digits
     * @return 0-padded string of input contents
     */
    private String padStringTo14Characters(String contents) {
        String paddedGtin;
        int padLength = 14 - contents.length();
        StringBuilder paddingBuffer = new StringBuilder(padLength);
        for (int i = 0; i < padLength; i++) {
            paddingBuffer.append("0");
        }
        paddedGtin = paddingBuffer.toString().concat(contents);
        return paddedGtin;
    }
}
