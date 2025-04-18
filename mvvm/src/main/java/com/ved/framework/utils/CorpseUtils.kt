package com.ved.framework.utils

import android.graphics.Rect
import android.location.Geocoder
import android.os.Looper
import android.view.TouchDelegate
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.viewModelScope
import com.ved.framework.base.BaseViewModel
import com.ved.framework.net.IResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.*

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
     * 线程切换
     */
    fun fetch(viewModel: BaseViewModel<*>?,ioAction: (() -> Unit)?,mainAction: (() -> Unit)?){
        viewModel?.viewModelScope?.launch(Dispatchers.IO) {
            ioAction?.invoke()
            withContext(Dispatchers.Main){
                mainAction?.invoke()
            }
        }
    }

    fun fetch(viewModel: BaseViewModel<*>?, iResponse: IResponse<*>?, error:String?){
        viewModel?.viewModelScope?.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main){
                viewModel.dismissDialog()
                iResponse?.onError(error)
            }
        }
    }

    /**
     * 延时执行某个动作
     */
    fun BaseViewModel<*>.delayedAction(delayMillis: Long,action: () -> Unit) {
        viewModelScope.launch {
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