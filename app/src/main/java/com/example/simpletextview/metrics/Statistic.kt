package com.example.simpletextview.metrics

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("StaticFieldLeak")
object Statistic {

    lateinit var toastCallback: (Long) -> Unit

    private var sbisContainerInflateTime: Long = 0
    private var sbisContainerInflateCount: Int = 0

    private var sbisContainerMeasureTime: Long = 0
    private var sbisContainerMeasureCount: Int = 0

    private var sbisContainerLayoutTime: Long = 0
    private var sbisContainerLayoutCount: Int = 0

    private var sbisInflateTime: Long = 0
    private var sbisInflateCount: Int = 0

    private var sbisMeasureTime: Long = 0
    private var sbisMeasureCount: Int = 0

    private var sbisLayoutTime: Long = 0
    private var sbisLayoutCount: Int = 0

    private var sbisObtainTextAppearanceTime: Long = 0
    private var sbisObtainTextAppearanceCount: Int = 0

    private var compatContainerInflateTime: Long = 0
    private var compatContainerInflateCount: Int = 0

    private var compatContainerMeasureTime: Long = 0
    private var compatContainerMeasureCount: Int = 0

    private var compatContainerLayoutTime: Long = 0
    private var compatContainerLayoutCount: Int = 0

    private var compatInflateTime: Long = 0
    private var compatInflateCount: Int = 0

    private var compatMeasureTime: Long = 0
    private var compatMeasureCount: Int = 0

    private var compatLayoutTime: Long = 0
    private var compatLayoutCount: Int = 0

    var cleared: Boolean = false

    fun addSbisContainerInflateTime(time: Long) {
        sbisContainerInflateTime += time
        sbisContainerInflateCount += 1
    }

    fun addSbisContainerMeasureTime(time: Long) {
        sbisContainerMeasureTime += time
        sbisContainerMeasureCount += 1
    }

    fun addSbisContainerLayoutTime(time: Long) {
        sbisContainerLayoutTime += time
        sbisContainerLayoutCount += 1
    }

    fun addSbisInflateTime(time: Long) {
        sbisInflateTime += time
        sbisInflateCount += 1
    }

    fun addSbisMeasureTime(time: Long) {
        sbisMeasureTime += time
        sbisMeasureCount += 1
        toastCallback(time)
    }

    fun addSbisLayoutTime(time: Long) {
        sbisLayoutTime += time
        sbisLayoutCount += 1
    }

    fun addSbisObtainTextAppearence(time: Long) {
        sbisObtainTextAppearanceTime += time
        sbisObtainTextAppearanceCount += 1
    }

    fun addCompatContainerInflateTime(time: Long) {
        compatContainerInflateTime += time
        compatContainerInflateCount += 1
    }

    fun addCompatContainerMeasureTime(time: Long) {
        compatContainerMeasureTime += time
        compatContainerMeasureCount += 1
    }

    fun addCompatContainerLayoutTime(time: Long) {
        compatContainerLayoutTime += time
        compatContainerLayoutCount += 1
    }

    fun addCompatInflateTime(time: Long) {
        compatInflateTime += time
        compatInflateCount += 1
    }

    fun addCompatMeasureTime(time: Long) {
        compatMeasureTime += time
        compatMeasureCount += 1
        toastCallback(time)
    }

    fun addCompatLayoutTime(time: Long) {
        compatLayoutTime+= time
        compatLayoutCount += 1
    }

