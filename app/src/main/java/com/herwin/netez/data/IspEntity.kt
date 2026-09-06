package com.herwin.netez.data

import androidx.room.*

@Entity(tableName = "isp_ratings")
data class IspRating(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ispName: String,
    val city: String,
    val avgDownload: Double,
    val avgUpload: Double,
    val avgPing: Double,
    val pricePerMbps: Double,
    val rating: Float,
    val reviewCount: Int,
    val lastUpdated: Long = System.currentTimeMillis(),
    val pros: String = "",
    val cons: String = ""
)

@Entity(
    tableName = "isp_reviews",
    foreignKeys = [
        ForeignKey(
            entity = IspRating::class,
            parentColumns = ["id"],
            childColumns = ["ispId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ispId")]
)
data class IspReview(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ispId: Int,
    val userName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val rating: Float,
    val downloadActual: Double,
    val uploadActual: Double,
    val pingActual: Double,
    val pricePerMonth: Double,
    val comment: String
)

@Dao
interface IspDao {
    @Query("SELECT * FROM isp_ratings WHERE city = :city ORDER BY rating DESC")
    suspend fun getIspsByCity(city: String): List<IspRating>

    @Query("SELECT * FROM isp_ratings WHERE id = :id")
    suspend fun getIspById(id: Int): IspRating?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIsp(isp: IspRating): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIsps(isps: List<IspRating>)

    @Query("SELECT * FROM isp_reviews WHERE ispId = :ispId ORDER BY timestamp DESC")
    suspend fun getReviewsForIsp(ispId: Int): List<IspReview>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: IspReview)

    @Query("SELECT COUNT(*) FROM isp_ratings")
    suspend fun getIspCount(): Int

    @Transaction
    suspend fun addReviewAndUpdateRating(review: IspReview) {
        insertReview(review)
        val reviews = getReviewsForIsp(review.ispId)
        val newRating = reviews.map { it.rating }.average().toFloat()
        val newDownload = reviews.map { it.downloadActual }.average()
        val newUpload = reviews.map { it.uploadActual }.average()
        val newPing = reviews.map { it.pingActual }.average()
        
        val isp = getIspById(review.ispId)
        if (isp != null) {
            insertIsp(isp.copy(
                rating = newRating,
                reviewCount = reviews.size,
                avgDownload = newDownload,
                avgUpload = newUpload,
                avgPing = newPing,
                lastUpdated = System.currentTimeMillis()
            ))
        }
    }
}
