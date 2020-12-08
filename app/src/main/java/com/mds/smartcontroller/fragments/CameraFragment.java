package com.mds.smartcontroller.fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jjoe64.graphview.series.DataPoint;
import com.mds.smartcontroller.R;
import com.mds.smartcontroller.utils.MusicItem;
import com.mds.smartcontroller.utils.NetworkUtil;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CameraFragment extends Fragment {

    /* thread to get camera data from server */
    private Thread mCareraThread;

    /* current Activity where CameraFragment belongs to */
    private Activity mActivity;

    /* RGB data -> Bitmap (RGB_565) -> ImageView */
    private Bitmap mBitmap;

    /* ImageView to show streaming camera data */
    private ImageView cameraView;

    /* Logitech C270 Specific */
    private final int HEIGHT = 144;
    private final int WIDTH = 176;

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

        View v = inflater.inflate(R.layout.fragment_camera, container, false);

        cameraView = v.findViewById(R.id.camera_view);

        asyncGetCameraDataAndUpdateUI();

        return v;
    }

    private void asyncGetCameraDataAndUpdateUI() {

        mCareraThread = new Thread(new Runnable() {
            @Override
            public void run() {
                OutputStream os;
                InputStream is;
                byte[] sendBytes;
                byte[] receivedBytes;
                int[] receivedInts;
                int bytesRead;
                int j;

                Socket sock = new Socket();

                try {
                    /* connect to server */
                    sock.connect(new InetSocketAddress(NetworkUtil.SERVER_IP,
                                    NetworkUtil.SERVER_PORT),
                            1000);

                    /* send command */
                    os = sock.getOutputStream();
                    sendBytes = NetworkUtil.SOCK_CMD_CAMERA_SERVER_TO_CLIENT.getBytes();
                    os.write(sendBytes, 0, sendBytes.length);
                    os.flush();

                    receivedBytes = new byte[WIDTH*HEIGHT*3];
                    receivedInts = new int[WIDTH*HEIGHT*3];

                    /* receive RGB unsigned data from server */
                    while (mActivity != null)
                    {
                        bytesRead = 0;

                        is = sock.getInputStream();

                        /**
                         * There is no "unsigned" type in JAVA.
                         * If you want to deal with unsigned data type,
                         * you should be careful about signed expansion.
                         * To prevent signed expansion, and(&) operation will be useful.
                         */
                        while (bytesRead < WIDTH*HEIGHT*3) {
                            bytesRead += is.read(receivedBytes, bytesRead, WIDTH*HEIGHT*3-bytesRead);
                        }
                        for (int i = 0; i < WIDTH*HEIGHT*3; i++) {
                            /* prevent signed expansion */
                            receivedInts[i] = receivedBytes[i] & 0xff;
                        }

                        /* make a RGB_565 type of bitmap from received RGB data */
                        mBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.RGB_565);

                        j = 0;
                        for (int y = 0; y < HEIGHT; y++) {
                            for (int x = 0; x < WIDTH; x++) {
                                mBitmap.setPixel(x, y, Color.rgb(receivedInts[3*j],
                                        receivedInts[3*j+1],
                                        receivedInts[3*j+2]));
                                j++;
                            }
                        }

                        /* update UI */
                        try {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    cameraView.setImageBitmap(mBitmap);
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
                    try {
                        sock.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mCareraThread.start();
    }
}
