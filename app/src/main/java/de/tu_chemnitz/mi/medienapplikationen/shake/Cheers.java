package de.tu_chemnitz.mi.medienapplikationen.shake;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ImageView;
import android.os.Vibrator;
import com.john.waveview.WaveView;


public class Cheers extends Activity implements View.OnClickListener, SensorEventListener {

    public WaveView wave;

    public double starttime_activity;
    public boolean activity_started;
    public boolean ice_moving;

    public double lasttime;
    public double currtime;

    public SensorManager sm;
    public Sensor acc;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cheers);

        starttime_activity = System.currentTimeMillis();
        activity_started = false;
        ice_moving = false;

        Vibrator v = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(2000);

        wave = (WaveView) findViewById(R.id.wave_view);
        TextView Cheers = (TextView)findViewById(R.id.Cheers);
        Typeface besomfont = Typeface.createFromAsset(getAssets(), "Besom-free-font.ttf");
        Cheers.setTypeface(besomfont);

        ImageButton another_one = (ImageButton) findViewById(R.id.another_one);
        another_one.setOnClickListener(this);

        initiateSensor();
    }


    // Sensor initialisieren

    public void initiateSensor(){
        // Zugriff auf die Hardware: Erstellen eines Sensor-Managers
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if(sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!=null) {  // GEÄNDERT!

            // Sensortyp festlegen: Beschleunigungssensor
            acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            // Verknüpfung des Sensors mit Listener
            sm.registerListener(this, acc, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }


    // Callback-Methode des Sensor-Event-Listeners

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        // "Animation" pausieren, solange Vibration aktiv ist
        if (!activity_started) {
            currtime = lasttime = System.currentTimeMillis();

            if (currtime - starttime_activity > 2000)
                activity_started = true;
        }

        else {
            currtime = System.currentTimeMillis();
            // Update: alle 30ms, da unsere "Animation" von der Aktualisierungsgeschwindigkeit
            // abhängt und relativ flüssig sein sollte
            if ((currtime - lasttime >= 30) && activity_started)
                CalculateRot(sensorEvent);
                lasttime = currtime;
        }

    }


    // 2. Callback-Methode des SensorEvent-Listeners
    // (wir machen im Fall einer Messgenauigkeitsänderung gar nichts)

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    // Rotation berechnen

    public void CalculateRot(SensorEvent sensorEvent) {

        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];

        // Selbst wenn das Handy nun gekippt still gehalten wird, wirkt immer noch die Schwerkraft darauf
        // Diese verwenden wir zum Berechnen der Drehung

        // Hier geht es uns nicht um die Länge des Vektors, sondern um die Richtung
        // Wir normalisieren den Vektor also (dann hat er die Länge 1)
        double norm = Math.sqrt(x * x + y * y + z * z);

        x = (float) (x / norm);
        y = (float) (y / norm);
        z = (float) (z / norm);

        // Wenn Vektor die Länge 1 hat, lassen sich direkt Kreisfunktionen darauf anwenden

        // Berechnung der Neigung für den Fall, dass Device flach liegt
        float inclination = (float) Math.round(Math.toDegrees(Math.acos(z)));

        // Berechnung der Rotation
        float rotation = (float) Math.round(Math.toDegrees(Math.atan2(x, y)));

        rotateWave(inclination, rotation);

    }


    // Reaktionen auf die Änderung der Rotation

    public void rotateWave(float inclination, float rotation){
        // Gerät liegt flach: Reset (keine Rotation in x oder y-Richtung)
        if (inclination < 20 || inclination > 150) {
            wave.setRotation(0);
        }

        // Gerät liegt nicht flach: Welle rotieren entsprechend dem Kippwinkel
        else {
            // ab 90 Grad nicht mehr weiter rotieren
            if (rotation < 90 && rotation > -90) {
                // für die Animation mit Faktor <1 multipliziert, sonst Drehung zu stark
                wave.setRotation(rotation * 0.6f);

                if (!ice_moving) {
                    moveIceCubes();
                    ice_moving = true;
                }
            }
        }
    }


    // Auslösen der Animation (Floating) der Eiswürfel

    public void moveIceCubes(){
        ImageView ice1 = (ImageView)findViewById(R.id.ice1);
        AnimatorSet moveice1= (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.move0);
        moveice1.setTarget(ice1);

        ImageView ice2 = (ImageView)findViewById(R.id.ice2);
        AnimatorSet moveice2= (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.move1);
        moveice2.setTarget(ice2);

        ImageView ice3 = (ImageView)findViewById(R.id.ice3);
        AnimatorSet moveice3= (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.move2);
        moveice3.setTarget(ice3);

        ImageView ice4 = (ImageView)findViewById(R.id.ice4);
        AnimatorSet moveice4= (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.move3);
        moveice4.setTarget(ice4);

        moveice1.start();
        moveice2.start();
        moveice3.start();
        moveice4.start();
    }


    @Override
    protected void onPause() {
        super.onPause();
        // SensorListener abmelden
        sm.unregisterListener(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        // SensorListener wieder anmelden
        sm.registerListener(this, acc, SensorManager.SENSOR_DELAY_NORMAL);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        sm.unregisterListener(this);
    }


    @Override
    public void onClick(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

}
