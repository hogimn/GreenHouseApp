package com.mds.smartcontroller.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mds.smartcontroller.R;

import java.util.ArrayList;

public class MusicItemAdapter extends ArrayAdapter<MusicItem> {

    private LayoutInflater inflater;
    private Fragment fragment;
    private Context context;

    public MusicItemAdapter(Context context,
                            ArrayList<MusicItem> items,
                            Fragment fragment) {
        super(context, 0, items);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public View getView(int position,
                        @Nullable View convertView,
                        @NonNull ViewGroup parent) {
        MusicItem musicItem = getItem(position);
        String title = musicItem.getTitle();
        boolean status = musicItem.getPlayOrStop();

        if (convertView == null) {
            convertView = inflater.from(context)
                    .inflate(R.layout.musiclist_item, parent, false);
        }

        TextView titleTV = convertView.findViewById(R.id.tv_title);
        TextView statusTV = convertView.findViewById(R.id.tv_state);
        titleTV.setText(title);
        if (status) {
            statusTV.setText("playing...");
        }
        else {
            statusTV.setText("");
        }

        return convertView;
    }
}
