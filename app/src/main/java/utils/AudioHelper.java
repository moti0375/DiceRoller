package utils;

import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.view.View;

import com.bartovapps.diceroller.R;

import java.io.IOException;

/**
 * Created by motibartov on 18/04/2017.
 */

public class AudioHelper {

    private SoundPool mSoundPool;

    private Activity mContext;

    private boolean mLoaded;
    private float mVolume;

    int rollDiceSoundId;


    public AudioHelper(Activity context){
        mContext = context;
    }

    public void prepareSoundPool() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        float actVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mVolume = actVolume / maxVolume;

        mContext.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attributes = new AudioAttributes.Builder().
                    setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            mSoundPool = new SoundPool.Builder().setAudioAttributes(attributes).build();

        } else {
            mSoundPool = new SoundPool(6, AudioManager.STREAM_MUSIC, 0);
        }

        mSoundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> mLoaded = true);

        rollDiceSoundId = mSoundPool.load(mContext, R.raw.roll_dice, 1);

    }

    public void playRollDice() {
        if(mLoaded){
            mSoundPool.play(rollDiceSoundId, mVolume, mVolume, 1, 0, 1f);
        }
    }

    public void stopDiceAudio(){
        if (mLoaded){
            mSoundPool.autoPause();

        }
    }


    public int getAudioDuration()  {
        //Using getApplicationContext will help to improve media player operation during configuration changes
        MediaPlayer mPopSound = MediaPlayer.create(mContext.getApplicationContext(), R.raw.roll_dice);

        int duration = mPopSound.getDuration();
        mPopSound.release();

        return duration;
    }

    }
