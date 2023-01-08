package com.mds.smartcontroller.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.mds.smartcontroller.R;
import com.mds.smartcontroller.databinding.FragmentHomeBinding;
import com.mds.smartcontroller.utils.NetworkUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class HomeFragment extends Fragment {

    private int mCurrentMode;

    /* mode AUTO/MANUAL */
    private final int MODE_AUTO   = 0;
    private final int MODE_MANUAL = 1;

    /* functional device state */
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
    private final int STATE_DRYER_ON = 11;
    private final int STATE_DRYER_OFF = 12;
    private final int STATE_FAN_VERY_FAST = 13;

    /* current device state */
    private volatile int mStateLED;
    private volatile int mStateFan;
    private volatile int mStateDrain;
    private volatile int mStateSolenoid;
    private volatile int mStateHumidifier;
    private volatile int mStateDryer;

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        setUpListener();
        asyncGetMode();
        asyncGetStates();
        asyncGetBoundary();
        return binding.getRoot();
    }

    private void setUpListener()
    {
        /* when user clicks button, mode is toggled */
        binding.btnMode.setOnClickListener(__ ->
                asyncToggleMode());

        binding.cvWater.setOnClickListener(__ -> {
            if (mStateSolenoid == STATE_SOLENOID_OPEN) {
                asyncChangeState(STATE_SOLENOID_CLOSE);
            } else if (mStateSolenoid == STATE_SOLENOID_CLOSE) {
                asyncChangeState(STATE_SOLENOID_OPEN);
            }
        });

        binding.cvDrain.setOnClickListener(__ -> {
            if (mStateDrain == STATE_DRAIN_OPEN) {
                asyncChangeState(STATE_DRAIN_CLOSE);
            } else if (mStateDrain == STATE_DRAIN_CLOSE) {
                asyncChangeState(STATE_DRAIN_OPEN);
            }
        });

        binding.cvLed.setOnClickListener(__ -> {
            if (mStateLED == STATE_LED_ON) {
                asyncChangeState(STATE_LED_OFF);
            } else if (mStateLED == STATE_LED_OFF) {
                asyncChangeState(STATE_LED_ON);
            }
        });

        binding.cvFan.setOnClickListener(__ -> {
            if (mStateFan == STATE_FAN_ON || mStateFan == STATE_FAN_VERY_FAST) {
                asyncChangeState(STATE_FAN_OFF);
            } else if (mStateFan == STATE_FAN_OFF) {
                asyncChangeState(STATE_FAN_ON);
            }
        });

        binding.cvHumidifier.setOnClickListener(__ -> {
            if (mStateHumidifier == STATE_HUMIDIFIER_ON) {
                asyncChangeState(STATE_HUMIDIFIER_OFF);
            }
            else if (mStateHumidifier == STATE_HUMIDIFIER_OFF) {
                asyncChangeState(STATE_HUMIDIFIER_ON);
            }
        });

        binding.cvDryer.setOnClickListener(__ -> {
            if (mStateDryer == STATE_DRYER_ON) {
                asyncChangeState(STATE_DRYER_OFF);
            }
            else if (mStateDryer == STATE_DRYER_OFF) {
                asyncChangeState(STATE_DRYER_ON);
            }
        });

        binding.btnBoundaryUpdate.setOnClickListener(__ ->
                asyncUpdateBoundaryData());
    }

    private void asyncUpdateBoundaryData()
    {
        Thread threadUpdateBoundary = new Thread(() -> {
            OutputStream os;
            byte[] sendBytes;
            String sendString;

            Socket sock = new Socket();

            try {
                /* open socket and connect to server */
                sock.connect(new InetSocketAddress(NetworkUtil.NETWORK_SERVER_IP,
                                NetworkUtil.NETWORK_SERVER_PORT),
                        1000);

                /* send command */
                os = sock.getOutputStream();
                sendBytes = NetworkUtil.NETWORK_CMD_BOUNDARY_CLIENT_TO_SERVER.getBytes();
                os.write(sendBytes, 0, sendBytes.length);
                os.flush();

                Thread.sleep(500);

                /* send boundary data to update */
                sendString = binding.etBoundaryHumidifier.getText()+"\n"+
                        binding.etBoundaryFan.getText()+"\n"+
                        binding.etBoundaryDryer.getText()+"\n"+
                        binding.etBoundaryCooler.getText()+"\n"+
                        binding.etBoundaryLed.getText();
                sendBytes = sendString.getBytes();
                os.write(sendBytes, 0, sendBytes.length);
                os.flush();

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                /* make sure to close socket */
                try {
                    sock.close();
                    asyncGetBoundary();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        threadUpdateBoundary.start();
    }

    private void asyncChangeState(int cmd)
    {
        Thread threadStateChange = new Thread(() -> {
            OutputStream os;
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

            } catch (IOException | InterruptedException e) {
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

        threadStateChange.start();
    }

    private void asyncGetStates() {

        /* receive data from the specified sensors */
        Thread mStateThread = new Thread(() -> {
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
                while (getActivity() != null) {
                    try {
                        int StateLED = Integer.parseInt(br.readLine());
                        int StateFan = Integer.parseInt(br.readLine());
                        int StateSolenoid = Integer.parseInt(br.readLine());
                        int StateDrain = Integer.parseInt(br.readLine());
                        int StateHumidifier = Integer.parseInt(br.readLine());
                        int StateDryer = Integer.parseInt(br.readLine());

                        /* update UI */
                        getActivity().runOnUiThread(() -> {
                            if (StateLED == STATE_LED_ON) {
                                binding.cvLed.setCardBackgroundColor(
                                        ContextCompat.getColor(getActivity(),
                                        R.color.card_background_color_active));
                                binding.ivLed.setImageResource(R.drawable.active_bolb);
                                mStateLED = STATE_LED_ON;
                            } else if (StateLED == STATE_LED_OFF) {
                                binding.cvLed.setCardBackgroundColor(
                                        ContextCompat.getColor(getActivity(),
                                        R.color.card_background_color_deactive));
                                binding.ivLed.setImageResource(R.drawable.bolb);
                                mStateLED = STATE_LED_OFF;
                            }

                            if (StateFan == STATE_FAN_ON) {
                                binding.cvFan.setCardBackgroundColor(
                                        ContextCompat.getColor(getActivity(),
                                        R.color.card_background_color_active));
                                binding.ivFan.setImageResource(R.drawable.active_fan);
                                mStateFan = STATE_FAN_ON;
                            } else if (StateFan == STATE_FAN_VERY_FAST) {
                                binding.cvFan.setCardBackgroundColor(ContextCompat.getColor(getActivity(),
                                        R.color.card_background_color_active));
                                binding.ivFan.setImageResource(R.drawable.active_fan);
                                mStateFan = STATE_FAN_VERY_FAST;
                            }
                            else if (StateFan == STATE_FAN_OFF) {
                                binding.cvFan.setCardBackgroundColor(
                                        ContextCompat.getColor(getActivity(),
                                        R.color.card_background_color_deactive));
                                binding.ivFan.setImageResource(R.drawable.fan);
                                mStateFan = STATE_FAN_OFF;
                            }

                            if (StateSolenoid == STATE_SOLENOID_OPEN) {
                                binding.cvWater.setCardBackgroundColor(
                                        ContextCompat.getColor(getActivity(),
                                        R.color.card_background_color_active));
                                binding.ivWater.setImageResource(R.drawable.active_droplet);
                                mStateSolenoid = STATE_SOLENOID_OPEN;
                            } else if (StateSolenoid == STATE_SOLENOID_CLOSE) {
                                binding.cvWater.setCardBackgroundColor(
                                        ContextCompat.getColor(getActivity(),
                                        R.color.card_background_color_deactive));
                                binding.ivWater.setImageResource(R.drawable.droplet);
                                mStateSolenoid = STATE_SOLENOID_CLOSE;
                            }

                            if (StateDrain == STATE_DRAIN_OPEN) {
                                binding.cvDrain.setCardBackgroundColor(
                                        ContextCompat.getColor(getActivity(),
                                        R.color.card_background_color_active));
                                binding.ivDrain.setImageResource(R.drawable.active_drainage);
                                mStateDrain = STATE_DRAIN_OPEN;
                            } else if (StateDrain == STATE_DRAIN_CLOSE) {
                                binding.cvDrain.setCardBackgroundColor(
                                        ContextCompat.getColor(getActivity(),
                                        R.color.card_background_color_deactive));
                                binding.ivDrain.setImageResource(R.drawable.drainage);
                                mStateDrain = STATE_DRAIN_CLOSE;
                            }

                            if (StateHumidifier == STATE_HUMIDIFIER_ON) {
                                binding.cvHumidifier.setCardBackgroundColor(
                                        ContextCompat.getColor(getActivity(),
                                        R.color.card_background_color_active));
                                binding.ivHumidifier.setImageResource(R.drawable.active_humidifier);
                                mStateHumidifier = STATE_HUMIDIFIER_ON;
                            } else if (StateHumidifier == STATE_HUMIDIFIER_OFF) {
                                binding.cvHumidifier.setCardBackgroundColor(
                                        ContextCompat.getColor(getActivity(),
                                        R.color.card_background_color_deactive));
                                binding.ivHumidifier.setImageResource(R.drawable.humidifier);
                                mStateHumidifier = STATE_HUMIDIFIER_OFF;
                            }

                            if (StateDryer == STATE_DRYER_ON) {
                                binding.cvDryer.setCardBackgroundColor(
                                        ContextCompat.getColor(getActivity(),
                                        R.color.card_background_color_active));
                                binding.ivDryer.setImageResource(R.drawable.active_dryer);
                                mStateDryer = STATE_DRYER_ON;
                            } else if (StateDryer == STATE_DRYER_OFF) {
                                binding.cvDryer.setCardBackgroundColor(
                                        ContextCompat.getColor(getActivity(),
                                        R.color.card_background_color_deactive));
                                binding.ivDryer.setImageResource(R.drawable.dryer);
                                mStateDryer = STATE_DRYER_OFF;
                            }
                        });
                    } catch (NullPointerException | NumberFormatException e) {
                        break;
                    }
                }
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

        mStateThread.start();
    }

    private void asyncToggleMode()
    {
        Thread mThreadModeToggle = new Thread(() -> {
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
                while (getActivity() != null) {
                    try {
                        int mode = Integer.parseInt(br.readLine());

                        /* update mCurrentMode */
                        mCurrentMode = mode;

                    /* update UI */
                        getActivity().runOnUiThread(() -> {
                            if (mode == MODE_AUTO)
                            {
                                binding.tvMode.setText("Current Mode is AUTO");
                            }
                            else if (mode == MODE_MANUAL)
                            {
                                binding.tvMode.setText("Current Mode is MANUAL");
                            }
                        });
                    } catch (NullPointerException | NumberFormatException e) {
                        break;
                    }
                }
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

        mThreadModeToggle.start();
    }

    private void asyncGetMode()
    {
        /* receive mode data from server */
        Thread mThreadMode = new Thread(() -> {
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
                while (getActivity() != null) {
                    try {
                        int mode = Integer.parseInt(br.readLine());

                        /* update UI */
                        getActivity().runOnUiThread(() -> {
                            if (mode == MODE_AUTO) {
                                binding.tvMode.setText("Current Mode is AUTO");
                            } else if (mode == MODE_MANUAL) {
                                binding.tvMode.setText("Current Mode is MANUAL");
                            }
                        });
                    } catch (NullPointerException | NumberFormatException e) {
                        break;
                    }
                }
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

        mThreadMode.start();
    }

    private void asyncGetBoundary()
    {
        Thread mThreadBoundary = new Thread(() -> {
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
                sendBytes = NetworkUtil.NETWORK_CMD_BOUNDARY_SERVER_TO_CLIENT.getBytes();
                os.write(sendBytes, 0, sendBytes.length);
                os.flush();

                /* receive boundary data */
                is = sock.getInputStream();
                br = new BufferedReader(new InputStreamReader(is));
                String boundary_humidifier = br.readLine();
                String boundary_fan = br.readLine();
                String boundary_dryer = br.readLine();
                String boundary_cooler = br.readLine();
                String boundary_led = br.readLine();

                /* update UI */
                requireActivity().runOnUiThread(() -> {
                    binding.etBoundaryHumidifier.setText(boundary_humidifier);
                    binding.etBoundaryFan.setText(boundary_fan);
                    binding.etBoundaryDryer.setText(boundary_dryer);
                    binding.etBoundaryCooler.setText(boundary_cooler);
                    binding.etBoundaryLed.setText(boundary_led);
                });
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

        mThreadBoundary.start();
    }
}