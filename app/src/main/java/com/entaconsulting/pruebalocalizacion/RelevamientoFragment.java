package com.entaconsulting.pruebalocalizacion;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.table.query.Query;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;
import java.util.List;

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

    private OnFragmentInteractionListener mListener;
    private ProgressFilter mProgressFilter;

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
        setHasOptionsMenu(true);

        inicializarDatos();
    }

    private void inicializarDatos() {

        mProgressFilter = new ProgressFilter(getActivity());
        mClient = new DataHelper(getActivity(),mProgressFilter);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View main = inflater.inflate(R.layout.fragment_relevamiento, container, false);
        inicializarLista(getActivity(), main);

        return main;
    }

    private void inicializarLista(Context context, View view){
        mProgressBar = (ProgressBar) view.findViewById(R.id.loadingProgressBar);
        mProgressFilter.setProgressBar(mProgressBar);
        mProgressBar.setVisibility(ProgressBar.GONE);

        // Create the Mobile Service Client instance, using the provided
        // Mobile Service URL and key
        // Get the Mobile Service Table instance to use
        mRelevamientoTable = mClient.getClient().getSyncTable(Relevamiento.class);
        mPullQuery = mClient.getClient().getTable(Relevamiento.class).where();

        // Create an adapter to bind the items with the view
        mAdapter = new RelevamientoAdapter(context, R.layout.row_list_relevamiento);
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
            refreshItemsFromTable();
        }

        return true;
    }


    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
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
                    final List<Relevamiento> results =
                            mRelevamientoTable.read(mPullQuery).get();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.clear();

                            for (Relevamiento item : results) {
                                mAdapter.add(item);
                            }
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
                    mClient.getClient().getSyncContext().push().get();
                    mRelevamientoTable.pull(mPullQuery).get();
                    loadItemsFromTable();
                } catch (Exception e){
                    MessageHelper.createAndShowDialog(getActivity(), e, "Error");
                }

                return null;
            }
        }.execute();

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

    public class RelevamientoAdapter extends ArrayAdapter<Relevamiento> {

        /**
         * Adapter context
         */
        Context mContext;

        /**
         * Adapter View layout
         */
        int mLayoutResourceId;

        public RelevamientoAdapter(Context context, int layoutResourceId) {
            super(context, layoutResourceId);

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
            textDate.setText(currentItem.getFecha().toString());

            return row;
        }

    }
}

