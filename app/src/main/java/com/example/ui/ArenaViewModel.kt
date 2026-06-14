package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

// Active Screen View Enum
enum class Screen {
    HOME, FRIENDS, LOCKER, RANKS, SHOP
}

// Active Game Mode Enum
enum class GameMode {
    NONE, BRAIN_RACE, NUMBER_DUEL, SURVIVAL
}

// Brain Race Duel State
enum class RaceState {
    MATCHING, READY, ACTIVE, FINISHED
}

// Question Structure
data class Question(
    val query: String,
    val options: List<String>,
    val correctIdx: Int
)

// Opponent Participant progress state
data class Participant(
    val id: String,
    val name: String,
    val avatar: String,
    val solvedCount: Int,
    val speedFactor: Long, // Tick time in ms per question solved
    val isFinished: Boolean = false
)

class ArenaViewModel(private val repository: ArenaRepository) : ViewModel() {

    // Main App States
    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen = _currentScreen.asStateFlow()

    private val _activeMode = MutableStateFlow(GameMode.NONE)
    val activeMode = _activeMode.asStateFlow()

    // Persistent States from Room db
    val userStats = repository.userStats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val gameHistory = repository.gameHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val shopItems = repository.shopItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val friends = repository.friends.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Matchmaking & Interactive Arena Gameplay States (Brain Race)
    private val _raceState = MutableStateFlow(RaceState.MATCHING)
    val raceState = _raceState.asStateFlow()

    private val _opponents = MutableStateFlow<List<Participant>>(emptyList())
    val opponents = _opponents.asStateFlow()

    private val _userSolvedCount = MutableStateFlow(0)
    val userSolvedCount = _userSolvedCount.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex = _currentQuestionIndex.asStateFlow()

    private val _currentQuestion = MutableStateFlow<Question?>(null)
    val currentQuestion = _currentQuestion.asStateFlow()

    private val _raceTimerMs = MutableStateFlow(0L)
    val raceTimerMs = _raceTimerMs.asStateFlow()

    private val _isUserPenalized = MutableStateFlow(false)
    val isUserPenalized = _isUserPenalized.asStateFlow()

    private val _opponentJob = MutableStateFlow<Job?>(null)
    private val _timerJob = MutableStateFlow<Job?>(null)
    private val _survivalTimerJob = MutableStateFlow<Job?>(null)

    // Game Mode Results parameters
    private val _userFinalPlacement = MutableStateFlow(1) // 1st, 2nd, 3rd, 4th
    val userFinalPlacement = _userFinalPlacement.asStateFlow()

    private val _gemsReward = MutableStateFlow(0)
    val gemsReward = _gemsReward.asStateFlow()

    private val _xpReward = MutableStateFlow(0)
    val xpReward = _xpReward.asStateFlow()

    // Number Duel Mode (123 Challenge) States
    private val _targetSum = MutableStateFlow(0)
    val targetSum = _targetSum.asStateFlow()

    private val _gridNumbers = MutableStateFlow<List<Int>>(emptyList())
    val gridNumbers = _gridNumbers.asStateFlow()

    private val _selectedGridIndices = MutableStateFlow<Set<Int>>(emptySet())
    val selectedGridIndices = _selectedGridIndices.asStateFlow()

    private val _duelBotScore = MutableStateFlow(0)
    val duelBotScore = _duelBotScore.asStateFlow()

    private val _duelUserScore = MutableStateFlow(0)
    val duelUserScore = _duelUserScore.asStateFlow()

    private val _duelTimeRemainingSec = MutableStateFlow(10)
    val duelTimeRemainingSec = _duelTimeRemainingSec.asStateFlow()

    private val _duelStatus = MutableStateFlow("") // "MATCHING", "PLAYING", "OVER"
    val duelStatus = _duelStatus.asStateFlow()

    // Survival Mode States
    private val _survivalEquation = MutableStateFlow("")
    val survivalEquation = _survivalEquation.asStateFlow()

