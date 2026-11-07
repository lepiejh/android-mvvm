package com.ved.framework.bus;


import com.ved.framework.binding.command.BindingAction;
import com.ved.framework.binding.command.BindingConsumer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 基于 token 的消息总线，用于 ViewModel 之间解耦通信（弱引用保存订阅，失效订阅自动清理）。
 * <p>
 * 详细使用方法（定义 token、注册监听、发送消息、注销监听、token 命名规范等）
 * 见项目 README.md「Messenger 消息总线」章节。
 * <p>
 * 订阅数据的存取全部收敛到内部值对象 {@link SubscriptionStore}：
 * 原实现对 HashMap 的注册写入完全无锁、注销路径却有锁，读写不对称，
 * 并发注册+注销时可能丢订阅甚至触发 HashMap 扩容死循环；现统一经单一对象锁保护。
 */
public class Messenger {

    /**
     * 线程安全的单例（双重检查锁），保证 {@link #getDefault()} 返回唯一实例
     */
    private static volatile Messenger defaultInstance;

    /**
     * 订阅存储（实例字段：{@link #overrideDefault} 可整体替换实例，存储随实例走）
     */
    private final SubscriptionStore store = new SubscriptionStore();

    public static Messenger getDefault() {
        if (defaultInstance == null) {
            synchronized (Messenger.class) {
                if (defaultInstance == null) {
                    defaultInstance = new Messenger();
                }
            }
        }
        return defaultInstance;
    }


    public static void overrideDefault(Messenger newWeakMessenger) {
        defaultInstance = newWeakMessenger;
    }

    public static void reset() {
        defaultInstance = null;
    }

    /**
     * @param recipient the receiver,if register in activity the recipient always set "this",
     *                  and "WeakMessenger.getDefault().unregister(this)" in onDestroy,if in ViewModel,
     *                  you can also register with Activity context and also in onDestroy to unregister.
     * @param action    do something on message received
     */
    public void register(Object recipient, BindingAction action) {
        register(recipient, null, false, action);
    }

    /**
     * @param recipient                 the receiver,if register in activity the recipient always set "this",
     *                                  and "WeakMessenger.getDefault().unregister(this)" in onDestroy,if in ViewModel,
     *                                  you can also register with Activity context and also in onDestroy to unregister.
     * @param receiveDerivedMessagesToo whether Derived class of recipient can receive the message
     * @param action                    do something on message received
     */
    public void register(Object recipient, boolean receiveDerivedMessagesToo, BindingAction action) {
        register(recipient, null, receiveDerivedMessagesToo, action);
    }

    /**
     * @param recipient the receiver,if register in activity the recipient always set "this",
     *                  and "WeakMessenger.getDefault().unregister(this)" in onDestroy,if in ViewModel,
     *                  you can also register with Activity context and also in onDestroy to unregister.
     * @param token     register with a unique token,when a messenger send a msg with same token,it
     *                  will
     *                  receive this msg
     * @param action    do something on message received
     */
    public void register(Object recipient, Object token, BindingAction action) {
        register(recipient, token, false, action);
    }

    /**
     * @param recipient                 the receiver,if register in activity the recipient always set "this",
     *                                  and "WeakMessenger.getDefault().unregister(this)" in onDestroy,if in ViewModel,
     *                                  you can also register with Activity context and also in onDestroy to unregister.
     * @param token                     register with a unique token,when a messenger send a msg with same token,it
     *                                  will
     *                                  receive this msg
     * @param receiveDerivedMessagesToo whether Derived class of recipient can receive the message
     * @param action                    do something on message received
     */
    public void register(Object recipient, Object token, boolean receiveDerivedMessagesToo, BindingAction action) {
        // 菱形推断为 WeakAction<Object>（无参回调不携带消息体，T 无意义），
        // 与下面 register(..., BindingConsumer<T>, Class<T>) 收敛后的存储类型保持一致。
        doRegister(NotMsgType.class, receiveDerivedMessagesToo, new WeakAction<>(recipient, action), token);
    }

    /**
     * @param recipient 消息接收者，在 Activity 中通常传 "this"，并在 onDestroy 中调用
     *                  {@code Messenger.getDefault().unregister(this)}；在 ViewModel 中
     *                  也可以用 Activity 作为 recipient，同样在 onDestroy 中注销。
     * @param tClass    消息数据类型
     * @param action    收到消息时执行的回调，该回调接收一个 tClass 类型的参数
     * @param <T>       消息数据类型
     */
    public <T> void register(Object recipient, Class<T> tClass, BindingConsumer<T> action) {
        register(recipient, null, false, action, tClass);
    }

