package com.bluefletch.motorola.plugin;

import android.content.Intent;
import android.util.Log;

import com.bluefletch.motorola.BarcodeScan;
import com.bluefletch.motorola.ScanCallback;
import com.foodlogiq.connect.MainActivity;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MotorolaDatawedgePlugin extends CordovaPlugin {

    protected static String TAG = "MotorolaDatawedgePlugin";

    MainActivity mainActivity;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mainActivity = (MainActivity) cordova.getActivity();
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if ("scanner.register".equals(action)) {
            mainActivity.setScanCallback(new ScanCallback<BarcodeScan>() {
                @Override
                public void execute(BarcodeScan scan) {
                    try {
                        JSONObject obj = new JSONObject();
                        obj.put("type", scan.scanFormat);
                        obj.put("barcode", scan.barcode);
                        obj.put("gtin", scan.globalTradeNumber);
                        obj.put("lot", scan.lot);
                        obj.put("packed", scan.packedDate);
                        obj.put("use", scan.useThroughDate);
                        obj.put("serial_number", scan.serialNumber);
                        obj.put("quantity", scan.quantity);

                        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, obj);
                        pluginResult.setKeepCallback(true);
                        callbackContext.sendPluginResult(pluginResult);

                    } catch (JSONException e) {
                        Log.e(TAG, "Error building json object", e);

                    }
                }
            });
        } else if ("scanner.unregister".equals(action)) {
            mainActivity.setScanCallback(null);
        } else if ("scanner.laserScanOn".equals(action)) {
            mainActivity.startLaserScanning();
            callbackContext.success();
        } else if ("scanner.cameraScanOn".equals(action)) {
            mainActivity.startCameraScanning();
            callbackContext.success();
        } else if ("scanner.softScanOff".equals(action)) {
            callbackContext.success();
        } else if ("magstripe.register".equals(action)) {
            mainActivity.setMagstripeReadCallback(new ScanCallback<List<String>>() {
                @Override
                public void execute(List<String> result) {
                    Log.i(TAG, "Magstripe result [" + result + "].");
                    JSONArray tracks = new JSONArray(result);
                    //send plugin result
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, tracks);
                    pluginResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(pluginResult);
                }
            });
        } else if ("magstripe.unregister".equals(action)) {
            mainActivity.setMagstripeReadCallback(null);
        } else if ("switchProfile".equals(action)) {
            mainActivity.switchProfile(args.getString(0));
        }

        return true;
    }

    /**
     * Always close the current intent reader
     */
    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
    }

    @Override
    public void onNewIntent(Intent intent) {
    }

    /**
     * Always resume the current activity
     */
    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
    }
}
