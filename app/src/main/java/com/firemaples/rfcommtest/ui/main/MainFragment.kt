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

class MainFragment : Fragment(R.layout.main_fragment) {

    private val viewModel: MainViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.bt_setAsServer).setOnClickListener {
            findNavController().navigate(MainFragmentDirections.actionAsServer())
        }

        view.findViewById<View>(R.id.bt_setAsClientWithBTDiscovery).setOnClickListener {
            val permissions = mutableListOf(Permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions.add(Permission.ACCESS_BACKGROUND_LOCATION)
            }
            runWithPermissions(*permissions.toTypedArray()) {
                findNavController().navigate(MainFragmentDirections.actionAsClientWithBTDiscovery())
            }
        }

        view.findViewById<View>(R.id.bt_setAsClientWithCompanionPairing).setOnClickListener {
            findNavController().navigate(MainFragmentDirections.actionAsClientWithCompanionPairing())
        }
    }
}
