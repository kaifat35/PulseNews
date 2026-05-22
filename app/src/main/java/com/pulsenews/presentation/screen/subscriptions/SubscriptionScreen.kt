package com.pulsenews.presentation.screen.subscriptions

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.pulsenews.R
import com.pulsenews.domain.entity.Article
import com.pulsenews.presentation.utils.formatDate
import kotlin.math.abs

@Composable
fun SubscriptionScreen(onNavigationToSettings: () -> Unit, onOpenArticle: (String) -> Unit, viewModel: SubscriptionsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = { SubscriptionTopBar({ viewModel.processCommand(SubscriptionsCommand.RefreshData) }, { viewModel.processCommand(SubscriptionsCommand.ClearArticles) }, onNavigationToSettings) }) { innerPadding ->
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentPadding = innerPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Subscriptions(state.subscriptions, state.query, state.subscribeButtonEnable, { viewModel.processCommand(SubscriptionsCommand.InputTopic(it)) }, { viewModel.processCommand(SubscriptionsCommand.ToggleTopicSelection(it)) }, { viewModel.processCommand(SubscriptionsCommand.RemoveSubscription(it)) }, { viewModel.processCommand(SubscriptionsCommand.ClickSubscribe) }) }
            if (state.articles.isNotEmpty()) {
                item { Text("Tinder News: свайпни 👈/👉", fontWeight = FontWeight.Bold) }
                items(state.articles, key = { it.url }) { art ->
                    SwipeableArticleCard(article = art, onOpenArticle = onOpenArticle, onSwipe = { liked -> viewModel.processCommand(SubscriptionsCommand.SwipeArticle(art, liked)) })
                }
            }
        }
    }
}

@Composable
private fun SwipeableArticleCard(article: Article, onOpenArticle: (String) -> Unit, onSwipe: (Boolean) -> Unit) {
    var drag by remember { mutableStateOf(0f) }
    Card(Modifier.fillMaxWidth().pointerInput(article.url) { detectHorizontalDragGestures(onHorizontalDrag = { _, amt -> drag += amt }, onDragEnd = { if (abs(drag) > 120f) onSwipe(drag > 0); drag = 0f }) }) {
        article.imageUrl?.let { AsyncImage(model = it, contentDescription = null, modifier = Modifier.fillMaxWidth().heightIn(200.dp), contentScale = ContentScale.Crop) }
        Column(Modifier.padding(12.dp)) {
            Text(article.sourceName, color = MaterialTheme.colorScheme.primary)
            if (article.author.isNotBlank()) Text(article.author, fontSize = 12.sp)
            Text(article.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (article.description.isNotBlank()) Text(article.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(article.publishedAt.formatDate(), fontSize = 12.sp)
                Button(onClick = { onOpenArticle(article.url) }) { Text(stringResource(R.string.read)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionTopBar(onRefreshDataClick: () -> Unit, onClearArticlesClick: () -> Unit, onSettingsClick: () -> Unit) { TopAppBar(title = { Text(stringResource(R.string.subscriptions_title)) }, actions = { Icon(Modifier.clip(CircleShape).padding(8.dp), Icons.Default.Refresh, null); IconButton(onClick = onRefreshDataClick) { Icon(Icons.Default.Refresh, null) }; IconButton(onClick = onClearArticlesClick) { Icon(Icons.Default.Clear, null) }; IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, null) } }) }

@Composable
private fun Subscriptions(subscriptions: Map<String, Boolean>, query: String, isSubscribeButtonEnabled: Boolean, onQueryChanged: (String) -> Unit, onTopicClick: (String) -> Unit, onDeleteSubscription: (String) -> Unit, onSubscribeButtonClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(Modifier.fillMaxWidth(), query, onValueChange = onQueryChanged, label = { Text(stringResource(R.string.what_interests_you)) })
        Button(Modifier.fillMaxWidth(), onClick = onSubscribeButtonClick, enabled = isSubscribeButtonEnabled) { Text(stringResource(R.string.add_subscription_button)) }
        if (subscriptions.isNotEmpty()) LazyRow { subscriptions.forEach { (topic, selected) -> item { FilterChip(selected = selected, onClick = { onTopicClick(topic) }, label = { Text(topic) }, trailingIcon = { IconButton(onClick = { onDeleteSubscription(topic) }) { Icon(Icons.Default.Clear, null) } }) } } }
        else Text(modifier = Modifier.fillMaxWidth(), text = stringResource(R.string.no_subscriptions), textAlign = TextAlign.Center)
    }
}
