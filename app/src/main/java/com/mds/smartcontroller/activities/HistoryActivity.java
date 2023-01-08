package com.mds.smartcontroller.activities;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
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

    /* graph to show accumulated sensor data */
    private GraphView mSensorGraph;

    /* data series to be added by server */
    private LineGraphSeries<DataPoint> mSensorSeries;

    /* get added by 1 whenever new DataPoint is appended to mSensorSeries */
    private int mLastX = 0;

    /* sensor name */
    private String mSensor;

    /* thread to receive database data from server */
    private Thread mGraphThread;

    /* current Activity */
    private Activity mActivity;

    /* maximum DataPoints to show in a screen */
    private final int mMaxDataPoints = 100000;

    private final int WHITE = Color.rgb(255,255,255);

    private final int DATABASE_HUMI = 1;
    private final int DATABASE_TEMP = 2;
    private final int DATABASE_PHOTO = 3;
    private final int DATABASE_MAGNET = 4;
    private final int DATABASE_MOISTURE = 5;

    private final String TAG = this.getClass().getName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        /* receive interActivity data */
        mSensor = getIntent().getStringExtra("sensor");

        /* save current Activity */
        mActivity = HistoryActivity.this;

        initializeView();

        asyncGetSensorData();
    }

    private void asyncGetSensorData()
    {
        mGraphThread = new Thread(() -> {
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
                sendBytes = NetworkUtil.NETWORK_CMD_DATABASE_SERVER_TO_CLIENT.getBytes();
                os.write(sendBytes, 0, sendBytes.length);
                os.flush();

                Thread.sleep(500);

                /* send second command */
                switch (mSensor) {
                    case "humi":
                        sendBytes = String.valueOf(DATABASE_HUMI).getBytes();
                        break;
                    case "temp":
                        sendBytes = String.valueOf(DATABASE_TEMP).getBytes();
                        break;
                    case "photo":
                        sendBytes = String.valueOf(DATABASE_PHOTO).getBytes();
                        break;
                    case "magnet":
                        sendBytes = String.valueOf(DATABASE_MAGNET).getBytes();
                        break;
                    case "moisture":
                        sendBytes = String.valueOf(DATABASE_MOISTURE).getBytes();
                        break;
                    default:
                        throw new NullPointerException();
                }

                os.write(sendBytes, 0, sendBytes.length);
                os.flush();

                /* receive humid sensor (2 bytes) data */
                is = sock.getInputStream();
                br = new BufferedReader(new InputStreamReader(is));
                while (mActivity != null) {
                    /* update UI */
                    try {
                        /* receive sensor data */
                        String sensorData = br.readLine();

                        mActivity.runOnUiThread(() -> {
                            /* append to mSensorSeries */
                            mSensorSeries.appendData(new DataPoint(mLastX++,
                                            Integer.parseInt(sensorData)),
                                    true,
                                    mMaxDataPoints);
                            mSensorGraph.onDataChanged(true,
                                    true);
                        });
                    } catch (NullPointerException e) {
                        break;
                    }
                }
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
        });

        mGraphThread.start();
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
        vp_sensor.setXAxisBoundsManual(true);

        /* scrollable in x-axis */
        vp_sensor.setScrollable(true);
        /* zoom in/out */
        vp_sensor.setScalable(true);

        /* show only 1000 DataPoints at a time */
        vp_sensor.setMinX(0);
        vp_sensor.setMaxX(1000);

        /* set Y range according to selected sensor */
        switch (mSensor) {
            case "temp":
                vp_sensor.setMinY(0);
                vp_sensor.setMaxY(50);
                break;
            case "humi":
                vp_sensor.setMinY(0);
                vp_sensor.setMaxY(100);
                break;
            case "photo":
                vp_sensor.setMinY(0);
                vp_sensor.setMaxY(2000);
                break;
            case "magnet":
            case "moisture":
                vp_sensor.setMinY(0);
                vp_sensor.setMaxY(1);
                break;
        }

        mSensorSeries = new LineGraphSeries<>();
        mSensorGraph.addSeries(mSensorSeries);

        /* set color of graph */
        mSensorSeries.setColor(Color.rgb(0xe9, 0x1e, 0x63));

        LinearLayout layout_sensor = findViewById(R.id.sensor_graph);
        layout_sensor.addView(mSensorGraph);
    }
}
