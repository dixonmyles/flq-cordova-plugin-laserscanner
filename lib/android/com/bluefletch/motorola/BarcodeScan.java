package com.bluefletch.motorola;

/**
 * Simple class for holding barcode data
 */
public class BarcodeScan {
	public String LabelType;
	public String Barcode;
	public String Gtin;
	public String Lot;
	public String Quantity;
	public String UseThroughDate;
	public String PackedDate;

	public BarcodeScan (String label, String code, String gtin, String lot, String quantity, String useThroughDate, String packedDate){
		this.LabelType = label;
		this.Barcode = code;
		this.Gtin = gtin;
		this.Lot = lot;
		this.Quantity = quantity;
		this.UseThroughDate = useThroughDate;
		this.PackedDate = packedDate;
	}
}
