package dev.bobbucks;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.chromium.IsReadyToPayService;
import org.chromium.IsReadyToPayServiceCallback;
import org.json.JSONException;
import org.json.JSONObject;

public class IsReadyToPayServiceImpl extends Service {
    private static final String TAG = "IsReadyToPayServiceImpl";

    private final IsReadyToPayService.Stub mBinder =
            new IsReadyToPayService.Stub() {
                @Override
                public void isReadyToPay(IsReadyToPayServiceCallback callback, Bundle parameters) {
                    Log.d(TAG, "isReadyToPay called");
                    Log.d(TAG, "parameters: " + (parameters == null ? "<null>" : parameters.toString()));

                    // For testing, this sample app allows the website to directly override what
                    // the return value of IS_READY_TO_PAY will be. In practice, you should
                    // determine this based on internal app state (e.g., is the current user logged
                    // in, do they have a payment method on file, etc).
                    boolean returnValue = true;
                    if (parameters != null) {
                        Log.d(TAG, "parameters keys: " + parameters.keySet().toString());
                        Bundle methodData = parameters.getBundle("methodData");
                        if (methodData != null) {
                            Log.d(TAG, "methodData keys: " + methodData.keySet().toString());
                            String bobBucksMethodData = methodData.getString("https://bobbucks.dev/pay");
                            if (bobBucksMethodData != null) {
                                Log.d(TAG, "BobBucks method data: " + bobBucksMethodData);
                                try {
                                    JSONObject dataObject = new JSONObject(bobBucksMethodData);
                                    if (dataObject.has("returnValue")) {
                                        try {
                                            returnValue = dataObject.getBoolean("returnValue");
                                            Log.d(TAG, "Overriding return value to " + returnValue);
                                        } catch (JSONException e) {
                                            Log.d(TAG, "'returnValue' parameter was not a boolean: " + bobBucksMethodData);
                                        }
                                    } else {
                                        Log.d(TAG, "no 'returnValue' present");
                                    }
                                } catch (JSONException e) {
                                    Log.e(TAG, "Unable to parse BobBucks methodData as JSON: " + bobBucksMethodData);
                                }
                            } else {
                                Log.d(TAG, "No 'https://bobbucks.dev/pay' method data inside methodData");
                            }
                        } else {
                            Log.e(TAG, "'methodData' was null!");
                        }
                    }

                    // Check permission here.
                    try {
                        callback.handleIsReadyToPay(returnValue);
                    } catch (RemoteException e) {
                        // Ignore.
                    }
                }
            };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
