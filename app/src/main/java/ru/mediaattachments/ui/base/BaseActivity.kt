package ru.mediaattachments.ui.base

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import ru.mediaattachments.databinding.PopUpOptionsVerticalBinding


open class BaseActivity : AppCompatActivity() {

    open var popUpAlert: PopupWindow? = null

    open fun closeKeyBoard() {
        val imm: InputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    open fun showPopupVerticalOptions(
        topHeaderMessage: String,
        secondHeaderMessage: String = "",
        actionText: String,
        action: () -> Unit,
        secondaryActionText: String = "",
        secondaryAction: (() -> Unit)? = null,
        dismissActionText: String,
        onDismiss: (() -> Unit)? = null,
    ) {
        if (popUpAlert == null) {
            disableUserInteractionForAWhile(disableTimeInMillis = 500)
            closeKeyBoard()
            val binding = PopUpOptionsVerticalBinding.inflate(layoutInflater)

            popUpAlert = PopupWindow(
                binding.root, ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT, true
            )

            popUpAlert?.showAtLocation(binding.root, Gravity.NO_GRAVITY, 0, 0)

            binding.apply {

                if (secondaryAction != null) {
                    popUpOptionsVerticalBottomActionTv.visibility = View.VISIBLE
                    popUpOptionsVerticalBottomDivider.visibility = View.VISIBLE
                    popUpOptionsVerticalBottomActionTv.text = secondaryActionText

                    popUpOptionsVerticalBottomActionTv.setOnClickListener {
                        popUpAlert?.dismiss()
                        popUpAlert = null
                        secondaryAction.invoke()
                    }
                }

                popUpOptionsVerticalMainHeaderTv.text = topHeaderMessage
                popUpOptionsVerticalSecondaryHeaderTv.text = secondHeaderMessage
                if (secondHeaderMessage.isNullOrEmpty()) {
                    popUpOptionsVerticalSecondaryHeaderTv.visibility = View.GONE
                }

                popUpOptionsVerticalTopActionTv.text = actionText
                popUpOptionsVerticalTopActionTv.text = dismissActionText
                popUpOptionsVerticalContentCl.apply {
                    alpha = 0f
                    scaleX = 0f
                    scaleY = 0f
                    visibility = View.VISIBLE
                    animate().alpha(1f)
                        .scaleY(1f)
                        .scaleX(1f)
                        .duration = 200

                }

                popUpOptionsVerticalMainCl.setOnClickListener {
                    popUpAlert?.dismiss()
                    popUpAlert = null
                }
                popUpOptionsVerticalMiddleActionTv.setOnClickListener {
                    popUpAlert?.dismiss()
                    popUpAlert = null
                    onDismiss?.invoke()
                }
                popUpOptionsVerticalMiddleActionTv.setOnClickListener {
                    popUpAlert?.dismiss()
                    popUpAlert = null
                    action.invoke()
                }
            }

            popUpAlert?.setOnDismissListener {
                onDismiss?.invoke()
                popUpAlert = null
            }
        }
    }

    open fun disableUserInteractionForAWhile(disableTimeInMillis: Long) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
        Handler(Looper.getMainLooper()).postDelayed({
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }, disableTimeInMillis)
    }

}