    /**
     * @param recipient                 消息接收者
     * @param receiveDerivedMessagesToo 是否允许 recipient 的派生类也收到消息
     * @param tClass                    消息数据类型
     * @param action                    收到消息时执行的回调，该回调接收一个 tClass 类型的参数
     * @param <T>                       消息数据类型
     */
    public <T> void register(Object recipient, boolean receiveDerivedMessagesToo, Class<T> tClass, BindingConsumer<T> action) {
        register(recipient, null, receiveDerivedMessagesToo, action, tClass);
    }

    /**
     * @param recipient 消息接收者
     * @param token     注册的 token，发送方用相同 token 发送时，该订阅者才会收到消息
     * @param tClass    消息数据类型
     * @param action    收到消息时执行的回调，该回调接收一个 tClass 类型的参数
     * @param <T>       消息数据类型
     */
    public <T> void register(Object recipient, Object token, Class<T> tClass, BindingConsumer<T> action) {
        register(recipient, token, false, action, tClass);
    }

    /**
     * @param recipient                 消息接收者
     * @param token                     注册的 token，发送方用相同 token 发送时，该订阅者才会收到消息
     * @param receiveDerivedMessagesToo 是否允许 recipient 的派生类也收到消息
     * @param action                    收到消息时执行的回调，该回调接收一个 tClass 类型的参数
     * @param tClass                    消息数据类型
     * @param <T>                       消息数据类型
     */
    public <T> void register(Object recipient, Object token, boolean receiveDerivedMessagesToo, BindingConsumer<T> action, Class<T> tClass) {
        // 为什么把 WeakAction<T> 收敛成 WeakAction<Object>：本类是个异构消息分发器，
        // 一个 List<WeakActionAndToken> 里混存了各种 T 的订阅，无法用单一实参描述；
        // 以前直接用裸类型 WeakAction 来表达这一点，代价是 13 处 rawtypes 告警。
        // 两者擦除后完全相同，运行期行为一字不差；真正的类型安全本来就靠
        // sendToTargetOrType 里的 messageType 匹配来保证，而不是靠这里的泛型。
        // @SuppressWarnings 只盖这一个局部变量，不外溢到整个方法。
        @SuppressWarnings("unchecked")
        WeakAction<Object> weakAction = (WeakAction<Object>) new WeakAction<T>(recipient, action);
        doRegister(tClass, receiveDerivedMessagesToo, weakAction, token);
    }

    /**
     * 模板方法：抽取 register 系列方法的公共注册流程，实际存取委托给订阅存储。
     */
    private void doRegister(Type messageType, boolean receiveDerivedMessagesToo, WeakAction<Object> weakAction, Object token) {
        store.add(messageType, receiveDerivedMessagesToo, weakAction, token);
    }

    /**
     * @param token send with a unique token,when a receiver has register with same token,it will
     *              receive this msg
     */
    public void sendNoMsg(Object token) {
        sendToTargetOrType(null, null, token);
    }

    /**
     * send to recipient directly with has not any message
     *
     * @param target WeakMessenger.getDefault().register(this, ..) in a activity,if target set this
     *               activity
     *               it will receive the message
     */
    public void sendNoMsgToTarget(Object target) {
        sendToTargetOrType(null, target.getClass(), null);
    }

    /**
     * send message to target with token,when a receiver has register with same token,it will
     * receive this msg
     *
     * @param token  send with a unique token,when a receiver has register with same token,it will
     *               receive this msg
     * @param target send to recipient directly with has not any message,
     *               WeakMessenger.getDefault().register(this, ..) in a activity,if target set this activity
     *               it will receive the message
     */
    public void sendNoMsgToTargetWithToken(Object token, Object target) {
        sendToTargetOrType(null, target.getClass(), token);
    }

    /**
     * send the message type of T, all receiver can receive the message
     *
     * @param message any object can to be a message
     * @param <T>     message data type
     */
    public <T> void send(T message) {
        sendToTargetOrType(message, null, null);
    }

    /**
     * send the message type of T, all receiver can receive the message
     *
     * @param message any object can to be a message
     * @param token   send with a unique token,when a receiver has register with same token,it will
     *                receive this message
     * @param <T>     message data type
     */
    public <T> void send(T message, Object token) {
        sendToTargetOrType(message, null, token);
    }

    /**
     * send message to recipient directly
     *
     * @param message any object can to be a message
     * @param target  send to recipient directly with has not any message,
     *                WeakMessenger.getDefault().register(this, ..) in a activity,if target set this activity
     *                it will receive the message
     * @param <T>     message data type
     * @param <R>     target
     */
    public <T, R> void sendToTarget(T message, R target) {
        sendToTargetOrType(message, target.getClass(), null);
    }

