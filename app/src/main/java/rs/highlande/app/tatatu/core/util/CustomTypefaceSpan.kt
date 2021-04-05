package rs.highlande.app.tatatu.core.util

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.TypefaceSpan


/**
 * TODO - Class description
 * @author mbaldrighi on 2019-08-07.
 */
class HLCustomTypefaceSpan(family: String, type: Typeface) : TypefaceSpan(family) {

    private val newType: Typeface = type



//    // TODO: 3/20/2018   if ever used, needs to be written correctly
//    constructor(`in`: Parcel) : this(`in`) {
//
//        // typeface needs to be read from parcel
//    }

    //endregion


    override fun updateDrawState(ds: TextPaint) {
        applyCustomTypeface(ds, newType)
    }

    override fun updateMeasureState(paint: TextPaint) {
        applyCustomTypeface(paint, newType)
    }

    companion object {

        private fun applyCustomTypeface(paint: Paint, tf: Typeface) {
            val oldStyle: Int
            val old = paint.typeface
            oldStyle = old?.style ?: 0

            val fake = oldStyle and tf.style.inv()
            if (fake and Typeface.BOLD != 0) {
                paint.isFakeBoldText = true
            }

            if (fake and Typeface.ITALIC != 0) {
                paint.textSkewX = -0.25f
            }

            paint.typeface = tf
        }


        // region == Parcelable CREATOR ==

//        val CREATOR: Parcelable.Creator<*> = object : Parcelable.Creator {
//            override fun createFromParcel(`in`: Parcel): HLCustomTypefaceSpan {
//                return HLCustomTypefaceSpan(`in`)
//            }
//
//            override fun newArray(size: Int): Array<HLCustomTypefaceSpan> {
//                return arrayOfNulls(size)
//            }
//        }
    }

    //endregion
}
