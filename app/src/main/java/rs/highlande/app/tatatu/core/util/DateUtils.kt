package rs.highlande.app.tatatu.core.util

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.annotation.PluralsRes
import rs.highlande.app.tatatu.R
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


/**
Created by Leandro Garcia - leandro.garcia@highlande.rs
 */


const val DB_FORMAT_DATE = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
const val EDIT_FORMAT_DATE = "MM/dd/yyyy"


/**
 * Extension function to convert any string date coming from the server to its millisecond value.
 *
 * @return The date's milliseconds `long` value.
 * @throws ParseException if something goes wrong while parsing the date in question.
 */
@Throws(ParseException::class)
fun String.getDateMillisFromDB(): Long {
    val dateIn = SimpleDateFormat(DB_FORMAT_DATE, Locale.getDefault())
    return dateIn.parse(this.replace("Z$".toRegex(), "+0000"))?.time ?: 0L
}


/**
 * Extension function to convert any string date coming from the server to a Java Date object.
 *
 * @return The wanted Date object.
 * @throws ParseException if something goes wrong while parsing the date in question.
 */
fun String.getDateFromDB(): Date? {
    return try {
        if (isNotBlank()) {

            var d = this
            if (contains("Z")) {
                val split = split("\\.")
                if (split.size == 2 && (split[1].length > 4))
                    d = split[0] + "." + split[1].substring(0, 3) + "Z"
            }

            val dateIn = SimpleDateFormat(DB_FORMAT_DATE, Locale.getDefault())
            dateIn.parse((d).replace("Z$".toRegex(), "+0000"))
        } else null
    } catch (ex: ParseException) {
        ex.printStackTrace()
        null
    }
}

/**
 * Extension function to convert any string date in a given string format to a Java Date object.
 *
 * @param dateFormat the given input's string format.
 * @return The wanted Date object.
 * @throws ParseException if something goes wrong while parsing the date in question.
 */
@Throws(ParseException::class)
fun String.convertStringToDate(dateFormat: String): Date? {
    return if (isNotBlank()) SimpleDateFormat(dateFormat, Locale.getDefault()).parse(this) else null
}


/**
 * Extension function to convert any Java Date object to a string date with the wanted format.
 *
 * @param dateFormat the wanted string format for the output.
 * @return The converted string date.
 */
fun Date.convertTimeAndDate(dateFormat: String): String {
    return SimpleDateFormat(dateFormat, Locale.getDefault()).format(this) ?: ""
}

/**
 * Extension function to convert any Java Date object to a string date with the format [DB_FORMAT_DATE] for TTU DB.
 *
 * @return The converted string date.
 */
fun Date.toDBDate(): String {
    return convertTimeAndDate(DB_FORMAT_DATE)
}

/**
 * Extension function to convert any millis [Long] representation to a string date with the format [DB_FORMAT_DATE] for TTU DB.
 *
 * @return The converted string date.
 */
fun Long.toDBDate(): String {
    return Date(this).convertTimeAndDate(DB_FORMAT_DATE)
}


fun nowToDBDate() = System.currentTimeMillis().toDBDate()


fun formatDateToAge(context: Context, date: String): String {

    val t1 = date.getDateFromDB()?.time ?: 0
    val t2 = System.currentTimeMillis()

    val timestamp = (t2 - t1)

    val formattedResult: String
    @PluralsRes val stringRes: Int

    when {
        timestamp < DateUtils.MINUTE_IN_MILLIS -> {
            formattedResult = "0"
            stringRes = R.plurals.date_min_plur
        }
        timestamp < DateUtils.HOUR_IN_MILLIS -> {
            formattedResult = Date(timestamp).convertTimeAndDate("m")
            stringRes = R.plurals.date_min_plur
        }
        timestamp < DateUtils.DAY_IN_MILLIS -> {
            formattedResult = Date(timestamp).convertTimeAndDate("H")
            stringRes = R.plurals.date_hour_plur
        }
        timestamp < (DateUtils.DAY_IN_MILLIS * 30) -> {
            formattedResult = Date(timestamp).convertTimeAndDate("d")
            stringRes = R.plurals.date_day_plur
        }
        timestamp < DateUtils.YEAR_IN_MILLIS -> {
            formattedResult = Date(timestamp).convertTimeAndDate("M")
            stringRes = R.plurals.date_month_plur
        }
        else -> {
            formattedResult = Date(timestamp).convertTimeAndDate("y")
            stringRes = R.plurals.date_year_plur
        }
    }

    return String.format(
        Locale.getDefault(),
        context.resources.getQuantityString(stringRes, formattedResult.toInt()),
        formattedResult
    )
}

// TODO : Make format con
fun getDateDifferenceFromNowInDay(date: String): Long {
    val dateInMillis = date.convertStringToDate(DB_FORMAT_DATE)?.time ?: 0
    val currentTimeInMillis = System.currentTimeMillis()
    val differenceInMillis = (currentTimeInMillis - dateInMillis)
    return TimeUnit.MILLISECONDS.toDays(differenceInMillis)
}