    /**
     * Unregister the receiver such as:
     * WeakMessenger.getDefault().unregister(this)" in onDestroy in the Activity is required avoid
     * to
     * memory leak!
     *
     * @param recipient receiver of message
     */
    public void unregister(Object recipient) {
        store.unregister(recipient);
    }


    public void unregister(Object recipient, Object token) {
        store.unregister(recipient, token);
    }

    private static boolean classImplements(Type instanceType, Type interfaceType) {
        if (interfaceType == null
                || instanceType == null) {
            return false;
        }
        Class<?>[] interfaces = ((Class<?>) instanceType).getInterfaces();
        for (Class<?> currentInterface : interfaces) {
            if (currentInterface == interfaceType) {
                return true;
            }
        }

        return false;
    }

    private void sendToTargetOrType(Object message, Type messageTargetType, Object token) {
        // 无消息（sendNoMsg 系列）时用 NotMsgType 哨兵类型匹配"无参订阅"，
        // 有消息时按消息的运行时类型匹配，两套分发流程由此收敛为一份
        Class<?> messageType = message != null ? message.getClass() : NotMsgType.class;

        // Clone to protect from people registering in a "receive message" method
        // Bug correction Messaging BL0008.002
        List<Type> listClone = store.derivedKeysSnapshot();
        for (Type type : listClone) {
            List<WeakActionAndToken> list = null;

            if (messageType == type
                    || ((Class<?>) type).isAssignableFrom(messageType)
                    || classImplements(messageType, type)) {
                list = store.derivedSnapshot(type);
            }

            sendToList(message, list, messageTargetType, token);
        }

        List<WeakActionAndToken> strictList = store.strictSnapshot(messageType);
        if (strictList != null) {
            sendToList(message, strictList, messageTargetType, token);
        }

        store.cleanup();
    }

    /**
     * 消息分发模板：message 为 null 表示"无参事件"（走 BindingAction 通道），
     * 否则按类型分发给注册了对应消息类型的 BindingConsumer。
     * 入参快照在锁外执行回调，避免持锁调用外部代码造成死锁。
     */
    private static void sendToList(
            Object message,
            Collection<WeakActionAndToken> list,
            Type messageTargetType,
            Object token) {
        if (list != null) {
            // Clone to protect from people registering in a "receive message" method
            // Bug correction Messaging BL0004.007
            ArrayList<WeakActionAndToken> listClone = new ArrayList<>(list);

            for (WeakActionAndToken item : listClone) {
                WeakAction<Object> executeAction = item.getAction();
                if (executeAction != null
                        && item.getAction().isLive()
                        && item.getAction().getTarget() != null
                        && (messageTargetType == null
                        || item.getAction().getTarget().getClass() == messageTargetType
                        || classImplements(item.getAction().getTarget().getClass(), messageTargetType))
                        && ((item.getToken() == null && token == null)
                        || item.getToken() != null && item.getToken().equals(token))) {
                    if (message != null) {
                        executeAction.execute(message);
                    } else {
                        executeAction.execute();
                    }
                }
            }
        }
    }


    /**
     * 订阅存储（值对象，内在锁）：
     * 封装「严格类型 / 派生类型」两个订阅容器，注册、注销、清理、快照全部经由
     * 本对象的同一把监视器锁（synchronized 方法），消除原实现"注册无锁、注销有锁"
     * 的读写不对称；对外只暴露快照查询，回调执行不持锁。
     */
    private static final class SubscriptionStore {

        /** 严格模式订阅：只匹配注册时声明的消息类型本身 */
        private final Map<Type, List<WeakActionAndToken>> strictSubscriptions = new HashMap<>();

        /** 派生模式订阅：消息类型是注册类型的子类/实现类时也分发 */
        private final Map<Type, List<WeakActionAndToken>> derivedSubscriptions = new HashMap<>();

        /** 注册订阅并顺带清理失效条目（synchronized：与注销/快照共用同一把锁） */
        synchronized void add(Type messageType, boolean receiveDerivedMessagesToo, WeakAction<Object> weakAction, Object token) {
            Map<Type, List<WeakActionAndToken>> recipients = recipientsFor(receiveDerivedMessagesToo);
            List<WeakActionAndToken> list = recipients.get(messageType);
            if (list == null) {
                list = new ArrayList<>();
                recipients.put(messageType, list);
            }
            list.add(new WeakActionAndToken(weakAction, token));
            cleanupLocked();
        }

        synchronized void unregister(Object recipient) {
            if (recipient == null) {
                return;
            }
            markForDeletion(recipient, strictSubscriptions);
            markForDeletion(recipient, derivedSubscriptions);
            cleanupLocked();
        }

