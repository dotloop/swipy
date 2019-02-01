package com.dotloop.sweepy

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.Gravity.LEFT
import android.view.Gravity.NO_GRAVITY
import android.view.Gravity.RIGHT
import android.widget.AdapterView
import android.widget.FrameLayout
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import androidx.customview.widget.ViewDragHelper.create
import com.dotloop.sweepy.SwipeLayout.DragEdge.LEFT_EDGE
import com.dotloop.sweepy.SwipeLayout.DragEdge.RIGHT_EDGE
import com.dotloop.sweepy.SwipeLayout.Status.CLOSE
import com.dotloop.sweepy.SwipeLayout.Status.MIDDLE
import com.dotloop.sweepy.SwipeLayout.Status.OPEN

/**
 * Credit to https://github.com/daimajia/AndroidSwipeLayout
 * This is not the original class, it has been modify to fit dotloop's needs
 */
class SwipeLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {
    private var touchSlop: Int = 0
    private var dragHelper: ViewDragHelper
    private var dragEdges: HashMap<SwipeLayout.DragEdge, View> = HashMap()
    private var edgeSwipesOffset = FloatArray(2)
    private var clickToClose = false
    private var dragHelperCallback: ViewDragHelper.Callback = object : ViewDragHelper.Callback() {

        //Restrict the motion of the dragged child view along the horizontal axis.
        //The default implementation does not allow horizontal motion
        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return when (currentDragEdge) {
                DragEdge.RIGHT_EDGE -> Math.max(left, -(currentActionView?.width ?: left))
                else                -> Math.min(left, currentActionView?.width ?: left)

            }
        }

        //Called when the user's input indicates that they want to capture the given child view with
        //the pointer indicated by pointerId. The callback should return true if the user is
        //permitted to drag the given view with the indicated pointer.
        override fun tryCaptureView(child: View, pointerId: Int): Boolean =
            child == surfaceView || actionViews.contains(child)

        //Return the magnitude of a draggable child view's horizontal range of motion in pixels.
        //This method should return 0 for views that cannot move horizontally.
        override fun getViewHorizontalDragRange(child: View): Int = dragDistance

        //Called when the child view is no longer being actively dragged.
        override fun onViewReleased(releasedChild: View, xVelocity: Float, yVelocity: Float) {
            super.onViewReleased(releasedChild, xVelocity, yVelocity)
            processHandRelease(xVelocity)
            invalidate()
        }

        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            val surfaceView = surfaceView ?: return

            val currentActionView = currentActionView
            if (actionViews.contains(changedView)) {

                val rect = computeActionLayDown(currentDragEdge)
                currentActionView?.layout(rect.left, rect.top, rect.right, rect.bottom)

                var newLeft = surfaceView.left + dx

                if (currentDragEdge == LEFT_EDGE && newLeft < paddingLeft)
                    newLeft = paddingLeft
                else if (currentDragEdge == RIGHT_EDGE && newLeft > paddingLeft)
                    newLeft = paddingLeft

                surfaceView.layout(newLeft, surfaceView.top, newLeft + measuredWidth, surfaceView.top + measuredHeight)
            }

            dispatchSwipeEvent()

            invalidate()