    private val _survivalIsCorrect = MutableStateFlow(false)
    val survivalIsCorrect = _survivalIsCorrect.asStateFlow()

    private val _survivalStreak = MutableStateFlow(0)
    val survivalStreak = _survivalStreak.asStateFlow()

    private val _survivalTimeRemainingSec = MutableStateFlow(4)
    val survivalTimeRemainingSec = _survivalTimeRemainingSec.asStateFlow()

    private val _survivalStatus = MutableStateFlow("") // "PLAYING", "OVER"
    val survivalStatus = _survivalStatus.asStateFlow()

    // Current Feedback Toast message support
    private val _toastMsg = MutableStateFlow("")
    val toastMsg = _toastMsg.asStateFlow()

    // Question store representing procedural generation or standard index list
    private var activeQuestionList = mutableListOf<Question>()

    init {
        // Build initial mock tables if db empty. This fills our Arena environment.
        viewModelScope.launch {
            val dbUser = UserStats()
            val schoolInventory = listOf(
                ShopItem("avatar_science", "AVATAR", "Default Chemist", "👨‍🔬", 0, isOwned = true, isEquipped = true),
                ShopItem("avatar_telepath", "AVATAR", "Telekinetic Sage", "🧠", 400),
                ShopItem("avatar_cybermind", "AVATAR", "Cyborg Synthesizer", "🤖", 600),
                ShopItem("avatar_overlord", "AVATAR", "Anomalous Overlord", "🛸", 1000),
                ShopItem("avatar_sage", "AVATAR", "Venerable Owl", "🦉", 350),
                ShopItem("title_genius", "TITLE", "Default Genius", "Genius", 0, isOwned = true, isEquipped = true),
                ShopItem("title_seer", "TITLE", "Quantum Seer", "Quantum Seer", 500),
                ShopItem("title_god", "TITLE", "Calculus God", "Calculus God", 800),
                ShopItem("title_megamind", "TITLE", "Mega-Mind", "Mega-Mind", 250)
            )

            val initialFriends = listOf(
                Friend(0, "Quantum_Mind", "🛸", "Ascended", "THINKING", "89%"),
                Friend(0, "Nova_Bot", "🤖", "Expert", "ONLINE", "61%"),
                Friend(0, "Cerebral_X", "🧠", "Grandmaster", "THINKING", "81%"),
                Friend(0, "Zero_Cool", "🕶️", "Rookie", "OFFLINE", "44%"),
                Friend(0, "Brainiac_99", "🦉", "Master", "ONLINE", "74%")
            )

            repository.populateInitialData(dbUser, schoolInventory, initialFriends)
        }
    }

    // Navigation and screen routing
    fun setScreen(screen: Screen) {
        _currentScreen.value = screen
    }

    fun startMode(mode: GameMode) {
        // Cancel any pending animations or game scopes
        cancelAllJobs()
        _activeMode.value = mode

        when (mode) {
            GameMode.BRAIN_RACE -> {
                initiateBrainRace()
            }
            GameMode.NUMBER_DUEL -> {
                initiateNumberDuel()
            }
            GameMode.SURVIVAL -> {
                initiateSurvival()
            }
            else -> {}
        }
    }

    fun exitMode() {
        cancelAllJobs()
        _activeMode.value = GameMode.NONE
    }

    private fun cancelAllJobs() {
        _opponentJob.value?.cancel()
        _timerJob.value?.cancel()
        _survivalTimerJob.value?.cancel()
        _opponentJob.value = null
        _timerJob.value = null
        _survivalTimerJob.value = null
    }

    fun triggerToast(msg: String) {
        viewModelScope.launch {
            _toastMsg.value = msg
            delay(2000)
            if (_toastMsg.value == msg) {
                _toastMsg.value = ""
            }
        }
    }

