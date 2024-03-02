package com.github.pyzahl.sofimagic;

        import android.content.Context;
        import android.graphics.Rect;
        import android.graphics.Color;
        import android.util.AttributeSet;
        import android.view.KeyEvent;
        import android.widget.TextView;

        import com.sony.scalar.sysutil.ScalarInput;

public class IndexEntry extends TextView {

    public IndexEntry(Context context) {
        super(context);
    }
    public IndexEntry(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public IndexEntry(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private int index;

    private String prefix="";
    private String unit="";

    private int imax = 100;
    private int imin = 0;

    public void setPrefix(String Prefix){
        prefix=Prefix;
    }
    public void setUnit(String u){
        unit=u;
    }

    public void setRange(int min, int max){
        imin = min; imax = max;
    }

    public int getIndex(){
        int i = index; //Integer.parseInt(getText().toString());
        return i;
    }

    public void setIndex(int i){
        if (i>= imax && i <= imax) {
            index = i;
            setText(prefix + String.format("%d", index) + unit);
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
            case ScalarInput.ISV_DIAL_1_CLOCKWISE: i++; break;
            case ScalarInput.ISV_DIAL_1_COUNTERCW: i--; break;
            case ScalarInput.ISV_DIAL_2_CLOCKWISE: i+=10; break;
            case ScalarInput.ISV_DIAL_2_COUNTERCW: i-=10; break;
            case ScalarInput.ISV_DIAL_3_CLOCKWISE: i+=100; break;
            case ScalarInput.ISV_DIAL_3_COUNTERCW: i-=100; break;
            default:
                return super.onKeyUp(keyCode, event);
        }
        setIndex(i);
        return super.onKeyUp(keyCode, event);
    }
}
