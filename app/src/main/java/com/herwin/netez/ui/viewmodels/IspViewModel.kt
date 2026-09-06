package com.herwin.netez.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herwin.netez.data.AppDatabase
import com.herwin.netez.data.IspRating
import com.herwin.netez.data.IspReview
import com.herwin.netez.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class IspComparisonState(
    val isps: List<IspRating> = emptyList(),
    val currentIspName: String = "",
    val currentCity: String = "Tangerang",
    val isLoading: Boolean = false,
    val selectedIspReviews: List<IspReview> = emptyList(),
    val bestValueIsp: IspRating? = null
)

class IspViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val ispDao = db.ispDao()

    private val _uiState = MutableStateFlow(IspComparisonState())
    val uiState: StateFlow<IspComparisonState> = _uiState

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val ispData = NetworkUtils.getIspData()
            val city = ispData["city"] ?: "Tangerang"
            val myIsp = ispData["isp"] ?: ""
            
            _uiState.update { it.copy(currentCity = city, currentIspName = myIsp) }

            if (ispDao.getIspCount() == 0) {
                seedInitialData(city)
            }

            refreshIsps(city)
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun refreshIsps(city: String) {
        val list = ispDao.getIspsByCity(city)
        val best = list.maxByOrNull { (it.avgDownload / it.pricePerMbps) * it.rating }
        _uiState.update { it.copy(isps = list, bestValueIsp = best) }
    }

    private suspend fun seedInitialData(city: String) {
        val initialIsps = listOf(
            IspRating(
                ispName = "Telkom Indihome", 
                city = city, 
                avgDownload = 50.0, 
                avgUpload = 15.0, 
                avgPing = 25.0, 
                pricePerMbps = 5600.0, 
                rating = 3.8f, 
                reviewCount = 120,
                pros = "Jangkauan luas, Tersedia di pelosok, Bundling TV stabil",
                cons = "Upload tidak simetris, FUP ketat, Latensi internasional fluktuatif"
            ),
            IspRating(
                ispName = "Biznet Home", 
                city = city, 
                avgDownload = 85.0, 
                avgUpload = 85.0, 
                avgPing = 12.0, 
                pricePerMbps = 4100.0, 
                rating = 4.5f, 
                reviewCount = 85,
                pros = "Kecepatan Simetris (1:1), Latensi gaming sangat rendah, Tanpa FUP",
                cons = "Coverage belum merata, Harga router awal cukup tinggi"
            ),
            IspRating(
                ispName = "MyRepublic", 
                city = city, 
                avgDownload = 75.0, 
                avgUpload = 35.0, 
                avgPing = 18.0, 
                pricePerMbps = 4500.0, 
                rating = 4.2f, 
                reviewCount = 64,
                pros = "Paket gaming dedicated, IP Public tersedia, Harga kompetitif",
                cons = "Customer service slow response, Kabel sering maintenance"
            ),
            IspRating(
                ispName = "First Media", 
                city = city, 
                avgDownload = 40.0, 
                avgUpload = 10.0, 
                avgPing = 30.0, 
                pricePerMbps = 6200.0, 
                rating = 3.5f, 
                reviewCount = 95,
                pros = "Saluran TV kabel terbaik, Kualitas gambar HD jernih",
                cons = "Masih menggunakan Coaxial, Sering gangguan cuaca, Upload lambat"
            ),
            IspRating(
                ispName = "Oxygen.id", 
                city = city, 
                avgDownload = 60.0, 
                avgUpload = 60.0, 
                avgPing = 15.0, 
                pricePerMbps = 4800.0, 
                rating = 4.0f, 
                reviewCount = 42,
                pros = "Full Fiber Optic, Stabil untuk WFH, CS responsif",
                cons = "Coverage sangat terbatas, Jarang promo"
            )
        )
        ispDao.insertIsps(initialIsps)
    }

    fun loadReviews(ispId: Int) {
        viewModelScope.launch {
            val reviews = ispDao.getReviewsForIsp(ispId)
            _uiState.update { it.copy(selectedIspReviews = reviews) }
        }
    }

    fun submitReview(
        ispId: Int,
        rating: Float,
        download: Double,
        upload: Double,
        ping: Double,
        price: Double,
        comment: String
    ) {
        viewModelScope.launch {
            val review = IspReview(
                ispId = ispId,
                userName = "User anonim",
                rating = rating,
                downloadActual = download,
                uploadActual = upload,
                pingActual = ping,
                pricePerMonth = price,
                comment = comment
            )
            ispDao.addReviewAndUpdateRating(review)
            refreshIsps(_uiState.value.currentCity)
            loadReviews(ispId)
        }
    }
}
