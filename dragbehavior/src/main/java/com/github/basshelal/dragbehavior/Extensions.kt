@file:Suppress("NOTHING_TO_INLINE")

package com.github.basshelal.dragbehavior

import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Interpolator
import android.view.animation.Transformation
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.core.graphics.toRectF
import androidx.core.view.children
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.github.basshelal.BuildConfig
import com.google.android.material.snackbar.Snackbar
import org.jetbrains.anko.Orientation
import org.jetbrains.anko.childrenRecursiveSequence
import org.jetbrains.anko.configuration
import org.jetbrains.anko.displayMetrics
import org.jetbrains.anko.find
import org.jetbrains.anko.windowManager
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min

@PublishedApi
internal inline val Number.I: Int
    get() = this.toInt()

@PublishedApi
internal inline val Number.D: Double
    get() = this.toDouble()

@PublishedApi
internal inline val Number.F: Float
    get() = this.toFloat()

@PublishedApi
internal inline val Number.L: Long
    get() = this.toLong()

internal inline val now: Long get() = System.currentTimeMillis()

internal inline fun <K, V> HashMap<K, V>.putIfAbsentSafe(key: K, value: V) {
    if (!this.containsKey(key)) this[key] = value
}

internal inline fun View.shortSnackBar(string: String) =
        Snackbar.make(this, string, Snackbar.LENGTH_SHORT).show()

inline val View.locationOnScreen: Point
    get() {
        val point = IntArray(2).also {
            this.getLocationOnScreen(it)
        }
        return Point(point[0], point[1])
    }

inline val View.isTransparent: Boolean
    get() = alpha == 0F

inline val View.isClear: Boolean
    get() = alpha == 1F

inline fun View.removeOnClickListener() = this.setOnClickListener(null)

inline fun View.changeParent(newParent: ViewGroup) {
    this.parentViewGroup?.removeView(this)
    newParent.addView(this)
}

val View.parents: List<ViewGroup>
    get() {
        val result = ArrayList<ViewGroup>()
        var current = parent
        while (current != null && current is ViewGroup) {
            result.add(current)
            current = current.parent
        }
        return result
    }

inline val View.parentView: View?
    get() = parent as? View?

inline val View.parentViewGroup: ViewGroup?
    get() = parent as? ViewGroup?

inline val View.rootViewGroup: ViewGroup?
    get() = this.rootView as? ViewGroup

inline val View.globalVisibleRect: Rect
    get() = Rect().also { this.getGlobalVisibleRect(it) }

inline val View.globalVisibleRectF: RectF
    get() = globalVisibleRect.toRectF()

inline val View.localVisibleRect: Rect
    get() = Rect().also { this.getLocalVisibleRect(it) }

inline val View.localVisibleRectF: RectF
    get() = localVisibleRect.toRectF()

inline val RectF.detailedString: String
    get() = "L: $left, T: $top, R: $right, B: $bottom"

inline val Rect.detailedString: String
    get() = "L: $left, T: $top, R: $right, B: $bottom"

inline fun <reified T : View> View.find(@IdRes id: Int, apply: T.() -> Unit): T = find<T>(id).apply(apply)

inline val View.millisPerFrame get() = 1000F / context.windowManager.defaultDisplay.refreshRate

// Screen width that the context is able to use, this doesn't include navigation bars
inline val View.usableScreenWidth: Int get() = context.displayMetrics.widthPixels

// Screen height that the context is able to use, this doesn't include navigation bars
inline val View.usableScreenHeight: Int get() = context.displayMetrics.heightPixels

inline val View.realScreenWidth: Int
    get() = Point().also {
        context.windowManager.defaultDisplay.getRealSize(it)
    }.x

inline val View.realScreenHeight: Int
    get() = Point().also {
        context.windowManager.defaultDisplay.getRealSize(it)
    }.y