    fun getStatistic(): String {
        val resultCompatContainerInflateTime = compatContainerInflateTime / compatContainerInflateCount.coerceAtLeast(1)
        val resultCompatContainerMeasureTime = compatContainerMeasureTime / compatContainerMeasureCount.coerceAtLeast(1)
        val resultCompatContainerLayoutTime = compatContainerLayoutTime / compatContainerLayoutCount.coerceAtLeast(1)
        val resultCompatContainerTime = resultCompatContainerInflateTime + resultCompatContainerMeasureTime + resultCompatContainerLayoutTime
        val resultCompatContainerCount = compatContainerInflateCount

        val resultCompatInflateTime = compatInflateTime / compatInflateCount.coerceAtLeast(1)
        val resultCompatMeasureTime = compatMeasureTime / compatMeasureCount.coerceAtLeast(1)
        val resultCompatLayoutTime = compatLayoutTime / compatLayoutCount.coerceAtLeast(1)
        val resultCompatTime = resultCompatInflateTime + resultCompatMeasureTime + resultCompatLayoutTime

        val resultSbisContainerInflateTime = sbisContainerInflateTime / sbisContainerInflateCount.coerceAtLeast(1)
        val resultSbisContainerMeasureTime = sbisContainerMeasureTime / sbisContainerMeasureCount.coerceAtLeast(1)
        val resultSbisContainerLayoutTime = sbisContainerLayoutTime / sbisContainerLayoutCount.coerceAtLeast(1)
        val resultSbisContainerTime = resultSbisContainerInflateTime + resultSbisContainerMeasureTime + resultSbisContainerLayoutTime
        val resultSbisContainerCount = sbisContainerInflateCount

        val resultSbisInflateTime = sbisInflateTime / sbisInflateCount.coerceAtLeast(1)
        val resultSbisMeasureTime = sbisMeasureTime / sbisMeasureCount.coerceAtLeast(1)
        val resultSbisLayoutTime = sbisLayoutTime / sbisLayoutCount.coerceAtLeast(1)
        val resultSbisObtainTime = sbisObtainTextAppearanceTime / sbisObtainTextAppearanceCount.coerceAtLeast(1)
        val resultSbisTime = resultSbisInflateTime + resultSbisMeasureTime + resultSbisLayoutTime

        return StringBuilder()
            .appendLine("AppCompat count $resultCompatContainerCount")
            .appendLine()
            .appendLine("Container time $resultCompatContainerTime")
            .appendLine("Inflate $resultCompatContainerInflateTime")
            .appendLine("Measure $resultCompatContainerMeasureTime")
            .appendLine("Layout $resultCompatContainerLayoutTime")
            .appendLine()
            .appendLine("AppCompatTextView time $resultCompatTime")
            .appendLine("Inflate $resultCompatInflateTime")
            .appendLine("Measure $resultCompatMeasureTime")
            .appendLine("Layout $resultCompatLayoutTime")
            .appendLine()
            .appendLine("Sbis count $resultSbisContainerCount")
            .appendLine()
            .appendLine("Container time $resultSbisContainerTime")
            .appendLine("Inflate $resultSbisContainerInflateTime")
            .appendLine("Measure $resultSbisContainerMeasureTime")
            .appendLine("Layout $resultSbisContainerLayoutTime")
            .appendLine()
            .appendLine("SbisTextView time $resultSbisTime")
            .appendLine("Inflate $resultSbisInflateTime")
            .appendLine("Measure $resultSbisMeasureTime")
            .appendLine("Layout $resultSbisLayoutTime")
            .appendLine("Obtain TextAppearance $resultSbisObtainTime")
            .toString()
    }

    fun clear() {
        cleared = true
        compatContainerInflateTime = 0
        compatContainerMeasureTime = 0
        compatContainerMeasureCount = 0
        compatContainerLayoutTime = 0
        compatContainerLayoutCount = 0
        compatContainerInflateCount = 0
        compatInflateTime = 0
        compatInflateCount = 0
        compatMeasureTime = 0
        compatMeasureCount = 0
        compatLayoutTime = 0
        compatLayoutCount = 0
        sbisContainerInflateTime = 0
        sbisContainerInflateCount = 0
        sbisContainerMeasureTime = 0
        sbisContainerMeasureCount = 0
        sbisContainerLayoutTime = 0
        sbisContainerLayoutCount = 0
        sbisContainerInflateCount = 0
        sbisInflateTime = 0
        sbisInflateCount = 0
        sbisMeasureTime = 0
        sbisMeasureCount = 0
        sbisLayoutTime = 0
        sbisLayoutCount = 0
    }
}