package com.patel.memezone.activitys

import android.os.Bundle
import com.patel.memezone.BaseActivity
import com.patel.memezone.R
import com.patel.memezone.databinding.ActivityHomeBinding

class HomeActivity : BaseActivity<ActivityHomeBinding>() {
    override fun getLayoutId(): Int {
        return R.layout.activity_home
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
    }
    fun init() {
        binding!!.customeToolbar.title.text = "Meme Bhandar"
        binding!!.customeToolbar.backBtn.visibility = android.view.View.GONE
        binding!!.customeToolbar.logoutBtn.setOnClickListener {
            auth.signOut()
            changeAct(act,LoginActivity::class.java)
        }
    }
}