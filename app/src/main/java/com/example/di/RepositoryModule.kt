package com.example.di

import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.MatchHistoryRepositoryImpl
import com.example.data.repository.MultiplayerLobbyRepositoryImpl
import com.example.data.repository.OnlineMultiplayerRepositoryImpl
import com.example.data.repository.PreferencesRepositoryImpl
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.MatchHistoryRepository
import com.example.domain.repository.MultiplayerLobbyRepository
import com.example.domain.repository.OnlineMultiplayerRepository
import com.example.domain.repository.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
  @Binds
  @Singleton
  abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

  @Binds
  @Singleton
  abstract fun bindMatchHistoryRepository(impl: MatchHistoryRepositoryImpl): MatchHistoryRepository

  @Binds
  @Singleton
  abstract fun bindMultiplayerLobbyRepository(
    impl: MultiplayerLobbyRepositoryImpl
  ): MultiplayerLobbyRepository

  @Binds
  @Singleton
  abstract fun bindOnlineMultiplayerRepository(
    impl: OnlineMultiplayerRepositoryImpl
  ): OnlineMultiplayerRepository

  @Binds
  @Singleton
  abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
