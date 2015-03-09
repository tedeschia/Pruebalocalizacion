package com.entaconsulting.pruebalocalizacion;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class Relevamiento {

    @com.google.gson.annotations.SerializedName("id")
    private String mId;
    @com.google.gson.annotations.SerializedName("fecha")
    private Date mFecha;

    public Relevamiento(){

    }
    public Relevamiento(Date mFecha) {
        this.mFecha = mFecha;
    }

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Relevamiento && ((Relevamiento) o).mId == mId;
    }


    public Date getFecha() {
        return mFecha;
    }

    public void setFecha(Date mFecha) {
        this.mFecha = mFecha;
    }
}
