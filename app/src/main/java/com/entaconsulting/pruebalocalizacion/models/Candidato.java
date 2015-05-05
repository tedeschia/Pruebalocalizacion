package com.entaconsulting.pruebalocalizacion.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Candidato implements Parcelable {

    @com.google.gson.annotations.SerializedName("id")
    private String Id;
    @com.google.gson.annotations.SerializedName("nombre")
    private String Nombre;
    @com.google.gson.annotations.SerializedName("color")
    private String Color;
    @com.google.gson.annotations.SerializedName("proyectoId")
    private String ProyectoId;
    @com.google.gson.annotations.SerializedName("orden")
    private int Orden;

    public Candidato(){

    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Candidato && ((Candidato) o).getId().equals(getId());
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
        dest.writeString(getId());
        dest.writeString(getNombre());
        dest.writeString(getColor());
        dest.writeString(getProyectoId());
        dest.writeInt(getOrden());
    }
    public static final Creator<Candidato> CREATOR = new Creator<Candidato>() {
        public Candidato createFromParcel(Parcel pc) {
            return new Candidato(pc);
        }
        public Candidato[] newArray(int size) {
            return new Candidato[size];
        }
    };
    public Candidato(Parcel pc){
        setId(pc.readString());
        setNombre(pc.readString());
        setColor(pc.readString());
        setProyectoId(pc.readString());
        setOrden(pc.readInt());
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getNombre() {
        return Nombre;
    }

    public void setNombre(String nombre) {
        Nombre = nombre;
    }

    public String getColor() {
        return Color;
    }

    public void setColor(String color) {
        Color = color;
    }

    public String getProyectoId() {
        return ProyectoId;
    }

    public void setProyectoId(String proyectoId) {
        ProyectoId = proyectoId;
    }

    public int getOrden() {
        return Orden;
    }

    public void setOrden(int orden) {
        Orden = orden;
    }
}