        synchronized void unregister(Object recipient, Object token) {
            if (recipient == null) {
                return;
            }
            markForDeletionWithToken(recipient, token, strictSubscriptions);
            markForDeletionWithToken(recipient, token, derivedSubscriptions);
            cleanupLocked();
        }

        synchronized void cleanup() {
            cleanupLocked();
        }

        /** 派生容器全部消息类型的快照（发送方遍历用，避免遍历时容器被并发修改） */
        synchronized List<Type> derivedKeysSnapshot() {
            return new ArrayList<>(derivedSubscriptions.keySet());
        }

        /** 派生容器中某消息类型的订阅快照，无订阅返回 null */
        synchronized List<WeakActionAndToken> derivedSnapshot(Type messageType) {
            return snapshot(derivedSubscriptions, messageType);
        }

        /** 严格容器中某消息类型的订阅快照，无订阅返回 null */
        synchronized List<WeakActionAndToken> strictSnapshot(Type messageType) {
            return snapshot(strictSubscriptions, messageType);
        }

        private static List<WeakActionAndToken> snapshot(Map<Type, List<WeakActionAndToken>> subscriptions, Type messageType) {
            List<WeakActionAndToken> list = subscriptions.get(messageType);
            return list == null ? null : new ArrayList<>(list);
        }

        private Map<Type, List<WeakActionAndToken>> recipientsFor(boolean receiveDerivedMessagesToo) {
            return receiveDerivedMessagesToo ? derivedSubscriptions : strictSubscriptions;
        }

        private static void markForDeletion(Object recipient, Map<Type, List<WeakActionAndToken>> lists) {
            // 直接遍历 entrySet，避免 keySet + get 的二次查找
            for (Map.Entry<Type, List<WeakActionAndToken>> entry : lists.entrySet()) {
                for (WeakActionAndToken item : entry.getValue()) {
                    WeakAction<Object> weakAction = item.getAction();
                    if (weakAction != null && recipient == weakAction.getTarget()) {
                        weakAction.markForDeletion();
                    }
                }
            }
        }

        /**
         * 与原实现保持一致的语义：带 token 的注销只清理「无参订阅」（NotMsgType 桶），
         * 不触碰带消息类型的订阅桶。
         */
        private static void markForDeletionWithToken(Object recipient, Object token, Map<Type, List<WeakActionAndToken>> lists) {
            List<WeakActionAndToken> targetList = lists.get(NotMsgType.class);
            if (targetList == null) {
                return;
            }
            for (WeakActionAndToken item : targetList) {
                WeakAction<Object> weakAction = item.getAction();
                if (weakAction != null
                        && recipient == weakAction.getTarget()
                        && (token == null
                        || token.equals(item.getToken()))) {
                    weakAction.markForDeletion();
                }
            }
        }

        /** 移除已失效（弱引用回收 / 标记删除）的订阅及空桶，仅在持锁状态下调用 */
        private void cleanupLocked() {
            cleanupList(derivedSubscriptions);
            cleanupList(strictSubscriptions);
        }

        private static void cleanupList(Map<Type, List<WeakActionAndToken>> lists) {
            Iterator<Map.Entry<Type, List<WeakActionAndToken>>> iterator = lists.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Type, List<WeakActionAndToken>> entry = iterator.next();
                List<WeakActionAndToken> itemList = entry.getValue();
                if (itemList != null) {
                    // 使用迭代器安全删除失效的订阅，避免 ConcurrentModificationException
                    Iterator<WeakActionAndToken> itemIterator = itemList.iterator();
                    while (itemIterator.hasNext()) {
                        WeakActionAndToken item = itemIterator.next();
                        if (item.getAction() == null
                                || !item.getAction().isLive()) {
                            itemIterator.remove();
                        }
                    }
                    if (itemList.isEmpty()) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    /**
     * 订阅条目：把一个 {@code WeakAction} 与可选的 token 绑在一起。
     * <p>存储类型统一用 {@code WeakAction<Object>}：本类是异构分发器，同一个列表里
     * 混存了不同消息类型的订阅，无法用单一泛型实参描述（详见 register 处的说明）。
     * <p>本类是 private 静态内部类，不对外暴露，因此收紧泛型不影响任何公开 API；
     * 声明为 static 避免每个订阅条目隐式持有外部 Messenger 实例。
     */
    private static class WeakActionAndToken {
        private final WeakAction<Object> action;
        private final Object token;

        public WeakActionAndToken(WeakAction<Object> action, Object token) {
            this.action = action;
            this.token = token;
        }

        public WeakAction<Object> getAction() {
            return action;
        }

        public Object getToken() {
            return token;
        }
    }

    public static class NotMsgType {

    }
}
