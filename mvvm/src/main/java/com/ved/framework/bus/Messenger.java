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
 * About : kelin的Messenger
 * 使用方法：
 * 1.定义一个静态String类型的字符串token
 * public static final String TOKEN_LOGINVIEWMODEL_REFRESH = "token_loginviewmodel_refresh";
 * 2.在ViewModel中注册消息监听
 * //注册一个空消息监听
 * //参数1：接受人（上下文）
 * //参数2：定义的token
 * //参数3：执行的回调监听
 * Messenger.getDefault().register(this, LoginViewModel.TOKEN_LOGINVIEWMODEL_REFRESH, new BindingAction() {
 *     @Override
 *     public void call() {
 *
 *     }
 * });
 *
 * //注册一个带数据回调的消息监听
 * //参数1：接受人（上下文）
 * //参数2：定义的token
 * //参数3：实体的泛型约束
 * //参数4：执行的回调监听
 * Messenger.getDefault().register(this, LoginViewModel.TOKEN_LOGINVIEWMODEL_REFRESH, String.class, new BindingConsumer<String>() {
 *     @Override
 *     public void call(String s) {
 *
 *     }
 * });
 * 3.在需要回调的地方使用token发送消息
 * //发送一个空消息
 * //参数1：定义的token
 * Messenger.getDefault().sendNoMsg(LoginViewModel.TOKEN_LOGINVIEWMODEL_REFRESH);
 *
 * //发送一个带数据回调消息
 * //参数1：回调的实体
 * //参数2：定义的token
 * Messenger.getDefault().send("refresh",LoginViewModel.TOKEN_LOGINVIEWMODEL_REFRESH);
 *
 * token最好不要重名，不然可能就会出现逻辑上的bug，为了更好的维护和清晰逻辑，建议以aa_bb_cc的格式来定义token。aa：TOKEN，bb：ViewModel的类名，cc：动作名（功能名）。
 *
 * 为了避免大量使用Messenger，建议只在ViewModel与ViewModel之间使用，View与ViewModel之间采用ObservableField去监听UI上的逻辑，可在继承了Base的Activity或Fragment中重写initViewObservable()方法来初始化UI的监听
 */
public class Messenger {

    /**
     * 线程安全的单例（双重检查锁），保证 {@link #getDefault()} 返回唯一实例
     */
    private static volatile Messenger defaultInstance;

    private HashMap<Type, List<WeakActionAndToken>> recipientsOfSubclassesAction;

