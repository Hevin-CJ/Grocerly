package com.example.grocerly.model

import android.os.Parcelable
import com.example.grocerly.utils.CancellationStatus
import com.example.grocerly.utils.CancelledBy
import kotlinx.parcelize.Parcelize

@Parcelize
data class CancellationInfo(
    val cancellationStatus: CancellationStatus = CancellationStatus.Non_Cancelled,
    val cancelledBy: CancelledBy = CancelledBy.NONE,
    val cancelledAt: Long = 0L,
    val reason: String? = null
): Parcelable