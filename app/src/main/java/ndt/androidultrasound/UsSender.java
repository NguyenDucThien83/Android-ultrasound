package ndt.androidultrasound;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

/**
 * Created by Thien
 */
public class UsSender {
    static final String TAG = "UsSender";

    private AudioTrack player;
    private long audioSendID;
    private Message message;
    private OnUsSendListener listener;
    private Exception exception;
    private final int periodInFrames = Paras.SAMPLE_RATE/20;
    private int countFrames = 0;
    protected int totalFrames = 441000;

    public UsSender(long audioSendID, OnUsSendListener onAudioSendListener){
        Log.v(TAG, "construct");
        this.audioSendID = audioSendID;
        this.listener = onAudioSendListener;
        exception = new Exception(" Error in sending ultrasound message.");

    }

    public void send(Message message){
        Log.v(TAG, "send");
        this.message = message;
        boolean result = execute();
        onPostExecute(result);
    }
    /**
     * Stop sending immediately.
     */
    public void stop(){
        Log.v(TAG, "stop");
        if (player.getPlayState()== AudioTrack.PLAYSTATE_PLAYING){
            player.stop();
        }
    }


    private boolean execute() {
        Log.v(TAG, "execute");
        try{
            //1. encode message
            short[] audioSignal = modulate(message.getEncodedMsgBits());
            if(audioSignal == null){
                exception = new Exception("Cannot encode the message to audio signal");
                Log.v(TAG, "Cannot encode the message to audio signal");
                return false;
            }
            totalFrames = audioSignal.length;
            // initialize audio player
            if(!initPlayer(totalFrames)){
                exception = new Exception("Cannot initialize Audio player.");
                return false;
            }

            // 2. write to buffer
            player.write(audioSignal, 0, audioSignal.length);
            Log.v(TAG, " audioSignal.length " + audioSignal.length);
            Log.v(TAG, " timeToPlay " + 1000 * audioSignal.length / player.getSampleRate());


            //3. play audio
            player.play();

            int count = 0;
            try{
                Log.v(TAG, "start playing");
                while (player.getPlayState() == AudioTrack.PLAYSTATE_PLAYING && count<10 ) {
                    if(count == 0){
                        Thread.sleep(message.getTimeToSend());
                    }else{
                        Thread.sleep(20);
                    }
                    count++;
                    Log.v(TAG, " still playing " + count);

                }
                if(player.getPlayState() == AudioTrack.PLAYSTATE_PLAYING){
                    Log.v(TAG, "Why does the player not stop?");
                }
                Log.v(TAG, "stop player");
                player.stop();
                if(player.getPlayState() == AudioTrack.PLAYSTATE_PLAYING){
                    Log.v(TAG, "Why does the player not stop?");
                }else Log.v(TAG, "player is stopped");

            }catch(Exception e) {
                exception = e;
            }

        }catch (Exception e){
            exception =e;
            return false;
        }
        return true;
    }


    protected void onProgressUpdate(Integer... progress) {
        if(listener != null && progress.length >0){
            listener.onSendProgress(audioSendID,progress[0]);
        }

    }


    protected void onPostExecute(Boolean status){
        reset();
        if(listener != null && status){
            Log.v(TAG, "onSendFinish");
            listener.onSendFinish(audioSendID);

        } else{
            Log.v(TAG, "onSendFailed");
            exception.printStackTrace();
            listener.onSendFailed(audioSendID, exception);

        }

    }

    protected AudioTrack.OnPlaybackPositionUpdateListener playerUpdateListener = new AudioTrack.OnPlaybackPositionUpdateListener() {
        @Override
        public void onMarkerReached(AudioTrack audioTrack) {
            player.stop();
        }

        @Override
        public void onPeriodicNotification(AudioTrack audioTrack) {
            countFrames += periodInFrames;
            int percent = 100*countFrames/totalFrames;
            //publishProgress(percent);
            Log.v("onPlayNotification", "" + percent);

        }

    };

    /**
     * Stop playing and prepare player for using in the future.
     */
    protected void reset(){
        Log.v(TAG, "reset");
        if (player.getPlayState()== AudioTrack.PLAYSTATE_PLAYING){
            player.stop();
        }
        player.release();
    }

    private short[] modulate(byte[] msgBits){
        Modulator modulator = new Modulator();
        return modulator.modulate(msgBits);
    }

    private boolean initPlayer(int bufferSizeInInFrames){
        try{
            player = new AudioTrack(AudioManager.STREAM_MUSIC, Paras.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                    Paras.AUDIO_ENCODING, bufferSizeInInFrames*2 +2, AudioTrack.MODE_STATIC);
            player.setPlaybackPositionUpdateListener(playerUpdateListener);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                player.setVolume((float)0.95 * AudioTrack.getMaxVolume());
            }else{
                player.setStereoVolume((float) 0.95 * AudioTrack.getMaxVolume(), (float) 0.95 * AudioTrack.getMaxVolume());
            }
            player.setNotificationMarkerPosition(bufferSizeInInFrames);
        } catch(Exception e){
            exception = e;
            Log.v(TAG, "cannot initPlayer");
            return false;
        }
        return  true;
    }

}
