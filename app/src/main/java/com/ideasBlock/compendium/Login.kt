package com.ideasBlock.compendium

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.View.*
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import com.facebook.*
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ideasBlock.compendium.utils.CallAPI
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class Login :   AppCompatActivity(),
                CallAPI.ResponseAPI {

    private var callbackManager     :CallbackManager? = null
    private var userId              :String? = null
    private var userName            :String? = null
    private var userMail            :String? = null
    private var userDescription     :String? = null
    private var userPictureReference:String? = null
    private var cityName            :String? = "Vilnius"

    private var mLocalUser          :String? = null

    private lateinit var mNewObject :JSONObject

    // Used to pass to the Facebook API to request permissions
    private lateinit var mThisActivity :Activity

    // Vilnius, Lithuania.
    private var latitude:Double  = 54.6872
    private var longitude:Double = 25.2797

    // If in the process of logging in, for gui effects
    var logging :Boolean = false

    // Last known location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var mSharedPref : SharedPreferences

    /**
     * Overridden from parent class initialise all
     * @param savedInstanceState a Bundle with info from the state of the context
     */
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)

        //Remove notification bar
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // For Facebook permissions
        mThisActivity = this@Login

        //set content view AFTER ABOVE sequence (to avoid crash)
        setContentView(R.layout.activity_login)

        // Initialise Fuse Client for last known location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLastLocation()

        /*
            Facebook Login
         */
        callbackManager = CallbackManager.Factory.create()
        val accessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn  = accessToken != null && !accessToken.isExpired
        // TODO: if not causing no problem, delete following line...
