package com.entaconsulting.pruebalocalizacion.helpers;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;

import com.entaconsulting.pruebalocalizacion.RelevamientoFragment;
import com.entaconsulting.pruebalocalizacion.exceptions.NoAutenticadoException;
import com.entaconsulting.pruebalocalizacion.models.Candidato;
import com.entaconsulting.pruebalocalizacion.models.Categoria;
import com.entaconsulting.pruebalocalizacion.models.Proyecto;
import com.entaconsulting.pruebalocalizacion.models.Relevamiento;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonObject;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceException;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceConflictExceptionJson;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.query.ExecutableQuery;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncContext;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.ColumnDataType;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.SQLiteLocalStore;
import com.microsoft.windowsazure.mobileservices.table.sync.operations.RemoteTableOperationProcessor;
import com.microsoft.windowsazure.mobileservices.table.sync.operations.TableOperation;
import com.microsoft.windowsazure.mobileservices.table.sync.operations.TableOperationError;
import com.microsoft.windowsazure.mobileservices.table.sync.push.MobileServicePushCompletionResult;
import com.microsoft.windowsazure.mobileservices.table.sync.synchandler.MobileServiceSyncHandler;
import com.microsoft.windowsazure.mobileservices.table.sync.synchandler.MobileServiceSyncHandlerException;

