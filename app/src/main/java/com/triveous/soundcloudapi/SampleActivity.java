package com.triveous.soundcloudapi;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import com.soundcloud.SoundcloudService;
import com.soundcloud.utils.SoundcloudConstants;

/**
 * Created by sohammondal on 20/09/14.
 */
public class SampleActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SoundcloudService.authSoundcloud(this);
        SoundcloudService.uploadTrack(this, "my title", "/alpha.mp3");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // check entry flag
        if (SoundcloudService.authStarted) {
            // get the uri in the intent
            try {
                Uri uri = Uri.parse(getIntent().getDataString());
                // find the token
                String totalAccessToken = uri.getFragment();
                String finalAccessToken = String.valueOf(totalAccessToken.subSequence(totalAccessToken.indexOf("=") + 1,
                        totalAccessToken.lastIndexOf("&")));

                // save the state, token and code
                SharedPreferences prefs = getSharedPreferences("recorder", MODE_PRIVATE);
                SharedPreferences.Editor prefEdit = prefs.edit();

                SoundcloudConstants.PREFERENCE_SOUNDCLOUD_ACTIVEACCOUNT = true;
                SoundcloudConstants.PREFERENCE_SOUNDCLOUD_ACCESS_TOKEN = finalAccessToken;
                SoundcloudConstants.PREFERENCE_SOUNDCLOUD_CODE = uri.getQueryParameter("code");

                return;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
