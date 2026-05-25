package io.documentnode.epub4kmp.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.documentnode.epub4kmp.domain.Book
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

/**
 * Renders [book]'s cover image, if it has one. Renders nothing if the book
 * has no cover.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun CoverImage(
	book: Book,
	modifier: Modifier = Modifier,
	contentScale: ContentScale = ContentScale.Fit,
) {
	val cover = book.coverImage ?: return
	val bitmap = remember(cover) { runCatching { cover.bytes().decodeToImageBitmap() }.getOrNull() }
	if (bitmap != null) {
		Image(
			bitmap = bitmap,
			contentDescription = book.title.ifBlank { "Book cover" },
			modifier = modifier,
			contentScale = contentScale,
		)
	} else {
		Box(modifier = modifier)
	}
}
