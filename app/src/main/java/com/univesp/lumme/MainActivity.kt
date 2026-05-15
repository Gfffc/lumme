package com.univesp.lumme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.univesp.lumme.presentation.navigation.LummeNavGraph
import com.univesp.lumme.presentation.ui.theme.LummeTheme
import com.univesp.lumme.presentation.viewmodel.OAuthCallbackViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val oauthVm: OAuthCallbackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleOAuthIntent(intent)

        setContent {
            LummeTheme {
                val authState by oauthVm.state.collectAsState()
                LummeNavGraph(oauthState = authState)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("LummeOAuth", "onNewIntent called")
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        Log.d("LummeOAuth", "handleOAuthIntent intent=$intent data=${intent?.data}")
        val data = intent?.data ?: return
        Log.d(
            "LummeOAuth",
            "deep link scheme=${data.scheme} host=${data.host} " +
                    "code=${data.getQueryParameter("code")?.take(20)} " +
                    "state=${data.getQueryParameter("state")} " +
                    "error=${data.getQueryParameter("error")}"
        )
        if (data.scheme == "lumme" && data.host == "oauth-callback") {
            val code = data.getQueryParameter("code")
            val state = data.getQueryParameter("state")
            val error = data.getQueryParameter("error")
            oauthVm.onCallback(code = code, state = state, error = error)
        }
    }
}
