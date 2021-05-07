package com.rikucherry.backgroundcountdowntimer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import static com.rikucherry.backgroundcountdowntimer.Constants.ACTION_DELETE_NOTIFICATION;
import static com.rikucherry.backgroundcountdowntimer.Constants.TIMER_CHANNEL_ID;
import static com.rikucherry.backgroundcountdowntimer.Constants.TIMER_CHANNEL_NAME;
import static com.rikucherry.backgroundcountdowntimer.Constants.TIMER_ID;

public class TimerExpiredReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(TIMER_CHANNEL_ID, TIMER_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setDescription("Notification from Timer");
            manager.createNotificationChannel(channel);
        }


        Intent contentIntent = new Intent(context, MainActivity.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(context, TIMER_ID, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent deleteIntent = new Intent(ACTION_DELETE_NOTIFICATION);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, TIMER_ID, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, TIMER_CHANNEL_ID);
        builder.setContentTitle("Timer Expired!!")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(contentPendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setAutoCancel(true);

        manager.notify(TIMER_ID, builder.build());
    }
}