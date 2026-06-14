package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1, // Single player row
    val name: String = "Alex_Nova",
    val rankTitle: String = "Genius",
    val gems: Int = 1240,
    val xp: Int = 450,
    val level: Int = 1,
    val avatar: String = "👨‍🔬",
    val matchesWon: Int = 1,
    val totalMatches: Int = 3,
    val questProgress: Int = 1 // 1 out of 3 math duels won for Daily Quest
) {
    val xpNeeded: Int
        get() = level * 1000
}

@Entity(tableName = "game_history")
data class GameHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val modeName: String,
    val resultRank: String, // "1st", "2nd", "3rd", "4th", "Victory", "Defeat", etc.
    val xpGained: Int,
    val gemsGained: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "shop_items")
data class ShopItem(
    @PrimaryKey val itemId: String, // e.g. "avatar_telepath"
    val type: String, // "AVATAR" or "TITLE"
    val name: String,
    val value: String, // e.g. "🧠" or "Quantum Seer"
    val price: Int,
    val isOwned: Boolean = false,
    val isEquipped: Boolean = false
)

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val avatarIcon: String,
    val rank: String,
    val status: String, // "ONLINE", "OFFLINE", "THINKING"
    val winRatio: String // e.g., "72%"
)
