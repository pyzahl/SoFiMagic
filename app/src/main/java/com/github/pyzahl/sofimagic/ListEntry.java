package com.github.pyzahl.sofimagic;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.Color;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sony.scalar.sysutil.ScalarInput;

import java.util.concurrent.Callable;

public class ListEntry extends TextView {

    public ListEntry(Context context) {
        super(context); index=0; imax=0;
        setSingleLine(true);
        //setEllipsize(TextUtils.TruncateAt.MARQUEE);
        setEms(10);
        setInputType(4); // "time"
        setWidth(60);
        setTextColor(Color.WHITE);
        setBackgroundColor(Color.parseColor("#f86d6d"));
        setFocusable(true);
        setClickable(true);
    }
    public ListEntry(Context context, AttributeSet attrs) {
        super(context, attrs); index=0; imax=0;
        setSingleLine(true);
        setWidth(60);
        setTextColor(Color.WHITE);
        setEms(10);
        setInputType(4); // "time"
        setBackgroundColor(Color.parseColor("#f86d6d"));
        setFocusable(true);
        setClickable(true);
    }
    public ListEntry(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); index=0; imax=0;
        setSingleLine(true);
        setWidth(60);
        setTextColor(Color.WHITE);
        setEms(10);
        setInputType(4); // "time"
        setBackgroundColor(Color.parseColor("#f86d6d"));
        setFocusable(true);
        setClickable(true);
    }

    private int index;
    private int imax;

    public void setMaxIndex(int max){
        imax = max;
    }


    public interface LookupFunction<RESULT,INPUT> {
        RESULT call(INPUT input);
    }

    private LookupFunction <String, Integer> Lookup;

    public void setLookup(LookupFunction <String, Integer> LookupFunc){
       Lookup = LookupFunc;
    }
    public String getItem(int i){
        if (i>=0 && i <= imax)
            return Lookup.call(i);
        return "?";
    }

    public int getIndex(){
        int i = index;
        return i;
    }

    public void setIndex(int i){
        if (i>= 0 && i <= imax) {
            index = i;
            setText(Lookup.call(i));
        }
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

        int i = getIndex();
        switch (event.getScanCode()) {
            case ScalarInput.ISV_DIAL_3_CLOCKWISE:
            case ScalarInput.ISV_DIAL_1_CLOCKWISE: i++; break;
            case ScalarInput.ISV_DIAL_3_COUNTERCW:
            case ScalarInput.ISV_DIAL_1_COUNTERCW: i--; break;
            case ScalarInput.ISV_DIAL_2_CLOCKWISE: i+=3; break;
            case ScalarInput.ISV_DIAL_2_COUNTERCW: i-=3; break;
            default:
                return super.onKeyUp(keyCode, event);
        }
        setIndex(i);
        return super.onKeyUp(keyCode, event);
    }
}
