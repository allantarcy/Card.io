package com.example.cardio

data class Player(
    val name: String,
    var balance: Int,
    val tag : String


) {
    override fun toString(): String {
        return "$name: Solde= $balance, Tag= $tag"
    }
}