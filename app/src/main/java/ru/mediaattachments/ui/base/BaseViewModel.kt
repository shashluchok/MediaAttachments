package ru.mediaattachments.ui.base

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.koin.java.KoinJavaComponent.inject

open class BaseViewModel<STATE> : ViewModel() {

    val appContext: Context by inject(Context::class.java)

    protected val _state: MutableLiveData<STATE> = MutableLiveData()
    val state: LiveData<STATE> = _state

    open fun isNetworkAvailableAndConnected(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return (cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isAvailable
                && cm.activeNetworkInfo!!.isConnected)
    }

    fun clearState() {
        _state.value = null
    }

}