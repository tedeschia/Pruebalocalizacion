package com.entaconsulting.pruebalocalizacion;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
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
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.entaconsulting.pruebalocalizacion.helpers.ConfigurationHelper;
import com.entaconsulting.pruebalocalizacion.models.Candidato;
import com.entaconsulting.pruebalocalizacion.models.Categoria;
import com.entaconsulting.pruebalocalizacion.models.Relevamiento;
import com.google.gson.Gson;

import org.w3c.dom.Text;

import java.util.ArrayList;
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

    private HashMap<String, DatoRelevamientoPublicidad> mDatosRelevamiento;
    private ArrayList<Candidato> mCandidatos;
    private ArrayList<Categoria> mMateriales;

    private OnFragmentInteractionListener mListener;
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
            mLocalizacion = getArguments().getParcelable(ARG_LOCALIZACION);
        }
        setHasOptionsMenu(true);

        leerConfiguracion();

        if (savedInstanceState != null) {
            mDatosRelevamiento = datosBdAVista((DatoRelevamientoPublicidad[]) savedInstanceState.getParcelableArray(STATE_DATOS_RELEVAMIENTO));
        } else {
            mDatosRelevamiento = datosBdAVista(null);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelableArray(STATE_DATOS_RELEVAMIENTO, datosVistaABd());
    }

    private DatoRelevamientoPublicidad[] datosVistaABd() {
        DatoRelevamientoPublicidad[] result = new DatoRelevamientoPublicidad[mDatosRelevamiento.values().size()];
        return mDatosRelevamiento.values().toArray(result);
    }

    private HashMap<String, DatoRelevamientoPublicidad> datosBdAVista(DatoRelevamientoPublicidad[] datosIniciales) {
        HashMap<String, DatoRelevamientoPublicidad> datos = new HashMap<>();
        if (datosIniciales == null) {
            for (Categoria mMaterial : mMateriales) {
                for (Candidato mCandidato : mCandidatos) {
                    DatoRelevamientoPublicidad dato = new DatoRelevamientoPublicidad(mCandidato.getNombre(), mMaterial.getNombre(), 0);
                    datos.put(getKey(mCandidato, mMaterial), dato);
                }
            }
        } else {
            for (DatoRelevamientoPublicidad dato : datosIniciales) {
                datos.put(getKey(dato), dato);
            }
        }
        return datos;
    }

    private String getKey(DatoRelevamientoPublicidad dato) {
        return dato.getCandidato() + dato.getMaterial();
    }

    private String getKey(Candidato candidato, Categoria material) {
        return candidato.getNombre() + material.getNombre();
    }

    private void leerConfiguracion() {
        mCandidatos = ConfigurationHelper.getCandidatos();
        mMateriales = ConfigurationHelper.getCategorias();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                guardar();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void guardar() {
        Relevamiento relevamiento = new Relevamiento(new Date());
        String json = new Gson().toJson(datosVistaABd());
        relevamiento.setDatos(json);
        relevamiento.setLatitud(mLocalizacion.getLatitude());
        relevamiento.setLongitud(mLocalizacion.getLongitude());
        if (mListener != null) {
            mListener.onItemSaved(relevamiento);
        }

    }

    private void buildTable(TableLayout tableLayout, LayoutInflater inflater, Context context) {

        Resources res = getResources();

        String[] gradosCumplimientoStr = res.getStringArray(R.array.grado_cumplimiento_array);
        //String[] gradosCumplimiento = new String[gradosCumplimientoStr.length];
        //for (int i = 0; i < gradosCumplimientoStr.length; i++) {
        //    gradosCumplimiento[i] = gradosCumplimientoStr[i];
        //}

        TableRow.LayoutParams tableRowParams = new TableRow.LayoutParams();

        //Cabecera de la tabla
        TableRow tableRow = (TableRow) inflater.inflate(R.layout.table_row_relevamiento, tableLayout, false);
        tableRow.setId(ViewId.getInstance().getUniqueId());

        TextView rowHeaderText = new TextView(context);
        rowHeaderText.setText("");
        tableRow.addView(rowHeaderText);
        for (Candidato candidato:mCandidatos) {
            View candidatoView = inflater.inflate(R.layout.spinner_grado_cumplimiento_selected, tableRow, false);
            candidatoView.setBackgroundColor(GradoCumplimientoViewHelper.GetGradoCumplimientoColor(Color.parseColor(candidato.getColor()),1,1));
            tableRow.addView(candidatoView);
        }
        tableLayout.addView(tableRow);


        tableRowParams = new TableRow.LayoutParams();
        for (Categoria materiale : mMateriales) {
            tableRow = (TableRow) inflater.inflate(R.layout.table_row_relevamiento, tableLayout, false);

            rowHeaderText = new TextView(context);
            rowHeaderText.setGravity(Gravity.LEFT);
            rowHeaderText.setText(materiale.getNombre());

            tableRow.addView(rowHeaderText);

            for (Candidato candidato:mCandidatos) {

                DatoRelevamientoPublicidad dato = mDatosRelevamiento.get(getKey(candidato, materiale));
                View spinner = crearSpinner(context, candidato.getColor(), gradosCumplimientoStr, dato);

                tableRow.addView(spinner, tableRowParams);
            }

            tableLayout.addView(tableRow);
        }
    }

    private View crearSpinner(Context context, String color, String[] gradosCumplimiento, final DatoRelevamientoPublicidad dato) {
        Spinner spinner = new Spinner(context);
        spinner.setId(ViewId.getInstance().getUniqueId());

        GradoCumplimientoAdapter adapter = new GradoCumplimientoAdapter(context, R.layout.spinner_grado_cumplimiento_selected, gradosCumplimiento, color);
        adapter.setDropDownViewResource(R.layout.spinner_grado_cumplimiento_items);
        spinner.setAdapter(adapter);
        spinner.setSelection(dato.getCumplimiento());

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                dato.setCumplimiento(position);
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

    public class GradoCumplimientoAdapter extends ArrayAdapter<String> {

        //private String[] mObjects;
        private int mBaseColor;
        private int mMaxGrado;

        public GradoCumplimientoAdapter(Context ctx, int txtViewResourceId, String[] grados, String baseColor) {
            super(ctx, txtViewResourceId, grados);
            //mObjects = objects;
            mBaseColor = Color.parseColor(baseColor);
            mMaxGrado = grados.length-1;
        }

        @Override
        public View getDropDownView(int position, View cnvtView, ViewGroup prnt) {
            TextView view=(TextView)cnvtView;
            if(view==null){
                LayoutInflater inflater = getActivity().getLayoutInflater();
                view = (TextView)inflater.inflate(R.layout.spinner_grado_cumplimiento_items, prnt, false);
            }
            int color = GradoCumplimientoViewHelper.GetGradoCumplimientoColor(mBaseColor, position, mMaxGrado);
            view.setBackgroundColor(color);
            view.setText(getItem(position));
            return view;
        }

        @Override
        public View getView(int pos, View cnvtView, ViewGroup prnt) {
            View view;
            if(cnvtView!=null){
                view = cnvtView;
            } else{
                LayoutInflater inflater = getActivity().getLayoutInflater();
                view = inflater.inflate(R.layout.spinner_grado_cumplimiento_selected, prnt, false);
            }
            int color = GradoCumplimientoViewHelper.GetGradoCumplimientoColor(mBaseColor, pos, mMaxGrado);
            view.setBackgroundColor(color);

            return view;
        }

        //public View getCustomView(int position, View convertView, ViewGroup parent) {
            //LayoutInflater inflater = getActivity().getLayoutInflater();
            //return GradoCumplimientoViewHelper.GetGradoCumplimientoView(mBaseColor, getItem(position), mMaxGrado, inflater, parent);
        //}
    }

    public static class GradoCumplimientoViewHelper {
        private static final double MIN_RATIO_CUMPLIMIENTO = 0.15;

        public static View GetGradoCumplimientoView(int baseColor, int gradoCumplimiento, int maxGradoCumplimiento, LayoutInflater inflater, ViewGroup parent) {
            View view = inflater.inflate(R.layout.spinner_grado_cumplimiento_selected, parent, false);
            view.setId(ViewId.getInstance().getUniqueId());

            int color = GetGradoCumplimientoColor(baseColor, gradoCumplimiento, maxGradoCumplimiento);
            ImageView imgColor = (ImageView) view;
            imgColor.setBackgroundColor(color);

            return view;
        }
        public static int GetGradoCumplimientoColor(int baseColor, int gradoCumplimiento, int maxGradoCumplimiento) {
            double ratioCumplimiento = 0;//(float)gradoCumplimiento / maxGradoCumplimiento;
            if (gradoCumplimiento > 0) {
                if (gradoCumplimiento < maxGradoCumplimiento) {
                    ratioCumplimiento = MIN_RATIO_CUMPLIMIENTO +
                            ((gradoCumplimiento - 1.0) / (maxGradoCumplimiento - 1.0)) * (1.0 - MIN_RATIO_CUMPLIMIENTO);
                } else {
                    ratioCumplimiento = 1.0;
                }
            }
            Double alpha = 255.0 * ratioCumplimiento;

            int color = Color.argb(alpha.intValue(), Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
            return color;
        }

        public static View GetGradoCumplimientoView(int color, LayoutInflater inflater, ViewGroup parent) {
            return GetGradoCumplimientoView(color, 1, 1, inflater, parent);
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
class DatoRelevamientoPublicidad implements Parcelable {

    @com.google.gson.annotations.SerializedName("candidato")
    private String mCandidato;
    @com.google.gson.annotations.SerializedName("material")
    private String mMaterial;
    @com.google.gson.annotations.SerializedName("cumplimiento")
    private int mCumplimiento;

    public DatoRelevamientoPublicidad() {

    }

    public DatoRelevamientoPublicidad(String candidato, String material, int cumplimiento) {
        setCandidato(candidato);
        setMaterial(material);
        setCumplimiento(cumplimiento);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mCandidato);
        dest.writeString(mMaterial);
        dest.writeInt(mCumplimiento);

    }
    public static final Parcelable.Creator<DatoRelevamientoPublicidad> CREATOR = new Parcelable.Creator<DatoRelevamientoPublicidad>() {
        public DatoRelevamientoPublicidad createFromParcel(Parcel pc) {
            return new DatoRelevamientoPublicidad(pc);
        }
        public DatoRelevamientoPublicidad[] newArray(int size) {
            return new DatoRelevamientoPublicidad[size];
        }
    };
    public DatoRelevamientoPublicidad(Parcel pc){
        mCandidato = pc.readString();
        mMaterial=pc.readString();
        mCumplimiento=pc.readInt();
    }

    public String getCandidato() {
        return mCandidato;
    }

    public void setCandidato(String candidato) {
        mCandidato = candidato;
    }

    public String getMaterial() {
        return mMaterial;
    }

    public void setMaterial(String mMaterial) {
        this.mMaterial = mMaterial;
    }

    public int getCumplimiento() {
        return mCumplimiento;
    }

    public void setCumplimiento(int mCumplimiento) {
        this.mCumplimiento = mCumplimiento;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DatoRelevamientoPublicidad && ((DatoRelevamientoPublicidad) o).mCandidato == mCandidato;
    }
}

