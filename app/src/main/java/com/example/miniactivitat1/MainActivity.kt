package com.example.miniactivitat1

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.miniactivitat1.ui.theme.MiniActivitat1Theme
import kotlin.math.abs


class MainActivity : ComponentActivity(), SensorEventListener {
    lateinit var sensorManager: SensorManager
    private val colorState = mutableStateOf(Color.Green)
    var lastUpdate: Long = 0
    var lastRange: Float = 0f
    private var logLight = mutableListOf<String>()
    private lateinit var logListState: LazyListState

    private val logSaver = Saver<SnapshotStateList<String>,String>(
        save = {
            it.toString()
        },
        restore = {
            SnapshotStateList()
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        //getting the sensors
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        //getSensor may be null -> check it before registering listeners
        if (accelerometer != null)
            sensorManager.registerListener(
                this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL)
        if (lightSensor != null)
            sensorManager.registerListener(
                this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
                SensorManager.SENSOR_DELAY_NORMAL)

        lastUpdate = System.currentTimeMillis()
        super.onCreate(savedInstanceState)
        //Screen objects:
        setContent {
            MiniActivitat1Theme {
                //First, we'll initialize all things we need for light sensor & screen drawing
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                val firstLog = if (lightSensor != null)
                    "There is a light sensor!\nMaximum Range: ${lightSensor.maximumRange} lxs"
                else
                    "Sorry, there is no light sensor"
                logLight = rememberSaveable(saver = logSaver) {
                    mutableStateListOf(firstLog)
                }
                logListState = rememberLazyListState()

                //Then, we set up all objects on our screen
                Column {
                    //Top part: color changing when shuffle
                    ColorChangingSurface(colorState.value, screenHeight)

                    //Middle part: accelerometer data
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenHeight / 4),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (accelerometer != null) {
                            Text(
                                text = buildString {
                                    appendLine("Accelerometer Sensor Capabilities:")
                                    appendLine("Name: ${accelerometer.name}")
                                    appendLine("Vendor: ${accelerometer.vendor}")
                                    appendLine("Version: ${accelerometer.version}")
                                    appendLine("Power: ${accelerometer.power} mA")
                                    appendLine("Resolution: ${accelerometer.resolution} m/s^2")
                                    appendLine("Maximum Range: ${accelerometer.maximumRange} m/s^2")
                                }
                            )
                        } else {
                            Text("Sorry, there is no accelerometer")
                        }
                    }

                    //Bottom part: light sensor updates
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenHeight / 2)
                            .background(Color.Yellow),
                        state = logListState
                    )
                    {
                        itemsIndexed(logLight) { _, log ->
                            Text(
                                text = log,
                            )
                        }
                    }

                }
            }
        }
    }

    //This method should do the least possible work, therefore it only diferentiates which sensor
    //sent the info
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) getAccelerometer(event)
        else if (event.sensor.type == Sensor.TYPE_LIGHT) getLight(event, event.sensor.maximumRange)
    }

    //With physical devices, the maximumRange reading works weird...
    private fun getLight(event: SensorEvent, range: Float) {
        val values = event.values
        val top = range*2/3
        val bot = range/3
        val actualTime = System.currentTimeMillis()

        //Not enough change -> do nothing
        if (actualTime - lastUpdate < 1000 || abs(values[0] - lastRange) < 200) return
        //Enough change -> check intensity and append message
        if (values[0] >= top){
            logLight.add("New value light sensor = ${values[0]}\nHIGH intensity")
        }else if (values[0] >= bot){
            logLight.add("New value light sensor = ${values[0]}\nMEDIUM intensity")
        } else{
            logLight.add("New value light sensor = ${values[0]}\nLOW intensity")
        }
        lastUpdate = actualTime
        lastRange = values[0]
    }

    //This method is not needed for this assignment
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
    }

    //When leaving the app -> unregister listeners to not waste battery
    override fun onPause() {
        // unregister listener
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    //When returning to the app -> register listeners again
    override fun onResume() {
        // re-register listener
        super.onResume()
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        if (accelerometer != null)
            sensorManager.registerListener(
                this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL)
        if (lightSensor != null)
            sensorManager.registerListener(
                this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
                SensorManager.SENSOR_DELAY_NORMAL)
    }

    //Modified: managing the changing color
    private fun getAccelerometer(event: SensorEvent) {
        val values = event.values
        // Movement
        val x = values[0]
        val y = values[1]
        val z = values[2]
        val accelerationSquareRoot = (x * x + y * y + z * z
                / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH))
        val actualTime = System.currentTimeMillis()
        if (accelerationSquareRoot >= 200) {
            if (actualTime - lastUpdate < 1000) {
                return
            }
            lastUpdate = actualTime
            Toast.makeText(this, "Device was shuffed", Toast.LENGTH_SHORT).show()

            //with this line we can get the color to change on the screen
            colorState.value = if (colorState.value == Color.Green) Color.Red else Color.Green
        }
    }
}

//Element created to fulfill the first part
@Composable
fun ColorChangingSurface(color: Color, screenHeight: Dp) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(screenHeight / 4),
        color = color
    ) {}
}