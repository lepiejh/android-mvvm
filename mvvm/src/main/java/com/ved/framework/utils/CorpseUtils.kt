package com.ved.framework.utils

import android.graphics.Rect
import android.location.Geocoder
import android.os.Looper
import android.text.SpannableStringBuilder
import android.view.TouchDelegate
import android.view.View
import androidx.annotation.ColorInt
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.*
import okhttp3.Request

object CorpseUtils {
    fun remove(s: String?): String? = s?.replace("[\r\n]".toRegex(), "")?.replace(" ", "")

    fun bytesToHex(s: String?): String? = StringUtils.bytesToHex(s?.toByteArray(Charsets.UTF_8))

    fun first(s:String?) : String{
        s?.let {
            if (it.contains(".")){
                return it.split(".")[0]
            }else{
                return it
            }
        } ?: kotlin.run {
            return ""
        }
    }

    /**
     * 当B不为空时，将B值赋给A
     */
    inline fun <T> T.assignIfNotNull(source: T?, action: (T) -> Unit) {
        source?.let {
            if (StringUtils.isNotEmpty(it)){
                action(it)
            }
        }
    }

    /**
     * 移除字符串中  最后一个字符
     */
    fun String.modifiedString() = takeUnless { isNullOrEmpty() }?.substring(0,length -1) ?: ""

    /**
     * 获取最大值
     */
    fun findMax(list: List<Int?>): Int? {
        return list.sortedWith(compareBy { it }).last()
    }

    fun makeTime(t:Int?) : String?{
        t?.let {
            if (it >= 10){
                return StringUtils.parseStr(it)
            }else{
                return "0${it}"
            }
        } ?: kotlin.run {
            return ""
        }
    }

    /**
     * 查询字符串str  里面ch字符的个数
     */
    fun countChar(str: String, ch: Char): Int {
        // 将字符串转换为字符数组
        val chs = str.toCharArray()
        // 定义变量count存储字符串出现的次数
        var count = 0
        for (i in chs.indices) {
            if (chs[i] == ch) {
                count++
            }
        }
        return count
    }

    fun split(s:String) : Int{
        val str = s.split(".")
        val ss = str.getOrNull(1)
        return if (ss?.isNotEmpty() == true){
            ss.length
        }else{
            0
        }
    }

    /**
     * 判断当前线程是否为主线程
     */
    fun isMainThread(): Boolean {
        return Looper.getMainLooper().thread == Thread.currentThread()
    }

    /**
     * 根据经纬度获取详细地址信息
     */
    fun fetchAddressFromLocation(lifecycleScope: LifecycleCoroutineScope, latitude: Double, longitude: Double, onResult: (String?) -> Unit) {
        lifecycleScope.launch {
            val address = withContext(Dispatchers.IO) {
                try {
                    // 在 IO 线程执行地理编码
                    val geocoder = Geocoder(Utils.getContext(), Locale.getDefault())
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    addresses?.firstOrNull()?.getAddressLine(0) // 获取地址行
                } catch (e: Exception) {
                    null // 处理异常
                }
            }
            // 切换回主线程返回结果
            onResult(address)
        }
    }

    /**
     * 扩展方法，扩大点击区域
     * NOTE: 需要保证目标targetView有父View，否则无法扩大点击区域
     *
     * @param expandSize 扩大的大小，单位px
     */
    fun View.expandTouchView(expandSize: Int = DisplayUtil.dip2px(Utils.getContext(),10f)) {
        val parentView = (parent as? View)
        parentView?.post {
            val rect = Rect()
            getHitRect(rect) //getHitRect(rect)将视图在父容器中所占据的区域存储到rect中。
            rect.left -= expandSize
            rect.top -= expandSize
            rect.right += expandSize
            rect.bottom += expandSize
            parentView.touchDelegate = TouchDelegate(rect, this)
        }
    }

    internal inline fun <reified T : Any> noOpDelegate(): T {
        val javaClass = T::class.java
        return Proxy.newProxyInstance(
            javaClass.classLoader, arrayOf(javaClass), NO_OP_HANDLER
        ) as T
    }

    private val NO_OP_HANDLER = InvocationHandler { _, _, _ ->
        // no op
    }

    fun processRequestBody(request: Request) {
        val body = request.body ?: run {
            KLog.e("Interceptor","Request has no body")
            return
        }
        try {
            val contentType = body.contentType()
            val contentString = body.toString()
            KLog.e("Interceptor","contentString: $contentString")
            when {
                // 如果是 JSON 类型
                contentType?.subtype?.equals("json", ignoreCase = true) == true -> {
                    try {
                        val map = JsonPraise.jsonToObj(contentString, Map::class.java)
                        KLog.e("Interceptor","Parsed JSON: $map")
                    } catch (e: Exception) {
                        KLog.e("Interceptor","Could not parse as JSON: ${e.message}")
                    }
                }
                // 如果是表单数据
                contentType?.subtype?.equals("x-www-form-urlencoded", ignoreCase = true) == true -> {
                    KLog.e("Interceptor","Form Data Request Body: $contentString")
                }
                // 如果是 multipart 数据
                contentType?.subtype?.equals("multipart", ignoreCase = true) == true -> {
                    KLog.e("Interceptor","Multipart Request Body (not showing content)")
                }
                // 其他类型
                else -> {
                    KLog.e("Interceptor","Other Request Body (${contentType}): $contentString")
                }
            }
        } catch (e: IOException) {
            KLog.e("Interceptor","Error reading request body: ${e.message}")
        }
    }
}