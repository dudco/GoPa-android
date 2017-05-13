package com.example.dudco.gopa;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * Created by dudco on 2017. 5. 14..
 */

public class CustomEditText extends EditText {
    public CustomEditText(Context context) {
        super(context);
    }

    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        readAttributes(context, attrs);
    }

    public CustomEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        readAttributes(context, attrs);
    }

    public CustomEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        readAttributes(context, attrs);
    }

    private void setFont(Context context, String font){
        this.setTypeface(Typeface.createFromAsset(context.getAssets(), font));
    }

    private void readAttributes(Context context, AttributeSet attrs){
        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.CustomTextView);
        String filename = null;
        for(int i = 0 ; i < arr.getIndexCount(); i++){
            int attr = arr.getIndex(i);
            if(attr == R.styleable.CustomTextView_font){
                filename = arr.getString(attr);
            }
        }

        arr.recycle();
        if(filename != null){
            this.setTypeface(Typeface.createFromAsset(context.getAssets(), filename));
        }
    }
}
