package com.mds.smartcontroller.activities;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

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

public class HistoryActivity extends Activity {

    private GraphView mSensorGraph;
    private LineGraphSeries<DataPoint> mSensorSeries;
    private int mLastX = 0;
    private String mSensor;
    private Thread mGraphThread;
    private Activity mActivity;
    private final int mMaxDataPoints = 100000;
    private final int WHITE = Color.rgb(255,255,255);
    private final String TAG = this.getClass().getName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mSensor = getIntent().getStringExtra("sensor");
        mActivity = HistoryActivity.this;

        initializeView();

        getSensorData();
    }

    private void getSensorData()
    {
        mGraphThread = new Thread(new Runnable() {
            @Override
            public void run() {
                OutputStream os;
                InputStream is;
                BufferedReader br;
                byte[] sendBytes;

                Socket sock = new Socket();

                try {
                    /* connect to server */
                    sock.connect(new InetSocketAddress(NetworkUtil.SERVER_IP,
                                    NetworkUtil.SERVER_PORT),
                            1000);

                    /* send command */
                    os = sock.getOutputStream();
                    if (mSensor.equals("humi")) {
                        sendBytes = NetworkUtil.SOCK_CMD_HUMI_SERVER_TO_CLIENT.getBytes();
                    } else if (mSensor.equals("temp")) {
                        sendBytes = NetworkUtil.SOCK_CMD_TEMP_SERVER_TO_CLIENT.getBytes();
                    } else {
                        sendBytes = "fail".getBytes();
                    }

                    os.write(sendBytes, 0, sendBytes.length);
                    os.flush();

                    /* receive humid sensor (2byte) data */
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
                                    mSensorSeries.appendData(new DataPoint(mLastX++,
                                                    Integer.parseInt(strHumi)),
                                            true,
                                            mMaxDataPoints);
                                    mSensorGraph.onDataChanged(true,
                                            true);
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

        mGraphThread.start();
    }

    private void getHumiData()
    {

    }

    private void initializeView() {

        GridLabelRenderer sensorGridLabelRenderer;

        mSensorGraph = new GraphView(mActivity);
        mSensorGraph.setTitle(mSensor);
        mSensorGraph.setTitleColor(WHITE);

        sensorGridLabelRenderer = mSensorGraph.getGridLabelRenderer();
        sensorGridLabelRenderer.setHorizontalLabelsVisible(false);
        sensorGridLabelRenderer.setGridColor(WHITE);
        sensorGridLabelRenderer.setVerticalLabelsColor(WHITE);
        sensorGridLabelRenderer.setHorizontalLabelsColor(WHITE);

        Viewport vp_sensor = mSensorGraph.getViewport();
        vp_sensor.setYAxisBoundsManual(true);
        vp_sensor.setScrollable(true);
        if (mSensor.equals("temp")) {
            vp_sensor.setMinY(0);
            vp_sensor.setMaxY(50);
        } else if (mSensor.equals("humi")) {
            vp_sensor.setMinY(0);
            vp_sensor.setMaxY(100);
        }

        mSensorSeries = new LineGraphSeries<DataPoint>();
        mSensorGraph.addSeries(mSensorSeries);

        mSensorSeries.setColor(Color.rgb(56, 163, 226));

        LinearLayout layout_sensor = findViewById(R.id.sensor_graph);
        layout_sensor.addView(mSensorGraph);
    }
}
