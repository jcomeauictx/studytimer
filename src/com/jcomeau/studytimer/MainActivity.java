package com.jcomeau.studytimer;
// sample code from https://medium.com/@authmane512/
// how-to-build-an-apk-from-command-line-without-ide-7260e1e22676
// and other samples on StackOverflow and elsewhere
// NOTE: Android 19 does not support String.join()!
import java.util.Locale;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
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
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.speech.tts.TextToSpeech;
import java.io.File;

public class MainActivity extends Activity implements OnCompletionListener,
        OnItemSelectedListener {
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
    // Following two Lists must be identical length
    // SELECTIONS[0] = 0 for no particular reason, it's just a placeholder
    // SELECTIONS[4] = 0 to indicate no more spinners after R.id.classes
    List<Integer> SELECTIONS = Arrays.asList(
        0,
        R.id.schools,
        R.id.years,
        R.id.classes,
        0
    );
    List<String> DIRECTORY = Arrays.asList("", "", "", "", "");
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
    String[] schools, years, classes, media;
    Spinner selectSchool, selectYear, selectClass;
    MediaPlayer player;
    int mediaIndex;
    int mediaOffset;
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
            mediaIndex = 0;
            mediaOffset = 0;
        } else {
            elapsed = savedInstanceState.getLong("elapsed", 0);
            active = savedInstanceState.getString("active", null);
            mediaIndex = savedInstanceState.getInt("mediaIndex", 0);
            mediaOffset = savedInstanceState.getInt("mediaOffset", 0);
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
            Log.e(APP, "error: " + problem);
            version = "unknown";
        }
        Log.d(APP, "version: " + version);
        if (active == "study") {
            studyButton.setText(STUDY_BUTTON_TEXT[0]);
            listenButton.setText(LISTEN_BUTTON_TEXT[1]);
            listenButton.setVisibility(View.GONE);
        } else if (active == "listen") {
            listenButton.setText(LISTEN_BUTTON_TEXT[0]);
            studyButton.setText(STUDY_BUTTON_TEXT[1]);
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
        DIRECTORY.set(0, appContext.getExternalFilesDir(null).toString());
        this.onItemSelected(null, null, 0, 0);
        player = new MediaPlayer();
        player.setOnCompletionListener(this);
        if (active == "listen") {
            try {
                play();
                Log.d(APP, "restarting play where left off.");
            } catch (Exception problem) {
                Log.e(APP, "listen on recreate failed: " + problem);
                // stop the clock if there was an error
                listen(findViewById(R.id.listen));
            }
        }
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
        try {
            player.stop();
        } catch (java.lang.IllegalStateException ignored) {
            Log.d(APP, "no need to stop player");
        }
        try {
            player.reset();
        } catch (java.lang.IllegalStateException ignored) {
            Log.d(APP, "no need to reset player");
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        Log.d(APP, "saving instance state");
        elapsed = milliseconds(chronometer.getText().toString());
        state.putLong("elapsed", elapsed);
        state.putString("active", active);
        state.putInt("mediaIndex", mediaIndex);
        if (player != null && player.isPlaying()) {
            Log.d(APP, "saving current position of " + player);
            try {
                state.putInt("mediaOffset", player.getCurrentPosition());
            } catch (java.lang.IllegalStateException error) {
                Log.e(APP, "cannot save player position: " + error);
            }
        } else {
            state.putInt("mediaOffset", mediaOffset);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
            long id) {
        // This will be called on initialization of each spinner, and on
        // each user selection.
        int spinner = parent == null ? 0 : parent.getId();
        int index = SELECTIONS.indexOf(spinner);
        File directory;
        ArrayAdapter<String> adapter;
        String[] listing;
        Spinner child = null;
        Log.d(APP, "onItemSelected called, SELECTIONS=" +
              SELECTIONS.toString() +
              ", parent=" + spinner +
              ", index=" + index +
              ", view=" + view +
              ", position=" + position +
              ", id=" + id);
        // reinitialize next downstream spinner
        if (parent != null) DIRECTORY.set(
            index, (String)parent.getItemAtPosition(position));
        directory = new File(join(
            File.separator, DIRECTORY.subList(0, index + 1)));
        Log.d(APP, "files path: " + directory +
              " is directory: " + directory.isDirectory() +
              " is readable: " + directory.canRead());
        listing = directory.list();
        if (listing == null || listing.length == 0) {
            Log.d(APP, "no files found in " + directory.toString());
        } else if (SELECTIONS.get(index + 1) == 0) {
            // populate global `media`
            media = listing; Arrays.sort(media);
            Log.d(APP, "found " + media.length + " media files at " +
                  directory.toString());
            mediaIndex = 0;
            mediaOffset = 0;
        } else {
            Log.d(APP, "first selection: " + listing[0]);
            adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, listing);
            adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
            child = (Spinner)findViewById(SELECTIONS.get(index + 1));
            child.setAdapter(adapter);
            // the following forces a bubbling down of calls to this routine
            child.setOnItemSelectedListener(this);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.d(APP, "onNothingSelected from AdapterView " + parent.getId());
    }

    public String join(String separator, String[] pieces) {
        String joined = null;
        if (pieces.length > 0) joined = "";
        for (int i = 0; i < pieces.length; i++) {
            joined += pieces[i];
            if (i < pieces.length - 1) joined += separator;
        }
        return joined;
    }

    public String join(String separator, List pieces) {
        return join(separator, (String[])pieces.toArray(new String[0]));
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

    public void play() {
        try {
            String directory = join(File.separator, DIRECTORY);
            Log.d(APP, "DIRECTORY=" + DIRECTORY.toString() + ", directory=" +
                  directory);
            String path = join(File.separator,
                new String[] {directory, media[mediaIndex]});
            Log.d(APP, "setting path of player " + player + " to " + path);
            player.setDataSource(path);
            Log.d(APP, "preparing player");
            player.prepare();
            if (mediaOffset > 0) player.seekTo(mediaOffset);
            Log.d(APP, "starting play of " + media[mediaIndex] +
                  " at position " + mediaOffset);
            player.start();
        } catch (IllegalStateException | IOException error) {
            Log.e(APP, "failed to play media: " + error);
            listen(findViewById(R.id.listen)); // toggle state back to idle
        }
    }

    public void onCompletion(MediaPlayer player) {
        Log.d(APP, "play of audio file completed");
        mediaOffset = 0;  // shouldn't have to do this, but sometimes
        // following file starts at previous offset
        player.seekTo(mediaOffset);
        if (mediaIndex < media.length - 1) {
            player.stop();
            player.reset();
            mediaIndex += 1;
            play();
        } else {
            mediaIndex = 0;  // next time start from the beginning
            textToSpeech.speak(
                "End of audio content for the " +
                    selectClass.getSelectedItem().toString() + " class.",
                TextToSpeech.QUEUE_FLUSH,
                null);
            listen(findViewById(R.id.listen)); // toggle state back to idle
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
            try {
                play();
            } catch (Exception problem) {
                Log.e(APP, "listen failed: " + problem);
                listen(view);  // stop the clock if there was an error
            }
        } else {
            active = null;
            other.setVisibility(View.VISIBLE);
            Log.d(APP, "stop listening");
            button.setText(LISTEN_BUTTON_TEXT[1]);
            chronometer.stop();
            elapsed = milliseconds(chronometer.getText().toString());
            player.stop();
            mediaOffset = player.getCurrentPosition();
            Log.d(APP, "current position: " + mediaOffset);
            player.reset();  // idle state needed for setDataSource()
        }
    }
}
// vim: tabstop=8 expandtab shiftwidth=4 softtabstop=4