    // ============================================
    // PROCEDURAL QUESTION BUILDER
    // ============================================
    private fun createProceduralQuestion(): Question {
        val opModes = listOf("+", "-", "*", "/")
        val chosenOp = opModes.random()
        var left = 0
        var right = 0
        var correctVal = 0

        when (chosenOp) {
            "+" -> {
                left = Random.nextInt(10, 99)
                right = Random.nextInt(10, 99)
                correctVal = left + right
            }
            "-" -> {
                left = Random.nextInt(20, 150)
                right = Random.nextInt(10, left)
                correctVal = left - right
            }
            "*" -> {
                left = Random.nextInt(3, 15)
                right = Random.nextInt(3, 12)
                correctVal = left * right
            }
            "/" -> {
                right = Random.nextInt(3, 10)
                correctVal = Random.nextInt(4, 15)
                left = right * correctVal
            }
        }

        val options = mutableSetOf<String>()
        options.add(correctVal.toString())
        while (options.size < 4) {
            val variance = Random.nextInt(-10, 10)
            val fakeVal = correctVal + variance
            if (fakeVal > 0 && fakeVal != correctVal) {
                options.add(fakeVal.toString())
            }
        }

        val shuffled = options.toList().shuffled()
        return Question(
            query = "$left $chosenOp $right = ?",
            options = shuffled,
            correctIdx = shuffled.indexOf(correctVal.toString())
        )
    }

    private fun generateSequenceQuestion(): Question {
        val type = Random.nextInt(0, 3)
        var sequence = ""
        var correctVal = 0

        when (type) {
            0 -> { // Arithmetic
                val start = Random.nextInt(2, 20)
                val step = Random.nextInt(3, 12)
                sequence = "${start}, ${start + step}, ${start + step * 2}, ${start + step * 3}, ?"
                correctVal = start + step * 4
            }
            1 -> { // Geometric
                val start = Random.nextInt(2, 5)
                val step = Random.nextInt(2, 4)
                sequence = "${start}, ${start * step}, ${start * step * step}, ${start * step * step * step}, ?"
                correctVal = start * step * step * step * step
            }
            2 -> { // Odd increments
                val start = Random.nextInt(1, 5)
                // Increments: 1, 3, 5, 7 or similar
                val step1 = 1
                val step2 = 3
                val step3 = 5
                val val1 = start
                val val2 = val1 + step1
                val val3 = val2 + step2
                val val4 = val3 + step3
                sequence = "$val1, $val2, $val3, $val4, ?"
                correctVal = val4 + 7
            }
        }

        val options = mutableSetOf<String>()
        options.add(correctVal.toString())
        while (options.size < 4) {
            val delta = Random.nextInt(-15, 15)
            val fakeVal = correctVal + delta
            if (fakeVal > 0 && fakeVal != correctVal) {
                options.add(fakeVal.toString())
            }
        }
        val shuffled = options.toList().shuffled()
        return Question(
            query = "Sequence: $sequence",
            options = shuffled,
            correctIdx = shuffled.indexOf(correctVal.toString())
        )
    }

    private fun generateFormulaQuestion(): Question {
        // Missing operator logic: e.g., "15 ? 3 = 5"
        val left = Random.nextInt(2, 16)
        val right = Random.nextInt(2, 16)
        val ops = listOf("+", "-", "*")
        val op = ops.random()
        val result = when (op) {
            "+" -> left + right
            "-" -> left - right
            "*" -> left * right
            else -> 0
        }

        val correctOpSym = op
        val shuffledOps = listOf("+", "-", "*", "÷").shuffled()
        return Question(
            query = "Identify operator: $left [?] $right = $result",
            options = shuffledOps,
            correctIdx = shuffledOps.indexOf(correctOpSym)
        )
    }


