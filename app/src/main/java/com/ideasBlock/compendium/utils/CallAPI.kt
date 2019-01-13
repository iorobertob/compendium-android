package com.ideasBlock.compendium.utils

import android.os.AsyncTask
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

class CallAPI : AsyncTask<JSONObject, JSONObject, String>()  {

    // The end point in the server to connect to
    var endPoint:String? = null

    // Placeholder for the url 
    private var urlString:String? = null

    /*
     * Interface to send data to the main thread
    */
    var mResponseInterface: ResponseAPI? = null

    /**
     * Interface to exchange data with the ain UserHome thread
     */
    interface ResponseAPI {
        fun processResponse(endPoint: String, response:String)
    }

    private          var mPrivateKey    :String = Credentials.mPrivateKey
    private lateinit var mUserId        :String
    private lateinit var mUserName      :String
    private lateinit var mUserMail      :String
    private lateinit var mDescription   :String
    private lateinit var mPicture       :String
    private lateinit var mLatitude      :String
    private lateinit var mLongitude     :String
    private lateinit var mCity          :String


    /**
     * Do different GET or POST API requests depending on the endPoint that is specified. Runs in a background thread.
     * @param params A variable argument of type JSONObject that contains different data depending on the endpoint
     * @return a String with the result of the server API request
     */
    override fun doInBackground(vararg params: JSONObject): String {

        when (endPoint) {
            "signUP" -> {

                urlString = "https://compendium.ideas-block.com/signUp"

                mUserName             = params[0].getString("name")
                mUserMail             = params[0].getString("email")
                mPicture              = params[0].getString("picture")
                mLatitude             = params[0].getString("latitude")
                mLongitude            = params[0].getString("longitude")
                mCity                 = params[0].getString("city")

                var reqParam =    URLEncoder.encode("key",           "UTF-8") + "=" + URLEncoder.encode(mPrivateKey,    "UTF-8")
                reqParam += "&" + URLEncoder.encode("name",          "UTF-8") + "=" + URLEncoder.encode(mUserName,      "UTF-8")
                reqParam += "&" + URLEncoder.encode("email"  ,       "UTF-8") + "=" + URLEncoder.encode(mUserMail,      "UTF-8")
                reqParam += "&" + URLEncoder.encode("password"  ,    "UTF-8") + "=" + URLEncoder.encode("password", "UTF-8")
                reqParam += "&" + URLEncoder.encode("description"  , "UTF-8") + "=" + URLEncoder.encode("Description","UTF-8")
                reqParam += "&" + URLEncoder.encode("latitude"  ,    "UTF-8") + "=" + URLEncoder.encode(mLatitude,      "UTF-8")
                reqParam += "&" + URLEncoder.encode("longitude"  ,   "UTF-8") + "=" + URLEncoder.encode(mLongitude,     "UTF-8")
                reqParam += "&" + URLEncoder.encode("city"  ,        "UTF-8") + "=" + URLEncoder.encode(mCity,          "UTF-8")
                reqParam += "&" + URLEncoder.encode("picture"  ,     "UTF-8") + "=" + URLEncoder.encode(mPicture,       "UTF-8")

                return sendPostRequest(reqParam, URL(urlString))
            }
            "getProfiles" -> {
                urlString = "https://compendium.ideas-block.com/getProfiles"
                return sendGetRequest("Not Used", "Not Used", urlString!!)
            }
            "getSingleProfile" -> {
                urlString = "https://compendium.ideas-block.com/getProfile"
                val name = params[0].getString("name")
                return sendGetRequest(name, "Not Used", urlString!!)
            }
            "updateProfile" -> {
                urlString = "https://compendium.ideas-block.com/updateProfile"

                mUserId      = params[0].getString("userId")
                mUserName    = params[0].getString("name")
                mUserMail    = params[0].getString("email")
                mDescription = params[0].getString("description")
                mLatitude    = params[0].getString("latitude")
                mLongitude   = params[0].getString("longitude")
                mCity        = params[0].getString("city")

                var reqParam =    URLEncoder.encode("key",         "UTF-8") + "=" + URLEncoder.encode(mPrivateKey, "UTF-8")
                reqParam += "&" + URLEncoder.encode("user_id",     "UTF-8") + "=" + URLEncoder.encode(mUserId, "UTF-8")
                reqParam += "&" + URLEncoder.encode("name",        "UTF-8") + "=" + URLEncoder.encode(mUserName, "UTF-8")
                reqParam += "&" + URLEncoder.encode("email",       "UTF-8") + "=" + URLEncoder.encode(mUserMail, "UTF-8")
                reqParam += "&" + URLEncoder.encode("description", "UTF-8") + "=" + URLEncoder.encode(mDescription, "UTF-8")
                reqParam += "&" + URLEncoder.encode("latitude",    "UTF-8") + "=" + URLEncoder.encode(mLatitude, "UTF-8")
                reqParam += "&" + URLEncoder.encode("longitude",   "UTF-8") + "=" + URLEncoder.encode(mLongitude, "UTF-8")
                reqParam += "&" + URLEncoder.encode("city",        "UTF-8") + "=" + URLEncoder.encode(mCity, "UTF-8")

                val response = sendPostRequest(reqParam, URL(urlString))
                Log.d("RESPONSE RETURN", response)
                return response
            }
            "generalSearch" -> {
                urlString = "https://compendium.ideas-block.com/generalSearch"
                val query = params[0].getString("query")
                return sendGetRequest(query, null, urlString!!)
            }
            else -> return "Error do In Background"
        }
    }

