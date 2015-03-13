package com.entaconsulting.pruebalocalizacion;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.microsoft.windowsazure.mobileservices.table.query.ExecutableQuery;
import com.microsoft.windowsazure.mobileservices.table.query.Query;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Created by atedeschi on 10/3/15.
 */
public class FetchAdressIntentService extends IntentService {
    private static final String TAG = "com.entaconsulting.pruebalocalizacion.FetchAdressIntentService";

    protected ResultReceiver mReceiver;
    private DataHelper mClient;
    private MobileServiceSyncTable<Relevamiento> mRelevamientoTable;
    private Query mPullQuery;
    private AlarmReceiver mAlarm;
    private int mTotalSteps;
    private int mCurrentStep;

    public FetchAdressIntentService(){
        super(TAG);
    }
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public FetchAdressIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        //mReceiver = intent.getParcelableExtra(Constants.RECEIVER);

        if(!ConnectivityHelper.isConnected(getApplicationContext())){
            broadcastSuccess();
            scheduleRetry();
            return;
        }

        try {
            if(mClient==null) {
                mClient = new DataHelper(getApplicationContext());
                mRelevamientoTable = mClient.getClient().getSyncTable(Relevamiento.class);
                /*mPullQuery = mClient.getClient().getTable(Relevamiento.class)
                        .orderBy("fecha", QueryOrder.Descending)
                        .top(1000);*/
            }

            ArrayList<Relevamiento> pendientes = getPendientes();

            mTotalSteps = pendientes.size() + 2;
            broadcastProgress(null);

            if(pendientes.size()>0){
                procesarPendientes(pendientes);
            }

            //sincronizo con el server
            mClient.getClient().getSyncContext().push().get();
            broadcastProgress(null);
            //mRelevamientoTable.pull(mPullQuery).get();

            broadcastSuccess();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
            broadcastError(e.getMessage());

        }

    }

    private void scheduleRetry() {

    }

    private void procesarPendientes(ArrayList<Relevamiento> pendientes) {
        for(Relevamiento pendiente:pendientes){
            if(!ConnectivityHelper.isConnected(getApplicationContext())){
                break;
            }
            resolveLocation(pendiente);

            if(pendiente.getDireccionEstado()!=Relevamiento.EstadosDireccion.Pendiente){
                broadcastProgress(pendiente);
            }
        }
    }

    private void resolveLocation(Relevamiento relevamiento){
        // Get the location passed to this service through an extra.
        String estadoOriginal = relevamiento.getDireccionEstado();
        String direccionOriginal = relevamiento.getDireccion();

        Geocoder geocoder = new Geocoder(this, new Locale("es","ES"));

        List<Address> addresses = null;
        Boolean servicioNoDisponible = false;

        try {
            addresses = geocoder.getFromLocation(
                    relevamiento.getLatitud(),
                    relevamiento.getLongitud(),
                    1);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            relevamiento.setDireccionEstado(Relevamiento.EstadosDireccion.ErrorInesperado);
            relevamiento.setDireccion(getString(R.string.invalid_lat_long_used));
        } catch (IOException ioException) {
            servicioNoDisponible=true;
        }

        if(!servicioNoDisponible) {
            if (addresses == null || addresses.size() == 0) {
                relevamiento.setDireccionEstado(Relevamiento.EstadosDireccion.NoEncontrada);
            } else {
                Address address = addresses.get(0);
                ArrayList<String> addressFragments = new ArrayList<String>();

                for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                    addressFragments.add(address.getAddressLine(i));
                }
                if(addressFragments.size()>0) {
                    relevamiento.setDireccion(TextUtils.join(", ", addressFragments));
                    relevamiento.setDireccionEstado(Relevamiento.EstadosDireccion.Resuelta);
                } else{
                    relevamiento.setDireccionEstado(Relevamiento.EstadosDireccion.NoEncontrada);
                }
            }
            try {
                if(relevamiento.getDireccionEstado()!=Relevamiento.EstadosDireccion.Pendiente){
                    mRelevamientoTable.update(relevamiento).get();
                }
            } catch (Exception e) {
                relevamiento.setDireccion(direccionOriginal);
                relevamiento.setDireccionEstado(estadoOriginal);
            }
        }
    }
    private void broadcastError(String message) {
        Intent intent = new Intent(Constants.BROADCAST_ACTION)
            .putExtra(Constants.STATUS_DATA_EXTRA,Constants.FAILURE_RESULT)
            .putExtra(Constants.RESULT_DATA_KEY, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastSuccess() {
        Intent intent = new Intent(Constants.BROADCAST_ACTION)
                .putExtra(Constants.STATUS_DATA_EXTRA,Constants.SUCCESS_RESULT);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastProgress(Relevamiento relevamiento) {
        mCurrentStep++;
        Intent intent = new Intent(Constants.BROADCAST_ACTION)
                .putExtra(Constants.STATUS_DATA_EXTRA,Constants.PROGRESS_RESULT)
                .putExtra(Constants.PROGRESS_DATA_EXTRA, mCurrentStep * 1.0 / mTotalSteps)
                .putExtra(Constants.RELEVAMIENTO_DATA_EXTRA, relevamiento);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }



    public ArrayList<Relevamiento> getPendientes() throws ExecutionException, InterruptedException {
        ExecutableQuery<Relevamiento> query = mClient.getClient().getTable(Relevamiento.class)
                .where().field("direccionEstado").eq(Relevamiento.EstadosDireccion.Pendiente);

        return mRelevamientoTable.read(query).get();
    }

    public final class Constants {
        public static final int SUCCESS_RESULT = 0;
        public static final int FAILURE_RESULT = 1;
        public static final int PROGRESS_RESULT = 2;
        public static final String PACKAGE_NAME =
                "com.google.android.gms.location.sample.locationaddress";
        public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
        public static final String RESULT_DATA_KEY = PACKAGE_NAME +
                ".RESULT_DATA_KEY";
        public static final String LOCATION_LAT_DATA_EXTRA = PACKAGE_NAME +
                ".LOCATION_LAT_DATA_EXTRA";
        public static final String LOCATION_LON_DATA_EXTRA = PACKAGE_NAME +
                ".LOCATION__LON_DATA_EXTRA";

        public static final String RELEVAMIENTO_DATA_EXTRA = PACKAGE_NAME + ".RELEVAMIENTO_DATA_EXTRA";

        public static final String BROADCAST_ACTION = PACKAGE_NAME + ".BROADCAST";
        public static final String STATUS_DATA_EXTRA = PACKAGE_NAME + ".STATUS_DATA_EXTRA";
        public static final String PROGRESS_DATA_EXTRA = PACKAGE_NAME + ".PROGRESS_DATA_EXTRA";
    }
}
