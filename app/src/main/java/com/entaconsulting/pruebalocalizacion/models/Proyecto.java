package com.entaconsulting.pruebalocalizacion.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class Proyecto implements Parcelable {

    @com.google.gson.annotations.SerializedName("id")
    private String mId;
    @com.google.gson.annotations.SerializedName("nombre")
    private String mNombre;
    @com.google.gson.annotations.SerializedName("clave")
    private String mClave;

    public Proyecto(){

    }
    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Proyecto && ((Proyecto) o).mId.equals(mId);
    }


    public String getNombre() {
        return mNombre;
    }

    public void setNombre(String mNombre) {
        this.mNombre = mNombre;
    }

    public String getClave() {
        return mClave;
    }

    public void setClave(String mClave) {
        this.mClave = mClave;
    }

    /*
    IMPLEMENTACION PARCELABLE
     */
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeString(mNombre);
    }
    public static final Creator<Proyecto> CREATOR = new Creator<Proyecto>() {
        public Proyecto createFromParcel(Parcel pc) {
            return new Proyecto(pc);
        }
        public Proyecto[] newArray(int size) {
            return new Proyecto[size];
        }
    };
    public Proyecto(Parcel pc){
        mId = pc.readString();
        mNombre = pc.readString();
    }

}
