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
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
    String TAG = "studytimer";
    int NAG_INTERVAL = 6 * 60 * 1000;  // milliseconds
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
        intent = new Intent(context, BroadcastReceiver.class);
        alarmIntent = PendingIntent.getService(context, REQUEST_ID, intent,
                                               PendingIntent.FLAG_NO_CREATE);
        alarmManager =
       	    (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
    public void nag(View view) {
        Log.i(TAG, "start nagging");
	alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + NAG_INTERVAL, NAG_INTERVAL,
	    alarmIntent);
    }
    public void stopNagging() {
	if (alarmIntent != null && alarmManager != null) {
		  alarmManager.cancel(alarmIntent);
	}
    }
}
// vim: tabstop=8 expandtab shiftwidth=4 softtabstop=4
