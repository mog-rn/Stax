package com.hover.stax.requests;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "requests")
public class Request {

	@PrimaryKey(autoGenerate = true)
	@NonNull
	public int id;

	@NonNull
	@ColumnInfo(name = "recipient")
	public String recipient;

	@ColumnInfo(name = "amount")
	public String amount;

	@ColumnInfo(name = "reason")
	public String reason;

	@NonNull
	@ColumnInfo(name = "message")
	public String message;

	@ColumnInfo(name = "matched_transaction_uuid")
	public String matched_transaction_uuid;

	@NonNull
	@ColumnInfo(name = "date_sent", defaultValue = "CURRENT_TIMESTAMP")
	public Long date_sent;
}
