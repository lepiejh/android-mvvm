package com.ved.framework.binding.viewadapter.edittext;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.ved.framework.command.BindingCommand;
import com.ved.framework.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

import androidx.databinding.BindingAdapter;

/**
 * Created by ved on 2017/6/16.
 */

public class ViewAdapter {

    /**
     * 查表法：将自定义的 {@link com.ved.framework.entity.InputType} 常量映射为 Android 原生 {@link InputType} 值，
     * 替代原先 35 个 if-else 分支，新增类型只需扩展映射表即可。
     */
    private static final Map<Integer, Integer> INPUT_TYPE_MAP = new HashMap<>();

    static {
        //"none" 无限制类型
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_NULL, InputType.TYPE_NULL);
        //"text" 普通文本类型
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_VARIATION_NORMAL, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        //"textCapCharacters" 全部字符大写
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        //"textCapWords" 单词首字母大写
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_FLAG_CAP_WORDS, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        //"textCapSentences" 句子首字母大写
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        //"textAutoCorrect" 自动修正
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_FLAG_AUTO_CORRECT, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        //"textAutoComplete" 自动补全
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
        //"textMultiLine" 多行输入
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_FLAG_MULTI_LINE, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        //"textImeMultiLine" 输入法多行输入
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
        //"textNoSuggestions" 无提示候选信息
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        //"textUri" uri格式输入
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_VARIATION_URI, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        //"textEmailAddress" 邮件地址格式
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        //"textEmailSubject" 邮件主题
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT);
        //"textShortMessage" 短消息信息模式
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE);
        //"textLongMessage" 长消息信息模式
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE);
        //"textPersonName" 人名输入
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_VARIATION_PERSON_NAME, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        //"textPostalAddress" 邮寄地址
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        //"textPassword" 密码格式
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        //"textVisiblePassword" 密码可见格式
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        //"textWebEditText" web表单格式
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT);
        //"textFilter" 文本筛选
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_VARIATION_FILTER, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_FILTER);
        //"textPhonetic" 拼音输入
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_VARIATION_PHONETIC, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PHONETIC);
        //"textWebEmailAddress" web表单中添加邮件地址
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS);
        //"textWebPassword" web表单中添加密码
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD);
        //"number" 数字格式
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_NUMBER_VARIATION_NORMAL, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        //"numberSigned" 有符号数字格式
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_NUMBER_FLAG_SIGNED, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        //"numberDecimal" 浮点数字格式
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_NUMBER_FLAG_DECIMAL, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        //"numberPassword" 纯数字密码格式
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_NUMBER_VARIATION_PASSWORD, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        //"phone" 电话号码模式
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_PHONE);
        //"datetime" 时间日期格式
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_DATETIME_VARIATION_NORMAL, InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_NORMAL);
        //"date" 日期键盘
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_DATETIME_VARIATION_DATE, InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE);
        //"time" 时间键盘
        INPUT_TYPE_MAP.put(com.ved.framework.entity.InputType.TYPE_DATETIME_VARIATION_TIME, InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
    }

    /**
     * EditText重新获取焦点的事件绑定
     */
    @BindingAdapter(value = {"requestFocus"}, requireAll = false)
    public static void requestFocusCommand(EditText editText, final Boolean needRequestFocus) {
        if (needRequestFocus != null && needRequestFocus) {
            editText.setSelection(editText.getText().length());
            editText.requestFocus();
            InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        }
        editText.setFocusableInTouchMode(needRequestFocus != null && needRequestFocus);
    }

    /**
     * EditText输入文字改变的监听
     */
    @BindingAdapter(value = {"beforeTextChanged","textChanged","afterTextChanged"}, requireAll = false)
    public static void addTextChangedListener(EditText editText, final BindingCommand<String> beforeTextChanged,
                                              final BindingCommand<String> textChanged,
                                              final BindingCommand<com.ved.framework.entity.Editable> afterTextChanged) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (beforeTextChanged != null){
                    beforeTextChanged.execute(charSequence.toString());
                }
            }

            @Override
            public void onTextChanged(CharSequence text, int i, int i1, int i2) {
                if (textChanged != null) {
                    textChanged.execute(text.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (afterTextChanged != null){
                    afterTextChanged.execute(new com.ved.framework.entity.Editable(editText,editable));
                }
            }
        });
    }

    /**
     * EditText imeOptions的事件绑定
     * 设置：android:inputType="text|textVisiblePassword"
     *     android:imeOptions="actionSearch"
     */
    @BindingAdapter(value = {"onEditorActionListener"}, requireAll = false)
    public static void setOnEditorActionListener(EditText editText, final BindingCommand<String> onEditorActionListener) {
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // 执行搜索操作
                if (onEditorActionListener != null) {
                    onEditorActionListener.execute(editText.getText().toString());
                }
                return true;
            }
            return false;
        });
    }

    @BindingAdapter(value = {"digits"}, requireAll = false)
    public static void digitsCommand(EditText editView, final String digit){
        if (StringUtils.isNotEmpty(digit)) {
            editView.setKeyListener(DigitsKeyListener.getInstance(digit));
        }
    }

    @BindingAdapter(value = {"inputType"}, requireAll = false)
    public static void inputTypeCommand(EditText editView, final int type) {
        Integer inputType = INPUT_TYPE_MAP.get(type);
        if (inputType != null) {
            editView.setInputType(inputType);
        }
    }
}
