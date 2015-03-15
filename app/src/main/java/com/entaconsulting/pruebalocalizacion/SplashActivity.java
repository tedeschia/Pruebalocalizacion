package com.entaconsulting.pruebalocalizacion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuItem;

import com.entaconsulting.pruebalocalizacion.helpers.DataHelper;
import com.entaconsulting.pruebalocalizacion.helpers.GooglePlayServicesHelper;
import com.entaconsulting.pruebalocalizacion.helpers.MessageHelper;


public class SplashActivity extends Activity implements GooglePlayServicesHelper.OnConnectedCallback {

    private static final long SPLASH_TIME_OUT_ERROR = 4000;
    private static final long SPLASH_TIME_OUT_OK = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //conecto acá porque si no no activa después el gps.. no se por qué
        GooglePlayServicesHelper GooglePlayServices = new GooglePlayServicesHelper(this);

        try{

        DataHelper client = new DataHelper(this);
        client.connect(new DataHelper.IServiceCallback() {
                    @Override
                    public void onServiceReady() {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent i = new Intent(SplashActivity.this, RelevamientoActivity.class);
                                startActivity(i);

                                finish();
                            }
                        }, SPLASH_TIME_OUT_OK);

                    }

                    @Override
                    public void onAuthenticationFailed() {
                        error("No se ha podido autenticar el usuario");
                    }
                });
        } catch(Exception e){
            error(e.getMessage());
        }


    }
    private void error(String texto){
        MessageHelper.createAndShowDialog(SplashActivity.this, texto , "Error");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, SPLASH_TIME_OUT_ERROR);
    }

    @Override
    public void onConnected() {

    }
}
