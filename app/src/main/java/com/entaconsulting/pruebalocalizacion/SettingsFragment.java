package com.entaconsulting.pruebalocalizacion;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.entaconsulting.pruebalocalizacion.helpers.DataHelper;
import com.entaconsulting.pruebalocalizacion.helpers.MessageHelper;
import com.entaconsulting.pruebalocalizacion.models.Proyecto;
import com.entaconsulting.pruebalocalizacion.ui.EditTextShowPreference;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.table.query.ExecutableQuery;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;


public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String KEY_PREF_PROYECTO_CLAVE = "proyecto";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        String proyectoActual = getPreferenceScreen().getSharedPreferences().getString(KEY_PREF_PROYECTO_CLAVE, "");
        EditTextShowPreference proyectoPref = (EditTextShowPreference)findPreference(KEY_PREF_PROYECTO_CLAVE);
        // Set summary to be the user-description for the selected value
        proyectoPref.setSummary(proyectoActual);

        //si el proyecto está en blanco abro directamente el popup
        if(proyectoActual == null || proyectoActual.equals("")){
            proyectoPref.show();
        }


    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }
    @Override
    public void onPause() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals(KEY_PREF_PROYECTO_CLAVE)) {
            Preference proyectoPref = findPreference(key);
            // Set summary to be the user-description for the selected value
            proyectoPref.setSummary(sharedPreferences.getString(key, ""));
        }
    }

}
