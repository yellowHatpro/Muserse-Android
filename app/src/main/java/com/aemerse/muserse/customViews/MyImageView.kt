package com.aemerse.muserse.customViews

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.aemerse.muserse.R

class MyImageView : AppCompatImageView {
    private fun getUrl(): String? {
        return url
    }

    fun setUrl(url: String?) {
        this.url = url
        Glide.with(context).load(getUrl()).into(this)
    }

    private var url: String? = null

    internal constructor(context: Context?) : super((context)!!)
    internal constructor(context: Context, attr: AttributeSet?) : super(context, attr) {
        val array: TypedArray = context.obtainStyledAttributes(attr, R.styleable.MyImageView)
        url = array.getString(R.styleable.MyImageView_url)
        if (url != null) {
            Glide.with(context).load(url).into(this)
        }
        array.recycle()
    }
}