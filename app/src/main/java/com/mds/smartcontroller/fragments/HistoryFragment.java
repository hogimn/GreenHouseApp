package com.mds.smartcontroller.fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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

    /* GraphViews of each graph */
    private GraphView mTempGraph;
    private GraphView mHumiGraph;

    /* initial x label of each graph */
    private int mTempLastX = 0;
    private int mHumiLastX = 0;

    /* thread to get temperature / humidity sensor data from server */
    private Thread mThreadTempHumi;

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
        getSensorData();
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
        mThreadTempHumi.interrupt();
    }

    private void getSensorData() {

        /* receive data from temperature / humidity sensors */
        mThreadTempHumi = new Thread(new Runnable() {
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
                    sendBytes = NetworkUtil.SOCK_CMD_SENSOR_SERVER_TO_CLIENT.getBytes();
                    os.write(sendBytes, 0, sendBytes.length);
                    os.flush();

                    /* receive temparature (2byte) / humid sensor (2byte) data */
                    is = sock.getInputStream();
                    br = new BufferedReader(new InputStreamReader(is));
                    while (mActivity != null) {
                        /* first receive humidity data */
                        String strHumi = br.readLine();

                        /* update UI */
                        try {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mHumiSeries.appendData(new DataPoint(mHumiLastX++,
                                                    Integer.parseInt(strHumi)),
                                            true,
                                            20);
                                    mHumiGraph.onDataChanged(true,
                                            true);
                                }
                            });
                        } catch (NullPointerException e) {
                            break;
                        }

                        /* receive temperature data next */
                        String strTemp = br.readLine();
                        Log.d("run", "temp: " + strTemp);

                        /* update UI */
                        try {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTempSeries.appendData(new DataPoint(mTempLastX++,
                                                    Integer.parseInt(strTemp)),
                                            true,
                                            20);
                                    mTempGraph.onDataChanged(true, true);
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

        mThreadTempHumi.start();
    }

    private void initializeView(View v) {

        GridLabelRenderer tempGridLabelRenderer;
        GridLabelRenderer humiGridLabelRenderer;

        mTempGraph = new GraphView(mActivity);
        mHumiGraph = new GraphView(mActivity);

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

        mTempSeries = new LineGraphSeries<DataPoint>();
        mTempGraph.addSeries(mTempSeries);
        mHumiSeries = new LineGraphSeries<DataPoint>();
        mHumiGraph.addSeries(mHumiSeries);

        mTempSeries.setColor(Color.rgb(56, 163, 226));
        mHumiSeries.setColor(Color.rgb(56, 163, 226));

        LinearLayout layout_temp = v.findViewById(R.id.temp_graph);
        layout_temp.addView(mTempGraph);

        LinearLayout layout_humi = v.findViewById(R.id.humi_graph);
        layout_humi.addView(mHumiGraph);
    }
}
