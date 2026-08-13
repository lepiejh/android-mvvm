package com.ved.framework.utils.bland.code;
import com.ved.framework.utils.Utils;

import android.app.Application;

import androidx.core.content.FileProvider;

public class UtilsFileProvider extends FileProvider {

    @Override
    public boolean onCreate() {
        //noinspection ConstantConditions
        Utils.init((Application) getContext().getApplicationContext());
        return true;
    }
}
