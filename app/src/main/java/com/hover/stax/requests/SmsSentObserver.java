package com.hover.stax.requests;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.telephony.PhoneNumberUtils;

import com.hover.stax.R;
import com.hover.stax.contacts.StaxContact;
import com.hover.stax.utils.AnalyticsUtil;

import java.util.List;

import timber.log.Timber;

public class SmsSentObserver extends ContentObserver {
    private static final Uri uri = Uri.parse("content://sms/");

    private static final int MESSAGE_TYPE_SENT = 2;
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_ADDRESS = "address";
    private static final String[] PROJECTION = {COLUMN_ADDRESS, COLUMN_TYPE};
    final private SmsSentListener listener;
    final private List<StaxContact> recipients;
    final private String successMsg;
    private ContentResolver resolver;
    private boolean wasSent = false;
    private final Context context;

    public SmsSentObserver(SmsSentListener l, List<StaxContact> contacts, Handler handler, Context c) {
        super(handler);

        this.resolver = c.getContentResolver();
        this.listener = l;
        this.recipients = contacts;
        this.context = c;

        successMsg = c.getString(R.string.sms_sent_success);
    }

    public void start() {
        resolver.registerContentObserver(uri, true, this);
    }

    public void stop() {
        resolver.unregisterContentObserver(this);
        resolver = null;
    }

    private void callBack() {
        listener.onSmsSendEvent(wasSent);
        stop();
    }

    @Override
    public void onChange(boolean selfChange) {
        if (wasSent) return;

        try (Cursor cursor = resolver.query(uri, PROJECTION, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                final String address = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS));
                final int type = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TYPE));
                for (StaxContact c : recipients) {
                    if (PhoneNumberUtils.compare(address, c.accountNumber) && type == MESSAGE_TYPE_SENT) {
                        wasSent = true;
                        callBack();

                        AnalyticsUtil.logAnalyticsEvent(successMsg, context);

                        break;
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e, "FAILURE");
        }
    }

    public interface SmsSentListener {
        void onSmsSendEvent(boolean sent);
    }
}
