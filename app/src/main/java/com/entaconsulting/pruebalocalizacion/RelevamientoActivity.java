package com.entaconsulting.pruebalocalizacion;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.entaconsulting.pruebalocalizacion.helpers.ConfigurationHelper;
import com.entaconsulting.pruebalocalizacion.helpers.DataHelper;
import com.entaconsulting.pruebalocalizacion.helpers.GooglePlayServicesHelper;
import com.entaconsulting.pruebalocalizacion.helpers.MessageHelper;
import com.entaconsulting.pruebalocalizacion.models.Proyecto;
import com.entaconsulting.pruebalocalizacion.models.Relevamiento;

import java.util.concurrent.ExecutionException;

public class RelevamientoActivity extends FragmentActivity
        implements RelevamientoFragment.OnFragmentInteractionListener,
        GooglePlayServicesHelper.OnConnectedCallback
{
    private static final String TAG = "RelevamientoActivity";
    private GooglePlayServicesHelper mGooglePlayServices;
    private SharedPreferences mSharedPref;
    private String mProyectoClave;
    private DataHelper mClient;
    private ProgressDialog mProgressDlg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(mGooglePlayServices==null){
            mGooglePlayServices = new GooglePlayServicesHelper(this);
        }else {
            mGooglePlayServices.setIsInResolution(savedInstanceState.getBoolean(GooglePlayServicesHelper.KEY_IN_RESOLUTION, false));
        }

        startLoading();

        //instancio el cliente de datos
        mClient = ConfigurationHelper.getClient();
        if(mClient==null){
            try {

                ConfigurationHelper.setClient(new DataHelper(this));
                mClient = ConfigurationHelper.getClient();
                mClient.connect(new DataHelper.IServiceCallback() {
                    @Override
                    public void onServiceReady() {

                        onClientReady();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        MessageHelper.createAndShowDialog(RelevamientoActivity.this, "No se ha podido autenticar al usuario", "Error");
                    }
                });
            }catch(Exception e) {
                MessageHelper.createAndShowDialog(this, e, "Error");
            }
        } else{
            onClientReady();
        }

    }

    private void startLoading() {
        setContentView(R.layout.activity_relevamiento_loading);
    }
    private void cargaProyectoFinalizada() {
        setContentView(R.layout.activity_relevamiento);
    }

    private void loadSettings() {
        //settings defaults
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //si no tengo proyecto redirijo a settings
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mProyectoClave = mSharedPref.getString(SettingsFragment.KEY_PREF_PROYECTO_CLAVE,null);

        if(mProyectoClave == null || mProyectoClave == ""){
            MessageHelper.createAndShowDialog(this, "Debe ingresar una clave de proyecto", "Error");
            settings();
            return;
        }


    }

    private void onClientReady() {
        loadSettings();
        if (mProyectoClave == null || mProyectoClave == "") {
            return;
        }

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    final boolean proyectoCargado = loadConfiguration();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (proyectoCargado) {
                                cargaProyectoFinalizada();
                            } else {
                                MessageHelper.createAndShowDialog(RelevamientoActivity.this, "No se ha encontrado ningún proyecto con ese codigo. Corrijalo y vuelva a intentar", "Error");
                                settings();

                            }
                        }
                    });
                } catch (Exception e) {
                    MessageHelper.createAndShowDialog(RelevamientoActivity.this, e, "Error");
                }

                return null;
            }
        }.execute();

    }

    private boolean loadConfiguration() throws ExecutionException, InterruptedException {
        if(ConfigurationHelper.getProyectoClave()==null || ConfigurationHelper.getProyectoClave() != mProyectoClave){
            Proyecto proyecto = mClient.getProyectoPorClave(mProyectoClave);
            if(proyecto==null){
                //si no tengo el proyecto cargado, lo cargo ahora
                mClient.pullProyecto(mProyectoClave);
                proyecto = mClient.getProyectoPorClave(mProyectoClave);
            }
            if(proyecto!=null) {
                ConfigurationHelper.setCandidatos(mClient.getCandidatoSyncTable().read(mClient.getCandidatoQuery(proyecto.getId())).get());
                ConfigurationHelper.setCategorias(mClient.getCategoriaSyncTable().read(mClient.getCategoriaQuery(proyecto.getId())).get());
                ConfigurationHelper.setProyectoClave(mProyectoClave);
            }
        }
        return ConfigurationHelper.getProyectoClave()!=null && ConfigurationHelper.getProyectoClave() == mProyectoClave;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(GooglePlayServicesHelper.KEY_IN_RESOLUTION, mGooglePlayServices.getIsInResolution());
    }

    @Override
    protected void onStart(){
        super.onStart();
        mGooglePlayServices.start();
    }
    @Override
    protected void onResume(){
        super.onResume();
        mGooglePlayServices.startLocationUpdates(3000, 1000);
    }
    @Override
    protected void onPause(){
        super.onPause();
        mGooglePlayServices.stopLocationUpdates();
    }
    @Override
    protected void onStop(){
        super.onStop();
        mGooglePlayServices.stop();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case RelevamientoDetalleActivity.REQUEST_ADD:
                if(resultCode == RESULT_OK){
                    Relevamiento relevamiento = data.getParcelableExtra(RelevamientoDetalleActivity.EXTRA_RELEVAMIENTO);
                    addRelevamientoResult(relevamiento);
                }
                break;
            default:
                mGooglePlayServices.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void addRelevamientoResult(final Relevamiento relevamiento) {
        RelevamientoFragment fragment = (RelevamientoFragment) getSupportFragmentManager().findFragmentById(R.id.relevamiento_fragment);
        fragment.addItem(relevamiento);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_new:
                addRelevamiento();
                return true;
            case R.id.action_settings:
                settings();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void addRelevamiento() {
        Location location = getLastKnownLocation();
        if(location == null){
            MessageHelper.createAndShowDialog(this, "No se ha podido obtener la localización, no se pueden registrar datos sin la posición geográfica", "Error");
            return;
        }

        Intent intent = new Intent(this, RelevamientoDetalleActivity.class);
        intent.putExtra(RelevamientoDetalleFragment.ARG_ACTION_MESSAGE, RelevamientoDetalleFragment.ARG_ACTION_ADD);
        intent.putExtra(RelevamientoDetalleFragment.ARG_LOCALIZACION, location);
        startActivityForResult(intent, RelevamientoDetalleActivity.REQUEST_ADD);
    }
    public void settings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    public void editRelevamiento(String relevamientoId){
        Intent intent = new Intent(this, RelevamientoDetalleActivity.class);
        intent.putExtra(RelevamientoDetalleFragment.ARG_ACTION_MESSAGE, RelevamientoDetalleFragment.ARG_ACTION_EDIT);
        intent.putExtra(RelevamientoDetalleFragment.ARG_RELEVAMIENTO_ID, relevamientoId);
        startActivity(intent);
    }

    @Override
    public void onConnected() {
    }

    public Location getLastKnownLocation() {

        return mGooglePlayServices.getLastKnownLocation();
    }

    @Override
    public void seleccionarProyecto() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}