    private HashMap<Type, List<WeakActionAndToken>> recipientsStrictAction;

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
        doRegister(NotMsgType.class, receiveDerivedMessagesToo, new WeakAction(recipient, action), token);
    }

    /**
     * @param recipient {}
     * @param tClass    class of T
     * @param action    this action has one params that type of tClass
     * @param <T>       message data type
     */
    public <T> void register(Object recipient, Class<T> tClass, BindingConsumer<T> action) {
        register(recipient, null, false, action, tClass);
    }

    /**
     * see {}
     *
     * @param recipient                 receiver of message
     * @param receiveDerivedMessagesToo whether derived class of recipient can receive the message
     * @param tClass                    class of T
     * @param action                    this action has one params that type of tClass
     * @param <T>                       message data type
     */
    public <T> void register(Object recipient, boolean receiveDerivedMessagesToo, Class<T> tClass, BindingConsumer<T> action) {
        register(recipient, null, receiveDerivedMessagesToo, action, tClass);
    }

    /**
     * see {}
     *
     * @param recipient receiver of message
     * @param token     register with a unique token,when a messenger send a msg with same token,it
     *                  will
     *                  receive this msg
     * @param tClass    class of T for BindingConsumer
     * @param action    this action has one params that type of tClass
     * @param <T>       message data type
     */
    public <T> void register(Object recipient, Object token, Class<T> tClass, BindingConsumer<T> action) {
        register(recipient, token, false, action, tClass);
    }

    /**
     * see {}
     *
     * @param recipient                 receiver of message
     * @param token                     register with a unique token,when a messenger send a msg with same token,it
     *                                  will
     *                                  receive this msg
     * @param receiveDerivedMessagesToo whether derived class of recipient can receive the message
     * @param action                    this action has one params that type of tClass
     * @param tClass                    class of T for BindingConsumer
     * @param <T>                       message data type
     */
    public <T> void register(Object recipient, Object token, boolean receiveDerivedMessagesToo, BindingConsumer<T> action, Class<T> tClass) {
        doRegister(tClass, receiveDerivedMessagesToo, new WeakAction<T>(recipient, action), token);
    }

    /**
     * 模板方法：抽取 register 系列方法的公共注册流程。
     * 根据 receiveDerivedMessagesToo 选择对应的接收者容器，再写入消息订阅。
     */
    private void doRegister(Type messageType, boolean receiveDerivedMessagesToo, WeakAction weakAction, Object token) {
        HashMap<Type, List<WeakActionAndToken>> recipients = getRecipients(receiveDerivedMessagesToo);

        List<WeakActionAndToken> list = recipients.computeIfAbsent(messageType, k -> new ArrayList<WeakActionAndToken>());

        list.add(new WeakActionAndToken(weakAction, token));
        cleanup();
    }

    /**
     * 根据是否接收派生类消息，返回对应的接收者容器（懒加载）
     */
    private HashMap<Type, List<WeakActionAndToken>> getRecipients(boolean receiveDerivedMessagesToo) {
        HashMap<Type, List<WeakActionAndToken>> recipients;
        if (receiveDerivedMessagesToo) {
            if (recipientsOfSubclassesAction == null) {
                recipientsOfSubclassesAction = new HashMap<Type, List<WeakActionAndToken>>();
            }
            recipients = recipientsOfSubclassesAction;
        } else {
            if (recipientsStrictAction == null) {
                recipientsStrictAction = new HashMap<Type, List<WeakActionAndToken>>();
            }
            recipients = recipientsStrictAction;
        }
        return recipients;
    }

    private void cleanup() {
        cleanupList(recipientsOfSubclassesAction);
        cleanupList(recipientsStrictAction);
    }

    /**
     * @param token send with a unique token,when a receiver has register with same token,it will
     *              receive this msg
     */
    public void sendNoMsg(Object token) {
        sendToTargetOrType(null, token);
    }

    /**
     * send to recipient directly with has not any message
     *
     * @param target WeakMessenger.getDefault().register(this, ..) in a activity,if target set this
     *               activity
     *               it will receive the message
     */
    public void sendNoMsgToTarget(Object target) {
        sendToTargetOrType(target.getClass(), null);
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
        sendToTargetOrType(target.getClass(), token);
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
        unregisterFromLists(recipient, recipientsOfSubclassesAction);
        unregisterFromLists(recipient, recipientsStrictAction);
        cleanup();
    }


    public <T> void unregister(Object recipient, Object token) {
        unregisterFromLists(recipient, token, null, recipientsStrictAction);
        unregisterFromLists(recipient, token, null, recipientsOfSubclassesAction);
        cleanup();
    }


    private static <T> void sendToList(
            T message,
            Collection<WeakActionAndToken> list,
            Type messageTargetType,
            Object token) {
        if (list != null) {
            // Clone to protect from people registering in a "receive message" method
            // Bug correction Messaging BL0004.007
            ArrayList<WeakActionAndToken> listClone = new ArrayList<>();
            listClone.addAll(list);

            for (WeakActionAndToken item : listClone) {
                WeakAction executeAction = item.getAction();
                if (executeAction != null
                        && item.getAction().isLive()
                        && item.getAction().getTarget() != null
                        && (messageTargetType == null
                        || item.getAction().getTarget().getClass() == messageTargetType
                        || classImplements(item.getAction().getTarget().getClass(), messageTargetType))
                        && ((item.getToken() == null && token == null)
                        || item.getToken() != null && item.getToken().equals(token))) {
                    executeAction.execute(message);
                }
            }
        }
    }

    private static void unregisterFromLists(Object recipient, HashMap<Type, List<WeakActionAndToken>> lists) {
        if (recipient == null
                || lists == null
                || lists.size() == 0) {
            return;
        }
        synchronized (lists) {
            for (Type messageType : lists.keySet()) {
                for (WeakActionAndToken item : lists.get(messageType)) {
                    WeakAction weakAction = item.getAction();

                    if (weakAction != null
                            && recipient == weakAction.getTarget()) {
                        weakAction.markForDeletion();
                    }
                }
            }
        }
        cleanupList(lists);
    }

    /**
     * 模板方法：按 messageType 定位订阅列表，统一匹配 recipient/token/action 并标记待删除。
     * action 支持 {@link BindingAction} 与 {@link BindingConsumer} 两种回调，null 表示不限定回调。
     */
    private static void unregisterFromLists(
            Object recipient,
            Object token,
            Object action,
            HashMap<Type, List<WeakActionAndToken>> lists,
            Type messageType) {
        if (recipient == null
                || lists == null
                || lists.size() == 0
                || !lists.containsKey(messageType)) {
            return;
        }

        synchronized (lists) {
            for (WeakActionAndToken item : lists.get(messageType)) {
                WeakAction weakAction = item.getAction();

                if (weakAction != null
                        && recipient == weakAction.getTarget()
                        && (action == null
                        || action == weakAction.getBindingConsumer()
                        || action == weakAction.getBindingAction())
                        && (token == null
                        || token.equals(item.getToken()))) {
                    weakAction.markForDeletion();
                }
            }
        }
    }

    private static void unregisterFromLists(
            Object recipient,
            Object token,
            Object action,
            HashMap<Type, List<WeakActionAndToken>> lists) {
        unregisterFromLists(recipient, token, action, lists, NotMsgType.class);
    }

    private static boolean classImplements(Type instanceType, Type interfaceType) {
        if (interfaceType == null
                || instanceType == null) {
            return false;
        }
        Class[] interfaces = ((Class) instanceType).getInterfaces();
        for (Class currentInterface : interfaces) {
            if (currentInterface == interfaceType) {
                return true;
            }
        }

        return false;
    }

    private static void cleanupList(HashMap<Type, List<WeakActionAndToken>> lists) {
        if (lists == null) {
            return;
        }
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
                if (itemList.size() == 0) {
                    iterator.remove();
                }
            }
        }
    }

    private void sendToTargetOrType(Type messageTargetType, Object token) {
        Class messageType = NotMsgType.class;
        if (recipientsOfSubclassesAction != null) {
            // Clone to protect from people registering in a "receive message" method
            // Bug correction Messaging BL0008.002
//            var listClone = recipientsOfSubclassesAction.Keys.Take(_recipientsOfSubclassesAction.Count()).ToList();
            List<Type> listClone = new ArrayList<>();
            listClone.addAll(recipientsOfSubclassesAction.keySet());
            for (Type type : listClone) {
                List<WeakActionAndToken> list = null;

                if (messageType == type
                        || ((Class) type).isAssignableFrom(messageType)
                        || classImplements(messageType, type)) {
                    list = recipientsOfSubclassesAction.get(type);
                }

                sendToList(list, messageTargetType, token);
            }
        }

        if (recipientsStrictAction != null) {
            if (recipientsStrictAction.containsKey(messageType)) {
                List<WeakActionAndToken> list = recipientsStrictAction.get(messageType);
                sendToList(list, messageTargetType, token);
            }
        }

        cleanup();
    }

    private static void sendToList(
            Collection<WeakActionAndToken> list,
            Type messageTargetType,
            Object token) {
        if (list != null) {
            // Clone to protect from people registering in a "receive message" method
            // Bug correction Messaging BL0004.007
            ArrayList<WeakActionAndToken> listClone = new ArrayList<>();
            listClone.addAll(list);

            for (WeakActionAndToken item : listClone) {
                WeakAction executeAction = item.getAction();
                if (executeAction != null
                        && item.getAction().isLive()
                        && item.getAction().getTarget() != null
                        && (messageTargetType == null
                        || item.getAction().getTarget().getClass() == messageTargetType
                        || classImplements(item.getAction().getTarget().getClass(), messageTargetType))
                        && ((item.getToken() == null && token == null)
                        || item.getToken() != null && item.getToken().equals(token))) {
                    executeAction.execute();
                }
            }
        }
    }

    private <T> void sendToTargetOrType(T message, Type messageTargetType, Object token) {
        Class messageType = message.getClass();


        if (recipientsOfSubclassesAction != null) {
            // Clone to protect from people registering in a "receive message" method
            // Bug correction Messaging BL0008.002
//            var listClone = recipientsOfSubclassesAction.Keys.Take(_recipientsOfSubclassesAction.Count()).ToList();
            List<Type> listClone = new ArrayList<>();
            listClone.addAll(recipientsOfSubclassesAction.keySet());
            for (Type type : listClone) {
                List<WeakActionAndToken> list = null;

                if (messageType == type
                        || ((Class) type).isAssignableFrom(messageType)
                        || classImplements(messageType, type)) {
                    list = recipientsOfSubclassesAction.get(type);
                }

                sendToList(message, list, messageTargetType, token);
            }
        }

        if (recipientsStrictAction != null) {
            if (recipientsStrictAction.containsKey(messageType)) {
                List<WeakActionAndToken> list = recipientsStrictAction.get(messageType);
                sendToList(message, list, messageTargetType, token);
            }
        }

        cleanup();
    }

    private class WeakActionAndToken {
        private WeakAction action;
        private Object token;

        public WeakActionAndToken(WeakAction action, Object token) {
            this.action = action;
            this.token = token;
        }

        public WeakAction getAction() {
            return action;
        }

        public void setAction(WeakAction action) {
            this.action = action;
        }

        public Object getToken() {
            return token;
        }

        public void setToken(Object token) {
            this.token = token;
        }
    }

    public static class NotMsgType {

    }
}
