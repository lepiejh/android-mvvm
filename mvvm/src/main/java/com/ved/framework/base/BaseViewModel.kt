package com.ved.framework.base

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trello.rxlifecycle4.LifecycleProvider
import com.ved.framework.bus.ISubscription
import com.ved.framework.bus.RxBus
import com.ved.framework.bus.event.eventbus.EventBusUtil
import com.ved.framework.bus.event.eventbus.MessageEvent
import com.ved.framework.permission.IPermission
import com.ved.framework.utils.KLog
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by ved on 2017/6/15.
 */
open class BaseViewModel<M : BaseModel?> @JvmOverloads constructor(
    application: Application,
    private var model: M? = null
) : AndroidViewModel(
    application
), IBaseViewModel, Consumer<Disposable>, ISubscription {

    //弱引用持有
    private var lifecycle: WeakReference<LifecycleProvider<*>>? = null

    // UI 命令门面（门面模式）：面向 ICommand 接口编程，解耦命令发起与执行
    private val command: ICommand = UICommand()
    private var eventStrategy: IEventSubscriptionStrategy? = null
    private val backgroundJobs = ConcurrentHashMap<String, Job>()

    // ViewModel 自己的订阅容器（仅当 Model 为空时使用）
    private var selfCompositeDisposable: CompositeDisposable? = null

    init {
        initEventStrategy()
    }

    // region ISubscription 实现：智能切换 Model 或自管理

    /**
     * 获取有效的 CompositeDisposable
     * 优先使用 Model 的，如果 Model 为空则使用自己的
     */
    private fun getCompositeDisposable(): CompositeDisposable {
        // 优先使用 Model 的
        model?.getCompositeDisposable()?.let { return it }

        // Model 为空，使用自己的
        if (selfCompositeDisposable == null) {
            selfCompositeDisposable = CompositeDisposable()
        }
        return selfCompositeDisposable!!
    }

    override fun add(s: Disposable) {
        getCompositeDisposable().add(s)
    }

    override fun remove(s: Disposable) {
        getCompositeDisposable().remove(s)
    }

    override fun isDisposed(): Boolean = getCompositeDisposable().isDisposed

    override fun clear() {
        getCompositeDisposable().clear()
    }

    override fun dispose() {
        getCompositeDisposable().dispose()
    }

    // endregion

    override fun openEventSubscription(): Boolean {
        return false
    }

    override fun onEventSticky(): Boolean {
        return false
    }

    /**
     * 注入RxLifecycle生命周期
     */
    fun injectLifecycleProvider(lifecycle: LifecycleProvider<*>) {
        this.lifecycle = WeakReference(lifecycle)
    }

    fun getLifecycleProvider() = lifecycle?.get()

    fun getUC(): UIChangeLiveData = command.liveData

    fun showDialog() {
        command.showDialog()
    }

    fun showDialog(title: String?) {
        command.showDialog(title)
    }

    fun dismissDialog() {
        command.dismissDialog()
    }

    /**
     * 跳转页面
     *
     * @param clz 所跳转的目的Activity类
     */
    fun startActivity(clz: Class<*>?) {
        command.startActivity(clz)
    }

    /**
     * 跳转页面
     *
     * @param clz    所跳转的目的Activity类
     * @param bundle 跳转所携带的信息
     */
    fun startActivity(clz: Class<*>?, bundle: Bundle?) {
        command.startActivity(clz, bundle)
    }

    fun sendReceiver() {
        command.sendReceiver()
    }

    fun sendReceiver(bundle: Bundle?) {
        command.sendReceiver(bundle)
    }

    fun startActivityForResult(clz: Class<*>?, requestCode: Int) {
        command.startActivityForResult(clz, requestCode)
    }

    fun startActivityForResult(clz: Class<*>?, bundle: Bundle?, requestCode: Int) {
        command.startActivityForResult(clz, bundle, requestCode)
    }

    /**
     * 跳转容器页面
     *
     * @param canonicalName 规范名 : Fragment.class.getCanonicalName()
     */
    fun startContainerActivity(canonicalName: String?) {
        command.startContainerActivity(canonicalName)
    }

    /**
     * 跳转容器页面
     *
     * @param canonicalName 规范名 : Fragment.class.getCanonicalName()
     * @param bundle        跳转所携带的信息
     */
    fun startContainerActivity(canonicalName: String?, bundle: Bundle?) {
        command.startContainerActivity(canonicalName, bundle)
    }

    fun requestPermissions(iPermission: IPermission?, vararg permissions: String?) {
        command.requestPermissions(iPermission, *permissions)
    }

    fun callPhone(phoneNumber: String?) {
        command.callPhone(phoneNumber)
    }

    fun getWifiRssi(){
        command.getWifiRssi()
    }

    /**
     * 关闭界面
     */
    fun finish() {
        command.finish()
    }

    /**
     * 返回上一层
     */
    fun onBackPressed() {
        command.onBackPressed()
    }

    /**
     * 取消后台任务
     *
     * @param key 任务唯一标识，如果为null则取消所有任务
     * @param removeOnly 是否只从Map中移除而不实际取消任务(默认false)
     * @return Boolean 是否成功找到并取消了任务(当key为null时总是返回true)
     */
    fun cancelJob(key: String? = null, removeOnly: Boolean = false): Boolean {
        return when (key) {
            // 取消所有任务
            null -> {
                backgroundJobs.forEach { (_, job) ->
                    if (!removeOnly) job.cancel()
                }
                backgroundJobs.clear()
                true
            }
            // 取消指定key的任务
            else -> {
                val job = backgroundJobs[key]
                if (job != null) {
                    if (!removeOnly) job.cancel()
                    backgroundJobs.remove(key)
                    true
                } else {
                    false
                }
            }
        }
    }

    /**
     * 后台任务管理模板方法（模板方法模式）：
     * 统一处理任务注册、取消同 key 旧任务、异常兜底与任务清理，
     * 具体任务只关心自身业务逻辑。
     *
     * @param key     任务唯一标识
     * @param onError 业务异常回调（默认主线程记录日志）
     * @param onCancel 任务取消回调
     * @param task    具体任务体
     */
    private fun launchManagedTask(
        key: String,
        onError: (Throwable) -> Unit = { KLog.e(it.message) },
        onCancel: (Throwable) -> Unit = { KLog.e(it.message) },
        task: suspend CoroutineScope.() -> Unit
    ): Job {
        cancelJob(key) // 取消同key的旧任务
        val job = viewModelScope.launch {
            try {
                task()
            } catch (e: CancellationException) {
                onCancel(e)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e) }
            } finally {
                backgroundJobs.remove(key)
            }
        }
        backgroundJobs[key] = job
        return job
    }

    /**
     * 线程切换 - 带键值管理
     */
    fun fetchWithCancel(
        key: String,
        ioAction: suspend CoroutineScope.() -> Unit = {},
        uiAction: suspend () -> Unit = {},
        onError: (Throwable) -> Unit = { KLog.e(it.message) },
        onCancel: (Throwable) -> Unit = { KLog.e(it.message) }
    ) {
        launchManagedTask(key, onError, onCancel) {
            val ioJob = launch(Dispatchers.IO) { ioAction() }
            ioJob.join()
            withContext(Dispatchers.Main) { // 确保UI更新在主线程
                uiAction()
            }
        }
    }

    /**
     * 延时执行某个动作 - 带键值管理
     */
    fun delayedAction(
        key: String,
        delay: Long,
        block: () -> Unit
    ) {
        launchManagedTask(key) {
            delay(delay)
            block()
        }
    }

    override fun onCreate() {
        getUC().onLoadEvent.call()
    }

    private fun initEventStrategy() {
        eventStrategy = if (onEventSticky()) StickyEventStrategy() else DefaultEventStrategy()
    }

    // region Event dispatch（策略模式 + 高阶函数）
    private val rxBusPoster = EventPoster { event, sticky ->
        if (sticky) RxBus.getDefault().postSticky(event) else RxBus.getDefault().post(event)
    }

    private val eventBusPoster = EventPoster { event, sticky ->
        if (sticky) EventBusUtil.sendStickyEvent(event) else EventBusUtil.sendEvent(event)
    }

    private fun dispatchEvent(event: MessageEvent<*>?, poster: EventPoster) {
        poster.post(event, onEventSticky())
    }

    fun sendRxEvent(messageEvent: MessageEvent<*>?) = dispatchEvent(messageEvent, rxBusPoster)

    fun sendEvent(messageEvent: MessageEvent<*>?) = dispatchEvent(messageEvent, eventBusPoster)

    // endregion

    private fun onStartEventSubscription() {
        eventStrategy?.setupSubscription(this)
    }

    /**
     * 处理RxBus 失败回调
     */
    override fun onError(throwable: Throwable?) { throwable?.message?.let { KLog.e(it) } }

    override fun onResume() {
        getUC().onResumeEvent.call()
    }

    override fun registerRxBus() {
        if (openEventSubscription()) {
            onStartEventSubscription()
        }
    }

    override fun removeRxBus() {
        if (openEventSubscription()) {
            eventStrategy?.remove()
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            // Model 的 onCleared() 会清理 CompositeDisposable
            model?.onCleared()

            // 清理自己的订阅（如果存在）
            selfCompositeDisposable?.clear()
            selfCompositeDisposable?.dispose()
            selfCompositeDisposable = null

            cancelJob()
            viewModelScope.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    override fun accept(disposable: Disposable) {
        add(disposable)
    }
}