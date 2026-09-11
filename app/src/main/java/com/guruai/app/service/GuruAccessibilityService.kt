package com.guruai.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.guruai.app.data.Prefs
import java.util.concurrent.atomic.AtomicReference

class GuruAccessibilityService : AccessibilityService() {

    private lateinit var prefs: Prefs

    override fun onServiceConnected() {
        prefs = Prefs(this)
        instance.set(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!::prefs.isInitialized || !prefs.screenMonitorEnabled) return
        event?.packageName?.toString()?.let { lastPackage = it }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance.compareAndSet(this, null)
        super.onDestroy()
    }

    fun readVisibleText(maxChars: Int = 4000): String {
        if (::prefs.isInitialized && !prefs.screenMonitorEnabled) {
            return "(Screen Monitoring is off. Turn it on in Settings to let Guru read the screen.)"
        }
        val root = rootInActiveWindow ?: return "(No active window – open the app/screen you want read.)"
        val sb = StringBuilder()
        collectText(root, sb, maxChars)
        root.recycle()
        return sb.toString().ifBlank { "(No text nodes found on screen.)" }
    }

    private fun collectText(node: AccessibilityNodeInfo?, sb: StringBuilder, max: Int) {
        if (node == null || sb.length >= max) return
        val t = node.text?.toString()?.trim()
        if (!t.isNullOrEmpty()) {
            if (sb.isNotEmpty()) sb.append('\n')
            sb.append(t)
        }
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), sb, max)
        }
    }

    fun typeIntoFocusedField(text: String): Boolean {
        if (::prefs.isInitialized && !prefs.screenMonitorEnabled) return false
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findEditable(root)
        if (focused == null) {
            root.recycle()
            return false
        }
        val args = android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val ok = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        focused.recycle()
        root.recycle()
        return ok
    }

    private fun findEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return AccessibilityNodeInfo.obtain(node)
        for (i in 0 until node.childCount) {
            val found = findEditable(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    fun tap(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    companion object {
        private val instance = AtomicReference<GuruAccessibilityService?>(null)
        @Volatile var lastPackage: String = ""

        fun get(): GuruAccessibilityService? = instance.get()

        fun isEnabled(): Boolean = instance.get() != null
    }
}
