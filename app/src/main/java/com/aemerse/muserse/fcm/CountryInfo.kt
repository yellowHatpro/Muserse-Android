package com.aemerse.muserse.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.aemerse.muserse.ApplicationClass
import com.aemerse.muserse.R
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CountryInfo : Thread(Runnable {
    var country: String = ApplicationClass.getPref().getString(ApplicationClass.Companion.getContext()
        .getString(R.string.pref_user_country), "")!!
    if (country == "") {
        Log.d("CountryInfo", "run: fetching country info ")
        val client = OkHttpClient()
        client.setConnectTimeout(10, TimeUnit.SECONDS)
        client.setReadTimeout(30, TimeUnit.SECONDS)
        val request = Request.Builder()
            .url("http://ip-api.com/json")
            .build()
        try {
            val response = client.newCall(request).execute()
            Log.d("CountryInfo", "run: $response")
            val jsonData = response.body().string()
            Log.d("CountryInfo", "run: $jsonData")
            val jObject = JSONObject(jsonData)
            country = jObject.getString("country")
            Log.d("CountryInfo", "run: $country")
        } catch (e: Exception) {
            country =
                ApplicationClass.Companion.getContext().resources.configuration.locale.country
            e.printStackTrace()
        }
        if (country == "") {
            country = "unknown"
        }
        try {
            country = country.replace(" ".toRegex(), "_")
            FirebaseMessaging.getInstance().subscribeToTopic(country)
            FirebaseMessaging.getInstance().subscribeToTopic("ab_music")

            /*FirebaseDatabase database = FirebaseDatabase.getInstance();
                     final DatabaseReference myRef = database.getReference("countries").child(country);

                     myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                         @Override
                         public void onDataChange(DataSnapshot dataSnapshot) {
                             try {
                                 if (dataSnapshot.getValue() == null) {
                                     myRef.setValue(1L);
                                 } else {
                                     myRef.setValue((Long) dataSnapshot.getValue() + 1L);
                                 }
                             } catch (Exception ignored) {
                             }
                         }

                         @Override
                         public void onCancelled(DatabaseError databaseError) {

                         }
                     });*/
        }
        catch (ignored: Exception) { }
        ApplicationClass.getPref().edit().putString(ApplicationClass.Companion.getContext().getString(R.string.pref_user_country), country).apply()
    }
})