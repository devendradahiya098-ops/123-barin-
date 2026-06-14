package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainArenaContainer()
            }
        }
    }
}

@Composable
fun MainArenaContainer() {
    val context = LocalContext.current
    // Build standard database references & constructor injection
    val database = remember { AppDatabase.getInstance(context) }
    val repository = remember { ArenaRepository(database.arenaDao()) }
    val viewModel: ArenaViewModel = viewModel(
        factory = ArenaViewModelFactory(repository)
    )

    // Collect reactive state flows
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeMode by viewModel.activeMode.collectAsStateWithLifecycle()
    val userStats by viewModel.userStats.collectAsStateWithLifecycle()
    val toastMsg by viewModel.toastMsg.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SleekDarkBg,
        bottomBar = {
            // Only draw bottom navigation if we aren't in active play modes to maximize gaming view focus
            if (activeMode == GameMode.NONE) {
                SleekBottomNavigation(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.setScreen(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeMode == GameMode.NONE) {
                // Standard visual modes dashboard
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Area
                    SleekProfileHeader(
                        userStats = userStats,
                        onSettingsClicked = { showSettingsDialog = true }
                    )

                    // Thinking Arena status ticker
                    SleekArenaStatusBar()

                    // Main view router
                    Box(modifier = Modifier.weight(1f)) {
                        when (currentScreen) {
                            Screen.HOME -> HomeScreenDashboard(viewModel = viewModel)
                            Screen.FRIENDS -> FriendsScreen(viewModel = viewModel)
                            Screen.LOCKER -> LockerScreen(viewModel = viewModel)
                            Screen.RANKS -> RanksScreen(viewModel = viewModel)
                            Screen.SHOP -> ShopScreen(viewModel = viewModel)
                        }
                    }
                }
            } else {
                // Deep immersive Gaming Modes Window
                GamingArenaScreen(viewModel = viewModel)
            }

            // Centralized Floating micro Toast notifications
            AnimatedVisibility(
                visible = toastMsg.isNotEmpty(),
                enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { 50 }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            ) {
                Surface(
                    color = SleekDarkBg.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(24.dp),
                    border = borderIndicatorBrush(SleekCyan),
                    tonalElevation = 12.dp
                ) {
                    Text(
                        text = toastMsg,
                        color = SleekCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Settings Dialogue Overlay
            if (showSettingsDialog) {
                SettingsDialog(
                    userStats = userStats,
                    viewModel = viewModel,
                    onClose = { showSettingsDialog = false }
                )
            }
        }
    }
}

// Custom cyan neon border helper
@Composable
fun borderIndicatorBrush(color: Color) = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    brush = Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.2f)))
)

