package com.entaconsulting.pruebalocalizacion.helpers;

import com.entaconsulting.pruebalocalizacion.models.Candidato;
import com.entaconsulting.pruebalocalizacion.models.Categoria;

import java.util.ArrayList;

/**
 * Created by atedeschi on 1/5/15.
 */
public class ConfigurationHelper {
    private static ArrayList<Candidato> candidatos;
    private static ArrayList<Categoria> categorias;
    private static String proyectoClave;
    private static DataHelper client;


    public static ArrayList<Candidato> getCandidatos() {
        return candidatos;
    }

    public static void setCandidatos(ArrayList<Candidato> candidatos) {
        ConfigurationHelper.candidatos = candidatos;
    }

    public static ArrayList<Categoria> getCategorias() {
        return categorias;
    }

    public static void setCategorias(ArrayList<Categoria> categorias) {
        ConfigurationHelper.categorias = categorias;
    }

    public static String[] getCategoriasArray() {
        String[] result = new String[categorias.size()];
        int i=0;
        for(Categoria c:categorias){
            result[i++] = c.getNombre();
        }
        return result;
    }
    public static String[] getCandidatosArray() {
        String[] result = new String[candidatos.size()];
        int i=0;
        for(Candidato c:candidatos){
            result[i++] = c.getNombre();
        }
        return result;
    }

    public static String getProyectoClave() {
        return proyectoClave;
    }

    public static void setProyectoClave(String proyectoClave) {
        ConfigurationHelper.proyectoClave = proyectoClave;
    }

    public static DataHelper getClient() {
        return client;
    }

    public static void setClient(DataHelper client) {
        ConfigurationHelper.client = client;
    }
}
