package com.example.simpletextview

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class ChoseCaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chose_case_activity)
        findViewById<View>(R.id.show_linear_appcompat).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ExampleFragment.newInstance(ExampleType.LINEAR_APP_COMPAT))
                .addToBackStack(null)
                .commit()
        }
        findViewById<View>(R.id.show_linear_simple).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ExampleFragment.newInstance(ExampleType.LINEAR_SIMPLE))
                .addToBackStack(null)
                .commit()
        }
        findViewById<View>(R.id.show_relative_appcompat).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ExampleFragment.newInstance(ExampleType.RELATIVE_APP_COMPAT))
                .addToBackStack(null)
                .commit()
        }
        findViewById<View>(R.id.show_relative_simple).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ExampleFragment.newInstance(ExampleType.RELATIVE_SIMPLE))
                .addToBackStack(null)
                .commit()
        }
        findViewById<View>(R.id.show_constraint_appcompat).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ExampleFragment.newInstance(ExampleType.CONSTRAINT_APP_COMPAT))
                .addToBackStack(null)
                .commit()
        }
        findViewById<View>(R.id.show_constraint_simple).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ExampleFragment.newInstance(ExampleType.CONSTRAINT_SIMPLE))
                .addToBackStack(null)
                .commit()
        }
    }
}