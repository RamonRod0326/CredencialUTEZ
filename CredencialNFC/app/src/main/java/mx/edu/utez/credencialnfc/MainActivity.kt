package mx.edu.utez.credencialnfc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import mx.edu.utez.credencialnfc.ui.navigation.NavegacionApp
import mx.edu.utez.credencialnfc.ui.theme.CredencialUtezTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CredencialUtezTheme {
                NavegacionApp()
            }
        }
    }
}
