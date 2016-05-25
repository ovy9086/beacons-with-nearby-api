package com.corebuild.beacons;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageFilter;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.NearbyMessagesStatusCodes;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "BeaconsActivity";
    private static final int RESOLVE_USER_CONSENT = 111222;
    private GoogleApiClient googleApiClient;
    private boolean resolvingError = false;

    private TextView logText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logText = (TextView) findViewById(R.id.textViewLog);
        findViewById(R.id.subscribeBackgroundBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backgroundSubscribe();
            }
        });
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addApi(Nearby.CONNECTIONS_API)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESOLVE_USER_CONSENT) {
            if (resultCode == RESULT_OK) {
                log("User consent granted...");
                subscribe();
            } else {
                log("User not granted consent !");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Connected...");
        Nearby.Messages.getPermissionStatus(googleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                log("Status : " + status.getStatusMessage() + " Code : " + status.getStatusCode());
                if (status.getStatusCode() == NearbyMessagesStatusCodes.APP_NOT_OPTED_IN) {
                    log("Requiring permission");
                    handleUserNotGrantedConsent(status);
                } else if (status.getStatusCode() == NearbyMessagesStatusCodes.SUCCESS) {
                    log("Permission granted...");
                    subscribe();
                }
            }
        });
    }

    private void handleUserNotGrantedConsent(Status status) {
        log("User not granted consent...");
        if (resolvingError) {
            // Already attempting to resolve an error.
            return;
        }
        if (status.hasResolution()) {
            try {
                status.startResolutionForResult(this, RESOLVE_USER_CONSENT);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            if (status.getStatusCode() == CommonStatusCodes.NETWORK_ERROR) {
                Toast.makeText(getApplicationContext(),
                        "No connectivity, cannot proceed. Fix in 'Settings' and try again.",
                        Toast.LENGTH_LONG).show();
            } else {
                // To keep things simple, pop a toast for all other error messages.
                Toast.makeText(getApplicationContext(), "Unsuccessful: " +
                        status.getStatusMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void subscribe() {
        log("Subscribing...");
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(Strategy.BLE_ONLY)
                .setFilter(MessageFilter.INCLUDE_ALL_MY_TYPES)
                .setCallback(new SubscribeCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        log("Subscription expired...");
                    }
                })
                .build();
        Nearby.Messages.subscribe(googleApiClient, new MessageListener() {
            @Override
            public void onFound(Message message) {
                log("Found : " + message);
            }

            @Override
            public void onLost(Message message) {
                log("Lost : " + message);
            }
        }, options).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                log("Subscription result : " + status.getStatus());
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        log("Connection failed.");
    }

    private void log(String msg) {
        Log.d(TAG, msg);
        logText.append(msg + "\n");
    }

    private void backgroundSubscribe() {
        Nearby.Messages.subscribe(googleApiClient, getPendingIntent(), new SubscribeOptions.Builder()
                .setStrategy(Strategy.BLE_ONLY)
                .build()).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                log("Background subscribe result : " + status);
            }
        });
    }

    private PendingIntent getPendingIntent() {
        return PendingIntent.getService(this, 0,
                getBackgroundSubscribeServiceIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Intent getBackgroundSubscribeServiceIntent() {
        return new Intent(this, BackgroundSubscribeIntentService.class);
    }
}
