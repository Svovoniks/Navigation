package com.svovo.navigation

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject
import java.io.File

class LoginServer {
    private val rootURL: String = "https://gradually-quality-mole.ngrok-free.app"
    private val loginURL: String = "login/"
    private val registerURL: String = "register/"
    private val requestQueue: RequestQueue

    init {
        val cache = DiskBasedCache(File("cache"), 1024 * 1024)
        val network = BasicNetwork(HurlStack())
        val requestQueue = RequestQueue(cache, network).apply {
            start()
        }
        this.requestQueue = requestQueue;
    }
    fun login(username: String, password: String, onResponse: (User) -> Unit, onError: (VolleyError) -> Unit) {
        val url = listOf(rootURL, loginURL).joinToString(separator = "/")
        val requestJSON = JSONObject()
        requestJSON.put("username", username)
        requestJSON.put("password", password)

        val request = JsonObjectRequest(Request.Method.POST, url, requestJSON,
            {response -> onResponse(parseUser(response))},
            {error -> onError(error)}
        )
        requestQueue.add(request)
    }

    fun register(email: String, username: String, password: String, onResponse: (User) -> Unit, onError: (VolleyError) -> Unit){
        val url = listOf(rootURL, registerURL).joinToString(separator = "/")
        val requestJSON = JSONObject()
        requestJSON.put("username", username)
        requestJSON.put("password", password)
        requestJSON.put("email", email)

        val request = JsonObjectRequest(Request.Method.POST, url, requestJSON,
            {response -> onResponse(parseUser(response))},
            {error -> onError(error)}
        )
        requestQueue.add(request)
    }

    private fun parseUser(jsonObject: JSONObject): User{
        return User(
            jsonObject.getBoolean("authenticated"),
            jsonObject.getString("name"),
            jsonObject.getString("email"),
            jsonObject.getString("session_key"),
        )
    }
}
