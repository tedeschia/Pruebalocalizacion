package com.entaconsulting.pruebalocalizacion.helpers;

import android.app.Activity;
import android.content.Context;

import com.google.gson.JsonObject;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceConflictExceptionJson;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncContext;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.ColumnDataType;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.MobileServiceLocalStoreException;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.SQLiteLocalStore;
import com.microsoft.windowsazure.mobileservices.table.sync.operations.RemoteTableOperationProcessor;
import com.microsoft.windowsazure.mobileservices.table.sync.operations.TableOperation;
import com.microsoft.windowsazure.mobileservices.table.sync.push.MobileServicePushCompletionResult;
import com.microsoft.windowsazure.mobileservices.table.sync.synchandler.MobileServiceSyncHandler;
import com.microsoft.windowsazure.mobileservices.table.sync.synchandler.MobileServiceSyncHandlerException;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class DataHelper {
    private MobileServiceClient mClient;

    public DataHelper(Context context) throws Exception {
        connect(context, null);
    }

    public DataHelper(Activity activity){
        this(activity, null);
    }
    public DataHelper(Activity activity, ProgressFilter filter) {
        try {
            connect(activity, filter);
        } catch (Exception e) {
            MessageHelper.createAndShowDialog(activity, e, "Error");
        }
    }
    private void connect(Context context, ProgressFilter filter) throws Exception {
        try {
            // Create the Mobile Service Client instance, using the provided
            // Mobile Service URL and key
            mClient = new MobileServiceClient(
                    "https://relevamientoterritorial.azure-mobile.net/",
                    "BQbVnKfbptcoGWgAuKQJyzYmCDjdII54",
                    context);
            if(filter!=null)
                mClient = mClient.withFilter(filter);

            buildLocalTableDefinitions(mClient);

        } catch (MalformedURLException e) {
            throw new Exception("Error creando el Servicio Movil, verificar la URL", e);
        } catch (Exception e) {
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            throw new Exception("Error desconocido: " + t.getMessage(), e);
        }
    }

    public MobileServiceClient getClient() {
        return mClient;
    }

    private void buildLocalTableDefinitions(MobileServiceClient client) throws MobileServiceLocalStoreException, ExecutionException, InterruptedException {
        SQLiteLocalStore localStore = new SQLiteLocalStore(client.getContext(), "Relevamiento", null, 1);
        ConflictResolvingSyncHandler handler = new ConflictResolvingSyncHandler();
        MobileServiceSyncContext syncContext = client.getSyncContext();

        Map<String, ColumnDataType> tableDefinition = new HashMap<String, ColumnDataType>();
        tableDefinition.put("id", ColumnDataType.String);
        tableDefinition.put("fecha", ColumnDataType.Date);
        tableDefinition.put("datos", ColumnDataType.String);
        tableDefinition.put("latitud", ColumnDataType.Real);
        tableDefinition.put("longitud", ColumnDataType.Real);
        tableDefinition.put("direccion", ColumnDataType.String);
        tableDefinition.put("direccionEstado", ColumnDataType.String);

        localStore.defineTable("Relevamiento", tableDefinition);
        syncContext.initialize(localStore, handler).get();

    }

    private class ConflictResolvingSyncHandler implements MobileServiceSyncHandler {

        @Override
        public JsonObject executeTableOperation(
                RemoteTableOperationProcessor processor, TableOperation operation)
                throws MobileServiceSyncHandlerException {

            MobileServiceConflictExceptionJson ex = null;
            JsonObject result = null;
            try {
                result = operation.accept(processor);
            } catch(MobileServiceConflictExceptionJson e){
                ex = e;
            } catch (Throwable e) {
                ex =  (MobileServiceConflictExceptionJson)e.getCause();
            }

            if (ex != null) {
                // A conflict was detected; let's force the server to "win"
                // by discarding the client version of the item
                // Other policies could be used, such as prompt the user for
                // which version to maintain.

                JsonObject serverItem = ex.getValue();

                if (serverItem == null) {
                    // Item not returned in the exception, retrieving it from the server
                    try {
                        serverItem = mClient.getTable(operation.getTableName()).lookUp(operation.getItemId()).get();
                    } catch (Exception e) {
                        throw new MobileServiceSyncHandlerException(e);
                    }
                }

                result = serverItem;
            }

            return result;
        }

        @Override
        public void onPushComplete(MobileServicePushCompletionResult result)
                throws MobileServiceSyncHandlerException {
        }
    }
}
