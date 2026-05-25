package io.documentnode.epub4kmp.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.documentnode.epub4kmp.domain.Book

/**
 * Shows a Material 3 card with the book's cover, title, and authors.
 */
@Composable
fun MetadataCard(book: Book, modifier: Modifier = Modifier) {
	Card(modifier = modifier.fillMaxWidth()) {
		Row(
			modifier = Modifier.padding(12.dp),
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			CoverImage(book = book, modifier = Modifier.size(width = 80.dp, height = 120.dp))
			Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
				Text(book.title.ifBlank { "(untitled)" }, style = MaterialTheme.typography.titleLarge)
				val authors = book.metadata.getAuthors().joinToString { author ->
					listOf(author.firstname, author.lastname).joinToString(" ").trim()
				}
				if (authors.isNotBlank()) {
					Text(authors, style = MaterialTheme.typography.bodyMedium)
				}
				val lang = book.metadata.language
				if (lang.isNotBlank()) {
					Text(lang, style = MaterialTheme.typography.bodySmall)
				}
			}
		}
	}
}
