package com.entaconsulting.pruebalocalizacion;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;

import java.security.InvalidParameterException;
import java.util.Date;


public class RelevamientoActivity extends ActionBarActivity
        implements RelevamientoFragment.OnFragmentInteractionListener,
        GooglePlayServicesHelper.OnConnectedCallback
{
    private static final String TAG = "RelevamientoActivity";
    private GooglePlayServicesHelper mGooglePlayServices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(mGooglePlayServices==null){
            mGooglePlayServices = new GooglePlayServicesHelper(this);
        }

        if (savedInstanceState != null) {
            mGooglePlayServices.setIsInResolution(savedInstanceState.getBoolean(GooglePlayServicesHelper.KEY_IN_RESOLUTION, false));
        }

        setContentView(R.layout.activity_relevamiento);

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
        RelevamientoFragment fragment = (RelevamientoFragment) getSupportFragmentManager().findFragmentById(R.id.detalle_relevamiento_fragment);
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
        if (item.getItemId() == R.id.action_new) {
            addRelevamiento();
        }

        return super.onOptionsItemSelected(item);
    }

    public void addRelevamiento() {
        Location location = getLastKnownLocation();
        if(location == null){
            MessageHelper.createAndShowDialog(this, "No se ha podido obtener la localización, no se pueden registrar datos sin la posición geográfica","Error");
            return;
        }

        Intent intent = new Intent(this, RelevamientoDetalleActivity.class);
        intent.putExtra(RelevamientoDetalleFragment.ARG_ACTION_MESSAGE, RelevamientoDetalleFragment.ARG_ACTION_ADD);
        intent.putExtra(RelevamientoDetalleFragment.ARG_LOCALIZACION, location);
        startActivityForResult(intent, RelevamientoDetalleActivity.REQUEST_ADD);
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

    @Override
    public Location getLastKnownLocation() {

        return mGooglePlayServices.getLastKnownLocation();
    }

    class AddressResultReceiver extends ResultReceiver {
        private String mAddressOutput;

        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            mAddressOutput = resultData.getString(FetchAdressIntentService.Constants.RESULT_DATA_KEY);

            // Show a toast message if an address was found.
            if (resultCode == FetchAdressIntentService.Constants.SUCCESS_RESULT) {
                MessageHelper.createAndShowDialog(getParent(),mAddressOutput,"Direccion");
            }

        }
    }
}

