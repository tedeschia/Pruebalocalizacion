package com.entaconsulting.pruebalocalizacion;

import android.os.Parcel;
import android.os.Parcelable;

public class DatoRelevamiento implements Parcelable {

    @com.google.gson.annotations.SerializedName("id")
    private String mId;
    @com.google.gson.annotations.SerializedName("candidato")
    private String mCandidato;
    @com.google.gson.annotations.SerializedName("material")
    private String mMaterial;
    @com.google.gson.annotations.SerializedName("cumplimiento")
    private int mCumplimiento;

    public DatoRelevamiento(){

    }
    public DatoRelevamiento(String candidato, String material, int cumplimiento, String id) {
        this.setId(id);
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
        dest.writeString(getCandidato());
        dest.writeString(getMaterial());
        dest.writeInt(getCumplimiento());

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

    public String getId() {
        return mId;
    }

    public void setId(String mId) {
        this.mId = mId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DatoRelevamiento && ((DatoRelevamiento) o).mId == mId;
    }


}
