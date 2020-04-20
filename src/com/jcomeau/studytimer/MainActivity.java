package com.jcomeau.studytimer;
// sample code from https://medium.com/@authmane512/
// how-to-build-an-apk-from-command-line-without-ide-7260e1e22676
// and other samples on StackOverflow and elsewhere
import java.util.Locale;
import android.util.Log;
import android.os.Bundle;
import android.os.SystemClock;
import android.app.AlarmManager;
import android.app.Activity;
import android.app.PendingIntent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.content.IntentFilter;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.pm.ApplicationInfo;
import android.widget.Button;
import android.widget.Chronometer;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;

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
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
    String[] BUTTON_TEXT = {"Stop", "Start"};
    boolean STOPPED;
    AlarmManager alarmManager;
    PendingIntent alarmIntent;
    Context appContext;
    Intent intent;
    TextToSpeech textToSpeech;
    Chronometer chronometer;
    long elapsed;
    BroadcastReceiver alarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(APP, "received " + intent);
            textToSpeech.speak(
                "Are you still studying?",
                TextToSpeech.QUEUE_FLUSH,
                null);
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(APP, "onCreate starting");
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            STOPPED = true;
            Window window = getWindow();
            window.addFlags(SCREEN_ON);
            elapsed = 0;
        } else {
            STOPPED = savedInstanceState.getBoolean("STOPPED", true);
            elapsed = savedInstanceState.getLong("elapsed", 0);
        }
        setContentView(R.layout.activity_main);
        Button button = (Button)findViewById(R.id.start);
        Log.d(APP, "button: " + button);
        Log.d(APP, "Setting button text to " + BUTTON_TEXT[STOPPED ? 1 : 0]);
        button.setText(BUTTON_TEXT[STOPPED ? 1 : 0]);
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
        textToSpeech = new TextToSpeech(getApplicationContext(),
                new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    // a British voice somehow isn't quite so aggravating
                    textToSpeech.setLanguage(Locale.UK);
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        Chronometer chronometer = (Chronometer)findViewById(R.id.chronometer);
        elapsed = milliseconds(chronometer.getText().toString());
        state.putBoolean("STOPPED", STOPPED);
        state.putLong("elapsed", elapsed);
    }

    public long milliseconds(String time) {
        // convert string time to milliseconds
        // https://stackoverflow.com/a/1291253/493161
        String array[] = ("0:" + time).split(":");
        Log.d(APP, "array: " + array);
        long seconds = Integer.parseInt(array[array.length - 1]);
        long minutes = Integer.parseInt(array[array.length - 2]);
        long hours = Integer.parseInt(array[array.length - 3]);
        return ((((hours * 60) + minutes) * 60) + seconds) * 1000;
    }

    public void nag(View view) {
        Button button = (Button)findViewById(view.getId());
        Chronometer chronometer = (Chronometer)findViewById(R.id.chronometer);
        Log.d(APP, "button " + button + " pushed");
        if (STOPPED) {
            Log.d(APP, "start nagging");
            button.setText(BUTTON_TEXT[0]);
            Log.d(APP, "scheduling intent: " + alarmIntent);
	    alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + NAG_INTERVAL,
                NAG_INTERVAL,
	        alarmIntent);
            chronometer.setBase(SystemClock.elapsedRealtime() - elapsed);
            chronometer.start();
        } else {
            Log.d(APP, "stop nagging");
            button.setText(BUTTON_TEXT[1]);
            alarmManager.cancel(alarmIntent);
            chronometer.stop();
            elapsed = milliseconds(chronometer.getText().toString());
        }
        STOPPED = !STOPPED;
    }
}
// vim: tabstop=8 expandtab shiftwidth=4 softtabstop=4
