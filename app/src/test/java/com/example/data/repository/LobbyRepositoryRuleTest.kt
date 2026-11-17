package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.remote.firestore.FirestoreLobbyDataSource
import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertThrows
import org.junit.Test

class LobbyRepositoryRuleTest : FirestoreEmulatorTestBase() {

  private fun createRepository(): MultiplayerLobbyRepositoryImpl {
    val dataSource = FirestoreLobbyDataSource(firestore, auth)
    return MultiplayerLobbyRepositoryImpl(dataSource, auth)
  }

  @Test
  fun ensurePlayerProfile_authenticatedOwner_createsAndObservesProfile() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = createRepository()

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensurePlayerProfile(uid, "অরিত্র")
    }
    assertThat(result.isSuccess).isTrue()

    val profile = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observePlayerProfile(uid).first { it != null }
    }
    assertThat(profile?.userId).isEqualTo(uid)
    assertThat(profile?.displayName).isEqualTo("অরিত্র")
  }

  @Test
  fun createAndObservePublicRoom_authenticatedUsers_succeeds() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = createRepository()

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createRoom(
        userId = aliceUid,
        hostName = "অরিত্র",
        title = "রবীন্দ্র সরোবর আসর",
        maxPlayers = 4,
        isPublic = true,
      )
    }
    assertThat(createResult.isSuccess).isTrue()
    val roomId = createResult.getOrThrow()

    val bobUid = signInTestUser(BOB_EMAIL)
    val bobRepo = createRepository()
    val rooms = withTimeout(FLOW_TIMEOUT_MS) {
      bobRepo.observePublicRooms().first { list -> list.any { it.id == roomId } }
    }
    assertThat(rooms.first { it.id == roomId }.title).isEqualTo("রবীন্দ্র সরোবর আসর")

    val joinResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.joinRoom(roomId, bobUid)
    }
    assertThat(joinResult.isSuccess).isTrue()
  }

  @Test
  fun deleteRoom_nonHostUser_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = createRepository()
    val roomId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createRoom(
        userId = aliceUid,
        hostName = "অরিত্র",
        title = "ব্যক্তিগত আসর",
        maxPlayers = 4,
        isPublic = true,
      ).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = createRepository()
    val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.deleteRoom(roomId)
    }
    assertThat(deleteResult.isFailure).isTrue()
    val error = deleteResult.exceptionOrNull()?.unwrapFirestoreException()
    assertThat(error?.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
  }

  @Test
  fun observePublicRooms_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = createRepository()

    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observePublicRooms().first()
      }
    } catch (e: Exception) {
      caught = e
    }
    val firestoreException = checkNotNull(caught?.unwrapFirestoreException()) {
      "Expected nested FirebaseFirestoreException, got $caught"
    }
    assertThat(firestoreException.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
  }

  @Test
  fun readCrossUserProfile_authenticatedBob_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = createRepository()
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.ensurePlayerProfile(aliceUid, "অরিত্র").getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val exception = assertThrows(Exception::class.java) {
      runBlocking {
        withTimeout(DEFAULT_TIMEOUT_MS) {
          firestore.collection("users").document(aliceUid).get().await()
        }
      }
    }
    val firestoreException = checkNotNull(exception.unwrapFirestoreException()) {
      "Expected FirebaseFirestoreException, got $exception"
    }
    assertThat(firestoreException.code).isEqualTo(FirebaseFirestoreException.Code.PERMISSION_DENIED)
  }

  private companion object {
    const val ALICE_EMAIL = "alice_cards@test.com"
    const val BOB_EMAIL = "bob_cards@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
