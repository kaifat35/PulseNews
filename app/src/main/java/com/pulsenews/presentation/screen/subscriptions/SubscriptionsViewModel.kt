package com.pulsenews.presentation.screen.subscriptions

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulsenews.domain.entity.Article
import com.pulsenews.domain.usecase.AddSubscriptionsUseCase
import com.pulsenews.domain.usecase.ClearAllArticlesUseCase
import com.pulsenews.domain.usecase.GetAllSubscriptionsUseCase
import com.pulsenews.domain.usecase.GetArticlesByTopicsUseCase
import com.pulsenews.domain.usecase.RemoveSubscriptionsUseCase
import com.pulsenews.domain.usecase.UpdateSubscribedArticlesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val addSubscriptionsUseCase: AddSubscriptionsUseCase,
    private val clearAllArticlesUseCase: ClearAllArticlesUseCase,
    private val getAllSubscriptionsUseCase: GetAllSubscriptionsUseCase,
    private val getArticlesByTopicsUseCase: GetArticlesByTopicsUseCase,
    private val removeSubscriptionsUseCase: RemoveSubscriptionsUseCase,
    private val updateSubscribedArticlesUseCase: UpdateSubscribedArticlesUseCase,
    private val preferences: SharedPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionsState())
    val state = _state.asStateFlow()

    init {
        _state.update {
            it.copy(favoriteUrls = preferences.getStringSet("favorites", emptySet()) ?: emptySet())
        }
        observeSubscriptions()
        observeSelectedTopics()
    }

    fun processCommand(command: SubscriptionsCommand) {
        when (command) {
            SubscriptionsCommand.ClearArticles -> viewModelScope.launch {
                clearAllArticlesUseCase(
                    state.value.selectedTopics
                )
            }

            SubscriptionsCommand.ClickSubscribe -> viewModelScope.launch {
                addSubscriptionsUseCase(state.value.query.trim())
                _state.update { it.copy(query = "") }
            }

            is SubscriptionsCommand.InputTopic -> _state.update { it.copy(query = command.query) }
            SubscriptionsCommand.RefreshData -> viewModelScope.launch { updateSubscribedArticlesUseCase() }
            is SubscriptionsCommand.RemoveSubscription -> viewModelScope.launch {
                removeSubscriptionsUseCase(
                    command.topic
                )
            }

            is SubscriptionsCommand.ToggleTopicSelection -> _state.update {
                val subscriptions = it.subscriptions.toMutableMap()
                subscriptions[command.topic] = !(subscriptions[command.topic] ?: false)
                it.copy(subscriptions = subscriptions)
            }

            is SubscriptionsCommand.SwipeArticle -> onArticleSwiped(command.article, command.liked)
        }
    }

    private fun onArticleSwiped(article: Article, liked: Boolean) {
        val tokens = tokenize(article)
        preferences.edit().apply {
            val setKey = if (liked) "favorites" else "disliked"
            putStringSet(
                setKey,
                (preferences.getStringSet(setKey, emptySet()) ?: emptySet()) + article.url
            )
            tokens.forEach { token ->
                val key = "kw_$token"
                val current = preferences.getInt(key, 0)
                putInt(key, current + if (liked) 2 else -1)
            }
        }.apply()

        val favorites = preferences.getStringSet("favorites", emptySet()) ?: emptySet()
        _state.update { current ->
            val dismissed = current.dismissedUrls + article.url
            current.copy(
                favoriteUrls = favorites,
                dismissedUrls = dismissed,
                articles = current.articles
                    .filterNot { it.url in dismissed }
                    .sortedWith(compareByDescending<Article> { score(it) }.thenByDescending { it.publishedAt })
            )
        }
    }

    private fun score(article: Article): Int =
        tokenize(article).sumOf { preferences.getInt("kw_$it", 0) }

    private fun tokenize(article: Article): List<String> =
        (article.title + " " + article.description).lowercase()
            .split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.length > 2 }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSelectedTopics() {
        state.map { it.selectedTopics }
            .distinctUntilChanged()
            .flatMapLatest { getArticlesByTopicsUseCase(it) }
            .onEach { articles ->
                _state.update { state ->
                    state.copy(
                        articles = articles
                        .filterNot { it.url in state.dismissedUrls }
                        .sortedWith(
                            compareByDescending<Article> { score(it) }
                                .thenByDescending { it.publishedAt }
                        ))
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeSubscriptions() {
        getAllSubscriptionsUseCase().onEach { subscriptions ->
            _state.update { prev ->
                prev.copy(subscriptions = subscriptions.associateWith {
                    prev.subscriptions[it] ?: true
                })
            }
        }.launchIn(viewModelScope)
    }
}

sealed interface SubscriptionsCommand {
    data class InputTopic(val query: String) : SubscriptionsCommand
    data object ClickSubscribe : SubscriptionsCommand
    data object RefreshData : SubscriptionsCommand
    data class ToggleTopicSelection(val topic: String) : SubscriptionsCommand
    data object ClearArticles : SubscriptionsCommand
    data class RemoveSubscription(val topic: String) : SubscriptionsCommand
    data class SwipeArticle(val article: Article, val liked: Boolean) : SubscriptionsCommand
}

data class SubscriptionsState(
    val query: String = "",
    val subscriptions: Map<String, Boolean> = mapOf(),
    val articles: List<Article> = listOf(),
    val favoriteUrls: Set<String> = emptySet(),
    val dismissedUrls: Set<String> = emptySet()
) {
    val subscribeButtonEnable: Boolean get() = query.isNotBlank()
    val selectedTopics: List<String> get() = subscriptions.filter { it.value }.map { it.key }
}
