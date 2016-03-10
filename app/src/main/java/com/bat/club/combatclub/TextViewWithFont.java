package com.bat.club.combatclub;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by Noyloy on 27-Jan-16.
 */
public class TextViewWithFont extends TextView {

    public TextViewWithFont(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setTypeface(GameSession.typeface);
    }

    public TextViewWithFont(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setTypeface(GameSession.typeface);
    }

    public TextViewWithFont(Context context) {
        super(context);
        this.setTypeface(GameSession.typeface);
    }

}