    // ============================================
    // GAME MODE 1: BRAIN RACE
    // ============================================
    private fun initiateBrainRace() {
        _raceState.value = RaceState.MATCHING
        _userSolvedCount.value = 0
        _currentQuestionIndex.value = 0
        _isUserPenalized.value = false
        _raceTimerMs.value = 0L

        // Generate 5 active questions for the match
        activeQuestionList.clear()
        for (i in 0 until 5) {
            val choice = Random.nextInt(0, 3)
            val question = when (choice) {
                0 -> createProceduralQuestion()
                1 -> generateSequenceQuestion()
                else -> generateFormulaQuestion()
            }
            activeQuestionList.add(question)
        }
        _currentQuestion.value = activeQuestionList.firstOrNull()

        // Configure 3 virtual opponents with slightly varied mental capability
        _opponents.value = listOf(
            Participant("nova", "Nova_Bot", "🤖", 0, Random.nextLong(2800, 3900)),
            Participant("quantum", "Quantum_Mind", "🛸", 0, Random.nextLong(2300, 3400)),
            Participant("cerebral", "Cerebral_X", "🧠", 0, Random.nextLong(3000, 4200))
        )

        // Simulate Matchmaking Delay (2.5s)
        viewModelScope.launch {
            delay(2500)
            _raceState.value = RaceState.READY
            delay(2000) // COUNTDOWN Screen "DUEL!"
            _raceState.value = RaceState.ACTIVE
            startBrainRaceGameplay()
        }
    }

    private fun startBrainRaceGameplay() {
        // Start live Arena Timer clock
        val startClockTimestamp = System.currentTimeMillis()
        _timerJob.value = viewModelScope.launch {
            while (_raceState.value == RaceState.ACTIVE) {
                _raceTimerMs.value = System.currentTimeMillis() - startClockTimestamp
                delay(40)
            }
        }

        // Simulating competitors solving puzzles concurrently
        _opponentJob.value = viewModelScope.launch {
            while (_raceState.value == RaceState.ACTIVE) {
                delay(100)
                val currentOpponents = _opponents.value.toMutableList()
                var updated = false

                for (idx in currentOpponents.indices) {
                    val op = currentOpponents[idx]
                    if (!op.isFinished) {
                        // Check if the opponent completes another puzzle based on elapsed time
                        val timeElapsed = _raceTimerMs.value
                        val expectedSolved = (timeElapsed / op.speedFactor).toInt().coerceAtMost(5)
                        if (expectedSolved > op.solvedCount) {
                            val solvedCount = expectedSolved
                            val isFinished = solvedCount == 5
                            currentOpponents[idx] = op.copy(
                                solvedCount = solvedCount,
                                isFinished = isFinished
                            )
                            updated = true
                        }
                    }
                }

                if (updated) {
                    _opponents.value = currentOpponents
                    checkForRaceCompletion()
                }
            }
        }
    }

    fun submitRaceAnswer(selectedIdx: Int) {
        if (_raceState.value != RaceState.ACTIVE || _isUserPenalized.value) return

        val q = _currentQuestion.value ?: return
        if (selectedIdx == q.correctIdx) {
            // Correct answer
            val nextSolvedCount = _userSolvedCount.value + 1
            _userSolvedCount.value = nextSolvedCount

            if (nextSolvedCount == 5) {
                checkForRaceCompletion(userFinishedNow = true)
            } else {
                _currentQuestionIndex.value = nextSolvedCount
                _currentQuestion.value = activeQuestionList.getOrNull(nextSolvedCount)
                triggerToast("✓ Bulletproof Accuracy!")
            }
        } else {
            // Incorrect answer: lock penalty "RECALIBRATING" for 1.5s
            viewModelScope.launch {
                _isUserPenalized.value = true
                triggerToast("✗ Recalibrating mind parameters...")
                delay(1500)
                _isUserPenalized.value = false
            }
        }
    }

