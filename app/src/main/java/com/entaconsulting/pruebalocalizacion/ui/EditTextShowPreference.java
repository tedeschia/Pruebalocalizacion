package com.entaconsulting.pruebalocalizacion.ui;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * Created by atedeschi on 7/5/15.
 */
public class EditTextShowPreference extends EditTextPreference {

    public EditTextShowPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public EditTextShowPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextShowPreference(Context context) {
        super(context);
    }

    public void show(){
        showDialog(null);
    }
}
