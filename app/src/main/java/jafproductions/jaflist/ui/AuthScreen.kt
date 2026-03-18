package jafproductions.jaflist.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import jafproductions.jaflist.R
import jafproductions.jaflist.ui.theme.JAFListTheme
import jafproductions.jaflist.viewmodels.AuthViewModel

@Composable
fun AuthScreen(authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                authViewModel.handleSignInResult(idToken)
            } else {
                authViewModel.setError("Sign-in failed: no ID token received.")
            }
        } catch (e: ApiException) {
            if (e.statusCode != 12501) {
                authViewModel.setError("Sign-in failed (${e.statusCode}). Please try again.")
            }
        }
    }

    AuthScreenContent(
        errorMessage = errorMessage,
        isLoading = isLoading,
        onSignIn = { launcher.launch(authViewModel.getSignInIntent(context)) }
    )
}

@Composable
private fun AuthScreenContent(
    errorMessage: String?,
    isLoading: Boolean,
    onSignIn: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckBox,
                contentDescription = "JAFList",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "JAFList",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (errorMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            color = Color(0xFFD32F2F),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                OutlinedButton(
                    onClick = onSignIn,
                    modifier = Modifier
                        .width(280.dp)
                        .height(44.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google",
                        modifier = Modifier.size(18.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign in with Google",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "AuthScreen - Sign In", showSystemUi = true)
@Composable
private fun AuthScreenPreview() {
    JAFListTheme(dynamicColor = false) {
        AuthScreenContent(errorMessage = null, isLoading = false, onSignIn = {})
    }
}

@Preview(showBackground = true, name = "AuthScreen - Loading", showSystemUi = true)
@Composable
private fun AuthScreenLoadingPreview() {
    JAFListTheme(dynamicColor = false) {
        AuthScreenContent(errorMessage = null, isLoading = true, onSignIn = {})
    }
}

@Preview(showBackground = true, name = "AuthScreen - Error", showSystemUi = true)
@Composable
private fun AuthScreenErrorPreview() {
    JAFListTheme(dynamicColor = false) {
        AuthScreenContent(
            errorMessage = "Sign-in failed (7). Please try again.",
            isLoading = false,
            onSignIn = {}
        )
    }
}
