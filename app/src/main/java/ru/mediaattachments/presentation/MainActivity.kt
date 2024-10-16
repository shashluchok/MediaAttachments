package ru.mediaattachments.presentation

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import ru.mediaattachments.utils.animate.please
import ru.mediaattachments.R
import ru.mediaattachments.databinding.ActivityMainBinding
import ru.mediaattachments.presentation.base.BaseActivity
import ru.mediaattachments.presentation.base.IOnBackPressed

class MainActivity : BaseActivity() {

    private var navHostFragment: Fragment? = null
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)

        please(duration = 0L) {
            animate(binding.loaderCl) toBe {
                invisible()
                leftOf(binding.mainCl)
            }
        }.start()

    }

    fun showDeleteNotePopUp(
        singleNote: Boolean = true,
        action: () -> Unit,
        onDismiss: (() -> Unit)? = null
    ) {
        showPopupVerticalOptions(
            topHeaderMessage = if (singleNote) {
                getString(R.string.pop_up_top_header_message)
            } else {
                getString(R.string.pop_up_second_header_message_plural)
            },
            actionText = getString(R.string.pop_up_top_action_text),
            dismissActionText = getString(R.string.pop_up_middle_action_text),
            action = action,
            onDismiss = onDismiss
        )
    }

    fun showLoader() {
        please(duration = 0L) {
            animate(binding.loaderCl) toBe {
                originalPosition()
            }
        }.thenCouldYou(150L) {
            animate(binding.loaderCl) toBe {
                visible()
            }
        }.start()
    }

    fun hideLoader() {
        please(duration = 150L) {
            animate(binding.loaderCl) toBe {
                invisible()
            }
        }.thenCouldYou(150L) {
            animate(binding.loaderCl) toBe {
                leftOf(binding.mainCl)
            }
        }.start()
    }

    override fun onBackPressed() {
        if (popUpAlert == null) {
            val fragment =
                navHostFragment?.childFragmentManager?.fragments?.last()
            if (fragment is IOnBackPressed) {
                (fragment as? IOnBackPressed)?.onBackPressed()?.let {
                    if (it) {
                        super.onBackPressed()
                    } else {
                        return
                    }
                }
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val frag = navHostFragment?.childFragmentManager?.fragments?.lastOrNull()
        frag?.let {
            frag.onActivityResult(requestCode, resultCode, data)
        }
    }
}