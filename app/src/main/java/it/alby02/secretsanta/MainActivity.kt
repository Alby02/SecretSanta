package it.alby02.secretsanta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import it.alby02.secretsanta.ui.theme.SecretSantaTheme

// A (hypothetical) activity to show the main app screen after login
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecretSantaTheme {
                Text("Welcome to the Main App!")
            }
        }
    }
}