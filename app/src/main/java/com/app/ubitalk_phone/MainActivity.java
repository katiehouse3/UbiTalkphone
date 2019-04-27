package com.app.ubitalk_phone;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.SpeechRecognizer;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.CountDownTimer;

import java.io.PrintWriter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {


    Button speak, stop,new_user;
    static TextView textView, timerView;
    EditText username;
    static ProgressBar progressBar;
    static String usernameinput;
    static long overall_start_time;
    TextView nameView;

    private SpeechRecognizer sr;
    private static final String TAG = "MyActivity";
    ProgressDialog dialog;
    int code;
    private Messenger mServiceMessenger;
    boolean isEndOfSpeech = false;
    boolean serviceconneted;
    boolean new_user_clicked = false;
    static final Integer LOCATION = 0x1;

    // Layout
    Handler customHandler;
    static ConstraintLayout layout;

    // Layout Colors
    final String GREEN = "#08c935";
    final String LIGHTGRAY = "#F8F8F8";


    String speed = "On pace";

    PrintWriter writer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layout = findViewById(R.id.frameLayout);

        speak = findViewById(R.id.speak);
        stop = findViewById(R.id.stop);
        new_user = findViewById(R.id.new_user);
        nameView = findViewById(R.id.nameView);
        username = findViewById(R.id.username);
        progressBar = findViewById(R.id.progressBar);
        timerView = findViewById(R.id.timerView);

        textView = findViewById(R.id.write);
        textView.setVisibility(View.INVISIBLE);

        username.setVisibility(View.INVISIBLE);

        customHandler = new android.os.Handler();

        final CountUpTimer timer = new CountUpTimer(10000000) {
            public void onTick(int second) {
                String text = String.format(Locale.getDefault(), "%02d:%02d.%1d",
                        TimeUnit.MILLISECONDS.toMinutes(second) % 60,
                        TimeUnit.MILLISECONDS.toSeconds(second) % 60,
                        ((TimeUnit.MILLISECONDS.toMillis(second)
                                - TimeUnit.MILLISECONDS.toSeconds(second) % 60) % 1000) /100 % 10);
                timerView.setText(text);
            }
        };

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        stop.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        layout.setBackgroundColor(Color.parseColor(LIGHTGRAY));

        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    askForPermission(Manifest.permission.RECORD_AUDIO, LOCATION);
                }
                Intent i = new Intent(MainActivity.this, com.app.ubitalk_phone.MyService.class);
                bindService(i, connection, code);
                startService(i);
                progressBar.setVisibility(View.VISIBLE);
                speak.setVisibility(View.INVISIBLE);
                stop.setVisibility(View.VISIBLE);
                username.setVisibility(View.INVISIBLE);

                timer.start();
                overall_start_time = System.currentTimeMillis();
                MainActivity.layout.setBackgroundColor(Color.parseColor(GREEN));
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, com.app.ubitalk_phone.MyService.class);
                stopService(i);
                Toast toast = Toast.makeText(getApplicationContext(), "stop Speaking", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                textView.setText("");
                speak.setVisibility(View.VISIBLE);
                stop.setVisibility(View.INVISIBLE);
                layout.setBackgroundColor(Color.parseColor(LIGHTGRAY));
                timer.cancel();

            }
        });

        new_user.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (new_user_clicked == false){
                    username.setVisibility(View.VISIBLE);
                    new_user_clicked = true;
                    nameView.setVisibility(View.INVISIBLE);
                    new_user.setText("enter");
                }
                else {
                    username.setVisibility(View.INVISIBLE);
                    usernameinput = username.getText().toString();
                    nameView.setText(usernameinput);
                    nameView.setVisibility(View.VISIBLE);
                    new_user_clicked = false;
                    new_user.setText("new speech");
                };
            }
        });
    }
    private void askForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);

            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
            }
        } else {
            //Toast.makeText(this, "" + permission + " is already granted.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED) {


            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            }

        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }



    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            mServiceMessenger = new Messenger(service);
            Message msg = new Message();
            msg.what = com.app.ubitalk_phone.MyService.MSG_RECOGNIZER_START_LISTENING;
            try {
                mServiceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            serviceconneted = false;
        }
    };
    public abstract class CountUpTimer extends CountDownTimer {
        private static final long INTERVAL_MS = 1;
        private final long duration;

        protected CountUpTimer(long durationMs) {
            super(durationMs, INTERVAL_MS);
            this.duration = durationMs;
        }

        public abstract void onTick(int second);

        @Override
        public void onTick(long msUntilFinished) {
            int second = (int) ((duration - msUntilFinished));
            onTick(second);
        }

        @Override
        public void onFinish() {
            onTick(duration);
        }
    }
}

