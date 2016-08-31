/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.foodlogiq.connect;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.IntentIntegrator;
import com.android.IntentResult;
import com.bluefletch.motorola.BarcodeScan;
import com.bluefletch.motorola.scanhelpers.DataWedgeParser;
import com.bluefletch.motorola.ScanCallback;

import org.apache.cordova.CordovaActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends CordovaActivity {

    private static final String ACTION_SOFT_SCAN = "com.motorolasolutions.emdk.datawedge.api.ACTION_SOFTSCANTRIGGER";
    private static final String EXTRA_TOGGLE_SCAN_PARAM = "com.motorolasolutions.emdk.datawedge.api.EXTRA_PARAMETER";
    private static final String TOGGLE_SCAN = "TOGGLE_SCANNING";
    private static final String SOFT_SCAN_INTENT = "com.foodlogiq.mobile.scan";

    private static final String ACTION_ZXING_SCAN = "com.google.zxing.client.android.SCAN";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);

        /**
         * Prompts the user to import our datawedge profile, if it hasn't already been imported.
         */
        if ((new File("/enterprise/device/settings/datawedge/autoimport/")).isDirectory()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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
        InputStream in = getResources().getAssets().open("dwprofile_FoodLogiQ.db");
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Register barcode scanning reciever to receive intents from laser scanner.
        registerReceiver(barcodeScanningReceiver, new IntentFilter(SOFT_SCAN_INTENT));
//        registerReceiver(enumerateScanner, new IntentFilter(enumeratedList));

        // first send the intent to enumerate the available scanners on the device.
//        Intent intent = new Intent(enumerateScanners);
//        sendBroadcast(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(barcodeScanningReceiver);
//        unregisterReceiver(enumerateScanner);
    }

    public void startLaserScanning() {
        toggleSoftScan();
    }

    public void startCameraScanning() {
        startCameraScan();
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

    private BroadcastReceiver barcodeScanningReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Parse laser scan result.
            parseDataWedgeBarcode(intent);
        }
    };

    private ScanCallback<BarcodeScan> scanCallback;

    public void setScanCallback(ScanCallback<BarcodeScan> scanCallback) {
        this.scanCallback = scanCallback;
    }

    private void toggleSoftScan() {
        Intent scanOnIntent = new Intent(ACTION_SOFT_SCAN);
        scanOnIntent.putExtra(EXTRA_TOGGLE_SCAN_PARAM, TOGGLE_SCAN);
        sendBroadcast(scanOnIntent);
    }

    private void parseDataWedgeBarcode(Intent intent) {
        Bundle extras = intent.getExtras();
        ArrayList<CharSequence> scanContent = extras.getCharSequenceArrayList("com" +
                ".motorolasolutions.emdk.datawedge.decode_data");

        String scanFormat = extras.getString("com.motorolasolutions.emdk.datawedge" +
                ".label_type");
        String scannedString = extras.getString("com.motorolasolutions.emdk.datawedge.data_string");

        if (scanFormat == null) return;

        byte[] bytes = (byte[]) ((List<?>) scanContent).get(0);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

    public void switchProfile(String profile) {
        Intent profileIntent = new Intent("com.motorolasolutions.emdk.datawedge.api.ACTION_SETDEFAULTPROFILE");
        profileIntent.putExtra("com.motorolasolutions.emdk.datawedge.api.EXTRA_PROFILENAME", profile);
        sendBroadcast(profileIntent);
    }

    boolean zxingStarted = false;

    private void startCameraScan() {
        if (!zxingStarted) {
            zxingStarted = true;
            Intent intentScan = new Intent(ACTION_ZXING_SCAN);
            intentScan.addCategory(Intent.CATEGORY_DEFAULT);
            intentScan.setPackage(getApplicationContext().getPackageName());
            startActivityForResult(intentScan, IntentIntegrator.REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        zxingStarted = false;
        IntentResult scanningResult = IntentIntegrator.parseActivityResult
                (requestCode, resultCode, intent);
        if (scanningResult != null) {
            byte[] scanContent = scanningResult.getRawBytes();
            String scanFormat = scanningResult.getFormatName();
            if (scanFormat == null) return;

            // Create the dialog for recognize barcode when scanning.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
}