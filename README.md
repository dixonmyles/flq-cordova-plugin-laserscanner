Cordova FoodLogiq DataWedge Plugin
============
This plugin is an customized one for just FoodLogiq application.
This is a Cordova/Phonegap plugin to interact with Motorola ruggedized devices' Barcode Scanners and Magnetic Stripe Readers (eg, ET1, MC40, TC55).  The plugin works by interacting with the "DataWedge" application configured to output scan and magstripe events.

=============

This plugin is compatible with plugman.  To install, run the following from your project command line: 
```$ cordova plugin add https://github.com/toboroz/fql-cordova-plugin-laserscanner.git```


==============

<h3>Configure DataWedge:</h3>
You have two options to interact with the current version of the DataWedge:

1. Broadcast an intent from the Default (0) profile.  This can be a custom action of your choosing.  NOTE: Category must be empty.
2. Create a custom profile associated to your app, and send a "startActivity" intent.  This must use the default plugin action: _"com.bluefletch.motorola.datawedge.ACTION"_ and EMPTY category.

The DataWedge User Guide is located here: `https://launchpad.motorolasolutions.com/documents/dw_user_guide.html`

Intent configuration: `https://launchpad.motorolasolutions.com/documents/dw_user_guide.html#_intent_output`

Special configuration for option 2:

1. You'll need to first create a Profile in your DataWedge Application to run **startActivity** for an intent on scan/magstripe events as applicable.  NOTE: you MUST set the action to: _"com.bluefletch.motorola.datawedge.ACTION"_ and category to EMPTY/BLANK
2. Associate your app to the DataWedge profile so it loads with your app. Configure this under `(Your profile) > Associated apps > New app/activity (menu button) > (Select your app)`
3. Lastly, you need to set your application to be "singleTop" in Cordova.  This will make sure each scan doesn't launch a new instance of your app. Add the following to your config.xml: 
```<preference name="AndroidLaunchMode" value="singleTop" />```




<h3>To Use:</h3>

1) First, you need to "activate" the plugin and OPTIONALLY tell it what intent to listen for.  Default is _"com.foodlogiq.mobile.scan"_
```
   document.addEventListener("deviceready", function(){ 
      if (window.datawedge) {
      	 datawedge.start(); //uses default
         //datawedge.start("com.foodlogiq.mobile.scan");
      }
   });
```

2) Register for callbacks for barcode scanning and/or magnetic stripe reads:
```
   document.addEventListener("deviceready", function(){ 
       ...
       datawedge.registerForBarcode(function(data){
           var labelType = data.type,
               barcode   = data.barcode,
               gtin   = data.gtin,
               lot   = data.lot,
               quantity   = data.quantity,
               packed   = data.packed,
               use   = data.use;

           console.log("Barcode scanned.  Label type is: " + labelType + ", " + barcode + ", " + gtin + ", " + lot + ", " + quantity + ", " + packed + ", " + use);
           
           //TODO: handle barcode/label type
       });
       //This function will be passed an array with the read values for each track.  
       //NOTE: If a track could not be read, it will contain null
       datawedge.registerForMagstripe(function(tracks){
       	   var track1, track2;
           console.log("read magstripe value : " + JSON.stringify(tracks));

           //read track 1
           if (tracks[0]) {
              //track 1 uses carets as dividers
              track1 = tracks[0].split('^');
              
              var cc = {
                 number : track1[0].substr(2), //strip leading %B
                 name : track1[1].trim(),
                 expr : '20' + track1[2].substr(0,2) + '-' + track1[2].substr(2,2)
              };

              //TODO: handle track 1
           } 
           else if (tracks[1]) {
           	  track2 = tracks[1];
              
              //TODO: handle track 2
           } 
           else {
              //ERROR: track 3 almost never has anything in it
           }
		
       });
```
1) Third, you need to add some code on MainActivity.java to set from datawedge.db file
```
   public class MainActivity extends CordovaActivity
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
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
            InputStream in = getResources().getAssets().open("datawedge.db");
            OutputStream out = new FileOutputStream
                    ("/enterprise/device/settings/datawedge/autoimport/datawedge.db");
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
            Runtime.getRuntime().exec("chmod 777 " +
                    "/enterprise/device/settings/datawedge/autoimport/datawedge.db");
        }
    }
```

=============
<h3>More API options:</h3>

<h5>Data Wedge</h5>
* Switch Profile: if you need to change DataWedge Profiles (ie, to disable the scanner buttons or to apply rules to data, etc), call `datawedge.switchProfile('DisabledScannerButton')`

<h5>Scanner:</h5>
* You can wire a soft button to the barcode scanner by calling `datawedge.startScanner()`
* Turn off the scanner manually using: `datawedge.stopScanner()`
* Unregister for barcode scans by calling: `datawedge.unregisterBarcode()`

<h5>Magstripe:</h5>
* Unregister for barcode scans by calling: `datawedge.unregisterMagstripe()`

==============
Copyright 2016 Anatoly Sokolov

