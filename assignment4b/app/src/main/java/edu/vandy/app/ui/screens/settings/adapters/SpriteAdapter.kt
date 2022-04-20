package edu.vandy.app.ui.screens.settings.adapters

import android.content.Context
import android.content.res.TypedArray
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import edu.vanderbilt.crawler.ui.screens.settings.adapters.BindingAdapter
import edu.vandy.R
import edu.vandy.app.extensions.setPadding
import edu.vandy.app.extensions.show
import edu.vandy.app.ui.adapters.dpToPx

internal class SpriteAdapter(context: Context) : BindingAdapter<Int>(context) {
    private val sprites: List<Int>

    init {
        val ids = mutableListOf(0)
        ids.addAll(getSpriteResourceIds(context))
        sprites = ids.toList()
    }

    override fun getCount(): Int {
        return sprites.size
    }

    override fun getItem(position: Int): Int? {
        return sprites[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun newView(
        inflater: LayoutInflater,
        position: Int,
        container: ViewGroup
    ): View {
        return with(container.context) {
            // Dynamic creation of list item.
            LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                orientation = LinearLayout.HORIZONTAL
                setPadding(4.dpToPx.toInt())
                (1 until count).forEach {
                    gravity = Gravity.LEFT
                    ImageView(this@with).apply {
                        setImageResource(getItem(it)!!)
                        adjustViewBounds = true
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        layoutParams = ViewGroup.LayoutParams(30.dpToPx.toInt(), 50.dpToPx.toInt())
                    }
                }
            }
        }
    }

    override fun bindView(item: Int?, position: Int, view: View) {
        with(view as LinearLayout) {
            (0 until childCount).forEach {
                if (position == 0) {
                    getChildAt(it).show = true
                } else {
                    getChildAt(it).show = it == (position - 1)
                }
            }
        }
    }

    override fun newDropDownView(
        inflater: LayoutInflater,
        position: Int,
        container: ViewGroup
    ): View {
        return newView(inflater, position, container)
    }

    fun getPositionForValue(value: Int): Int {
        return sprites.indices.first { sprites[it] == value }
    }

    companion object {
        fun getSpriteResourceIds(context: Context): List<Int> {
            val array: TypedArray = context.resources.obtainTypedArray(R.array.sprites)

            try {
                return with(array) {
                    (0 until length()).map {
                        getResourceId(it, -1)
                    }.toList()
                }
            } finally {
                array.recycle()
            }
        }
    }
}
