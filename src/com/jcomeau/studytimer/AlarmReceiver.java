package com.jcomeau.studytimer;

import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {
    MediaPlayer mediaPlayer;
    String TAG = "studytimer";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "received " + intent);
        mediaPlayer = mediaPlayer.create(context, R.raw.alarmclock2);
        mediaPlayer.start();
        Toast.makeText(context,
            "Are you still studying?",
            Toast.LENGTH_LONG).show();
    }
}
// vim: tabstop=8 expandtab shiftwidth=4 softtabstop=4
