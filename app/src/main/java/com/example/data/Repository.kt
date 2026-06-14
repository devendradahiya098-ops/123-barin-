package com.example.data

import kotlinx.coroutines.flow.Flow

class ArenaRepository(private val dao: ArenaDao) {
    val userStats: Flow<UserStats?> = dao.getUserStats()
    val gameHistory: Flow<List<GameHistory>> = dao.getAllHistory()
    val shopItems: Flow<List<ShopItem>> = dao.getAllShopItems()
    val friends: Flow<List<Friend>> = dao.getAllFriends()

    suspend fun getUserStatsSync(): UserStats? = dao.getUserStatsSync()

    suspend fun saveUserStats(stats: UserStats) = dao.insertUserStats(stats)

    suspend fun updateUserStats(stats: UserStats) = dao.updateUserStats(stats)

    suspend fun addGameHistory(item: GameHistory) = dao.insertHistory(item)

    suspend fun updateShopItem(item: ShopItem) = dao.updateShopItem(item)

    suspend fun populateInitialData(
        startingStats: UserStats,
        startingShop: List<ShopItem>,
        startingFriends: List<Friend>
    ) {
        if (dao.getUserStatsSync() == null) {
            dao.insertUserStats(startingStats)
            dao.insertShopItems(startingShop)
            dao.insertFriends(startingFriends)
        }
    }

    suspend fun purchaseShopItem(item: ShopItem, stats: UserStats): Boolean {
        if (stats.gems >= item.price && !item.isOwned) {
            val updatedItem = item.copy(isOwned = true)
            val updatedStats = stats.copy(gems = stats.gems - item.price)
            dao.updateShopItem(updatedItem)
            dao.updateUserStats(updatedStats)
            return true
        }
        return false
    }

    suspend fun equipShopItem(item: ShopItem, stats: UserStats) {
        if (!item.isOwned) return
        if (item.type == "AVATAR") {
            dao.unequipAllAvatars()
            val updatedItem = item.copy(isEquipped = true)
            dao.updateShopItem(updatedItem)
            dao.updateUserStats(stats.copy(avatar = item.value))
        } else if (item.type == "TITLE") {
            dao.unequipAllTitles()
            val updatedItem = item.copy(isEquipped = true)
            dao.updateShopItem(updatedItem)
            dao.updateUserStats(stats.copy(rankTitle = item.value))
        }
    }
}
