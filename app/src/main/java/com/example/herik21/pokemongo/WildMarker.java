package com.example.herik21.pokemongo;

import com.google.android.gms.maps.model.Marker;

/**
 * Created by Herik21 on 16/09/2016.
 */
public class WildMarker {
    public int pkid,id;
    public double[] loc;
    public boolean visible;
    public Marker mk;

    public WildMarker(int pid,double[] markerloc,boolean visible,int listid){
        this.pkid=pid;
        this.id= listid;
        this.loc=markerloc;
        this.visible=visible;
    }
    public void setMarker(Marker marker){
        this.mk=marker;
    }
}
