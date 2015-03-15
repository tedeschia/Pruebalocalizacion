package com.entaconsulting.pruebalocalizacion.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Date;


public class GooglePlayServicesHelper implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{

    private static final String TAG = "GooglePlayServicesActivity";

    public static final String KEY_IN_RESOLUTION = "is_in_resolution";

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;

    private boolean mIsInResolution;

    private Activity mActivity;
    private OnConnectedCallback mListener;
    private boolean mLocationUpdates;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private Date mLastUpdateTime;

    public GooglePlayServicesHelper(Activity activity){
        mActivity = activity;
        mGoogleApiClient = new GoogleApiClient.Builder(mActivity)
                // Optionally, add additional APIs and scopes if required.
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        try {
            mListener = (OnConnectedCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnConnectedCallback");
        }

    }

    /**
     * Llamar en "onStart"
     * A connection to Play Services need to be initiated as
     * soon as the activity is visible. Registers {@code ConnectionCallbacks}
     * and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    public void start() {
        mGoogleApiClient.connect();
    }

    /**
     * Called when activity gets invisible. Connection to Play Services needs to
     * be disconnected as soon as an activity is invisible.
     */
    public void stop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Handles Google Play Services resolution callbacks.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_RESOLUTION:
                retryConnecting();
                break;
        }
    }

    private void retryConnecting() {
        setIsInResolution(false);
        if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Called when {@code mGoogleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        if(mLocationUpdates){
            startLocationUpdatesInternal();
        }

        mListener.onConnected();
    }

    public GoogleApiClient getClient() {
        return mGoogleApiClient;
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = new Date();
    }

    public interface OnConnectedCallback{
        public void onConnected();
    }

    /**
     * Called when {@code mGoogleApiClient} connection is suspended.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        retryConnecting();
    }

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!result.hasResolution()) {
            // Show a localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(
                    result.getErrorCode(), mActivity, 0, new OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            retryConnecting();
                        }
                    }).show();
            return;
        }
        // If there is an existing resolution error being displayed or a resolution
        // activity has started before, do nothing and wait for resolution
        // progress to be completed.
        if (getIsInResolution()) {
            return;
        }
        setIsInResolution(true);
        try {
            result.startResolutionForResult(mActivity, REQUEST_CODE_RESOLUTION);
        } catch (SendIntentException e) {
            retryConnecting();
        }
    }

    /**
     * Determines if the client is in a resolution state, and
     * waiting for resolution intent to return.
     */
    public boolean getIsInResolution() {
        return mIsInResolution;
    }

    public void setIsInResolution(boolean mIsInResolution) {
        this.mIsInResolution = mIsInResolution;
    }

    public void startLocationUpdates(long interval, long fastestInterval){
        if(!mGoogleApiClient.isConnected())
            return;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(interval);
        mLocationRequest.setFastestInterval(fastestInterval);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationUpdates = true;
        startLocationUpdatesInternal();

    }
    public void stopLocationUpdates(){
        if(!mGoogleApiClient.isConnected())
            return;

        mLocationUpdates = false;
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }
    public Location getLastKnownLocation(){
        if(mLocationUpdates){
            return mCurrentLocation;
        }
        return null;
    }
    private void startLocationUpdatesInternal(){
        if(mGoogleApiClient.isConnected()){
            //tomo la ultima conocida porque si no cambia no reporta el evento de update
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = new Date();

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }
    }
}
