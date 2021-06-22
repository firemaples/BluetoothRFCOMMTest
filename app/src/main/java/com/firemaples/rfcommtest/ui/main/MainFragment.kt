package com.firemaples.rfcommtest.ui.main

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.afollestad.assent.Permission
import com.afollestad.assent.runWithPermissions
import com.firemaples.rfcommtest.R
import com.firemaples.rfcommtest.utility.Constant

class MainFragment : Fragment(R.layout.main_fragment) {

    private val viewModel: MainViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.bt_setAsServer).setOnClickListener {
            findNavController().navigate(MainFragmentDirections.actionAsServer())
        }

        view.findViewById<View>(R.id.bt_setAsClient).setOnClickListener {
            val permissions = mutableListOf<Permission>()
            if (!Constant.useCompanionManager) {
                permissions.add(Permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions.add(Permission.ACCESS_BACKGROUND_LOCATION)
            }
            runWithPermissions(*permissions.toTypedArray()) {
                findNavController().navigate(MainFragmentDirections.actionAsClient())
            }
        }
    }
}
