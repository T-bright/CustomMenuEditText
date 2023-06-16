package com.james.custommenuedittext

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.*
import androidx.appcompat.R
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * @author: tongsiwei
 * @date: 2023/6/15
 * @Description:
 */
class CustomMenuMenuEditText @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = R.attr.editTextStyle) :
    AppCompatEditText(context, attrs, defStyleAttr) {

    companion object {
        const val TAG = "CustomMenuMenuEditText"
    }

    //start 禁用系统复制操作弹框-----------------------------------------------------------------------------
    override fun showContextMenu(): Boolean {
        return false
    }

    override fun startActionMode(callback: ActionMode.Callback?): ActionMode? {
        return null
    }

    override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
        return null
    }

    override fun onCreateContextMenu(menu: ContextMenu?) {

    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        return false
    }
    //end -----------------------------------------------------------------------------


    private val attachStateChangeListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View?) {
            Log.d(TAG,"view attached to window")
        }

        override fun onViewDetachedFromWindow(v: View?) {
            Log.d(TAG,"view detached from window")
            destroy()
        }
    }

    private val scrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
        removeCallbacks(showSelectPopRunnable)//滑动后，不要显示操作弹框
        if (selectPpw?.isShowing == true) {//如果操作弹框是显示的，要消失掉
            Log.e(TAG,"scrollChangedListener  dismissSelectPpw")
            dismissSelectPpw()
            hideCursorHandler()
            setSelection(selectionEnd)
        }
    }

    init {
        setOnLongClickListener {
            messageEditTextIsLongClick = true
            return@setOnLongClickListener true
        }
        initSelectPpw()
        addOnAttachStateChangeListener(attachStateChangeListener)
        viewTreeObserver.addOnScrollChangedListener(scrollChangedListener)
    }

    //多次点击的超时时间。如300毫秒，在300毫秒内多次点击，则拦截事件。防止系统EditText 文本被选中。
    private val manyTapTimeOut = ViewConfiguration.getDoubleTapTimeout().toLong()
    private var messageEditTextIsLongClick = false

    //多次点击的key
    private val manyTapKey = 5
    private val manyTapHandler = Handler(Looper.myLooper()!!)

    private var editTextTouchX = 0f
    private var editTextTouchY = 0f
    private var editTextLastSelectIndex = 0
    private var selectPpw: EditTextSelectPpw2? = null
    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (selStart == selEnd) {//相同，表示未选择多个文本字段
            dismissSelectPpw()
        } else {
            selectPpw?.refreshAfterSelect(selEnd - selStart == this.length())
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.e("AAA", "onTouchEvent : ${event?.action}")
        if (event == null) return super.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_DOWN) {
            editTextTouchX = event.x
            editTextTouchY = event.y
            val hadManyTapMessage = manyTapHandler.hasMessages(manyTapKey)
            if (hadManyTapMessage) manyTapHandler.removeMessages(manyTapKey)
            manyTapHandler.sendEmptyMessageDelayed(manyTapKey, manyTapTimeOut)
            Log.e("AAA", "onTouchEvent ACTION_DOWN  hadManyTapMessage: $hadManyTapMessage")
            if (hadManyTapMessage) {//如果有，表示之前有快速点击的事件，要拦截。此处拦截主要防止双击Edittext时，系统会选中文本。
                return true
            }
        }

        if (event.action == MotionEvent.ACTION_UP) {
            if (!messageEditTextIsLongClick) { //非长按，点击的效果。
                val position: Int = getOffsetForPosition(event.x, event.y)
                if (selectPpw != null) {
                    if (selectPpw?.isShowing == true) {
                        dismissSelectPpw()
                    } else {
                        Log.e("AAA", "onTouchEvent ACTION_UP  position: ${position}     ----    editTextLastSelectIndex = ${editTextLastSelectIndex}")
                        if (position == editTextLastSelectIndex) {
                            if (isSoftKeyboardVisible(this)) {
                                showSelectPop()
                            }
                        }
                    }
                }
                editTextLastSelectIndex = position
            } else {//长按
                if (isSoftKeyboardVisible(this)) {
                    showSelectPop()
                }
            }
            messageEditTextIsLongClick = false
        }
        return super.onTouchEvent(event)
    }

    fun dismissSelectPpw() {
        if (selectPpw?.isShowing == false) return //ppw如果不显示，就不重复操作了

        selectPpw?.dismiss()

        //selectPpw 消失后，可以显示放大镜
        val editors = getEditorInfo()
        if (editors.isNullOrEmpty() || editors.size < 2) return
        val mEditor = editors[0]
        val mEditorClass = editors[1] as Class<*>
        showMagnifier(mEditorClass, mEditor!!)
    }

    private fun initSelectPpw() {
        if (selectPpw == null) {
            selectPpw = EditTextSelectPpw2(context)
            selectPpw?.setOnSelectPpwListener(object : EditTextSelectPpw2.SelectPpwListener {
                override fun onSelect(isSelectAll: Boolean) {
                    dismissSelectPpw()
                    val textLength: Int = this@CustomMenuMenuEditText.text?.length ?: 0
                    if (isSelectAll) {
                        this@CustomMenuMenuEditText.setSelection(0, textLength)
                        selectPpw?.showAfterSelect(this@CustomMenuMenuEditText, true)
                    } else {
                        val position: Int = this@CustomMenuMenuEditText.getOffsetForPosition(editTextTouchX, editTextTouchY)
                        if (position == 0) {
                            this@CustomMenuMenuEditText.setSelection(0, 1)
                        } else {
                            this@CustomMenuMenuEditText.setSelection(position - 1, position)
                        }
                        selectPpw?.showAfterSelect(this@CustomMenuMenuEditText, false)
                    }
                    showCursorHandler()
                }

                override fun onCopy() {
                    dismissSelectPpw()
                    this@CustomMenuMenuEditText.post {
                        val cb = this@CustomMenuMenuEditText.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val selectText = this@CustomMenuMenuEditText.text.toString().subSequence(this@CustomMenuMenuEditText.selectionStart, this@CustomMenuMenuEditText.selectionEnd)
                        cb.setPrimaryClip(ClipData.newPlainText("text", selectText))
                        hideCursorHandler()
                        this@CustomMenuMenuEditText.setSelection(this@CustomMenuMenuEditText.selectionEnd)
                    }
                }

                override fun onCut() {
                    dismissSelectPpw()
                    this@CustomMenuMenuEditText.post {
                        val cb = this@CustomMenuMenuEditText.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val selectText = this@CustomMenuMenuEditText.text.toString().subSequence(this@CustomMenuMenuEditText.selectionStart, this@CustomMenuMenuEditText.selectionEnd)
                        cb.setPrimaryClip(ClipData.newPlainText("text", selectText))
                        this@CustomMenuMenuEditText.text?.delete(this@CustomMenuMenuEditText.selectionStart, this@CustomMenuMenuEditText.selectionEnd)
                        hideCursorHandler()
                        this@CustomMenuMenuEditText.setSelection(this@CustomMenuMenuEditText.selectionEnd)
                    }
                }

                override fun onPaste() {
                    dismissSelectPpw()
                    this@CustomMenuMenuEditText.post {
                        val cb = this@CustomMenuMenuEditText.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = cb.primaryClip
                        if (cb.hasPrimaryClip() && clipData != null && clipData.itemCount > 0) {
                            val item = clipData.getItemAt(0)
                            this@CustomMenuMenuEditText.text?.insert(this@CustomMenuMenuEditText.selectionStart, item.text.toString())
                        }
                        hideCursorHandler()
                    }
                }

                override fun onWrap() {
                    dismissSelectPpw()
                    this@CustomMenuMenuEditText.text?.insert(this@CustomMenuMenuEditText.selectionStart, "\n")
                }

                override fun onFullScreen() {
                    dismissSelectPpw()

                }
            })
        }
    }

    private val showSelectPopRunnable = Runnable {
        selectPpw?.showSelect(this, this.text.toString())
    }

    private fun showSelectPop() {
        this.postDelayed(showSelectPopRunnable, 200)
    }

    //显示游标
    private fun showCursorHandler() {
        reflectOperateCursorHandler("show")
    }

    //隐藏游标
    private fun hideCursorHandler() {
        reflectOperateCursorHandler("hide")
    }


    /**
     *  放大镜的缩放倍数：若缩放过大，系统会将放大镜隐藏。所以根据修改系统的此字段数值来达到控制放大镜的显影。
     *  系统的逻辑代码在 android.widget.Editor.HandleView.HandleView.tooLargeTextForMagnifier() 方法中。
     */
    private var mMagnifierZoom = -1.0f

    /**
     * 反射控制显影 游标
     * @param method 需要反射调用的方法名
     */
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun reflectOperateCursorHandler(method: String) {
        try {
            val editors = getEditorInfo()
            if (editors.isNullOrEmpty() || editors.size < 2) return
            val mEditor = editors[0]
            val mEditorClass = editors[1] as Class<*>
            val mSelectionModifierCursorControllerField = mEditorClass.getDeclaredField("mSelectionModifierCursorController")
            mSelectionModifierCursorControllerField.isAccessible = true
            val mSelectionModifierCursorController = mSelectionModifierCursorControllerField.get(mEditor)
            val showMethod = mSelectionModifierCursorController.javaClass.getDeclaredMethod(method)
            showMethod.invoke(mSelectionModifierCursorController)
            //显示游标时，要隐藏放大镜
            if (method == "show") {
                hideMagnifier(mEditorClass, mEditor!!)
            }
        } catch (e: Exception) {
            Log.e(TAG,e.toString())
        }
    }


    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun getEditorInfo(): Array<Any?>? {
        try {
            var mTextViewClass = this.javaClass.superclass
            while (true) {
                if (mTextViewClass.name != "android.widget.TextView") {
                    mTextViewClass = mTextViewClass.superclass
                    if (mTextViewClass.name == "android.view.View") {
                        break
                    }
                } else {
                    break
                }
            }
            val mEditorField = mTextViewClass.getDeclaredField("mEditor")
            mEditorField.isAccessible = true
            val mEditor = mEditorField.get(this)
            var mEditorClass = mEditor.javaClass
            while (true) {
                if (mEditorClass.name != "android.widget.Editor") {
                    mEditorClass = mEditorClass.superclass
                    if (mEditorClass.name == "android.view.View") {
                        break
                    }
                } else {
                    break
                }
            }
            return arrayOf(mEditor, mEditorClass)
        } catch (e: Exception) {
            Log.e(TAG,e.toString())
        }
        return null
    }

    /**
     * 隐藏放大镜。当 [EditTextSelectPpw] 显示时，滑动游标，放大镜功能不显示。
     */
    private fun hideMagnifier(mEditorClass: Class<*>, mEditor: Any) {
        try {
            val getMagnifierAnimatorMethod = mEditorClass.getDeclaredMethod("getMagnifierAnimator")
            getMagnifierAnimatorMethod.isAccessible = true
            getMagnifierAnimatorMethod.invoke(mEditor)

            val mMagnifierAnimatorField = mEditorClass.getDeclaredField("mMagnifierAnimator")
            mMagnifierAnimatorField.isAccessible = true
            val mMagnifierAnimator = mMagnifierAnimatorField.get(mEditor)

            val mMagnifierField = mMagnifierAnimator.javaClass.getDeclaredField("mMagnifier")
            mMagnifierField.isAccessible = true
            val mMagnifier = mMagnifierField.get(mMagnifierAnimator)

            val mZoomField = mMagnifier.javaClass.getDeclaredField("mZoom")
            mZoomField.isAccessible = true
            if (mMagnifierZoom == -1.0f) {
                mMagnifierZoom = mZoomField.get(mMagnifier) as Float
            }
            Log.d(TAG,"  current mZoom = $mMagnifierZoom -- after set 200")
            mZoomField.set(mMagnifier, 200f)
        } catch (e: Exception) {
            Log.d(TAG,e.toString())
        }
    }

    /**
     * 显示放大镜。当 [EditTextSelectPpw] 隐藏时，滑动 EditText 是可以显示放大镜的。
     */
    private fun showMagnifier(mEditorClass: Class<*>, mEditor: Any) {
        try {
            val getMagnifierAnimatorMethod = mEditorClass.getDeclaredMethod("getMagnifierAnimator")
            getMagnifierAnimatorMethod.isAccessible = true
            getMagnifierAnimatorMethod.invoke(mEditor)

            val mMagnifierAnimatorField = mEditorClass.getDeclaredField("mMagnifierAnimator")
            mMagnifierAnimatorField.isAccessible = true
            val mMagnifierAnimator = mMagnifierAnimatorField.get(mEditor)

            val mMagnifierField = mMagnifierAnimator.javaClass.getDeclaredField("mMagnifier")
            mMagnifierField.isAccessible = true
            val mMagnifier = mMagnifierField.get(mMagnifierAnimator)

            val mZoomField = mMagnifier.javaClass.getDeclaredField("mZoom")
            mZoomField.isAccessible = true
            if (mMagnifierZoom != -1.0f) {
                Log.d(TAG," current mZoom = 200 -- after set $mMagnifierZoom")
                mZoomField.set(mMagnifier, mMagnifierZoom)
            }
        } catch (e: Exception) {
            Log.d(TAG,e.toString())
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun destroy() {
        removeCallbacks(showSelectPopRunnable)
        viewTreeObserver.removeOnScrollChangedListener(scrollChangedListener)
        removeOnAttachStateChangeListener(attachStateChangeListener)
    }


    private fun isSoftKeyboardVisible(view: View): Boolean {
        val insets = ViewCompat.getRootWindowInsets(view)
        return insets?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
    }

}