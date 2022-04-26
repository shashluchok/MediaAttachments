package ru.leadfrog.ui

import android.os.*
import android.view.*
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.android.synthetic.main.fragment_start.*
import ru.leadfrog.R
import ru.leadfrog.ui.base.BaseFragment

class StartFragment: BaseFragment() {
    override val layoutResId: Int
        get() = R.layout.fragment_start

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textView.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_mediaNotesFragment)
        }
    }



}