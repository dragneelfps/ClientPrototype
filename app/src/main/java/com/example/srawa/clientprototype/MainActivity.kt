package com.example.srawa.clientprototype

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.opengl.GLSurfaceView
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import okio.ByteString

class MainActivity : AppCompatActivity() {

    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLastLocation: Location? = null
    private var mTag = this.javaClass.simpleName
    private var REQ_CODE = 2345
    private val UPDATE_INTERVAL: Long = 10*1000
    private val FASTEST_INTERVAL: Long = 2000
    private var mLocationRequest: LocationRequest? = null
    private val URL = "ws://echo.websocket.org"
    private var client: OkHttpClient? = null
    private var ws: WebSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        client = OkHttpClient()
        start()

        send_loc.setOnClickListener {
            startLocationUpdates()
        }
    }

    inner private class EchoWebSocketListener: WebSocketListener(){
        override fun onOpen(webSocket: WebSocket?, response: Response?) {
            Log.i(mTag, "websocket opened succesfully")
        }

        override fun onMessage(webSocket: WebSocket?, text: String?) {
            if (text != null)
                output(text)
            else
                Log.i(mTag, "Null message received")
        }

        override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
            if (bytes != null)
                output(bytes?.hex())
            else
                Log.i(mTag, "Null bytes message received")
        }

        override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
            output("Connection to websocket can't be established")
        }
    }

    private fun start(){
        var request: Request = Request.Builder().url(URL).build()
        var listener = EchoWebSocketListener()
        ws = client?.newWebSocket(request, listener)
        client?.dispatcher()?.executorService()?.shutdown()
    }

    private fun output(txt: String) {
        runOnUiThread { response.text = "Recieved from server $txt" }
    }

    override fun onStop() {
        super.onStop()
        ws?.close(1000,"Goodbye !!")
    }

    @SuppressWarnings("MissingPermission")
    private fun startLocationUpdates(){
        mLocationRequest = LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(UPDATE_INTERVAL).setFastestInterval(FASTEST_INTERVAL)
        var builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        var locationSettingsRequest = builder.build()

        var settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingsRequest)

        mFusedLocationClient?.requestLocationUpdates(mLocationRequest, object: LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult?) {
                onLocationChanged(locationResult?.lastLocation)
            }
        }, Looper.myLooper())
    }
    private fun onLocationChanged(location: Location?){
        if(location != null){
            val msg = "${location.latitude} : ${location.longitude}"
//            response.text = msg
            Log.i(mTag, "Location changed to $msg")
            if(ws == null){
                Log.i(mTag, "Websocket is null")
            }else{
                ws?.send(msg)
            }

        }
    }



//    @SuppressWarnings("MissingPermission")
//    private fun getLoc(){
//        mFusedLocationClient?.lastLocation?.addOnCompleteListener{task ->
//            if(task.isSuccessful && task.result != null){
//                mLastLocation = task.result
//                val lat = mLastLocation!!.latitude
//                val long = mLastLocation!!.longitude
//                Log.i(mTag, "$lat : $long")
//            }
//        }
//    }

    override fun onStart() {
        super.onStart()
        if(!checkPermissions()){
            requestPermissions()
        }else{
            Log.i(mTag, "Good to go.")
        }
    }

    private fun checkPermissions(): Boolean {
        val permissionState: Int = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }
    private fun requestPermissions(){
        Log.i(mTag,"Requesting permissions")
        startLocationPermissionRequest()
    }
    private fun startLocationPermissionRequest(){
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),REQ_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.i(mTag, "onRequestPermissionsResult")
        if(requestCode == REQ_CODE){
            if(grantResults.size <= 0){
                Log.i(mTag, "User cancelled")
            }else if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                response.text = "Location permission acquired"
            }else{
                response.text = "Location permission denied. Rerun app."
            }
        }
    }
}
