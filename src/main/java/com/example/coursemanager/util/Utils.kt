package com.example.coursemanager.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import retrofit2.HttpException
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T> safeCall(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> "Lỗi máy chủ (mã ${code()}). Vui lòng thử lại."
    is IOException -> "Không kết nối được máy chủ. Hãy kiểm tra mạng rồi thử lại."
    else -> message ?: "Đã xảy ra lỗi không xác định."
}

fun Double.toVnd(): String =
    NumberFormat.getNumberInstance(Locale("vi", "VN")).format(toLong()) + " đ"

fun Double.clean(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

fun ImageView.loadUrl(url: String?) {
    Glide.with(this)
        .load(url)
        .placeholder(android.R.drawable.ic_menu_gallery)
        .error(android.R.drawable.ic_menu_report_image)
        .centerCrop()
        .into(this)
}
