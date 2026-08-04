package dev.bobbucks;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.chromium.IsReadyToPayService;
import org.chromium.IsReadyToPayServiceCallback;

public class IsReadyToPayServiceImpl extends Service {
    private static final String TAG = "IsReadyToPayServiceImpl";

    private final IsReadyToPayService.Stub mBinder =
            new IsReadyToPayService.Stub() {
                @Override
                public void isReadyToPay(IsReadyToPayServiceCallback callback, Bundle parameters) {
                    Log.d(TAG, "isReadyToPay called");

                    // For testing, this sample app allows the website to directly override what
                    // the return value of IS_READY_TO_PAY will be. In practice, you should
                    // determine this based on internal app state (e.g., is the current user logged
                    // in, do they have a payment method on file, etc).
                    String methodDataJson = findCurrentMethodData(parameters);
                    IsReadyToPayData.Result result = IsReadyToPayData.parse(methodDataJson);
                    Log.i(
                            TAG,
                            IsReadyToPayData.LOG_PREFIX
                                    + " method=" + IsReadyToPayData.METHOD_IDENTIFIER
                                    + " status=" + result.status
                                    + " testField=" + logValue(result.testField)
                                    + " returnValue=" + logValue(result.suppliedReturnValue)
                                    + " callbackResult=" + result.returnValue);

                    // Check permission here.
                    if (callback == null) {
                        Log.e(TAG, IsReadyToPayData.LOG_PREFIX + " callback=<null>");
                        return;
                    }
                    try {
                        callback.handleIsReadyToPay(result.returnValue);
                    } catch (RemoteException e) {
                        // Ignore.
                    }
                }
            };

    private static String findCurrentMethodData(Bundle parameters) {
        if (parameters == null
                || !IsReadyToPayData.methodNamesAllowCurrentMethod(parameters.get("methodNames"))) {
            return null;
        }

        Object rawMethodData = parameters.get("methodData");
        if (!(rawMethodData instanceof Bundle)) {
            return null;
        }

        Object value = ((Bundle) rawMethodData).get(IsReadyToPayData.METHOD_IDENTIFIER);
        return value instanceof String ? (String) value : null;
    }

    private static String logValue(String value) {
        if (value == null) {
            return "<null>";
        }
        return '"' + value.replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n") + '"';
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
