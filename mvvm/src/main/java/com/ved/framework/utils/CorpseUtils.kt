package com.ved.framework.utils

import android.graphics.Rect
import android.location.Geocoder
import android.os.Looper
import android.view.TouchDelegate
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.viewModelScope
import com.ved.framework.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

object CorpseUtils {
    fun trim(s: String?): String? = s?.replace("[\r\n]".toRegex(), "")?.replace(" ", "")

    fun bytesToHex(s: String?): String? = StringUtils.bytesToHex(s?.toByteArray(Charsets.UTF_8))

    /**
     * value is Float : 浮点数转 IEEE 754 字节数组
     * value is Int : int32转换为4字节数组
     *
     * LITTLE_ENDIAN 存储  (小端序)
    内存地址增加方向 →
    +------+------+------+------+
    | 0x78 | 0x56 | 0x34 | 0x12 |
    +------+------+------+------+
    低地址                高地址
    BIG_ENDIAN 存储  (大端序)
    内存地址增加方向 →
    +------+------+------+------+
    | 0x12 | 0x34 | 0x56 | 0x78 |
    +------+------+------+------+
    低地址                高地址
     */
    fun toBytes(value: Any, byte: Int = 4, boType: Int = 1): ByteArray {
        return ByteBuffer.allocate(byte).order(if (boType == 1) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN).apply {
            when(value){
                is Float -> putFloat(value)
                is Double -> putDouble(value)
                is Int -> putInt(value)
                is Long -> putLong(value)
                is Short -> putShort(value)
                is Char -> putChar(value)
                else -> putInt(0)
            }
        }.array()
    }

    /**
     * 线程切换
     */
    fun BaseViewModel<*>?.fetch(ioAction: (() -> Unit)?,mainAction: (() -> Unit)?){
        this?.viewModelScope?.launch(Dispatchers.IO) {
            ioAction?.invoke()
            withContext(Dispatchers.Main){
                mainAction?.invoke()
            }
        }
    }

    /**
     * 延时执行某个动作
     */
    fun BaseViewModel<*>?.delayedAction(delayMillis: Long,action: () -> Unit) {
        this?.viewModelScope?.launch {
            delay(delayMillis) // 协程的delay函数
            action.invoke()
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
    fun View.expandTouchView(expandSize: Float = 10f) {
        val parentView = (parent as? View)
        val size = DisplayUtil.dip2px(Utils.getContext(),expandSize)
        parentView?.post {
            val rect = Rect()
            getHitRect(rect) //getHitRect(rect)将视图在父容器中所占据的区域存储到rect中。
            rect.left -= size
            rect.top -= size
            rect.right += size
            rect.bottom += size
            parentView.touchDelegate = TouchDelegate(rect, this)
        }
    }

    inline fun <reified T : Any> noOpDelegate(): T = T::class.java.let { Proxy.newProxyInstance(it.classLoader,arrayOf(it)){ _, _, _ -> } as T }

    fun inspectRequestBody(request: Request) {
        val body = request.body ?: return
        // 复制一份 Buffer 以便多次读取
        val buffer = okio.Buffer()
        body.writeTo(buffer)
        val contentBytes = buffer.readByteArray()
        // 根据 Content-Type 处理
        when (val contentType = body.contentType()) {
            null -> {
                KLog.e("Interceptor","Raw body (no content type): ${contentBytes.decodeToString()}")
            }
            else -> when (contentType.subtype.lowercase()) {
                "json" -> {
                    val jsonString = contentBytes.decodeToString()
                    KLog.e("Interceptor","JSON Body: $jsonString")
                }
                "x-www-form-urlencoded" -> {
                    KLog.i("Interceptor","Form Data: ${contentBytes.decodeToString()}")
                }
                else -> {
                    KLog.i("Interceptor","Body (${contentType}): ${contentBytes.take(100)}... [truncated]")
                }
            }
        }
    }

    fun isStandardJson(jsonString: String) = try {
        val json = JSONObject(jsonString)
        json.length() == 3 && json.has("code") && json.has("msg") && json.has("data")
    } catch (e: Exception) {
        false
    }
}