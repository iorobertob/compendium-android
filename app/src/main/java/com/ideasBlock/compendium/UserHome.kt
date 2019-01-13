package com.ideasBlock.compendium

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.ideasBlock.compendium.utils.CallAPI
import com.ideasBlock.compendium.utils.SnapHelperOneByOne
import kotlinx.android.synthetic.main.activity_user_home.*
import org.json.JSONArray
import org.json.JSONObject

//TODO: Add Update method
//TODO: Add Menu via Drawer view
//TODO: Add log out flow
//TODO: Enable upload of images (service?)
//TODO: Enrich profile
//TODO: When coming back from profile, display the same user with the same query that was enabled before

class UserHome :    AppCompatActivity(),
                    OnMapReadyCallback,
                    CallAPI.ResponseAPI,
                    SnapHelperOneByOne.PositionUpdate,
                    CardsAdapter.ListItemClickListener,
                    GoogleMap.OnMarkerClickListener {

    // This Activity's map
    private lateinit var mMap: GoogleMap

    private lateinit var mUserID        :String
    private lateinit var mLocalID       :String
    private lateinit var mLocalUser     :String
    private lateinit var mName          :String
    private lateinit var mMail          :String
    private lateinit var mCity          :String
    private lateinit var mDescription   :String
    private lateinit var mSearchString  :String

    private var latitude:Double  = 54.6872  // Vilnius
    private var longitude:Double = 25.2797  // Vilnius

    private var mEditable = false

    private var mPosition   :Int   = 0

    // Initial zoom level
    private var mZoom       :Float = 13f


    /* Recycler View */
    private lateinit var mCardsList: RecyclerView

    // Data to be handed to the RecycleAdapter
    var data:JSONArray       = JSONArray()

    // RecycleAdapter
    private val adapter:CardsAdapter = CardsAdapter(this, this)


    /**
     * Override Android's function to initialise all
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_home)

        mZoom = 13f

        val sharedPref = this.getSharedPreferences(getString(R.string.app_name_pref), Context.MODE_PRIVATE)

        if (sharedPref.contains("LOCAL_USER"))
        {
            mLocalUser = sharedPref.getString("LOCAL_USER", null)!!
        }
        if (sharedPref.contains("LOCAL_ID"))
        {
            mLocalID = sharedPref.getString("LOCAL_ID", null)!!
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fetchData()

        initGUI()
    }

    /**
     * Overridden Android Activity lifecycle method
     */
    override fun onResume() {
        super.onResume()
        if(mEditable)
        {
            search()
        }
    }

    /**
     *  This interface receives clicks on the items
     *  @param clickedItemIndex An Int on the position of the item in the adapter data where the click happened
     */
    override fun onListItemClick(clickedItemIndex: Int) {
        positionUpdate(clickedItemIndex)
        goToProfile()
    }

    /**
     * Intent to go to the profile Activity
     */
    private fun goToProfile()
    {
        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra("ID",           mUserID)
        intent.putExtra("NAME",         mName)
        intent.putExtra("LATITUDE" ,    latitude.toString())
        intent.putExtra("LONGITUDE",    longitude.toString())
        intent.putExtra("EMAIL",        mMail)
        intent.putExtra("DESCRIPTION",  mDescription)
        intent.putExtra("CITY",         mCity)


        mEditable = if (mUserID == mLocalID) {
            intent.putExtra("EDITABLE" , true)
            true
        } else {
            intent.putExtra("EDITABLE" , false)
            false
        }
        startActivity(intent)
    }

    /**
     * Get profiles data from the server
     */
    private fun fetchData()
    {
        val callAPI = CallAPI()
        callAPI.endPoint = "getProfiles"
        callAPI.mResponseInterface = this
        callAPI.execute(null)
    }

    /**
     * Starts all GUI resources
     */
    private fun initGUI()
    {
        mName       = intent.getStringExtra("NAME")
        latitude    = intent.getStringExtra("LATITUDE").toDouble()
        longitude   = intent.getStringExtra("LONGITUDE").toDouble()
        mCity       = intent.getStringExtra("CITY")


        mCardsList = findViewById(R.id.rv_cards)

        mCardsList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false )

        mCardsList.adapter = adapter

        mCardsList.onFlingListener = null

        val snap = SnapHelperOneByOne()
        snap.mPositionInterface = this
        snap.attachToRecyclerView(mCardsList)
    }

    /**
     *  This interface receives information from the API Calls
     *  @param endPoint The server endpoint that we are receiving the response from
     *  @param response The actual response we are getting from the most recent API call to the server
     */
    override fun processResponse(endPoint: String, response: String)
    {
        progress_bar_home.visibility = GONE
        try{
            if ((endPoint == "getProfiles") || ( endPoint  == "generalSearch"))
            {
                data = JSONArray(response)
                adapter.swapJSON(data)
                initGUI()

                mMap.clear()
                initMarkers()

                if (mEditable)
                {
                    mZoom = mMap.cameraPosition.zoom

                    for (i in 0..(data.length()-1))
                    {
                        if( JSONArray(data[i].toString())[1] == mLocalUser )
                        {
                            positionUpdate(i)
                            mCardsList.layoutManager!!.scrollToPosition(i)
                        }
                    }
                }
                else
                {
                    positionUpdate(0)
                }
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, R.string.get_data_error, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * This interface receives the position change in the recycle view
     * @param position the index number of the JSON array where we are at the moment
     */
    override fun positionUpdate(position: Int)
    {
        mPosition = position

        val userArray = JSONArray(data[position].toString())

        latitude  = userArray[7].toString().toDouble()
        longitude = userArray[8].toString().toDouble()

        mUserID  = userArray[0].toString()
        mName    = userArray[1].toString()
        mMail    = userArray[3].toString()
        mDescription = userArray[12].toString()

        goToLocation(latitude, longitude)
    }


    /**
     * Manipulates the map once available.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     * @param googleMap the map that is ready to use
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker and move the camera
        val location = LatLng(latitude,longitude)

        initMarkers()

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, mZoom))
        mMap.setOnMarkerClickListener(this)
    }

    /**
     * Initialises and creates all markers from the current query on the map
     */
    private fun initMarkers()
    {
        for (i in 0..(data.length()-1))
        {
            val userArray = JSONArray(data[i].toString())
            val thisName      = userArray[1].toString()
            val thisLatitude  = userArray[7].toString().toDouble()
            val thisLongitude = userArray[8].toString().toDouble()
            val thisLocation  = LatLng(thisLatitude,thisLongitude)
            mMap.addMarker(MarkerOptions().position(thisLocation).title(thisName))
        }
    }

    /**
     * Override google maps function to receive clicks on markers and focus the camera on such user
     * and display his/her card
     * @param marker the marker that was pressed
     * @returns Boolean false if default behaviour is commanded after this
     */
    override fun onMarkerClick(marker: Marker?): Boolean {

        val title = marker?.title.toString()

        mZoom = mMap.cameraPosition.zoom

        for (i in 0..(data.length()-1))
        {
            if( JSONArray(data[i].toString())[1] == title )
            {
                positionUpdate(i)
                mCardsList.layoutManager!!.scrollToPosition(i)
                return false
            }
        }
        return false
    }

    /**
     * Change focus of current map's camera
     * @param latitude a double for the current location
     * @param longitude a double for the current location
     */
    private fun goToLocation(latitude:Double, longitude:Double)
    {
        // Add a marker in coordinates and move the camera
        val newLocation = LatLng(latitude,longitude)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newLocation, mZoom))
    }

    /*
     * Not implemented yet
     */
//    fun logout(view: View) {
//        LoginManager.getInstance().logOut()
//        goLoginScreen()
//    }
//
//    /*
//     * Not implemented yet
//     */
//    private fun goLoginScreen()
//    {
//        val intent = Intent(this, Login::class.java)
//        startActivity(intent)
//    }

    /**
     * Perform a search query on the server with whatever is on the search text edit view
     */
    private fun search ()
    {
        mSearchString = et_search.text.toString()

        mZoom = 13f

        val newObject  = JSONObject()

        if(mSearchString.isEmpty())
        {
            newObject.put("query", "ALL")
        }else{
            newObject.put("query", mSearchString)
        }

        val callAPI = CallAPI()
        callAPI.endPoint = "generalSearch"
        callAPI.mResponseInterface = this
        callAPI.execute(newObject)

        progress_bar_home.visibility = VISIBLE

        et_search.hideKeyboard()
    }

    /**
     * Do General search
     * @param view The view that was just clicked
     */
    fun searchClick(view:View)
    {
        if (view.id == R.id.search_button){
            search()
        }

    }

    /**
     * Hide the soft keyboard
     */
    private fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

}
