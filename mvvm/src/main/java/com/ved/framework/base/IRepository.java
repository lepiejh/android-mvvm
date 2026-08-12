package com.ved.framework.base;

/**
 * 数据仓库统一抽象：
 * 隔离数据获取（网络/本地缓存）细节，供 Model 层按仓库粒度组织数据源，
 * 随宿主（Model/ViewModel）生命周期释放资源。
 */
public interface IRepository {

    void onCleared();
}
