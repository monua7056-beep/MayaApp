package com.maya.assistant

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

class UiInteractor(private val service: AccessibilityService) {

    private fun findNode(pred: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        val root = service.rootInActiveWindow ?: return null
        val q = ArrayDeque<AccessibilityNodeInfo>()
        q.add(root)
        while (q.isNotEmpty()) {
            val n = q.removeFirst()
            if (pred(n)) return n
            for (i in 0 until n.childCount) n.getChild(i)?.let { q.add(it) }
        }
        return null
    }

    private fun climbToClickable(n: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var cur: AccessibilityNodeInfo? = n
        while (cur != null) {
            if (cur.isClickable) return cur
            cur = cur.parent
        }
        return null
    }

    fun clickByText(text: String): Boolean {
        val node = findNode {
            (it.text?.toString()?.contains(text, true) == true ||
             it.contentDescription?.toString()?.contains(text, true) == true) &&
            (it.isClickable || it.isFocusable)
        } ?: return false
        return (climbToClickable(node) ?: node)
            .performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun clickByResourceId(id: String): Boolean {
        val node = findNode {
            it.viewIdResourceName?.contains(id, true) == true && it.isClickable
        } ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    fun typeIntoFocused(text: String): Boolean {
        val root = service.rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text
            )
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun scroll(direction: String): Boolean {
        val scrollable = findNode { it.isScrollable } ?: return false
        val action = if (direction == "down")
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        else
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return scrollable.performAction(action)
    }
}
