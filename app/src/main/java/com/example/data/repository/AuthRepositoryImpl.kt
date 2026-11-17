package com.example.data.repository

import com.example.domain.repository.AuthRepository
import com.example.domain.repository.AuthenticatedUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class AuthRepositoryImpl @Inject constructor(
  private val auth: FirebaseAuth,
) : AuthRepository {
  override val currentUser: Flow<AuthenticatedUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser?.toDomain())
    }
    auth.addAuthStateListener(listener)
    trySend(auth.currentUser?.toDomain())
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  override fun getCurrentUserSnapshot(): AuthenticatedUser? =
    auth.currentUser?.toDomain()

  private fun FirebaseUser.toDomain(): AuthenticatedUser = AuthenticatedUser(
    uid = uid,
    displayName = displayName?.takeIf { it.isNotBlank() } ?: "অতিথি খেলোয়াড়",
    email = email,
  )
}
