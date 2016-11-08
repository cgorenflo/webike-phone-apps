package com.sensordc;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Calendar;

public class SensorDCService extends IntentService {

    private static final String TAG = SensorDCService.class.getSimpleName();

    public SensorDCService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Toast.makeText(this, "SensorDC service started", Toast.LENGTH_LONG).show();
        try {
            SetAutoBootAndDisableBootAudio();
            SetAlarms();
            Toast.makeText(this, "SensorDC service stopped", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            SensorDCLog.e(TAG, Log.getStackTraceString(e));
            Toast.makeText(this, "SensorDC service failed", Toast.LENGTH_LONG).show();
        } finally {
            MainReceiver.completeWakefulIntent(intent);
        }
    }

    private void SetAutoBootAndDisableBootAudio() throws IOException, InterruptedException {
        Log.d(TAG, "SetAutoBoot ");

        final String command1 = "mount -o remount,rw /system";
        Process proc = Runtime.getRuntime().exec(new String[]{"su", "-c", command1});
        proc.waitFor();

        final String command2 = "echo '#!/system/bin/sh' > /sdcard/playlpm";
        proc = Runtime.getRuntime().exec(new String[]{"su", "-c", command2});
        proc.waitFor();

        final String command3 = "echo '/system/bin/reboot' >> /sdcard/playlpm";
        proc = Runtime.getRuntime().exec(new String[]{"su", "-c", command3});
        proc.waitFor();

        // We have now constructed a reboot script at /sdcard/playlpm
        // We now replace /system/bin/playlpm with /sdcard/playlpm
        final String command4 = "cp /system/bin/playlpm /sdcard/playlpm.bak";
        proc = Runtime.getRuntime().exec(new String[]{"su", "-c", command4});
        proc.waitFor();

        final String command5 = "cp /sdcard/playlpm /system/bin/playlpm";
        proc = Runtime.getRuntime().exec(new String[]{"su", "-c", command5});
        proc.waitFor();

        final String command6 = "chmod 0755 /system/bin/playlpm";
        proc = Runtime.getRuntime().exec(new String[]{"su", "-c", command6});
        proc.waitFor();

        final String command7 = "chown root.shell /system/bin/playlpm";
        proc = Runtime.getRuntime().exec(new String[]{"su", "-c", command7});
        proc.waitFor();

        // The above steps work on Android 4.3 but on Android 4.4
        // For Android 4.4 we need the following steps
        //We replace /system/bin/lpm with /sdcard/playlpm

        final String command8 = "cp /system/bin/lpm /sdcard/lpm.bak";
        proc = Runtime.getRuntime().exec(new String[]{"su", "-c", command8});
        proc.waitFor();

        final String command9 = "cp /sdcard/playlpm /system/bin/lpm";
        proc = Runtime.getRuntime().exec(new String[]{"su", "-c", command9});
        proc.waitFor();

        final String command10 = "chmod 0755 /system/bin/lpm";
        proc = Runtime.getRuntime().exec(new String[]{"su", "-c", command10});
        proc.waitFor();

        final String command11 = "chown root.shell /system/bin/lpm";
        proc = Runtime.getRuntime().exec(new String[]{"su", "-c", command11});
        proc.waitFor();


        // Disabling boot up sound
        final String command12 =
                "mv /system/media/audio/ui/PowerOn.ogg /system/media/audio/ui/PowerOn.ogg" + "" + ".disabled";
        proc = Runtime.getRuntime().exec(new String[]{"su", "-c", command12});
        proc.waitFor();
    }

    private void SetAlarms() {
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(ALARM_SERVICE);

        SensorDCLog.i(TAG, "Setting reboot alarm");
        SetRebootAlarm(alarmManager);

        SensorDCLog.i(TAG, "Setting data collection alarm");
        setRepeatingAlarm(alarmManager, DataCollectionAlarmReceiver.class,
                getResources().getInteger(R.integer.dataCollectionIntervalInMS), 101);

        SensorDCLog.i(TAG, "Setting data upload alarm");
        setRepeatingAlarm(alarmManager, DataUploadAlarmReceiver.class,
                getResources().getInteger(R.integer.dataUploadIntervalInMS), 102);

        SensorDCLog.i(TAG, "All alarms set");
    }

    private void SetRebootAlarm(AlarmManager alarmManager) {
        Intent rebootIntent = new Intent(this, HandleRebootReceiver.class);
        PendingIntent pendingRebootIntent = PendingIntent.getBroadcast(this, 103, rebootIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        if (calendar.get(Calendar.MINUTE) >= getResources().getInteger(R.integer.rebootAtMinuteOfHour)) {
            calendar.add(Calendar.HOUR_OF_DAY, 1);
        }
        calendar.set(Calendar.MINUTE, getResources().getInteger(R.integer.rebootAtMinuteOfHour));

        alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingRebootIntent);
    }

    private void setRepeatingAlarm(AlarmManager alarmManager, Class<?> receivingBroadcast, int alarmInterval,
                                   int intentID) {
        Intent intent = new Intent(this, receivingBroadcast);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, intentID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), alarmInterval,
                pendingIntent);
    }
}
