package net.yusukezzz.ssmtc.util.okhttp

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class RetryWithDelayInterceptor(
    private val maxRetryCount: Int = 3,
    private val retryDelayMillis: Long = 1000L
) : Interceptor {
    companion object {
        val TAG: String = RetryWithDelayInterceptor::class.java.simpleName
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        var res = chain.proceed(req)

        for (tryCount in 1.rangeTo(maxRetryCount)) {
            if ((res.code() / 100) != 5) break

            // retry if 5xx response
            res.close()
            Log.d(TAG, "[status=${res.code()}] http request failed, retrying... " + tryCount)
            Thread.sleep(retryDelayMillis)
            res = chain.proceed(req)
        }

        return res
    }
}
