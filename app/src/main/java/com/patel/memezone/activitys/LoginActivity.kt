package com.patel.memezone.activitys

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.patel.memezone.BaseActivity
import com.patel.memezone.R
import com.patel.memezone.databinding.ActivityLoginBinding
import com.patel.memezone.dataclass.User

class LoginActivity : BaseActivity<ActivityLoginBinding>() {
    var username : String? = null
    private lateinit var customPopup: View
    private var isUserAdminFirst = "adminfirst@gmail.com"
    private var isUserAdminSecond = "adminsecond@gmail.com"

    override fun getLayoutId(): Int {
       return R.layout.activity_login
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        customPopup = findViewById(R.id.custome_popup)
        init()
    }
    @SuppressLint("SuspiciousIndentation")
    fun init() {
        binding!!.customeToolbar.backBtn.visibility = View.GONE
        binding!!.customeToolbar.logoutBtn.visibility = View.GONE
            binding!!.loginBtn.setOnClickListener {
                logInPerform()
            }
            binding!!.signBtn.setOnClickListener {
                changeAct(act,SignUpActivity::class.java)
            }
            binding!!.forgatPassword.setOnClickListener {
                binding!!.rootLayout.visibility = View.GONE
                binding!!.customeToolbar.title.text = "Change Password"
                customPopup.visibility = View.VISIBLE
            }
            binding!!.customePopup.backToLogin.setOnClickListener {
                customPopup.visibility = View.GONE
                binding!!.customeToolbar.title.text = "Login"
                binding!!.rootLayout.visibility = View.VISIBLE
            }
            binding!!.customePopup.changePassword.setOnClickListener {
                if (validdChangePasswords()){
                    Toast.makeText(act,"TOUC",Toast.LENGTH_SHORT).show()
                    if (binding!!.customePopup.changePassEmailedt.text.toString().isNotEmpty()) {
                        auth.sendPasswordResetEmail(binding!!.customePopup.changePassEmailedt.text.toString())
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Password reset email sent successfully, show a success message
                                    dialog(
                                        "Password Manager",
                                        "Check Your Mail And Change Your Password."
                                    )
//                                    Handler().postDelayed({
//                                    }, 2000)
                                } else {
                                    // Password reset email failed, display an error message
                                    dialog(
                                        "Password Manager",
                                        "Your Password Is Not Chnage This Time Try To Few Hours."
                                    )
//                                    Handler().postDelayed({
//                                        changeAct(act,LoginActivity::class.java)
//                                    }, 2000)
                                }
                            }
                    }
                }
            }
    }
    private fun validd(): Boolean {
        if (binding!!.emailEdt.text!!.isEmpty()) {
            binding!!.emailEdt.error = "Email is required"
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding!!.emailEdt.text.toString()).matches()) {
            binding!!.emailEdt.error = "Enter a valid email address"
            return false
        }
        if (binding!!.passwordEdt.text!!.isEmpty()) {
            binding!!.passwordEdt.error = "Password is required"
            return false
        }
        return true
    }
    private fun validdChangePasswords(): Boolean {
        if (binding!!.customePopup.changePassEmailedt.text!!.isEmpty()) {
            binding!!.customePopup.changePassEmailedt.error = "Email is required"
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding!!.customePopup.changePassEmailedt.text.toString()).matches()) {
            binding!!.customePopup.changePassEmailedt.error = "Enter a valid email address"
            return false
        }
        return true
    }
//    private fun isUserAdmin(email: String): Boolean {
//        val adminEmail = "sakdasariyaparth1639@gmail.com"
//        return email == adminEmail
//    }
    fun dialog(tit : String , reason : String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(reason)
        builder.setTitle(tit)
        builder.setCancelable(false)
        builder.setNegativeButton("OK") {
                dialog, which -> dialog.cancel()
                changeAct(act,LoginActivity::class.java)
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }
    private fun logInPerform() {
        binding!!.loginBtn.startAnimation(animation)
        Handler().postDelayed({
            if (validd()) {
                binding!!.rootLayout.visibility = View.GONE
                binding!!.customeLoader.visibility = View.VISIBLE
                auth.signInWithEmailAndPassword(binding!!.emailEdt.text.toString(), binding!!.passwordEdt.text.toString())
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            if(isUserAdminFirst == binding!!.emailEdt.text.toString() || isUserAdminSecond == binding!!.emailEdt.text.toString()) {
                                changeAct(act,AdminViewActivity::class.java)
                                Toast.makeText(act,"ADMIN LOG IN",Toast.LENGTH_SHORT).show()
                            } else {
                                val userId = auth.currentUser?.uid ?: ""
                                val userReference =
                                    FirebaseDatabase.getInstance().reference.child("users")
                                        .child(userId)
                                userReference.addListenerForSingleValueEvent(object :
                                    ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val user = snapshot.getValue(User::class.java)
                                        if (user != null) {
                                            username = user.username
                                            // You can use the retrieved username as needed
                                            Toast.makeText(
                                                act,
                                                "Welcome, $username",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    override fun onCancelled(error: DatabaseError) {
                                        Log.d("DATABASE ERROR", error.message)
                                    }
                                })
                                // Login successful, navigate to the next activity
                                changeAct(act, HomeActivity::class.java)
                                Log.d("SUCCESSFULL", "TRUE")
                            }
                        } else {
                            binding!!.customeLoader.visibility = View.GONE
                            dialog("Login Manager","Wrong Email And Password...")
                            Log.d("ERRORR","FALSE")
                        }
                    }
            }
        }, 520)
    }
}