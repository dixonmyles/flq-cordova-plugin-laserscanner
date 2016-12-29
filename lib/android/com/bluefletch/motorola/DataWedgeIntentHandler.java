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

import java.util.ArrayList;
import java.util.List;

public class DataWedgeIntentHandler extends Activity {

    protected static Object stateLock = new Object();
    protected static boolean hasInitialized = false;

    protected static String TAG = DataWedgeIntentHandler.class.getSimpleName();

    protected Context applicationContext;

    protected static String DEFAULT_ACTION = "com.bluefletch.motorola.datawedge.ACTION";
    protected String dataWedgeAction = DEFAULT_ACTION;

    /**
     * This function must be called with the intent Action as configured in the DataWedge Application
     **/
    public void setDataWedgeIntentAction(String action) {
        Log.i(TAG, "Setting data wedge intent to " + action);
        if (action == null || "".equals(action)) return;
        this.dataWedgeAction = action;
    }

    protected ScanCallback<BarcodeScan> scanCallback;

    public void setScanCallback(ScanCallback<BarcodeScan> callback) {
        scanCallback = callback;
    }

    protected ScanCallback<List<String>> magstripeCallback;

    public void setMagstripeReadCallback(ScanCallback<List<String>> callback) {
        magstripeCallback = callback;
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
            parseDataWedgeBarcode(intent);
        }
    };

    @SuppressLint("LongLogTag")
    private void parseDataWedgeBarcode(Intent intent) {
    }
}