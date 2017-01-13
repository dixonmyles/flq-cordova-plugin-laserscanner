package com.bluefletch.motorola.plugin;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.IntentIntegrator;
import com.android.IntentResult;
import com.bluefletch.motorola.BarcodeScan;
import com.bluefletch.motorola.DataWedgeIntentHandler;
import com.bluefletch.motorola.ScanCallback;
import com.bluefletch.motorola.scanhelpers.DataWedgeParser;
import com.foodlogiq.connect.MainActivity;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class MotorolaDatawedgePlugin extends CordovaPlugin {

    protected static String TAG = "MotorolaDatawedgePlugin";

    private DataWedgeIntentHandler mainActivity;

    private static final String ACTION_ZXING_SCAN = "com.google.zxing.client.android.SCAN";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mainActivity = new DataWedgeIntentHandler(cordova.getActivity().getBaseContext());
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Log.i(TAG, "Action to execute is " + action);
        if ("scanner.register".equals(action)) {
          Log.i(TAG, "We scanner now");
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
            this.setScanCallback(new ScanCallback<BarcodeScan>() {
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
            startCameraScan();
            callbackContext.success();
        } else if ("scanner.softScanOff".equals(action)) {
            callbackContext.success();
        } else if ("scanner.dbimport".equals(action)) {
            dbimport();
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
        //start plugin now if not already started
        if ("start".equals(action) || "magstripe.register".equals(action) || "scanner.register".equals(action)) {

            //try to read intent action from inbound params
            String intentAction = null;
            if (args.length() > 0) {
                intentAction = args.getString(0);
            }
            if (intentAction != null && intentAction.length() > 0) {
                Log.i(TAG, "Intent action length  " + intentAction.length());

                mainActivity.setDataWedgeIntentAction(intentAction);
            }
            mainActivity.start();
        }

        return true;
    }

    /**
     * Always close the current intent reader
     */
    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        mainActivity.stop();
    }

    @Override
    public void onNewIntent(Intent intent) {
      Log.i(TAG, "Got inbound intent: " + intent.getAction());
      mainActivity.handleIntent(intent);
    }

    /**
     * Always resume the current activity
     */
    @Override
    public void onResume(boolean multitasking) {
      super.onResume(multitasking);
      mainActivity.start();
    }

    boolean zxingStarted = false;

    private void startCameraScan() {
        if (!zxingStarted) {
            zxingStarted = true;
            Intent intentScan = new Intent(ACTION_ZXING_SCAN);
            intentScan.addCategory(Intent.CATEGORY_DEFAULT);
            intentScan.setPackage(this.cordova.getActivity().getApplicationContext().getPackageName());
            this.cordova.startActivityForResult((CordovaPlugin) this, intentScan, IntentIntegrator.REQUEST_CODE);
        }
    }
    private ScanCallback<BarcodeScan> scanCallback;

    public void setScanCallback(ScanCallback<BarcodeScan> scanCallback) {
        this.scanCallback = scanCallback;
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        zxingStarted = false;
        IntentResult scanningResult = IntentIntegrator.parseActivityResult
                (requestCode, resultCode, intent);
        if (scanningResult != null) {
            byte[] scanContent = scanningResult.getRawBytes();
            String scanFormat = scanningResult.getFormatName();
            if (scanFormat == null) return;

            // Create the dialog for recognize barcode when scanning.
            AlertDialog.Builder builder = new AlertDialog.Builder(this.cordova.getActivity().getApplicationContext());
            builder.setPositiveButton(android.R.string.ok, new DialogInterface
                    .OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });

            switch (scanFormat) {
                case "CODE_128":
                    if (scanContent == null) return;
                    // BarcodeParser barcodeParser = new BarcodeParser(scanContent);
                    DataWedgeParser barcodeParser = new DataWedgeParser(scanContent);
                    scanCallback.execute(new BarcodeScan(
                            scanFormat,
                            scanningResult.getContents(),
                            barcodeParser.getGlobalTradeItemNumber(),
                            barcodeParser.getLot(),
                            barcodeParser.getPackDate(),
                            barcodeParser.getUseThroughDate(),
                            barcodeParser.getSerialNumber(),
                            barcodeParser.getQuantity()
                    ));
                    break;
                case "ITF":
                    scanCallback.execute(new BarcodeScan(
                            scanFormat,
                            scanningResult.getContents(),
                            scanningResult.getContents(),
                            "ITF-14",
                            "",
                            "",
                            "",
                            1
                    ));
                    break;
                //These all have the same functionality. Just pad the content with
                // zeros on the left.
                case "EAN_13":
                case "UPC_A":
                case "UPC_E":
                    String paddedGtin = padStringTo14Characters(scanningResult.getContents());
                    scanCallback.execute(new BarcodeScan(
                            scanFormat,
                            scanningResult.getContents(),
                            scanningResult.getContents(),
                            "UPC",
                            "",
                            "",
                            "",
                            1
                    ));
                    break;
                default:
                    scanCallback.execute(new BarcodeScan(
                            scanFormat,
                            scanningResult.getContents(),
                            scanningResult.getContents(),
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
    /**
     * Prompts the user to import our datawedge profile, if it hasn't already been imported.
     */
    private void dbimport() {
        if ((new File("/enterprise/device/settings/datawedge/autoimport/")).isDirectory()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.cordova.getActivity());
            builder.setMessage("Import Datawedge");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        importDataWedgeConfig();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
            builder.create().show();
        }

    }
    /**
     * Loads the datawedge profile from assets into the datawedge autoimport folder, which is picked
     * up by datawedge and overwrites the current configuration with the one needed for our
     * implementations.
     * The datawedge configuration should be stored in the $ROOT/src/main/assets/
     */
    private void importDataWedgeConfig() throws IOException {
        InputStream in = this.cordova.getActivity().getResources().getAssets().open("dwprofile_FoodLogiQ.db");
        OutputStream out = new FileOutputStream
                ("/enterprise/device/settings/datawedge/autoimport/dwprofile_FoodLogiQ.db");
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.flush();
        out.close();
        Runtime.getRuntime().exec("chmod 777 " +
                "/enterprise/device/settings/datawedge/autoimport/dwprofile_FoodLogiQ.db");
    }
}
