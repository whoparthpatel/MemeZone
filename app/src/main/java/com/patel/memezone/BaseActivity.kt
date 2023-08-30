package com.patel.memezone

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.google.firebase.auth.FirebaseAuth

abstract class BaseActivity<T : ViewDataBinding> : AppCompatActivity() {
    lateinit var act : Activity
    lateinit var animation: Animation
    var binding: T? = null
    @LayoutRes
    abstract fun getLayoutId(): Int
    lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, getLayoutId())
        act = this
        auth = FirebaseAuth.getInstance()
        animation = AnimationUtils.loadAnimation(this,R.anim.bounce)
        val window: Window = this.window
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.noti)
    }
    open fun onClick(view: View) {}
    fun changeAct(context: Context, targetActivity: Class<*>) {
        val intent = Intent(context, targetActivity)
        context.startActivity(intent)
        if (context is Activity) {
            context.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            context.finish()
        }
    }
    fun logout(){
        auth.signOut()
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("isLoggedIn", false)
        editor.apply()
        editor.clear()
    }
}