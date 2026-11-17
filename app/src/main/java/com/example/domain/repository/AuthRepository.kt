package com.example.domain.repository

import kotlinx.coroutines.flow.Flow

data class AuthenticatedUser(
  val uid: String,
  val displayName: String,
  val email: String?,
)

interface AuthRepository {
  val currentUser: Flow<AuthenticatedUser?>

  fun getCurrentUserSnapshot(): AuthenticatedUser?
}
