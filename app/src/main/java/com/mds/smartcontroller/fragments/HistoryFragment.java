package com.mds.smartcontroller.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.mds.smartcontroller.R;
import com.mds.smartcontroller.activities.HistoryActivity;
import com.mds.smartcontroller.utils.NetworkUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class HistoryFragment extends Fragment {
    /* LineGraphSeries to record each sensor data */
    private LineGraphSeries<DataPoint> mTempSeries;
    private LineGraphSeries<DataPoint> mHumiSeries;
    private LineGraphSeries<DataPoint> mPhotoSeries;
    private LineGraphSeries<DataPoint> mMagnetSeries;
    private LineGraphSeries<DataPoint> mMoistureSeries;

    /* GraphViews of each graph */
    private GraphView mTempGraph;
    private GraphView mHumiGraph;
    private GraphView mPhotoGraph;
    private GraphView mMagnetGraph;
    private GraphView mMoistureGraph;

    /* initial x label of each graph */
    private int mTempLastX = 0;
    private int mHumiLastX = 0;
    private int mPhotoLastX = 0;
    private int mMagnetLastX = 0;
    private int mMoistureLastX = 0;

    /* thread to get sensor data from server */
    private Thread mThreadSensor;

    /* current Activity where the fragment belongs to */
    private Activity mActivity;

    private final int WHITE = Color.rgb(255,255,255);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_history, container, false);
        initializeView(v);
        asyncGetSensorData();
        return v;
    }

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

    private void asyncGetSensorData() {

        /* receive sensor data from server */
        mThreadSensor = new Thread(() -> {
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
                sendBytes = NetworkUtil.NETWORK_CMD_SENSOR_SERVER_TO_CLIENT.getBytes();
                os.write(sendBytes, 0, sendBytes.length);
                os.flush();

                /* receive temparature (2byte) / humid sensor (2byte) data */
                is = sock.getInputStream();
                br = new BufferedReader(new InputStreamReader(is));
                while (mActivity != null) {

                    try {
                    /* receive sensor data */
                    int humi = Integer.parseInt(br.readLine());
                    int temp = Integer.parseInt(br.readLine());
                    int photo = Integer.parseInt(br.readLine());
                    int magnet = Integer.parseInt(br.readLine());
                    int moisture = Integer.parseInt(br.readLine());

                    /* update UI */
                        mActivity.runOnUiThread(() -> {
                            mHumiSeries.appendData(new DataPoint(mHumiLastX++,
                                            humi),
                                    true,
                                    20);
                            mTempSeries.appendData(new DataPoint(mTempLastX++,
                                            temp),
                                    true,
                                    20);
                            mPhotoSeries.appendData(new DataPoint(mPhotoLastX++,
                                            photo),
                                    true,
                                    20);
                            mMagnetSeries.appendData(new DataPoint(mMagnetLastX++,
                                            magnet),
                                    true,
                                    20);
                            mMoistureSeries.appendData(new DataPoint(mMoistureLastX++,
                                            moisture),
                                    true,
                                    20);

                            mHumiGraph.onDataChanged(true,
                                    true);
                            mTempGraph.onDataChanged(true,
                                    true);
                            mPhotoGraph.onDataChanged(true,
                                    true);
                            mMagnetGraph.onDataChanged(true,
                                    true);
                            mMoistureGraph.onDataChanged(true,
                                    true);
                        });
                    } catch (NullPointerException e) {
                        break;
                    } catch (NumberFormatException e) {
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
        });

        mThreadSensor.start();
    }

    private void initializeView(View v) {

        GridLabelRenderer tempGridLabelRenderer;
        GridLabelRenderer humiGridLabelRenderer;
        GridLabelRenderer photoGridLabelRenderer;
        GridLabelRenderer magnetGridLabelRenderer;
        GridLabelRenderer moistureGridLabelRenderer;

        mTempGraph = new GraphView(mActivity);
        mHumiGraph = new GraphView(mActivity);
        mPhotoGraph = new GraphView(mActivity);
        mMagnetGraph = new GraphView(mActivity);
        mMoistureGraph = new GraphView(mActivity);

        mTempGraph.setTitle("Temparature");
        mTempGraph.setHorizontalScrollBarEnabled(true);
        mTempGraph.setTitleColor(WHITE);
        tempGridLabelRenderer = mTempGraph.getGridLabelRenderer();
        tempGridLabelRenderer.setHorizontalLabelsVisible(false);
        tempGridLabelRenderer.setGridColor(WHITE);
        tempGridLabelRenderer.setVerticalLabelsColor(WHITE);
        tempGridLabelRenderer.setHorizontalLabelsColor(WHITE);

        mHumiGraph.setTitle("Humidity");
        mHumiGraph.setHorizontalScrollBarEnabled(true);
        mHumiGraph.setTitleColor(WHITE);
        humiGridLabelRenderer = mHumiGraph.getGridLabelRenderer();
        humiGridLabelRenderer.setHorizontalLabelsVisible(false);
        humiGridLabelRenderer.setGridColor(WHITE);
        humiGridLabelRenderer.setVerticalLabelsColor(WHITE);
        humiGridLabelRenderer.setHorizontalLabelsColor(WHITE);

        mPhotoGraph.setTitle("Photo Intensity");
        mPhotoGraph.setHorizontalScrollBarEnabled(true);
        mPhotoGraph.setTitleColor(WHITE);
        photoGridLabelRenderer = mPhotoGraph.getGridLabelRenderer();
        photoGridLabelRenderer.setHorizontalLabelsVisible(false);
        photoGridLabelRenderer.setGridColor(WHITE);
        photoGridLabelRenderer.setVerticalLabelsColor(WHITE);
        photoGridLabelRenderer.setHorizontalLabelsColor(WHITE);

        mMagnetGraph.setTitle("Magnet Detection");
        mMagnetGraph.setHorizontalScrollBarEnabled(true);
        mMagnetGraph.setTitleColor(WHITE);
        magnetGridLabelRenderer = mMagnetGraph.getGridLabelRenderer();
        magnetGridLabelRenderer.setHorizontalLabelsVisible(false);
        magnetGridLabelRenderer.setGridColor(WHITE);
        magnetGridLabelRenderer.setVerticalLabelsColor(WHITE);
        magnetGridLabelRenderer.setHorizontalLabelsColor(WHITE);

        mMoistureGraph.setTitle("Moisture Detection");
        mMoistureGraph.setHorizontalScrollBarEnabled(true);
        mMoistureGraph.setTitleColor(WHITE);
        moistureGridLabelRenderer = mMoistureGraph.getGridLabelRenderer();
        moistureGridLabelRenderer.setHorizontalLabelsVisible(false);
        moistureGridLabelRenderer.setGridColor(WHITE);
        moistureGridLabelRenderer.setVerticalLabelsColor(WHITE);
        moistureGridLabelRenderer.setHorizontalLabelsColor(WHITE);

        /* temperature ranges 0 ~ 50 celsius degree */
        Viewport vp_temp = mTempGraph.getViewport();
        vp_temp.setYAxisBoundsManual(true);
        vp_temp.setMinY(0);
        vp_temp.setMaxY(50);

        /* humidity ranges 0 ~ 100 percentage */
        Viewport vp_humi = mHumiGraph.getViewport();
        vp_humi.setYAxisBoundsManual(true);
        vp_humi.setMinY(0);
        vp_humi.setMaxY(100);

        /* photo intensity ranges 0 ~ 2000 percentage */
        Viewport vp_photo = mPhotoGraph.getViewport();
        vp_photo.setYAxisBoundsManual(true);
        vp_photo.setMinY(0);
        vp_photo.setMaxY(2000);

        /* magnet ranges 0 ~ 1 percentage */
        Viewport vp_magnet = mMagnetGraph.getViewport();
        vp_magnet.setYAxisBoundsManual(true);
        vp_magnet.setMinY(0);
        vp_magnet.setMaxY(1);

        /* moisture ranges 0 ~ 100 percentage */
        Viewport vp_moisture = mMoistureGraph.getViewport();
        vp_moisture.setYAxisBoundsManual(true);
        vp_moisture.setMinY(0);
        vp_moisture.setMaxY(1);

        mTempSeries = new LineGraphSeries<>();
        mTempGraph.addSeries(mTempSeries);
        mHumiSeries = new LineGraphSeries<>();
        mHumiGraph.addSeries(mHumiSeries);
        mPhotoSeries = new LineGraphSeries<>();
        mPhotoGraph.addSeries(mPhotoSeries);
        mMagnetSeries = new LineGraphSeries<>();
        mMagnetGraph.addSeries(mMagnetSeries);
        mMoistureSeries = new LineGraphSeries<>();
        mMoistureGraph.addSeries(mMoistureSeries);

        mTempSeries.setColor(Color.rgb(0xe9, 0x1e, 0x63));
        mHumiSeries.setColor(Color.rgb(0xe9, 0x1e, 0x63));
        mPhotoSeries.setColor(Color.rgb(0xe9, 0x1e, 0x63));
        mMagnetSeries.setColor(Color.rgb(0xe9, 0x1e, 0x63));
        mMoistureSeries.setColor(Color.rgb(0xe9, 0x1e, 0x63));

        LinearLayout layout_temp = v.findViewById(R.id.temp_graph);
        layout_temp.addView(mTempGraph);

        LinearLayout layout_humi = v.findViewById(R.id.humi_graph);
        layout_humi.addView(mHumiGraph);

        LinearLayout layout_photo = v.findViewById(R.id.photo_graph);
        layout_photo.addView(mPhotoGraph);

        LinearLayout layout_magnet = v.findViewById(R.id.magnet_graph);
        layout_magnet.addView(mMagnetGraph);

        LinearLayout layout_moisture = v.findViewById(R.id.moisture_graph);
        layout_moisture.addView(mMoistureGraph);

        mTempGraph.setOnClickListener(__ -> {
            Intent intent = new Intent(getContext(), HistoryActivity.class);
            intent.putExtra("sensor", "temp");
            startActivity(intent);
        });
        mHumiGraph.setOnClickListener(__ -> {
            Intent intent = new Intent(getContext(), HistoryActivity.class);
            intent.putExtra("sensor", "humi");
            startActivity(intent);
        });
        mPhotoGraph.setOnClickListener(__ -> {
            Intent intent = new Intent(getContext(), HistoryActivity.class);
            intent.putExtra("sensor", "photo");
            startActivity(intent);
        });
        mMagnetGraph.setOnClickListener(__ -> {
            Intent intent = new Intent(getContext(), HistoryActivity.class);
            intent.putExtra("sensor", "magnet");
            startActivity(intent);
        });
        mMoistureGraph.setOnClickListener(__ -> {
            Intent intent = new Intent(getContext(), HistoryActivity.class);
            intent.putExtra("sensor", "moisture");
            startActivity(intent);
        });
    }
}
