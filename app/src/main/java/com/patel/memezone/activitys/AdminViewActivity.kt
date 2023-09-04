package com.patel.memezone.activitys

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.patel.memezone.BaseActivity
import com.patel.memezone.R
import com.patel.memezone.databinding.ActivityAdminViewBinding
import kotlin.math.max
import kotlin.math.min

class AdminViewActivity : BaseActivity<ActivityAdminViewBinding>() {
    private lateinit var mScaleGestureDetector: ScaleGestureDetector
    private var mScaleFactor = 1.0f
    private lateinit var imagesReference: DatabaseReference
    private lateinit var databaseReference : DatabaseReference
    private lateinit var imageUrls: ArrayList<String>
    private var currentImageIndex = 0
    private var imageUrlAtIndex : String? = null
    override fun getLayoutId(): Int {
        return R.layout.activity_admin_view
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        databaseReference = FirebaseDatabase.getInstance().reference
        imagesReference = databaseReference.child("users").child("images")
        imageUrls = ArrayList()
        mScaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
        init()
    }
    fun init() {
        retrieveImagesAndDisplay()

        Log.d("CURRENT INDEX", currentImageIndex.toString())
        binding!!.customeToolbar.backBtn.visibility = android.view.View.GONE
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val Admin = sharedPref.getInt("Admin", 0)
        val User = sharedPref.getInt("User", 0)
        if (Admin == 1) {

            binding!!.customeToolbar.title.text = "Admin Pannel"
            binding!!.deleteImg.visibility = View.VISIBLE
            binding!!.upImage.visibility = View.VISIBLE
        } else {

            binding!!.customeToolbar.title.text = "Meme Bhandar"
            binding!!.deleteImg.visibility = View.GONE
            binding!!.upImage.visibility = View.GONE
        }
        binding!!.customeToolbar.logoutBtn.setOnClickListener {
            logout()
            changeAct(act,LoginActivity::class.java)
        }
        binding!!.nextImg.setOnClickListener {
            showNextImage()
        }
        binding!!.prvImg.setOnClickListener {
            showPreviousImage()
        }
        binding!!.shareImg.setOnClickListener {
            shareImage()
        }
        binding!!.deleteImg.setOnClickListener{
            showDeleteConfirmationDialog(act,imageUrlAtIndex.toString())
        }
        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    val selectedImageUri = data?.data
                    selectedImageUri?.let {
                        uploadImageToFirebaseStorage(it)
                    }
                }
            }
        binding!!.upImage.setOnClickListener {
            val galleryIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            resultLauncher.launch(galleryIntent)
        }
        binding!!.downloadImg.setOnClickListener{
            downloadImage(imageUrlAtIndex.toString())
        }
    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        binding!!.prvImg.visibility =  View.GONE
        binding!!.upImage.visibility = View.GONE
        binding!!.nextImg.visibility = View.GONE
        mScaleGestureDetector.onTouchEvent(motionEvent)
        if (mScaleFactor <= 1.60f) {
            binding!!.prvImg.visibility = View.VISIBLE
            binding!!.upImage.visibility = View.VISIBLE
            binding!!.nextImg.visibility = View.VISIBLE
        }
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            mScaleFactor *= scaleGestureDetector.scaleFactor
            mScaleFactor = max(0.1f, min(mScaleFactor, 10.0f))
            Log.d("ZOOMEFFECT",mScaleFactor.toString())
            binding!!.upImageSave.scaleX = mScaleFactor
            binding!!.upImageSave.scaleY = mScaleFactor
            return true
        }
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri) {
        binding!!.upImageSave.visibility = View.GONE
        binding!!.customeLoader.visibility = View.VISIBLE
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("images/${imageUri.lastPathSegment}")
        val uploadTask = imageRef.putFile(imageUri)
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            imageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val imageUrl = task.result.toString()
                saveImageMetadataToDatabase(imageUrl)
            } else {
                // Handle upload failure
            }
        }
    }
    private fun showDeleteConfirmationDialog(context: Context, imageUrlToDelete: String) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle("Delete Manager")
        alertDialogBuilder.setMessage("Are you sure you want to delete this image ?")
        alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
            // User clicked OK, proceed with deletion
            deleteImageByStorrage(imageUrlToDelete)
            displayImage(this.currentImageIndex)
            dialog.dismiss()
