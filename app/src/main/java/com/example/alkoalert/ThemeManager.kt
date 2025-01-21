import androidx.compose.runtime.*

val LocalDarkMode = compositionLocalOf { mutableStateOf(false) }

@Composable
fun ProvideDarkMode(
    isDarkMode: Boolean,
    content: @Composable () -> Unit
) {
    val darkModeState = remember { mutableStateOf(isDarkMode) }
    CompositionLocalProvider(LocalDarkMode provides darkModeState) {
        content()
    }
}
