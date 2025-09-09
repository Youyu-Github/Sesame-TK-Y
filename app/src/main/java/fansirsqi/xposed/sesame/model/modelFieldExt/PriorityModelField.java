package fansirsqi.xposed.sesame.model.modelFieldExt;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.model.ModelField;
import fansirsqi.xposed.sesame.ui.ChoiceDialog;

/**
 * PriorityModelField 现在继承自 ChoiceModelField，
 * 因此可以直接作为 ChoiceDialog.show 的第三个参数传入。
 */
public class PriorityModelField extends ChoiceModelField {
    private String[] choiceArray;

    // 下面的构造器将调用父类的构造器 —— 若父类构造器签名不同，请把 ChoiceModelField 的构造器贴上来，我会调整 super(...) 的参数。

    public PriorityModelField(String code, String name, Integer value) {
        super(code, name, value);
    }

    public PriorityModelField(String code, String name, Integer value, String[] choiceArray) {
        super(code, name, value, choiceArray);
        this.choiceArray = choiceArray;
    }

    public PriorityModelField(String code, String name, Integer value, String desc) {
        super(code, name, value, desc);
    }

    public PriorityModelField(String code, String name, Integer value, String[] choiceArray, String desc) {
        super(code, name, value, choiceArray, desc);
        this.choiceArray = choiceArray;
    }

    @Override
    public String getType() {
        return "CHOICE";
    }

    public String[] getExpandKey() {
        return choiceArray;
    }

    public boolean isEnable() {
        return getValue() > 0;
    }

    @Override
    public View getView(Context context) {
        Button btn = new Button(context);
        btn.setText(getName());
        btn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btn.setTextColor(ContextCompat.getColor(context, R.color.selection_color));
        btn.setBackground(ContextCompat.getDrawable(context, R.drawable.dialog_list_button));
        btn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        btn.setMinHeight(150);
        btn.setMaxHeight(180);
        btn.setPaddingRelative(40, 0, 40, 0);
        btn.setAllCaps(false);
        // 现在 this 是 ChoiceModelField 的子类，可以直接传入
        btn.setOnClickListener(v -> ChoiceDialog.show(v.getContext(), ((Button) v).getText(), this));
        return btn;
    }
}
