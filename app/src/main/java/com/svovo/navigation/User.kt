package com.svovo.navigation

import org.osmdroid.util.GeoPoint

class User(
    authenticated: Boolean,
    username: String,
    email: String,
    sessionKey: String
) {
    var authenticated: Boolean
    var username: String
    var email: String
    var sessionKey: String
    var trails: ArrayList<Trail>
    init {
        this.authenticated = authenticated
        this.username = username
        this.email = email
        this.sessionKey = sessionKey
        this.trails = ArrayList()
        fetchTrails {}
    }

    fun authenticateUser(){
        MainActivity.loggedIn = true
        MainActivity.user = this
        fetchTrails {  }
        if (MainActivity.userPreferences != null) {
            with (MainActivity.userPreferences!!.edit()){
                putString(MainActivity.USERNAME_PREF, username)
                putString(MainActivity.EMAIL_PREF, email)
                putString(MainActivity.SESSION_PREF, sessionKey)
                apply()
            }
        }
    }

    fun fetchTrails(onFinish: () -> Unit) {
        UserTrailsServer().getTrails(this.sessionKey, {list -> this.trails = list; onFinish()}, {})
    }

    fun addTrail(trail: Trail){
        trails.add(trail)
        UserTrailsServer().addTrail(sessionKey, trail, {}, {})
    }


    companion object{
        fun logout(){
            MainActivity.loggedIn = false
            MainActivity.user = null
            if (MainActivity.userPreferences != null) {
                with (MainActivity.userPreferences!!.edit()){
                    remove(MainActivity.USERNAME_PREF)
                    remove(MainActivity.EMAIL_PREF)
                    remove(MainActivity.SESSION_PREF)
                    apply()
                }
            }
        }
    }
}

data class Trail(
    val id: Long,
    val name: String,
    val points: List<GeoPoint>
)