import org.apache.http.Header;
import org.apache.http.StatusLine;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataHelper {
    private MobileServiceClient mClient;
    private boolean mTableDefinitionsReady;
    private Context mContext;
    private Boolean mIsActivity;

    public boolean bAuthenticating = false;
    public final Object mAuthenticationLock = new Object();

    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF = "uid";
    public static final String TOKENPREF = "tkn";

    public DataHelper(Context context) {
        mContext = context;
        mIsActivity = context instanceof Activity;
    }

    public void connect(IServiceCallback serviceCallback) throws Exception {
        try {
            // Create the Mobile Service Client instance, using the provided
            // Mobile Service URL and key
            mClient = new MobileServiceClient(
                    "https://relevamientoterritorial.azure-mobile.net/",
                    "BQbVnKfbptcoGWgAuKQJyzYmCDjdII54", // produccion
                    /*"https://relevamientoterritorial-test.azure-mobile.net/",
                    "HeUerGjvQaIVRMmVQFMdhIlmnKxUxs61", // testing*/
                    mContext)
                    .withFilter(new RefreshTokenCacheFilter());

            authenticate(mContext, false, serviceCallback);

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

    public void authenticate(final Context context, boolean bRefreshCache, final IServiceCallback serviceCallback) throws Exception {

        bAuthenticating = true;

        if (bRefreshCache || !loadUserTokenCache(context, mClient))
        {
            if(!mIsActivity){
                throw new Exception("No se encuentra autenticado");
            }

            // Login using the Google provider.
            ListenableFuture<MobileServiceUser> mLogin = mClient.login(MobileServiceAuthenticationProvider.Facebook);

            Futures.addCallback(mLogin, new FutureCallback<MobileServiceUser>() {
                @Override
                public void onFailure(Throwable exc) {
                    processError(exc);
                    synchronized(mAuthenticationLock) {
                        bAuthenticating = false;
                        mAuthenticationLock.notifyAll();
                    }

                    if(serviceCallback!=null){
                        serviceCallback.onAuthenticationFailed();
                    }

                }

                @Override
                public void onSuccess(MobileServiceUser user) {
                    synchronized (mAuthenticationLock) {
                        cacheUserToken(context, mClient.getCurrentUser());
                        bAuthenticating = false;
                        mAuthenticationLock.notifyAll();
                    }

                    try {
                        buildLocalTableDefinitions();
                    } catch (Exception e) {
                        processError(e);
                        if(serviceCallback!=null){
                            serviceCallback.onAuthenticationFailed();
                        }
                    }

                    if(serviceCallback!=null){
                        serviceCallback.onServiceReady();
                    }
                }

            });
        }
        else
        {
            // Other threads may be blocked waiting to be notified when
            // authentication is complete.
            synchronized(mAuthenticationLock)
            {
                bAuthenticating = false;
                mAuthenticationLock.notifyAll();
            }
            buildLocalTableDefinitions();

            if(serviceCallback!=null){
                serviceCallback.onServiceReady();
            }

        }
    }
    public Boolean isAuthenticated(){
        return mClient.getCurrentUser()!=null;
    }

    private void processError(Throwable exc) {
        if(mIsActivity !=null){
            MessageHelper.createAndShowDialog((Activity)mContext, exc, "Error");
        }else{
            exc.printStackTrace();
        }
    }

    private void cacheUserToken(Context context, MobileServiceUser user)
    {
        SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USERIDPREF, user.getUserId());
        editor.putString(TOKENPREF, user.getAuthenticationToken());
        editor.commit();
    }
    private boolean loadUserTokenCache(Context context, MobileServiceClient client)
    {
        SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        String userId = prefs.getString(USERIDPREF, "undefined");
        if (userId == "undefined")
            return false;
        String token = prefs.getString(TOKENPREF, "undefined");
        if (token == "undefined")
            return false;

        MobileServiceUser user = new MobileServiceUser(userId);
        user.setAuthenticationToken(token);
        client.setCurrentUser(user);

        return true;
    }
    private void waitAndUpdateRequestToken(ServiceFilterRequest request)
    {
        MobileServiceUser user = null;
        if (detectAndWaitForAuthentication())
        {
            user = mClient.getCurrentUser();
            if (user != null)
            {
                request.removeHeader("X-ZUMO-AUTH");
                request.addHeader("X-ZUMO-AUTH", user.getAuthenticationToken());
            }
        }
    }

    public boolean detectAndWaitForAuthentication()
    {
        boolean detected = false;
        synchronized(mAuthenticationLock)
        {
            do
            {
                if (bAuthenticating == true)
                    detected = true;
                try
                {
                    mAuthenticationLock.wait(1000);
                }
                catch(InterruptedException e)
                {}
            }
            while(bAuthenticating == true);
        }
        if (bAuthenticating == true)
            return true;

        return detected;
    }
    public MobileServiceSyncTable<Relevamiento> getRelevamientoSyncTable() {
        return mClient.getSyncTable(Relevamiento.class);
    }
    public MobileServiceTable<Relevamiento> getRelevamientoTable() {
        return mClient.getTable(Relevamiento.class);
    }
    public MobileServiceSyncTable<Proyecto> getProyectoSyncTable() {
        return mClient.getSyncTable(Proyecto.class);
    }
    public MobileServiceSyncTable<Categoria> getCategoriaSyncTable() {
        return mClient.getSyncTable(Categoria.class);
    }
    public MobileServiceSyncTable<Candidato> getCandidatoSyncTable() {
        return mClient.getSyncTable(Candidato.class);
    }
    public MobileServiceTable<Proyecto> getProyectoTable() {

        return mClient.getTable(Proyecto.class);
    }

    public com.google.common.util.concurrent.ListenableFuture<Void> pushData(){
        return mClient.getSyncContext().push();
    }

    private void buildLocalTableDefinitions() throws Exception {
        if(mTableDefinitionsReady)
            return;

        mTableDefinitionsReady = true;

        SQLiteLocalStore localStore = new SQLiteLocalStore(mClient.getContext(), "Relevamiento", null, 1);
        ConflictResolvingSyncHandler handler = new ConflictResolvingSyncHandler();
        MobileServiceSyncContext syncContext = mClient.getSyncContext();

        Map<String, ColumnDataType> tableDefinition = new HashMap<String, ColumnDataType>();
        tableDefinition.put("id", ColumnDataType.String);
        tableDefinition.put("fecha", ColumnDataType.Date);
        tableDefinition.put("datos", ColumnDataType.String);
        tableDefinition.put("latitud", ColumnDataType.Real);
        tableDefinition.put("longitud", ColumnDataType.Real);
        tableDefinition.put("direccion", ColumnDataType.String);
        tableDefinition.put("direccionEstado", ColumnDataType.String);
        tableDefinition.put("proyectoClave", ColumnDataType.String);
        localStore.defineTable("Relevamiento", tableDefinition);

        Map<String, ColumnDataType> tableDefinitionProyecto = new HashMap<String, ColumnDataType>();
        tableDefinitionProyecto.put("id", ColumnDataType.String);
        tableDefinitionProyecto.put("nombre", ColumnDataType.String);
        tableDefinitionProyecto.put("clave", ColumnDataType.String);
        localStore.defineTable("Proyecto", tableDefinitionProyecto);

        Map<String, ColumnDataType> tableDefinitionCandidato = new HashMap<String, ColumnDataType>();
        tableDefinitionCandidato.put("id", ColumnDataType.String);
        tableDefinitionCandidato.put("nombre", ColumnDataType.String);
        tableDefinitionCandidato.put("color", ColumnDataType.String);
        tableDefinitionCandidato.put("proyectoId", ColumnDataType.String);
        tableDefinitionCandidato.put("orden", ColumnDataType.Integer);
        localStore.defineTable("Candidato", tableDefinitionCandidato);

        Map<String, ColumnDataType> tableDefinitionCategoria = new HashMap<String, ColumnDataType>();
        tableDefinitionCategoria.put("id", ColumnDataType.String);
        tableDefinitionCategoria.put("nombre", ColumnDataType.String);
        tableDefinitionCategoria.put("proyectoId", ColumnDataType.String);
        tableDefinitionCategoria.put("orden", ColumnDataType.Integer);
        localStore.defineTable("Categoria", tableDefinitionCategoria);

        syncContext.initialize(localStore, handler).get();

    }

    public ExecutableQuery<Relevamiento> getRelevamientoQuery(String proyectoClave) {
        return getRelevamientoTable()
                .where().field("proyectoClave").eq(proyectoClave)
                .orderBy("fecha", QueryOrder.Descending)
                .top(100);
    }
    public ExecutableQuery<Proyecto> getProyectoQuery(String proyectoClave) {
        return getProyectoTable()
                .where().field("clave").eq(proyectoClave)
                .top(1);
    }
    public ExecutableQuery<Candidato> getCandidatoQuery(String proyectoId) {
        return mClient.getTable(Candidato.class)
                .where().field("proyectoId").eq(proyectoId)
                .orderBy("orden", QueryOrder.Ascending);
    }
    public ExecutableQuery<Categoria> getCategoriaQuery(String proyectoId) {
        return mClient.getTable(Categoria.class)
                .where().field("proyectoId").eq(proyectoId)
                .orderBy("orden",QueryOrder.Ascending);
    }

    public Void pullProyecto(String proyectoClave) throws InterruptedException, ExecutionException {
        getCandidatoSyncTable().purge(null).get();
        getCategoriaSyncTable().purge(null).get();
        getProyectoSyncTable().purge(null).get();

        getProyectoSyncTable().pull(getProyectoQuery(proyectoClave)).get();
        //si el proyecto se cargó, cargo el resto de la configuración
        Proyecto proyecto = getProyectoPorClave(proyectoClave);
        if(proyecto!=null){
            getCandidatoSyncTable().pull(getCandidatoQuery(proyecto.getId())).get();
            getCategoriaSyncTable().pull(getCategoriaQuery(proyecto.getId())).get();
        }
        return null;

    }

    public Proyecto getProyectoPorClave(String proyectoClave) throws ExecutionException, InterruptedException {
        MobileServiceList<Proyecto> proyectos = getProyectoSyncTable().read(getProyectoQuery(proyectoClave)).get();
        if(proyectos.size()!=1){
            return null;
        }else{
            return proyectos.get(0);
        }
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
                Throwable cause = e.getCause();
                if(cause instanceof MobileServiceConflictExceptionJson){
                    ex =  (MobileServiceConflictExceptionJson)cause;
                }else{
                    throw new MobileServiceSyncHandlerException(e);
                }
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
            if(result.getOperationErrors().size()>0){
                throw new MobileServiceSyncHandlerException("Error enviando datos al servidor");
            }
        }
    }

    public interface IServiceCallback{
        public void onServiceReady();
        public void onAuthenticationFailed();
    }

    private class RefreshTokenCacheFilter implements ServiceFilter {

        AtomicBoolean mAtomicAuthenticatingFlag = new AtomicBoolean();

        @Override
        public ListenableFuture<ServiceFilterResponse> handleRequest(
                final ServiceFilterRequest request,
                final NextServiceFilterCallback nextServiceFilterCallback
        )
        {
            // In this example, if authentication is already in progress we block the request
            // until authentication is complete to avoid unnecessary authentications as
            // a result of HTTP status code 401.
            // If authentication was detected, add the token to the request.
            waitAndUpdateRequestToken(request);

            // Send the request down the filter chain
            // retrying up to 5 times on 401 response codes.
            ListenableFuture<ServiceFilterResponse> future = null;
            ServiceFilterResponse response = null;
            int responseCode = 401;
            for (int i = 0; (i < 5 ) && (responseCode == 401); i++)
            {
                future = nextServiceFilterCallback.onNext(request);
                try {
                    response = future.get();
                    responseCode = response.getStatus().getStatusCode();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    if (e.getCause().getClass() == MobileServiceException.class)
                    {
                        MobileServiceException mEx = (MobileServiceException) e.getCause();
                        responseCode = mEx.getResponse().getStatus().getStatusCode();
                        if (responseCode == 401)
                        {
                            // Two simultaneous requests from independent threads could get HTTP status 401.
                            // Protecting against that right here so multiple authentication requests are
                            // not setup to run on the UI thread.
                            // We only want to authenticate once. Requests should just wait and retry
                            // with the new token.
                            if (mAtomicAuthenticatingFlag.compareAndSet(false, true))
                            {
                                if(mIsActivity) { //SOLO PUEDO AUTENTICAR SI ESTOY EN UNA ACTIVITY, SI ESTOY EN UN SERVICIO NO
                                    // Authenticate on UI thread
                                    ((Activity)mContext).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // Force a token refresh during authentication.
                                            try {
                                                authenticate(mContext, true, null);
                                            } catch (Exception e1) {
                                                e1.printStackTrace();
                                            }
                                        }
                                    });
                                    // Wait for authentication to complete then update the token in the request.
                                    waitAndUpdateRequestToken(request);
                                    mAtomicAuthenticatingFlag.set(false);
                                }else{
                                    SettableFuture<ServiceFilterResponse> failureFuture = SettableFuture.create();
                                    failureFuture.setException(mEx);
                                    return failureFuture;
                                    //future.setException()
                                }
                            }

                        }
                    }
                }
            }
            return future;
        }
    }
}
