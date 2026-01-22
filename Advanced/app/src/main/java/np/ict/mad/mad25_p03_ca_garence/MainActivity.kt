package np.ict.mad.mad25_p03_ca_garence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AppNav() } }
    }
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    var currentUserId by rememberSaveable { mutableIntStateOf(-1) }
    var currentUsername by rememberSaveable { mutableStateOf("") }

    NavHost(
        navController = navController,
        startDestination = "auth"
    ) {
        composable("auth") {
            AuthScreen(
                onSignedIn = { userId, username ->
                    currentUserId = userId
                    currentUsername = username
                    navController.navigate("game") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("game") {
            GameInterface(
                currentUserId = currentUserId,
                currentUsername = currentUsername,
                onOpenSettings = { navController.navigate("settings") },
                onOpenLeaderboard = { navController.navigate("leaderboard") },
                onLogout = {
                    currentUserId = -1
                    currentUsername = ""
                    navController.navigate("auth") {
                        popUpTo("game") { inclusive = true }
                    }
                }
            )
        }

        composable("settings") {
            SettingsInterface(
                onBack = { navController.popBackStack() }
            )
        }

        composable("leaderboard") {
            LeaderboardScreen(
                currentUserId = currentUserId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun AuthScreen(
    onSignedIn: (userId: Int, username: String) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sign In / Sign Up", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Wack", fontSize = 80.sp)
                    Text("a", fontSize = 80.sp)
                    Text("Mole", fontSize = 80.sp)
                }

                Spacer(modifier = Modifier.height(80.dp))

                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .border(2.dp, Color.Black)
                )

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password / PIN") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .border(2.dp, Color.Black)
                )


                if (message.isNotBlank()) {
                    Text(message, color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val u = username.trim()
                                val p = password.trim()

                                if (u.isBlank() || p.isBlank()) {
                                    message = "Please fill in username and password."
                                    return@launch
                                }

                                val hashed = hashPassword(p)
                                val user = db.userDao().signIn(u, hashed)

                                if (user != null) {
                                    message = ""
                                    onSignedIn(user.userId, user.username)
                                } else {
                                    message = "Invalid username or password."
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Sign In")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                val u = username.trim()
                                val p = password.trim()

                                if (u.isBlank() || p.isBlank()) {
                                    message = "Please fill in username and password."
                                    return@launch
                                }

                                val existing = db.userDao().getUserByUsername(u)
                                if (existing != null) {
                                    message = "Username already exists. Please sign in."
                                    return@launch
                                }

                                val hashed = hashPassword(p)
                                val newId = db.userDao()
                                    .insertUser(UserEntity(username = u, passwordHash = hashed))
                                    .toInt()

                                message = ""
                                onSignedIn(newId, u)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Sign Up")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun GameInterface(
    currentUserId: Int,
    currentUsername: String,
    onOpenSettings: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(30) }
    var molePosition by remember { mutableIntStateOf((0..8).random()) }
    var isRunning by remember { mutableStateOf(false) }
    var hasStarted by remember { mutableStateOf(false) }
    var highScore by remember { mutableIntStateOf(0) }

    // Time and loop the mole movement
    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect

        val timerJob = launch {
            while (isRunning && timeLeft > 0) {
                delay(1000L)
                timeLeft -= 1
            }

            if (timeLeft <= 0) {
                isRunning = false
                if (score > highScore) highScore = score

                if (currentUserId != -1) {
                    scope.launch {
                        db.scoreDao().insertScore(
                            ScoreEntity(
                                userId = currentUserId,
                                score = score,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }

        val mole = launch {
            while (isRunning && timeLeft > 0) {
                delay((700..1000).random().toLong())
                molePosition = (0..8).random()
            }
        }

        timerJob.join()
        mole.cancel()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Wack-a-Mole", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentUsername.isNotBlank()) {
                Text("User: $currentUsername", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // for current score and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Score: $score", fontSize = 30.sp)
                Text("Time: $timeLeft", fontSize = 30.sp)
            }

            Spacer(modifier = Modifier.height(44.dp))

            // For the High score
            Text("High Score: $highScore", fontSize = 30.sp)

            Spacer(modifier = Modifier.height(44.dp))

            MoleGrid(
                molePosition = molePosition,
                isRunning = isRunning,
                onHoleClick = { index ->
                    if (isRunning && timeLeft > 0 && index == molePosition) {
                        score += 1
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // To Start and Restart
            StartRestartButton(
                hasStarted = hasStarted,
                onClick = {
                    hasStarted = true
                    score = 0
                    timeLeft = 30
                    molePosition = (0..8).random()
                    isRunning = true
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            //leaderboard
            Button(
                onClick = onOpenLeaderboard,
                modifier = Modifier
                    .width(200.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Leaderboard",
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .width(200.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Logout",
                    fontSize = 24.sp
                )
            }

            if (!isRunning && hasStarted && timeLeft == 0) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Game Over! Final score: $score",
                    fontSize = 24.sp
                )
            }
        }
    }
}

@Composable
fun MoleGrid(
    molePosition: Int,
    isRunning: Boolean,
    onHoleClick: (Int) -> Unit
) {
    val holes = (0..8).toList()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(60.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        items(holes) { index ->
            HoleButton(
                showMole = isRunning && index == molePosition,
                onClick = { onHoleClick(index) }
            )
        }
    }
}

@Composable
fun HoleButton(showMole: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF7A5C3A),
            contentColor = Color.White
        ),
        modifier = Modifier.size(90.dp)
    ) {
        Text(if (showMole) "M" else "", fontSize = 36.sp)
    }
}

@Composable
fun StartRestartButton(
    hasStarted: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(200.dp)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4CAF50),
            contentColor = Color.White
        )
    ) {
        Text(
            text = if (hasStarted) "Restart" else "Start",
            fontSize = 36.sp
        )
    }
}

@Composable
fun SettingsInterface(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Settings", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Settings screen")
        }
    }
}

private fun hashPassword(raw: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