class LogarithmicInterpolator : Interpolator {
    override fun getInterpolation(input: Float): Float {
        // kotlin.math.log(x = (4.0 * input.D) + 1.0, base = 5.0)
        // log10((9.0 * input.D) + 1.0)
        return log2(input + 1)
    }
}

inline operator fun Interpolator.get(float: Float) = this.getInterpolation(float)

inline val View.orientation: Orientation
    get() = when (context.configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> Orientation.LANDSCAPE
        Configuration.ORIENTATION_PORTRAIT -> Orientation.PORTRAIT
        else -> if (usableScreenHeight >= usableScreenWidth)
            Orientation.PORTRAIT else Orientation.LANDSCAPE
    }

inline val ViewGroup.allChildren: List<View>
    get() = this.childrenRecursiveSequence().toList()

inline fun ViewGroup.forEachReversed(action: (view: View) -> Unit) {
    for (index in childCount downTo 0) {
        getChildAt(index)?.also(action)
    }
}

inline fun ViewGroup.childUnder(x: Int, y: Int): View? {
    // Copied from RecyclerView.findChildViewUnder()
    children.toList().asReversed().forEach {
        val translationX = it.translationX
        val translationY = it.translationY
        if (x >= it.left + translationX &&
                x <= it.right + translationX &&
                y >= it.top + translationY &&
                y <= it.bottom + translationY) {
            return it
        }
    }
    return null
}

inline fun View.updateLayoutParamsSafe(block: ViewGroup.LayoutParams.() -> Unit) {
    layoutParams?.apply(block)
    requestLayout()
}

@JvmName("updateLayoutParamsSafeTyped")
inline fun <reified T : ViewGroup.LayoutParams> View.updateLayoutParamsSafe(block: T.() -> Unit) {
    (layoutParams as? T)?.apply(block)
    requestLayout()
}

inline fun RecyclerView.findChildViewUnderRaw(rawX: Float, rawY: Float): View? {
    val rect = this.globalVisibleRectF
    val x = rawX - rect.left
    val y = rawY - rect.top
    return this.findChildViewUnder(x, y)
}

inline fun RecyclerView.addOnScrollListener(
        crossinline onScrollStateChanged: (newState: Int) -> Unit = { _ -> },
        crossinline onScrolled: (dx: Int, dy: Int) -> Unit = { _, _ -> }) {
    addOnScrollListener(onScrollListener(onScrollStateChanged, onScrolled))
}

inline fun RecyclerView.onScrollListener(
        crossinline onScrollStateChanged: (newState: Int) -> Unit = { _ -> },
        crossinline onScrolled: (dx: Int, dy: Int) -> Unit = { _, _ -> }) =
        object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                onScrolled(dx, dy)
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                onScrollStateChanged(newState)
            }
        }


inline fun recycledViewPool(maxCount: Int) =
        object : RecyclerView.RecycledViewPool() {
            override fun setMaxRecycledViews(viewType: Int, max: Int) =
                    super.setMaxRecycledViews(viewType, maxCount)
        }

inline val RecyclerView.Adapter<*>.lastPosition: Int
    get() = this.itemCount - 1

inline fun RecyclerView.Adapter<*>.isAdapterPositionValid(position: Int): Boolean {
    return position >= 0 && position <= this.lastPosition
}

inline fun RecyclerView.Adapter<*>.isAdapterPositionNotValid(position: Int): Boolean {
    return !isAdapterPositionValid(position)
}

inline fun RecyclerView.Adapter<*>.notifySwapped(fromPosition: Int, toPosition: Int) {
    notifyItemRemoved(fromPosition)
    notifyItemInserted(fromPosition)
    notifyItemRemoved(toPosition)
    notifyItemInserted(toPosition)
}

inline fun RecyclerView.isViewPartiallyVisible(view: View): Boolean =
        this.layoutManager?.isViewPartiallyVisible(view, false, true) ?: false

