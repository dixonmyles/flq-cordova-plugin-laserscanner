package com.bluefletch.motorola.scanhelpers;

import java.util.HashMap;

/**
 * Takes raw bytes from Barcode Scanner app and attempts to parse them into usable data for case
 * data.
 * GS1 Barcodes are encoded/decoded in an odd way. In order to limit byte length, GS1 barcodes
 * utilize
 * a system of three tables that translate byte values to ascii values. Code_C is usually the
 * default
 * starting table. However, when appropriate, GS1 will switch to one of the other tables in order to
 * handle special characters in a more efficient byte sequence. For this reason, there are
 * special byte
 * values that signify a new CODE table is to be used.
 * <p/>
 * There are also special FNC1 characters that are not visible in the barcode, but can be used to
 * sybmolize
 * the end of a variable length Application Identifier value. For instance, if a user created a
 * barcode
 * with the values in this order, (GTIN,LOT,Pack_Date), because Lot is a variable length, there
 * is no
 * programmatic way to determine the end of the LOT and the beginning of Pack_Date. This is where
 * FNC1
 * comes in, as it is present in the byte sequence, but invisible to the user. Note: When
 * possible, a
 * user should always try and move the variable length value to the end of the barcode, to eliminate
 * the need for any extra bytes.
 * <p/>
 * This is all very confusing, but hey, we're just supposed to convert it, not criticize...
 */
public class BarcodeParser {
    private final byte[] bytes;
    private final byte startByte;
    private final byte endByte;
    private final byte fncOneByte;
    private final HashMap<String, String> ais;

    /**
     * Stores fncOneByte (which delimits the end of variable length AI's.
     * Initializes ai hashmap where ai's and their values will be stored.
     *
     * @param bytes Raw bytes from Barcode Scanner app.
     */
    public BarcodeParser(byte[] bytes) {
        this.bytes = bytes;
        this.startByte = bytes[0];
        this.fncOneByte = bytes[1];
        this.endByte = bytes[bytes.length - 1];
        this.ais = new HashMap<String, String>();
        decodeBytes();
    }

