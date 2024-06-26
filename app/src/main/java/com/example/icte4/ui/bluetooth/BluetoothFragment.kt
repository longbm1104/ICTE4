package com.example.icte4.ui.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.registerReceiver
import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Intent
import android.os.Build
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.icte4.R
import com.example.icte4.databinding.FragmentBluetoothBinding
import com.example.icte4.databinding.FragmentCameraBinding
import com.example.icte4.databinding.FragmentHomeBinding
import com.example.icte4.ui.home.HomeViewModel
import java.util.UUID

class BluetoothFragment : Fragment() {

    private lateinit var binding: FragmentBluetoothBinding

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = requireContext().getSystemService(BluetoothManager::class.java) as BluetoothManager
        bluetoothManager.adapter
    }

    private val requestMultiplePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
        if (permissions.entries.any { !it.value }) {
            Toast.makeText(requireActivity(), "Required permission needed", Toast.LENGTH_LONG).show()
            requireActivity().finish()
        } else {
            requireActivity().recreate()
        }
    }

    private val connect = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val deviceName = gatt?.device?.name ?: "Unknown Device"
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    // Connection established, you can now discover services, etc.
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Connected to $deviceName", Toast.LENGTH_SHORT).show()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    // Connection disconnected
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Disconnected from $deviceName", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private lateinit var arrayAdapter: ArrayAdapter<String>
    private lateinit var buttonDiscover: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()

        if (!hasPermissions(requireContext(), REQUIRED_PERMISSIONS)) {
            requestMultiplePermissions.launch(
                REQUIRED_PERMISSIONS
            )
        }

        buttonDiscover = binding.btnDiscover
        // Setup the ListView and ArrayAdapter to display discovered devices
        val listView: ListView = binding.listDevices
        arrayAdapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1)
        listView.adapter = arrayAdapter

        listView.setOnItemClickListener { parent, listViewItemView, position, id ->
            val item = parent.getItemAtPosition(position) as String
            val deviceAddress = item.split(" | ")[1] // Extract device address from item
            val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            connectToDevice(device)
        }

        // IntentFilter to listen for Bluetooth devices found during discovery
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        activity.registerReceiver(receiver, filter)


        buttonDiscover.setOnClickListener {
            startDiscovery()
        }
    }

    private fun connectToDevice(it: BluetoothDevice) {
        Toast.makeText(requireContext(), "Clicked Devices" + it.name, Toast.LENGTH_SHORT).show()
        // Connect to the device using connectGatt
        val bluetoothGatt = if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED && !hasPermissions(requireContext(), REQUIRED_PERMISSIONS)
        ) {
            Toast.makeText(requireContext(), "Missing BLUETOOTH_CONNECT permission", Toast.LENGTH_SHORT).show()
            return
        } else {
            it.connectGatt(requireContext(), false, connect)
        }
    }

    private fun startDiscovery() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "BLUETOOTH_SCAN permission granted", Toast.LENGTH_SHORT).show()
            if (!hasPermissions(requireContext(), REQUIRED_PERMISSIONS)) {
                requestMultiplePermissions.launch(
                    REQUIRED_PERMISSIONS
                )
            } else {
                bluetoothAdapter?.startDiscovery()
            }
        } else {
            Toast.makeText(requireContext(), "BLUETOOTH_SCAN permission not granted", Toast.LENGTH_SHORT).show()
            requestMultiplePermissions.launch(
                REQUIRED_PERMISSIONS
            )
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Retrieve the BluetoothDevice from the intent
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        // Check for BLUETOOTH_CONNECT permission before accessing device name and address
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            )
                            == PackageManager.PERMISSION_GRANTED || hasPermissions(requireContext(), REQUIRED_PERMISSIONS)
                        ) {
                            arrayAdapter.add("${it.name ?: "Unknown"} | ${it.address}")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the broadcast receiver to avoid memory leaks
        requireActivity().unregisterReceiver(receiver)
    }

    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.isEmpty() || permissions.all {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBluetoothBinding.inflate(inflater, container, false)
        return binding.root
    }

    private companion object {
        val REQUIRED_PERMISSIONS =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
    }
}