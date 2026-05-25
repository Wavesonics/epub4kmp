package io.documentnode.epub4kmp.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.documentnode.epub4kmp.browsersupport.NavigationEvent
import io.documentnode.epub4kmp.browsersupport.NavigationEventListener
import io.documentnode.epub4kmp.browsersupport.Navigator
import io.documentnode.epub4kmp.domain.Book
import io.documentnode.epub4kmp.domain.Resource
import io.documentnode.epub4kmp.domain.TOCReference

/**
 * State holder for [EpubReader]. Wraps the core [Navigator] and exposes its
 * current position as snapshot state so Compose recomposes on chapter changes.
 */
class EpubReaderState internal constructor(
	internal val book: Book,
	internal val navigator: Navigator,
) {
	// Bumped on every navigation so Compose recomposes; the actual current
	// resource is read from the navigator.
	internal var navTick by mutableStateOf(0)
		private set

	init {
		if (navigator.book !== book) navigator.book = book
		val firstSpine = book.spine.getResource(0)
		if (firstSpine != null) navigator.gotoResource(firstSpine, this)
		navigator.addNavigationEventListener(object : NavigationEventListener {
			override fun navigationPerformed(navigationEvent: NavigationEvent) {
				navTick++
			}
		})
	}

	val currentResource: Resource?
		get() = navigator.currentResource

	/** Last `#fragment` requested by [goto] / [gotoResource]. */
	val currentFragmentId: String?
		get() = navigator.currentFragmentId?.ifEmpty { null }

	val hasPrevious: Boolean get() = navigator.hasPreviousSpineSection()
	val hasNext: Boolean get() = navigator.hasNextSpineSection()

	fun next() {
		navigator.gotoNextSpineSection(this)
	}

	fun previous() {
		navigator.gotoPreviousSpineSection(this)
	}

	fun goto(ref: TOCReference) {
		val resource = ref.resource ?: return
		navigator.gotoResource(resource, ref.fragmentId.orEmpty(), this)
	}

	fun gotoHref(href: String) {
		navigator.gotoResource(href, this)
	}

	/**
	 * Navigate to [resource], optionally scrolling to the named [fragmentId]
	 * within the chapter (the part after `#`).
	 */
	fun gotoResource(resource: Resource, fragmentId: String? = null) {
		navigator.gotoResource(resource, fragmentId.orEmpty(), this)
	}
}

/**
 * Remembers a [EpubReaderState] tied to [book]. A new state is created when
 * [book] changes.
 */
@Composable
fun rememberEpubReaderState(book: Book): EpubReaderState =
	remember(book) { EpubReaderState(book, Navigator(book)) }

/**
 * Batteries-included EPUB reader. Wires the TOC, prev/next controls, and the
 * [EpubContent] WebView surface around a [Navigator]. Use the building-block
 * composables ([EpubContent], [TableOfContents], [CoverImage], [MetadataCard])
 * if you want to compose your own layout.
 */
@Composable
fun EpubReader(
	book: Book,
	modifier: Modifier = Modifier,
	state: EpubReaderState = rememberEpubReaderState(book),
) {
	val current by remember(state) {
		derivedStateOf {
			@Suppress("UNUSED_EXPRESSION") state.navTick
			state.currentResource
		}
	}
	val currentFragment by remember(state) {
		derivedStateOf {
			@Suppress("UNUSED_EXPRESSION") state.navTick
			state.currentFragmentId
		}
	}
	var tocOpen by rememberSaveable { mutableStateOf(false) }

	Row(modifier = modifier.fillMaxSize()) {
		if (tocOpen) {
			Box(modifier = Modifier.width(260.dp).fillMaxSize()) {
				TableOfContents(
					book = book,
					onSelect = { ref ->
						state.goto(ref)
						tocOpen = false
					},
					modifier = Modifier.fillMaxSize().padding(8.dp),
				)
			}
			VerticalDivider()
		}
		Column(modifier = Modifier.fillMaxSize()) {
			ReaderTopBar(
				title = current?.let { resolveChapterTitle(book, it) } ?: book.title,
				onToggleToc = { tocOpen = !tocOpen },
			)
			HorizontalDivider()
			Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
				val resource = current
				if (resource != null) {
					EpubContent(
						book = book,
						resource = resource,
						modifier = Modifier.fillMaxSize(),
						fragmentId = currentFragment,
						onLinkClicked = { click ->
							val target = click.resource ?: return@EpubContent
							state.gotoResource(target, click.fragmentId)
						},
					)
				}
			}
			HorizontalDivider()
			ReaderBottomBar(state = state)
		}
	}
}

@Composable
private fun ReaderTopBar(title: String, onToggleToc: () -> Unit) {
	Row(
		modifier = Modifier.fillMaxWidth().padding(8.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(8.dp),
	) {
		Button(onClick = onToggleToc) { Text("TOC") }
		Text(title, style = MaterialTheme.typography.titleMedium)
	}
}

/**
 * Walks the book's TOC looking for an entry whose resource matches [resource]
 * (by href). Returns the entry's title if found, else the book title, else the
 * resource id as a last resort.
 */
private fun resolveChapterTitle(book: Book, resource: Resource): String {
	val href = resource.href
	if (href != null) {
		fun search(refs: List<TOCReference>): String? {
			for (ref in refs) {
				if (ref.resource?.href == href) {
					val title = ref.title
					if (!title.isNullOrBlank()) return title
				}
				val nested = search(ref.children)
				if (nested != null) return nested
			}
			return null
		}
		search(book.tableOfContents.getTocReferences())?.let { return it }
	}
	if (book.title.isNotBlank()) return book.title
	return resource.id.orEmpty()
}

@Composable
private fun ReaderBottomBar(state: EpubReaderState) {
	Row(
		modifier = Modifier.fillMaxWidth().padding(8.dp),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
	) {
		Button(onClick = { state.previous() }, enabled = state.hasPrevious) { Text("Previous") }
		Button(onClick = { state.next() }, enabled = state.hasNext) { Text("Next") }
	}
	LaunchedEffect(state.navTick) { /* drives recompose on nav */ }
}