    /**
     * Iterates through each byte character, and maps it to the {@link ApplicationIdentifiers}
     * translation.
     * There are multiple cases to account for:
     * <ul>
     * <li>
     * The start byte determines which CODE table to get the translated character from.
     * <ul>
     * <li><b>103</b> indicates that the translation starts in
     * {@link ApplicationIdentifiers#CODE_A}</li>
     * <li><b>104</b> indicates that the translation starts in
     * {@link ApplicationIdentifiers#CODE_B}</li>
     * <li><b>105</b> indicates that the translation starts in
     * {@link ApplicationIdentifiers#CODE_C}</li>
     * </ul>
     * </li>
     * <li>
     * There are multiple checks before adding a byte value to the current AI:
     * <ul>
     * <li>
     * Was the previous byte the start of a 3 or 4-character AI. If true:
     * <ul>
     * <li>
     * Is it present in the tripleAIs (3-character AI's), then take the first character
     * as the completion of the AI, then add the second character as the beginning
     * of the ai's value. Move to the next byte.
     * </li>
     * <li>
     * Is it present in the quadrupleAIs(4-character AI's), then take both characters as the
     * completion of the AI, and move to the next byte.
     * </li>
     * </ul>
     * </li>
     * <li>
     * Is the byte a code switching byte. This means that we are switching which CODE table we
     * translate our next bytes with. If true, set the new code as the current code field.
     * </li>
     * <li>
     * If it's the second or third to last byte, we know we've reached the end of the relevant
     * sequence, because the last 3 or 2 bytes are used for the barcode reader and not relevant
     * to the data we need.
     * </li>
     * <li>
     * If the byte is a FNC1 byte, then we know the next byte is going to be a new AI bytecode.
     * Set the flag and continue.
     * </li>
     * <li>
     * If the byte has specifically be denoted as an ai code byte, or the current AI has reached
     * it's maximum limit, The byte being read in is a new AI code, so store the current AI value
     * and read in the new AI code to set a different current AI.
     * </li>
     * <li>
     * If none of the above has been satisfied, and the byte is not the starting byte of the
     * sequence,
     * then it is a piece of the Current AI's value. Add it to the value and move on.
     * </li>
     * </ul>
     * </li>
     * </ul>
     */
    public void decodeBytes() {
        int currentCode = -1;
        switch (String.valueOf((bytes[0] & 0xFF))) {
            case "103":
                currentCode = ApplicationIdentifiers.CODE_A;
                break;
            case "104":
                currentCode = ApplicationIdentifiers.CODE_B;
                break;
            case "105":
                currentCode = ApplicationIdentifiers.CODE_C;
                break;
        }
        boolean done = false;
        int i = 0;
        boolean nextByteIsAI = false;
        /*
         * Support for triple and quadruple character AI's.
         * nextByteIsSecondaryAI: flags the next byte as being part of an AI and not content
         * primaryAI: In the event that a triple or quadruple AI start is initiated, we store that
         * initial byte pair to append it to the next byte, which completes the byte pair.
         */
        boolean nextByteIsSecondaryAI = false;
        String primaryAI = "";
        AI currentAi = null;
        int currentAiMax = -1;
        int currentAiCharCount = 0;

        while (true) {
            String byteValue = String.valueOf((bytes[i] & 0xFF));
            if (byteValue.length() == 1) byteValue = "0" + byteValue;

            //Handle Byte

            if (nextByteIsSecondaryAI) {
                if (ApplicationIdentifiers.tripleAis.contains(primaryAI)) {
                    //First character should be the last character for the AI
                    String aiCode = primaryAI + byteValue.substring(0, 1);
                    currentAi = new AI(ApplicationIdentifiers.names.get(aiCode));
                    //Second value should be first value of the new AI, after it's been mapped
                    String mappedVal = mapCharacterFromByteValue(byteValue, currentCode)
                            .substring(1);
                    currentAi.addToValue(mappedVal);
                    currentAiMax = ApplicationIdentifiers.lengths.get(aiCode);
                } else {
                    //Complete the AI and don't add a value yet, because the next byte starts
                    //the AI's value.
                    byteValue = primaryAI + byteValue;
                    currentAi = new AI(ApplicationIdentifiers.names.get(byteValue));
                    currentAiMax = ApplicationIdentifiers.lengths.get(byteValue);
                }
                nextByteIsSecondaryAI = false;
                primaryAI = "";
            }
            /*
            If the byte is a code switch byte, handle that switch and consume the byte
             */
            else if (byteIsCodeSwitch(currentCode, byteValue)) {
                currentCode = getNewCode(currentCode, byteValue);
            }
            /*
            Because the second (and third if it's a code change byte) to last byte
            are used for the barcode reader we don't really need to know that.
            */
            else if (i >= bytes.length - 2) {
                if (currentAi != null)
                    ais.put(currentAi.aiName, currentAi.aiValue);
                return;
            }
            //if the byte is the fnc1 byte then we know that the next value will be an AI
            else if (bytes[i] == this.fncOneByte) {
                nextByteIsAI = true;
            }
            /*
            If this byte is explicitly set as an AI, or if the current read values are the max
            value for the current ai value, then we store it and read in the next value as an
            ai byte
             */
            else if (nextByteIsAI || (currentAi != null && currentAiMax == currentAi.getAiValue()
                    .length())) {
                if (ApplicationIdentifiers.tripleAis.contains(byteValue) ||
                        ApplicationIdentifiers.quadrupleAis.contains(byteValue)) {
                    // We need to delay reading in data in order to complete the 3/4 byte
                    nextByteIsSecondaryAI = true;
                    primaryAI = byteValue;
                } else {
                    if (currentAi != null) ais.put(currentAi.aiName, currentAi.aiValue);
                    currentAi = new AI(ApplicationIdentifiers.names.get(byteValue));
                    currentAiMax = ApplicationIdentifiers.lengths.get(byteValue);
                }
                nextByteIsAI = false;
            }
            /*
                If we get this far, then the byte is clearly a value for the current AI,
                but only if it isn't the start byte
             */
            else if (bytes[i] != this.startByte && currentAi != null) {
                String mappedVal = mapCharacterFromByteValue(byteValue, currentCode);
                currentAi.addToValue(mappedVal);
            }
            i++;
        }
    }

    /**
     * @param currentCode The current code that is being used to translate bytes.
     * @param byteValue   The byte value indicating that a new CODE table needs to be used.
     * @return The new CODE table constant.
     */
    private int getNewCode(int currentCode, String byteValue) {
        switch (currentCode) {
            case ApplicationIdentifiers.CODE_A:
                if (byteValue.equals("99")) {
                    return ApplicationIdentifiers.CODE_C;
                }
                if (byteValue.equals("100")) {
                    return ApplicationIdentifiers.CODE_B;
                }
                break;
            case ApplicationIdentifiers.CODE_B:
                if (byteValue.equals("99")) {
                    return ApplicationIdentifiers.CODE_C;
                }
                if (byteValue.equals("101")) {
                    return ApplicationIdentifiers.CODE_A;
                }
                break;
            case ApplicationIdentifiers.CODE_C:
                //Trust me, this is supposed to be in this order.
                if (byteValue.equals("101")) {
                    return ApplicationIdentifiers.CODE_A;
                }
                if (byteValue.equals("100")) {
                    return ApplicationIdentifiers.CODE_B;
                }
                break;
        }
        return -1;
    }

