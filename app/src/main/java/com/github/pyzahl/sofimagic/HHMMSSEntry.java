package com.github.pyzahl.sofimagic;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.TextView;

import com.sony.scalar.sysutil.ScalarInput;

public class HHMMSSEntry extends TextView {

    public HHMMSSEntry(Context context) {
        super(context);
    }

    public HHMMSSEntry(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public HHMMSSEntry(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public int getSec(){
        String[] hms = getText().toString().split(":");
        int h = Integer.parseInt(hms[0]);
        int m = Integer.parseInt(hms[1]);
        int s = Integer.parseInt(hms[2]);
        return h*3600+m*60+s;
    }

    public void setSec(int s){
            long tmp = Math.abs(s);
            long HH = tmp / 3600;
            tmp -= HH * 3600;
            long MM = tmp / 60;
            tmp -= MM * 60;
            long SS = tmp;
            if (s >= 0)
                setText(String.format("%02d:%02d:%02d", HH, MM, SS));
            else
                setText(String.format("-%02d:%02d:%02d", HH, MM, SS));
    }

    @Override
    public void setOnKeyListener(OnKeyListener l) {
        super.setOnKeyListener(l);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        setTextColor(isFocused() ? Color.GREEN : Color.WHITE);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        int s = getSec();
        switch (event.getScanCode()) {
            case ScalarInput.ISV_DIAL_1_CLOCKWISE: s++; break;
            case ScalarInput.ISV_DIAL_1_COUNTERCW: s--; break;
            case ScalarInput.ISV_DIAL_2_CLOCKWISE: s+=60; break;
            case ScalarInput.ISV_DIAL_2_COUNTERCW: s-=60; break;
            case ScalarInput.ISV_DIAL_3_CLOCKWISE: s+=3600; break;
            case ScalarInput.ISV_DIAL_3_COUNTERCW: s-=3600; break;
            default:
                return super.onKeyUp(keyCode, event);
        }
        setSec(s);
        return super.onKeyUp(keyCode, event);
    }
}
