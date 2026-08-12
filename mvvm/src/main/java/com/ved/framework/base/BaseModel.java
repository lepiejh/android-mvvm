package com.ved.framework.base;

import java.util.ArrayList;
import java.util.List;

/**
 * Model 层基类：
 * 按仓库模式组织数据源，持有并级联管理多个 {@link IRepository}，
 * 在 onCleared 时统一释放所有仓库资源。
 */
public class BaseModel implements IModel {
    private final List<IRepository> mRepositories = new ArrayList<>();

    public BaseModel() {
    }

    /**
     * 注册数据仓库，随 Model 生命周期统一释放
     */
    public void addRepository(IRepository repository) {
        if (repository != null) {
            mRepositories.add(repository);
        }
    }

    @Override
    public void onCleared() {
        for (IRepository repository : mRepositories) {
            repository.onCleared();
        }
        mRepositories.clear();
    }
}
