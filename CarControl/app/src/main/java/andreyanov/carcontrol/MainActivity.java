package andreyanov.carcontrol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import andreyanov.carcontrol.R;
import andreyanov.carcontrol.tools.Bluetooth;
import andreyanov.carcontrol.widget.JoystickView;
import andreyanov.carcontrol.widget.VerticalSeekBar;

public class MainActivity extends Activity implements Bluetooth.BluetoothDataListener
{
    TextView myLabel;

    Bluetooth b;

    ProgressBar distance;

    JoystickView control;
    SeekBar head;

    int abs_l = 0, abs_r = 0, sa = 90;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        control = (JoystickView) findViewById(R.id.control);
        control.invalidate();

        head = (SeekBar) findViewById(R.id.seekBar2);

        Button openButton = (Button)findViewById(R.id.open);
        myLabel = (TextView)findViewById(R.id.log);
        b = new Bluetooth(this);
        b.addListener(this);
        //Open Button
        openButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    Log.d("MY", "start init BT");
                    b.init();
                }
            });
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        b.closeBT();
    }
    @Override
    public void dataReady(String data) {
        String[] pairs = data.split(",");

        Log.d("BT", "ReceiveData: " + data);
        for (String s : pairs) {
            String[] comm = s.split("=");
            try {
                int value = Integer.parseInt(comm[1]);
                if (comm[0].equalsIgnoreCase("ud")) {
//                    distance.setProgress(value);
                }
            } catch(Exception e) {}
        }
    }

    @Override
    public void connected() {
        Log.i("MY", "BT connected, start send");
        control.postDelayed(new ControlSendRunner(), 1000L);
    }

    void setEngineData() {
        float l = 0, r = 0;
        if (control == null) return;

        int direction = control.getDirection();
        int power = control.getPower();
        Log.d("Control", "direction = " + direction + "; power = " + power + ";  angle = " + control.getAngle());

        switch (direction) {
            case JoystickView.FRONT: {
                l = 1f;
                r = 1f;
                break;
            }
            case JoystickView.LEFT_FRONT: {
                l = 1f;
                r = 0.75f;
                break;
            }
            case JoystickView.LEFT: {
                l = 1;
                r = -1;
                break;
            }
            case JoystickView.RIGHT: {
                l = -1;
                r = 1;
                break;
            }
            case JoystickView.FRONT_RIGHT: {
                r = 1f;
                l = 0.75f;
                break;
            }
            case JoystickView.BOTTOM: {
                l = -1f;
                r = -1f;
                break;
            }
            case JoystickView.BOTTOM_LEFT: {
                l = -1;
                r = -0.75f;
                break;
            }
            case JoystickView.RIGHT_BOTTOM: {
                r = -1f;
                l = -0.75f;
                break;
            }
            default: {
                l = 0;
                r = 0;
                break;
            }
        }
        if (power < 10) {
            l = 0;
            r = 0;
        }

        abs_l = (int) (l * 255);
        abs_r = (int) (r * 255);
    }


    class ControlSendRunner implements Runnable {

        @Override
        public void run() {
            setEngineData();
            int sa = head.getProgress();
            b.sendData("abs_l="+abs_l+",abs_r="+abs_r+",sa="+sa);

            control.postDelayed(this , 20L);
        }
    }
}