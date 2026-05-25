package io.documentnode.epub4kmp.samples.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.documentnode.epub4kmp.compose.EpubReader
import io.documentnode.epub4kmp.domain.Book
import io.documentnode.epub4kmp.domain.Stylesheets
import io.github.kdroidfilter.nucleus.darkmodedetector.isSystemInDarkMode
import io.github.kdroidfilter.nucleus.window.material.MaterialDecoratedWindow
import io.github.kdroidfilter.nucleus.window.material.MaterialTitleBar
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import okio.FileSystem
import okio.Path.Companion.toPath
import io.documentnode.epub4kmp.epub.EpubReader as EpubReaderIo

fun main() = application {
	val windowState = rememberWindowState(
		size = DpSize(800.dp, 900.dp),
	)
	val colorScheme = if (isSystemInDarkMode()) darkColorScheme() else lightColorScheme()

	MaterialTheme(colorScheme = colorScheme) {
		MaterialDecoratedWindow(
			onCloseRequest = ::exitApplication,
			state = windowState,
			title = "epub4kmp reader",
		) {
			MaterialTitleBar { _ ->
				Text(
					"epub4kmp reader",
					modifier = Modifier.align(Alignment.CenterHorizontally),
					color = MaterialTheme.colorScheme.onSurface,
				)
			}
			App()
		}
	}
}

private sealed interface AppState {
	data object PickBook : AppState
	data class WithBook(val book: Book) : AppState
	data class Error(val message: String) : AppState
}

@Composable
private fun App() {
	var state: AppState by remember { mutableStateOf(AppState.PickBook) }

	Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
		when (val s = state) {
			AppState.PickBook -> PickBookScreen(
				onPicked = { book -> state = AppState.WithBook(book) },
				onError = { msg -> state = AppState.Error(msg) },
			)

			is AppState.WithBook -> EpubReader(book = s.book, modifier = Modifier.fillMaxSize())
			is AppState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
				Text("Failed to open book: ${s.message}")
			}
		}
	}
}

@Composable
private fun PickBookScreen(
	onPicked: (Book) -> Unit,
	onError: (String) -> Unit,
) {
	val launcher = rememberFilePickerLauncher(
		type = FileKitType.File(extensions = listOf("epub")),
	) { file: PlatformFile? ->
		if (file == null) return@rememberFilePickerLauncher
		runCatching {
			val book = EpubReaderIo().readEpub(FileSystem.SYSTEM, file.path.toPath())
			book.addStylesheet(Stylesheets.defaultReader())
			book
		}.fold(
			onSuccess = onPicked,
			onFailure = { onError(it.message ?: it::class.simpleName.orEmpty()) },
		)
	}

	Box(Modifier.fillMaxSize(), Alignment.Center) {
		Button(onClick = { launcher.launch() }) {
			Text("Open EPUB…")
		}
	}
}
