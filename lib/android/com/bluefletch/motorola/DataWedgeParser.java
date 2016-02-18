package com.bluefletch.motorola;

import java.util.HashMap;
import java.util.Map;

/**
 * Parse a GS1128Barcode from data retrieved from devices
 * Similar to {@link ApplicationIdentifiers}
 */
public class DataWedgeParser {

    public static final Map<String, Integer> aiLengths = new HashMap<String, Integer>();

    static {
        aiLengths.put("00", 18);
        aiLengths.put("01", 14);
        aiLengths.put("02", 14);
        aiLengths.put("10", 0);
        aiLengths.put("11", 6);
        aiLengths.put("12", 6);
        aiLengths.put("13", 6);
        aiLengths.put("15", 6);
        aiLengths.put("16", 6);
        aiLengths.put("17", 6);
        aiLengths.put("20", 2);
        aiLengths.put("21", 0);
        aiLengths.put("30", 0);
        aiLengths.put("37", 0);
        aiLengths.put("3201", 0);
        aiLengths.put("3202", 0);
    }

    private final HashMap<String, String> ais;

    public DataWedgeParser(byte[] bytes) {
        this.ais = new HashMap<String, String>();
        parseFromDataWedge(bytes);
    }

    public void parseFromDataWedge(byte[] bytes) {

        boolean done = false;
        int i = 0;
        while (!done) {
            // get the AI from the byte array (could be 2, 3, or 4 digits)
            String ai = null;
            int[] lengths = new int[]{2, 3, 4};
            for (int j = 0; (j < lengths.length) && (ai == null); j++) {
                String a = "";
                for (int k = 0; k < j; k++) {
                    a += String.valueOf((char) (bytes[i + k] & 0xFF));
                }

                if (aiLengths.get(a) != null) {
                    ai = a;
                }
            }


            if (ai == null) {
                done = true;
            } else {
                i += ai.length();

                // if the length is not obvious from the AI, look for the termination character
                boolean foundTerm = false;
                Integer l = aiLengths.get(ai);
                if (l == 0) {
                    int c = 1;
                    while (!foundTerm && (i + c < bytes.length)) {
                        String a = String.valueOf(bytes[i + c]);
                        if (a.equals("29")) {
                            foundTerm = true;
                        } else {
                            c++;
                        }
                    }
                    l = c;
                }

                String contentData = "";
                for (int j = 0; j < l; j++) {
                    contentData += String.valueOf((char) (bytes[i + j] & 0xFF));
                }
                this.ais.put(ApplicationIdentifiers.names.get(ai), contentData);
                i += l;

                // if a termination character was found, skip it before reading
                // the next barcode
                if (foundTerm) i++;
            }
            if (i == bytes.length) {
                done = true;
            }
        }
    }

    public boolean isGlobalTradeItemNumber() {
        return ais.containsKey("gtin");
    }

    public boolean isSSCC() {
        return ais.containsKey("sscc");
    }

    public String getGlobalTradeItemNumber() {
        if (isGlobalTradeItemNumber()) {
            return this.ais.get("gtin");
        }
        return "";
    }

    public String getLot() {
        return this.ais.containsKey("batchLot") ? this.ais.get("batchLot") : "";
    }

    public String getUseThroughDate() {
        return this.ais.containsKey("useThrough") ? this.ais.get("useThrough") : "";
    }

    public String getPackDate() {
        return this.ais.containsKey("packDate") ? this.ais.get("packDate") : "";
    }

    public String getSerialNumber() {
        return this.ais.containsKey("serial") ? this.ais.get("serial") : "";
    }

    public String getQuantity() {
        return this.ais.containsKey("count") ? this.ais.get("count") : "";
    }
}