fun isUser18Older(dobString: String): Boolean {
    val birthDate: Date = dobString.convertStringToDate(EDIT_FORMAT_DATE) ?: return false
    var years = 0
    var months = 0
    var days = 0

    //create calendar object for birth day
    val birthDay = Calendar.getInstance()
    birthDay.timeInMillis = birthDate.time

    //create calendar object for current day
    val currentTime = System.currentTimeMillis()
    val now = Calendar.getInstance()
    now.timeInMillis = currentTime

    //Get difference between years
    years = now.get(Calendar.YEAR) - birthDay.get(Calendar.YEAR)
    val currMonth = now.get(Calendar.MONTH) + 1
    val birthMonth = birthDay.get(Calendar.MONTH) + 1

    //Get difference between months
    months = currMonth - birthMonth

    //if month difference is in negative then reduce years by one
    //and calculate the number of months.
    if (months < 0) {
        years--
        months = 12 - birthMonth + currMonth
        if (now.get(Calendar.DATE) < birthDay.get(Calendar.DATE))
            months--
    } else if (months == 0 && now.get(Calendar.DATE) < birthDay.get(Calendar.DATE)) {
        years--
        months = 11
    }

    //Calculate the days
    if (now.get(Calendar.DATE) > birthDay.get(Calendar.DATE))
        days = now.get(Calendar.DATE) - birthDay.get(Calendar.DATE)
    else if (now.get(Calendar.DATE) < birthDay.get(Calendar.DATE)) {
        val today = now.get(Calendar.DAY_OF_MONTH)
        now.add(Calendar.MONTH, -1)
        days = now.getActualMaximum(Calendar.DAY_OF_MONTH) - birthDay.get(Calendar.DAY_OF_MONTH) + today
    } else {
        days = 0
        if (months == 12) {
            years++
            months = 0
        }
    }
    return years >= 18
}


fun convertEditProfileDateToDB(date: String?): String {
    return date?.let {
        it.convertStringToDate(EDIT_FORMAT_DATE)?.toDBDate() ?: ""
    } ?: ""
}

fun getEditProfileDateFromDB(date: String?): String {
    return date?.let {
        it.getDateFromDB()?.convertTimeAndDate(EDIT_FORMAT_DATE) ?: ""
    } ?: ""
}

/**
 * Method used to format any date expressed in milliseconds with a provided format.
 *
 * @param millis the date to be sent to the server expressed in milliseconds.
 * @return The wanted formatted date String.
 */
fun formatDate(millis: Long, format: String): String {
    if (!isStringValid(format)) return ""

    val dateOut = SimpleDateFormat(format, Locale.getDefault())
    return dateOut.format(Date(millis))
}

/**
 * Method used to format any {@link Date} to be sent to the server.
 *
 * @param date the {@link Date} to be sent to the server.
 * @return The wanted formatted date String.
 */
fun formatDate(date: Date?, format: String): String {
    if (date == null || !isStringValid(format)) return ""

   return SimpleDateFormat(
        DateFormat.getBestDateTimePattern(
            Locale.getDefault(),
            format.replace(" ", "")
        ),
        Locale.getDefault()
    ).format(date)
}

/**
 * Method used to format any [Date] to be sent to the server.
 *
 * @param date the [Date] to be sent to the server.
 * @return The wanted formatted date String.
 */
fun formatTime(context: Context?, date: Date?): String {
    return if (date == null || !isContextValid(context)) ""
    else DateFormat.getTimeFormat(context).format(date)

}

/**
 * Method used to format any [Date] to be sent to the server.
 *
 * @param date the [Date] to be sent to the server.
 * @return The wanted formatted date String.
 */
fun formatDateWithTime(context: Context?, date: Date?, dateFormat: String, separator: Boolean): String {
    return if (date == null || !isStringValid(dateFormat)) ""
    else formatDate(date, dateFormat) + (if (separator) ", " else "") + formatTime(context, date)
}

/**
 * Method used to check whether two dates correspond to the exact same day.
 * @return `true` if the two provided dates point to the same day, `false` otherwise.
 */
fun Date.isSameDateAsNow(): Boolean {

    val cal = Calendar.getInstance().apply { this.time = this@isSameDateAsNow }
    val calNow = Calendar.getInstance()

    return cal[Calendar.YEAR] == calNow[Calendar.YEAR] &&
            cal[Calendar.MONTH] == calNow[Calendar.MONTH] &&
            cal[Calendar.DAY_OF_MONTH] == calNow[Calendar.DAY_OF_MONTH]
}

/**
 * Method used to check whether two dates have the exact same year.
 * @return `true` if the two provided dates point to the same year, `false` otherwise.
 */
fun Date.isSameYear(): Boolean {

    val cal = Calendar.getInstance().apply { this.time = this@isSameYear }
    val calNow = Calendar.getInstance()

    return cal[Calendar.YEAR] == calNow[Calendar.YEAR]
}