    private fun checkForRaceCompletion(userFinishedNow: Boolean = false) {
        val userCount = _userSolvedCount.value
        val ops = _opponents.value

        // Check if user is 100% complete or any of the opponents of interest hit 5 solved states
        val finishedCount = ops.count { it.isFinished } + (if (userCount == 5 || userFinishedNow) 1 else 0)

        if (userCount == 5 || ops.any { it.solvedCount == 5 }) {
            // Someone finished! Calculate user placement
            // User placement = 1 + number of opponents who solved more than user, or who solved same and finished first
            viewModelScope.launch {
                // If user finished, check their rank
                var placement = 1
                for (op in ops) {
                    if (op.solvedCount > userCount) {
                        placement++
                    } else if (op.solvedCount == userCount && !userFinishedNow) {
                        // Opponent finished first with same solved count
                        placement++
                    }
                }

                _userFinalPlacement.value = placement

                // Configure prizes
                val gReward = when (placement) {
                    1 -> 500
                    2 -> 200
                    3 -> 100
                    else -> 20
                }
                val xReward = when (placement) {
                    1 -> 150
                    2 -> 80
                    3 -> 40
                    else -> 10
                }

                _gemsReward.value = gReward
                _xpReward.value = xReward

                // Stop active races and jobs
                _raceState.value = RaceState.FINISHED
                cancelAllJobs()

                // Persist the results immediately to the Room local DB!
                persistRaceResults(placement, gReward, xReward)
            }
        }
    }

    private suspend fun persistRaceResults(placement: Int, gemsRewardAmt: Int, xpRewardAmt: Int) {
        val activeStats = repository.getUserStatsSync() ?: UserStats()
        
        // Progress Daily Quest (Increment matching duels wins if user finishes 1st or 2nd)
        var wonIncrement = if (placement <= 2) 1 else 0
        var newQuestProgress = activeStats.questProgress + wonIncrement
        
        var addedGems = gemsRewardAmt
        var addedXp = xpRewardAmt

        // If daily quest is complete (reached 3), distribute additional 500 XP and 500 Gems!
        var questBonusText = ""
        if (activeStats.questProgress < 3 && newQuestProgress >= 3) {
            newQuestProgress = 3 // Cap
            addedGems += 500
            addedXp += 500
            questBonusText = " + Daily Quest Complete (+500 Gems, +500 XP)!"
        }

        // Dynamic level up calculation
        var updatedXp = activeStats.xp + addedXp
        var updatedLvl = activeStats.level
        while (updatedXp >= updatedLvl * 1000) {
            updatedXp -= (updatedLvl * 1000)
            updatedLvl++
        }

        val updatedStats = activeStats.copy(
            gems = activeStats.gems + addedGems,
            xp = updatedXp,
            level = updatedLvl,
            matchesWon = activeStats.matchesWon + (if (placement == 1) 1 else 0),
            totalMatches = activeStats.totalMatches + 1,
            questProgress = newQuestProgress
        )

        repository.updateUserStats(updatedStats)

        // Save to historic matches
        repository.addGameHistory(
            GameHistory(
                modeName = "Brain Race",
                resultRank = when (placement) {
                    1 -> "1st Place"
                    2 -> "2nd Place"
                    3 -> "3rd Place"
                    else -> "4th Place"
                },
                xpGained = addedXp,
                gemsGained = addedGems
            )
        )

        triggerToast("Match Finished! Claimed ${addedGems} Gems & ${addedXp} XP$questBonusText")
    }


    // ============================================
    // GAME MODE 2: 123 CHALLENGE (NUMBER DUEL)
    // ============================================
    private fun initiateNumberDuel() {
        _duelStatus.value = "MATCHING"
        _duelBotScore.value = 0
        _duelUserScore.value = 0
        _duelTimeRemainingSec.value = 15

        viewModelScope.launch {
            delay(1500)
            _duelStatus.value = "PLAYING"
            setupNewDuelRound()
            startDuelGameLoops()
        }
    }

