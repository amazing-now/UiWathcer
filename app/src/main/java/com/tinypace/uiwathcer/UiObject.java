package com.tinypace.uiwathcer;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;

public class UiObject {
    private Point phoneSize;
    private AccessibilityNodeInfo mCachedNode;

    private String text;
    private String className;
    private String contentDescription;
    private Rect visibleBounds;
    private String resourceName;

    public UiObject(AccessibilityNodeInfo cachedNode) {
        mCachedNode = cachedNode;
    }

    UiObject(AccessibilityNodeInfo cachedNode, Point point) {
        mCachedNode = cachedNode;
        phoneSize = point;
    }

    public String getClassName() {
        if (mCachedNode != null) {
            CharSequence chars = mCachedNode.getClassName();
            return chars != null ? chars.toString() : null;
        } else {
            return className;
        }
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getContentDescription() {
        if (mCachedNode != null) {
            CharSequence chars = mCachedNode.getContentDescription();
            return chars != null ? chars.toString() : null;
        } else {
            return contentDescription;
        }
    }

    public void setContentDescription(String contentDescription) {
        this.contentDescription = contentDescription;
    }

    public String getResourceName() {
        if (mCachedNode != null) {
            CharSequence chars = mCachedNode.getViewIdResourceName();
            return chars != null ? chars.toString() : null;
        } else {
            return resourceName;
        }
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getText() {
        if (mCachedNode != null) {
            CharSequence chars = mCachedNode.getText();
            return chars != null ? chars.toString() : null;
        } else {
            return text;
        }
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isCheckable() {
        return mCachedNode.isCheckable();
    }

    public void setCheckable(boolean checkable) {
        mCachedNode.setCheckable(checkable);
    }

    public boolean isChecked() {
        return mCachedNode.isChecked();
    }

    public void setChecked(boolean checked) {
        mCachedNode.setChecked(checked);
    }

    public boolean isClickable() {
        return mCachedNode.isClickable();
    }

    public void setClickable(boolean clickable) {
        mCachedNode.setClickable(clickable);
    }

    public boolean isEnabled() {
        return mCachedNode.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        mCachedNode.setEnabled(enabled);
    }

    public boolean isFocusable() {
        return mCachedNode.isFocusable();
    }

    public void setFocusable(boolean focusable) {
        mCachedNode.setFocusable(focusable);
    }

    public boolean isFocused() {
        return mCachedNode.isFocused();
    }

    public void setFocused(boolean focused) {
        mCachedNode.setFocused(focused);
    }

    public boolean isLongClickable() {
        return mCachedNode.isLongClickable();
    }

    public void setLongClickable(boolean longClickable) {
        mCachedNode.setLongClickable(longClickable);
    }

    public boolean isScrollable() {
        return mCachedNode.isScrollable();
    }

    public void setScrollable(boolean scrollable) {
        mCachedNode.setScrollable(scrollable);
    }

    public boolean isSelected() {
        return mCachedNode.isSelected();
    }

    public void setSelected(boolean selected) {
        mCachedNode.setSelected(selected);
    }

    public Rect getVisibleBounds() {
        if (mCachedNode != null) {
            return getVisibleBounds(mCachedNode);
        } else {
            return visibleBounds;
        }
    }

    public void setVisibleBounds(Rect rect) {
        this.visibleBounds = rect;
    }

    private Rect getVisibleBounds(AccessibilityNodeInfo node) {
        Rect ret = new Rect();
        node.getBoundsInScreen(ret);
        Rect screen = new Rect(0, 0, phoneSize.x, phoneSize.y);
        ret.intersect(screen);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Rect window = new Rect();
            if (node.getWindow() != null) {
                node.getWindow().getBoundsInScreen(window);
                ret.intersect(window);
            }
        }
        AccessibilityNodeInfo ancestor = null;
        for (ancestor = node.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
            if (ancestor.isScrollable()) {
                Rect ancestorRect = getVisibleBounds(ancestor);
                ret.intersect(ancestorRect);
                break;
            }
        }
        return ret;
    }

    public Point getVisibleCenter() {
        Rect bounds = getVisibleBounds();
        return new Point(bounds.centerX(), bounds.centerY());
    }

}
