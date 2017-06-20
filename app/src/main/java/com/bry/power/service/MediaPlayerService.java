package com.bry.power.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.drm.DrmStore;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.bry.power.ui.MainActivity;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by bryon on 6/18/2017.
 */

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
      MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener{

    private final IBinder iBinder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private String mediaFile;
    private int resumePosition;
    private AudioManager audioManager;
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    private ArrayList<Audio> audioList;
    private int audioIndex = -1;
    private Audio activeAudio;

    /////
    @Override
    public void onCreate(){
        super.onCreate();
        callStateListener();
        registerBecomingNoisyReceiver();
        register_playNewAudio();
    }
    @Override
    public IBinder onBind(Intent intent){
        return iBinder;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent){

    }

    @Override
    public void onCompletion(MediaPlayer mp){
        //Invoked when playback of a media source has completed.
        stopMedia();
        //stops the service
        stopSelf();
    }

      //For handling errors..
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra){
        //Invoked when an asynchronous operation has an error
        switch (what){
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error","MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK" + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED" + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("mediaPlayer Error","MEDIA ERROR UNKNOWN");
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what,int extra){
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp){
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp){

    }

    @Override
    public void onAudioFocusChange(int focusState){
        switch (focusState){
            case AudioManager.AUDIOFOCUS_GAIN:
                if(mediaPlayer == null) initMediaPlayer();
                else if(!mediaPlayer.isPlaying())mediaPlayer.start();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if(mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if(mediaPlayer.isPlaying())mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if(mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }
    ///////

    private void initMediaPlayer(){
        mediaPlayer = new MediaPlayer();

        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);

        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try{
            mediaPlayer.setDataSource(mediaFile);
        }catch(IOException e){
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
    }

    private void playMedia(){
        if(!mediaPlayer.isPlaying()){
            mediaPlayer.start();
        }
    }

    private void stopMedia(){
        if(mediaPlayer == null)return;
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
    }

    private void pauseMedia(){
        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }
    }

    private void resumeMedia(){
        if(!mediaPlayer.isPlaying()){
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }
    }

    private boolean requestAudioFocus(){
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
        if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            return true;
        }
        return false;
    }

    private boolean removeAudioFocus(){
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this);
    }

    //requested by the activity(s)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        try{
            mediaFile = intent.getExtras().getString("media");
        }catch(NullPointerException e){
            Log.d("onStartMedia method","DID NOT FIND ANY MEDIA FILES!!!!!!!!!!!!!!");
            stopSelf();
        }
        if(requestAudioFocus() == false)stopSelf();

        if(mediaFile != null && mediaFile != "") initMediaPlayer();

       return super.onStartCommand(intent, flags,startId);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (mediaPlayer != null){
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();
    }

    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
           pauseMedia();
//            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void registerBecomingNoisyReceiver(){
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver,intentFilter);
    }

    private void callStateListener(){
        telephonyManager= (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener(){
            @Override
            public void onCallStateChanged(int state, String incomingNumber){
                switch(state){
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if(mediaPlayer != null){
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if(mediaPlayer != null){
                            if(ongoingCall){
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_CALL_STATE);
    }

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            audioIndex = new StorageUtil(getApplicationContext()).loadAudioIndex();
            if(audioIndex != -1 && audioIndex< audioList.size()){
                activeAudio = audioList.get(audioIndex);
            }else{
                stopSelf();
            }
            stopMedia();
            mediaPlayer.reset();
            initMediaPlayer();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        }
    };

    private void register_playNewAudio(){
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio,filter);
    }


 public class LocalBinder extends Binder {
        public MediaPlayerService getService(){
            return MediaPlayerService.this;
        }
    }
}
