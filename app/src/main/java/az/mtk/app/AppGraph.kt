package az.mtk.app

import android.content.Context
import az.mtk.app.data.api.AuthInterceptor
import az.mtk.app.data.api.MtkRepository
import az.mtk.app.data.api.MtkService
import az.mtk.app.data.api.TokenAuthenticator
import az.mtk.app.data.auth.AppLockManager
import az.mtk.app.data.auth.AuthRepository
import az.mtk.app.data.auth.BiometricHelper
import az.mtk.app.data.auth.PinManager
import coil.ImageLoader
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Single, manually wired dependency graph — the app is small enough to skip DI frameworks. */
object AppGraph {
    lateinit var appContext: Context
        private set

    lateinit var auth: AuthRepository
        private set

    lateinit var repository: MtkRepository
        private set

    lateinit var imageLoader: ImageLoader
        private set

    lateinit var httpClient: OkHttpClient
        private set

    lateinit var pinManager: PinManager
        private set

    lateinit var lockManager: AppLockManager
        private set

    lateinit var biometricHelper: BiometricHelper
        private set

    private lateinit var retrofitInstance: Retrofit

    fun init(context: Context) {
        appContext = context.applicationContext

        pinManager = PinManager(appContext)
        lockManager = AppLockManager(appContext)
        biometricHelper = BiometricHelper(appContext)

        auth = AuthRepository(appContext)

        httpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(auth))
            .authenticator(TokenAuthenticator(auth))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(AppConfig.ApiBaseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()

        retrofitInstance = retrofit

        repository = MtkRepository(retrofit.create(MtkService::class.java))

        imageLoader = ImageLoader.Builder(appContext)
            .okHttpClient(httpClient)
            .build()
    }

    private val serviceCache = java.util.concurrent.ConcurrentHashMap<Class<*>, Any>()

    /** Lazily-created, cached Retrofit service for feature modules. */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> service(clazz: Class<T>): T =
        serviceCache.getOrPut(clazz) { retrofitInstance.create(clazz) } as T
}
