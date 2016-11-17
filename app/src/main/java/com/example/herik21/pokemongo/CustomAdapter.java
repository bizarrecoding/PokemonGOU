package com.example.herik21.pokemongo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * Created by Herik21 on 19/09/2016.
 */
public class CustomAdapter extends BaseAdapter {

    public List<TeamPokemon> team;
    public Context ctx;
    public CustomAdapter(Context context,List<TeamPokemon> myTeam){
        this.team = myTeam;
        this.ctx = context;
    }

    public void setTeam(List<TeamPokemon> team) {
        this.team = team;
    }
    @Override
    public int getCount() {
        return team.size();
    }

    @Override
    public Object getItem(int position) {
        return team.get(position);
    }

    @Override
    public long getItemId(int position) {
        return team.get(position).id;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        TeamPokemon tpk = team.get(position);
        if(view==null) {
            LayoutInflater linf = (LayoutInflater) ctx.getSystemService(ctx.LAYOUT_INFLATER_SERVICE);
            view = linf.inflate(R.layout.pkmnrow, null);
        }
        ImageView icon = (ImageView) view.findViewById(R.id.iconPk);
        new DownloadImageTask(icon).execute(tpk.basePokemon.imgFront);
        TextView name = (TextView) view.findViewById(R.id.name);
        name.setText(tpk.basePokemon.name);
        TextView chp = (TextView) view.findViewById(R.id.current);
        chp.setText(tpk.currenthp+"");
        TextView thp = (TextView) view.findViewById(R.id.total);
        thp.setText(tpk.hp+"");
        return view;
    }
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView target;

        public DownloadImageTask(ImageView img) {
            target = img;
        }

        protected Bitmap doInBackground(String... urls) {
            String imageURL = urls[0];
            Bitmap image = null;
            try {
                InputStream in = new URL(imageURL).openStream();
                image = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return image;
        }

        protected void onPostExecute(Bitmap result) {
            target.setImageBitmap(result);
        }
    }
}
