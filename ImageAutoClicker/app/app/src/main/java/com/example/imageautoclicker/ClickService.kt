package com.example.imageautoclicker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

// service aksesibilitas, nanti dipakai untuk menekan layar (dispatchGesture)
class ClickService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }
}
