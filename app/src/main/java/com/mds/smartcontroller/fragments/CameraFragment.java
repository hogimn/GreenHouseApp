package com.mds.smartcontroller.fragments;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mds.smartcontroller.databinding.FragmentCameraBinding;
import com.mds.smartcontroller.utils.NetworkUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class CameraFragment extends Fragment {

    /* Logitech C270 Specific */
    private final int HEIGHT = 144;
    private final int WIDTH = 176;

    private FragmentCameraBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentCameraBinding.inflate(inflater, container, false);
        asyncGetCameraDataAndUpdateUI();
        return binding.getRoot();
    }

    private void asyncGetCameraDataAndUpdateUI() {

        /* connect to server */
        /* send command */
        /* receive RGB unsigned data from server */
        /**
         * There is no "unsigned" type in JAVA.
         * If you want to deal with unsigned data type,
         * you should be careful about signed expansion.
         * To prevent signed expansion, and(&) operation will be useful.
         */
        /* prevent signed expansion */
        /* make a RGB_565 type of bitmap from received RGB data */
        /* update UI */
        /* thread to get camera data from server */
        Thread mCareraThread = new Thread(() -> {
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
                sock.connect(new InetSocketAddress(NetworkUtil.NETWORK_SERVER_IP,
                                NetworkUtil.NETWORK_SERVER_PORT),
                        1000);

                /* send command */
                os = sock.getOutputStream();
                sendBytes = NetworkUtil.NETWORK_CMD_CAMERA_SERVER_TO_CLIENT.getBytes();
                os.write(sendBytes, 0, sendBytes.length);
                os.flush();

                receivedBytes = new byte[WIDTH * HEIGHT * 3];
                receivedInts = new int[WIDTH * HEIGHT * 3];

                /* receive RGB unsigned data from server */
                while (getActivity() != null) {
                    bytesRead = 0;

                    is = sock.getInputStream();

                    /**
                     * There is no "unsigned" type in JAVA.
                     * If you want to deal with unsigned data type,
                     * you should be careful about signed expansion.
                     * To prevent signed expansion, and(&) operation will be useful.
                     */
                    while (bytesRead < WIDTH * HEIGHT * 3) {
                        bytesRead += is.read(receivedBytes, bytesRead, WIDTH * HEIGHT * 3 - bytesRead);
                    }
                    for (int i = 0; i < WIDTH * HEIGHT * 3; i++) {
                        /* prevent signed expansion */
                        receivedInts[i] = receivedBytes[i] & 0xff;
                    }

                    /* RGB data -> Bitmap (RGB_565) -> ImageView */
                    /* make a RGB_565 type of bitmap from received RGB data */
                    Bitmap mBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.RGB_565);

                    j = 0;
                    for (int y = 0; y < HEIGHT; y++) {
                        for (int x = 0; x < WIDTH; x++) {
                            mBitmap.setPixel(x, y, Color.rgb(receivedInts[3 * j],
                                    receivedInts[3 * j + 1],
                                    receivedInts[3 * j + 2]));
                            j++;
                        }
                    }

                    /* update UI */
                    try {
                        getActivity().runOnUiThread(() ->
                                binding.cameraView.setImageBitmap(mBitmap));
                    } catch (NullPointerException e) {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    sock.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        mCareraThread.start();
    }
}
