package com.entaconsulting.pruebalocalizacion;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.entaconsulting.pruebalocalizacion.helpers.ConnectivityHelper;
import com.entaconsulting.pruebalocalizacion.helpers.DataHelper;
import com.entaconsulting.pruebalocalizacion.helpers.MessageHelper;
import com.entaconsulting.pruebalocalizacion.models.Relevamiento;
import com.entaconsulting.pruebalocalizacion.services.SincronizationService;
import com.google.common.base.Optional;
import com.microsoft.windowsazure.mobileservices.table.query.Query;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;

import java.text.DateFormat;
import java.util.ArrayList;

import static com.microsoft.windowsazure.mobileservices.table.query.QueryOperations.val;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RelevamientoFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RelevamientoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RelevamientoFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "RelevamientoFragment";
    private static final java.lang.String STATE_SINCRONIZANDO = "relevamiento_sincronizando";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private DataHelper mClient;

    /**
     * Mobile Service Table used to access data
     */
    private MobileServiceSyncTable<Relevamiento> mRelevamientoTable;
    private Query mPullQuery;

    /**
     * Adapter to sync the items list with the view
     */
    private RelevamientoAdapter mAdapter;


    /**
     * Progress spinner to use for table operations
     */
    private ProgressBar mProgressBar;
    private MenuItem mAnimatedRefreshMenuItem;

    private OnFragmentInteractionListener mListener;
    private ProgressFilter mProgressFilter;

    private ArrayList<Relevamiento> mDatosRelevamiento;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RelevamientoFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RelevamientoFragment newInstance(String param1, String param2) {
        RelevamientoFragment fragment = new RelevamientoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public RelevamientoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        registerBroadcastReceiver();
        setHasOptionsMenu(true);

        inicializarDatos();
    }

    private void registerBroadcastReceiver() {
        // The filter's action is BROADCAST_ACTION
        IntentFilter mStatusIntentFilter = new IntentFilter(
                SincronizationService.Constants.BROADCAST_ACTION);
        // Instantiates a new DownloadStateReceiver
        AddressResultReceiver mAddressReceiver =
                new AddressResultReceiver();
        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                mAddressReceiver,
                mStatusIntentFilter);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }
    private void inicializarDatos() {

        mProgressFilter = new ProgressFilter(getActivity());
        mClient = new DataHelper(getActivity());//,mProgressFilter);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View main = inflater.inflate(R.layout.fragment_relevamiento, container, false);
        mProgressBar = (ProgressBar)main.findViewById(R.id.loadingProgressBar);
        mProgressBar.setVisibility(ProgressBar.GONE);

        inicializarLista(getActivity(), main);

        return main;
    }

    private void inicializarLista(Context context, View view){

        // Create the Mobile Service Client instance, using the provided
        // Mobile Service URL and key
        // Get the Mobile Service Table instance to use
        mRelevamientoTable = mClient.getClient().getSyncTable(Relevamiento.class);
        mPullQuery = mClient.getClient().getTable(Relevamiento.class)
                .orderBy("fecha", QueryOrder.Descending)
                .top(1000);

        mDatosRelevamiento = new ArrayList<>();
        // Create an adapter to bind the items with the view
        mAdapter = new RelevamientoAdapter(context, R.layout.row_list_relevamiento, mDatosRelevamiento);
        ListView listViewRelevamiento = (ListView) view.findViewById(R.id.listViewRelevamiento);
        listViewRelevamiento.setAdapter(mAdapter);

        // Load the items from the Mobile Service
        loadItemsFromTable();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_relevamiento_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            sincronizar();
            return true;
        }

        return false;
    }

    private void sincronizar() {
        if(!ConnectivityHelper.isConnected(getActivity())){
            MessageHelper.createAndShowDialog(getActivity(), "No se encuentra conectado", "Error");
        }else {
            startSyncService();
        }
    }

    private void startAnimation(MenuItem item) {
        mAnimatedRefreshMenuItem = item;

        LayoutInflater inflater = getActivity().getLayoutInflater();
        ImageView iv = (ImageView) inflater.inflate(R.layout.action_refresh_image, null);

        Animation rotation = AnimationUtils.loadAnimation(getActivity(), R.anim.refresh_rotate);
        rotation.setRepeatCount(Animation.INFINITE);
        iv.startAnimation(rotation);

        item.setActionView(iv);
    }
    private void stopAnimation(){
        if(mAnimatedRefreshMenuItem!=null) {
            View actionView = mAnimatedRefreshMenuItem.getActionView();
            if (actionView != null) {
                actionView.clearAnimation();
                mAnimatedRefreshMenuItem.setActionView(null);
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void loadItemsFromTable() {

        // Get the items that weren't marked as completed and add them in the
        // adapter

        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mDatosRelevamiento.clear();
                    mDatosRelevamiento.addAll(mRelevamientoTable.read(mPullQuery).get());
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.notifyDataSetChanged();
                        }
                    });
                } catch (Exception e){
                    MessageHelper.createAndShowDialog(getActivity(), e, "Error");
                }

                return null;
            }
        }.execute();

    }
    private void refreshItemsFromTable() {

        // Get the items that weren't marked as completed and add them in the
        // adapter

        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    //mClient.getClient().getSyncContext().push().get();
                    //mRelevamientoTable.pull(mPullQuery).get();
                    loadItemsFromTable();
                } catch (Exception e){
                    MessageHelper.createAndShowDialog(getActivity(), e, "Error");
                }

                return null;
            }
        }.execute();

    }

    public void addItem(final Relevamiento relevamiento) {
        if (mClient == null) {
            MessageHelper.createAndShowDialog(getActivity(), "No se encuentra conectado al servicio de datos","ERROR");
        }

        //la dirección está pendiente hasta que se resuelva
        relevamiento.setDireccionEstado(Relevamiento.EstadosDireccion.Pendiente);

        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    final Relevamiento entity = mRelevamientoTable.insert(relevamiento).get();

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.insert(entity, 0);
                            if(ConnectivityHelper.isConnected(getActivity())) {
                                startSyncService();
                            }
                        }
                    });
                }catch (Exception e){
                    MessageHelper.createAndShowDialog(getActivity(), e, "Error");
                }

                return null;
            }
        }.execute();
    }

    public void updateItem(final Relevamiento relevamiento){
        int pos = mDatosRelevamiento.indexOf(relevamiento);
        if(pos>=0){
            mDatosRelevamiento.set(pos, relevamiento);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void startSyncService() {

        mProgressBar.setVisibility(ProgressBar.VISIBLE);
        mProgressBar.setProgress(0);
        // Create an intent for passing to the intent service responsible for fetching the address.
        Intent intent = new Intent(getActivity(), SincronizationService.class);

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        getActivity().startService(intent);

    }
    private void endSyncService(){
        mProgressBar.setVisibility(ProgressBar.GONE);
    }




    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        public Location getLastKnownLocation();

    }

    public class RelevamientoAdapter extends ArrayAdapter<Relevamiento> {

        /**
         * Adapter context
         */
        Context mContext;

        /**
         * Adapter View layout
         */
        int mLayoutResourceId;

        public RelevamientoAdapter(Context context, int layoutResourceId, ArrayList<Relevamiento> datos) {
            super(context, layoutResourceId, datos);

            mContext = context;
            mLayoutResourceId = layoutResourceId;
        }

        /**
         * Returns the view for a specific item on the list
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;

            final Relevamiento currentItem = getItem(position);

            if (row == null) {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                row = inflater.inflate(mLayoutResourceId, parent, false);
            }

            row.setTag(currentItem);
            final TextView textDate = (TextView) row.findViewById(R.id.relevamiento_date);
            textDate.setText(DateFormat.getDateTimeInstance().format(currentItem.getFecha()));

            final TextView textAddress = (TextView) row.findViewById(R.id.relevamiento_address);
            String addressText;
            switch(Optional.fromNullable(currentItem.getDireccionEstado()).or("")){
                case Relevamiento.EstadosDireccion.MultiplesCandidatos:
                    addressText = mContext.getString(R.string.direccion_estado_multiples_candidatos);
                    break;
                case Relevamiento.EstadosDireccion.NoEncontrada:
                    addressText = mContext.getString(R.string.direccion_estado_no_encontrada);
                    break;
                case Relevamiento.EstadosDireccion.Resuelta:
                    addressText = currentItem.getDireccion();
                    break;
                default:
                    addressText = mContext.getString(R.string.direccion_estado_pendiente);
            }
            textAddress.setText(addressText);

            return row;
        }

    }

    /**
     * Receiver for data sent from FetchAddressIntentService.
     */
    class AddressResultReceiver extends BroadcastReceiver {

        private AddressResultReceiver(){ }

        /**
         *  Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        @Override
        public void onReceive(Context context, Intent intent) {

            int resultCode = intent.getIntExtra(SincronizationService.Constants.STATUS_DATA_EXTRA,-1);


            switch (resultCode) {
                case SincronizationService.Constants.PROGRESS_RESULT:
                    double progress = intent.getDoubleExtra(SincronizationService.Constants.PROGRESS_DATA_EXTRA,1);

                    mProgressBar.setVisibility(ProgressBar.VISIBLE);
                    mProgressBar.setProgress((int)(progress * 100));

                    Relevamiento relevamientoActualizado = intent.getParcelableExtra(SincronizationService.Constants.RELEVAMIENTO_DATA_EXTRA);
                    if(relevamientoActualizado!=null){
                        updateItem(relevamientoActualizado);
                    }
                    break;
                case SincronizationService.Constants.SUCCESS_RESULT:
                    endSyncService();
                    break;
                case SincronizationService.Constants.FAILURE_RESULT:
                    String message = intent.getStringExtra(SincronizationService.Constants.RESULT_DATA_KEY);
                    MessageHelper.createAndShowDialog(getActivity(), message, "Error");
                    endSyncService();
            }

        }
    }

}

