package com.patel.memezone.activitys


import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import com.patel.memezone.BaseActivity
import com.patel.memezone.R
import com.patel.memezone.dataclass.User
import com.patel.memezone.databinding.ActivitySignUpBinding

class SignUpActivity : BaseActivity<ActivitySignUpBinding>() {
    override fun getLayoutId(): Int {
        return R.layout.activity_sign_up
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
    }
    fun init() {
        binding!!.customeToolbar.title.text = "Sign Up"
        binding!!.customeToolbar.backBtn.visibility = View.GONE
        binding!!.customeToolbar.logoutBtn.visibility = View.GONE
        binding!!.loginBtn.setOnClickListener {
                changeAct(act,LoginActivity::class.java)
        }
        binding!!.signBtn.setOnClickListener {
            binding!!.signBtn.startAnimation(animation)
            Handler().postDelayed({
                if (validd()) {
                    binding!!.rootLayout.visibility = View.GONE
                    binding!!.customeLoader.visibility = View.VISIBLE
                    auth.fetchSignInMethodsForEmail(binding!!.emailEdt.text.toString())
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val signInMethods = task.result?.signInMethods ?: emptyList()
                                if (signInMethods.isNotEmpty()) {
                                    // Email is already registered, show an error message
                                    binding!!.customeLoader.visibility = View.GONE
                                    dialog("Email Manager","Email Is Already Registered Please Try Different Email")
//                                    Toast.makeText(act, "Email is already registered", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Email is not registered, proceed with signup
                                    auth.createUserWithEmailAndPassword(
                                        binding!!.emailEdt.text.toString(),
                                        binding!!.passwordEdt.text.toString()
                                    ).addOnCompleteListener(this) { task ->
                                        if (task.isSuccessful) {
                                            val userId = auth.currentUser?.uid ?: ""
                                            val user = User(
                                                binding!!.userEdt.text.toString(),
                                                binding!!.emailEdt.text.toString()
                                            ) // Assuming you have a User data class
                                            val userReference =
                                                FirebaseDatabase.getInstance().reference.child("users").child(userId)
                                            userReference.setValue(user)
                                            // Signup successful, navigate to the next activity or show a success message
                                            Toast.makeText(act, "Sign Up Complete", Toast.LENGTH_SHORT).show()
                                            changeAct(act, LoginActivity::class.java)
                                        } else {
                                            // Signup failed, display an error message or handle the error
                                            Toast.makeText(act, "Sign Up Failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                // Handle error fetching sign-in methods
                                Toast.makeText(act, "Error checking email uniqueness", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(act,"Please Enter All Fields...",Toast.LENGTH_SHORT).show()
                }
            }, 520)
        }
    }
    fun validd(): Boolean {
        if (binding!!.userEdt.text!!.isEmpty()) {
            binding!!.userEdt.error = "Username is required"
            return false
        }
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
    fun dialog(tit:String , reason : String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(reason)
        builder.setTitle(tit)
        builder.setCancelable(false)
        builder.setNegativeButton("OK") {
                dialog, which -> dialog.cancel()
                binding!!.rootLayout.visibility = View.VISIBLE
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }
}