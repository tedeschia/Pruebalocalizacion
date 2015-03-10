package com.entaconsulting.pruebalocalizacion;

import android.content.Intent;
import android.location.Location;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class RelevamientoDetalleActivity extends ActionBarActivity
        implements RelevamientoDetalleFragment.OnFragmentInteractionListener {

    public static final int REQUEST_ADD = 1;
    public static final String EXTRA_RELEVAMIENTO = "relevamiento";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_relevamiento_detalle);

        if (savedInstanceState == null) {

            String action = RelevamientoDetalleFragment.ARG_ACTION_ADD;
            String relevamientoId = "";
            Location localizacion = null;
            // getIntent() is a method from the started activity
            Intent intent = getIntent(); // gets the previously created intent
            if(intent!=null){
                action = intent.getStringExtra(RelevamientoDetalleFragment.ARG_ACTION_MESSAGE);
                relevamientoId = intent.getStringExtra(RelevamientoDetalleFragment.ARG_RELEVAMIENTO_ID);
                localizacion = intent.getParcelableExtra(RelevamientoDetalleFragment.ARG_LOCALIZACION);
            }

            RelevamientoDetalleFragment fragment = RelevamientoDetalleFragment.newInstance(action, relevamientoId, localizacion);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSaved(Relevamiento relevamiento) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(EXTRA_RELEVAMIENTO,relevamiento);
        setResult(RESULT_OK,returnIntent);
        finish();
    }
}
