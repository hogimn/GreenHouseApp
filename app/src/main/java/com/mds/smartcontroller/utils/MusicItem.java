package com.mds.smartcontroller.utils;

public class MusicItem {
    private String mTitle;
    private boolean mPlayOrStop;

    public MusicItem(String title, boolean playOrStop) {
        this.mTitle = title;
        this.mPlayOrStop = playOrStop;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public boolean getPlayOrStop() {
        return mPlayOrStop;
    }

    public void setPlayOrStop(boolean playOrStop) {
        this.mPlayOrStop = playOrStop;
    }
}
