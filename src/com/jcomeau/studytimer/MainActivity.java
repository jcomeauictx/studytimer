package com.jcomeau.studytimer;
// sample code from https://medium.com/@authmane512/
// how-to-build-an-apk-from-command-line-without-ide-7260e1e22676
// and other samples on StackOverflow and elsewhere
import java.util.Locale;
import android.util.Log;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Environment;
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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import java.io.File;

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
    Button studyButton;
    Button listenButton;
    String[] STUDY_BUTTON_TEXT = {"Stop", "Study"};
    String[] LISTEN_BUTTON_TEXT = {"Stop", "Listen"};
    AlarmManager alarmManager;
    PendingIntent alarmIntent;
    Context appContext;
    Intent intent;
    TextToSpeech textToSpeech;
    Chronometer chronometer;
    long elapsed;
    String active;  // button currently active if any
    String version;
    Environment environment;
    File internalFiles;
    File externalFiles;
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
            elapsed = 0;
            active = null;
        } else {
            elapsed = savedInstanceState.getLong("elapsed", 0);
            active = savedInstanceState.getString("active", null);
        }
        setContentView(R.layout.activity_main);
        studyButton = (Button)findViewById(R.id.study);
        listenButton = (Button)findViewById(R.id.listen);
        Log.d(APP, "buttons: " + studyButton + ", " + listenButton);
        chronometer = (Chronometer)findViewById(R.id.chronometer);
        try {
            PackageInfo packageInfo = this.getPackageManager()
                .getPackageInfo(getPackageName(), 0);
            version = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException problem) {
            Log.e(APP, "Error: " + problem);
            version = "unknown";
        }
        Log.d(APP, "version: " + version);
        if (active == "study") {
            studyButton.setText(STUDY_BUTTON_TEXT[0]);
            listenButton.setVisibility(View.GONE);
        } else if (active == "listen") {
            listenButton.setText(LISTEN_BUTTON_TEXT[0]);
            studyButton.setVisibility(View.GONE);
        } else {
            studyButton.setText(STUDY_BUTTON_TEXT[1]);
            listenButton.setText(LISTEN_BUTTON_TEXT[1]);
        }
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
            NAG_INTERVAL = 90 * 1000;  // 1.5 minutes when debugging
        } else {
            NAG_INTERVAL = 6 * 60 * 1000;  // .1 hour (6 minutes) for normal use
        }
        Log.d(APP, "DEBUG=" + DEBUG);
        if (elapsed > 0) {
            chronometer.setBase(SystemClock.elapsedRealtime() - elapsed);
        }
        if (active != null) {
            Log.d(APP, "restarting chronometer");
            chronometer.start();
        }
        getWindow().addFlags(SCREEN_ON);
        environment = new Environment();
        internalFiles = appContext.getExternalFilesDir(null);
        Log.d(APP, "Internal files path: " + internalFiles);
        externalFiles = environment.getExternalStorageDirectory();
        Log.d(APP, "External files path: " + externalFiles);
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
    protected void onDestroy() {
        unregisterReceiver(alarmReceiver);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        Log.d(APP, "saving instance state");
        elapsed = milliseconds(chronometer.getText().toString());
        state.putLong("elapsed", elapsed);
        state.putString("active", active);
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
        Button other = (Button)findViewById(R.id.listen);
        Log.d(APP, "button " + button + " pushed");
        if (active == null) {
            active = "study";
            other.setVisibility(View.GONE);
            Log.d(APP, "start nagging");
            button.setText(STUDY_BUTTON_TEXT[0]);
            Log.d(APP, "scheduling intent: " + alarmIntent);
	    alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + NAG_INTERVAL,
                NAG_INTERVAL,
	        alarmIntent);
            chronometer.setBase(SystemClock.elapsedRealtime() - elapsed);
            chronometer.start();
        } else {
            active = null;
            other.setVisibility(View.VISIBLE);
            Log.d(APP, "stop nagging");
            button.setText(STUDY_BUTTON_TEXT[1]);
            alarmManager.cancel(alarmIntent);
            chronometer.stop();
            elapsed = milliseconds(chronometer.getText().toString());
        }
    }

    public void listen(View view) {
        Button button = (Button)findViewById(view.getId());
        Button other = (Button)findViewById(R.id.study);
        Log.d(APP, "button " + button + " pushed");
        if (active == null) {
            active = "listen";
            other.setVisibility(View.GONE);
            Log.d(APP, "start listening");
            button.setText(LISTEN_BUTTON_TEXT[0]);
            chronometer.setBase(SystemClock.elapsedRealtime() - elapsed);
            chronometer.start();
        } else {
            active = null;
            other.setVisibility(View.VISIBLE);
            Log.d(APP, "stop listening");
            button.setText(LISTEN_BUTTON_TEXT[1]);
            chronometer.stop();
            elapsed = milliseconds(chronometer.getText().toString());
        }
    }
}
// vim: tabstop=8 expandtab shiftwidth=4 softtabstop=4
