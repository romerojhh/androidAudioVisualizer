package com.example.apiexploration;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AudioFxDemo extends Activity {
    private static final String TAG = "AudioFxDemo";
    private static final float VISUALIZER_HEIGHT_DIP = 100f;
    private MediaPlayer mMediaPlayer;
    private Visualizer mVisualizer;
    private Equalizer mEqualizer;
    private LinearLayout mLinearLayout;
    private VisualizerView mVisualizerView;
    private TextView mStatusTextView;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mStatusTextView = new TextView(this);
        mLinearLayout = new LinearLayout(this);
        mLinearLayout.setOrientation(LinearLayout.VERTICAL);
        mLinearLayout.addView(mStatusTextView);
        setContentView(mLinearLayout);
        // Create the MediaPlayer
        startRecording();
        setupVisualizerFxAndUI();
        mStatusTextView.setText("Say something..");
    }

    private AudioRecord audioRecord;
    private int bufferSize;
    private BlockingQueue<byte[]> audioDataQueue;

    @SuppressLint("MissingPermission")
    public void startRecording() {
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_8BIT;
        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) - 1000;

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize);

        audioDataQueue = new LinkedBlockingQueue<byte[]>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[bufferSize];
                audioRecord.startRecording();
                while (!Thread.currentThread().isInterrupted()) {
                    int numBytes = audioRecord.read(buffer, 0, bufferSize);
                    byte[] data = new byte[numBytes];
                    System.arraycopy(buffer, 0, data, 0, numBytes);
                    try {
                        audioDataQueue.put(data);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                audioRecord.stop();
                audioRecord.release();
            }
        }).start();
    }

    public void stopRecording() {
        audioRecord.stop();
        audioRecord.release();
    }

    public byte[] getAudioData() throws InterruptedException {
        return audioDataQueue.take();
    }

    private void setupVisualizerFxAndUI() {
        // Create a VisualizerView (defined below), which will render the simplified audio
        // wave form to a Canvas.
        mVisualizerView = new VisualizerView(this);
        mVisualizerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                (int) (VISUALIZER_HEIGHT_DIP * getResources().getDisplayMetrics().density)));
        mLinearLayout.addView(mVisualizerView);

        Timer timer = new Timer();
        long interval = 15; // 1/20 second in milliseconds

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // This code will be executed every 1/20 second
                try {
                    mVisualizerView.updateVisualizer(getAudioData());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 0, interval);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing() && audioRecord != null) {
            stopRecording();
        }
    }
}

/**
 * A simple class that draws waveform data received from a
 * {@link Visualizer.OnDataCaptureListener#onWaveFormDataCapture }
 */
class VisualizerView extends View {
    private byte[] mBytes;
    private float[] mPoints;
    private final Rect mRect = new Rect();
    private final Paint mForePaint = new Paint();

    public VisualizerView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mBytes = null;
        mForePaint.setStrokeWidth(1f);
        mForePaint.setAntiAlias(true);
        mForePaint.setColor(Color.rgb(0, 128, 255));
    }

    public void updateVisualizer(byte[] bytes) {
        mBytes = bytes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBytes == null) {
            return;
        }
        if (mPoints == null || mPoints.length < mBytes.length * 4) {
            mPoints = new float[mBytes.length * 4];
        }
        mRect.set(0, 0, getWidth(), getHeight());
        for (int i = 0; i < mBytes.length - 1; i++) {
            mPoints[i * 4] = mRect.width() * i / (float)(mBytes.length - 1);
            mPoints[i * 4 + 1] = mRect.height() / (float)2
                    + ((byte) (mBytes[i] + 128)) * (mRect.height() / (float)2) / 128;
            mPoints[i * 4 + 2] = mRect.width() * (i + 1) / (float)(mBytes.length - 1);
            mPoints[i * 4 + 3] = mRect.height() / (float) 2
                    + ((byte) (mBytes[i + 1] + 128)) * (mRect.height() / (float) 2) / 128;
        }
        canvas.drawLines(mPoints, mForePaint);
    }
}