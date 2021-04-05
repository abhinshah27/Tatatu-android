package rs.highlande.app.tatatu.core.ui

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import rs.highlande.app.tatatu.R
import rs.highlande.app.tatatu.core.util.isValid

/**
 * TODO - File description
 * @author mbaldrighi on 2019-07-29.
 */



class TTUAlertDialogBuilder(context: Context) : MaterialAlertDialogBuilder(context, R.style.TTUDialog)


fun showDialogGeneric(
    context: Context,
    message: String,
    title: String? = null,
    buttonPositive: String? = null,
    buttonNegative: String? = null,
    onPositive: DialogInterface.OnClickListener? = null,
    onNegative: DialogInterface.OnClickListener? = null,
    cancelable: Boolean = true,
    show: Boolean = true
): AlertDialog? {

    if (!context.isValid()) return null

    val builder = TTUAlertDialogBuilder(context).setMessage(message).setCancelable(cancelable)

    if (!title.isNullOrBlank())
        builder.setTitle(title)

    if (!buttonPositive.isNullOrBlank() && onPositive != null)
        builder.setPositiveButton(buttonPositive, onPositive)

    if (!buttonNegative.isNullOrBlank()) {
        builder.setNegativeButton(
            buttonNegative,
            onNegative ?: DialogInterface.OnClickListener { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
        )
    }

    return if (show) builder.show()
    else builder.create()
}

fun showDialogGeneric(
    context: Context,
    @StringRes message: Int,
    @StringRes title: Int? = null,
    @StringRes buttonPositive: Int? = null,
    @StringRes buttonNegative: Int? = null,
    onPositive: DialogInterface.OnClickListener? = null,
    onNegative: DialogInterface.OnClickListener? = null,
    cancelable: Boolean = true,
    show: Boolean = true
): AlertDialog? {

    if (!context.isValid()) return null

    val builder = TTUAlertDialogBuilder(context).setMessage(message).setCancelable(cancelable)

    if (title != null)
        builder.setTitle(title)

    if (buttonPositive != null && onPositive != null)
        builder.setPositiveButton(buttonPositive, onPositive)

    if (buttonNegative != null) {
        builder.setNegativeButton(
            buttonNegative,
            onNegative ?: DialogInterface.OnClickListener { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
        )
    }

    return if (show) builder.show()
    else builder.create()
}


fun showProgressDialog(
    context: Context,
    @StringRes message: Int? = null
) = showDialogGeneric(
    context,
    message ?: R.string.dialog_progress_generic,
    cancelable = false,
    show = true
)

fun getProgressDialog(
    context: Context,
    @StringRes message: Int? = null
) = showDialogGeneric(
    context,
    message ?: R.string.dialog_progress_generic,
    cancelable = false,
    show = true
)


fun showProgressDialog(
    context: Context,
    message: String? = null
) = showDialogGeneric(
    context,
    message ?: context.getString(R.string.dialog_progress_generic),
    cancelable = false,
    show = true
)

fun getProgressDialog(
    context: Context,
    message: String? = null
) = showDialogGeneric(
    context,
    message ?: context.getString(R.string.dialog_progress_generic),
    cancelable = false,
    show = true
)