    /**
     * This runs in the main thread again, when the background task has finished and provided the result
     * @param restult A string with the result of the background task
     */
    override fun onPostExecute(result: String?)
    {
        Log.d("RESPONSE TO $endPoint: ", result)
        mResponseInterface!!.processResponse(endPoint!!,result!!)
        super.onPostExecute(result)
    }

    /**
     * POST https request
     * @param params A string with the formatted arguments for the post request
     * @param mURL An URL object with the address of the server
     * @return A String with the result of the https request
     */
    private fun sendPostRequest(params:String, mURL:URL):String {

        val response = StringBuffer()

        val urlConnection : HttpsURLConnection = mURL.openConnection() as HttpsURLConnection
        urlConnection.sslSocketFactory = MySSLSocketFactory(urlConnection.sslSocketFactory)
        with (urlConnection){
            // optional default is GET
            requestMethod = "POST"

            val wr = OutputStreamWriter(outputStream)
            wr.write(params)
            wr.flush()

            println("URL : $url")
            println("Response Code : $responseCode")

            BufferedReader(InputStreamReader(inputStream)).use {

                var inputLine = it.readLine()
                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = it.readLine()
                }
                it.close()
                println("Response : $response")
            }
        }
        return response.toString()
    }

    /**
     * GET https request
     * @param param1 used to build the url query path
     * @param param2 used to build the url query path
     * @param urlString The string of the url where to make the call to
     * @return A String with the result of the https request
     */

    private fun sendGetRequest(param1:String?, param2: String?, urlString:String):String {

        try {

            if(param2 != null)
            {
                Log.d("PARAM2: " , param2)
            }

            val mURL:URL
            mURL = when (endPoint) {
                "getSingleProfile" -> {
                    val reqParam = URLEncoder.encode("name", "UTF-8") + "=" + URLEncoder.encode(param1, "UTF-8")
                    val newUrl = "$urlString?$reqParam"
                    URL(newUrl)
                }
                "generalSearch" -> {
                    val reqParam = URLEncoder.encode("query", "UTF-8") + "=" + URLEncoder.encode(param1, "UTF-8")
                    val newUrl = "$urlString?$reqParam"
                    URL(newUrl)
                }
                else -> URL(urlString)
            }


            val urlConnection : HttpsURLConnection = mURL.openConnection() as HttpsURLConnection
            urlConnection.sslSocketFactory = MySSLSocketFactory(urlConnection.sslSocketFactory)
            with (urlConnection){

                // optional default is GET
                requestMethod = "GET"

                println("URL : $url")
                println("Response Code : $responseCode")

                BufferedReader(InputStreamReader(inputStream)).use {
                    val response = StringBuffer()

                    var inputLine = it.readLine()
                    while (inputLine != null) {
                        response.append(inputLine)
                        inputLine = it.readLine()
                    }
                    it.close()
                    println("Response : $response")

                    return JSONArray(response.toString()).toString()
                }
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            return e.toString()
//                dismissDialogLogin()
        }
    }

}