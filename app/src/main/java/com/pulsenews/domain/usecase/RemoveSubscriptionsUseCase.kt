package com.pulsenews.domain.usecase

import com.pulsenews.domain.repository.NewsRepository
import javax.inject.Inject

class RemoveSubscriptionsUseCase @Inject constructor(
    private val newsRepository: NewsRepository
) {
    suspend operator fun invoke(topic: String) {
        newsRepository.removeSubscription(topic)
    }
}