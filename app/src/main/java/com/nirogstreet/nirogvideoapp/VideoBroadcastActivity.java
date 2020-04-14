package com.nirogstreet.nirogvideoapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.nirogstreet.nirogvideoapp.stats.StatsManager;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.BeautyOptions;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

public class VideoBroadcastActivity extends AppCompatActivity {

    private static final String LOG_TAG = VideoBroadcastActivity.class.getSimpleName();
    private static final int PERMISSION_REQ_ID = 22;
    // Ask for Android device permissions at runtime.
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private RtcEngine rtcEngine;
    private int mRole;
    private boolean mCallEnd;
    private FrameLayout mLocalContainer;
    private VideoGridContainer videoGridContainer;
    private SurfaceView localSurfaceView;
    //private String mRoomName;
    private StatsManager statsManager = new StatsManager();
    private RelativeLayout mRemoteContainer;
    private SurfaceView remoteSurfaceView;
    private ImageView mMuteAudioBtn;
    private ImageView mMuteVideoBtn;

    private static final int BEAUTY_EFFECT_DEFAULT_CONTRAST = BeautyOptions.LIGHTENING_CONTRAST_NORMAL;
    private static final float BEAUTY_EFFECT_DEFAULT_LIGHTNESS = 0.7f;
    private static final float BEAUTY_EFFECT_DEFAULT_SMOOTHNESS = 0.5f;
    private static final float BEAUTY_EFFECT_DEFAULT_REDNESS = 0.1f;
    public static final BeautyOptions DEFAULT_BEAUTY_OPTIONS = new BeautyOptions(
            BEAUTY_EFFECT_DEFAULT_CONTRAST,
            BEAUTY_EFFECT_DEFAULT_LIGHTNESS,
            BEAUTY_EFFECT_DEFAULT_SMOOTHNESS,
            BEAUTY_EFFECT_DEFAULT_REDNESS);

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {

         @Override
        // Listen for the onJoinChannelSuccess callback.
        // This callback occurs when the local user successfully joins the channel.
        public void onJoinChannelSuccess(String channel, final int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("agora", "Join channel success, uid: " + (uid & 0xFFFFFFFFL));
                   /* if (mRole == Constants.CLIENT_ROLE_AUDIENCE) {
                        mMuteVideoBtn.setActivated(false);
                        onMuteAudioClicked(mMuteVideoBtn);
                    }*/
                }
            });
        }

        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("agora", "First remote video decoded, uid: " + (uid & 0xFFFFFFFFL));
                    setupRemoteVideo(uid);
                }
            });
            super.onFirstRemoteVideoDecoded(uid, width, height, elapsed);
        }

        @Override
        // Listen for the onUserOffline callback.
        // This callback occurs when the broadcaster leaves the channel or drops offline.
        public void onUserOffline(final int uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("agora", "User offline, uid: " + (uid & 0xFFFFFFFFL));
                    removeRemoteUser(uid);
                }
            });
        }

        @Override
        public void onFirstRemoteVideoFrame(int uid, int width, int height, int elapsed) {
            super.onFirstRemoteVideoFrame(uid, width, height, elapsed);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_broadcast);


        // If all the permissions are granted, initialize the RtcEngine object and join a channel.
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
            initAgoraEngineAndJoinChannel();
        }

    }

    private void initAgoraEngineAndJoinChannel() {
        // Pass in the user role set by the user.
        mRole = getIntent().getIntExtra("CRole", 0);
        //mRoomName = getIntent().getStringExtra("CName");

        boolean isBroadcaster =  (mRole == Constants.CLIENT_ROLE_BROADCASTER);

        mMuteVideoBtn = findViewById(R.id.live_btn_mute_video);
        mMuteVideoBtn.setActivated(isBroadcaster);

        mMuteAudioBtn = findViewById(R.id.live_btn_mute_audio);
        mMuteAudioBtn.setActivated(isBroadcaster);

        ImageView beautyBtn = findViewById(R.id.live_btn_beautification);
        beautyBtn.setActivated(true);

        videoGridContainer = findViewById(R.id.live_video_grid_layout);
        videoGridContainer.setStatsManager(statsManager);

        initializeEngine();     // Tutorial Step 1
        setChannelProfile();
        setupVideoProfile();
        setClientRole();
        joinChannel();               // Tutorial Step 2
    }

    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }

        return true;
    }


    // Initialize the RtcEngine object.
    private void initializeEngine() {
        try {
            rtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
            //rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        } catch (Exception e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void setChannelProfile() {
        rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
    }

    private void setClientRole() {
        rtcEngine.setClientRole(mRole);
        if (mRole == Constants.CLIENT_ROLE_BROADCASTER) {
            setupLocalVideo();
        }
    }

    private void joinChannel() {

        // For SDKs earlier than v3.0.0, call this method to enable interoperability between the Native SDK and the Web SDK if the Web SDK is in the channel. As of v3.0.0, the Native SDK enables the interoperability with the Web SDK by default.
        rtcEngine.enableWebSdkInteroperability(true);
        // Join a channel with a token.
        String token = getString(R.string.agora_access_token);
        if (TextUtils.isEmpty(token) || TextUtils.equals(token, "Your Access Token")) {
            token = null; // default, no token
        }
        rtcEngine.joinChannel(token, "NirogVideoApp", "Extra Optional Data", 0);
    }

    private void setupLocalVideo() {
        // Enable the video module.
        rtcEngine.enableVideo();
        // Create a SurfaceView object.
        localSurfaceView = rtcEngine.CreateRendererView(getBaseContext());
        localSurfaceView.setZOrderMediaOverlay(true);
        //videoGridContainer.addView(localSurfaceView);
        videoGridContainer.addUserVideoSurface(0, localSurfaceView, true);
        // Set the local video view.
        VideoCanvas localVideoCanvas = new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0);
        rtcEngine.setupLocalVideo(localVideoCanvas);
    }

    private void removeLocalVideo() {
        rtcEngine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
        rtcEngine.setupLocalVideo(null);
        videoGridContainer.removeUserVideo(0, true);
    }


    private void setupRemoteVideo(int uid) {
        // Create a SurfaceView object.
        remoteSurfaceView = RtcEngine.CreateRendererView(getBaseContext());
        // Set the remote video view.
        rtcEngine.setupRemoteVideo(new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
        videoGridContainer.addUserVideoSurface(uid, remoteSurfaceView, false);

    }

    private void removeRemoteUser(int uid) {
        videoGridContainer.removeUserVideo(uid, false);
        remoteSurfaceView = null;
        rtcEngine.setupRemoteVideo(new VideoCanvas(null, VideoCanvas.RENDER_MODE_HIDDEN, uid));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mCallEnd) {
            leaveChannel();
        }
        RtcEngine.destroy();
    }

    private void leaveChannel() {
        // Leave the current channel.
        rtcEngine.leaveChannel();
    }

    public void onLeaveClicked(View view) {
        finish();
    }

    public void onSwitchCameraClicked(View view) {
        rtcEngine.switchCamera();
    }

    public void onBeautyClicked(View view) {
        view.setActivated(!view.isActivated());
        rtcEngine.setBeautyEffectOptions(view.isActivated(), DEFAULT_BEAUTY_OPTIONS);
    }

    public void onMoreClicked(View view) {
        // Do nothing at the moment
    }

    public void onPushStreamClicked(View view) {
        // Do nothing at the moment
    }

    public void onMuteAudioClicked(View view) {
        if (!mMuteVideoBtn.isActivated()) return;
        rtcEngine.muteLocalAudioStream(view.isActivated());
        view.setActivated(!view.isActivated());
    }

    public void onMuteVideoClicked(View view) {
        if (view.isActivated()) {
            mRole=Constants.CLIENT_ROLE_AUDIENCE;
            removeLocalVideo();
        } else {
            mRole=Constants.CLIENT_ROLE_BROADCASTER;
            setClientRole();
        }
        view.setActivated(!view.isActivated());
    }

    public static int[] VIDEO_MIRROR_MODES = new int[]{
            io.agora.rtc.Constants.VIDEO_MIRROR_MODE_AUTO,
            io.agora.rtc.Constants.VIDEO_MIRROR_MODE_ENABLED,
            io.agora.rtc.Constants.VIDEO_MIRROR_MODE_DISABLED,
    };

    private void setupVideoProfile() {
        rtcEngine.enableVideo();
        rtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(VideoEncoderConfiguration.VD_640x480, VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
    }
}
