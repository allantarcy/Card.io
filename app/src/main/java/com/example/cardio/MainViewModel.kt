package com.example.cardio

import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    val maxPlayer = 6
    private val _players: MutableList<Player> = mutableListOf()
    val players: MutableList<Player>
        get() = _players

    fun addPlayer(player: Player) {
        if (_players.size < maxPlayer) _players.add(player)
    }
}