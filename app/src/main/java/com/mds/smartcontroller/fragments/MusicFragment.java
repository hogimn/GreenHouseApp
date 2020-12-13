package com.mds.smartcontroller.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.ListFragment;

import com.mds.smartcontroller.R;
import com.mds.smartcontroller.utils.MusicItem;
import com.mds.smartcontroller.utils.MusicItemAdapter;
import com.mds.smartcontroller.utils.NetworkUtil;
import com.mds.smartcontroller.utils.RealPathUtil;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import static android.widget.AdapterView.OnClickListener;
import static android.widget.AdapterView.OnItemClickListener;
import static android.widget.AdapterView.OnItemLongClickListener;

public class MusicFragment extends ListFragment {

    /* button to select mp3 file to transfer */
    private Button mBtnChooseFile;

    /* button to start transferring mp3 file */
    private Button mBtnFileTransfer;

    /* textview to show real path to the selected file */
    private TextView mTVSelectedFilePath;

    /**
     *  request code for startActivityForResult
     *  which will start file explorer activity
     *  for user to select mp3 file to transfer
     */
    private final int CHOOSE_AUDIO_FILE = 1001;

    /**
     * file explorer activity will return
     * the uri of selected file path,
     * which will be converted to real path (mRealPath)
     * by getRealPath() method of RealPathUtil class
     */
    private Uri mUriSelectedFilePath = null;

    /* uri path will be converted to real path */
    private String mRealPath = null;

    /* async task to get music list from server */
    private AsyncTask<Void, Void, Void> mAsyncTask;

    /* array adapter for ListView of music list */
    private MusicItemAdapter mArrayAdaper;

    /* container for music list */
    private ArrayList<MusicItem> mMusicItemList;

    /* indicates whether music is currently playing or not */
    private int mStopOrPlay;

    /* currently playing music */
    private MusicItem currentMusic;

    /* Activity where the fragment belongs to */
    private Activity mActivity;

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
        View v = inflater.inflate(R.layout.fragment_music, container, false);

        initializeDynamicObject();

        initializeView(v);

        asyncGetMusicList();

        setListAdapter(mArrayAdaper);
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                /* extract selected musicItem in the ListView */
                MusicItem musicItem = (MusicItem) parent
                        .getAdapter()
                        .getItem(position);

                /* get selected music title */
                String fileName = musicItem.getTitle();

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        /* play selected music */
                        playMusic(fileName);

                        /* update "playing" state on the "@layout/musiclist_item.xml" */
                        updatePlayingState(musicItem);

