package dev.bobbucks;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.chromium.components.payments.IPaymentDetailsUpdateService;
import org.chromium.components.payments.IPaymentDetailsUpdateServiceCallback;

public class PaymentDetailsUpdateServiceCallbackImpl extends Service {
    private static final String TAG = "PaymentDetailsUpdateServiceCallbackImpl";

    public static final String ACTION_UPDATE_PAYMENT = "dev.bobbucks.UPDATE_PAYMENT";
    public static final String EXTRA_PAYMENT_DETAILS = "updated_payment_details";

    private IPaymentDetailsUpdateService mChromeService = null;
    private BroadcastReceiver mChangeReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        mChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (PaymentActivity.ACTION_CHANGE_PAYMENT_METHOD.equals(action)) {
                    Log.i(TAG, "Received ACTION_CHANGE_PAYMENT_METHOD broadcast");
                    if (mChromeService == null) {
                        Log.e(TAG, "mChromeService is null, cannot send change payment method request");
                        return;
                    }

                    Bundle paymentHandlerMethodData = intent.getBundleExtra(PaymentActivity.EXTRA_PAYMENT_HANDLER_METHOD_DATA);
                    if (paymentHandlerMethodData == null) {
                        Log.e(TAG, "ACTION_CHANGE_PAYMENT_METHOD intent has no payment handler method data");
                        return;
                    }

                    IPaymentDetailsUpdateServiceCallback.Stub callbackStub = new PaymentDetailsUpdateServiceCallbackImpl().getBinder();
                    try {
                        Log.d(TAG, "Calling into mChromeService.changePaymentMethod");
                        mChromeService.changePaymentMethod(paymentHandlerMethodData, callbackStub);
                    } catch (RemoteException e) {
                        Log.e(TAG, "RemoteException when calling changePaymentMethod:", e);
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(PaymentActivity.ACTION_CHANGE_PAYMENT_METHOD);
        LocalBroadcastManager.getInstance(this).registerReceiver(mChangeReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChangeReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mChangeReceiver);
        }
    }

    private final IPaymentDetailsUpdateServiceCallback.Stub mBinder = new IPaymentDetailsUpdateServiceCallback.Stub() {
        @Override
        public void updateWith(Bundle updatedPaymentDetails) throws RemoteException {
            Log.e(TAG, "updateWith called");
            Log.e(TAG, "\terror: " + updatedPaymentDetails.getString("error"));
            Log.e(TAG, "\tstringifiedPaymentMethodErrors: "
                    + updatedPaymentDetails.getString("stringifiedPaymentMethodErrors"));

            Intent intent = new Intent(ACTION_UPDATE_PAYMENT);
            intent.putExtra(EXTRA_PAYMENT_DETAILS, updatedPaymentDetails);
            LocalBroadcastManager.getInstance(PaymentDetailsUpdateServiceCallbackImpl.this).sendBroadcast(intent);
        }

        @Override
        public void paymentDetailsNotUpdated() throws RemoteException {
            Log.e(TAG, "paymentDetailsNotUpdated called");
        }

        @Override
        public void setPaymentDetailsUpdateService(IPaymentDetailsUpdateService service) throws RemoteException {
            Log.e(TAG, "setPaymentDetailsUpdateService called");
            mChromeService = service;
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public IPaymentDetailsUpdateServiceCallback.Stub getBinder() {
        return mBinder;
    }
}
