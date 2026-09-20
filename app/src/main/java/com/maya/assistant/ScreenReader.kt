package com.maya.assistant

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

class ScreenReader(private val service: AccessibilityService) {

    data class Node(
        val text: String?,
        val contentDesc: String?,
        val resourceId: String?,
        val className: String?,
        val clickable: Boolean,
        val focusable: Boolean,
        val editable: Boolean,
        val bounds: Rect
    )

    fun read(): List<Node> {
        val root = service.rootInActiveWindow ?: return emptyList()
        val out = mutableListOf<Node>()
        traverse(root, out)
        return out
    }

    private fun traverse(n: AccessibilityNodeInfo?, out: MutableList<Node>) {
        n ?: return
        val r = Rect().also { n.getBoundsInScreen(it) }
        out += Node(
            text = n.text?.toString(),
            contentDesc = n.contentDescription?.toString(),
            resourceId = n.viewIdResourceName,
            className = n.className?.toString(),
            clickable = n.isClickable,
            focusable = n.isFocusable,
            editable = n.isEditable,
            bounds = r
        )
        for (i in 0 until n.childCount) traverse(n.getChild(i), out)
    }
}
