package com.ved.framework.base;

import com.ved.framework.bus.event.eventbus.MessageEvent;

/**
 * 事件发送策略接口（策略模式）：
 * 抽象事件发送后端（RxBus / EventBus），配合高阶函数消除 sendRxEvent / sendEvent 中的重复分支逻辑。
 * 新增事件后端时只需实现本接口，无需修改现有方法（开闭原则）。
 *
 * <p><b>为什么是 Java 包级接口而不是 Kotlin {@code fun interface}：</b>
 * 本接口只是 {@link BaseViewModel} 内部事件分发的私有协作对象，不属于对外 API。
 * Kotlin 没有包级可见性（只有 public / internal / protected / private），写成
 * {@code fun interface} 就必然是 public，会把一个纯内部抽象泄漏进 AAR 的公开接口。
 * 改为 Java 包级后与同包的 ICommand / UICommand / BaseView 封装粒度一致，
 * 且能被库模块的混淆规则放开重命名（-keep public interface 不覆盖包级接口）。
 *
 * <p>Kotlin 侧仍然可以直接用 SAM 转换构造实例，写法与之前的 fun interface 完全相同：
 * <pre>
 * private val rxBusPoster = EventPoster { event, sticky -&gt; ... }
 * </pre>
 * 因为 rxBusPoster / eventBusPoster / dispatchEvent 都是 private 成员，
 * 引用包级 Java 类型不会触发 Kotlin 的 “public function exposes its 'public/*&#47;package*&#47;'
 * type” 检查 —— 同一文件里的 {@code private val command: ICommand = UICommand()} 就是先例。
 */
interface EventPoster {

    /**
     * 发送事件
     *
     * @param event  事件载体，允许为 null
     * @param sticky 是否以粘性方式发送（由 {@code BaseViewModel#onEventSticky()} 决定）
     */
    void post(MessageEvent<?> event, boolean sticky);
}
