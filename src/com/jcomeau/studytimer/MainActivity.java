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
import android.view.Window;
import android.view.WindowManager;
import android.content.IntentFilter;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.widget.Button;
import android.media.MediaPlayer;
import android.content.pm.ApplicationInfo;

public class MainActivity extends Activity {
    String APP = "studytimer";
    String PACKAGE = "com.jcomeau." + APP;
    String ACTION = PACKAGE + ".NAG";
    // https://medium.com/@elye.project/
    // checking-debug-build-the-right-way-d12da1098120
    boolean DEBUG;
    // Time is in milliseconds
    // No matter what you set it to, the first alarm comes in no earlier
    // than 4 seconds on Android 6, and the 2nd no earlier than 1 minute
    // after the first. (This is with alarmManager.setRepeating())
    int NAG_INTERVAL;
    int REQUEST = 1;  // request ID
    int SCREEN_ON = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
    AlarmManager alarmManager;
    PendingIntent alarmIntent;
    Context appContext;
    Intent intent;
    Button start;
    BroadcastReceiver alarmReceiver = new BroadcastReceiver() {
        MediaPlayer mediaPlayer;
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(APP, "received " + intent);
            mediaPlayer = mediaPlayer.create(context, R.raw.alarmclock2);
            Window window = getWindow();
            window.clearFlags(SCREEN_ON);
            window.addFlags(SCREEN_ON);
            mediaPlayer.start();
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appContext = getApplicationContext();
        intent = new Intent(ACTION);
        registerReceiver(alarmReceiver, new IntentFilter(ACTION));
        alarmIntent = PendingIntent.getBroadcast(
            appContext, REQUEST, intent, 0);
        alarmManager = (AlarmManager) appContext.getSystemService(
            Context.ALARM_SERVICE);
        DEBUG = ((appContext.getApplicationInfo().flags &
                 ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        if (DEBUG) {
            NAG_INTERVAL = 60 * 1000;  // 1 minute when debugging
        } else {
            NAG_INTERVAL = 6 * 60 * 1000;  // .1 hour (6 minutes) for normal use
        }
        Log.d(APP, "DEBUG=" + DEBUG);
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
