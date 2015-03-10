package com.entaconsulting.pruebalocalizacion;

import android.app.Activity;
import android.content.Context;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncContext;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.ColumnDataType;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.MobileServiceLocalStoreException;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.SQLiteLocalStore;
import com.microsoft.windowsazure.mobileservices.table.sync.synchandler.SimpleSyncHandler;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class DataHelper {
    private MobileServiceSyncContext mSyncContext;
    private MobileServiceClient mClient;

    public DataHelper(Activity activity, ProgressFilter filter){
        try {
            // Create the Mobile Service Client instance, using the provided
            // Mobile Service URL and key
            mClient = new MobileServiceClient(
                    "https://relevamientoterritorial.azure-mobile.net/",
                    "BQbVnKfbptcoGWgAuKQJyzYmCDjdII54",
                    activity).withFilter(filter);

            buildLocalTableDefinitions(mClient);

        } catch (MalformedURLException e) {
            MessageHelper.createAndShowDialog(activity, new Exception("There was an error creating the Mobile Service. Verify the URL"), "Error");
        } catch (Exception e) {
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            MessageHelper.createAndShowDialog(activity, new Exception("Unknown error: " + t.getMessage()), "Error");
        }
    }

    public MobileServiceClient getClient() {
        return mClient;
    }

    private void buildLocalTableDefinitions(MobileServiceClient client) throws MobileServiceLocalStoreException, ExecutionException, InterruptedException {
        SQLiteLocalStore localStore = new SQLiteLocalStore(client.getContext(), "Relevamiento", null, 1);
        SimpleSyncHandler handler = new SimpleSyncHandler();
        mSyncContext = client.getSyncContext();

        Map<String, ColumnDataType> tableDefinition = new HashMap<String, ColumnDataType>();
        tableDefinition.put("id", ColumnDataType.String);
        tableDefinition.put("fecha", ColumnDataType.Date);
        tableDefinition.put("datos", ColumnDataType.String);
        tableDefinition.put("latitud", ColumnDataType.Real);
        tableDefinition.put("longitud", ColumnDataType.Real);

        localStore.defineTable("Relevamiento", tableDefinition);
        mSyncContext.initialize(localStore, handler).get();

    }
}
