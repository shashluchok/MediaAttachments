package ru.mediaattachments.presentation.base

import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import ru.mediaattachments.databinding.PopUpOptionsVerticalBinding
import ru.mediaattachments.utils.fadeIn

private const val disableTimeInMillis = 500L
private const val popUpAnimationDuration = 200L

open class BaseActivity : AppCompatActivity() {

    open var popUpAlert: PopupWindow? = null
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
            disableUserInteractionForAWhile(disableTimeInMillis = disableTimeInMillis)
            val binding = PopUpOptionsVerticalBinding.inflate(layoutInflater)
            popUpAlert = PopupWindow(
                binding.root, ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT, true
            )

            popUpAlert?.showAtLocation(binding.root, Gravity.NO_GRAVITY, 0, 0)

            binding.apply {

                mainHeaderTv.text = topHeaderMessage
                actionTv.text = actionText
                dismissActionTv.text = dismissActionText

                actionTv.setOnClickListener {
                    action()
                    popUpAlert?.dismiss()
                }
                dismissActionTv.setOnClickListener {
                    popUpAlert?.dismiss()
                }


                if (secondHeaderMessage.isEmpty()) {
                    secondaryHeaderTv.text = secondHeaderMessage
                    secondaryHeaderTv.visibility = View.GONE
                }
                if (secondaryAction != null) {
                    secondaryActionTv.visibility = View.VISIBLE
                    bottomDivider.visibility = View.VISIBLE
                    secondaryActionTv.text = secondaryActionText
                    secondaryActionTv.setOnClickListener {
                        popUpAlert?.dismiss()
                        secondaryAction.invoke()
                    }
                }


                popUpOptionsVerticalContentCl.fadeIn(popUpAnimationDuration)

                popUpOptionsVerticalMainCl.setOnClickListener {
                    popUpAlert?.dismiss()
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