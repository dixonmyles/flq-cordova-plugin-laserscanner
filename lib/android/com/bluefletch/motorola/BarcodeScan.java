package com.bluefletch.motorola;

/**
 * Model class for holding barcode data
 */
public class BarcodeScan {

	public String scanFormat;
	public String barcode;
	public String globalTradeNumber;
	public String lot;
	public String packedDate;
	public String useThroughDate;
	public String serialNumber;
	public Integer quantity;

	public BarcodeScan(String scanFormat,
					   String barcode,
					   String globalTradeNumber,
					   String lot,
					   String packedDate,
					   String useThroughDate,
					   String serialNumber,
					   Integer quantity) {
		this.scanFormat = scanFormat;
		this.barcode = barcode;
		this.globalTradeNumber = globalTradeNumber;
		this.lot = lot;
		this.packedDate = packedDate;
		this.useThroughDate = useThroughDate;
		this.serialNumber = serialNumber;
		this.quantity = quantity;
	}
}