// ============================================
// HEADER PROFILE AREA (SLEEK STYLE)
// ============================================
@Composable
fun SleekProfileHeader(
    userStats: UserStats?,
    onSettingsClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SleekCardBg.copy(alpha = 0.8f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // User Meta
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(SleekCyan.copy(alpha = 0.3f), Color.Black)
                        )
                    )
                    .border(2.dp, SleekCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userStats?.avatar ?: "👨‍🔬",
                    fontSize = 24.sp
                )
            }

            Column {
                Text(
                    text = "RANK: ${userStats?.rankTitle?.uppercase() ?: "GENIUS"}",
                    color = SleekCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = userStats?.name ?: "Alex_Nova",
                    color = SleekTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action Metrics & Settings Gear
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Gems Counter Badge
            Surface(
                color = SleekBorder,
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "💎", fontSize = 12.sp)
                    Text(
                        text = String.format("%,d", userStats?.gems ?: 1240),
                        color = SleekTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Gear settings
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SleekBorder)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .clickable { onSettingsClicked() }
                    .testTag("settings_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "System Settings",
                    tint = SleekTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ============================================
// STATUS TICKER (BREATHING CYBER)
// ============================================
@Composable
fun SleekArenaStatusBar() {
    val infiniteTransition = rememberInfiniteTransition(label = "ledger_pulse")
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_glow"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SleekCyan.copy(alpha = 0.05f))
            .border(width = 1.dp, color = SleekCyan.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(SleekGreen.copy(alpha = breathingAlpha))
            )
            Text(
                text = "ONLINE ARENA: 12,402 THINKING",
                color = SleekCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
        Text(
            text = "v1.0.4-Beta",
            color = SleekTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============================================
// LOWER NAVIGATION BAR (SLEEK STYLE)
// ============================================
@Composable
fun SleekBottomNavigation(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        color = SleekDarkBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Home,
                label = "Home",
                isActive = currentScreen == Screen.HOME,
                onClick = { onNavigate(Screen.HOME) },
                testTag = "nav_home"
            )
            BottomNavItem(
                icon = Icons.Default.People,
                label = "Friends",
                isActive = currentScreen == Screen.FRIENDS,
                onClick = { onNavigate(Screen.FRIENDS) },
                testTag = "nav_friends"
            )
            BottomNavItem(
                icon = Icons.Default.Face,
                label = "Locker",
                isActive = currentScreen == Screen.LOCKER,
                onClick = { onNavigate(Screen.LOCKER) },
                testTag = "nav_locker"
            )
            BottomNavItem(
                icon = Icons.Default.Leaderboard,
                label = "Ranks",
                isActive = currentScreen == Screen.RANKS,
                onClick = { onNavigate(Screen.RANKS) },
                testTag = "nav_ranks"
            )
            BottomNavItem(
                icon = Icons.Default.ShoppingCart,
                label = "Shop",
                isActive = currentScreen == Screen.SHOP,
                onClick = { onNavigate(Screen.SHOP) },
                testTag = "nav_shop"
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) SleekCyan else SleekTextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label.uppercase(),
            color = if (isActive) SleekTextPrimary else SleekTextSecondary.copy(alpha = 0.4f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

// ============================================
// DASHBOARD VIEW (HOME TAB)
// ============================================
@Composable
fun HomeScreenDashboard(viewModel: ArenaViewModel) {
    val userStats by viewModel.userStats.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero: Brain Race featured mode
        item {
            BrainRaceHeroCard(onEnterArena = { viewModel.startMode(GameMode.BRAIN_RACE) })
        }

        // Sub modes title
        item {
            Text(
                text = "COGNITIVE MODULES",
                color = SleekTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Dual columns grid for other modes (123 Challenge & Survival mode)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 123 Challenge Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1.1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SleekCardBg)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .clickable { viewModel.startMode(GameMode.NUMBER_DUEL) }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SleekOrangeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔢", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Column {
                            Text(
                                text = "123 Challenge",
                                color = SleekTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "NUMBER DUEL",
                                color = SleekOrange,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // Survival Mode Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1.1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SleekCardBg)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .clickable { viewModel.startMode(GameMode.SURVIVAL) }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SleekPurpleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🧬", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Column {
                            Text(
                                text = "Survival Mode",
                                color = SleekTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "HARDCORE IQ",
                                color = SleekPurple,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Daily Quest card Progress
        item {
            DailyQuestProgressCard(userStats = userStats)
        }
    }
}

// ============================================
// MULTIPLAYER BRAIN RACE HERO CARD
// ============================================
@Composable
fun BrainRaceHeroCard(onEnterArena: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(SleekIndigo, SleekFuchsia)
                )
            )
            .clickable { onEnterArena() }
            .testTag("enter_arena_card")
    ) {
        // Procedural carbon fibre overlay lines drawn in canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 15.dp.toPx()
            val strokeWidth = 1f
            for (i in -size.height.toInt()..size.width.toInt() step step.toInt()) {
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(i.toFloat(), 0f),
                    end = Offset(i.toFloat() + size.height, size.height),
                    strokeWidth = strokeWidth
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Label tag top
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "FEATURED MODE",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
            }

            // Description and Launch
            Column {
                Text(
                    text = "BRAIN RACE",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Real-time multiplayer speed puzzles. Beat 3 opponents to rank up.",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 14.dp)
                        .widthIn(max = 240.dp)
                )

                // Hot neon activation button
                Button(
                    onClick = { onEnterArena() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("enter_arena_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ENTER ARENA",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(text = "⚡", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ============================================
// DAILY QUEST GRAPH (ELEGANT INTEGRATION)
// ============================================
@Composable
fun DailyQuestProgressCard(userStats: UserStats?) {
    val progressFraction = (userStats?.questProgress ?: 1).toFloat() / 3f

    Surface(
        color = Color.White.copy(alpha = 0.03f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "🏆", fontSize = 22.sp)
                    Column {
                        Text(
                            text = "DAILY QUEST",
                            color = SleekTextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Win 3 Math Duels (${userStats?.questProgress ?: 1}/3)",
                            color = SleekTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "+500 XP",
                    color = SleekCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Neon cyan linear progress meter
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(SleekCyan, SleekCyan.copy(alpha = 0.4f))
                            )
                        )
                )
            }
        }
    }
}

// ============================================
// MODULE: FRIENDS SCHEMATIC (FRIENDS TAB)
// ============================================
@Composable
fun FriendsScreen(viewModel: ArenaViewModel) {
    val friendsList by viewModel.friends.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "COMPETITIVE CONTACTS",
            color = SleekTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (friendsList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No contacts initialized.", color = SleekTextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(friendsList) { friend ->
                    Surface(
                        color = SleekCardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(SleekBorder),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = friend.avatarIcon, fontSize = 20.sp)
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = friend.name,
                                            color = SleekTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        // Status Dot
                                        val dotColor = when (friend.status) {
                                            "ONLINE" -> SleekGreen
                                            "THINKING" -> SleekCyan
                                            else -> SleekTextSecondary.copy(alpha = 0.4f)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                    }
                                    Text(
                                        text = "${friend.rank} • Win Ratio: ${friend.winRatio}",
                                        color = SleekTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Challenge btn
                            Button(
                                onClick = {
                                    if (friend.status != "OFFLINE") {
                                        viewModel.startMode(GameMode.NUMBER_DUEL)
                                        viewModel.triggerToast("Direct challenge issued to ${friend.name}!")
                                    } else {
                                        viewModel.triggerToast("${friend.name} is offline. Challenge unavailable.")
                                    }
                                },
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("friend_challenge_${friend.name}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (friend.status != "OFFLINE") SleekCyan.copy(alpha = 0.15f) else SleekBorder,
                                    contentColor = if (friend.status != "OFFLINE") SleekCyan else SleekTextSecondary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = if (friend.status != "OFFLINE") "DUEL" else "AWAY",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// LOCKER SYSTEM & PROFILE CUSTOMIZER (LOCKER TAB)
// ============================================
@Composable
fun LockerScreen(viewModel: ArenaViewModel) {
    val userStats by viewModel.userStats.collectAsStateWithLifecycle()
    val shopItems by viewModel.shopItems.collectAsStateWithLifecycle()

    var editingName by remember { mutableStateOf(userStats?.name ?: "Alex_Nova") }
    var isSaving by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Identity config card
        item {
            Surface(
                color = SleekCardBg,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "IDENTITY CALIBRATOR",
                        color = SleekTextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editingName,
                        onValueChange = { editingName = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SleekCyan,
                            unfocusedBorderColor = SleekBorder,
                            focusedTextColor = SleekTextPrimary,
                            unfocusedTextColor = SleekTextPrimary
                        ),
                        singleLine = true,
                        label = { Text("Agent handle name", color = SleekTextSecondary) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (editingName.trim().isNotEmpty()) {
                                isSaving = true
                                viewModel.updateUserName(editingName.trim()) {
                                    isSaving = false
                                }
                            } else {
                                viewModel.triggerToast("Handle name cannot be empty.")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("save_identity_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isSaving) "SAVING PARAMS..." else "UPDATE PROFILE PARAMETERS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // Equipment List title
        item {
            Text(
                text = "OWNED INVENTORY",
                color = SleekTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Owned assets list (can equip here)
        val ownedItems = shopItems.filter { it.isOwned }
        if (ownedItems.isEmpty()) {
            item {
                Text(
                    text = "Inventory empty. Visit the Shop to unlock accessories.",
                    color = SleekTextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                )
            }
        } else {
            items(ownedItems) { accessory ->
                Surface(
                    color = SleekCardBg,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (accessory.isEquipped) SleekCyan.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SleekBorder),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (accessory.type == "AVATAR") accessory.value else "🏷️",
                                    fontSize = 20.sp
                                )
                            }
                            Column {
                                Text(
                                    text = accessory.name,
                                    color = SleekTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${accessory.type} • Value: \"${accessory.value}\"",
                                    color = SleekTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.equipItem(accessory) },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("equip_btn_${accessory.itemId}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (accessory.isEquipped) SleekCyan else SleekBorder,
                                contentColor = if (accessory.isEquipped) Color.Black else SleekTextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (accessory.isEquipped) "EQUIPPED" else "EQUIP",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// SYSTEM LEADERBOARDS & STATS (RANKS TAB)
// ============================================
@Composable
fun RanksScreen(viewModel: ArenaViewModel) {
    val stats by viewModel.userStats.collectAsStateWithLifecycle()
    val historyLogs by viewModel.gameHistory.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick player stats digest
        item {
            Surface(
                color = SleekCardBg,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "LEVEL", color = SleekTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${stats?.level ?: 1}", color = SleekCyan, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "TOTAL DUELS", color = SleekTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${stats?.totalMatches ?: 3}", color = SleekTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "VICTORIES", color = SleekTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${stats?.matchesWon ?: 1}", color = SleekGreen, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Leaderboard title
        item {
            Text(
                text = "GLOBAL BRACKETS",
                color = SleekTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Static brackets but combining user results dynamically
        val rankers = listOf(
            Triple("Quantum_Mind", "🛸 Ascended", 1450),
            Triple("Cerebral_X", "🧠 Grandmaster", 1280),
            Triple("Alex_Nova", "👨‍🔬 " + (stats?.rankTitle ?: "Genius"), 1140 + (stats?.level ?: 1) * 30),
            Triple("Brainiac_99", "🦉 Master", 1120),
            Triple("Nova_Bot", "🤖 Expert", 980),
            Triple("Zero_Cool", "🕶️ Rookie", 450)
        ).sortedByDescending { it.third }

        items(rankers.size) { idx ->
            val competitor = rankers[idx]
            val isUser = competitor.first == "Alex_Nova"
            val displayTitle = if (isUser) (stats?.rankTitle ?: "Genius") else competitor.second
            val displayName = if (isUser) (stats?.name ?: "Alex_Nova") else competitor.first
            val displayAvatar = if (isUser) (stats?.avatar ?: "👨‍🔬") else {
                when (competitor.first) {
                    "Quantum_Mind" -> "🛸"
                    "Cerebral_X" -> "🧠"
                    "Brainiac_99" -> "🦉"
                    "Nova_Bot" -> "🤖"
                    else -> "🕶️"
                }
            }

            Surface(
                color = if (isUser) SleekCyan.copy(alpha = 0.05f) else SleekCardBg,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isUser) SleekCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "#${idx + 1}",
                            color = if (idx == 0) SleekCyan else SleekTextSecondary,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SleekBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = displayAvatar, fontSize = 16.sp)
                        }

                        Column {
                            Text(
                                text = displayName,
                                color = SleekTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = displayTitle,
                                color = SleekTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Text(
                        text = "${competitor.third} IQ",
                        color = if (isUser) SleekCyan else SleekTextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Action logs
        item {
            Text(
                text = "RECENT COMBAT LOGS",
                color = SleekTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (historyLogs.isEmpty()) {
            item {
                Text(
                    text = "No recorded combat duels in system log.",
                    color = SleekTextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            }
        } else {
            items(historyLogs) { run ->
                Surface(
                    color = SleekCardBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = run.modeName, color = SleekTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            val formattedTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(
                                java.util.Date(run.timestamp)
                            )
                            Text(text = "$formattedTime • Result: ${run.resultRank}", color = SleekTextSecondary, fontSize = 11.sp)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "+${run.gemsGained} 💎", color = SleekCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "+${run.xpGained} XP", color = SleekGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// MARKET TRANSACTION PORTAL (SHOP TAB)
// ============================================
@Composable
fun ShopScreen(viewModel: ArenaViewModel) {
    val userStats by viewModel.userStats.collectAsStateWithLifecycle()
    val shopItems by viewModel.shopItems.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Gems banner balance
        Surface(
            color = SleekCyan.copy(alpha = 0.05f),
            border = androidx.compose.foundation.BorderStroke(1.dp, SleekCyan.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "CURRENT CREDIT BALANCES", color = SleekTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Quantum Node Wallet", color = SleekTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "💎", fontSize = 18.sp)
                    Text(
                        text = String.format("%,d", userStats?.gems ?: 1240),
                        color = SleekTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Text(
            text = "PREMIUM UTILITIES",
            color = SleekTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(shopItems) { item ->
                // Skip default owned free starter options in the catalog to keep clean
                if (item.price > 0) {
                    Surface(
                        color = SleekCardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SleekBorder),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (item.type == "AVATAR") item.value else "🏷️",
                                        fontSize = 20.sp
                                    )
                                }
                                Column {
                                    Text(
                                        text = item.name,
                                        color = SleekTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${item.type} • Accessory value: \"${item.value}\"",
                                        color = SleekTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Dynamic purchase or equip action state
                            if (item.isOwned) {
                                Button(
                                    onClick = { viewModel.equipItem(item) },
                                    modifier = Modifier.height(34.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (item.isEquipped) SleekCyan else SleekBorder,
                                        contentColor = if (item.isEquipped) Color.Black else SleekTextPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = if (item.isEquipped) "EQUIPPED" else "EQUIP",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.purchaseItem(item) },
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("shop_buy_${item.itemId}"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SleekCyan.copy(alpha = 0.15f),
                                        contentColor = SleekCyan
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = "💎", fontSize = 10.sp)
                                        Text(
                                            text = item.price.toString(),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// SYSTEM PREFERENCES OVERLAY (SETTINGS)
// ============================================
@Composable
fun SettingsDialog(
    userStats: UserStats?,
    viewModel: ArenaViewModel,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onClose() },
        title = {
            Text(
                text = "SYSTEM PREFERENCES",
                color = SleekCyan,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                letterSpacing = 0.8.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Aesthetic core calibrated in carbon-slate framework v1.0.4 with full client-side state engines securely persisting data locally.",
                    color = SleekTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Divider(color = Color.White.copy(alpha = 0.08f))

                // Stats reset trigger
                OutlinedButton(
                    onClick = {
                        viewModel.resetUserStats()
                        onClose()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("reset_progress_button"),
                    border = borderIndicatorBrush(Color.Red.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Wipe Stats", modifier = Modifier.size(16.dp))
                        Text(text = "RESET PROGRESS DATABASE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onClose() },
                colors = ButtonDefaults.buttonColors(containerColor = SleekCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "CLOSE MONITOR", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = SleekCardBg,
        textContentColor = SleekTextPrimary
    )
}

// ============================================
// ACTIVE GAMING SCREENS ROUTER
// ============================================
@Composable
fun GamingArenaScreen(viewModel: ArenaViewModel) {
    val activeMode by viewModel.activeMode.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekDarkBg)
    ) {
        when (activeMode) {
            GameMode.BRAIN_RACE -> BrainRacePlayEngine(viewModel = viewModel)
            GameMode.NUMBER_DUEL -> NumberDuelPlayEngine(viewModel = viewModel)
            GameMode.SURVIVAL -> SurvivalPlayEngine(viewModel = viewModel)
            else -> {}
        }
    }
}

// ============================================
// PLAYABLE MODE 1: BRAIN RACE INTERACTIVE
// ============================================
@Composable
fun BrainRacePlayEngine(viewModel: ArenaViewModel) {
    val raceState by viewModel.raceState.collectAsStateWithLifecycle()
    val opponents by viewModel.opponents.collectAsStateWithLifecycle()
    val userSolved by viewModel.userSolvedCount.collectAsStateWithLifecycle()
    val questionIdx by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    val question by viewModel.currentQuestion.collectAsStateWithLifecycle()
    val timerMs by viewModel.raceTimerMs.collectAsStateWithLifecycle()
    val isPenalized by viewModel.isUserPenalized.collectAsStateWithLifecycle()

    val finalPlacement by viewModel.userFinalPlacement.collectAsStateWithLifecycle()
    val gemsRewarded by viewModel.gemsReward.collectAsStateWithLifecycle()
    val xpRewarded by viewModel.xpReward.collectAsStateWithLifecycle()

    when (raceState) {
        RaceState.MATCHING -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = SleekCyan, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "CORRELATING CENTRAL NODES...",
                    color = SleekCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Aligning simulated logic streams with active peers",
                    color = SleekTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        RaceState.READY -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ESTABLISHED LINK ✓",
                    color = SleekGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "INITIALIZING RACE MATRIX",
                    color = SleekTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "First participant to solve 5 equations overrides the terminal!",
                    color = SleekTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // Render matching grid
                Surface(
                    color = SleekCardBg,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "RACE GRID PARTICIPANTS:", color = SleekTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(text = "• YOU (👨‍🔬 Alex_Nova) -> ping 4 ms", color = SleekCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "• 🤖 Nova_Bot -> ping 22 ms", color = SleekTextPrimary, fontSize = 12.sp)
                        Text(text = "• 🛸 Quantum_Mind -> ping 18 ms", color = SleekTextPrimary, fontSize = 12.sp)
                        Text(text = "• 🧠 Cerebral_X -> ping 34 ms", color = SleekTextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }
        RaceState.ACTIVE -> {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header clock
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BRAIN DUEL ACTIVE",
                        color = SleekCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )

                    // Format elapsed time (seconds + tenths)
                    val secs = timerMs / 1000
                    val tenths = (timerMs % 1000) / 100
                    Text(
                        text = "TIME: ${secs}.${tenths}s",
                        color = SleekTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Opponents progress indicators side metrics
                Surface(
                    color = SleekCardBg,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "ARENA SOLVE BRACKETS:", color = SleekTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)

                        // Render user bar
                        OpponentProgressBar(name = "YOU", solved = userSolved, max = 5, barColor = SleekCyan)

                        // Render opponent bars
                        for (op in opponents) {
                            OpponentProgressBar(name = op.name, solved = op.solvedCount, max = 5, barColor = SleekIndigo)
                        }
                    }
                }

                // Central Active Equation card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SleekCardBg)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPenalized) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(text = "⛔", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "RECALIBRATING PARAMS...", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text(text = "+1.5s lock panel penalty", color = SleekTextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.getDp()))
                        }
                    } else if (question != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "QUESTION ${questionIdx + 1} OF 5",
                                color = SleekTextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = question!!.query,
                                color = SleekTextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        CircularProgressIndicator(color = SleekCyan)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Options answer grid
                if (question != null && !isPenalized) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (idx in 0 until question!!.options.size step 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Left opt
                                Button(
                                    onClick = { viewModel.submitRaceAnswer(idx) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("race_option_$idx"),
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekCardBg),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = question!!.options[idx],
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = SleekTextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                // Right opt
                                if (idx + 1 < question!!.options.size) {
                                    Button(
                                        onClick = { viewModel.submitRaceAnswer(idx + 1) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("race_option_${idx + 1}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = SleekCardBg),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text(
                                            text = question!!.options[idx + 1],
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = SleekTextPrimary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Emergency Exit Button
                OutlinedButton(
                    onClick = { viewModel.exitMode() },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("exit_game_button"),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = "ABANDON DUEL (WIPE FIELD)", color = SleekTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        RaceState.FINISHED -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val bannerColor = if (finalPlacement == 1) SleekCyan else SleekTextSecondary
                val crownIdx = if (finalPlacement == 1) "👑 VICTOR" else "COMBAT FINISHED"

                Text(
                    text = crownIdx,
                    color = bannerColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "PLACEMENT: #${finalPlacement}",
                    color = SleekTextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Final calculation completed successfully",
                    color = SleekTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Earnings card
                Surface(
                    color = SleekCardBg,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "GEMS GAINED", color = SleekTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = "+$gemsRewarded 💎", color = SleekCyan, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "XP EARNED", color = SleekTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = "+$xpRewarded XP", color = SleekGreen, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Button(
                    onClick = { viewModel.exitMode() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("finish_race_continue_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = "CLAIM REWARDS & RETURN", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
    }
}

// Convert numbers helper safely
fun Int.getDp() = this.dp

@Composable
fun OpponentProgressBar(name: String, solved: Int, max: Int, barColor: Color) {
    val progressFraction = solved.toFloat() / max.toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = name, color = SleekTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = "$solved / $max Solved", color = SleekTextSecondary, fontSize = 10.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressFraction)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}

// ============================================
// PLAYABLE MODE 2: 123 CHALLENGE (NUMBER DUEL)
// ============================================
@Composable
fun NumberDuelPlayEngine(viewModel: ArenaViewModel) {
    val status by viewModel.duelStatus.collectAsStateWithLifecycle()
    val targetVal by viewModel.targetSum.collectAsStateWithLifecycle()
    val gridList by viewModel.gridNumbers.collectAsStateWithLifecycle()
    val selectedIndices by viewModel.selectedGridIndices.collectAsStateWithLifecycle()
    val userSc by viewModel.duelUserScore.collectAsStateWithLifecycle()
    val botSc by viewModel.duelBotScore.collectAsStateWithLifecycle()
    val timerRemainder by viewModel.duelTimeRemainingSec.collectAsStateWithLifecycle()

    val gemsRewarded by viewModel.gemsReward.collectAsStateWithLifecycle()
    val xpRewarded by viewModel.xpReward.collectAsStateWithLifecycle()

    when (status) {
        "MATCHING" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = SleekOrange, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "SEARCHING DUEL CORRIDORS...",
                    color = SleekOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Connecting active nodes to Nova_Bot...",
                    color = SleekTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        "PLAYING" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header HUD
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "123 DUEL SHOOTOUT",
                            color = SleekOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "TIMER: ${timerRemainder}s",
                            color = if (timerRemainder <= 3) Color.Red else SleekTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Scoreboard dashboard
                    Surface(
                        color = SleekCardBg,
                        border = borderIndicatorBrush(SleekOrange),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "YOU (Alex_Nova)", color = SleekTextSecondary, fontSize = 10.sp)
                                Text(text = "$userSc Pts", color = SleekCyan, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                            Text(text = "VS", color = SleekTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Nova_Bot", color = SleekTextSecondary, fontSize = 10.sp)
                                Text(text = "$botSc Pts", color = SleekOrange, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                // Main gameplay Target
                Surface(
                    color = SleekCardBg,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "FIND 2 VALUES THAT MULTIPLY / ADD TO TOTAL:",
                            color = SleekTextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "TARGET:  $targetVal",
                            color = SleekOrange,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Grid of 6 integers
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(gridList.size) { index ->
                        val value = gridList[index]
                        val isSelected = selectedIndices.contains(index)
                        Surface(
                            color = if (isSelected) SleekOrange.copy(alpha = 0.15f) else SleekCardBg,
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = if (isSelected) SleekOrange else Color.White.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .clickable { viewModel.selectGridIndex(index) }
                                .height(64.dp)
                                .testTag("duel_grid_cell_$index")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = value.toString(),
                                    color = if (isSelected) SleekOrange else SleekTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { viewModel.exitMode() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = "ABANDON COMBAT", color = SleekTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        "OVER" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val subtext = if (userSc > botSc) "DUEL VICTORY ✓" else if (userSc == botSc) "COMBAT TIE •" else "DUEL DEFEAT ✗"
                val cl = if (userSc > botSc) SleekGreen else if (userSc == botSc) SleekTextSecondary else Color.Red

                Text(text = subtext, color = cl, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SCORE: $userSc vs Bot $botSc",
                    color = SleekTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Rewards distributed recursively into local nodes",
                    color = SleekTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Earning stats
                Surface(
                    color = SleekCardBg,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "GEMS RECEIVED", color = SleekTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = "+$gemsRewarded 💎", color = SleekCyan, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "XP GAINED", color = SleekTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = "+$xpRewarded XP", color = SleekGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Button(
                    onClick = { viewModel.exitMode() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = "CLAIM CREDITS & GO BACK", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
    }
}

// ============================================
// PLAYABLE MODE 3: SURVIVAL SUDDEN DEATH
// ============================================
@Composable
fun SurvivalPlayEngine(viewModel: ArenaViewModel) {
    val status by viewModel.survivalStatus.collectAsStateWithLifecycle()
    val equation by viewModel.survivalEquation.collectAsStateWithLifecycle()
    val streak by viewModel.survivalStreak.collectAsStateWithLifecycle()
    val remainderTicks by viewModel.survivalTimeRemainingSec.collectAsStateWithLifecycle()

    val gemsRewarded by viewModel.gemsReward.collectAsStateWithLifecycle()
    val xpRewarded by viewModel.xpReward.collectAsStateWithLifecycle()

    when (status) {
        "PLAYING" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Area
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SURVIVAL MODE (SUDDEN DEATH)",
                            color = SleekPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                            Text(
                                text = "LIVES: 1 / 1",
                                color = Color.Red,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Streak meter
                    Surface(
                        color = SleekCardBg,
                        border = borderIndicatorBrush(SleekPurple),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "STREAK AMPLIFIER", color = SleekTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "$streak Wins", color = SleekPurple, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Rapid Equation card
                Surface(
                    color = SleekCardBg,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CHOOSE CORRECTNESS RAPIDLY:",
                            color = SleekTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = equation,
                            color = SleekTextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Shrinking timer indicator bar
                        val ticksFraction = remainderTicks.toFloat() / 4f
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(ticksFraction)
                                    .clip(CircleShape)
                                    .background(if (remainderTicks <= 1) Color.Red else SleekPurple)
                            )
                        }
                        Text(
                            text = "${remainderTicks}s",
                            color = if (remainderTicks <= 1) Color.Red else SleekTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Yes/No Quick Choice Button Group layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Correct Btn
                    Button(
                        onClick = { viewModel.submitSurvivalGuess(guessTrue = true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("survival_btn_true"),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekGreen),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(text = "CORRECT ✓", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.Black)
                    }

                    // Incorrect Btn
                    Button(
                        onClick = { viewModel.submitSurvivalGuess(guessTrue = false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("survival_btn_false"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(text = "INCORRECT ✗", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { viewModel.exitMode() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = "ABORT RUN", color = SleekTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        "OVER" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "SURVIVAL DEFEAT ✗", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "MAX STREAK: $streak",
                    color = SleekTextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Each streak step distributed 10 Gems and 5 XP into bank Nodes",
                    color = SleekTextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Payout
                Surface(
                    color = SleekCardBg,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "GEMS REWARDED", color = SleekTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = "+$gemsRewarded 💎", color = SleekCyan, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "XP AWARDED", color = SleekTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(text = "+$xpRewarded XP", color = SleekGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Button(
                    onClick = { viewModel.exitMode() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = "RECORD METRICS & EXIT", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
    }
}
