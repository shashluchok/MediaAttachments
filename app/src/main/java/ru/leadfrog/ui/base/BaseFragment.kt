package ru.leadfrog.ui.base

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import ru.leadfrog.R

import java.util.*


abstract class BaseFragment : Fragment() {
    abstract val layoutResId: Int
    private var mProgressDialog: AlertDialog? = null

    private val APP_PREFERENCES = "settings"
    private val APP_PREFERENCES_LANG = "language"

    fun vibrate() {
        val vibe =
            requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibe?.vibrate(
                VibrationEffect.createOneShot(
                    100,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else vibe?.vibrate(100);
    }

    fun enableUserInteraction() {
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    open fun disableUserInteractionForAWhile(disableTimeInMillis: Long) {
        (requireActivity()).window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
        Handler(Looper.getMainLooper()).postDelayed({
            (requireActivity()).window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }, disableTimeInMillis)
    }

    fun changeLang(lang: String) {
        val myLocale = Locale(lang)
        Locale.setDefault(myLocale)
        val config = Configuration()
        config.setLocale(myLocale)
        requireActivity().baseContext.resources.updateConfiguration(config, requireActivity().baseContext.resources.displayMetrics)
        saveLocale(lang)

    }

   fun saveLocale(lang: String) {
        val sp = requireActivity().getSharedPreferences(APP_PREFERENCES, AppCompatActivity.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putString(APP_PREFERENCES_LANG, lang)
        editor.apply()
    }

    fun getCurrentLocale(): String {
        val sp = (requireActivity()).getSharedPreferences(
                APP_PREFERENCES,
                AppCompatActivity.MODE_PRIVATE
        )
        return sp.getString(APP_PREFERENCES_LANG, "") ?: ""

    }

    fun getDefaultNavOptions(popUpToDestinationId:Int,inclusive:Boolean): NavOptions {
        return NavOptions.Builder()
                .setPopUpTo(popUpToDestinationId, inclusive)
                .setEnterAnim(R.anim.my_fade_enter)
                .setExitAnim(R.anim.my_fade_exit)
                .setPopEnterAnim(R.anim.my_fade_enter)
                .setPopExitAnim(R.anim.my_fade_exit)
                .build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutResId, container, false)
    }

    fun hideKeyboard() {
        val imm: InputMethodManager =
            requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }

    fun morePowerfulHideKeyboard(view: View) {
        val imm: InputMethodManager =
            requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    protected fun checkCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getProgressDialog(context: Context): AlertDialog {
        val ll = LinearLayout(context)
        ll.orientation = LinearLayout.HORIZONTAL
        ll.gravity = Gravity.CENTER
        val llParam = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        )
        llParam.gravity = Gravity.CENTER
        ll.layoutParams = llParam

        val progressBar = ProgressBar(context)
        progressBar.isIndeterminate = true
        progressBar.layoutParams = llParam

        ll.addView(progressBar)

        val builder = AlertDialog.Builder(context)
        builder.setCancelable(false)
        builder.setView(ll)

        val dialog = builder.create()
        val window = dialog.window
        if (window != null) {
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(dialog.window?.attributes)
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
            dialog.window?.attributes = layoutParams
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        return dialog
    }

    fun getTransparentPercentage(num: Int): String {
        return when (num) {
            100 -> "FF"
            99 -> "FC"
            98 -> "FA"
            97 -> "F7"
            96 -> "F5"
            95 -> "F2"
            94 -> "F0"
            93 -> "ED"
            92 -> "EB"
            91 -> "E8"
            90 -> "E6"
            89 -> "E3"
            88 -> "E0"
            87 -> "DE"
            86 -> "DB"
            85 -> "D9"
            84 -> "D6"
            83 -> "D4"
            82 -> "D1"
            81 -> "CF"
            80 -> "CC"
            79 -> "C9"
            78 -> "C7"
            77 -> "C4"
            76 -> "C2"
            75 -> "BF"
            74 -> "BD"
            73 -> "BA"
            72 -> "B8"
            71 -> "B5"
            70 -> "B3"
            69 -> "B0"
            68 -> "AD"
            67 -> "AB"
            66 -> "A8"
            65 -> "A6"
            64 -> "A3"
            63 -> "A1"
            62 -> "9E"
            61 -> "9C"
            60 -> "99"
            59 -> "96"
            58 -> "94"
            57 -> "91"
            56 -> "8F"
            55 -> "8C"
            54 -> "8A"
            53 -> "87"
            52 -> "85"
            51 -> "82"
            50 -> "80"
            49 -> "7D"
            48 -> "7A"
            47 -> "78"
            46 -> "75"
            45 -> "73"
            44 -> "70"
            43 -> "6E"
            42 -> "6B"
            41 -> "69"
            40 -> "66"
            39 -> "63"
            38 -> "61"
            37 -> "5E"
            36 -> "5C"
            35 -> "59"
            34 -> "57"
            33 -> "54"
            32 -> "52"
            31 -> "4F"
            30 -> "4D"
            29 -> "4A"
            28 -> "47"
            27 -> "45"
            26 -> "42"
            25 -> "40"
            24 -> "3D"
            23 -> "3B"
            22 -> "38"
            21 -> "36"
            20 -> "33"
            19 -> "30"
            18 -> "2E"
            17 -> "2B"
            16 -> "29"
            15 -> "26"
            14 -> "24"
            13 -> "21"
            12 -> "1F"
            11 -> "1C"
            10 -> "1A"
            9 -> "17"
            8 -> "14"
            7 -> "12"
            6 -> "0F"
            5 -> "0D"
            4 -> "0A"
            3 -> "08"
            2 -> "05"
            1 -> "03"
            0 -> "00"
            else -> "00"
        }
    }

}