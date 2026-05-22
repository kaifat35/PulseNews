package com.pulsenews.domain.usecase

import com.pulsenews.domain.repository.NewsRepository
import javax.inject.Inject

class ClearAllArticlesUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    suspend operator fun invoke(topics: List<String>) {
        newsRepository.clearAlArticles(topics)
    }
}