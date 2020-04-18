package com.jcomeau.studytimer;
// sample code from https://medium.com/@authmane512/
// how-to-build-an-apk-from-command-line-without-ide-7260e1e22676
// and other samples on StackOverflow
import android.util.Log;
import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.view.View;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
    String TAG = "studytimer";
    // Comment out one of the following. Time in milliseconds
    int NAG_INTERVAL = 1000;  // 1 second when debugging
    //int NAG_INTERVAL = 6 * 60 * 1000;  // normal use
    int REQUEST_ID = 1;
    AlarmManager alarmManager;
    PendingIntent alarmIntent;
    Context context;
    Intent intent;
    Button start;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this.getApplicationContext();
        intent = new Intent(context, AlarmReceiver.class);
        alarmIntent = PendingIntent.getService(context, REQUEST_ID, intent, 0);
        alarmManager =
       	    (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
    public void nag(View view) {
        Button button = (Button)findViewById(view.getId());
        Log.d(TAG, "button " + button + " pushed");
        if (button.getText().equals("Start")) {
            Log.d(TAG, "start nagging");
            button.setText("Stop");
	    alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + NAG_INTERVAL,
                NAG_INTERVAL,
	        alarmIntent);
        } else {
            Log.d(TAG, "stop nagging");
            button.setText("Start");
            alarmManager.cancel(alarmIntent);
        }
    }
}
// vim: tabstop=8 expandtab shiftwidth=4 softtabstop=4
