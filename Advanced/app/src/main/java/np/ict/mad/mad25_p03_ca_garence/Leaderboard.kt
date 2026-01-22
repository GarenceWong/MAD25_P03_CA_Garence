package np.ict.mad.mad25_p03_ca_garence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LeaderboardScreen(
    currentUserId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }

    var rows by remember { mutableStateOf<List<LeaderboardRow>>(emptyList()) }
    var myBest by remember { mutableIntStateOf(0) }
    var myRank by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(currentUserId) {
        val list = withContext(Dispatchers.IO) {
            db.scoreDao().getLeaderboard()
        }
        rows = list

        val idx = list.indexOfFirst { it.userId == currentUserId }
        if (idx >= 0) {
            myBest = list[idx].bestScore
            myRank = idx + 1
        } else {
            myBest = 0
            myRank = null
        }
    }

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
                Text("Leaderboard", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (myRank != null) "Your Best: $myBest (Rank #$myRank)" else "Your Best: $myBest",
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (rows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No scores yet. Play a game first!", fontSize = 18.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(rows) { index, item ->
                        val isMe = item.userId == currentUserId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isMe) Color(0xFFE3F2FD) else Color.Transparent)
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${index + 1}  ${item.username}", fontSize = 18.sp)
                            Text("${item.bestScore}", fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}
