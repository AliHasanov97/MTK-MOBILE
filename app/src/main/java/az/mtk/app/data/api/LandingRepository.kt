package az.mtk.app.data.api

import az.mtk.app.AppGraph

class LandingRepository {
    private val articlesApi = AppGraph.service(ArticlesApi::class.java)
    private val companyApi = AppGraph.service(CompanyApi::class.java)

    suspend fun getLatestArticles(count: Int = 5): List<ArtArticle> {
        return try {
            articlesApi.latestArticles(count).data ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getArticle(id: String): ArtArticle? {
        return try {
            articlesApi.article(id).data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCompanyProfile(): CompanyProfile? {
        return try {
            companyApi.getPublicProfile().data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCompanyName(): String {
        return getCompanyProfile()?.name ?: "MTK"
    }
}