//        FacebookSdk.sdkInitialize(this.getApplicationContext())
        logging = false
        if (isLoggedIn) {
            getDataFromGraphRequest(AccessToken.getCurrentAccessToken())
            login_button.visibility = INVISIBLE
            progressBar.visibility  = VISIBLE
            logging = true
        }
        //Setup the Button, does not execute nothing
        facebookButton()
    }


    override fun onResume() {
        super.onResume()

        // For the progress bar to show only when in the logging flow, but not when coming back to the page.
        // And instead show the logout button
        if(!logging)
        {
            login_button.visibility = VISIBLE
            progressBar.visibility  = INVISIBLE
        }
    }

    /**
     *  Setup callbacks for Facebook Login
     */
    private fun facebookButton()
    {
        val loginButton = findViewById<View>(R.id.login_button) as LoginButton
        // If using in a fragment, call loginButton.setFragment(this);

        // Callback registration
        loginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {

            override fun onSuccess(loginResult: LoginResult) {

                // App code:
                // This is the Facebook API
                loginButton.visibility = INVISIBLE
                progressBar.visibility = VISIBLE
                getDataFromGraphRequest(loginResult.accessToken)
                logging = true

                // TODO: Request for revision form facebook to be able to request this permissions
//                LoginManager.getInstance().logInWithPublishPermissions(
//                    mThisActivity,
//                    Arrays.asList("manage_pages"))
            }

            override fun onCancel() {
                // App code
            }

            override fun onError(exception: FacebookException) {
                // App code
            }
        })
    }

    /**
     * This is called when Facebook login from the button returns to the activity
     * //TODO: Revise
     * @param requestCode identifier int
     * @param resultCode identifier int
     * @param data An intent with data from previous activity
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager!!.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * The Graph API is the facebook API (https://developers.facebook.com/docs/graph-api/)
     * @param token An AccessToken obtained from the Facebook Login Button
     */
    fun getDataFromGraphRequest(token:AccessToken)
    {
        val request = GraphRequest.newMeRequest(token) { jsonObject, response ->
            try {

                if (jsonObject.has("name") )
                {
                    userName = jsonObject.getString("name")
                    userMail = if (jsonObject.has("mEmail"))  jsonObject.getString("mEmail") else "Email"

                    userPictureReference = "https://graph.facebook.com/"+ jsonObject.getString("id") +"/picture?type=normal"

                    mNewObject = JSONObject()
                    mNewObject.put("name",       userName)
                    mNewObject.put("email",      userMail)
                    mNewObject.put("picture",    userPictureReference)
                    mNewObject.put("latitude",   latitude.toString())
                    mNewObject.put("longitude",  longitude.toString())
                    mNewObject.put("city",       cityName)
                    mNewObject.put("picture",    userPictureReference)


                    mSharedPref = this.getSharedPreferences(getString(R.string.app_name_pref), Context.MODE_PRIVATE)
                    mLocalUser  = mSharedPref.getString("LOCAL_USER", "Nothing in LocalDatabase")

                    if (userName == mLocalUser)
                    {
                        with (mSharedPref.edit()) {
                            putString("LOCAL_USER", userName)
                            apply()
                        }
                        fetchSingleProfile(userName!!)
                    }

                    if (userName != null && userName != mLocalUser)
                    {
                        with (mSharedPref.edit()) {
                            putString("LOCAL_USER", userName)
                            apply()
                        }
                        // ================  API  CALL ==============
                        signUPToCompendium(mNewObject)
                        // ================  API  CALL ==============
                    }


                } else {
                    Log.e("FBLOGIN_FAILED", jsonObject.toString() + ":" + response)
                    Toast.makeText(this, R.string.signup_faceboook_error, Toast.LENGTH_SHORT).show()
                    progressBar.visibility = GONE
                    login_button.visibility = VISIBLE
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, R.string.signup_faceboook_error, Toast.LENGTH_SHORT).show()
                progressBar.visibility = GONE
                login_button.visibility = VISIBLE
            }
        }

        val parameters = Bundle()
        parameters.putString("fields", "name, email, picture")
        request.parameters = parameters
        request.executeAsync()


    }

    /**
     * API Call to Compendium to Sign up the user with Facebook credentials
     * @param newObject The JSONObject containing the credentials, and location
     */
    private fun signUPToCompendium(newObject:JSONObject)
    {
        Log.d("SIGN UP" ,"SIGN UP")
        // ================  API  CALL ==============
        val callPOST = CallAPI()
        callPOST.mResponseInterface = this
        callPOST.endPoint = "signUP"
        callPOST.execute(newObject)
        // ================  API  CALL ==============
    }


    /**
     * This interface receives information from the API Calls
     * @param endPoint The string of the current api request we are making
     * @param response This method both receives and requests info, this is the response it gets
     */
    override fun processResponse(endPoint: String, response: String)
    {

        try{
            if (endPoint == "signUP")
            {
                if (!response.contains("Error") || (!response.contains("error"))) {
                    with (mSharedPref.edit()) {
                        putString("LOCAL_USER", response)
                        apply()
                    }

                    // This is done to check if user is first time user
                    fetchSingleProfile(response)
                }
                else{
                    Toast.makeText(this, R.string.signup_error, Toast.LENGTH_SHORT).show()
                    progressBar.visibility = GONE
                    login_button.visibility = VISIBLE
                }
            }
            if (endPoint == "getSingleProfile")
            {

                val data = JSONArray(response)
                val userArray = JSONArray(data[0].toString())

                userId          = userArray[0] .toString()
                userName        = userArray[1] .toString()
                userMail        = userArray[3] .toString()
                userDescription = userArray[12].toString()

                val sharedPref = this.getSharedPreferences(getString(R.string.app_name_pref), Context.MODE_PRIVATE)
                with (sharedPref!!.edit()) {
                    putString("LOCAL_ID", userId)
                    apply()
                }

                if(userDescription != "Description"){
                    goHome()
                }
                else
                {
                    goToProfile()
                }
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, R.string.signup_error, Toast.LENGTH_SHORT).show()
            progressBar.visibility = GONE
            login_button.visibility = VISIBLE
        }
    }

    /**
     * Get a single user's profiles
     * @param FacebookName The facebook name of the user,
     * TODO Should use the userId
     */
    private fun fetchSingleProfile(FacebookName : String)
    {
        mNewObject = JSONObject()
        mNewObject.put("name",FacebookName )

        val callAPI = CallAPI()
        callAPI.mResponseInterface = this
        callAPI.endPoint = "getSingleProfile"
        callAPI.execute(mNewObject)
    }

    /**
     *    Get the last known location of the current user, consider permissions
     */
    private fun getLastLocation()
    {
        setupPermissions()
        enableServices()
    }

    /**
     *  Figure out whether we need permissions
     */
    private fun setupPermissions()
    {
        val permissionCoarse = ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION)

        val permissionFine = ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_FINE_LOCATION)

        if ((permissionCoarse != PackageManager.PERMISSION_GRANTED) && (permissionFine != PackageManager.PERMISSION_GRANTED)) {
            Log.i("PERMISSION", "Permission to record denied")
            makePermissionRequest()
        }
        else{

            // Actual Location request
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location : Location? ->
                    // Got last known location. In some rare situations this can be null.
                    if (location != null){
                        latitude  = location.latitude
                        longitude = location.longitude

                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                        cityName = addresses[0].locality

                    }
                }
        }
    }

    /**
     * If we need permissions we ask the user to provide them
     */
    private fun makePermissionRequest() {
        ActivityCompat.requestPermissions(this,
            arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
            0)
    }

    /**
     *   After the permission has been granted we update the location
     */
    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode:Int, permissions:Array<String>, grantResults:IntArray)
    {
        // Actual Location request
        if ((requestCode == 0) && (grantResults[0] == PERMISSION_GRANTED))
        {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location : Location? ->
                    // Got last known location. In some rare situations this can be null.

                    if (location != null){
                        latitude  = location.latitude
                        longitude = location.longitude
                        Log.d("COORDINATES: " , latitude.toString() + ":" + longitude.toString())
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                        cityName = addresses[0].locality
                    }
                }
        }
    }


    /**
     *  Enable GPS
     */
    private fun enableServices()
    {
        val lm = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gpsEnabled     = false
        var networkEnabled = false

        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
        }

        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
        }


        if (!gpsEnabled && !networkEnabled) {
            // notify user
            val dialog = AlertDialog.Builder(this)
            dialog.setMessage(this.resources.getString(R.string.gps_network_not_enabled))
            dialog.setPositiveButton(this.resources.getString(R.string.open_location_settings)
            ) { paramDialogInterface, paramInt ->
                // TODO Auto-generated method stub
                Log.d("DIALOG INTERFACE: " , paramDialogInterface.toString() + ":" + paramInt)
                val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                this.startActivity(myIntent)
                //get gps
            }
            dialog.setNegativeButton(getString(R.string.cancel)
            ) { paramDialogInterface, paramInt ->
                // TODO Auto-generated method stub
                Log.d("DIALOG INTERFACE: " , paramDialogInterface.toString() + ":" + paramInt)
            }
            dialog.show()
        }
    }

    /**
     * Intent to go to home page
     */
    private fun goHome()
    {
        logging = false
        val intent = Intent(this, UserHome::class.java)
        intent.putExtra("NAME",      userName)
        intent.putExtra("LATITUDE" , latitude.toString())
        intent.putExtra("LONGITUDE", longitude.toString())
        intent.putExtra("CITY", cityName)
        startActivity(intent)
    }

    /**
     * Intent to go to Profile
     */
    private fun goToProfile()
    {
        logging = false
        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra("ID",           userId)
        intent.putExtra("NAME",         userName)
        intent.putExtra("LATITUDE" ,    latitude.toString())
        intent.putExtra("LONGITUDE",    longitude.toString())
        intent.putExtra("EMAIL",        userMail)
        intent.putExtra("DESCRIPTION",  userDescription)
        intent.putExtra("EDITABLE" , true)
        intent.putExtra("CITY", cityName)

        startActivity(intent)
    }
}