    private fun setupNewDuelRound() {
        _selectedGridIndices.value = emptySet()
        // Generate grid of 6 target integers
        val correctPairLeft = Random.nextInt(2, 12)
        val correctPairRight = Random.nextInt(3, 15)
        val target = correctPairLeft + correctPairRight
        _targetSum.value = target

        val list = mutableListOf(correctPairLeft, correctPairRight)
        while (list.size < 6) {
            val randomNum = Random.nextInt(2, 20)
            if (randomNum != correctPairLeft && randomNum != correctPairRight) {
                list.add(randomNum)
            }
        }
        _gridNumbers.value = list.shuffled()
    }

    private fun startDuelGameLoops() {
        // Core game timer
        _timerJob.value = viewModelScope.launch {
            while (_duelStatus.value == "PLAYING" && _duelTimeRemainingSec.value > 0) {
                delay(1000)
                _duelTimeRemainingSec.value -= 1
            }
            if (_duelStatus.value == "PLAYING") {
                finishDuelMatch()
            }
        }

        // Bot simulation solving progress
        _opponentJob.value = viewModelScope.launch {
            while (_duelStatus.value == "PLAYING") {
                // Nova_Bot solves with speed relative to competency
                delay(Random.nextLong(2000, 4200))
                if (_duelStatus.value == "PLAYING") {
                    _duelBotScore.value += 1
                    triggerToast("🤖 Nova_Bot scored!")
                }
            }
        }
    }

    fun selectGridIndex(index: Int) {
        if (_duelStatus.value != "PLAYING") return

        val currentSelected = _selectedGridIndices.value.toMutableSet()
        if (currentSelected.contains(index)) {
            currentSelected.remove(index)
            _selectedGridIndices.value = currentSelected
        } else {
            currentSelected.add(index)
            _selectedGridIndices.value = currentSelected

            if (currentSelected.size == 2) {
                // Validate if selected indices combine to total targetSum
                val indicesList = currentSelected.toList()
                val valL = _gridNumbers.value.getOrNull(indicesList[0]) ?: 0
                val valR = _gridNumbers.value.getOrNull(indicesList[1]) ?: 0

                if (valL + valR == _targetSum.value) {
                    _duelUserScore.value += 1
                    triggerToast("✓ Fast Math Precision!")
                    setupNewDuelRound()
                } else {
                    triggerToast("✗ Sum equals ${valL + valR}, expected ${_targetSum.value}")
                    viewModelScope.launch {
                        // Tonal penalty
                        _selectedGridIndices.value = emptySet()
                    }
                }
            }
        }
    }

    private fun finishDuelMatch() {
        _duelStatus.value = "OVER"
        cancelAllJobs()

        viewModelScope.launch {
            val userScore = _duelUserScore.value
            val botScore = _duelBotScore.value
            val isWon = userScore > botScore
            val isMatchTie = userScore == botScore

            val placementText = when {
                isWon -> "Winner"
                isMatchTie -> "Draw"
                else -> "Defeat"
            }

            // Grant rewards
            val gemsRewarded = if (isWon) 150 else if (isMatchTie) 60 else 15
            val xpRewarded = if (isWon) 80 else if (isMatchTie) 30 else 10

            _gemsReward.value = gemsRewarded
            _xpReward.value = xpRewarded

            // Save record to local database db
            val activeStats = repository.getUserStatsSync() ?: UserStats()
            
            // Advance Daily Quest
            var wonIncrement = if (isWon) 1 else 0
            var newQuestProgress = activeStats.questProgress + wonIncrement
            
            var totalGemsEarned = gemsRewarded
            var totalXpEarned = xpRewarded

            var questBonusText = ""
            if (activeStats.questProgress < 3 && newQuestProgress >= 3) {
                newQuestProgress = 3
                totalGemsEarned += 500
                totalXpEarned += 500
                questBonusText = " + Daily Quest Complete (+500 Gems, +500 XP)!"
            }

            // Evaluate level ups
            var finalXp = activeStats.xp + totalXpEarned
            var finalLvl = activeStats.level
            while (finalXp >= finalLvl * 1000) {
                finalXp -= (finalLvl * 1000)
                finalLvl++
            }

            val updatedStats = activeStats.copy(
                gems = activeStats.gems + totalGemsEarned,
                xp = finalXp,
                level = finalLvl,
                totalMatches = activeStats.totalMatches + 1,
                matchesWon = activeStats.matchesWon + wonIncrement,
                questProgress = newQuestProgress
            )

            repository.updateUserStats(updatedStats)

            repository.addGameHistory(
                GameHistory(
                    modeName = "123 Challenge",
                    resultRank = placementText,
                    xpGained = totalXpEarned,
                    gemsGained = totalGemsEarned
                )
            )

            triggerToast("Duel Ended: $placementText! Earned $totalGemsEarned Gems & $totalXpEarned XP$questBonusText")
        }
    }


