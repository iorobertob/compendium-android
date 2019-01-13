package com.ideasBlock.compendium.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ScrollView


class CustomScrollView : ScrollView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        when (action) {
            MotionEvent.ACTION_DOWN ->
                //Log.i("CustomScrollView", "onInterceptTouchEvent: DOWN super false" );
                super.onTouchEvent(ev)

            MotionEvent.ACTION_MOVE -> return false // redirect MotionEvents to ourselves

            MotionEvent.ACTION_CANCEL ->
                // Log.i("CustomScrollView", "onInterceptTouchEvent: CANCEL super false" );
                super.onTouchEvent(ev)

            MotionEvent.ACTION_UP ->
                //Log.i("CustomScrollView", "onInterceptTouchEvent: UP super false" );
                return false

            else -> {
            }
        }//Log.i("CustomScrollView", "onInterceptTouchEvent: " + action );

        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        super.onTouchEvent(ev)
        //Log.i("CustomScrollView", "onTouchEvent. action: " + ev.getAction() );
        return true
    }
}