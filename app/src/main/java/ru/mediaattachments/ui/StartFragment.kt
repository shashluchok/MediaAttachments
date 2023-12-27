package ru.mediaattachments.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import ru.mediaattachments.R
import ru.mediaattachments.databinding.FragmentStartBinding
import ru.mediaattachments.ui.base.BaseFragment

class StartFragment : BaseFragment<FragmentStartBinding>() {

    override fun inflateViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentStartBinding {
        return FragmentStartBinding.inflate(inflater,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textView.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_mediaNotesFragment)
        }
    }

}