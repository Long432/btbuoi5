package com.example.coursemanager.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @GET("categories") suspend fun getCategories(): List<Category>
    @POST("categories") suspend fun createCategory(@Body category: Category): Category
    @PUT("categories/{id}") suspend fun updateCategory(@Path("id") id: String, @Body category: Category): Category
    @DELETE("categories/{id}") suspend fun deleteCategory(@Path("id") id: String): Category

    @GET("courses") suspend fun getCourses(): List<Course>
    @GET("courses/{id}") suspend fun getCourse(@Path("id") id: String): Course
    @POST("courses") suspend fun createCourse(@Body course: Course): Course
    @PUT("courses/{id}") suspend fun updateCourse(@Path("id") id: String, @Body course: Course): Course
    @DELETE("courses/{id}") suspend fun deleteCourse(@Path("id") id: String): Course
}

object ApiClient {
    private const val BASE_URL = "https://6acdfd5af338359cf1c2d1dd.mockapi.io"

    val api: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

suspend fun ApiService.categoriesOrEmpty(): List<Category> =
    try { getCategories() } catch (e: HttpException) { if (e.code() == 404) emptyList() else throw e }

suspend fun ApiService.coursesOrEmpty(): List<Course> =
    try { getCourses() } catch (e: HttpException) { if (e.code() == 404) emptyList() else throw e }
