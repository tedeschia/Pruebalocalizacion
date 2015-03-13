package com.entaconsulting.pruebalocalizacion;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Date;

public class Relevamiento implements Parcelable {

    @com.google.gson.annotations.SerializedName("id")
    private String mId;
    @com.google.gson.annotations.SerializedName("fecha")
    private Date mFecha;
    @com.google.gson.annotations.SerializedName("datos")
    private String mDatos;
    @com.google.gson.annotations.SerializedName("latitud")
    private double mLatitud;
    @com.google.gson.annotations.SerializedName("longitud")
    private double mLongitud;
    @com.google.gson.annotations.SerializedName("direccionEstado")
    private String mDireccionEstado;
    @com.google.gson.annotations.SerializedName("direccion")
    private String mDireccion;


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
        return o instanceof Relevamiento && ((Relevamiento) o).mId.equals(mId);
    }


    public Date getFecha() {
        return mFecha;
    }

    public void setFecha(Date mFecha) {
        this.mFecha = mFecha;
    }

    public String getDatos() {
        return mDatos;
    }

    public void setDatos(String mDatos) {
        this.mDatos = mDatos;
    }

    public double getLatitud() {
        return mLatitud;
    }

    public void setLatitud(double mLatitud) {
        this.mLatitud = mLatitud;
    }

    public double getLongitud() {
        return mLongitud;
    }

    public void setLongitud(double mLongitud) {
        this.mLongitud = mLongitud;
    }

    public String getDireccionEstado() {
        return mDireccionEstado;
    }

    public void setDireccionEstado(String mDireccionEstado) {
        this.mDireccionEstado = mDireccionEstado;
    }

    public String getDireccion() {
        return mDireccion;
    }

    public void setDireccion(String mDireccion) {
        this.mDireccion = mDireccion;
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
        dest.writeString(mDatos);
        dest.writeSerializable(mFecha);
        dest.writeString(mDireccion);
        dest.writeString(mDireccionEstado);
        dest.writeDouble(mLatitud);
        dest.writeDouble(mLongitud);
    }
    public static final Parcelable.Creator<Relevamiento> CREATOR = new Parcelable.Creator<Relevamiento>() {
        public Relevamiento createFromParcel(Parcel pc) {
            return new Relevamiento(pc);
        }
        public Relevamiento[] newArray(int size) {
            return new Relevamiento[size];
        }
    };
    public Relevamiento(Parcel pc){
        mId = pc.readString();
        mDatos = pc.readString();
        mFecha = (Date) pc.readSerializable();
        mDireccion = pc.readString();
        mDireccionEstado = pc.readString();
        mLatitud = pc.readDouble();
        mLongitud = pc.readDouble();
    }


    public class EstadosDireccion{
        public static final String Pendiente = "pendiente";
        public static final String Resuelta = "resuelta";
        public static final String NoEncontrada = "noEncontrada";
        public static final String MultiplesCandidatos = "multiplesCandidatos";
        public static final String ErrorInesperado = "errorInesperado";
    }
}
