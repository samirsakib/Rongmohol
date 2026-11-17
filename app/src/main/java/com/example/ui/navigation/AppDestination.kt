package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppDestination {
  @Serializable
  data object HomeRoute : AppDestination

  @Serializable
  data object GameModeRoute : AppDestination

  @Serializable
  data class PlayerSelectionRoute(val modeName: String) : AppDestination

  @Serializable
  data class GameBoardRoute(
    val modeName: String,
    val playerCount: Int,
    val playerName: String,
    val avatarId: Int,
    val difficultyName: String = "MEDIUM",
  ) : AppDestination

  @Serializable
  data class ResultRoute(
    val modeName: String,
    val playerName: String,
    val avatarId: Int,
    val playerCount: Int,
    val isVictory: Boolean,
    val playerScore: Int,
    val opponentScore: Int,
  ) : AppDestination

  @Serializable
  data object LobbyRoute : AppDestination

  @Serializable
  data class OnlineRoomRoute(val roomId: String) : AppDestination

  @Serializable
  data object ProfileRoute : AppDestination

  @Serializable
  data object RulesRoute : AppDestination

  @Serializable
  data object SettingsRoute : AppDestination
}
