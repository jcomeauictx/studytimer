package com.jcomeau.studytimer;
// sample code from https://medium.com/@authmane512/
// how-to-build-an-apk-from-command-line-without-ide-7260e1e22676
// and other samples on StackOverflow and elsewhere
import android.util.Log;
import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.view.View;
import android.content.IntentFilter;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.widget.Button;
import android.widget.Toast;
import android.media.MediaPlayer;

public class MainActivity extends Activity {
    String APP = "studytimer";
    String PACKAGE = "com.jcomeau." + APP;
    String ACTION = PACKAGE + ".NAG";
    // Comment out one of the following. Time in milliseconds
    // No matter what you set it to, the first alarm comes in no earlier
    // than 4 seconds on Android 6, and the 2nd no earlier than 1 minute after
    // the first.
    //int NAG_INTERVAL = 10 * 1000;  // 10 seconds when debugging
    int NAG_INTERVAL = 6 * 60 * 1000;  // normal use
    int REQUEST = 1;  // request ID
    AlarmManager alarmManager;
    PendingIntent alarmIntent;
    Context context;
    Intent intent;
    Button start;
    BroadcastReceiver alarmReceiver = new BroadcastReceiver() {
        MediaPlayer mediaPlayer;
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(APP, "received " + intent);
            mediaPlayer = mediaPlayer.create(context, R.raw.alarmclock2);
            mediaPlayer.start();
            Toast.makeText(context,
                "Are you still studying?",
                Toast.LENGTH_LONG).show();
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this.getApplicationContext();
        intent = new Intent(ACTION);
        registerReceiver(alarmReceiver, new IntentFilter(ACTION));
        alarmIntent = PendingIntent.getBroadcast(MainActivity.this, REQUEST, intent, 0);
        alarmManager = (AlarmManager) context.getSystemService(
            Context.ALARM_SERVICE);
    }
    public void nag(View view) {
        Button button = (Button)findViewById(view.getId());
        Log.d(APP, "button " + button + " pushed");
        if (button.getText().equals("Start")) {
            Log.d(APP, "start nagging");
            button.setText("Stop");
            Log.d(APP, "scheduling intent: " + alarmIntent);
	    alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + NAG_INTERVAL,
                NAG_INTERVAL,
	        alarmIntent);
        } else {
            Log.d(APP, "stop nagging");
            button.setText("Start");
            alarmManager.cancel(alarmIntent);
        }
    }
}
// vim: tabstop=8 expandtab shiftwidth=4 softtabstop=4
