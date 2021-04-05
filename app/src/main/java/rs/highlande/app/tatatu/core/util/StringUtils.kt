package rs.highlande.app.tatatu.core.util

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.text.*
import android.text.style.URLSpan
import android.widget.TextView
import androidx.annotation.StringRes
import rs.highlande.app.tatatu.R
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParseException
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.abs


/**
 * Child class of [URLSpan] removing default URL underline.
 */
class URLSpanNoUnderline(url: String) : URLSpan(url) {
    override fun updateDrawState(drawState: TextPaint) {
        super.updateDrawState(drawState)
        drawState.isUnderlineText = false
    }
}

/**
 * Styles an original underlined [Spannable] removing underline.
 * @param text the spannable to by styled.
 */
fun removeUnderlines(text: Spannable) {
    val spans = text.getSpans(0, text.length, URLSpan::class.java)

    for (span in spans) {
        val start = text.getSpanStart(span)
        val end = text.getSpanEnd(span)
        text.removeSpan(span)
        val newSpan = URLSpanNoUnderline(span.url)
        text.setSpan(newSpan, start, end, 0)
    }
}

/**
 * Adds pre-existent HTML formatting to text.
 * @param seq the String to be HTML formatted.
 * @return The [Spanned] object trasformed from the original String.
 */
fun getFormattedHtml(seq: String?): Spanned? {
    return if (!seq.isNullOrBlank()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(seq, Html.FROM_HTML_MODE_LEGACY)
        else
            Html.fromHtml(seq)
    } else null
}

/**
 * Adds pre-existent HTML formatting to text.
 * @param res the application [Resources].
 * @param resId the [StringRes] int pointing to the String to be HTML formatted.
 * @return The [Spanned] object trasformed from the original String.
 */
fun getFormattedHtml(res: Resources, @StringRes resId: Int): Spanned? {
    val seq = res.getString(resId)
    return if (!seq.isBlank()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(seq, Html.FROM_HTML_MODE_LEGACY)
        else
            Html.fromHtml(seq)
    } else null
}

/**
 * Extension function to add pre-existent HTML formatting to text.
 * @param res the application [Resources].
 * @param resId the [StringRes] int pointing to the String to be HTML formatted.
 * @return The [Spanned] object trasformed from the original String.
 */
fun TextView.setFormattedHtml(seq: String?) {
    if (!seq.isNullOrBlank()) {
        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(seq, Html.FROM_HTML_MODE_LEGACY)
        else
            Html.fromHtml(seq)
    }
}

/**
 * Extension function to add pre-existent HTML formatting to text.
 * @param res the application [Resources].
 * @param resId the [StringRes] int pointing to the String to be HTML formatted.
 * @return The [Spanned] object trasformed from the original String.
 */
fun TextView.setFormattedHtml(@StringRes resId: Int) {
    val seq = resources.getString(resId)
    if (!seq.isBlank()) {
        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(seq, Html.FROM_HTML_MODE_LEGACY)
        else
            Html.fromHtml(seq)
    }
}


const val REGEX_EMAIL = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-\\+]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
const val REGEX_PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#!%*?&+])[A-Za-z\\d@#!%*?&+]{8,}"
const val REGEX_DOB= "^(0[0-9]||1[0-2])/([0-2][0-9]||3[0-1])/([0-9][0-9])?[0-9][0-9]\$"
const val REGEX_MOBILE = "^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$"

/**
 * Checks if the provided email string matches with the [REGEX_EMAIL] pattern.
 * @param email the provided email string.
 * @return True if the string matches the pattern, false otherwise.
 */
fun isEmailValid(email: String): Boolean {
    return doesStringMatchPattern(email, REGEX_EMAIL)
}

/**
 * Checks if the provided password string matches with the [REGEX_PASSWORD] pattern.
 * @param password the provided password string.
 * @return True if the string matches the pattern, false otherwise.
 */
fun isPasswordValid(password: String): Boolean {

    // INFO: 2019-09-23    Pwd validation is now performed only on length. All other constraints are removed.
//    return doesStringMatchPattern(password, REGEX_PASSWORD)

    return password.length >= 8
}

fun isDateOfBirthValid(DOB: String): Boolean {
    return doesStringMatchPattern(DOB, REGEX_DOB)
}

/**
 * Checks if the provided email string matches with the [REGEX_MOBILE] pattern.
 * @param countryCode the provided email string.
 * @param mobileNumber the provided email string.
 * @return True if the string matches the pattern, false otherwise.
 */
fun isPhoneValid(countryCode: String, mobileNumber: String): Boolean {
    var mMatcher: Matcher? = null
    val r = Pattern.compile(REGEX_MOBILE)
    val finalMobileNumber = countryCode + mobileNumber
    if (!TextUtils.isEmpty(finalMobileNumber)) {
        mMatcher = r.matcher(finalMobileNumber)
    }
    if (mMatcher!!.find()) {
        return true
    }
    return false
}