            captureChildrenBound()
        }
    }

    init {
        //ViewDragHelper is a utility class for writing custom ViewGroups. It offers a number of
        //useful operations and state tracking for allowing a user to drag and reposition
        //views within their parent ViewGroup.
        dragHelper = create(this, dragHelperCallback)

        //Distance in pixels a touch can wander before we think the user is scrolling
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop

        //Get XML attributes
        val a = context.obtainStyledAttributes(attrs, R.styleable.SwipeLayout)
        val dragEdgeChoices = a.getInt(R.styleable.SwipeLayout_drag_edge, DRAG_RIGHT)
        edgeSwipesOffset[LEFT_EDGE.ordinal] = a.getDimension(R.styleable.SwipeLayout_leftEdgeSwipeOffset, 0f)
        edgeSwipesOffset[RIGHT_EDGE.ordinal] = a.getDimension(R.styleable.SwipeLayout_rightEdgeSwipeOffset, 0f)
        clickToClose = a.getBoolean(R.styleable.SwipeLayout_clickToClose, clickToClose)

        if (dragEdgeChoices and DRAG_LEFT == DRAG_LEFT) {
            dragEdges.remove(LEFT_EDGE)
        }
        if (dragEdgeChoices and DRAG_RIGHT == DRAG_RIGHT) {
            dragEdges.remove(RIGHT_EDGE)
        }

        a.recycle()
    }

    companion object {
        private const val VIEW_PERCENT_TO_CLOSE = 0.75f
        private const val VIEW_PERCENT_TO_OPEN = 0.25f
        private const val DRAG_LEFT = 1
        private const val DRAG_RIGHT = 2
    }

    private var swipedX = -1f
    private var swipedY = -1f
    private var dragDistance = 0
    private var hitSurfaceRect: Rect = Rect()

    private var currentDragEdge: SwipeLayout.DragEdge = SwipeLayout.DragEdge.RIGHT_EDGE
        set(value) {
            field = value
            updateActionViews()
        }
    private var gestureDetector = GestureDetector(getContext(), SwipeDetector())
    private var swipeEnabled = true
    private var swipesEnabled = booleanArrayOf(true, true)
    private var isBeingDragged: Boolean = false

    var onSwipeListeners: OnSwipeListener? = null
    var simpleOnLayoutChangeListenerListener: SimpleOnLayoutChangeListener? = null
    private var clickListener: View.OnClickListener? = null

    private var viewBoundCache: HashMap<View, Rect> = HashMap()//save all children's bound, restore in onLayoutChange

    /**
     * Offset between the current position and the edge of the view
     */
    private val currentOffset: Float
        get() = edgeSwipesOffset[currentDragEdge.ordinal]

    /**
     * null if there is no action view
     */
    private val currentActionView: View?
        get() {
            val actionViews = actionViews
            return when {
                currentDragEdge.ordinal < actionViews.size -> actionViews[currentDragEdge.ordinal]
                else                                       -> null
            }
        }

    /**
     * All actionViews: left, right (may be null if the edge is not set)
     */
    private val actionViews: Array<View?>
        get() {
            val actionViews = arrayOfNulls<View>(DragEdge.values().size)
            for (dragEdge in DragEdge.values()) {
                actionViews[dragEdge.ordinal] = dragEdges[dragEdge]
            }
            return actionViews
        }

    /**
     * Null if there is no surface view(no children)
     */
    private val surfaceView: View?
        get() {
            return if (childCount == 0) null else getChildAt(childCount - 1)
        }

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        clickListener = l
    }

    private fun isTouchOnSurface(ev: MotionEvent): Boolean {
        val surfaceView = surfaceView ?: return false
        surfaceView.getHitRect(hitSurfaceRect)
        return hitSurfaceRect.contains(ev.x.toInt(), ev.y.toInt())
    }

    /**
     * get the open status.
     *
     * @return [Status] OPEN , CLOSE or
     * MIDDLE.
     */
    private val openStatus: Status?
        get() {
            val surfaceView = surfaceView ?: return CLOSE
            val surfaceLeft = surfaceView.left
            val surfaceTop = surfaceView.top
            when {
                surfaceLeft == paddingLeft && surfaceTop == paddingTop -> return CLOSE
                surfaceLeft == paddingLeft - dragDistance ||
                surfaceLeft == paddingLeft + dragDistance ||
                surfaceTop == paddingTop - dragDistance ||
                surfaceTop == paddingTop + dragDistance                -> return OPEN
            }
            return MIDDLE

        }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!swipeEnabled || event == null) return super.onTouchEvent(event)

        val action = event.actionMasked
        gestureDetector.onTouchEvent(event)

        when (action) {
            MotionEvent.ACTION_DOWN   -> {
                dragHelper.processTouchEvent(event)
                swipedX = event.rawX
                swipedY = event.rawY
                //the drag state and the direction are already judged at onInterceptTouchEvent
                checkCanDrag(event)
                if (isBeingDragged) {
                    parent.requestDisallowInterceptTouchEvent(true)
                    dragHelper.processTouchEvent(event)
                }
            }


            MotionEvent.ACTION_MOVE   -> {
                checkCanDrag(event)
                if (isBeingDragged) {
                    parent.requestDisallowInterceptTouchEvent(true)
                    dragHelper.processTouchEvent(event)
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                isBeingDragged = false
                dragHelper.processTouchEvent(event)
            }
            //handle other action, such as ACTION_POINTER_DOWN/UP
            else                      -> dragHelper.processTouchEvent(event)
        }

        return super.onTouchEvent(event) || isBeingDragged || action == MotionEvent.ACTION_DOWN
    }

    private val isLeftSwipeEnabled: Boolean
        get() {
            val bottomView = dragEdges[LEFT_EDGE]
            return bottomView != null && bottomView.parent == this && bottomView !== surfaceView && swipesEnabled[LEFT_EDGE.ordinal]
        }

    private val isRightSwipeEnabled: Boolean
        get() {
            val bottomView = dragEdges[RIGHT_EDGE]
            return (bottomView != null && bottomView.parent == this
                    && bottomView !== surfaceView && swipesEnabled[RIGHT_EDGE.ordinal])
        }

    private fun hasAdapterView(): Boolean = adapterView != null

    private var adapterView: AdapterView<*>? = parent as? AdapterView<*>

    /**
     * Process the surface release event.
     *
     * @param xVelocity xVelocity
     */
    fun processHandRelease(xVelocity: Float) {
        val surfaceView = surfaceView ?: return
        val minVelocity = dragHelper.minVelocity
        val currentDragEdge = this.currentDragEdge

        val viewPercent = if (openStatus == CLOSE)
            VIEW_PERCENT_TO_OPEN
        else
            VIEW_PERCENT_TO_CLOSE

        when (currentDragEdge) {
            LEFT_EDGE  -> {
                val openPercent = 1f * surfaceView.left / dragDistance
                when {
                    //First check the velocity
                    xVelocity > minVelocity   -> open()
                    xVelocity < -minVelocity  -> close()
                    //Then if the view has been dragged enough
                    openPercent > viewPercent -> open()
                    else                      -> close()
                }
            }
            RIGHT_EDGE -> {
                val openPercent = 1f * -surfaceView.left / dragDistance
                when {
                    //First check the velocity
                    xVelocity > minVelocity   -> close()
                    xVelocity < -minVelocity  -> open()
                    //Then if the view has been dragged enough
                    openPercent > viewPercent -> open()
                    else                      -> close()
                }
            }
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (!swipeEnabled) {
            return false
        }
        if (clickToClose && openStatus == OPEN && isTouchOnSurface(event)) {
            return true
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragHelper.processTouchEvent(event)
                isBeingDragged = false
                swipedX = event.rawX
                swipedY = event.rawY
                //if the swipe is in middle state(scrolling), should intercept the touch
                if (openStatus == MIDDLE) {
                    isBeingDragged = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val beforeCheck = isBeingDragged
                checkCanDrag(event)
                if (isBeingDragged) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (!beforeCheck && isBeingDragged) {
                    //let children has one chance to catch the touch, and request the swipe not intercept
                    //useful when swipeLayout wrap a swipeLayout or other gestural layout
                    return false
                }
            }

            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP   -> {
                isBeingDragged = false
                dragHelper.processTouchEvent(event)
            }
            //handle other action, such as ACTION_POINTER_DOWN/UP
            else                    -> dragHelper.processTouchEvent(event)
        }
        return isBeingDragged
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        updateActionViews()
        simpleOnLayoutChangeListenerListener?.onLayoutChange(this)
    }

    @JvmOverloads
    fun addDrag(dragEdge: DragEdge, child: View, layoutParams: ViewGroup.LayoutParams? = generateDefaultLayoutParams()) {
        var params = layoutParams
        if (!checkLayoutParams(params)) {
            params = generateLayoutParams(params)
        }
        val gravity = when (dragEdge) {
            LEFT_EDGE  -> LEFT
            RIGHT_EDGE -> RIGHT
        }
        if (params is FrameLayout.LayoutParams) {
            params.gravity = gravity
        }
        addView(child, 0, params)
    }

    @JvmOverloads
    fun open(smooth: Boolean = true, notify: Boolean = true, edge: DragEdge? = null) {
        edge?.let { currentDragEdge = edge }
        val surface = surfaceView ?: return
        val rect = computeSurfaceLayoutArea(true)
        when {
            smooth -> dragHelper.smoothSlideViewTo(surface, rect.left, rect.top)
            else   -> {
                surface.layout(rect.left, rect.top, rect.right, rect.bottom)
                if (notify) {
                    dispatchSwipeEvent()
                } else {
                    preventActionViewClicks()
                }
            }
        }
        invalidate()
    }

    /**
     * close surface
     *
     * @param smooth smoothly or not.
     * @param notify if notify all the listeners.
     */
    @JvmOverloads
    fun close(smooth: Boolean = true, notify: Boolean = true) {
        val surface = surfaceView ?: return
        if (smooth)
            dragHelper.smoothSlideViewTo(surface, paddingLeft, paddingTop)
        else {
            val rect = computeSurfaceLayoutArea(false)
            surface.layout(rect.left, rect.top, rect.right, rect.bottom)
            if (notify) {
                dispatchSwipeEvent()
            } else {
                preventActionViewClicks()
            }
        }
        invalidate()
    }

    private fun checkCanDrag(ev: MotionEvent) {
        if (isBeingDragged) return
        if (openStatus == MIDDLE) {
            isBeingDragged = true
            return
        }
        val status = openStatus
        val distanceX = ev.rawX - swipedX
        val distanceY = ev.rawY - swipedY
        val angle = Math.abs(distanceY / distanceX)
        val angleDegree = Math.toDegrees(Math.atan(angle.toDouble())).toFloat()
        if (openStatus == CLOSE) {
            val dragEdge = when {
                distanceX > 0 && isLeftSwipeEnabled  -> LEFT_EDGE
                distanceX < 0 && isRightSwipeEnabled -> RIGHT_EDGE
                else                                 -> return
            }
            currentDragEdge = dragEdge
        }

        val suitable = when {
            status == MIDDLE              -> true
            currentDragEdge == RIGHT_EDGE -> status == OPEN && distanceX > touchSlop || status == CLOSE && distanceX < -touchSlop
            currentDragEdge == LEFT_EDGE  -> status == OPEN && distanceX < -touchSlop || status == CLOSE && distanceX > touchSlop
            else                          -> false
        }

        val doNothing = angleDegree > 30 || !suitable
        isBeingDragged = !doNothing
    }

    private fun layoutLayDown() {
        actionViews.forEach {
            it?.visibility = if (it != currentActionView) View.INVISIBLE else View.VISIBLE
        }

        val surfaceRect: Rect = viewBoundCache[surfaceView] ?: computeSurfaceLayoutArea(false)
        surfaceView?.run {
            layout(surfaceRect.left, surfaceRect.top, surfaceRect.right, surfaceRect.bottom)
            bringChildToFront(surfaceView)
        }
        val actionViewRect: Rect = viewBoundCache[currentActionView]
                                   ?: computeActionLayoutAreaViaSurface(surfaceRect)
        currentActionView?.layout(actionViewRect.left, actionViewRect.top, actionViewRect.right, actionViewRect.bottom)
    }

    private fun dispatchSwipeEvent(open: Boolean = true) {
        preventActionViewClicks()
        val status = openStatus

        onSwipeListeners?.let {
            if (open) {
                it.onStartOpen(this)
            } else {
                it.onStartClose(this)
            }

            when (status) {
                CLOSE -> it.onClose(this)
                OPEN  -> {
                    val currentActionView = currentActionView
                    currentActionView?.isEnabled = true
                    it.onOpen(this)
                }
                else  -> null
            }
        }
    }

    /**
     * prevent action view get any touch event
     */
    private fun preventActionViewClicks() {
        when (openStatus) {
            CLOSE -> for (actionView in actionViews) {
                actionView?.run {
                    if (visibility != View.INVISIBLE)
                        visibility = View.VISIBLE
                }
            }
            else  -> {
                currentActionView?.run {
                    if (visibility != View.VISIBLE)
                        visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * save children's bounds, so they can restore the bound in [.onLayout]
     */
    private fun captureChildrenBound() {
        val currentActionView = currentActionView
        if (openStatus == Status.CLOSE) {
            viewBoundCache.remove(currentActionView)
            return
        }

        val views = arrayOf(surfaceView, currentActionView)
        views.filterNotNull()
            .forEach { child ->
                val rect: Rect = viewBoundCache[child] ?: Rect()
                viewBoundCache[child] = rect
                rect.left = child.left
                rect.top = child.top
                rect.right = child.right
                rect.bottom = child.bottom
            }

    }

    private fun computeSurfaceLayoutArea(open: Boolean): Rect {
        val left = when {
            !open                        -> paddingLeft
            currentDragEdge == LEFT_EDGE -> paddingLeft + dragDistance
            else                         -> paddingLeft - dragDistance
        }

        return Rect(left, paddingTop, left + measuredWidth, paddingTop + measuredHeight)
    }

    private fun computeActionLayoutAreaViaSurface(surfaceArea: Rect): Rect {
        val left = when (currentDragEdge) {
            RIGHT_EDGE -> surfaceArea.right - dragDistance
            else       -> surfaceArea.left
        }
        val right = when (currentDragEdge) {
            LEFT_EDGE -> surfaceArea.left + dragDistance
            else      -> surfaceArea.right
        }

        return Rect(left, surfaceArea.top, right, surfaceArea.bottom)

    }

    private fun computeActionLayDown(dragEdge: DragEdge): Rect {
        val left = when (dragEdge) {
            LEFT_EDGE  -> paddingLeft
            RIGHT_EDGE -> measuredWidth - dragDistance
        }
        val top = paddingTop
        val right = left + dragDistance
        val bottom = top + measuredHeight
        return Rect(left, top, right, bottom)
    }

    private fun dp2px(dp: Float): Int =
        (dp * context.resources.displayMetrics.density + 0.5f).toInt()

    private fun performAdapterViewItemClick() {
        if (openStatus != CLOSE) return
        val top = parent
        if (top is AdapterView<*>) {
            val position = top.getPositionForView(this)
            if (position != AdapterView.INVALID_POSITION) {
                top.performItemClick(top.getChildAt(position - top.firstVisiblePosition), position, top.adapter.getItemId(position))
            }
        }
    }

    private fun updateActionViews() {
        currentActionView?.run { dragDistance = measuredWidth - dp2px(currentOffset) }
        layoutLayDown()
        preventActionViewClicks()
    }

    override fun onViewRemoved(child: View) {
        dragEdges = HashMap(dragEdges.filterValues { value -> value == child })
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (child == null) return
        val gravity = gravityFromLayout(params)
        if (gravity > 0) {
            val absoluteGravity = GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this))
            when {
                absoluteGravity and LEFT == LEFT   -> dragEdges[LEFT_EDGE] = child
                absoluteGravity and RIGHT == RIGHT -> dragEdges[RIGHT_EDGE] = child
            }
        }

        if (child.parent == this) return
        super.addView(child, index, params)
    }

    private fun gravityFromLayout(params: ViewGroup.LayoutParams?): Int {
        try {
            params?.let { return params.javaClass.getField("gravity").get(params) as Int }
        } catch (e: Exception) {
            Log.e(javaClass.canonicalName, "Failed to get View Gravity. We default to Gravity.NO_GRAVITY")
            return NO_GRAVITY
        }
        return NO_GRAVITY
    }

    /**
     * This is called when the view is attached to a window.
     * At this point it has a Surface and will start drawing
     **/
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (hasAdapterView()) {
            clickListener?.run {
                setOnClickListener { performAdapterViewItemClick() }
            }
        }
    }

    override fun computeScroll() {
        super.computeScroll()
        if (dragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    private inner class SwipeDetector : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (clickToClose && isTouchOnSurface(e)) {
                close()
            }
            return super.onSingleTapUp(e)
        }
    }

    /**
     * Status fo the dragged view
     */
    enum class Status {
        MIDDLE,
        OPEN,
        CLOSE
    }

    /**
     * View edges that can be dragged
     */
    enum class DragEdge {
        LEFT_EDGE,
        RIGHT_EDGE
    }
}
