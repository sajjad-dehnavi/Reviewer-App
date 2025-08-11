package com.shiragin.review

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.shiragin.libraries.review.DataManager
import com.shiragin.review.ui.theme.ReviewerAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        DataManager.init(
            context = applicationContext,
            tapsellKey = "",
            testDeviceIdsForAdmob = listOf("33BE2250B43518CCDA7DE426D04EE231"),
            serverConfigUrl = "https://wiadevelopers.com/apps/pixel_press/dev/v2/server_config.json",
            marketUrl = "",
            vendingPackageName = "",
            iranLocationValidator = { isUserLocatedAtIran ->

            }
        )
        lifecycleScope.launch {
            DataManager.getSettings().also { settings ->
                Log.d("TAG", "onCreate: $settings")
            }
        }


        setContent {
            ReviewerAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ReviewerAppTheme {
        Greeting("Android")
    }
}