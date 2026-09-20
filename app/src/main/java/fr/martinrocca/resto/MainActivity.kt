package fr.martinrocca.resto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.martinrocca.resto.theme.RestoTheme
import fr.martinrocca.resto.ui.RestoApp
import fr.martinrocca.resto.ui.RestoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val application = application as RestoApplication
            val viewModel: RestoViewModel = viewModel(
                factory = RestoViewModel.Factory(
                    application.appContainer.restaurantRepository,
                    application.appContainer.backupRepository,
                    application.appContainer.geoapifyService,
                ),
            )
            RestoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RestoApp(viewModel = viewModel)
                }
            }
        }
    }
}