inline fun RecyclerView.isViewCompletelyVisible(view: View): Boolean =
        this.layoutManager?.isViewPartiallyVisible(view, true, true) ?: false

inline fun RecyclerView.isViewHolderPartiallyVisible(vh: RecyclerView.ViewHolder) =
        isViewPartiallyVisible(vh.itemView)

inline fun RecyclerView.isViewHolderCompletelyVisible(vh: RecyclerView.ViewHolder) =
        isViewCompletelyVisible(vh.itemView)

inline val RecyclerView.horizontalScrollOffset: Int get() = computeHorizontalScrollOffset()
inline val RecyclerView.verticalScrollOffset: Int get() = computeVerticalScrollOffset()
inline val RecyclerView.maxHorizontalScroll: Int get() = computeHorizontalScrollRange() - computeHorizontalScrollExtent()
inline val RecyclerView.maxVerticalScroll: Int get() = computeVerticalScrollRange() - computeVerticalScrollExtent()

inline fun RecyclerView.doOnFinishScroll(
        crossinline action: (recyclerView: RecyclerView) -> Unit) {
    if (this.scrollState == RecyclerView.SCROLL_STATE_IDLE) action(this)
    else addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE ||
                    recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                removeOnScrollListener(this)
                action(recyclerView)
            }
        }
    })
}

inline val RecyclerView.ViewHolder.isAdapterPositionValid: Boolean
    get() = adapterPosition != NO_POSITION

inline val Number.isValidAdapterPosition: Boolean
    get() = this.I >= 0

inline fun animation(crossinline applyTransformation:
                     (interpolatedTime: Float, transformation: Transformation) -> Unit): Animation {
    return object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            applyTransformation(interpolatedTime, t)
        }
    }
}

inline fun animationListener(crossinline onStart: (Animation) -> Unit,
                             crossinline onRepeat: (Animation) -> Unit,
                             crossinline onEnd: (Animation) -> Unit) =
        object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) = onStart(animation)
            override fun onAnimationRepeat(animation: Animation) = onRepeat(animation)
            override fun onAnimationEnd(animation: Animation) = onEnd(animation)
        }


inline fun logE(message: Any?, tag: String = "BoardView") {
    if (BuildConfig.DEBUG) Log.e(tag, message.toString())
}

inline fun FragmentManager.commitTransaction(block: FragmentTransaction.() -> Unit) {
    this.beginTransaction().apply(block).commit()
}

inline fun ComponentActivity.addOnBackPressedCallback(crossinline onBackPressed: () -> Unit) {
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = onBackPressed()
    })
}

inline fun MotionEvent.cancel(): MotionEvent =
        this.also { it.action = MotionEvent.ACTION_CANCEL }

inline infix fun Context.convertDpToPx(dp: Number): Float =
        (dp.F * (this.resources.displayMetrics.densityDpi.F / DisplayMetrics.DENSITY_DEFAULT))

inline infix fun Context.convertPxToDp(px: Number): Float =
        (px.F / (this.resources.displayMetrics.densityDpi.F / DisplayMetrics.DENSITY_DEFAULT))

inline fun RectF.verticalPercent(pointF: PointF): Float {
    val min = min(bottom, top)
    val max = max(bottom, top)
    return (((pointF.y - min) / (max - min)) * 100F)
}

inline fun RectF.verticalPercentInverted(pointF: PointF): Float {
    val min = min(bottom, top)
    val max = max(bottom, top)
    return (((pointF.y - max) / (min - max)) * 100F)
}

inline fun RectF.horizontalPercent(pointF: PointF): Float {
    val min = min(bottom, top)
    val max = max(bottom, top)
    return (((pointF.x - min) / (max - min)) * 100F)
}

inline fun RectF.horizontalPercentInverted(pointF: PointF): Float {
    val min = min(bottom, top)
    val max = max(bottom, top)
    return (((pointF.y - max) / (min - max)) * 100F)
}
