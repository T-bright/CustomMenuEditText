package com.james.custommenuedittext

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible

/**
 * @author: tongsiwei
 * @date: 2023/6/2
 * @Description:
 */
class EditTextSelectPpw2(private val context: Context) : PopupWindow(context) {


    private lateinit var tvSelect: TextView
    private lateinit var tvSelectAll: TextView
    private lateinit var tvSelectAll2: TextView
    private lateinit var tvPaste: TextView
    private lateinit var tvPaste2: TextView
    private lateinit var tvWrap: TextView
    private lateinit var tvCut: TextView
    private lateinit var tvCopy: TextView
    private lateinit var tvFullScreenInput: TextView
    private lateinit var llContent: LinearLayout
    private lateinit var line1: View
    private lateinit var line2: View
    private lateinit var line3: View
    private lateinit var lineAfter1: View

    private lateinit var llContentAfterSelect: LinearLayout

    private var mHeight: Int = 0
    private var mWidth: Int = 0

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.ppw_edit_select2, null)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        mHeight = view.measuredHeight
        mWidth = view.measuredWidth
        contentView = view
        width = ViewGroup.LayoutParams.MATCH_PARENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        isClippingEnabled = false
        isTouchable = true
        isOutsideTouchable = false
        setBackgroundDrawable(BitmapDrawable(context.resources, null as Bitmap?))
        findViews()
    }

    private fun findViews() {
        tvSelect = contentView.findViewById(R.id.tvSelect) as TextView
        tvSelectAll = contentView.findViewById(R.id.tvSelectAll) as TextView
        tvSelectAll2 = contentView.findViewById(R.id.tvSelectAll2) as TextView
        tvPaste = contentView.findViewById(R.id.tvPaste) as TextView
        tvPaste2 = contentView.findViewById(R.id.tvPaste2) as TextView
        tvWrap = contentView.findViewById(R.id.tvWrap) as TextView
        tvFullScreenInput = contentView.findViewById(R.id.tvFullScreenInput) as TextView
        llContent = contentView.findViewById(R.id.llContent) as LinearLayout
        line1 = contentView.findViewById(R.id.line1)
        line2 = contentView.findViewById(R.id.line2)
        line3 = contentView.findViewById(R.id.line3)
        lineAfter1 = contentView.findViewById(R.id.lineAfter1)
        llContentAfterSelect = contentView.findViewById(R.id.llContentAfterSelect)
        tvCut = contentView.findViewById(R.id.tvCut) as TextView
        tvCopy = contentView.findViewById(R.id.tvCopy) as TextView
        tvSelect.setOnClickListener {
            listener?.onSelect(false)
        }
        tvSelectAll.setOnClickListener {
            listener?.onSelect(true)
        }
        tvSelectAll2.setOnClickListener {
            listener?.onSelect(true)
        }
        tvPaste.setOnClickListener {
            listener?.onPaste()
        }
        tvPaste2.setOnClickListener {
            listener?.onPaste()
        }
        tvWrap.setOnClickListener {
            listener?.onWrap()
        }
        tvFullScreenInput.setOnClickListener {
            listener?.onFullScreen()
        }
        tvCut.setOnClickListener {
            listener?.onCut()
        }
        tvCopy.setOnClickListener {
            listener?.onCopy()
        }
    }

    private val mTempCoors = IntArray(2)
    fun showSelect(view: EditText, text: String) {
        showPop(view)
        tvSelect.isVisible = text.isNotEmpty()
        tvSelectAll.isVisible = text.isNotEmpty()
        line1.isVisible = text.isNotEmpty()
        line2.isVisible = text.isNotEmpty()
        llContentAfterSelect.isGone = true
        llContent.isVisible = true
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        cb?.let {
            val clipData = cb.primaryClip
            if (cb.hasPrimaryClip() && clipData != null && clipData.itemCount > 0) {
                val isHasClip = clipData.getItemAt(0).text.isNotEmpty()
                line3.isVisible = isHasClip
                tvPaste.isVisible = isHasClip
            } else {
                line3.isVisible = false
                tvPaste.isVisible = false
            }
        }
    }

    fun showAfterSelect(view: EditText, isSelectAll: Boolean) {
        showPop(view)
        refreshAfterSelect(isSelectAll)
    }

    fun refreshAfterSelect(isSelectAll: Boolean) {
        llContent.isGone = true
        llContentAfterSelect.isVisible = true
        tvSelectAll2.isGone = isSelectAll
        lineAfter1.isGone = isSelectAll
    }

    private fun showPop(view: EditText) {
        view.getLocationInWindow(mTempCoors)
        val layout = view.layout
        var posX: Int = layout.getPrimaryHorizontal(0).toInt() + mTempCoors[0]
        if (posX <= 0) posX = 16

        var posY: Int = layout.getLineTop(layout.getLineForOffset(0)) + mTempCoors[1]
        posY = if (posY < mHeight) {
            mHeight - 16
        } else {
            layout.getLineTop(layout.getLineForOffset(0)) + mTempCoors[1] - mHeight - 16
        }
        if (posY < 0) posY = 16

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            elevation = 8f
        }
        super.showAtLocation(view, Gravity.NO_GRAVITY, 10, posY)
    }


    private var listener: SelectPpwListener? = null
    fun setOnSelectPpwListener(listener: SelectPpwListener) {
        this.listener = listener
    }

    interface SelectPpwListener {
        /**
         * 选择文本
         * @param isSelectAll 是否是全选
         */
        fun onSelect(isSelectAll: Boolean)

        /**
         * 复制
         */
        fun onCopy()


        /**
         * 剪切
         */
        fun onCut()

        /**
         * 粘贴
         */
        fun onPaste()


        /**
         * 换行
         */
        fun onWrap()

        /**
         * 全屏
         */
        fun onFullScreen()
    }

}