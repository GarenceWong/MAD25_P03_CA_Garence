package np.ict.mad.mad25_p03_ca_garence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppNav()
            }
        }
    }
}

@Composable
fun AppNav() {
    val navController = rememberNavController()

    // Room db (single instance)
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    // "logged in user"
    var currentUserId by remember { mutableIntStateOf(-1) }

    NavHost(
        navController = navController,
        startDestination = "auth"
    ) {
        composable("auth") {
            AuthScreen(
                db = db,
                onAuthSuccess = { userId ->
                    currentUserId = userId
                    navController.navigate("game") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("game") {
            GameInterface(
                db = db,
                currentUserId = currentUserId,
                onOpenSettings = { navController.navigate("settings") }
            )
        }

        composable("settings") {
            SettingsInterface(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun AuthScreen(
    db: AppDatabase,
    onAuthSuccess: (Int) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    message = ""
                },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    message = ""
                },
                label = { Text("Password / PIN") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (message.isNotBlank()) {
                Text(message, color = Color.Red, fontSize = 16.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            message = "Please fill in username and password."
                            return@Button
                        }

                        loading = true
                        scope.launch {
                            val user = db.userDao().signIn(username.trim(), password)
                            loading = false

                            if (user != null) {
                                onAuthSuccess(user.userId)
                            } else {
                                message = "Invalid username or password."
                            }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sign In")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        if (username.isBlank() || password.isBlank()) {
                            message = "Please fill in username and password."
                            return@Button
                        }

                        loading = true
                        scope.launch {
                            val existing = db.userDao().getUserByUsername(username.trim())
                            if (existing != null) {
                                loading = false
                                message = "Username already exists. Try another."
                                return@launch
                            }

                            val newId = db.userDao().insertUser(
                                UserEntity(username = username.trim(), password = password)
                            ).toInt()

                            loading = false
                            onAuthSuccess(newId)
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sign Up")
                }
            }

            if (loading) {
                Spacer(modifier = Modifier.height(10.dp))
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun GameInterface(
    db: AppDatabase,
    currentUserId: Int,
    onOpenSettings: () -> Unit
) {
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(30) }
    var molePosition by remember { mutableIntStateOf((0..8).random()) }
    var isRunning by remember { mutableStateOf(false) }
    var hasStarted by remember { mutableStateOf(false) }

    // show personal best from Room
    var highScore by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // load high score once when entering game / when user changes
    LaunchedEffect(currentUserId) {
        if (currentUserId > -1) {
            val best = db.scoreDao().getUserBest(currentUserId) ?: 0
            highScore = best
        }
    }

    // Time and loop the mole moment
    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect

        val timerJob = launch {
            while (isRunning && timeLeft > 0) {
                delay(1000L)
                timeLeft -= 1
            }

            if (timeLeft <= 0) {
                isRunning = false

                // save score to DB when game ends
                if (currentUserId > -1) {
                    scope.launch {
                        db.scoreDao().insertScore(
                            ScoreEntity(
                                userId = currentUserId,
                                score = score,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        val best = db.scoreDao().getUserBest(currentUserId) ?: 0
                        highScore = best
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
