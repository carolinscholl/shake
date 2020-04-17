package de.tu_chemnitz.mi.medienapplikationen.shake;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import com.john.waveview.WaveView;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements SensorEventListener {


    public TextView Shake;
    public WaveView wave;
    public ImageButton sensorinfo;
    public ImageButton settings;

    // Diese Variablen brauchen wir, um den Sensor zu aktivieren und zu verwenden
    public SensorManager sm;
    public Sensor accel;

    // Diese Variablen brauchen wir, um zu berechnen, wann der Cocktail fertig gemixt ist
    private static final int HIGH_THRESHOLD = 5;
    private static final int LOW_THRESHOLD = 3;
    public static final int SCHWELLE_DOWN = 1;
    private int schwelle_up;


    public int waterlevel;
    public boolean StartedShake;

    public double lasttime;
    public double currtime;

    public boolean listpresent;
    public boolean readytoshake = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Shake = (TextView)findViewById(R.id.Shakeme);
        Typeface besomfont = Typeface.createFromAsset(getAssets(), "Besom-free-font.ttf");
        Shake.setTypeface(besomfont);
        if(readytoshake) {
            Shake.setVisibility(View.VISIBLE);
        }

        wave = (WaveView) findViewById(R.id.wave_view);

        // Default-Werte für Variablen bei erster Initialisierung
        waterlevel = 20;
        StartedShake = false;
        lasttime = System.currentTimeMillis();

        if(createSensorList()) {

            listpresent = false;

            sensorinfo = (ImageButton) findViewById(R.id.info);
            sensorinfo.setVisibility(View.VISIBLE);

            sensorinfo.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (!listpresent) {
                        showSensorList();
                        listpresent = true;
                    } else {
                        hideSensorList();
                        listpresent = false;
                    }
                }
            });
        }


        schwelle_up = HIGH_THRESHOLD;       // default threshold

        settings = (ImageButton) findViewById(R.id.sensitivity);
        settings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(MainActivity.this, settings);

                    popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            int id = item.getItemId();

                            if (id==R.id.high) {
                                schwelle_up = HIGH_THRESHOLD;
                            }
                            else if (id==R.id.low) {
                                schwelle_up = LOW_THRESHOLD;
                            }
                            return true;
                        }
                    });
                    if(!listpresent){
                        if(schwelle_up==HIGH_THRESHOLD)
                            popup.getMenu().findItem(R.id.high).setChecked(true);
                        if(schwelle_up==LOW_THRESHOLD)
                            popup.getMenu().findItem(R.id.low).setChecked(true);
                        popup.show();

                    }
                }
            });



        if(initiateSensor()){
            // Show a toast that accelerometer is active
            Toast toast = Toast.makeText(this, "Accelerometer now active", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
        }



    }


    // Sensorliste erstellen

    public boolean createSensorList(){

        // Zugriff auf die Hardware: Erstellen eines Sensor-Managers
        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensorlist = sm.getSensorList(Sensor.TYPE_ALL);
        if(sensorlist!=null){               // GEÄNDERT!
            createListView(sensorlist);
        }

        return true;
    }



    // Sensor initialisierern

    public boolean initiateSensor() {

        if(sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!=null) {

            // Sensortyp festlegen: Beschleunigungssensor
            accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            // Bekanntmachung des Sensor-Listeners mit dem Sensor-Manager
            sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
        }

        return true;
    }



    // 1. Callback-Methode des Sensor-Event-Listeners

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        // Der Funktion wird eine SensorEvent-Instanz übergeben. Diese nutzen wir nun:
        Sensor meinSensor = sensorEvent.sensor;

        // Überprüfung, ob der übergebene Sensor auch wirklich ein Beschleunigungssensor ist:
        if (meinSensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            // Wir fragen nur alle 200ms die Beschleunigungswerte ab
            currtime = System.currentTimeMillis();

            if (currtime - lasttime > 200) {
                calculateAcc(sensorEvent);
                lasttime = currtime;
            }
        }
    }


    // 2. Callback-Methode des SensorEvent-Listeners
    // (wir machen im Fall einer Messgenauigkeitsänderung gar nichts)

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }



    // Beschleunigung berechnen

    public void calculateAcc(SensorEvent sensorEvent) {

        // Speichern der Sensor-Werte für alle drei Achsen
        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        // Berechnen der Länge des Vektors über seinen Betrag
        float acc = (float) Math.sqrt((x * x + y * y + z * z));

        // Abziehen der Gravitation
        acc = acc - SensorManager.GRAVITY_EARTH;

        setWaterlevel(acc);
    }


    // Reaktionen auf die Änderung der Beschleunigung

    public void setWaterlevel(float acc){

        if (acc > schwelle_up) {
            waterlevel = waterlevel + 5;
            wave.setProgress(waterlevel);
        }

        if (waterlevel > 40 && !StartedShake) {
            Shake.setText("Keep shaking!");
            moveIceCubes();
            gradientAnimation();
            StartedShake = true;
        }

        if (waterlevel > 80 && StartedShake){
            Shake.setText("Almost done!");
        }

        if (acc < SCHWELLE_DOWN && waterlevel >= 22) {
            waterlevel = waterlevel - 2;
            wave.setProgress(waterlevel);
        }

        if (waterlevel < 60 && StartedShake){
            Shake.setText("Keep shaking!");
        }

        if (waterlevel < 23 && StartedShake) {
            Shake.setText("C'mon, shake!");
            StartedShake = false;
        }

        if (waterlevel >= 100) {
            Intent intent = new Intent(this, Cheers.class);
            startActivity(intent);
            finish();
        }

    }


    // Auslösen der Animation (Wackeln) der Eiswürfel

    public void moveIceCubes(){

        Animation shake1 = AnimationUtils.loadAnimation(this, R.anim.iceshake1);
        ImageView ice1 = (ImageView) findViewById(R.id.ice1);
        ice1.startAnimation(shake1);

        Animation shake2 = AnimationUtils.loadAnimation(this, R.anim.iceshake2);
        ImageView ice2 = (ImageView) findViewById(R.id.ice2);
        ice2.startAnimation(shake2);

        Animation shake3 = AnimationUtils.loadAnimation(this, R.anim.iceshake3);
        ImageView ice3 = (ImageView) findViewById(R.id.ice3);
        ice3.startAnimation(shake3);

        Animation shake4 = AnimationUtils.loadAnimation(this, R.anim.iceshake4);
        ImageView ice4 = (ImageView) findViewById(R.id.ice4);
        ice4.startAnimation(shake4);

    }


    // Umwandlung der Sensorliste in eine Stringliste

    public void createListView(List<Sensor> listsensor){
        List<String> stringlist = new ArrayList<>();
        stringlist.add("\n\n\n");
        stringlist.add("My Sensors: ");

        for(int i=0; i<listsensor.size(); i++){
            stringlist.add(listsensor.get(i).getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.activity_list_item,
                android.R.id.text1, stringlist
        );

        ListView listView = (ListView)findViewById(R.id.Sensorliste);
        listView.setAdapter(adapter);

    }


    // Auslösen einer Animation, um Sensorliste anzuzeigen

    public void showSensorList() {
        Animation bottomUp = AnimationUtils.loadAnimation(this,
                R.anim.bottom_up);
        ViewGroup hiddenPanel = (ViewGroup) findViewById(R.id.Sensorliste);
        hiddenPanel.startAnimation(bottomUp);
        hiddenPanel.setVisibility(View.VISIBLE);
        listpresent = true;
    }


    // Auslösen einer Animation, um Sensorliste wieder zu verstecken

    public void hideSensorList(){
        Animation bottomDown = AnimationUtils.loadAnimation(this,
                R.anim.bottom_down);
        ViewGroup hiddenPanel = (ViewGroup) findViewById(R.id.Sensorliste);
        hiddenPanel.startAnimation(bottomDown);
        hiddenPanel.setVisibility(View.INVISIBLE);
        listpresent = false;
    }



    // Auslösen der Gradientenanimation des Hintergrunds

    public void gradientAnimation(){
        WaveView Pre = (WaveView) findViewById(R.id.wave_view);
        AnimationDrawable animationDrawable = (AnimationDrawable) Pre.getBackground();
        animationDrawable.setEnterFadeDuration(1000);
        animationDrawable.setExitFadeDuration(1500);
        animationDrawable.start();
    }



    // Bei der Arbeit mit Sensoren ist es wichtig die Lebenszeit der Activity zu beachten!
    // Damit nicht unnötig Akku verschwendet wird, bietet es sich an, den Listener während
    // Pausierung abzumelden und bei Rückkehr zur Activity wieder anzumelden:

    @Override
    protected void onPause() {
        super.onPause();
        // SensorListener abmelden
        if(initiateSensor())
            sm.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // SensorListener wieder anmelden
        if(initiateSensor())
            sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(initiateSensor())
            sm.unregisterListener(this);
    }


    @Override
    public void onBackPressed() {
        if(!listpresent){
            super.onBackPressed();
       }
        else
            hideSensorList();
    }

}
