package io.documentnode.epub4kmp.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.documentnode.epub4kmp.domain.Book
import io.documentnode.epub4kmp.domain.TOCReference

/**
 * Renders [book]'s table of contents as a nested Material 3 list. Tapping
 * an entry invokes [onSelect].
 */
@Composable
fun TableOfContents(
	book: Book,
	onSelect: (TOCReference) -> Unit,
	modifier: Modifier = Modifier,
) {
	val entries = flatten(book.tableOfContents.getTocReferences(), depth = 0)
	LazyColumn(modifier = modifier) {
		items(entries.size) { index ->
			val ref = entries[index].first
			val depth = entries[index].second
			TextButton(
				onClick = { onSelect(ref) },
				modifier = Modifier.padding(start = (depth * 12).dp),
			) {
				Text(ref.title.orEmpty().ifBlank { "(untitled)" })
			}
			HorizontalDivider()
		}
	}
}

private fun flatten(
	refs: List<TOCReference>,
	depth: Int,
	out: MutableList<Pair<TOCReference, Int>> = mutableListOf(),
): List<Pair<TOCReference, Int>> {
	for (ref in refs) {
		out.add(ref to depth)
		if (ref.children.isNotEmpty()) flatten(ref.children, depth + 1, out)
	}
	return out
}
