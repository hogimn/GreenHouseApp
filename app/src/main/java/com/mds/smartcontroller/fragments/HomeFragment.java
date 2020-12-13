package com.mds.smartcontroller.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    private final String MODE_AUTO   = "0";
    private final String MODE_MANUAL = "1";
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

        return v;
    }

    private void initializeView(View v)
    {
        mTVMode = v.findViewById(R.id.tv_mode);
        mBtnMode = v.findViewById(R.id.btn_mode);

        /* when user click button, toggle mode */
        mBtnMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMode();
            }
        });
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
