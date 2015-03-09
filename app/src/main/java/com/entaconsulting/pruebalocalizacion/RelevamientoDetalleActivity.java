package com.entaconsulting.pruebalocalizacion;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;


public class RelevamientoDetalleActivity extends ActionBarActivity
        implements RelevamientoDetalleFragment.OnFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_relevamiento_detalle);

        if (savedInstanceState == null) {
            RelevamientoDetalleFragment fragment = RelevamientoDetalleFragment.newInstance(RelevamientoDetalleFragment.ACTION_ADD);
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
    public void onItemSaved() {
        NavUtils.navigateUpTo(this, new Intent(this, RelevamientoActivity.class));
    }
}
