package dev.bobbucks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.chromium.components.payments.IPaymentDetailsUpdateServiceCallback;
import org.json.JSONException;
import org.json.JSONObject;

public class PaymentActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "PaymentActivity";
    public static final String ACTION_CHANGE_PAYMENT_METHOD = "dev.bobbucks.CHANGE_PAYMENT_METHOD";
    public static final String EXTRA_PAYMENT_HANDLER_METHOD_DATA = "payment_handler_method_data";
    private boolean mError;
    private BroadcastReceiver mUpdateReceiver;
    private Button mContinueButton;
    private Button mSendUpdateButton;
    private TextView mUsernameText;
    private TextView mErrorMessage;
    private TextView mTotalTextView;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The default result is cancelled; this will be set to success instead in the 'Continue'
        // click listener if the user accepts.
        setResultData(RESULT_CANCELED);

        SharedPreferences sharedPref = getSharedPreferences("dev.bobbucks.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE);
        if (sharedPref.getBoolean("show_fails_key", false)) {
            finish();
            return;
        }

        setContentView(R.layout.activity_payment);

        mContinueButton = findViewById(R.id.continue_button);
        mSendUpdateButton = findViewById(R.id.send_update_button);
        mUsernameText = findViewById(R.id.username_text);
        mErrorMessage = findViewById(R.id.error_message);
        mTotalTextView = findViewById(R.id.total_text_view);

        mContinueButton.setOnClickListener(this);
        mSendUpdateButton.setOnClickListener(this);

        String username = sharedPref.getString("username_key", null);
        String welcomeText = "Welcome " + (username != null ? username : "non-signed in user") + "!";
        mUsernameText.setText(welcomeText);

        Intent callingIntent = getIntent();
        if (null == callingIntent) {
            showError("Calling intent is null.");
            return;
        }

        Bundle extras = callingIntent.getExtras();
        if (extras == null) {
            showError("Calling intent contains no extras.");
            return;
        }

        String totalString = extras.getString("total");
        if (TextUtils.isEmpty(totalString)) {
            showError("No total in the extras.");
            return;
        }

        try {
            JSONObject totalJson = new JSONObject(totalString);
            mTotalTextView.setText(String.format("Total %s %s",
                    totalJson.optString("currency"), totalJson.optString("value")));
        } catch (JSONException e) {
            showError("Cannot parse the total into JSON.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Layout the app to be a half-height modal at the bottom of the screen.
            WindowManager.LayoutParams attrs = getWindow().getAttributes();
            int displayHeight = getWindowManager().getCurrentWindowMetrics().getBounds().height();
            attrs.height = (int) (displayHeight * 0.5);
            attrs.width = getWindowManager().getCurrentWindowMetrics().getBounds().width();
            attrs.x = 0;
            attrs.y = 0;
            attrs.gravity = Gravity.BOTTOM;
            getWindow().setAttributes(attrs);
        }

        mUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (PaymentDetailsUpdateServiceCallbackImpl.ACTION_UPDATE_PAYMENT.equals(action)) {
                    Log.i(TAG, "Received ACTION_UPDATE_PAYMENT broadcast");
                    Bundle bundle = intent.getBundleExtra(PaymentDetailsUpdateServiceCallbackImpl.EXTRA_PAYMENT_DETAILS);
                    if (bundle != null) {
                        String error = bundle.getString("error");
                        if (!TextUtils.isEmpty(error)) {
                            // Can't use showError, because that also sets mError=true which isn't
                            // the case here, we're just showing this for demo purposes.
                            mErrorMessage.setText(
                                    "Received message from website: " + error);
                        }
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(PaymentDetailsUpdateServiceCallbackImpl.ACTION_UPDATE_PAYMENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mUpdateReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mUpdateReceiver);
        }
    }

    private void showError(String error) {
        mError = true;
        mErrorMessage.setText(error);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.continue_button:
                Log.d(TAG, "Handling 'continue' button press");
                setResultData(mError ? RESULT_CANCELED : RESULT_OK);
                finish();
                break;
            case R.id.send_update_button:
                Log.i(TAG, "Handling 'send update' button press");
                Intent intent = new Intent(ACTION_CHANGE_PAYMENT_METHOD);
                Bundle paymentHandlerMethodData = new Bundle();
                paymentHandlerMethodData.putString("methodName", "New Method Name");
                intent.putExtra(EXTRA_PAYMENT_HANDLER_METHOD_DATA, paymentHandlerMethodData);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                break;
            default:
                Log.e(TAG, "onClick, unknown view!");
                break;
        }
    }

    private void setResultData(int resultType) {
        Intent result = new Intent();
        Bundle extras = new Bundle();
        extras.putString("methodName", "https://bobbucks.dev/pay");
        extras.putString("instrumentDetails", "{\"token\": \"put-some-data-here\"}");
        result.putExtras(extras);
        setResult(resultType, result);
    }
}
