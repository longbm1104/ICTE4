package com.example.icte4.ui.camera

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.icte4.R
import com.example.icte4.databinding.FragmentCameraBinding

class CameraFragment : Fragment() {

    private lateinit var imageView: ImageView
    private lateinit var deleteButton: Button
    private lateinit var saveButton: Button
    private lateinit var binding: FragmentCameraBinding
    private lateinit var contentResolver: ContentResolver
    private lateinit var activityResultLauncher: ActivityResultLauncher<String>

    companion object {
        fun newInstance() = CameraFragment()
        // Request codes for camera and permissions
        private const val IMAGE_CAPTURE_CODE = 1002
        private const val CAMERA_REQUEST_CODE = 1003
        private const val PHOTO_LIB_REQUEST_CODE = 1004
    }

    private val viewModel: CameraViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()

        contentResolver = activity.contentResolver

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageView.setImageURI(uri)
                imageView.visibility = ImageView.VISIBLE // Show the ImageView
                deleteButton.visibility = Button.VISIBLE // Show the Delete Button
            }
        }

        // TODO: Use the ViewModel
        imageView = binding.selectedImageView
        deleteButton = binding.buttonDeleteImage
        saveButton = binding.saveImage

//         Set an onClickListener for the "Add Image" button to handle camera permission and opening the camera
        binding.buttonAddImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
            } else {
                openCamera()
            }
        }

        binding.buttonUploadImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Request camera permission if not granted
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PHOTO_LIB_REQUEST_CODE)
            } else {
                selectImage()
            }
        }

        saveButton.setOnClickListener {
            galleryAddPic()
        }

        // Set an onClickListener for the "Delete Image" button to clear and hide the image and button
        deleteButton.setOnClickListener {
            imageView.setImageBitmap(null) // Clears the image from ImageView
            imageView.visibility = ImageView.GONE // Hides the ImageView
            deleteButton.visibility = Button.GONE // Hides the Delete Button
            saveButton.visibility = Button.GONE
        }
    }
//
//    override fun onView(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val packageManager = requireActivity().packageManager
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, IMAGE_CAPTURE_CODE)
            //The startActivityForResult() method is deprecated in favor of the Activity Result API, which provides a more modern and flexible approach for handling the result returned by an activity.
        }
    }

    private fun selectImage() {
        activityResultLauncher.launch("image/*")
    }

    // Callback for the result from requesting permissions
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission was granted, open the camera
            openCamera()
        } else if (requestCode == PHOTO_LIB_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission was denied, handle the case
            selectImage()
        }
    }

    // Callback for the result from capturing an image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK && requestCode == IMAGE_CAPTURE_CODE) {
            // Process and display the captured image
            val imageBitmap = data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(imageBitmap)
            imageView.visibility = ImageView.VISIBLE // Show the ImageView
            deleteButton.visibility = Button.VISIBLE // Show the Delete Button
            saveButton.visibility = Button.VISIBLE
        }
    }

    private fun galleryAddPic() {

        val bitmap = (imageView.drawable as BitmapDrawable).bitmap

        // Save the image to the device's external storage directory
        val savedImageURI = MediaStore.Images.Media.insertImage(
            contentResolver,
            bitmap,
            "Image_${System.currentTimeMillis()}",
            "Image from ICTE3 app"
        )

        if (savedImageURI != null) {
            // Image saved successfully, show a toast message
            Toast.makeText(requireActivity(), "Image Successfully Saved to Gallery", Toast.LENGTH_SHORT).show()
        } else {
            // Failed to save image, show an error toast message
            Toast.makeText(requireActivity(), "Failed to Save Image", Toast.LENGTH_SHORT).show()
        }
    }
}