    // ============================================
    // GAME MODE 3: SURVIVAL MODE (HARDCORE IQ)
    // ============================================
    private fun initiateSurvival() {
        _survivalStatus.value = "PLAYING"
        _survivalStreak.value = 0
        setupNewSurvivalRound()
    }

    private fun setupNewSurvivalRound() {
        _survivalTimeRemainingSec.value = 4
        
        // Build simplified rapid true/false math calculations
        val left = Random.nextInt(5, 50)
        val right = Random.nextInt(4, 40)
        val isOpAdd = Random.nextBoolean()
        val opStr = if (isOpAdd) "+" else "-"
        val correctTotal = if (isOpAdd) left + right else left - right

        val isAnswerFactuallyCorrect = Random.nextBoolean()
        _survivalIsCorrect.value = isAnswerFactuallyCorrect

        val shownTotal = if (isAnswerFactuallyCorrect) {
            correctTotal
        } else {
            correctTotal + listOf(-5, -2, 2, 5, 10).random()
        }

        _survivalEquation.value = "$left $opStr $right = $shownTotal"

        // Cancel existing timer logic and schedule next countdown
        _survivalTimerJob.value?.cancel()
        _survivalTimerJob.value = viewModelScope.launch {
            while (_survivalTimeRemainingSec.value > 0 && _survivalStatus.value == "PLAYING") {
                delay(1000)
                _survivalTimeRemainingSec.value -= 1
            }
            if (_survivalStatus.value == "PLAYING") {
                // Timeout ends the run!
                finishSurvivalRun(causedByTimeout = true)
            }
        }
    }

    fun submitSurvivalGuess(guessTrue: Boolean) {
        if (_survivalStatus.value != "PLAYING") return

        val isExpressionCorrect = _survivalIsCorrect.value
        val userCorrect = guessTrue == isExpressionCorrect

        if (userCorrect) {
            _survivalStreak.value += 1
            triggerToast("✓ Correct!")
            setupNewSurvivalRound()
        } else {
            finishSurvivalRun(causedByTimeout = false)
        }
    }

    private fun finishSurvivalRun(causedByTimeout: Boolean) {
        _survivalStatus.value = "OVER"
        _survivalTimerJob.value?.cancel()

        viewModelScope.launch {
            val finalStreak = _survivalStreak.value
            val reason = if (causedByTimeout) "Time Ran Out" else "Wrong Decision"

            // Compute scaling stats rewards
            val gemsRewarded = finalStreak * 10
            val xpRewarded = finalStreak * 5

            _gemsReward.value = gemsRewarded
            _xpReward.value = xpRewarded

            val activeStats = repository.getUserStatsSync() ?: UserStats()
            
            // Increment level and XP
            var finalXp = activeStats.xp + xpRewarded
            var finalLvl = activeStats.level
            while (finalXp >= finalLvl * 1000) {
                finalXp -= (finalLvl * 1000)
                finalLvl++
            }

            val updatedStats = activeStats.copy(
                gems = activeStats.gems + gemsRewarded,
                xp = finalXp,
                level = finalLvl,
                totalMatches = activeStats.totalMatches + 1
            )

            repository.updateUserStats(updatedStats)

            repository.addGameHistory(
                GameHistory(
                    modeName = "Survival Mode",
                    resultRank = "Streak: $finalStreak",
                    xpGained = xpRewarded,
                    gemsGained = gemsRewarded
                )
            )

            triggerToast("Survival Over ($reason)! Maxed Streak: $finalStreak. Earned $gemsRewarded Gems")
        }
    }