//            currentImageIndex -= 1
        }
        alertDialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            // User clicked Cancel, do nothing
            dialog.dismiss()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun deleteImageByStorrage(imageUrlToDelete: String) {
        val storageRef = FirebaseStorage.getInstance().reference

        // Create a StorageReference using the image URL's lastPathSegment as the image name
        val imageName = Uri.parse(imageUrlToDelete).lastPathSegment


        Log.d("FILE NAME",imageName.toString())
        val imageRef = storageRef.child(imageName.toString())

        // Delete the image from Firebase Storage
        imageRef.delete()
            .addOnSuccessListener {
                // Image deleted successfully from Storage, now delete metadata from Database
//                Toast.makeText(act, "Successfully Storage", Toast.LENGTH_SHORT).show()
                deleteImageByUrl(imageUrlToDelete)
            }
            .addOnFailureListener {
//                Toast.makeText(act, "UNNNSuccessfully Storage ", Toast.LENGTH_SHORT).show()
                Log.d("ERROOOOOOOO",it.message.toString())
                // Handle delete failure
            }
    }
    private fun deleteImageByUrl(x: String) {
        Log.d("DELETED URL'S",x.toString())
        databaseReference.child("users").child("images").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                    for (imageSnapshot in snapshot.children) {
                        val imageUrl = imageSnapshot.child("imageUrl").getValue(String::class.java)
                        if (imageUrl == x) { // Compare with the image URL to delete
                            imageSnapshot.ref.removeValue()
                                .addOnSuccessListener {
//                                    Toast.makeText(act, "Successfully deleted image from Firebase Realtime Database", Toast.LENGTH_SHORT).show()
                                    recreate()
                                    // Image metadata deleted from Database
                                }
                                .addOnFailureListener {
                                    Toast.makeText(act, "Failed to delete image from Firebase Realtime Database", Toast.LENGTH_SHORT).show()
                                    // Handle Database deletion failure
                                }
                            return // Exit the loop once the image is found and deleted
                        }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }
    private fun saveImageMetadataToDatabase(imageUrl: String) {
        val imageKey = databaseReference.child("images").push().key
        if (imageKey != null) {
            val imageMetadata = mapOf(
                "imageUrl" to imageUrl,
                "timestamp" to ServerValue.TIMESTAMP
            )
            val imageUpdates = hashMapOf<String, Any>(
                "/users/images/$imageKey" to imageMetadata
            )
            databaseReference.updateChildren(imageUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        binding!!.customeLoader.visibility = View.GONE
                        binding!!.upImageSave.visibility = View.VISIBLE
                        dialog("upload Manager","Image Upload Successfully.")
                    } else {
                        dialog("upload Manager","Image Upload Failure.")
                    }
                }
        }
    }
    private fun retrieveImagesAndDisplay() {
        Log.d("CURRENT INDEX", currentImageIndex.toString())
        imagesReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    imageUrls.clear()
                    for (imageSnapshot in snapshot.children) {
                        val imageID = imageSnapshot.key // This gets the image's unique key
                        val imageUrl = imageSnapshot.child("imageUrl").getValue(String::class.java)
                        imageID?.let { Log.d("IMAGE IDDD", it) }
                        if (!imageUrl.isNullOrEmpty()) {
                            imageUrls.add(imageUrl)
                        }
                    }
                    if (imageUrls.isEmpty()) {
                        // Set a default image URL when no image URLs are available
                        val defaultImageUrl = "https://www.linkpicture.com/q/noresultfound-removebg.png"
                        imageUrls.add(defaultImageUrl)
                    }
                    imageUrlAtIndex = imageUrls[currentImageIndex]
                    Log.d("IMAGE URLS",imageUrlAtIndex.toString())
                    displayImage(currentImageIndex)
                } else {
                    // Set a default image URL when no data is available
                    val defaultImageUrl = "https://www.linkpicture.com/q/noresultfound-removebg.png"
                    imageUrls.add(defaultImageUrl)
                    imageUrlAtIndex = imageUrls[currentImageIndex]
                    Log.d("IMAGE URLS",imageUrlAtIndex.toString())
                    displayImage(currentImageIndex)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })

    }
    private fun displayImage(index: Int) {
        if (index in 0 until imageUrls.size) {
            binding!!.customeLoader.visibility = View.VISIBLE
            Glide.with(this)
                .load(imageUrls[index])
                .into(binding!!.upImageSave)
            var url = imageUrls[index]
            url = imageUrlAtIndex.also { imageUrlAtIndex = url }.toString()
            Log.d("IMAGE URLS",imageUrlAtIndex.toString())
//            imageUrlAtIndex?.let { Log.d("CURRENT INDEX", it) }
            currentImageIndex = index
            binding!!.upImageSave.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (binding!!.upImageSave.drawable != null) {
                        binding!!.customeLoader.visibility = View.GONE
                        binding!!.upImageSave.visibility = View.VISIBLE
                        binding!!.upImageSave.viewTreeObserver.removeOnPreDrawListener(this)
                    }
                    return true
                }
            })
        }
    }
    private fun showNextImage() {
        if(currentImageIndex == imageUrls.size-1)
        {
            currentImageIndex = -1
        }
        if (currentImageIndex <= imageUrls.size - 1) {
            displayImage(currentImageIndex + 1)
            Log.d("CURRENT INDEX", currentImageIndex.toString())
//            binding!!.upImage.text = currentImageIndex.toString()
        }

    }
    private fun showPreviousImage() {
        if (currentImageIndex >= 0) {
            displayImage(currentImageIndex - 1)
            Log.d("CURRENT INDEX", currentImageIndex.toString())
//            binding!!.upImage.text = currentImageIndex.toString()
        }
    }
    private fun shareImage() {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Welcome To Meme Bhandar \nAdmin Share A Meme \nClick Here And Show Meme\n$imageUrlAtIndex")
        sendIntent.type = "text/plain"
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }
    private fun dialog(tit : String, reason : String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(reason)
        builder.setTitle(tit)
        builder.setCancelable(false)
        builder.setNegativeButton("OK") {
                dialog, _ -> dialog.cancel()
                recreate()
//            changeAct(act,LoginActivity::class.java)
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }


    private fun downloadImage(imageUrl: String) {
        val request = DownloadManager.Request(Uri.parse(imageUrl))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "downloaded_image.jpg")
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
    }
}