/**
 * Checks if the provided email string matches with the [REGEX_MOBILE] pattern.
 * @param mobileNumber the provided email string.
 * @return True if the string matches the pattern, false otherwise.
 */
fun isPhoneValid(mobileNumber: String): Boolean {
    var mMatcher: Matcher? = null
    val r = Pattern.compile(REGEX_MOBILE)
    if (!TextUtils.isEmpty(mobileNumber)) {
        mMatcher = r.matcher(mobileNumber)
    }
    if (mMatcher!!.find()) {
        return true
    }
    return false
}


/**
 * Checks if the provided [String] matches a [Pattern] created with a provided regEx string.
 * @param string the provided string to be checked.
 * @param regex the provided pattern.
 * @return True if string matches the pattern, false otherwise.
 */
fun doesStringMatchPattern(string: String, regex: String): Boolean {
    if (!areStringsValid(string, regex))
        return false
    val pattern = Pattern.compile(regex)
    val matcher = pattern.matcher(string)
    return matcher.matches()
}

/**
 * More easily accessible method to get whether the provided String is null of 0-length.
 * @param s the provided String.
 * @return Whether the provided String is null or 0-length.
 */
fun isStringValid(s: String?) = areStringsValid(s)

/**
 * More easily accessible method to get whether AT LEAST ONE String within a provided set is null
 * or 0-length.
 * @param arr the provided String set.
 * @return Whether the provided String set is null or 0-length.
 */
fun areStringsValid(vararg arr: String?): Boolean {
    for (s in arr) {
        if (s.isNullOrBlank()) return false
    }
    return true
}

/**
 * Copies any text to system clipboard.
 * @param context the activity/application [Context].
 * @param text the string to be copied to system clipboard.
 */
fun copyToClipboard(context: Context, text: String = "") {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText(COPY_TEXT, text)
    clipboard.setPrimaryClip(clip)
}

/**
 * Formats any provided int count to the wanted String "divided" by a provided multiplier.
 * E.g. 1000 = 1K
 *
 * @param res the application/activity's Context instance.
 * @param count the provided int count.
 * @return The wanted formatted String.
 */
fun getReadableCount(res: Resources, count: Int): String {
    try {
        if (count > 0) {
            @StringRes var stringRes = 0
            var dividingBy = 0.0
            when {
                abs(count) > 999999999 -> {
                    stringRes = R.string.number_multiplier_b_string
                    dividingBy = 1E9
                }
                abs(count) > 999999 -> {
                    stringRes = R.string.number_multiplier_m_string
                    dividingBy = 1E6
                }
                abs(count) > 999 -> {
                    stringRes = R.string.number_multiplier_k_string
                    dividingBy = 1E3
                }
            }

            if (stringRes == 0)
                return count.toString()
            else {
                val divided = abs(count / dividingBy)

                val nf = NumberFormat.getInstance(Locale.getDefault())
                val df = nf as DecimalFormat
                df.roundingMode = RoundingMode.FLOOR
                df.minimumFractionDigits = 0
                df.maximumFractionDigits = 1

                val s = df.format(divided)
                return String.format(
                    Locale.getDefault(),
                    res.getString(stringRes),
                    s
                )
            }
        }
    } catch (e: ArithmeticException) {
        LogUtils.e("getReadableCount() FAIL", e.message, e)
    }

    return 0.toString()
}


fun parseNumberWithCommas(numberToParse: String): Number {
    val format = NumberFormat.getInstance(Locale.US)
    var number: Number = 0
    try {
        number = format.parse(numberToParse) ?: 0
    } catch (e: ParseException) {
        e.printStackTrace()
    }

    return number
}

fun formatNumberWithCommas(numberToFormat: Long): String {
    val format = NumberFormat.getInstance(Locale.getDefault())
    return format.format(numberToFormat)
}

fun formatNumberWithCommas(numberToFormat: Double): String {
    val format = NumberFormat.getInstance(Locale.getDefault())
    return format.format(numberToFormat)
}


fun formatTTUTokens(tokens: Double): String {

    val nf = NumberFormat.getInstance(Locale.getDefault())
    nf.isGroupingUsed = true
    val df = nf as DecimalFormat
    df.roundingMode = RoundingMode.FLOOR
    df.minimumFractionDigits = 2

    return df.format(tokens)
}


fun formatCurrency(tokens: Double): String {

    val nf = NumberFormat.getInstance(Locale.getDefault())
    nf.isGroupingUsed = true
    val df = nf as DecimalFormat
    df.roundingMode = RoundingMode.FLOOR
    df.minimumFractionDigits = 2

    return df.format(tokens)
}

fun getFormattedPhoneNumber(phoneNumber: String) = phoneNumber.replace("[^\\d]".toRegex(), "").trim { it <= ' ' }
