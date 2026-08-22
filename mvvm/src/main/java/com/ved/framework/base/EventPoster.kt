package com.ved.framework.base

import com.ved.framework.bus.event.eventbus.MessageEvent

/**
 * 事件发送策略接口（策略模式）：
 * 抽象事件发送后端（RxBus / EventBus），配合高阶函数消除 sendRxEvent / sendEvent 中的重复分支逻辑。
 * 新增事件后端时只需实现本接口，无需修改现有方法（开闭原则）。
 */
fun interface EventPoster {
    fun post(event: MessageEvent<*>?, sticky: Boolean)
}