                        /* update UI */
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mArrayAdaper.notifyDataSetChanged();
                            }
                        });
                    }
                }).start();
            }
        });

        /**
         * if user click a item in ListView long enough
         * delete the selected file from the server
         */
        getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent,
                                           View view,
                                           int position,
                                           long id) {

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("Delete file?");
                builder.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {

                            /* if OK clicked */
                            public void onClick(DialogInterface dialog, int which) {

                                /* extract selected musicItem */
                                MusicItem musicItem = (MusicItem) parent
                                        .getAdapter()
                                        .getItem(position);

                                /* get the title of the selected music Item */
                                String fileName = musicItem.getTitle();

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {

                                        /* delete file from the server */
                                        deleteFile(fileName);

                                        /* sleep 500ms to reopen socket successfully */
                                        try {
                                            Thread.sleep(500);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }

                                        /* get updated music list from the server */
                                        getMusicList();

                                        /* update UI */
                                        mActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mArrayAdaper.notifyDataSetChanged();
                                            }
                                        });
                                    }
                                }).start();
                            }
                        });

                builder.setNegativeButton("Cancle",

                        /* Cancle clicked */
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                /* nothing will happen */
                            }
                        });

                builder.show();

                return true;
            }
        });
    }

    /**
     * set TextView "@+id/tv_state" to "playing" or ""
     * in array "@layout/musiclist_item.xml"
     *
     * TODO: sometimes state mark and actual playing state on machine are different
     *
     * @param musicItem newly selected musicItem to play
     */
    private void updatePlayingState(MusicItem musicItem) {

        String fileName = musicItem.getTitle();

        if (currentMusic != null) {
            /* set TextView of current music to "" (stopped) */
            setPlayingState(currentMusic.getTitle(), false);

            /* if current music is not equal to newly selected Music */
            if (!fileName.equals(currentMusic.getTitle())) {
                /* set TextView of selected music to "playing" */
                setPlayingState(musicItem.getTitle(), true);
                /* update currentMusic to selected music */
                currentMusic = musicItem;
            }
            /* if current music is equal to newly selected Music */
            else {
                /* nothing to set TextView to "playing" */

                /* update currentMusic to null */
                currentMusic = null;
            }
        } else {
            /* set TextView of selected music to "playing" */
            setPlayingState(musicItem.getTitle(), true);
            /* update currentMusic to selected music */
            currentMusic = musicItem;
        }
    }

    private void initializeDynamicObject()
    {
        mMusicItemList = new ArrayList<>();
        mArrayAdaper = new MusicItemAdapter(getContext(),
                mMusicItemList,
                MusicFragment.this);
    }

    private void initializeView(View v)
    {
        mBtnChooseFile = v.findViewById(R.id.btn_filechooser);
        mBtnFileTransfer = v.findViewById(R.id.btn_filetransfer);
        mTVSelectedFilePath = v.findViewById(R.id.tv_selected_file);

        /* open file explorer where user can select only audio type file */
        mBtnChooseFile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

                /* filter only mp3 files */
                intent.setType("audio/*");

                /**
                 * start activity with request code CHOOSE_AUDIO_FILE
                 * onActivityResult hook method will be called
                 * when started activity exits
                 */
                startActivityForResult(intent, CHOOSE_AUDIO_FILE);
            }
        });

        /**
         * when user click "TRANSFER" button,
         * start to transfer file via socket
         */
        mBtnFileTransfer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        /* transfer file */
                        transferFile();

                        /* sleep 500ms to reopen socket successfully */
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        /* get updated music list from server */
                        getMusicList();

                        /* update UI */
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mArrayAdaper.notifyDataSetChanged();
                            }
                        });
                    }
                }).start();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode,
                                 int resultCode,
                                 @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_AUDIO_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                mUriSelectedFilePath = data.getData();
                mRealPath = RealPathUtil.getRealPath(getContext(), mUriSelectedFilePath);
                mTVSelectedFilePath.setText(String.format("File Path: %s", mRealPath));
            }
        }
    }

    private void asyncGetMusicList() {
        mAsyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                /* starts background thread to get a list of musics from client via socket */
                getMusicList();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                /* update UI */
                mArrayAdaper.notifyDataSetChanged();
            }
        };

        mAsyncTask.execute();
    }

    /**
     * play (or stop) the music in server
     * @param fileName music file name to play
     */
    private void playMusic(String fileName) {
        Socket sock;
        OutputStream os;
        byte[] sendBytes;

        sock = new Socket();

        try {
            /* connect to server */
            sock.connect(new InetSocketAddress(NetworkUtil.NETWORK_SERVER_IP,
                            NetworkUtil.NETWORK_SERVER_PORT),
                    1000);

            /* send command */
            os = sock.getOutputStream();
            sendBytes = NetworkUtil.NETWORK_CMD_PLAY_CLIENT_TO_SERVER.getBytes();
            os.write(sendBytes, 0, sendBytes.length);
            os.flush();

            Thread.sleep(500);

            /* send file name */
            sendBytes = fileName.getBytes();
            os.write(sendBytes, 0, sendBytes.length);
            os.flush();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * delete the music file from the server
     * @param fileName file name to delete
     */
    private void deleteFile(String fileName) {
        Socket sock;
        OutputStream os;
        byte[] sendBytes;

        sock = new Socket();

        try {
            /* connect to server */
            sock.connect(new InetSocketAddress(NetworkUtil.NETWORK_SERVER_IP,
                    NetworkUtil.NETWORK_SERVER_PORT),
                    1000);

            /* send command */
            os = sock.getOutputStream();
            sendBytes = NetworkUtil.NETWORK_CMD_DELETE_CLIENT_TO_SERVER.getBytes();
            os.write(sendBytes, 0, sendBytes.length);
            os.flush();

            Thread.sleep(500);

            /* send file name */
            sendBytes = fileName.getBytes();
            os.write(sendBytes, 0, sendBytes.length);
            os.flush();

            sock.close();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * get music list from the server
     */
    private void getMusicList() {
        Socket sock;
        OutputStream os;
        InputStream is;
        BufferedReader br;
        byte[] sendBytes;
        String receiveString;

        sock = new Socket();

        try {
            /* clear any element in ArrayList */
            mMusicItemList.clear();

            /* connect to server */
            sock.connect(new InetSocketAddress(NetworkUtil.NETWORK_SERVER_IP,
                    NetworkUtil.NETWORK_SERVER_PORT),
                    1000);

            /* send command */
            os = sock.getOutputStream();
            sendBytes = NetworkUtil.NETWORK_CMD_LIST_SERVER_TO_CLIENT.getBytes();
            os.write(sendBytes, 0, sendBytes.length);
            os.flush();

            Thread.sleep(500);

            /* receive music list */
            is = sock.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));

            while ((receiveString = br.readLine()) != null) {
                /* reached the end of music list */
                if (receiveString.equals(NetworkUtil.NETWORK_CMD_END)) {
                    break;
                }

                /* add them to arrayAdapter */
                mMusicItemList.add(new MusicItem(receiveString, false));
            }

            Thread.sleep(500);

            /* receive the title of the currently played music on the server */
            while ((receiveString = br.readLine()) != null) {

                if (receiveString.equals(NetworkUtil.NETWORK_CMD_END)) {
                    break;
                }

                /* set status of currently played music */
                setPlayingState(receiveString, true);

                /* update currentMusic */
                if (currentMusic == null) {
                    for (MusicItem musicItem : mMusicItemList) {
                        if (receiveString.equals(musicItem.getTitle())) {
                            currentMusic = musicItem;
                        }
                    }
                }
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * set playing state of the music to playing or stopped
     * @param title music title to set state
     * @param state true (playing) or false (stopped)
     */
    private void setPlayingState(String title, boolean state) {
        int index = getIndexFromTitle(title);
        if (index != -1) {
            mMusicItemList.get(index).setPlayOrStop(state);
        }
    }

    /**
     * get index of the selected music (title) from mMusicItemList
     * @param title which MusicItem to search
     * @return index of the found MusicItem
     */
    private int getIndexFromTitle(String title)
    {
        for(MusicItem musicItem : mMusicItemList)  {
            if(musicItem.getTitle().equals(title))
                return mMusicItemList.indexOf(musicItem);
        }

        return -1;
    }

    /**
     * transfer music file to server
     */
    private void transferFile() {
        Socket sock;
        OutputStream os;
        BufferedInputStream bis;
        byte[] sendBytes;
        String fileName;

        sock = new Socket();

        try {
            if (mUriSelectedFilePath == null) {
                return;
            }

            /* open socket and connect to server */

            sock.connect(new InetSocketAddress(NetworkUtil.NETWORK_SERVER_IP,
                    NetworkUtil.NETWORK_SERVER_PORT),
                    1000);

            /* send command */
            os = sock.getOutputStream();
            sendBytes = NetworkUtil.NETWORK_CMD_FILE_CLIENT_TO_SERVER.getBytes();
            os.write(sendBytes, 0, sendBytes.length);
            os.flush();

            Thread.sleep(500);

            /* send file name */
            File sendFile = new File(RealPathUtil
                    .getRealPath(getContext(), mUriSelectedFilePath));
            fileName = sendFile.getName();
            sendBytes = fileName.getBytes();
            os.write(sendBytes, 0, sendBytes.length);
            os.flush();

            Thread.sleep(500);

            /* send file content */
            sendBytes = new byte[(int) sendFile.length()];
            bis = new BufferedInputStream(
                    new FileInputStream(sendFile));
            bis.read(sendBytes, 0, sendBytes.length);
            os.write(sendBytes, 0, sendBytes.length);
            os.flush();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}