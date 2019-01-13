package com.ideasBlock.compendium

import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.ideasBlock.compendium.utils.CallAPI
import kotlinx.android.synthetic.main.activity_profile.*
import org.json.JSONObject
import java.util.*


/**
 * A login screen that offers login via mEmail/password.
 */
class ProfileActivity : AppCompatActivity(),
                        OnMapReadyCallback,
                        CallAPI.ResponseAPI{

    // This Activity's map
    private lateinit var mMap: GoogleMap

    private lateinit var mUserID        :String
    private lateinit var mUserName      :String
    private lateinit var mEmail         :String
    private lateinit var mDescription   :String
    private lateinit var mCity          :String
    private          var mEditable      :Boolean = false

    // Vilnius
    private var latitude:Double  = 54.6872
    private var longitude:Double = 25.2797

    /**
     * Android's activity lifecycle override method
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        // Set up the login form.
        populateAutoComplete()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Android's lifecycle method override, to save update if the app looses focus
     */
    override fun onPause() {
        super.onPause()
        if (mEditable)
        {
            Toast.makeText(this, R.string.updating_message, Toast.LENGTH_SHORT).show()
            updateProfile()
        }

    }

    /**
     * Hardware back button handling, it does the same as the update button
     */
    override fun onBackPressed() {
        super.onBackPressed()

        if (mEditable)
        {
            Toast.makeText(this, R.string.updating_message, Toast.LENGTH_SHORT).show()
            updateProfile()

        }

    }

    /**
     * Not implemented
     */
    override fun processResponse(endPoint: String, response: String) {
        //TODO("not implemented")
    }

    /**
     *  Google maps' own callback for when the current map has been loaded.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker and move the camera
        val location = LatLng(latitude,longitude)

        if (!mEditable)
        {
            markerview.visibility = GONE
            mMap.addMarker(MarkerOptions().position(location).title(mUserName))
        }
        else
        {
            markerview.visibility = VISIBLE
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 13f))
    }


    /**
     * Get data from the home page of the user that we are displaying the profile for and populate the page
     */
    private fun populateAutoComplete(){

        mUserID         = intent.getStringExtra("ID")
        mUserName       = intent.getStringExtra("NAME")
        mEmail          = intent.getStringExtra("EMAIL")
        mDescription    = intent.getStringExtra("DESCRIPTION")
        latitude        = intent.getStringExtra("LATITUDE") .toDouble()
        longitude       = intent.getStringExtra("LONGITUDE").toDouble()
        mCity           = intent.getStringExtra("CITY")
        mEditable       = intent.getBooleanExtra("EDITABLE", false)

        et_name.setText(mUserName)
        et_email.setText(mEmail)
        et_description.setText(mDescription)

        if (!intent.getBooleanExtra("EDITABLE", false))
        {
            et_name.isEnabled           = false
            et_email.isEnabled          = false
            et_description.isEnabled    = false
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
            email_sign_in_button.visibility = GONE
        }
        else
        {
            et_name.isEnabled           = true
            et_email.isEnabled          = true
            et_description.isEnabled    = true
            email_sign_in_button.visibility = VISIBLE
        }
    }

    /**
     * The button in the profile page starts an update call to the server
     */
    fun updateButtonClick(v:View)
    {
        if (v.id == R.id.email_sign_in_button){
            updateProfile()
            Toast.makeText(this, R.string.updating_message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Prepare the data to do a POST request to the server and update the user's data
     */
    private fun updateProfile()
    {
        val centerMap = mMap.cameraPosition.target

        latitude  = centerMap.latitude
        longitude = centerMap.longitude

        try{
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            mCity = addresses[0].locality
        }
        catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, R.string.error_geocoder, Toast.LENGTH_SHORT).show()
        }



        mDescription = et_description.text.toString()

        mUserName = et_name.text.toString()

        mEmail = et_email.text.toString()

        if (isEmailValid(mEmail))
        {
            updateRequest()
            goHome()
        }
        else
        {
            Toast.makeText(this, R.string.error_invalid_email, Toast.LENGTH_SHORT).show()
        }

    }

    /**
     * This is the API call to update the users data
     */
    private fun updateRequest()
    {
        try {
            val newObject = JSONObject()
            newObject.put("userId",     mUserID)
            newObject.put("name",       mUserName)
            newObject.put("email",      mEmail)
            newObject.put("description",mDescription)
            newObject.put("latitude",   latitude.toString())
            newObject.put("longitude",  longitude.toString())
            newObject.put("city",       mCity)

            // ================  API  CALL ==============
            val callPOST = CallAPI()
            callPOST.endPoint = "updateProfile"
            callPOST.mResponseInterface = this
            callPOST.execute(newObject)
            // ================  API  CALL ==============
        }
        catch (e: Exception) {
            e.printStackTrace()
//                dismissDialogLogin()
        }
        goHome()
    }


    /**
     * Intent to go to the home page
     */
    private fun goHome()
    {
        val intent = Intent(this, UserHome::class.java)
        intent.putExtra("NAME",     mUserName)
        intent.putExtra("LATITUDE" ,latitude.toString())
        intent.putExtra("LONGITUDE",longitude.toString())
        intent.putExtra("EDITABLE", mEditable)
        intent.putExtra("CITY", mCity)

        startActivity(intent)
    }


    /**
     * Check if the email string contains an '@'
     */
    private fun isEmailValid(email: String): Boolean {
        //TODO: Replace this with your own logic
        return email.contains("@")
    }
}
