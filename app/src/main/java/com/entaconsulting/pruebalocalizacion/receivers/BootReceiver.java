package com.entaconsulting.pruebalocalizacion.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.entaconsulting.pruebalocalizacion.receivers.AlarmReceiver;

public class BootReceiver extends BroadcastReceiver {

    AlarmReceiver alarm = new AlarmReceiver();
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            alarm.setAlarm(context, -1); //ahora
        }
    }
}