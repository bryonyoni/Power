package com.bry.power.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

/**
 * Created by bryon on 6/18/2017.
 */

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
      MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener{

    private final IBinder iBinder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private String mediaFile;

    @Override
    public IBinder onBind(Intent intent){
        return iBinder;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent){

    }

    @Override
    public void onCompletion(MediaPlayer mp){

    }

    //For handling errors..
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra){
        //Invoked when an asynchronous operation has an error
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what,int extra){
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp){

    }

    @Override
    public void onSeekComplete(MediaPlayer mp){

    }

    @Override
    public void onAudioFocusChange(int focusChange){

    }

    private void initMediaPlayer(){
        mediaPlayer = new MediaPlayer();

        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);

        mediaPlayer.reset();

//        mediaPlayer.setAudioStream
    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService(){
            return MediaPlayerService.this;
        }
    }
}
