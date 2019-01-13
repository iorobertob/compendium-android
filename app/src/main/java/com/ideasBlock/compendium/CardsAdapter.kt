package com.ideasBlock.compendium

import android.content.Context
import android.os.Build
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.single_card.view.*
import org.json.JSONArray

open class CardsAdapter(
    val context: Context,
    listener: ListItemClickListener) : RecyclerView.Adapter<CardsAdapter.ViewHolder>() {

    // Where the data will be put
    private var mJsonArray:JSONArray = JSONArray()

    /*
     * An on-click handler for an Activity to interface with our RecyclerView
     */
    open val mOnClickListener: ListItemClickListener? = listener

    /**
     * Add an interface called ListItemClickListener
     * Within that interface, define a void method called onListItemClick that takes an int as a parameter
     * The interface that receives onClick messages.
     */
    interface ListItemClickListener {
        fun onListItemClick(clickedItemIndex: Int)
    }

    /**
     * Inflates the item views
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.single_card, parent, false))
    }

    /**
     * Binds each card in the data to a view
     */
    override fun onBindViewHolder(parent: ViewHolder, position: Int)
    {
        val userArray = JSONArray(mJsonArray[position].toString())

        val name = userArray[1].toString()
        parent.tvName.text = name

        var description = userArray[12].toString()
        if (description == "null" ){
            description = "Description"
        }

        parent.tvDescription.text = description

        val url = userArray[15].toString()
        Picasso.with(context)
            .load(url)
            .into(parent.itemView.iv_cardpic)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            parent.itemView.iv_cardpic.elevation = 30f
        }
    }

    /**
     * Gets the number of items in the list
     */
    override fun getItemCount(): Int {
        return mJsonArray.length()
    }

    /**
     * When data changes and a re-query occurs, this function swaps the old JsonArray
     * with a newly updated one. This applies for teh search query Activity
     * @param inboundArray    The JSONArray coming from the Activity this Recycler is bound to.
     * @return                  the previous JSONArray, as temp.
     */
    fun swapJSON(inboundArray: JSONArray): JSONArray {

        // check if this array is the same as the previous cursor (mJsonArray)
        if (mJsonArray === inboundArray) {
            return JSONArray() // bc nothing has changed
        }

        this.mJsonArray = inboundArray // new cursor value assigned

        //check if this is a valid cursor, then update the cursor
        this.notifyDataSetChanged()

        return inboundArray
    }

    /**
     * This inner class is the view that holds the objects(views) of each item in the recycle adapter
     */
    inner class ViewHolder (view: View) : RecyclerView.ViewHolder(view), View.OnClickListener
    {
        init {
            view.setOnClickListener(this)
        }

        override fun onClick(view: View?)
        {
            val clickedPosition = adapterPosition
            mOnClickListener?.onListItemClick(clickedPosition)
        }

        // Holds the TextView that will add each animal to
        val tvName: TextView = view.tv_name
        val tvDescription: TextView = view.tv_desciption
    }
}


