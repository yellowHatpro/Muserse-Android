package com.aemerse.muserse.lyricCard

internal class QueryBuilder constructor(private var mFamilyName: String) {
    private var mWidth: Float? = null
    private var mWeight: Int? = null
    private var mItalic: Float? = null
    private var mBesteffort: Boolean? = null

    fun withFamilyName(familyName: String): QueryBuilder {
        mFamilyName = familyName
        return this
    }

    fun withWidth(width: Float): QueryBuilder {
        if (width <= Constants.WIDTH_MIN) {
            throw IllegalArgumentException("Width must be more than 0")
        }
        mWidth = width
        return this
    }

    fun withWeight(weight: Int): QueryBuilder {
        if (weight <= Constants.WEIGHT_MIN || weight >= Constants.WEIGHT_MAX) {
            throw IllegalArgumentException(
                "Weight must be between 0 and 1000 (exclusive)")
        }
        mWeight = weight
        return this
    }

    fun withItalic(italic: Float): QueryBuilder {
        if (italic < Constants.ITALIC_MIN || italic > Constants.ITALIC_MAX) {
            throw IllegalArgumentException("Italic must be between 0 and 1 (inclusive)")
        }
        mItalic = italic
        return this
    }

    fun withBestEffort(bestEffort: Boolean): QueryBuilder {
        mBesteffort = bestEffort
        return this
    }

    fun build(): String {
        if ((mWeight == null) && (mWidth == null) && (mItalic == null) && (mBesteffort == null)) {
            return mFamilyName
        }
        val builder: StringBuilder = StringBuilder()
        builder.append("name=").append(mFamilyName)
        if (mWeight != null) {
            builder.append("&weight=").append(mWeight)
        }
        if (mWidth != null) {
            builder.append("&width=").append(mWidth)
        }
        if (mItalic != null) {
            builder.append("&italic=").append(mItalic)
        }
        if (mBesteffort != null) {
            builder.append("&besteffort=").append(mBesteffort)
        }
        return builder.toString()
    }
}