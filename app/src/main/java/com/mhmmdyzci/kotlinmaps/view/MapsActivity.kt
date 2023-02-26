package com.mhmmdyzci.kotlinmaps.view

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.mhmmdyzci.kotlinmaps.R
import com.mhmmdyzci.kotlinmaps.databinding.ActivityMapsBinding
import com.mhmmdyzci.kotlinmaps.model.Place
import com.mhmmdyzci.kotlinmaps.roomdb.PlaceDao
import com.mhmmdyzci.kotlinmaps.roomdb.PlaceDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.schedulers.Schedulers.io

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,GoogleMap.OnMapLongClickListener {
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    private lateinit var sharedPreferences: SharedPreferences
    private  var trackBoolean: Boolean? = null
    private val TAG_TRACK : String = "trackBoolean"
    private var selectedLatitude : Double? = null
    private var selectedLongitude : Double? = null
    private lateinit var db : PlaceDatabase
    private lateinit var placeDao : PlaceDao
    //obje Disposable: Kullan-at
    val compositeDisposable = CompositeDisposable()
    var placeFromMain : Place? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        registerLauncher()
        sharedPreferences = this.getSharedPreferences("com.mhmmdyzci.kotlinmaps", MODE_PRIVATE)
        trackBoolean = false
        selectedLatitude =  0.0
        selectedLongitude = 0.0
        db = Room.databaseBuilder(applicationContext, PlaceDatabase::class.java, "Places").build()
        placeDao = db.placeDao()
        binding.saveButton.isEnabled = false


    }

    // onMapReady harita hazır olduğunda çağırılan fonksiyon
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        //uzun tıklamayı ekliyor
        mMap.setOnMapLongClickListener(this)

        val intent = intent
        val info = intent.getStringExtra("info")
        if(info.equals("new")){

            binding.saveButton.visibility = View.VISIBLE
            binding.deleteButton.visibility = View.GONE
            //casting
            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
            locationListener = object : LocationListener {
                override fun onLocationChanged(currentLocation: Location) {
                    trackBoolean = sharedPreferences.getBoolean(TAG_TRACK,false)
                    if(!trackBoolean!!){
                        val userLocation =  LatLng(currentLocation.latitude,currentLocation.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15f))
                        sharedPreferences.edit().putBoolean(TAG_TRACK,true).apply()
                    }
                }
            }
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){

                if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                    // Bu kod bloğu çalışırsa izni bir kere reddetmiş niye izin istediğini detaylı anlat
                    Snackbar.make(binding.root,"Permission needed for location",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission"){
                        //izin isteme
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

                    }.show()
                }else{
                    // izin iste
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }else{
                //izin verilmiş
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if(lastLocation != null){
                    val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15f))

                }
                mMap.isMyLocationEnabled = true

            }

        }else{
            mMap.clear()
            placeFromMain = intent.getSerializableExtra("selectedPlace") as? Place
            placeFromMain?.let {
                val latLng = LatLng(it.latitude,it.longitude)
                mMap.addMarker(MarkerOptions().position(latLng).title(it.name))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,16f))
                binding.placeNameText.setText(it.name)
                binding.saveButton.visibility = View.GONE
                binding.deleteButton.visibility = View.VISIBLE
            }
        }

    }
    private fun registerLauncher(){
       permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
           if(result){
               if(ContextCompat.checkSelfPermission(this@MapsActivity,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                   locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
                   val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                   if(lastLocation != null){
                       val lastUserLocation = LatLng(lastLocation.latitude,lastLocation.longitude)
                       mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15f))
                   }
                   mMap.isMyLocationEnabled = true

               }
           }else{
               Toast.makeText(this@MapsActivity,"Permission needed",Toast.LENGTH_LONG).show()
           }
       }
    }
    override fun onMapLongClick(p0: LatLng) {
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(p0))
        selectedLongitude = p0.longitude
        selectedLatitude = p0.latitude
        binding.saveButton.isEnabled = true
    }
    fun save (view: View){
        if (selectedLatitude != null && selectedLongitude != null ){
            val place = Place(binding.placeNameText.text.toString(),selectedLatitude!!,selectedLongitude!!)
            compositeDisposable.add(
                placeDao.insert(place)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponse)
            )
        }
    }
    private fun handleResponse(){
        val intent = Intent(this@MapsActivity,MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)

    }
    fun delete(view: View){
        compositeDisposable.add(
            placeDao.delete(placeFromMain as Place)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse)
        )

    }
    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }


}

