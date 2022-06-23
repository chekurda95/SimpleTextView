package com.example.simpletextview

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.simpletextview.ExampleType.*

class ChoseCaseActivity : AppCompatActivity() {

    private val screens = arrayOf(
        R.id.show_linear_appcompat to LINEAR_APP_COMPAT,
        R.id.show_linear_simple to LINEAR_SIMPLE,
        R.id.show_relative_appcompat to RELATIVE_APP_COMPAT,
        R.id.show_relative_simple to RELATIVE_SIMPLE,
        R.id.show_constraint_appcompat to CONSTRAINT_APP_COMPAT,
        R.id.show_constraint_simple to CONSTRAINT_SIMPLE,
        R.id.show_binding_appcompat to BINDING_COMPAT,
        R.id.show_binding_simple to BINDING_SIMPLE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chose_case_activity)
        screens.forEach { (id, type) -> setupListener(id, type) }
    }

    private fun setupListener(viewId: Int, openType: ExampleType) {
        findViewById<View>(viewId).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ExampleFragment.newInstance(openType))
                .addToBackStack(null)
                .commit()
        }
    }
}