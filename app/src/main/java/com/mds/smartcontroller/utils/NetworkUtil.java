package com.mds.smartcontroller.utils;

public class NetworkUtil {
    public static final String NETWORK_CMD_FILE_CLIENT_TO_SERVER = "0";
    public static final String NETWORK_CMD_LIST_SERVER_TO_CLIENT = "1";
    public static final String NETWORK_CMD_DELETE_CLIENT_TO_SERVER = "2";
    public static final String NETWORK_CMD_PLAY_CLIENT_TO_SERVER = "3";
    public static final String NETWORK_CMD_SENSOR_SERVER_TO_CLIENT = "4";
    public static final String NETWORK_CMD_CAMERA_SERVER_TO_CLIENT = "5";
    public static final String NETWORK_CMD_DATABASE_SERVER_TO_CLIENT = "6";
    public static final String NETWORK_CMD_MODE_SERVER_TO_CLIENT = "7";
    public static final String NETWORK_CMD_MODE_TOGGLE_CLIENT_TO_SERVER = "8";
    public static final String NETWORK_CMD_STATE_SERVER_TO_CLIENT = "9";
    public static final String NETWORK_CMD_STATE_CHANGE_CLIENT_TO_SERVER = "10";
    public static final String NETWORK_CMD_END = "-1";

    public static final int NETWORK_SERVER_PORT = 3000;
    public static final String NETWORK_SERVER_IP = "192.168.0.210";
}
