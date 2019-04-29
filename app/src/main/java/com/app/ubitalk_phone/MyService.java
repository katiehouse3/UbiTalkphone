package com.app.ubitalk_phone;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.app.ubitalk_phone.MainActivity;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class MyService extends Service {

    //Use audio manageer services
    static protected AudioManager mAudioManager;
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;
    protected final Messenger mServerMessenger = new Messenger(new IncomingHandler(this));

    //To turn screen On uitll lock phone
    PowerManager.WakeLock wakeLock;
    protected boolean mIsListening;
    protected volatile boolean mIsCountDownOn;
    static boolean mIsStreamSolo;

    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_CANCEL = 2;
    static int identify = 0, result = 0;
    String Currentdata = null, newcurrent = null;

    // Layout
    Handler customHandler;
    ConstraintLayout layout;

    // Layout Colors
    final String BLUE = "#448cff";
    final String RED = "#ea072c";
    final String GREEN = "#08c935";

    // Speed of speech
    final double slow = 1;
    final double fast = 4.5;
    private double n_words;
    private double n_words_total;
    public double total_accum_words = 0;
    private long start_time;
    private long overall_end_time;

    String speed = "On_pace";

    PrintWriter writer;

    @Override
    public void onCreate() {
        super.onCreate();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());


        try {
            overall_end_time = System.currentTimeMillis();
            String filename = MainActivity.usernameinput + "_" +
                    MainActivity.overall_start_time + "_" +
                    overall_end_time +".txt";
            Log.i("FILE", filename);
            FileOutputStream os = openFileOutput(filename, Context.MODE_PRIVATE);
            writer = new PrintWriter(os);
        } catch (Exception e) {
        }

    }

    protected static class IncomingHandler extends Handler {
        private WeakReference<MyService> mtarget;

        public IncomingHandler(MyService target) {
            mtarget = new WeakReference<MyService>(target);
        }


        @Override
        public void handleMessage(Message msg) {
            final MyService target = mtarget.get();

            switch (msg.what) {
                case MSG_RECOGNIZER_START_LISTENING:

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        // turn off beep sound
                        if (!mIsStreamSolo) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                            } else {
                                mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                            }
                            mIsStreamSolo = true;
                        }
                    }
                    if (!target.mIsListening) {
                        target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);
                        target.mIsListening = true;
                        //Log.d(TAG, "message start listening"); //$NON-NLS-1$
                    }
                    break;

                case MSG_RECOGNIZER_CANCEL:
                    if (mIsStreamSolo) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                            mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                        } else {
                            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                        }
                        mIsStreamSolo = false;
                    }
                    target.mSpeechRecognizer.cancel();
                    target.mIsListening = false;
                    //Log.d(TAG, "message canceled recognizer"); //$NON-NLS-1$
                    break;
            }
        }
    }

    // Count down timer for Jelly Bean work around
    protected CountDownTimer mNoSpeechCountDown;

    {
        mNoSpeechCountDown = new CountDownTimer(10000, 2000) {

            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                Log.i("UPDATE", "FINISHED");
                mIsCountDownOn = false;
                Message message;
                message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
                try {
                    mServerMessenger.send(message);
                    message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                    mServerMessenger.send(message);
                } catch (RemoteException e) {

                }
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mIsCountDownOn) {
            mNoSpeechCountDown.cancel();
        }
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.destroy();
        }

        writer.flush();
        writer.close();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mServerMessenger.getBinder();
    }

    protected class SpeechRecognitionListener implements RecognitionListener {

        @Override
        public void onBeginningOfSpeech() {
            // speech input will be processed, so there is no need for count down anymore
            Log.i("UPDATE", "BEGINNING OF SPEECH");
            if (mIsCountDownOn) {
                Log.i("UPDATE", "countdown on BEGINNING OF SPEECH");
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
            Log.i("UPDATE", "countdown off BEGINNING OF SPEECH");
            //Log.d(TAG, "onBeginingOfSpeech"); //$NON-NLS-1$
            start_time = System.currentTimeMillis();
            n_words_total = 0;
            n_words = 0;
        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {
            //Log.d(TAG, "onEndOfSpeech"); //$NON-NLS-1$
        }

        @Override
        public void onError(int error) {
            if (mIsCountDownOn) {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
            mIsListening = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
            try {
                mServerMessenger.send(message);
            } catch (RemoteException e) {

            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            Log.i("UPDATE", "onPartialResults");
            ArrayList data = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String word = (String) data.get(data.size() - 1);
            //if (Currentdata == null) {
            //MainActivity.textView.setText("" + word);
            //} else {
            //MainActivity.textView.setText(Currentdata + " " + word);
            //MainActivity.textView.setSelection(MainActivity.textView.getText().length());
            //}

            long curr_time = System.currentTimeMillis();

            newcurrent = MainActivity.textView.getText().toString();
            identify = 1;
            Log.i("UPDATE", "" + word);

            n_words = data.size();
            String curr_speed = speed;
            n_words_total += n_words;
            if (curr_time - start_time > 2000) {
                curr_speed = checkSpeed(n_words_total);
                start_time = curr_time;
                total_accum_words += n_words_total;
                writer.print(n_words_total + "," + total_accum_words + "," + curr_speed + "," + curr_time + "\n");
                n_words_total = 0;
            }
            Log.i("UPDATE", "TOTAL Number of Words: " + n_words_total);
            Log.i("Update", "Current Speed is" + speed);
            Log.i("Update", "New Speed is" + curr_speed);
            if (curr_speed != speed) {
                speed = curr_speed;
                changeColor(speed);
            }
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mIsCountDownOn = true;
                MainActivity.textView.setText("");
                mNoSpeechCountDown.start();
            }
            MainActivity.progressBar.setVisibility(View.INVISIBLE);
            Toast toast = Toast.makeText(getApplicationContext(), "Start Speaking", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            Log.i("UPDATE", "onReadyForSpeech"); //$NON-NLS-1$
        }

        @Override
        public void onResults(Bundle results) {

            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String word = (String) data.get(data.size() - 1);

            if (result == 0) {
                MainActivity.textView.setText(word);
                Currentdata = MainActivity.textView.getText().toString();
            } else if (result == 1) {
                if (Currentdata != null) {
                    MainActivity.textView.setText(Currentdata + "\n" + word);
                    //MainActivity.textView.setSelection(MainActivity.textView.getText().length());
                }
            }
            Currentdata = MainActivity.textView.getText().toString();

            Log.i("UPDATE", "" + Currentdata);

            if (mIsListening == true) {
                mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
            }
            result = 0;
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            Log.i("RMS", "MY SERVICE on RMS changed");
        }

    }
    private void startListening(String speed) {
        MainActivity.layout.setBackgroundColor(Color.parseColor(GREEN));
    }
    private void changeColor(String speed) {
        Log.i("UPDATE",  "Changing Color " +  speed);
        switch(speed) {
            case "Slow":
                MainActivity.layout.setBackgroundColor(Color.parseColor(BLUE));
                break;
            case "Fast":
                MainActivity.layout.setBackgroundColor(Color.parseColor(RED));
                break;
            default:
                MainActivity.layout.setBackgroundColor(Color.parseColor(GREEN));
        }
        //mText.setText(Integer.toString(n_filler));
    }
    private String checkSpeed(double n_words){

        double curr_speed = 0;

        curr_speed = n_words / 5; // calculate words per second
        Log.i("UPDATE",  "curr_speed " +  curr_speed);

        if (curr_speed <= slow){
            return "Slow";
        } else if (curr_speed > slow && curr_speed < fast) {
            return "On_pace";
        } else {
            return "Fast";
        }

    }
}