package net.yusukezzz.ssmtc.util.okhttp

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class RetryWithDelayInterceptor(
    val maxRetryCount: Int = 3,
    val retryDelayMillis: Long = 1000L
): Interceptor {
    companion object {
        val TAG: String = RetryWithDelayInterceptor::class.java.simpleName
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        var res = chain.proceed(req)

        for (tryCount in 1.rangeTo(maxRetryCount)) {
            // not 5xx response
            if ((res.code() / 100) != 5) break

            Log.d(TAG, "http request failed, retrying... " + tryCount)
            Thread.sleep(retryDelayMillis)
            res = chain.proceed(req)
        }

        return res
    }
}
