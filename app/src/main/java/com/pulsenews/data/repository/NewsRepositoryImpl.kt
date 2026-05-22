package com.pulsenews.data.repository

import android.util.Log
import androidx.work.Constraints

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pulsenews.data.background.RefreshDataWorker
import com.pulsenews.data.local.ArticleDbModel
import com.pulsenews.data.local.NewsDao
import com.pulsenews.data.local.SubscriptionDbModel
import com.pulsenews.data.mapper.toDbModel
import com.pulsenews.data.mapper.toEntities
import com.pulsenews.data.mapper.toQueryParam
import com.pulsenews.data.remote.NewsApiService
import com.pulsenews.domain.entity.Article
import com.pulsenews.domain.entity.Language
import com.pulsenews.domain.entity.RefreshConfig
import com.pulsenews.domain.repository.NewsRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class NewsRepositoryImpl @Inject constructor(
    private val newsDao: NewsDao,
    private val newsApiService: NewsApiService,
    private val workManager: WorkManager
) : NewsRepository {

    override fun getAllSubscription(): Flow<List<String>> {
        return newsDao.getAllSubscriptions().map { subscriptions ->
            subscriptions.map { it.topic }
        }
    }

    override suspend fun addSubscription(topic: String) {
        newsDao.addSubscription(SubscriptionDbModel(topic))
    }

    override suspend fun updateArticlesForTopic(topic: String, language: Language): Boolean {
        val articles = loadArticles(topic, language)
        val ids = newsDao.addArticles(articles)
        return ids.any { it != -1L }
    }

    private suspend fun loadArticles(topic: String, language: Language): List<ArticleDbModel> {
        return try {
            newsApiService.loadArticles(topic, language.toQueryParam()).toDbModel(topic)
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            Log.e("NewsRepository", e.stackTraceToString())
            listOf()
        }
    }

    override suspend fun removeSubscription(topic: String) {
        newsDao.deleteSubscription(SubscriptionDbModel(topic))
    }

    override suspend fun updateArticlesForAllSubscriptions(language: Language): List<String> {
        val updatesTopics = mutableListOf<String>()
        val subscriptions = newsDao.getAllSubscriptions().first()
        coroutineScope {
            subscriptions.forEach {
                launch {
                    val updated = updateArticlesForTopic(it.topic, language)
                    if (updated) {
                        updatesTopics.add(it.topic)
                    }
                }
            }
        }
        return updatesTopics
    }

    override fun getArticlesByTopics(topics: List<String>): Flow<List<Article>> {
        return newsDao.getAllArticlesByTopics(topics).map {
            it.toEntities()
        }
    }

    override fun startBackgroundRefresh(refreshConfig: RefreshConfig) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (refreshConfig.wifiOnly) {
                    NetworkType.UNMETERED
                } else {
                    NetworkType.CONNECTED
                }
            )
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<RefreshDataWorker>(
            refreshConfig.interval.minutes.toLong(),
            TimeUnit.MINUTES
        ).setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName = "Refresh data",
            existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request = request
        )
    }

    override suspend fun clearAlArticles(topics: List<String>) {
        newsDao.deleteArticlesByTopics(topics)
    }
}