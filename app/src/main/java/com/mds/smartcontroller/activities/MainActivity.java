package com.mds.smartcontroller.activities;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mds.smartcontroller.R;
import com.mds.smartcontroller.fragments.CameraFragment;
import com.mds.smartcontroller.fragments.HistoryFragment;
import com.mds.smartcontroller.fragments.HomeFragment;
import com.mds.smartcontroller.fragments.MusicFragment;

public class MainActivity extends FragmentActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;

            if (item.getItemId() == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_history) {
                selectedFragment = new HistoryFragment();
            } else if (item.getItemId() == R.id.nav_music) {
                selectedFragment = new MusicFragment();
            } else if (item.getItemId() == R.id.nav_camera) {
                selectedFragment = new CameraFragment();
            } else {
                return false;
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();

            return true;
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container,
                        new HomeFragment()).commit();
    }
}