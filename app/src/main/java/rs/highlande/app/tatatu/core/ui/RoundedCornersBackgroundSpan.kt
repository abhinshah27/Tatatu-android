/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.app.tatatu.core.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.LineBackgroundSpan

/**
 * @author mbaldrighi on 7/4/2018.
 */
class RoundedCornersBackgroundSpan(
    private val mBackgroundColor: Int,
    private val mPadding: Int,
    private val mRadius: Float
) : LineBackgroundSpan {
    private val mBgRect: RectF

    init {
        // Pre-create rect for performance
        mBgRect = RectF()
    }

    override fun drawBackground(
        c: Canvas,
        p: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lnum: Int
    ) {
        //		char ch = text.charAt(text.length() - 1);
        //		if (lnum > 0 &&	(Character.isWhitespace(ch) || Character.isSpaceChar(ch))) {
        //			text = text.subSequence(0, text.length() - 2);
        //		}
        val textWidth = Math.round(p.measureText(text, start, end))
        val paintColor = p.color
        // Draw the background
        mBgRect.set(
            (left - mPadding).toFloat(),
            (top + mPadding * 2 / 3).toFloat(),
            (left + textWidth + mPadding).toFloat(),
            baseline.toFloat() + p.descent() + (mPadding / 3).toFloat()
        )
        p.color = mBackgroundColor
        c.drawRoundRect(mBgRect, mRadius, mRadius, p)
        p.color = paintColor
    }
}
