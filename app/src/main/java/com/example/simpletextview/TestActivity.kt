package com.example.simpletextview

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.simpletextview.simple_tv.SbisTextView

class TestActivity : AppCompatActivity() {

    private var view: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test_activity)
        view = SbisTextView(this)
    }
}