package com.example.apiexploration

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.example.apiexploration.ui.theme.ApiexplorationTheme
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {
    //Create placeholder for user's consent to record_audio permission.
    //This will be used in handling callback
    private val MY_PERMISSIONS_RECORD_AUDIO = 1

    private val VISUALIZER_HEIGHT_DIP = 50f
    private var mVisualizer: Visualizer? = null
    private var mLinearLayout: LinearLayout? = null
    private var mVisualizerView: VisualizerView? = null
    private var mStatusTextView: TextView? = null
    private var mMediaPlayer: AudioRecord? = null
    private var bufferSizeInBytes: Int = 0
    private var isActive: Boolean = true
    private val job = Job()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupVisualizerFxAndUI()
        checkRecordAudioPermission()
//        setContent {
//            ApiexplorationTheme {
//                // A surface container using the 'background' color from the theme
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colors.background
//                ) {
//                    Greeting("Android")
//                }
//            }
//        }


        mStatusTextView = TextView(this)
        mLinearLayout = LinearLayout(this)
        mLinearLayout!!.orientation = LinearLayout.VERTICAL
        mLinearLayout!!.addView(mStatusTextView)
        setContentView(mLinearLayout)

        //mVisualizer!!.enabled = true
        // When the stream ends, we don't need to collect any more data. We don't do this in
        // setupVisualizerFxAndUI because we likely want to have more, non-Visualizer related code
        // in this callback.
        mStatusTextView!!.text = "Playing audio..."

        startAudioCapture()
    }

    suspend fun recordAudio(audioRecord: AudioRecord): ByteArray = withContext(Dispatchers.IO + job) {
        val buffer = ByteArray(audioRecord.bufferSizeInFrames)
        val result = ByteArrayOutputStream()
        audioRecord.startRecording()
        while (isActive) {
            System.out.println("asdfasfafasdfasdf")
            val readSize = audioRecord.read(buffer, 0, buffer.size)
            result.write(buffer, 0, readSize)
        }
        audioRecord.stop()
        return@withContext result.toByteArray()
    }

    private fun setupVisualizerFxAndUI() {
        // Create a VisualizerView (defined below), which will render the simplified audio
        // wave form to a Canvas.
        mVisualizerView = VisualizerView(this)
        mVisualizerView!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.FILL_PARENT,
            (this.VISUALIZER_HEIGHT_DIP * resources.displayMetrics.density).toInt()
        )
        mLinearLayout?.addView(mVisualizerView)
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            mVisualizer?.release()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAudioCapture() {
        val audioSource = MediaRecorder.AudioSource.MIC
        val sampleRateInHz = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_8BIT
        val bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)

        val audioRecord = AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes)


        val buffer = ByteArray(bufferSizeInBytes)

        val scope = CoroutineScope(Dispatchers.IO + job)


        scope.launch {
            // Do something with the recorded audio data
            audioRecord.startRecording()
            while (isActive) {
                val readSize = audioRecord.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)

                System.out.println(buffer.contentToString())
                mVisualizerView?.updateVisualizer(buffer.sliceArray(0 until readSize))
            }
            audioRecord.stop()
        }
    }


    private fun checkRecordAudioPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission has not been granted yet, request it from the user
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MY_PERMISSIONS_RECORD_AUDIO
            )
        } else {
            // Permission has already been granted, proceed with audio capture
            startAudioCapture()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted, proceed with audio capture
                startAudioCapture()
            } else {
                // Permission has been denied, show a message to the user or handle the error
                Toast.makeText(this, "Audio capture permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

}



@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ApiexplorationTheme {
        Greeting("Android")
    }
}