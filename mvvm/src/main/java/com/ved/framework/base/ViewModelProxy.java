package com.ved.framework.base;

interface ViewModelProxy<VM extends BaseViewModel>{
    VM createViewModel();
}
