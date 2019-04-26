package com.app.ubitalk_phone;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
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
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.app.ubitalk_phone.R;

import java.io.PrintWriter;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    Button speak, stop;
    static TextView textView;

    private SpeechRecognizer sr;
    private static final String TAG = "MyActivity";
    ProgressDialog dialog;
    int code;
    private Messenger mServiceMessenger;
    boolean isEndOfSpeech = false;
    boolean serviceconneted;

    static final Integer LOCATION = 0x1;

    // Layout
    Handler customHandler;
    static ConstraintLayout layout;

    // Layout Colors
    final String GREEN = "#08c935";

    String speed = "On pace";

    PrintWriter writer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layout = findViewById(R.id.frameLayout);

        speak = findViewById(R.id.speak);
        stop = findViewById(R.id.stop);

        textView = findViewById(R.id.write);

        customHandler = new android.os.Handler();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        stop.setVisibility(View.INVISIBLE);



        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, com.app.ubitalk_phone.MyService.class);
                stopService(i);
                Toast.makeText(MainActivity.this, "stop speaking", Toast.LENGTH_SHORT).show();
                textView.setText("");
                speak.setVisibility(View.VISIBLE);
                stop.setVisibility(View.INVISIBLE);
                layout.setBackgroundColor(Color.DKGRAY);
            }
        });

        //sr = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
        //sr.setRecognitionListener(new Listner());

        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    askForPermission(Manifest.permission.RECORD_AUDIO, LOCATION);
                }
                Intent i = new Intent(MainActivity.this, com.app.ubitalk_phone.MyService.class);
                bindService(i, connection, code);
                startService(i);
                Toast.makeText(MainActivity.this, "Start Speaking", Toast.LENGTH_SHORT).show();
                speak.setVisibility(View.INVISIBLE);
                stop.setVisibility(View.VISIBLE);
                MainActivity.layout.setBackgroundColor(Color.parseColor(GREEN));
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
            Toast.makeText(this, "" + permission + " is already granted.", Toast.LENGTH_SHORT).show();
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

            Log.d("service", "connected");

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
            Log.d("service", "disconnected");
        }
    };
}