    /**
     * @param currentCode Current Code table, to see if the byte value symbolizes CODE switch.
     * @param byteValue   The current byte value, which may or may not be a CODE switch byteValue.
     * @return true if the byte is indicative of an impending CODE switch.
     */
    private boolean byteIsCodeSwitch(int currentCode, String byteValue) {
        switch (currentCode) {
            case ApplicationIdentifiers.CODE_A:
                return byteValue.equals("99") || byteValue.equals("100");
            case ApplicationIdentifiers.CODE_B:
                return byteValue.equals("99") || byteValue.equals("101");
            case ApplicationIdentifiers.CODE_C:
                return byteValue.equals("100") || byteValue.equals("101");
        }
        return false;
    }

    /**
     * Returns the mapped byte value. This is done by looking at the current CODE value, and
     * matching
     * the input byteValue, <b>val</b>, to the CODE value's respective table.
     *
     * @param val         The actual byte code passed in from the barcode scanner.
     * @param currentCode The current CODE table to translate the value from.
     * @return The translated value character from the matching CODE table.
     */
    private String mapCharacterFromByteValue(String val, Integer currentCode) {
        if (currentCode.intValue() != ApplicationIdentifiers.CODE_C) {
            int l = 0;
        }
        HashMap<String, String> currentCodeValues = ApplicationIdentifiers.values.get(currentCode);
        return currentCodeValues.get(val);
    }

    /**
     * Does the current barcode contain a GTIN AI
     *
     * @return True if it does, false if not
     */
    public boolean isGlobalTradeItemNumber() {
        return ais.containsKey("gtin");
    }

    /**
     * Does the current barcode contain a Content GTIN AI
     *
     * @return True if it does, false if not
     */
    public boolean isContentGlobalTradeItemNumber() {
        return ais.containsKey("content");
    }

    /**
     * Does the current barcode contain a SSCC AI
     *
     * @return True if it does, false if not
     */
    public boolean isSSCC() {
        return ais.containsKey("sscc");

    }

    /**
     * @return value of GTIN ai in barcode, if it exists, empty string otherwise.
     */
    public String getGlobalTradeItemNumber() {
        if (isGlobalTradeItemNumber()) {
            return this.ais.get("gtin");
        }
        if (isContentGlobalTradeItemNumber()) {
            return this.ais.get("content");
        }
        return "";
    }

    /**
     * @return value of LOT ai in barcode, if it exists, empty string otherwise.
     */
    public String getLot() {
        return this.ais.containsKey("batchLot") ? this.ais.get("batchLot") : "";
    }

    /**
     * @return value of Use Through Date (as a simple date string) ai in barcode,
     * if it exists, empty string otherwise.
     */
    public String getUseThroughDate() {
        return this.ais.containsKey("useThrough") ? this.ais.get("useThrough") : "";
    }

    /**
     * @return value of Packed Date (as a simple date string) ai in barcode,
     * if it exists, empty string otherwise.
     */
    public String getPackDate() {
        return this.ais.containsKey("packDate") ? this.ais.get("packDate") : "";
    }

    /**
     * @return value of Serial Number ai in barcode, if it exists, empty string otherwise.
     */
    public String getSerialNumber() {
        return this.ais.containsKey("serial") ? this.ais.get("serial") : "";
    }

    /**
     * @return value of Quantity ai in barcode as an int, if it exists, and a 1 if it doesn't (we
     * assume
     * there's at least one in the case, even if the count isn't there).
     */
    public int getQuantity() {
        return this.ais.containsKey("count") ? Integer.parseInt(this.ais.get("count")) : 1;
    }

    /**
     * AI is a class to store an AI name (GTIN for instance) and the value of the AI name.
     */
    private class AI {
        private String aiName;
        private String aiValue;

        public AI(String aiName) {
            this.aiName = aiName;
            this.aiValue = "";
        }

        /**
         * Adds the input value onto the end of the aiValue.
         *
         * @param val new character to add to aiValue
         */
        public void addToValue(String val) {
            this.aiValue += val;
        }

        /**
         * @return value as a string for the AI
         */
        public String getAiValue() {
            return aiValue;
        }
    }
}
