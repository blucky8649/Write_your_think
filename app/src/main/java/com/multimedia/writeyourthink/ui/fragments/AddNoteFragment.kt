package com.multimedia.writeyourthink.ui.fragments

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.transition.MaterialContainerTransform
import com.google.firebase.auth.FirebaseAuth
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import com.multimedia.writeyourthink.R
import com.multimedia.writeyourthink.Util.formatDate
import com.multimedia.writeyourthink.Util.themeColor
import com.multimedia.writeyourthink.databinding.FragmentAddNoteBinding
import com.multimedia.writeyourthink.services.GpsTracker
import com.multimedia.writeyourthink.viewmodels.DiaryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class AddNoteFragment: Fragment(R.layout.fragment_add_note) {

    @Inject
    lateinit var gpsTracker: GpsTracker

    private val viewModel: DiaryViewModel by activityViewModels()

    private var _binding: FragmentAddNoteBinding? = null
    val binding get() = _binding!!

    val args: AddNoteFragmentArgs by navArgs()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddNoteBinding.inflate(layoutInflater, container, false)
        initToolbar()
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            duration = 300L
            scrimColor = Color.TRANSPARENT
            containerColor = requireContext().themeColor(R.attr.colorSurface)
            startContainerColor = requireContext().themeColor(R.attr.colorSurface)
            endContainerColor = requireContext().themeColor(R.attr.colorSurface)
        }
        binding.etLocation.setText(args.diary.where)
        binding.etContents.setText(args.diary.contents)
        binding.etLocation.clearFocus()
        binding.etLocation.requestFocus()
        viewLifecycleOwner.lifecycleScope.launch {
            delay(500L)
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etLocation,0)
        }

        return binding.root
    }

    private fun initToolbar() {
        binding.toolbar.inflateMenu(R.menu.top_menu)
        binding.toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.toolbar.isTitleCentered = true
        binding.toolbar.menu.removeItem(R.id.action_edit)
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_add -> {
                    Toast.makeText(activity, "add item successfully.", Toast.LENGTH_LONG).show()
                    val diary = args.diary.copy(
                        userUID = FirebaseAuth.getInstance().uid.toString(),
                        where = binding.etLocation.text.toString(),
                        contents = binding.etContents.text.toString(),
                        location = getCurrentAddress(
                            latitude = gpsTracker.latitude,
                            longitude = gpsTracker.longitude
                        ),
                        date = args.diary.date.ifEmpty { Date().formatDate() },
                        diaryDate = Date().formatDate()
                    )
                    viewModel.saveDiary(diary)
                    val action = AddNoteFragmentDirections.actionAddNoteFragmentToDiaryDetailFragment(diary)
                    findNavController().navigate(action)

                    true
                }
                else -> false
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun getCurrentAddress(latitude: Double, longitude: Double): String { //지오코더... GPS를 주소로 변환
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        val addresses: List<Address>?
        try {
            addresses = geocoder.getFromLocation(
                latitude,
                longitude,
                100
            )
        } catch (ioException: IOException) {
//네트워크 문제
            Toast.makeText(activity, R.string.geocoderUnavailable, Toast.LENGTH_LONG).show()
            tedPermission()
            return getString(R.string.geocoderUnavailable)
        } catch (illegalArgumentException: IllegalArgumentException) {
            Toast.makeText(activity, R.string.wrongGps, Toast.LENGTH_LONG).show()
            tedPermission()
            return ""
        }
        if (addresses == null || addresses.size == 0) {
            tedPermission()
            return ""
        }
        val address = addresses[0]
        return address.getAddressLine(0).toString() + "\n"
    }

    private fun tedPermission() {
        val permissionListener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                /** 권한요청성공  */
            }

            override fun onPermissionDenied(deniedPermissions: List<String>) {}
        }
        TedPermission.create()
            .setPermissionListener(permissionListener)
            .setRationaleMessage(resources.getString(R.string.permission_3))
            .setDeniedMessage(resources.getString(R.string.permission_1))
            .setPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
            .check()
    }

}