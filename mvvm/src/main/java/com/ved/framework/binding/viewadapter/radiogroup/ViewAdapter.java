package com.ved.framework.binding.viewadapter.radiogroup;

import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.ved.framework.binding.command.BindingCommand;

import androidx.annotation.IdRes;
import androidx.databinding.BindingAdapter;

/**
 * Created by ved on 2017/6/18.
 */
public class ViewAdapter {
    @BindingAdapter(value = {"onCheckedChangedCommand","isRepeat"}, requireAll = false)
    public static void onCheckedChangedCommand(final RadioGroup radioGroup, final BindingCommand<String> bindingCommand,final boolean isRepeat) {
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // checkedId 可能为 -1（RadioGroup.clearCheck() 时触发），需判空避免 NPE
            RadioButton radioButton = group.findViewById(checkedId);
            if (radioButton == null) {
                return;
            }
            if (isRepeat && !radioButton.isPressed()){
                return;
            }
            if (bindingCommand != null) {
                bindingCommand.execute(radioButton.getText().toString());
            }
        });
    }
}
