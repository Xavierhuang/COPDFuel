package com.copdhealthtracker.utils

import android.app.Application
import com.copdhealthtracker.auth.CopdAuth
import com.copdhealthtracker.api.CopdApiClient
import com.copdhealthtracker.data.AppDatabase
import com.copdhealthtracker.repository.DataRepository

class AppApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { DataRepository(database) }
    val copdAuth by lazy { CopdAuth(this) }
    val apiClient by lazy { CopdApiClient() }
    
    companion object {
        @Volatile
        private var instance: AppApplication? = null
        
        fun getInstance(): AppApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
