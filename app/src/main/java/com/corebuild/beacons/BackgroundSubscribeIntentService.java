package com.corebuild.beacons;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;

/**
 * Created by ovidiu.latcu on 5/25/2016.
 */
public class BackgroundSubscribeIntentService extends IntentService {

    public BackgroundSubscribeIntentService() {
        super(BackgroundSubscribeIntentService.class.getSimpleName());
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Nearby.Messages.handleIntent(intent, new MessageListener() {
                @Override
                public void onFound(Message message) {
                    showFoundNotification(message);
                }

                @Override
                public void onLost(Message message) {
                    showLostNotification(message);
                }
            });
        }
    }

    private void showLostNotification(Message message) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(1, new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.star_on)
                .setContentTitle("Lost beacon")
                .setContentText(message.toString()).build());
    }

    private void showFoundNotification(Message message) {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(1, new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.star_on)
                .setContentTitle("Found beacon")
                .setContentText(message.toString())
                .setStyle(
                        new android.support.v4.app.NotificationCompat.BigTextStyle().bigText("ID:" + message))
                .build());
    }


}
