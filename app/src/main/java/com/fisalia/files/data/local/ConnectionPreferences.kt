package com.fisalia.files.data.local

import android.content.Context
import com.fisalia.files.data.model.NetworkConnection
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ConnectionPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("fisalia_connections_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "saved_connections"

    fun getConnections(): List<NetworkConnection> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<NetworkConnection>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveConnection(connection: NetworkConnection) {
        val current = getConnections().toMutableList()
        val index = current.indexOfFirst { it.id == connection.id }
        if (index >= 0) {
            current[index] = connection
        } else {
            current.add(connection)
        }
        prefs.edit().putString(key, gson.toJson(current)).apply()
    }

    fun deleteConnection(connectionId: String) {
        val updated = getConnections().filterNot { it.id == connectionId }
        prefs.edit().putString(key, gson.toJson(updated)).apply()
    }

    fun getConnectionById(id: String): NetworkConnection? {
        return getConnections().find { it.id == id }
    }
}
