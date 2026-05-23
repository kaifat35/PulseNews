package com.pulsenews.presentation.screen.subscriptions

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.pulsenews.R
import com.pulsenews.domain.entity.Article
import com.pulsenews.presentation.utils.formatDate
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SubscriptionScreen(
    onNavigationToSettings: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onOpenArticle: (String) -> Unit,
    viewModel: SubscriptionsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val savedToFavoritesText = stringResource(R.string.saved_to_favorites)
    val skippedText = stringResource(R.string.skipped)

    Scaffold(
        topBar = {
            SubscriptionTopBar(
                onRefreshDataClick = { viewModel.processCommand(SubscriptionsCommand.RefreshData) },
                onClearArticlesClick = { viewModel.processCommand(SubscriptionsCommand.ClearArticles) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.swipe_hint),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Subscriptions(
                        subscriptions = state.subscriptions,
                        query = state.query,
                        isSubscribeButtonEnabled = state.subscribeButtonEnable,
                        onQueryChanged = {
                            viewModel.processCommand(
                                SubscriptionsCommand.InputTopic(
                                    it
                                )
                            )
                        },
                        onTopicClick = {
                            viewModel.processCommand(
                                SubscriptionsCommand.ToggleTopicSelection(
                                    it
                                )
                            )
                        },
                        onDeleteSubscription = {
                            viewModel.processCommand(
                                SubscriptionsCommand.RemoveSubscription(
                                    it
                                )
                            )
                        },
                        onSubscribeButtonClick = { viewModel.processCommand(SubscriptionsCommand.ClickSubscribe) }
                    )
                }
                items(state.articles, key = { it.url }) { article ->
                    SwipeableArticleCard(
                        article = article,
                        isFavorite = article.url in state.favoriteUrls,
                        onOpenArticle = onOpenArticle,
                        onSwipe = { liked ->
                            viewModel.processCommand(
                                SubscriptionsCommand.SwipeArticle(
                                    article,
                                    liked
                                )
                            )
                            scope.launch {
                                val message = if (liked) savedToFavoritesText else skippedText
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SwipeableArticleCard(
    article: Article,
    isFavorite: Boolean,
    onOpenArticle: (String) -> Unit,
    onSwipe: (Boolean) -> Unit,
    enableSwipe: Boolean = true
) {
    var drag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(article.url, enableSwipe) {
                if (!enableSwipe) return@pointerInput
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, amt -> drag += amt },
                    onDragEnd = {
                        if (abs(drag) > 120f) {
                            val liked = drag > 0
                            onSwipe(liked)
                        }
                        drag = 0f
                    }
                )
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(drag.roundToInt(), 0) }
        ) {
            Column {
                article.imageUrl?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(article.sourceName, color = MaterialTheme.colorScheme.primary)
                        if (isFavorite) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = stringResource(R.string.favorite),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (article.author.isNotBlank()) Text(article.author, fontSize = 12.sp)
                    Text(
                        article.title,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (article.description.isNotBlank()) Text(
                        article.description,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(article.publishedAt.formatDate(), fontSize = 12.sp)
                    }
                    Button(onClick = { onOpenArticle(article.url) }) {
                        Text(stringResource(R.string.read))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionTopBar(
    onRefreshDataClick: () -> Unit,
    onClearArticlesClick: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(R.string.subscriptions_title)) },
        actions = {
            IconButton(onClick = onRefreshDataClick) {
                Icon(Icons.Default.Refresh, null)
            }
            IconButton(onClick = onClearArticlesClick) {
                Icon(Icons.Default.Clear, null)
            }
        }
    )
}

@Composable
private fun Subscriptions(
    subscriptions: Map<String, Boolean>,
    query: String,
    isSubscribeButtonEnabled: Boolean,
    onQueryChanged: (String) -> Unit,
    onTopicClick: (String) -> Unit,
    onDeleteSubscription: (String) -> Unit,
    onSubscribeButtonClick: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.what_interests_you)) }
        )
        Button(
            modifier = Modifier.fillMaxWidth()
                .padding(top = 8.dp),
            onClick = onSubscribeButtonClick,
            enabled = isSubscribeButtonEnabled
        ) {
            Text(stringResource(R.string.add_subscription_button))
        }
        if (subscriptions.isNotEmpty()) {
            LazyRow {
                subscriptions.forEach { (topic, selected) ->
                    item {
                        FilterChip(
                            selected = selected,
                            onClick = { onTopicClick(topic) },
                            label = { Text(topic) },
                            trailingIcon = {
                                IconButton(onClick = { onDeleteSubscription(topic) }) {
                                    Icon(Icons.Default.Clear, null)
                                }
                            }
                        )
                    }
                }
            }
        } else {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.no_subscriptions),
                textAlign = TextAlign.Center
            )
        }
    }
}