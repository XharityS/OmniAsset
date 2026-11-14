package ru.echosystems.omni.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.LogIn
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.User
import ru.echosystems.omni.app.ui.OmniBackground

@Composable
fun AuthScreen(
    state: AuthUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLogin: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OmniBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(Modifier.padding(28.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Lucide.Package, null, tint = MaterialTheme.colorScheme.onPrimary) }
                        Spacer(Modifier.size(14.dp))
                        Column {
                            Text("OmniAsset", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Text("Учёт компьютерной техники", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    Text("Вход в систему", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text("Введите данные администратора или сотрудника", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(22.dp))

                    OutlinedTextField(
                        value = state.username,
                        onValueChange = onUsernameChanged,
                        label = { Text("Имя пользователя") },
                        leadingIcon = { Icon(Lucide.User, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = onPasswordChanged,
                        label = { Text("Пароль") },
                        leadingIcon = { Icon(Lucide.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Lucide.EyeOff else Lucide.Eye, null)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    state.errorMessage?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(Modifier.height(22.dp))
                    Button(onClick = onLogin, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Lucide.LogIn, null, tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.size(8.dp))
                            Text("Войти")
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("OmniAsset • inventory management", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
