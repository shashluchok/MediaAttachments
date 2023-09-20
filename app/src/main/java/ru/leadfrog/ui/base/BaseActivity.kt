package ru.leadfrog.ui.base

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.pop_up_alert.view.*
import kotlinx.android.synthetic.main.pop_up_alert_center_align.view.*
import kotlinx.android.synthetic.main.pop_up_options_horizontal.view.*
import kotlinx.android.synthetic.main.pop_up_options_horizontal_with_input.view.*
import kotlinx.android.synthetic.main.pop_up_options_vertical.view.*
import kotlinx.android.synthetic.main.pop_up_update_alert.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.leadfrog.R


open class BaseActivity : AppCompatActivity() {

    open var popUpAlert:PopupWindow? = null

    fun checkRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    open fun checkCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
                baseContext,
                android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    open fun checkReadContactsPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
                baseContext,
                android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    open fun disableUserInteraction() {
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }
    open fun enableUserInteraction() {
        window.clearFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }



    open fun closeKeyBoard(){
        val imm: InputMethodManager =
                getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }


    open fun showUpdateAlert() {
        if(popUpAlert == null) {
            disableUserInteractionForAWhile(disableTimeInMillis = 500)
            closeKeyBoard()

            val popUpView: View = layoutInflater.inflate(
                R.layout.pop_up_update_alert,
                    null)
            popUpAlert = PopupWindow(popUpView, ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.MATCH_PARENT, false)

            popUpAlert?.showAtLocation(popUpView, Gravity.NO_GRAVITY, 0, 0)

            popUpView.apply {


                pop_up_update_alert_content_cl.apply {
                    alpha = 0f
                    scaleX = 0f
                    scaleY = 0f
                    visibility = View.VISIBLE
                    animate().alpha(1f)
                            .scaleY(1f)
                            .scaleX(1f)
                            .duration = 200
                }
                pop_up_update_alert_action_skip_tv.setOnClickListener {
                    val appPackageName = context.packageName
                    try {
                        context.startActivity(
                                Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=$appPackageName")
                                )
                        )
                    } catch (e: ActivityNotFoundException) {
                        context.startActivity(
                                Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                                )
                        )
                    }
                    popUpAlert?.dismiss()
                }

                popUpAlert?.setOnDismissListener {
                    popUpAlert = null
                }

            }
        }
    }


    open fun showPopupAlertCenterAlign(topHeaderMessage: String, secondHeaderMessage: String, skipText:String? = null, onDismiss: (()->Unit)? = null) {
        if(popUpAlert == null) {
            closeKeyBoard()
            disableUserInteractionForAWhile(disableTimeInMillis = 500)

            val popUpView: View = layoutInflater.inflate(
                R.layout.pop_up_alert_center_align,
                    null
            )
            popUpAlert = PopupWindow(
                    popUpView, ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.MATCH_PARENT, true
            )

            popUpAlert?.showAtLocation(popUpView, Gravity.NO_GRAVITY, 0, 0)

            popUpView.apply {

                pop_up_options_main_header_tv.text = topHeaderMessage

                pop_up_options_secondary_header_tv.text = secondHeaderMessage

                skipText?.let{
                    pop_up_options_skip_tv.text = it
                }

                pop_up_options_content_cl.apply {
                    alpha = 0f
                    scaleX = 0f
                    scaleY = 0f
                    visibility = View.VISIBLE
                    animate().alpha(1f)
                            .scaleY(1f)
                            .scaleX(1f)
                            .duration = 200
                }
                pop_up_options_main_cl.setOnClickListener {
                    popUpAlert?.dismiss()
                    popUpAlert = null
                }
                pop_up_options_skip_tv.setOnClickListener {
                    popUpAlert?.dismiss()
                    popUpAlert = null
                }
            }
            popUpAlert?.setOnDismissListener {
                onDismiss?.invoke()
                popUpAlert = null
            }
        }
    }

    open fun showPopupAlert(message: String, onDismiss: (()->Unit)? = null, skipText:String? = null) {
        if(popUpAlert == null) {
            closeKeyBoard()
            disableUserInteractionForAWhile(disableTimeInMillis = 500)

            val popUpView: View = layoutInflater.inflate(
                R.layout.pop_up_alert,
                null
            )
            popUpAlert = PopupWindow(
                popUpView, ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT, true
            )

            popUpAlert?.showAtLocation(popUpView, Gravity.NO_GRAVITY, 0, 0)

            popUpView.apply {

                pop_up_alert_main_header_tv.text = message

                skipText?.let{
                    pop_up_alert_action_skip_tv.text = it
                }

                pop_up_alert_content_cl.apply {
                    alpha = 0f
                    scaleX = 0f
                    scaleY = 0f
                    visibility = View.VISIBLE
                    animate().alpha(1f)
                        .scaleY(1f)
                        .scaleX(1f)
                        .duration = 200
                }
                pop_up_alert_main_cl.setOnClickListener {
                    popUpAlert?.dismiss()
                    popUpAlert = null
                }
                pop_up_alert_action_skip_tv.setOnClickListener {
                    popUpAlert?.dismiss()
                    popUpAlert = null
                }
            }
            popUpAlert?.setOnDismissListener {
                onDismiss?.invoke()
                popUpAlert = null
            }
        }
    }

    open fun showPopupAlertAutoClose(message: String, onDismiss: (()->Unit)? = null) {
        if(popUpAlert == null) {
            closeKeyBoard()
            disableUserInteractionForAWhile(disableTimeInMillis = 500)

            val popUpView: View = layoutInflater.inflate(
                R.layout.pop_up_alert_standman_added,
                null
            )
            popUpAlert = PopupWindow(
                popUpView, ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT, true
            )
            GlobalScope.launch(Dispatchers.Main) {
                popUpAlert?.showAtLocation(popUpView, Gravity.NO_GRAVITY, 0, 0)
                delay(2000)
                popUpAlert?.dismiss()
                popUpAlert = null
            }

            popUpView.apply {

                pop_up_alert_main_header_tv.text = message

                pop_up_alert_content_cl.apply {
                    alpha = 0f
                    scaleX = 0f
                    scaleY = 0f
                    visibility = View.VISIBLE
                    animate().alpha(1f)
                        .scaleY(1f)
                        .scaleX(1f)
                        .duration = 200
                }
                pop_up_alert_main_cl.setOnClickListener {
                    popUpAlert?.dismiss()
                    popUpAlert = null
                }
            }
            popUpAlert?.setOnDismissListener {
                onDismiss?.invoke()
                popUpAlert = null
            }
        }
    }

    open fun showPopupVerticalOptions(
        topHeaderMessage: String,
        secondHeaderMessage: String = "",
        topActionText: String,
        topActionCallback: () -> Unit,
        middleActionText: String,
        middleActionCallback: (() -> Unit)? = null,
        bottomActionText: String = "",
        bottomActionCallback: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
        isTopButtonBlue: Boolean = false,
    ) {
        if (popUpAlert == null) {
            disableUserInteractionForAWhile(disableTimeInMillis = 500)
            closeKeyBoard()
            val popUpView: View = layoutInflater.inflate(
                R.layout.pop_up_options_vertical,
                null
            )

            popUpAlert = PopupWindow(popUpView, ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT, true)

            popUpAlert?.showAtLocation(popUpView, Gravity.NO_GRAVITY, 0, 0)

            popUpView.apply {

                if(isTopButtonBlue){
                    pop_up_options_vertical_top_action_tv.setTextColor(resources.getColor(R.color.colorPrimary))
                }

                pop_up_options_vertical_main_header_tv.text = topHeaderMessage
                pop_up_options_vertical_secondary_header_tv.text = secondHeaderMessage
                if (secondHeaderMessage.isNullOrEmpty()) {
                    pop_up_options_vertical_secondary_header_tv.visibility = View.GONE
                }

                if (!bottomActionText.isNullOrEmpty()) {
                    pop_up_options_vertical_bottom_action_tv.visibility = View.VISIBLE
                    pop_up_options_vertical_bottom_divider.visibility = View.VISIBLE
                    pop_up_options_vertical_bottom_action_tv.text = bottomActionText

                    pop_up_options_vertical_bottom_action_tv.setOnClickListener {
                        popUpAlert?.dismiss()
                        popUpAlert = null
                        bottomActionCallback?.invoke()
                    }
                }

                pop_up_options_vertical_top_action_tv.text = topActionText
                pop_up_options_vertical_middle_action_tv.text = middleActionText
                pop_up_options_vertical_content_cl.apply {
                    alpha = 0f
                    scaleX = 0f
                    scaleY = 0f
                    visibility = View.VISIBLE
                    animate().alpha(1f)
                        .scaleY(1f)
                        .scaleX(1f)
                        .duration = 200

                }

                pop_up_options_vertical_main_cl.setOnClickListener {
                    popUpAlert?.dismiss()
                    popUpAlert = null
                }
                pop_up_options_vertical_middle_action_tv.setOnClickListener {
                    popUpAlert?.dismiss()
                    popUpAlert = null
                    middleActionCallback?.invoke()
                }
                pop_up_options_vertical_top_action_tv.setOnClickListener {
                    popUpAlert?.dismiss()
                    popUpAlert = null
                    topActionCallback.invoke()
                }
            }

            popUpAlert?.setOnDismissListener {
                onDismiss?.invoke()
                popUpAlert = null
            }
        }
    }



    open fun dpToPx(dp: Float): Float {
        val displayMetrics: DisplayMetrics = Resources.getSystem().displayMetrics
        return dp * displayMetrics.density
    }


    open fun showPopupHorizontalOptions(
            topHeaderMessage: String,
            secondHeaderMessage: String,
            actionSkipText: String,
            actionActionText: String? = null,
            actionSkipCallback: (() -> Unit)? = null,
            actionActionCallBack: (() -> Unit)? = null,
            actionActionTextEnabled:Boolean = true
    ) {
        disableUserInteractionForAWhile(disableTimeInMillis = 500)
        closeKeyBoard()
        val popUpView: View = layoutInflater.inflate(
            R.layout.pop_up_options_horizontal,
            null
        )
        val mpopup = PopupWindow(
            popUpView, ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT, true
        )

        mpopup.showAtLocation(popUpView, Gravity.NO_GRAVITY, 0, 0)

        popUpView.apply {

            if(!actionActionTextEnabled){
                pop_up_options_horizontal_action_tv.visibility = View.GONE
            }

            pop_up_options_horizontal_main_header_tv.text = topHeaderMessage
            pop_up_options_horizontal_secondary_header_tv.text = secondHeaderMessage
            pop_up_options_horizontal_skip_tv.text = actionSkipText
            pop_up_options_horizontal_action_tv.text = actionActionText?:""

            pop_up_options_horizontal_content_cl.apply {
                alpha = 0f
                scaleX = 0f
                scaleY = 0f
                visibility = View.VISIBLE
                animate().alpha(1f)
                    .scaleY(1f)
                    .scaleX(1f)
                    .duration = 200
            }
            pop_up_options_horizontal_action_tv.setOnClickListener {
                actionActionCallBack?.invoke()
                mpopup.dismiss()
            }
            pop_up_options_horizontal_skip_tv.setOnClickListener{
                mpopup.dismiss()
                actionSkipCallback?.invoke()
            }
        }
    }

    open fun showPopupWithInput(header: String, inputHint:String,actualInput:String, actionAgreeText:String, actionAgreeCallback:(String)->Unit) {
        disableUserInteractionForAWhile(disableTimeInMillis = 500)
        closeKeyBoard()
        val popUpView: View = layoutInflater.inflate(
            R.layout.pop_up_options_horizontal_with_input,
                null)
        val mpopup = PopupWindow(popUpView, ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT, true)

        mpopup.showAtLocation(popUpView, Gravity.NO_GRAVITY, 0, 0)

        popUpView.apply {

            pop_up_options_horizontal_with_input_main_header_tv.text = header
            pop_up_options_horizontal_with_input_input_et.hint = inputHint
            pop_up_options_horizontal_with_input_secondary_action_agree_tv.text = actionAgreeText
            pop_up_options_horizontal_with_input_input_et.setText(actualInput)

            pop_up_options_horizontal_with_input_secondary_action_agree_tv.setOnClickListener {
                val imm: InputMethodManager =
                        getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(windowToken, 0)
                actionAgreeCallback.invoke(pop_up_options_horizontal_with_input_input_et.text.toString())
                mpopup.dismiss()
            }

            pop_up_options_horizontal_with_input_content_cl.apply {
                alpha = 0f
                scaleX = 0f
                scaleY = 0f
                visibility = View.VISIBLE
                animate().alpha(1f)
                        .scaleY(1f)
                        .scaleX(1f)
                        .duration = 200
            }
            pop_up_options_horizontal_with_input_main_cl.setOnClickListener {
                val imm: InputMethodManager =
                        getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(windowToken, 0)
                mpopup.dismiss()
            }
            pop_up_options_horizontal_with_input_secondary_action_skip_tv.setOnClickListener {
                val imm: InputMethodManager =
                        getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(windowToken, 0)
               mpopup.dismiss()
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