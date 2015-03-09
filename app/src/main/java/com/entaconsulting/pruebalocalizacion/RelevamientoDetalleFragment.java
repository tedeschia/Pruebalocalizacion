package com.entaconsulting.pruebalocalizacion;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

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
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String STATE_DATOS_RELEVAMIENTO = "datosCumplimiento";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private HashMap<String, DatoRelevamiento> mDatosRelevamiento;
    private String[] mCandidatos;
    private String[] mMateriales;

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RelevamientoFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RelevamientoDetalleFragment newInstance(String param1, String param2) {
        RelevamientoDetalleFragment fragment = new RelevamientoDetalleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
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
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        setHasOptionsMenu(true);

        leerConfiguracion();

        if(savedInstanceState!=null){
            mDatosRelevamiento = (HashMap<String, DatoRelevamiento>)savedInstanceState.getSerializable(STATE_DATOS_RELEVAMIENTO);
        }else{
            mDatosRelevamiento = generarDatos();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putSerializable(STATE_DATOS_RELEVAMIENTO, mDatosRelevamiento);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    private HashMap<String, DatoRelevamiento> generarDatos() {
        HashMap<String, DatoRelevamiento> datos = new HashMap<>();
        for (String mMaterial : mMateriales) {
            for (String mCandidato : mCandidatos) {
                DatoRelevamiento dato = new DatoRelevamiento(mCandidato, mMaterial, 0, "");
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

        buildTable(table, inflater, getActivity());
        return main;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_relevamiento_detalle_actions, menu);
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
        for (String mMateriale : mMateriales) {
            tableRow = (TableRow) inflater.inflate(R.layout.table_row_relevamiento, tableLayout, false);

            rowHeaderText = new TextView(context);
            rowHeaderText.setId(ViewId.getInstance().getUniqueId());
            rowHeaderText.setGravity(Gravity.LEFT);
            rowHeaderText.setText(mMateriale);

            tableRow.addView(rowHeaderText);

            for (int j = 0; j < mCandidatos.length; j++) {

                DatoRelevamiento dato = mDatosRelevamiento.get(getKey(mCandidatos[j], mMateriale));
                View spinner = crearSpinner(context, candidatosColores.getColor(j, 0), gradosCumplimiento, dato);

                tableRow.addView(spinner, tableRowParams);
            }

            tableLayout.addView(tableRow);
        }
    }

    private View crearSpinner(Context context, int color, Integer[] gradosCumplimiento, final DatoRelevamiento dato) {
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
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
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


