package com.example.simpletextview.example

import android.content.res.Resources
import android.graphics.Color
import com.example.simpletextview.custom_tools.utils.dp

data class DataVM(
    val text: String,
    val textColor: Int,
    val textSize: Int,
    val padding: Int,
    val maxLines: Int
) {
    companion object {
        fun instance(resources: Resources): DataVM =
            DataVM(
                text = "Some test text some test text",
                textColor = Color.BLACK,
                textSize = resources.dp(20),
                padding = resources.dp(1),
                maxLines = 1
            )
    }
}