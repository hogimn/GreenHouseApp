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
    private int mCurrentMode;

    private final int MODE_AUTO   = 0;
    private final int MODE_MANUAL = 1;

    private final int STATE_LED_ON = 1;
    private final int STATE_LED_OFF = 2;
    private final int STATE_FAN_ON = 3;
    private final int STATE_FAN_OFF = 4;
    private final int STATE_DRAIN_OPEN = 5;
    private final int STATE_DRAIN_CLOSE = 6;
    private final int STATE_SOLENOID_OPEN = 7;
    private final int STATE_SOLENOID_CLOSE = 8;
    private final int STATE_HUMIDIFIER_ON = 9;
    private final int STATE_HUMIDIFIER_OFF = 10;

    private volatile int mStateLED;
    private volatile int mStateFan;
    private volatile int mStateDrain;
    private volatile int mStateSolenoid;
    private volatile int mStateHumidifier;


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

        asyncGetMode();

        asyncGetStates();

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

        /* when user clicks button, mode is toggled */
        mBtnMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                asyncToggleMode();
            }
        });

        mCVWater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStateSolenoid == STATE_SOLENOID_OPEN) {
                    asyncStateChange(STATE_SOLENOID_CLOSE);
                } else if (mStateSolenoid == STATE_SOLENOID_CLOSE) {
                    asyncStateChange(STATE_SOLENOID_OPEN);
                }
            }
        });

        mCVDrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStateDrain == STATE_DRAIN_OPEN) {
                    asyncStateChange(STATE_DRAIN_CLOSE);
                } else if (mStateDrain == STATE_DRAIN_CLOSE) {
                    asyncStateChange(STATE_DRAIN_OPEN);
                }
            }
        });

        mCVLED.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStateLED == STATE_LED_ON) {
                    asyncStateChange(STATE_LED_OFF);
                } else if (mStateLED == STATE_LED_OFF) {
                    asyncStateChange(STATE_LED_ON);
                }
            }
        });

        mCVFan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStateFan == STATE_FAN_ON) {
                    asyncStateChange(STATE_FAN_OFF);
                } else if (mStateFan == STATE_FAN_OFF) {
                    asyncStateChange(STATE_FAN_ON);
                }
            }
        });

        mCVHumidifier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStateHumidifier == STATE_HUMIDIFIER_ON) {
                    asyncStateChange(STATE_HUMIDIFIER_OFF);
                }
                else if (mStateHumidifier == STATE_HUMIDIFIER_OFF) {
                    asyncStateChange(STATE_HUMIDIFIER_ON);
                }
            }
        });
    }

    private void asyncStateChange(int cmd)
    {
        Thread threadStateChange = new Thread(new Runnable() {
            @Override
            public void run() {
                OutputStream os;
                InputStream is;
                BufferedReader br;
                byte[] sendBytes;

                Socket sock = new Socket();

                try {
                    /* open socket and connect to server */
                    sock.connect(new InetSocketAddress(NetworkUtil.NETWORK_SERVER_IP,
                                    NetworkUtil.NETWORK_SERVER_PORT),
                            1000);

                    /* send command */
                    os = sock.getOutputStream();
                    sendBytes = NetworkUtil.NETWORK_CMD_STATE_CHANGE_CLIENT_TO_SERVER.getBytes();
                    os.write(sendBytes, 0, sendBytes.length);
                    os.flush();

                    Thread.sleep(500);

                    /* send second command */
                    sendBytes = String.valueOf(cmd).getBytes();
                    os.write(sendBytes, 0, sendBytes.length);
                    os.flush();

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
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

        threadStateChange.start();
    }

    private void asyncGetStates() {

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
                    /* connect to server */
                    sock.connect(new InetSocketAddress(NetworkUtil.NETWORK_SERVER_IP,
                                    NetworkUtil.NETWORK_SERVER_PORT),
                            1000);

                    /* send command */
                    os = sock.getOutputStream();
                    sendBytes = NetworkUtil.NETWORK_CMD_STATE_SERVER_TO_CLIENT.getBytes();
                    os.write(sendBytes, 0, sendBytes.length);
                    os.flush();

                    /* receive states data */
                    is = sock.getInputStream();
                    br = new BufferedReader(new InputStreamReader(is));
                    while (mActivity != null) {
                        int StateLED = Integer.parseInt(br.readLine());
                        int StateFan = Integer.parseInt(br.readLine());
                        int StateSolenoid = Integer.parseInt(br.readLine());
                        int StateDrain = Integer.parseInt(br.readLine());
                        int StateHumidifier = Integer.parseInt(br.readLine());

                        try {
                            /* update UI */
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (StateLED == STATE_LED_ON) {
                                        mCVLED.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_active));
                                        mIVLED.setImageResource(R.drawable.active_bolb);

                                        mStateLED = STATE_LED_ON;
                                    } else if (StateLED == STATE_LED_OFF) {
                                        mCVLED.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_deactive));
                                        mIVLED.setImageResource(R.drawable.bolb);

                                        mStateLED = STATE_LED_OFF;
                                    }

                                    if (StateFan == STATE_FAN_ON) {
                                        mCVFan.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_active));
                                        mIVFan.setImageResource(R.drawable.active_fan);

                                        mStateFan = STATE_FAN_ON;
                                    } else if (StateFan == STATE_FAN_OFF) {
                                        mCVFan.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_deactive));
                                        mIVFan.setImageResource(R.drawable.fan);

                                        mStateFan = STATE_FAN_OFF;
                                    }

                                    if (StateSolenoid == STATE_SOLENOID_OPEN) {
                                        mCVWater.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_active));
                                        mIVWater.setImageResource(R.drawable.active_droplet);

                                        mStateSolenoid = STATE_SOLENOID_OPEN;
                                    } else if (StateSolenoid == STATE_SOLENOID_CLOSE) {
                                        mCVWater.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_deactive));
                                        mIVWater.setImageResource(R.drawable.droplet);

                                        mStateSolenoid = STATE_SOLENOID_CLOSE;
                                    }

                                    if (StateDrain == STATE_DRAIN_OPEN) {
                                        mCVDrain.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_active));
                                        mIVDrain.setImageResource(R.drawable.active_drainage);

                                        mStateDrain = STATE_DRAIN_OPEN;
                                    } else if (StateDrain == STATE_DRAIN_CLOSE) {
                                        mCVDrain.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_deactive));
                                        mIVDrain.setImageResource(R.drawable.drainage);

                                        mStateDrain = STATE_DRAIN_CLOSE;
                                    }

                                    if (StateHumidifier == STATE_HUMIDIFIER_ON) {
                                        mCVHumidifier.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_active));
                                        mIVHumidifier.setImageResource(R.drawable.active_humidifier);

                                        mStateHumidifier = STATE_HUMIDIFIER_ON;
                                    } else if (StateHumidifier == STATE_HUMIDIFIER_OFF) {
                                        mCVHumidifier.setCardBackgroundColor(ContextCompat.getColor(mActivity,
                                                R.color.card_background_color_deactive));
                                        mIVHumidifier.setImageResource(R.drawable.humidifier);

                                        mStateHumidifier = STATE_HUMIDIFIER_OFF;
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
    private void asyncToggleMode()
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
                    sock.connect(new InetSocketAddress(NetworkUtil.NETWORK_SERVER_IP,
                                    NetworkUtil.NETWORK_SERVER_PORT),
                            1000);

                    /* send command */
                    os = sock.getOutputStream();
                    sendBytes = NetworkUtil.NETWORK_CMD_MODE_TOGGLE_CLIENT_TO_SERVER.getBytes();
                    os.write(sendBytes, 0, sendBytes.length);
                    os.flush();

                    /* receive changed mode data */
                    is = sock.getInputStream();
                    br = new BufferedReader(new InputStreamReader(is));
                    while (mActivity != null) {

                        int mode = Integer.parseInt(br.readLine());

                        /* update mCurrentMode */
                        mCurrentMode = mode;

                        /* update UI */
                        try {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mode == MODE_AUTO)
                                    {
                                        mTVMode.setText("Current Mode is AUTO");
                                    }
                                    else if (mode == MODE_MANUAL)
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

    private void asyncGetMode()
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
                    sock.connect(new InetSocketAddress(NetworkUtil.NETWORK_SERVER_IP,
                                    NetworkUtil.NETWORK_SERVER_PORT),
                            1000);

                    /* send command */
                    os = sock.getOutputStream();
                    sendBytes = NetworkUtil.NETWORK_CMD_MODE_SERVER_TO_CLIENT.getBytes();
                    os.write(sendBytes, 0, sendBytes.length);
                    os.flush();

                    /* receive mode data */
                    is = sock.getInputStream();
                    br = new BufferedReader(new InputStreamReader(is));
                    while (mActivity != null) {

                        int mode = Integer.parseInt(br.readLine());

                        try {
                            /* update UI */
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mode == MODE_AUTO) {
                                        mTVMode.setText("Current Mode is AUTO");
                                    } else if (mode == MODE_MANUAL) {
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