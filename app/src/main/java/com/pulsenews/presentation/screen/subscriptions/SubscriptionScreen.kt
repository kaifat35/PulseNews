package com.pulsenews.presentation.screen.subscriptions

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.pulsenews.R
import com.pulsenews.domain.entity.Article
import com.pulsenews.presentation.utils.formatDate
import kotlin.math.abs

@Composable
fun SubscriptionScreen(
    onNavigationToSettings: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onNavigateToFavorites: () -> Unit,
    viewModel: SubscriptionsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = {
        SubscriptionTopBar(
            { viewModel.processCommand(SubscriptionsCommand.RefreshData) },
            { viewModel.processCommand(SubscriptionsCommand.ClearArticles) },
            onNavigationToSettings,
            onNavigateToFavorites
        )
    }) { innerPadding ->
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
                        state.subscriptions,
                        state.query,
                        state.subscribeButtonEnable,
                        { viewModel.processCommand(SubscriptionsCommand.InputTopic(it)) },
                        { viewModel.processCommand(SubscriptionsCommand.ToggleTopicSelection(it)) },
                        { viewModel.processCommand(SubscriptionsCommand.RemoveSubscription(it)) },
                        { viewModel.processCommand(SubscriptionsCommand.ClickSubscribe) })
                }
                items(state.articles, key = { it.url }) { art ->
                    SwipeableArticleCard(
                        article = art,
                        isFavorite = art.url in state.favoriteUrls,
                        onOpenArticle = onOpenArticle,
                        onSwipe = { liked ->
                            viewModel.processCommand(
                                SubscriptionsCommand.SwipeArticle(
                                    art,
                                    liked
                                )
                            )
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
    var drag by remember { mutableStateOf(0f) }
    var swipeFeedback by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(swipeFeedback) {
        if (swipeFeedback != null) {
            kotlinx.coroutines.delay(500)
            swipeFeedback = null
        }
    }
    Card(
        Modifier
            .fillMaxWidth()
            .pointerInput(article.url, enableSwipe) {
                if (!enableSwipe) return@pointerInput
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, amt -> drag += amt },
                    onDragEnd = {
                        if (abs(drag) > 120f) {
                            val liked = drag > 0
                            swipeFeedback = if (liked) R.string.saved_to_favorites else R.string.skipped
                            onSwipe(liked)
                        }
                        drag = 0f
                    })
            }) {
        Box {
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
            swipeFeedback?.let { messageRes ->
                Text(
                    text = stringResource(messageRes),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer(scaleX = 1.15f, scaleY = 1.15f)
                        .alpha(0.92f)
                        .offset { IntOffset(0, -8) },
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(article.sourceName, color = MaterialTheme.colorScheme.primary)
                if (isFavorite) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                if (enableSwipe) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = null)
                }
            }
            Button(onClick = { onOpenArticle(article.url) }) { Text(stringResource(R.string.read)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionTopBar(
    onRefreshDataClick: () -> Unit,
    onClearArticlesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onFavoritesClick: () -> Unit
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
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, null)
            }
            IconButton(onClick = onFavoritesClick) {
                Icon(Icons.Default.Favorite, contentDescription = stringResource(R.string.favorites_title))
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
            modifier = Modifier.fillMaxWidth(),
            onClick = onSubscribeButtonClick,
            enabled = isSubscribeButtonEnabled
        ) { Text(stringResource(R.string.add_subscription_button)) }
        if (subscriptions.isNotEmpty()) LazyRow {
            subscriptions.forEach { (topic, selected) ->
                item {
                    FilterChip(
                        selected = selected,
                        onClick = { onTopicClick(topic) },
                        label = { Text(topic) },
                        trailingIcon = {
                            IconButton(onClick = { onDeleteSubscription(topic) }) {
                                Icon(
                                    Icons.Default.Clear,
                                    null
                                )
                            }
                        })
                }
            }
        }
        else Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.no_subscriptions),
            textAlign = TextAlign.Center
        )
    }
}
