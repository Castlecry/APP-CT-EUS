package com.example.cteus.data.api

import android.util.Log
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.net.InetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RetrofitClient {
    private const val TAG = "RetrofitClient"
    private const val BASE_HOST = "10.134.41.115"
    private const val BASE_URL = "http://$BASE_HOST:8000/"
    private var token: String? = null

    fun setToken(newToken: String?) {
        token = newToken
    }

    // 自定义 DNS 解析，仅供参考，对于 OSS 链接通常不生效
    private val dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return if (hostname == "oss") {
                InetAddress.getAllByName(BASE_HOST).toList()
            } else {
                Dns.SYSTEM.lookup(hostname)
            }
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val url = request.url.toString()
        
        val builder = request.newBuilder()
        
        // 仅当请求发往我们的 API 服务器时才添加 Authorization 头
        // 外部链接（如 OSS）自带签名，添加 Bearer Token 会导致 403
        if (url.startsWith(BASE_URL)) {
            token?.let {
                builder.addHeader("Authorization", "Bearer $it")
            }
        } else {
            Log.d(TAG, "Skipping Auth header for external URL: $url")
        }
        
        chain.proceed(builder.build())
    }

    private val responseInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        val body = response.body
        
        if (response.isSuccessful && body != null) {
            val contentType = body.contentType()
            val isJson = contentType?.toString()?.contains("application/json") == true
            
            if (isJson && body.contentLength() < 10 * 1024 * 1024) {
                val content = body.string()
                // 根据要求，仅将 localhost 替换为 BASE_HOST
                // 这样如果是后端返回的 localhost 链接可以被正确访问，而 OSS 等外部链接不受影响
                val replacedContent = content.replace("localhost", BASE_HOST)
                val newBody = replacedContent.toResponseBody(contentType)
                response.newBuilder().body(newBody).build()
            } else {
                response
            }
        } else {
            response
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS 
    }

    fun getUnsafeOkHttpClientBuilder(): OkHttpClient.Builder {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            OkHttpClient.Builder()
        }
    }

    val okHttpClient = getUnsafeOkHttpClientBuilder()
        .dns(dns)
        .addInterceptor(authInterceptor)
        .addInterceptor(responseInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(false)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val userService: UserService by lazy { retrofit.create(UserService::class.java) }
    val caseService: CaseService by lazy { retrofit.create(CaseService::class.java) }
    val aiService: AIService by lazy { retrofit.create(AIService::class.java) }

    /**
     * 手动下载文件
     */
    suspend fun downloadFile(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Requesting download: $url")
            val request = okhttp3.Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    Log.e(TAG, "Download failed with code: ${response.code} for URL: $url")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download Exception: ${e.message}")
            null
        }
    }
}