    // ============================================
    // SHOP OPERATIONS
    // ============================================
    fun purchaseItem(item: ShopItem) {
        viewModelScope.launch {
            val stats = repository.getUserStatsSync() ?: return@launch
            if (stats.gems < item.price) {
                triggerToast("⚡ Insufficient Gems! Earn more in the Brain Race arena.")
                return@launch
            }

            val success = repository.purchaseShopItem(item, stats)
            if (success) {
                triggerToast("🎁 Unlocked ${item.name} successfully!")
            }
        }
    }

    fun equipItem(item: ShopItem) {
        viewModelScope.launch {
            val stats = repository.getUserStatsSync() ?: return@launch
            repository.equipShopItem(item, stats)
            triggerToast("⚡ Configured equipped accessory to ${item.name}!")
        }
    }


    // ============================================
    // RESET STATS (FOR SETTINGS / CONVENIENCE)
    // ============================================
    fun resetUserStats() {
        viewModelScope.launch {
            _currentScreen.value = Screen.HOME
            _activeMode.value = GameMode.NONE
            
            val freshStats = UserStats(id = 1)
            repository.saveUserStats(freshStats)
            // Relock inventory
            val schoolInventory = listOf(
                ShopItem("avatar_science", "AVATAR", "Default Chemist", "👨‍🔬", 0, isOwned = true, isEquipped = true),
                ShopItem("avatar_telepath", "AVATAR", "Telekinetic Sage", "🧠", 400),
                ShopItem("avatar_cybermind", "AVATAR", "Cyborg Synthesizer", "🤖", 600),
                ShopItem("avatar_overlord", "AVATAR", "Anomalous Overlord", "🛸", 1000),
                ShopItem("avatar_sage", "AVATAR", "Venerable Owl", "🦉", 350),
                ShopItem("title_genius", "TITLE", "Default Genius", "Genius", 0, isOwned = true, isEquipped = true),
                ShopItem("title_seer", "TITLE", "Quantum Seer", "Quantum Seer", 500),
                ShopItem("title_god", "TITLE", "Calculus God", "Calculus God", 800),
                ShopItem("title_megamind", "TITLE", "Mega-Mind", "Mega-Mind", 250)
            )
            val initialFriends = listOf(
                Friend(0, "Quantum_Mind", "🛸", "Ascended", "THINKING", "89%"),
                Friend(0, "Nova_Bot", "🤖", "Expert", "ONLINE", "61%"),
                Friend(0, "Cerebral_X", "🧠", "Grandmaster", "THINKING", "81%"),
                Friend(0, "Zero_Cool", "🕶️", "Rookie", "OFFLINE", "44%"),
                Friend(0, "Brainiac_99", "🦉", "Master", "ONLINE", "74%")
            )

            // Re-overwrite items
            for (item in schoolInventory) {
                repository.updateShopItem(item)
            }
            repository.saveUserStats(freshStats)
            triggerToast("System Calibrations Reset to Zero Parameters.")
        }
    }

    fun updateUserName(newName: String, onFinished: () -> Unit) {
        viewModelScope.launch {
            val current = repository.getUserStatsSync() ?: UserStats()
            val updated = current.copy(name = newName)
            repository.updateUserStats(updated)
            triggerToast("Agent handle re-calibrated successfully!")
            onFinished()
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelAllJobs()
    }
}

// Factory instantiation provider
class ArenaViewModelFactory(private val repository: ArenaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArenaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArenaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
