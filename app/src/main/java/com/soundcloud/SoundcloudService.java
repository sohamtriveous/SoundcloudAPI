package com.soundcloud;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.soundcloud.utils.SoundcloudConstants;
import com.soundcloud.utils.SoundcloudUtils;
import com.soundcloud.utils.Track;

/**
 * Class that provides various Soundcloud related functionality like - Upload
 * files to SoundCloud ("push") - Update stats (play/comment/fav) for one track
 * ("updateone") - Update stats (play/comment/fav) for all tracks track
 * ("udpateall")
 */

public class SoundcloudService extends IntentService {
    private static final String KEY_METHOD = "METHOD";
    private static final int METHOD_AUTH = 0;
    private static final int METHOD_UPLOAD = 1;

    private static final String KEY_TITLE = "TITLE";
    private static final String KEY_PATH = "PATH";

    public static boolean authStarted = false;

    /**
     * Soundcloud object
     */
    public static SoundcloudUtils mSoundob;

    /**
     * WakeLock variable
     */
    WakeLock mSoundcloudWakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public SoundcloudService() {
        super("soundcloudservice");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        acquireWakeLock();
        switch (intent.getIntExtra(KEY_METHOD, -1)) {
            case METHOD_UPLOAD:
                authStarted = true;
                String title = intent.getStringExtra(KEY_TITLE);
                String localPath = intent.getStringExtra(KEY_PATH);
                insert(title, localPath);
                break;
            case METHOD_AUTH:
                auth();
                break;

        }
        authStarted = false;
        releaseWakeLock();
    }

    /**
     * Will return if soundcloud is authenticated
     * TODO: do this from preferences
     *
     * @return
     */
    private boolean isAuthenticated() {
        return false;
    }

    /**
     * Auth.
     */
    public boolean auth() {
        mSoundob = new SoundcloudUtils(getApplicationContext());
        // temp
        if (isAuthenticated()) {
            mSoundob.auth(
                    SoundcloudConstants.SOUNDCLOUD_CLIENT_ID,
                    SoundcloudConstants.SOUNDCLOUD_CLIENT_SECRET,
                    SoundcloudConstants.SOUNDCLOUD_REDIRECT_URI,
                    SoundcloudConstants.PREFERENCE_SOUNDCLOUD_ACCESS_TOKEN,
                    SoundcloudConstants.PREFERENCE_SOUNDCLOUD_CODE);
            return true;
        } else {
            return false;
        }
    }

    /**
     * upload a file to soundcloud
     *
     * @param title     the title of the file to be uploaded
     * @param localPath the path to the file
     * @return true, if successful
     */
    boolean insert(String title, String localPath) {
        // upload to soundcloud
        Track track = mSoundob.postTrack(new Track(title, localPath));
        //mSoundob.uploadFile(mSoundob.wrapper,"/mnt/sdcard/AudioRecorder/09-01-2013-210530.wav");
        return true;
    }

    /**
     * Updates/Finds the plays/comments/favourites for a track with track id trackid
     *
     * @param trackId the track id
     */
    private void updateOne(int trackId) {
        // network op to get the track info
        Track track = mSoundob.getTrack(trackId);
        if (track == null) {
            return;
        }

        // get the counts
        int playbackCount = track.getPlaybackCount();
        int commentCount = track.getCommentCount();
        int favoriteCount = track.getFavoritingsCount();
    }

    /**
     * Acquire Wake-Lock
     */
    public void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mSoundcloudWakeLock = pm.newWakeLock(pm.PARTIAL_WAKE_LOCK, "SoundcloudService");
        mSoundcloudWakeLock.acquire();
    }

    /**
     * Release Wake-Lock
     */
    public void releaseWakeLock() {
        try {
            if (mSoundcloudWakeLock != null)
                mSoundcloudWakeLock.release();
        } catch (Exception e) {
        }
    }

    public static void uploadTrack(final Context context, String title, String localPath) {
        Intent i = new Intent(context, SoundcloudService.class);
        i.putExtra(KEY_METHOD, METHOD_UPLOAD);
        i.putExtra(KEY_TITLE, title);
        i.putExtra(KEY_PATH, localPath);
        context.startService(i);
    }

    public static void authSoundcloud(final Context context) {
        Intent i = new Intent(context, SoundcloudService.class);
        i.putExtra(KEY_METHOD, METHOD_AUTH);
        context.startService(i);
    }
}
