package com.multimedia.writeyourthink.ui.fragments

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationSet
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.multimedia.writeyourthink.services.GpsTracker
import com.multimedia.writeyourthink.R
import com.multimedia.writeyourthink.Util.Constants.Companion.DOWN
import com.multimedia.writeyourthink.Util.Constants.Companion.UP
import com.multimedia.writeyourthink.databinding.Frag2Binding
import com.multimedia.writeyourthink.models.Diary
import com.multimedia.writeyourthink.ui.DiaryActivity
import com.multimedia.writeyourthink.viewmodels.DiaryViewModel
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class BottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: Frag2Binding? = null
    private val binding get() = _binding!!

    private var gpsTracker // 위치정보
            : GpsTracker? = null
    val set = AnimationSet(true)

    /** DB에 저장된 내용을 보여주기위한 리스트뷰  */
    private val myFormat = "yyyy-MM-dd" // 출력형식   2018/11/28
    private val sdf = SimpleDateFormat(myFormat, Locale.KOREA)
    private val sdf2 = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
    private var time: String? = null
    private var address: String? = null
    private var storage: FirebaseStorage? = null
    private var filePath: Uri? = null
    private var storageRef: StorageReference? = null
    private var stringUri: String? = null
    private var userName = "Master"
    private var auth // 파이어 베이스 인증 객체
            : FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private var date: String? = null
    var drawable: Drawable? = null

    lateinit var viewModel: DiaryViewModel

    var myCalendar = Calendar.getInstance()
    var myDatePicker: DatePickerDialog.OnDateSetListener =
        DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            myCalendar[Calendar.YEAR] = year
            myCalendar[Calendar.MONTH] = month
            myCalendar[Calendar.DAY_OF_MONTH] = dayOfMonth
            date = "$year/$month/$dayOfMonth"
            viewModel.setDate(sdf.format(myCalendar.time))
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = Frag2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = (activity as DiaryActivity).viewModel
        var diary = Diary.EMPTY

        val mArgs = arguments
        diary = mArgs?.getParcelable("diary") ?: diary

        viewModel.selectedDateTime.observe(viewLifecycleOwner) {
            binding.tvDateAndTime.text = it
        }
        auth = FirebaseAuth.getInstance() // 파이어베이스 인증 객체 초기화.
        user = auth!!.currentUser
        userName = user!!.uid
        binding.invisibleLayout.setVisibility(View.GONE)
        gpsTracker = GpsTracker(requireActivity())
        val latitude = gpsTracker!!.latitude
        /** 위도  */
        val longitude = gpsTracker!!.longitude
        /** 경도  */
        address = getCurrentAddress(latitude, longitude)
        Thread(r).start()
        /** 현재시간  */
        binding.button.setOnClickListener(View.OnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(intent, REQUEST_CODE)
        })
        binding.tvDateAndTime.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                myDatePicker,
                myCalendar[Calendar.YEAR],
                myCalendar[Calendar.MONTH],
                myCalendar[Calendar.DAY_OF_MONTH]
            ).show()
        } //달력 꺼내기
        binding.DateDown.setOnClickListener {
            dateUpDown(DOWN)
        }
        binding.DateUp.setOnClickListener {
            dateUpDown(UP)
        }

        binding.btnUpload.setOnClickListener{
            if ((binding.editTitle.text.toString() == "") || (binding.editContents.getText()
                    .toString() == "")
            ) {
                Toast.makeText(requireActivity().applicationContext, "내용을 입력하십시오.", Toast.LENGTH_SHORT)
                    .show()
            } else {
                address = getCurrentAddress(latitude, longitude)
                if ((binding.btnUpload.text.toString() == "수정")) { //수정일 때..ㅎ
                    val input = Diary(
                        userName,
                        if (diary.profile != null) if (stringUri != null) stringUri else diary.profile else if (stringUri != null) stringUri else " ",
                        binding.editTitle.text.toString(),
                        binding.editContents.text.toString(),
                        diary.date,
                        diary.location
                    )
                    viewModel.saveDiary(input)
                } else {
                    val input = Diary(
                        userName,
                        if (stringUri != null) stringUri else " ",
                        binding.editTitle.text.toString(),
                        binding.editContents.text.toString(),
                        binding.tvDateAndTime!!.text.toString() + "(" + time + ")",
                        if ((address == "주소 미발견") || (address == null)) " " else address!!.substring(
                            address!!.indexOf(" ") + 1, address!!.lastIndexOf(" ")
                        )
                    )
                    viewModel.saveDiary(input)
                }
                Toast.makeText(requireActivity().applicationContext, "끄적끄적!", Toast.LENGTH_LONG).show()
                dismiss()
            }
        }
        binding.tvDateAndTime.text = date
        binding.editTitle.setText("")
        binding.editContents.setText("")
        binding.btnUpload.text = "업로드"
        if (diary.location != "") {
            reviceModeON(diary)
            Glide.with(requireActivity().applicationContext).load(diary.profile).into(binding.imageView)
        }
    }
    private fun reviceModeON(diary: Diary) {
        binding.tvDateAndTime.text = diary.date
        binding.editTitle.setText(diary.where)
        binding.editContents.setText(diary.contents)
        binding.btnUpload.text = "수정"
        binding.tvDateAndTime.isFocusable = false
        binding.tvDateAndTime.isFocusableInTouchMode = false
        binding.tvDateAndTime.isEnabled = false
        binding.DateUp.visibility = View.GONE
        binding.DateDown.visibility = View.GONE
        if (diary.profile!!.length > 3) {
            binding.invisibleLayout.visibility = View.VISIBLE
        } else {
            binding.invisibleLayout.visibility = View.GONE
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    filePath = data!!.data
                    uploadFile()
                } catch (e: Exception) {
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
            }
        }
    }



    var r: Runnable = Runnable {
        while (true) {
            try {
                Thread.sleep(1000)
            } catch (e: Exception) {
            }
            if (activity != null) {
                requireActivity().runOnUiThread { time = sdf2.format(Date()) }
            }
        }
    }

    private fun tedPermission() {
        val permissionListener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                /** 권한요청성공  */
            }

            override fun onPermissionDenied(deniedPermissions: List<String>) {}
        }
        TedPermission.with(activity)
            .setPermissionListener(permissionListener)
            .setRationaleMessage(resources.getString(R.string.permission_3))
            .setDeniedMessage(resources.getString(R.string.permission_1))
            .setPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
            .check()
    }

    fun getCurrentAddress(latitude: Double, longitude: Double): String { //지오코더... GPS를 주소로 변환
        val geocoder = Geocoder(activity, Locale.getDefault())
        val addresses: List<Address>?
        try {
            addresses = geocoder.getFromLocation(
                latitude,
                longitude,
                100
            )
        } catch (ioException: IOException) {
//네트워크 문제
            Toast.makeText(activity, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show()
            tedPermission()
            return "지오코더 서비스 사용불가"
        } catch (illegalArgumentException: IllegalArgumentException) {
            Toast.makeText(activity, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show()
            tedPermission()
            return "잘못된 GPS 좌표"
        }
        if (addresses == null || addresses.size == 0) {
            tedPermission()
            return "주소 미발견"
        }
        val address = addresses[0]
        return address.getAddressLine(0).toString() + "\n"
    }

    interface BottomSheetListener {
        fun onButtonClicked(text: String?)
    }




    /** upload the file  */
    private fun uploadFile() {
        /** 업로드할 파일이 있으면 수행  */
        if (filePath != null) {
            /** 업로드 진행 Dialog 보이기  */
            val progressDialog = ProgressDialog(activity)
            progressDialog.setTitle("업로드중...")
            progressDialog.setCancelable(false)
            progressDialog.show()
            storage = FirebaseStorage.getInstance()
            /** Unique한 파일명을 만들자.  */
            val formatter = SimpleDateFormat("yyyyMMHH_mmss")
            val now = Date()
            val filename = formatter.format(now) + ".png"
            /** storage 주소와 폴더 파일명을 지정해 준다.  */
            storageRef = storage!!.getReferenceFromUrl("gs://diary-d5627.appspot.com/").child(
                "images/$userName/$filename"
            )
            /** 올라가거라...  */
            storageRef!!.putFile(filePath!!)
                /** 성공시  */
                .addOnSuccessListener {
                    clickLoad()
                    progressDialog.dismiss() //업로드 진행 Dialog 상자 닫기
                    binding.editUpload.setText(filePath.toString())
                    binding.invisibleLayout.visibility = View.VISIBLE
                    Toast.makeText(activity, "업로드 완료!", Toast.LENGTH_SHORT).show()
                }
                /** 실패시  */
                .addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(activity, "업로드 실패!", Toast.LENGTH_SHORT).show()
                }
                /** 진행중  */
                .addOnProgressListener { taskSnapshot ->
                    val progress//이걸 넣어 줘야 아랫줄에 에러가 사라진다. 넌 누구냐?
                            =
                        ((100 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount).toDouble()
                    //dialog에 진행률을 퍼센트로 출력해 준다
                    progressDialog.setMessage("Uploaded " + (progress.toInt()) + "% ...")
                }
        } else {
            Toast.makeText(activity, "파일을 먼저 선택하세요.", Toast.LENGTH_SHORT).show()
        }
    }

    fun clickLoad() {
        /** Firebase Storage에 저장되어 있는 이미지 파일 읽어오기  */
        /** 1. Firebase Storeage관리 객체 얻어오기  */
        val firebaseStorage = FirebaseStorage.getInstance()
        /** 2. 최상위노드 참조 객체 얻어오기  */
        val rootRef = firebaseStorage.reference
        /**
         * 읽어오길 원하는 파일의 참조객체 얻어오기
         * 예제에서는 자식노드 이름은 monkey.png
         */
        /** 하위 폴더가 있다면 폴더명까지 포함  */
        if (storageRef != null) {
            /** 참조객체로 부터 이미지의 다운로드 URL을 얻어오기  */
            storageRef!!.downloadUrl.addOnSuccessListener(OnSuccessListener { uri ->
                /** 다운로드 URL이 파라미터로 전달되어 옴.  */
                Glide.with(requireActivity().applicationContext).load(uri.toString()).into((binding.imageView))
                stringUri = uri.toString()
            })
        }
    }
    private fun dateUpDown(op: Int) {
        var day = binding.tvDateAndTime.text.toString().replace("-", "")
        var dayInt = day.toInt()
        dayInt += op
        day = dayInt.toString()
        val sdfmt = SimpleDateFormat("yyyyMMdd")
        try {
            val date = sdfmt.parse(day)
            day = SimpleDateFormat("yyyy-MM-dd").format(date)
            myCalendar.time = date
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        viewModel.setDate(day)
        binding.tvDateAndTime.text = day
    }
    companion object {
        private const val REQUEST_CODE = 0
    }

    override fun getTheme(): Int = R.style.AppBottomSheetDialogTheme

    /**
     * Fragment에서 View Binding을 사용할 경우 Fragment는 View보다 오래 지속되어,
     * Fregment의 Lifecycle로 인해 메모리 누수가 발생할 수 있다.
     * 따라서 반드시 binding 변수를 onDestroyView() 이후에 null로 만들어 주어야한다.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
