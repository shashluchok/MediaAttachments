package ru.mediaattachments.presentation.base

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


abstract class BaseFragment<T : ViewBinding> : Fragment() {

    protected lateinit var binding: T

    abstract fun inflateViewBinding(inflater: LayoutInflater, container: ViewGroup?): T

    protected fun doWithDelayOnUiThread(delayInMillis: Long, func: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(delayInMillis)
            withContext(Dispatchers.Main){
                func()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = inflateViewBinding(inflater, container)
        return binding.root
    }

}