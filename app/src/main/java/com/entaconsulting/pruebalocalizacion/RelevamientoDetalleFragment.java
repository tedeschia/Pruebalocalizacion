package com.entaconsulting.pruebalocalizacion;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.gson.Gson;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;

import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RelevamientoDetalleFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RelevamientoDetalleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RelevamientoDetalleFragment extends Fragment {
    private static final String STATE_DATOS_RELEVAMIENTO = "datosRelevamiento";

    public static final String ARG_ACTION_ADD = "add";
    public static final String ARG_ACTION_EDIT = "edit";
    public static final String ARG_ACTION_MESSAGE = "action";
    public static final String ARG_RELEVAMIENTO_ID = "relevamiento_id";
    public static final String ARG_LOCALIZACION = "localizacion";

    private String mAction;

    private HashMap<String, DatoRelevamientoPublicidad> mDatosRelevamiento;
    private String[] mCandidatos;
    private String[] mMateriales;

    private OnFragmentInteractionListener mListener;
    private DataHelper mClient;
    private ProgressFilter mProgressFilter;
    private String mRelevamientoId;
    private Location mLocalizacion;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param action_message Parameter 2.
     * @param localizacion
     * @return A new instance of fragment RelevamientoFragment.
     */
    public static RelevamientoDetalleFragment newInstance(String action_message, String relevamiento_id, Location localizacion) {
        RelevamientoDetalleFragment fragment = new RelevamientoDetalleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ACTION_MESSAGE, action_message);
        args.putString(ARG_RELEVAMIENTO_ID, relevamiento_id);
        args.putParcelable(ARG_LOCALIZACION, localizacion);
        fragment.setArguments(args);
        return fragment;
    }

    public RelevamientoDetalleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mAction = getArguments().getString(ARG_ACTION_MESSAGE);
            mRelevamientoId = getArguments().getString(ARG_RELEVAMIENTO_ID);
            mLocalizacion = getArguments().getParcelable(ARG_LOCALIZACION);
        }
        setHasOptionsMenu(true);

        leerConfiguracion();

        if(savedInstanceState!=null){
            mDatosRelevamiento = datosBdAVista((DatoRelevamientoPublicidad[]) savedInstanceState.getSerializable(STATE_DATOS_RELEVAMIENTO));
        }else{
            mDatosRelevamiento = datosBdAVista(null);
        }
        mProgressFilter = new ProgressFilter(getActivity());
        mClient = new DataHelper(getActivity(),mProgressFilter);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putSerializable(STATE_DATOS_RELEVAMIENTO, datosVistaABd());

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    private DatoRelevamientoPublicidad[] datosVistaABd() {
        DatoRelevamientoPublicidad[] result = new DatoRelevamientoPublicidad[mDatosRelevamiento.values().size()];
        return mDatosRelevamiento.values().toArray(result);
    }

    private HashMap<String, DatoRelevamientoPublicidad> datosBdAVista(DatoRelevamientoPublicidad[] datosIniciales) {
        HashMap<String, DatoRelevamientoPublicidad> datos = new HashMap<>();
        if(datosIniciales==null) {
            for (String mMaterial : mMateriales) {
                for (String mCandidato : mCandidatos) {
                    DatoRelevamientoPublicidad dato = new DatoRelevamientoPublicidad(mCandidato, mMaterial, 0);
                    datos.put(getKey(dato.getCandidato(), dato.getMaterial()), dato);
                }
            }
        }else{
            for(DatoRelevamientoPublicidad dato:datosIniciales){
                datos.put(getKey(dato.getCandidato(), dato.getMaterial()), dato);
            }
        }
        return datos;
    }

    private String getKey(String candidato, String material) {
        return candidato+material;
    }

    private void leerConfiguracion(){
        Resources res = getResources();
        mCandidatos = res.getStringArray(R.array.candidatos_array);
        mMateriales = res.getStringArray(R.array.materiales_array);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View main = inflater.inflate(R.layout.fragment_relevamiento_detalle, container, false);
        TableLayout table = (TableLayout) main.findViewById(R.id.main_table);

        //Setup progress bar
        View progressBar = container.findViewById(R.id.loadingProgressBar);
        if(progressBar!=null){
            mProgressFilter.setProgressBar(progressBar);
            progressBar.setVisibility(ProgressBar.GONE);
        }

        buildTable(table, inflater, getActivity());
        return main;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_relevamiento_detalle_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        switch(item.getItemId()){
            case R.id.action_save:
                guardar();
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    private void guardar() {
        if (mClient == null) {
            MessageHelper.createAndShowDialog(getActivity(), "No se encuentra conectado al servicio de datos","ERROR");
        }

        final MobileServiceSyncTable<Relevamiento> tableRelevamiento = mClient.getClient().getSyncTable(Relevamiento.class);
        //MobileServiceTable<Relevamiento> tableRelevamientoDetalle = mClient.getTable(RelevamientoDetalle.class);
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Relevamiento entity = new Relevamiento(new Date());
                    String json = new Gson().toJson(datosVistaABd());
                    entity.setDatos(json);
                    entity.setLatitud(mLocalizacion.getLatitude());
                    entity.setLongitud(mLocalizacion.getLongitude());

                    final Relevamiento relevamiento = tableRelevamiento.insert(entity).get();

                    if(mListener!=null){
                        mListener.onItemSaved(relevamiento);
                    }

                } catch (Exception e){
                    MessageHelper.createAndShowDialog(getActivity(), e, "Error");
                }

                return null;
            }
        }.execute();


    }

    private void close() {
    }


    private void buildTable(TableLayout tableLayout, LayoutInflater inflater, Context context) {

        Resources res = getResources();

        TypedArray candidatosColores = res.obtainTypedArray(R.array.candidatos_colores_array);
        int[] gradosCumplimientoInt = res.getIntArray(R.array.grado_cumplimiento_array);
        Integer[] gradosCumplimiento = new Integer[gradosCumplimientoInt.length];
        for (int i = 0; i < gradosCumplimientoInt.length; i++) {
            gradosCumplimiento[i]= gradosCumplimientoInt[i];
        }


        TableRow.LayoutParams tableRowParams = new TableRow.LayoutParams();

        //Cabecera de la tabla
        TableRow tableRow = (TableRow)inflater.inflate(R.layout.table_row_relevamiento,tableLayout,false);
        tableRow.setId(ViewId.getInstance().getUniqueId());

        TextView rowHeaderText=new TextView(context);
        rowHeaderText.setId(ViewId.getInstance().getUniqueId());
        rowHeaderText.setText("");
        tableRow.addView(rowHeaderText);
        for (int j= 0; j < mCandidatos.length; j++) {
            View candidatoView = GradoCumplimientoViewHelper.GetGradoCumplimientoView(candidatosColores.getColor(j,0), inflater,tableRow);
            candidatoView.setId(ViewId.getInstance().getUniqueId());
            tableRow.addView(candidatoView);
        }
        tableLayout.addView(tableRow);


        tableRowParams = new TableRow.LayoutParams();
        //tableRowParams.weight=1;
        for (String materiale : mMateriales) {
            tableRow = (TableRow) inflater.inflate(R.layout.table_row_relevamiento, tableLayout, false);

            rowHeaderText = new TextView(context);
            rowHeaderText.setId(ViewId.getInstance().getUniqueId());
            rowHeaderText.setGravity(Gravity.LEFT);
            rowHeaderText.setText(materiale);

            tableRow.addView(rowHeaderText);

            for (int j = 0; j < mCandidatos.length; j++) {

                DatoRelevamientoPublicidad dato = mDatosRelevamiento.get(getKey(mCandidatos[j], materiale));
                View spinner = crearSpinner(context, candidatosColores.getColor(j, 0), gradosCumplimiento, dato);

                tableRow.addView(spinner, tableRowParams);
            }

            tableLayout.addView(tableRow);
        }
    }

    private View crearSpinner(Context context, int color, Integer[] gradosCumplimiento, final DatoRelevamientoPublicidad dato) {
        Spinner spinner = new Spinner(context);
        spinner.setId(ViewId.getInstance().getUniqueId());

        GradoCumplimientoAdapter adapter = new GradoCumplimientoAdapter(context, android.R.layout.simple_spinner_item,gradosCumplimiento, color);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(dato.getCumplimiento());

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                dato.setCumplimiento((int) parent.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                dato.setCumplimiento(0);
            }
        });

        return spinner;
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
        void onItemSaved(Relevamiento relevamiento);
    }

    public class GradoCumplimientoAdapter extends ArrayAdapter<Integer> {

        //private String[] mObjects;
        private int mBaseColor;
        private int mMaxGrado;

        public GradoCumplimientoAdapter(Context ctx, int txtViewResourceId, Integer[] grados, int baseColor) {
            super(ctx, txtViewResourceId, grados);
            //mObjects = objects;
            mBaseColor = baseColor;
            mMaxGrado=0;
            for (Integer grado : grados) {
                if(grado>mMaxGrado)
                    mMaxGrado=grado;
            }

        }

        @Override
        public View getDropDownView(int position, View cnvtView, ViewGroup prnt) {
            return getCustomView(position, cnvtView, prnt);
        }

        @Override
        public View getView(int pos, View cnvtView, ViewGroup prnt) {
            return getCustomView(pos, cnvtView, prnt);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            return GradoCumplimientoViewHelper.GetGradoCumplimientoView(mBaseColor,getItem(position),mMaxGrado,inflater, parent);
        }
    }

    public static class GradoCumplimientoViewHelper{
        private static final double MIN_RATIO_CUMPLIMIENTO = 0.15;

        public static View GetGradoCumplimientoView(int baseColor, int gradoCumplimiento, int maxGradoCumplimiento, LayoutInflater inflater, ViewGroup parent){
            View view = inflater.inflate(R.layout.spinner_grado_cumplimiento, parent, false);
            view.setId(ViewId.getInstance().getUniqueId());

            double ratioCumplimiento = 0;//(float)gradoCumplimiento / maxGradoCumplimiento;
            if(gradoCumplimiento>0) {
                if (gradoCumplimiento < maxGradoCumplimiento) {
                    ratioCumplimiento = MIN_RATIO_CUMPLIMIENTO +
                            ((gradoCumplimiento - 1.0) / (maxGradoCumplimiento - 1.0)) * (1.0 - MIN_RATIO_CUMPLIMIENTO);
                } else {
                    ratioCumplimiento = 1.0;
                }
            }
            Double alpha = 255.0 * ratioCumplimiento;

            int color = Color.argb(alpha.intValue(), Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
            ImageView imgColor = (ImageView) view;
            imgColor.setBackgroundColor(color);

            return view;
        }

        public static View GetGradoCumplimientoView(int color, LayoutInflater inflater, ViewGroup parent) {
            return GetGradoCumplimientoView(color, 1,1,inflater,parent);
        }
    }

    public static class ViewId {

        private static ViewId INSTANCE = new ViewId();

        private AtomicInteger seq;

        private ViewId() {
            seq = new AtomicInteger(Integer.MAX_VALUE);
        }

        public int getUniqueId() {
            return seq.getAndDecrement();
        }

        public static ViewId getInstance() {
            return INSTANCE;
        }
    }

}


