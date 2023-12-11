package com.svovo.navigation

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
    init {
        this.authenticated = authenticated
        this.username = username
        this.email = email
        this.sessionKey = sessionKey
    }

    fun authenticateUser(){
        MainActivity.loggedIn = true
        if (MainActivity.userPreferences != null) {
            with (MainActivity.userPreferences!!.edit()){
                putString(MainActivity.USERNAME_PREF, username)
                putString(MainActivity.EMAIL_PREF, email)
                putString(MainActivity.SESSION_PREF, sessionKey)
                apply()
            }
        }
    }


    companion object{
        fun logout(){
            MainActivity.loggedIn = false
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