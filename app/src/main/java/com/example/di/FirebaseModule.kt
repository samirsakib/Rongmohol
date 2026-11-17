package com.example.di

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
  @Provides
  @Singleton
  fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

  @Provides
  @Singleton
  fun provideFirebaseFirestore(@ApplicationContext context: Context): FirebaseFirestore {
    val databaseId = context.getString(R.string.firestore_database_id)
    return FirebaseFirestore.getInstance(databaseId)
  }

  @Provides
  @Singleton
  fun provideFirebaseDatabase(@ApplicationContext context: Context): FirebaseDatabase {
    val rtdbUrl = context.getString(R.string.firebase_rtdb_url)
    return FirebaseDatabase.getInstance(rtdbUrl).apply {
      runCatching { setPersistenceEnabled(false) }
    }
  }

  @Provides
  @Singleton
  fun provideFirebaseFunctions(): FirebaseFunctions = Firebase.functions
}
