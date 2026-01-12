package np.ict.mad.mad25_p03_ca_garence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GameInterface()
            }
        }
    }
}

@Composable
fun GameInterface() {
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(30) }
    var highScore by remember { mutableIntStateOf(0) }

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
                IconButton(onClick = { }) {
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
                onHoleClick = { }

            )
        }
    }
}

@Composable
fun MoleGrid(onHoleClick: (Int) -> Unit) {
    val holes = (0..8).toList()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(80.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        items(holes) { index ->
            HoleButton(
                onClick = { onHoleClick(index) }
            )
        }
    }
}

@Composable
fun HoleButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF7A5C3A),
            contentColor = Color.White
        ),
        modifier = Modifier.size(90.dp)
    ) {
        Text("M", fontSize = 16.sp)
    }
}
