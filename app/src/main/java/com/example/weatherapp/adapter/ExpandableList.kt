package com.example.weatherapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView

class ExpandableList (
    private val context: Context,
    private val parentList: MutableList<String>,
    private val childList: Map<String, List<String>>
) : BaseExpandableListAdapter() {

    override fun getGroupCount(): Int = parentList.size

    override fun getChildrenCount(groupPosition: Int): Int =
        childList[parentList[groupPosition]]?.size ?: 0

    override fun getGroup(groupPosition: Int): Any = parentList[groupPosition]

    override fun getChild(groupPosition: Int, childPosition: Int): Any =
        childList[parentList[groupPosition]]?.get(childPosition) ?: ""

    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

    override fun getChildId(groupPosition: Int, childPosition: Int): Long =
        childPosition.toLong()

    override fun hasStableIds(): Boolean = false

    override fun getGroupView(
        groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?
    ): View {
        val groupText = getGroup(groupPosition) as String
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_expandable_list_item_1, parent, false)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = groupText
        return view
    }

    override fun getChildView(
        groupPosition: Int, childPosition: Int, isLastChild: Boolean,
        convertView: View?, parent: ViewGroup?
    ): View {
        val childText = getChild(groupPosition, childPosition) as String
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = childText
        return view
    }
    fun updateParentText(groupPosition: Int, newText: String) {
        parentList[groupPosition] = newText
        notifyDataSetChanged()  // Notify the adapter that data has changed
    }
    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true
}
