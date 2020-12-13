package com.mds.smartcontroller.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.mds.smartcontroller.R;
import com.mds.smartcontroller.utils.NetworkUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class HomeFragment extends Fragment {

    /* Activity where the fragment belongs to */
    private Activity mActivity;

    /* TextView to show current mode "MANUAL or AUTO" */
    private TextView mTVMode;

    /* Button to toggle mode */
    private Button mBtnMode;

    /* CardView to control functional devices */
    private CardView mCVWater;
    private CardView mCVDrain;
    private CardView mCVLED;
    private CardView mCVFan;
    private CardView mCVHumidifier;

    /* ImageView to show the state in which each device is */
    private ImageView mIVWater;
    private ImageView mIVDrain;
    private ImageView mIVLED;
    private ImageView mIVFan;
    private ImageView mIVHumidifier;

    /* current mode */
    private String mCurrentMode;

    private final String MODE_AUTO   = "0";
    private final String MODE_MANUAL = "1";

    private final String STATE_LED_ON = "1";
    private final String STATE_LED_OFF = "2";
    private final String STATE_FAN_ON = "3";
    private final String STATE_FAN_OFF = "4";
    private final String STATE_DRAIN_OPEN = "5";
    private final String STATE_DRAIN_CLOSE = "6";
    private final String STATE_SOLENOID_OPEN = "7";
    private final String STATE_SOLENOID_CLOSE = "8";
    private final String STATE_HUMIDIFIER_ON = "9";
    private final String STATE_HUMIDIFIER_OFF = "10";

    private final String TAG = getClass().getName();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        initializeView(v);

        getMode();

        getStates();

        return v;
    }

    private void initializeView(View v)
    {
        mTVMode = v.findViewById(R.id.tv_mode);

        mBtnMode = v.findViewById(R.id.btn_mode);

        mCVWater = v.findViewById(R.id.cv_water);
        mCVDrain = v.findViewById(R.id.cv_drain);
        mCVLED = v.findViewById(R.id.cv_led);
        mCVFan = v.findViewById(R.id.cv_fan);
        mCVHumidifier = v.findViewById(R.id.cv_humidifier);

        mIVWater = v.findViewById(R.id.iv_water);
        mIVDrain = v.findViewById(R.id.iv_drain);
        mIVLED = v.findViewById(R.id.iv_led);
        mIVFan = v.findViewById(R.id.iv_fan);
        mIVHumidifier = v.findViewById(R.id.iv_humidifier);

        /* when user click button, toggle mode */
        mBtnMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMode();
            }
        });

        mCVWater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        mCVDrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        mCVLED.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        mCVFan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        mCVHumidifier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }
    private void getStates() {

        /* receive data from the specified sensors */
        Thread mStateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                OutputStream os;
                InputStream is;
                BufferedReader br;
                byte[] sendBytes;

                Socket sock = new Socket();

                try {
                    /* onnect to server */
                    sock.connect(new InetSocketAddress(NetworkUtil.SERVER_IP,
                                    NetworkUtil.SERVER_PORT),
                            1000);

                    /* send command */
                    os = sock.getOutputStream();
                    sendBytes = NetworkUtil.SOCK_CMD_STATE_SERVER_TO_CLIENT.getBytes();
                    os.write(sendBytes, 0, sendBytes.length);
                    os.flush();

                    /* receive states data */
                    is = sock.getInputStream();
                    br = new BufferedReader(new InputStreamReader(is));
                    while (mActivity != null) {
                        String strStateLED = br.readLine();
                        String strStateFan = br.readLine();
                        String strStateSolenoid = br.readLine();
                        String strStateDrain = br.readLine();
                        String strStateHumidifier = br.readLine();

                        /* update UI */
                        try {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (strStateLED.equals(STATE_LED_ON)) {
                                        mCVLED.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_active));
                                        mIVWater.setImageResource(R.drawable.active_bolb);
                                    }
                                    else {
                                        mCVLED.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_deactive));
                                        mIVWater.setImageResource(R.drawable.bolb);
                                    }
                                    if (strStateFan.equals(STATE_FAN_ON)) {
                                        mCVFan.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_active));
                                        mIVFan.setImageResource(R.drawable.active_fan);
                                    }
                                    else {
                                        mCVFan.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_deactive));
                                        mIVFan.setImageResource(R.drawable.fan);
                                    }
                                    if (strStateSolenoid.equals(STATE_SOLENOID_OPEN)) {
                                        mCVWater.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_active));
                                        mIVWater.setImageResource(R.drawable.active_droplet);
                                    }
                                    else {
                                        mCVWater.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_deactive));
                                        mIVWater.setImageResource(R.drawable.droplet);
                                    }
                                    if (strStateDrain.equals(STATE_DRAIN_CLOSE)) {
                                        mCVDrain.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_active));
                                        mIVDrain.setImageResource(R.drawable.active_drainage);
                                    }
                                    else {
                                        mCVDrain.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_deactive));
                                        mIVDrain.setImageResource(R.drawable.drainage);
                                    }
                                    if (strStateHumidifier.equals(STATE_HUMIDIFIER_ON)) {
                                        mCVHumidifier.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_active));
                                        mIVHumidifier.setImageResource(R.drawable.active_humidifier);
                                    }
                                    else {
                                        mCVHumidifier.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_deactive));
                                        mIVHumidifier.setImageResource(R.drawable.humidifier);
                                    }
                                }
                            });
                        } catch (NullPointerException e) {
                            break;
                        }
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    /* make sure to close socket */
                    try {
                        sock.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mStateThread.start();
    }
    private void toggleMode()
    {
        Thread mThreadModeToggle = new Thread(new Runnable() {
            @Override
            public void run() {
                OutputStream os;
                InputStream is;
                BufferedReader br;
                byte[] sendBytes;

                Socket sock = new Socket();

                try {
                    /* open socket and connect to server */
                    sock.connect(new InetSocketAddress(NetworkUtil.SERVER_IP,
                                    NetworkUtil.SERVER_PORT),
                            1000);

                    /* send command */
                    os = sock.getOutputStream();
                    sendBytes = NetworkUtil.SOCK_CMD_MODE_TOGGLE_CLIENT_TO_SERVER.getBytes();
                    os.write(sendBytes, 0, sendBytes.length);
                    os.flush();

                    /* receive changed mode data */
                    is = sock.getInputStream();
                    br = new BufferedReader(new InputStreamReader(is));
                    while (mActivity != null) {

                        String strMode = br.readLine();

                        /* update mCurrentMode */
                        mCurrentMode = strMode;

                        /* update UI */
                        try {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (strMode.equals(MODE_AUTO))
                                    {
                                        mTVMode.setText("Current Mode is AUTO");
                                    }
                                    else if (strMode.equals(MODE_MANUAL))
                                    {
                                        mTVMode.setText("Current Mode is MANUAL");
                                    }
                                }
                            });
                        } catch (NullPointerException e) {
                            break;
                        }
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    /* make sure to close socket */
                    try {
                        sock.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mThreadModeToggle.start();
    }

    private void getMode()
    {
        /* receive mode data from server */
        Thread mThreadMode = new Thread(new Runnable() {
            @Override
            public void run() {
                OutputStream os;
                InputStream is;
                BufferedReader br;
                byte[] sendBytes;

                Socket sock = new Socket();

                try {
                    /* open socket and connect to server */
                    sock.connect(new InetSocketAddress(NetworkUtil.SERVER_IP,
                                    NetworkUtil.SERVER_PORT),
                            1000);

                    /* send command */
                    os = sock.getOutputStream();
                    sendBytes = NetworkUtil.SOCK_CMD_MODE_SERVER_TO_CLIENT.getBytes();
                    os.write(sendBytes, 0, sendBytes.length);
                    os.flush();

                    /* receive mode data */
                    is = sock.getInputStream();
                    br = new BufferedReader(new InputStreamReader(is));
                    while (mActivity != null) {

                        String strMode = br.readLine();

                        /* update UI */
                        try {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (strMode.equals(MODE_AUTO))
                                    {
                                        mTVMode.setText("Current Mode is AUTO");
                                    }
                                    else if (strMode.equals(MODE_MANUAL))
                                    {
                                        mTVMode.setText("Current Mode is MANUAL");
                                    }
                                }
                            });
                        } catch (NullPointerException e) {
                            break;
                        }
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    /* make sure to close socket */
                    try {
                        sock.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mThreadMode.start();
    }
}
