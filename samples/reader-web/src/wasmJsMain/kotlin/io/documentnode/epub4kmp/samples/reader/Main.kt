package io.documentnode.epub4kmp.samples.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import io.documentnode.epub4kmp.compose.EpubReader
import io.documentnode.epub4kmp.domain.Book
import io.documentnode.epub4kmp.domain.Stylesheets
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.browser.document
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import okio.Buffer
import io.documentnode.epub4kmp.epub.EpubReader as EpubReaderIo

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
	ComposeViewport(document.body!!) {
		MaterialTheme(colorScheme = darkColorScheme()) {
			App()
		}
	}
}

private sealed interface AppState {
	data object PickBook : AppState
	data object Loading : AppState
	data class WithBook(val book: Book) : AppState
	data class Error(val message: String) : AppState
}

@Composable
private fun App() {
	var state: AppState by remember { mutableStateOf(AppState.PickBook) }
	// Scope is owned by App, which stays composed across the PickBook -> Loading
	// transition. Scoping the read to PickBookScreen instead would cancel it the
	// moment that screen leaves the composition.
	val scope = rememberCoroutineScope()

	Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
		when (val s = state) {
			AppState.PickBook -> PickBookScreen(
				onFilePicked = { file ->
					state = AppState.Loading
					scope.launch {
						// The browser has no real filesystem, so read the picked
						// file into memory and feed the bytes to the core reader
						// as an okio Source.
						try {
							val bytes = file.readBytes()
							val source = Buffer().apply { write(bytes) }
							val book = EpubReaderIo().readEpub(source = source)
							book.addStylesheet(Stylesheets.defaultReader())
							state = AppState.WithBook(book)
						} catch (e: CancellationException) {
							throw e
						} catch (e: Throwable) {
							state = AppState.Error(e.message ?: e::class.simpleName.orEmpty())
						}
					}
				},
			)

			AppState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
				CircularProgressIndicator()
			}

			is AppState.WithBook -> EpubReader(book = s.book, modifier = Modifier.fillMaxSize())

			is AppState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
				Text("Failed to open book: ${s.message}")
			}
		}
	}
}

@Composable
private fun PickBookScreen(
	onFilePicked: (PlatformFile) -> Unit,
) {
	val launcher = rememberFilePickerLauncher(
		type = FileKitType.File(extensions = listOf("epub")),
	) { file: PlatformFile? ->
		if (file != null) onFilePicked(file)
	}

	Box(Modifier.fillMaxSize(), Alignment.Center) {
		Button(onClick = { launcher.launch() }) {
			Text("Open EPUB…")
		}
	}
}
