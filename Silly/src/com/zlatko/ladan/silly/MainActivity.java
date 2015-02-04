package com.zlatko.ladan.silly;

import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;

public class MainActivity extends ActionBarActivity implements
		SensorEventListener, OnItemSelectedListener {
	private static final int SAMPLE_RATE = 22050;
	private static final int DURATION = 100; // milliseconds
	private static final int SAMPLES = DURATION * SAMPLE_RATE / 1000;

	Handler m_handler = new Handler();
	private SensorManager m_sensorManager;
	private Sensor m_sensor = null;
	private float m_y;
	private final Object m_lock = new Object();
	private boolean m_stop = false;
	private AudioType m_audioType = AudioType.Sine;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Spinner spinner = (Spinner) findViewById(R.id.spinnerAudioType);
		spinner.setOnItemSelectedListener(this);
		m_sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		if (m_sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
			m_sensor = m_sensorManager
					.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		}
	}

	private static float Round(float a_value, int a_decimal) {
		float p = (float) Math.pow(10, a_decimal);
		a_value = a_value * p;
		return (float) (Math.round(a_value) / p);
	}

	private enum AudioType {
		Sine, Square, SawTooth
	}

	@Override
	protected void onResume() {
		super.onResume();
		m_sensorManager.registerListener(this, m_sensor,
				SensorManager.SENSOR_DELAY_NORMAL);
		setStop(false);
		// Use a new tread as this can take a while
		(new Thread() {
			public void run() {
				final double sample[] = new double[SAMPLES];
				final byte generatedSound[] = new byte[2 * SAMPLES];
				AudioTrack audioTrack = new AudioTrack(
						AudioManager.STREAM_MUSIC, SAMPLE_RATE,
						AudioFormat.CHANNEL_OUT_MONO,
						AudioFormat.ENCODING_PCM_16BIT, generatedSound.length,
						AudioTrack.MODE_STREAM);

				audioTrack.play();
				float y = 0;
				int idx = 0;
				short val = 0;
				// AudioType audioType = AudioType.Sine;
				AudioType audioType;

				while (!getStop()) {
					// fill out the array

					y = Round(getY(), 0);
					y = 440.0f + (y + 10.0f) * 110.0f;
					audioType = getAudioType();

					if (audioType == AudioType.Sine) {
						for (int i = 0; i < SAMPLES; ++i) {
							sample[i] = Math.sin((2.0f * Math.PI * i * y)
									/ SAMPLE_RATE);
						}
					} else if (audioType == AudioType.Square) {
						for (int i = 0; i < SAMPLES; ++i) {
							sample[i] = Math.sin((2.0f * Math.PI * i * y)
									/ SAMPLE_RATE);
							if (sample[i] > 0.0) {
								sample[i] = Byte.MAX_VALUE;
								continue;
							}

							if (sample[i] < 0.0) {
								sample[i] = Byte.MIN_VALUE;
							}
						}
					} else {
						for (int i = 0; i < SAMPLES; ++i) {
							sample[i] = 2 * (i % (SAMPLE_RATE / y))
									/ (SAMPLE_RATE / y) - 1;
						}
					}

					// convert to 16 bit pcm sound array
					// assumes the sample buffer is normalised.
					idx = 0;
					for (final double dVal : sample) {
						// scale to maximum amplitude
						val = (short) ((dVal * 32767));
						// in 16 bit wav PCM, first byte is the low order byte
						generatedSound[idx++] = (byte) (val & 0x00ff);
						generatedSound[idx++] = (byte) ((val & 0xff00) >>> 8);

					}

					audioTrack.write(generatedSound, 0, generatedSound.length);
				}
				audioTrack.stop();
				audioTrack.release();
			}
		}).start();
	}

	private synchronized boolean getStop() {
		return m_stop;
	}

	private synchronized void setStop(boolean a_value) {
		m_stop = a_value;
	}

	private synchronized AudioType getAudioType() {
		return m_audioType;
	}

	private synchronized void setAudioType(AudioType a_value) {
		m_audioType = a_value;
	}

	@Override
	protected void onPause() {
		super.onPause();
		m_sensorManager.unregisterListener(this);
		setStop(true);
	}

	private float getY() {
		synchronized (m_lock) {
			return m_y;
		}
	}

	@Override
	public void onSensorChanged(SensorEvent a_e) {
		synchronized (m_lock) {
			m_y = a_e.values[1];
		}
	}

	@Override
	public void onAccuracyChanged(Sensor a_sensor, int a_accuracy) {

	}

	@Override
	public void onItemSelected(AdapterView<?> a_parent, View a_view,
			int a_position, long id) {
		switch (a_position) {
		case 0:
			this.setAudioType(AudioType.Sine);
			break;

		case 1:
			this.setAudioType(AudioType.Square);
			break;

		default:
			this.setAudioType(AudioType.SawTooth);
			break;
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> a_parent) {
		this.setAudioType(AudioType.Sine);
	}
}
