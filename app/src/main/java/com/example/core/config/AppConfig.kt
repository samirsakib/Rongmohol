package com.example.core.config

import android.content.Context
import com.example.R

object AppConfig {
  const val DEFAULT_MAX_PLAYERS = 4
  const val MIN_PLAYERS = 2
  const val MAX_ROOM_TITLE_LENGTH = 60
  const val ROOM_CODE_LENGTH = 6
  const val PREFERENCES_DATASTORE_NAME = "bangla_card_game_prefs"
  const val ROOM_DATABASE_NAME = "bangla_card_game.db"

  fun configurableAppName(context: Context): String = context.getString(R.string.app_name)
}
