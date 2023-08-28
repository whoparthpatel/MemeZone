package com.patel.memezone.activitys

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
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
import java.io.File
import java.io.FileOutputStream
import android.graphics.drawable.Drawable as Drawable1

class AdminViewActivity : BaseActivity<ActivityAdminViewBinding>() {
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        databaseReference = FirebaseDatabase.getInstance().reference
        imagesReference = databaseReference.child("users").child(userId).child("images")

        imageUrls = ArrayList()
        init()
    }
    fun init() {
        retrieveImagesAndDisplay()
        binding!!.customeToolbar.title.text = "Admin Pannel"
        binding!!.customeToolbar.backBtn.visibility = android.view.View.GONE
        binding!!.customeToolbar.logoutBtn.setOnClickListener {
            logout()
            changeAct(act,LoginActivity::class.java)
        }
        retrieveImagesAndDisplay()
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

    private fun saveImageMetadataToDatabase(imageUrl: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val imageKey = databaseReference.child("images").push().key
        if (imageKey != null) {
            val imageMetadata = mapOf(
                "imageUrl" to imageUrl,
                "timestamp" to ServerValue.TIMESTAMP
            )
            val imageUpdates = hashMapOf<String, Any>(
                "/users/$userId/images/$imageKey" to imageMetadata
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
        imagesReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
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
                    displayImage(currentImageIndex)
                } else {
                    // Set a default image URL when no data is available
                    val defaultImageUrl = "https://www.linkpicture.com/q/noresultfound-removebg.png"
                    imageUrls.add(defaultImageUrl)
                    imageUrlAtIndex = imageUrls[currentImageIndex]
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
            Glide.with(this)
                .load(imageUrls[index])
                .into(binding!!.upImageSave)
            var url = imageUrls[index]
            url = imageUrlAtIndex.also { imageUrlAtIndex = url }.toString()
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
        if (currentImageIndex < imageUrls.size - 1) {
            displayImage(currentImageIndex + 1)
        }
    }

    private fun showPreviousImage() {
        if (currentImageIndex > 0) {
            displayImage(currentImageIndex - 1)
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

    fun dialog(tit : String , reason : String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(reason)
        builder.setTitle(tit)
        builder.setCancelable(false)
        builder.setNegativeButton("OK") {
                dialog, _ -> dialog.cancel()
//            changeAct(act,LoginActivity::class.java)
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }
    private fun showDeleteConfirmationDialog(context: Context, imageUrlToDelete: String) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle("Delete Manager")
        alertDialogBuilder.setMessage("Are you sure you want to delete this image ?")
        alertDialogBuilder.setPositiveButton("OK") { dialog, _ ->
            // User clicked OK, proceed with deletion
            deleteImageByUrl(imageUrlToDelete)
            displayImage(this.currentImageIndex)
            dialog.dismiss()
        }
        alertDialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            // User clicked Cancel, do nothing
            dialog.dismiss()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }
//    private fun deleteImageByUrl(imageUrl: String) {
//        databaseReference.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                for (userSnapshot in snapshot.children) {
//                    val imagesSnapshot = userSnapshot.child("images")
//                    for (imageSnapshot in imagesSnapshot.children) {
//                        val imageUrl = imageSnapshot.child("imageUrl").getValue(String::class.java)
//                        if (imageUrl == imageUrlAtIndex) {
//                            imageSnapshot.ref.removeValue()
//                                .addOnCompleteListener { task ->
//                                    if (task.isSuccessful) {
//                                        Toast.makeText(act,"Delete Successfully",Toast.LENGTH_SHORT).show()
//                                    } else {
//                                        Toast.makeText(act,"Delete Failed Please Try Again...",Toast.LENGTH_SHORT).show()
//                                    }
//                                }
//                        }
//                    }
//                }
//            }
//            override fun onCancelled(error: DatabaseError) {
//                // Handle database error
//            }
//        })
//    }

    private fun deleteImageByUrl(imageUrl: String) {
        databaseReference.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val imagesSnapshot = userSnapshot.child("images")
                    for (imageSnapshot in imagesSnapshot.children) {
                        val imageUrl = imageSnapshot.child("imageUrl").getValue(String::class.java)
                        if (imageUrl == imageUrl) {
                            val imageKey = imageSnapshot.key
                            if (imageKey != null) {
                                // Delete from Firebase Storage
                                val storageReference = FirebaseStorage.getInstance().getReference("images/$imageKey.jpg")
                                storageReference.delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(act,"SUCCESFULLY IMAGE DELETE TO FIREBASE STORAGE",Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(act,"FAIELD IMAGE DELETE TO FIREBASE STORAGE",Toast.LENGTH_SHORT).show()
                                        // Handle Storage deletion failure
                                    }
                                // Delete image metadata from Realtime Database
                                imageSnapshot.ref.removeValue()
                                    .addOnSuccessListener {
                                        Toast.makeText(act,"SUCCESFULLY IMAGE DELETE TO FIREBASE REAL TIME DATA BASE",Toast.LENGTH_SHORT).show()
                                        // Image metadata deleted from Database
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(act,"FAIELD IMAGE DELETE TO FIREBASE REAL TIME DATA BASE",Toast.LENGTH_SHORT).show()
                                        // Handle Database deletion failure
                                    }
                                break
                            }
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }
}