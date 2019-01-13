package com.ideasBlock.compendium.utils

import android.support.v7.widget.LinearSnapHelper
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.NO_POSITION

class SnapHelperOneByOne : LinearSnapHelper() {

    /*
     * Interface to send data to the main thread
    */
    var mPositionInterface: PositionUpdate? = null

    /**
     * Interface to exchange data with the ain UserHome thread
     */
    interface PositionUpdate {
        fun positionUpdate(position: Int)
    }

    override fun findTargetSnapPosition(
        layoutManager: RecyclerView.LayoutManager?,
        velocityX: Int,
        velocityY: Int
    ): Int {

        if (layoutManager !is RecyclerView.SmoothScroller.ScrollVectorProvider) {
            return RecyclerView.NO_POSITION
        }

        val currentView = findSnapView(layoutManager) ?: return RecyclerView.NO_POSITION

        val currentPosition = layoutManager.getPosition(currentView)

        if (currentPosition != NO_POSITION)
        {
            mPositionInterface?.positionUpdate(currentPosition)
        }

        return if (currentPosition == RecyclerView.NO_POSITION) {
            RecyclerView.NO_POSITION
        } else currentPosition
    }
}