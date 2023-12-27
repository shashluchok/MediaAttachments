package ru.scheduled.mediaattachmentslibrary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

fun <T : ViewBinding, ITEM> RecyclerView.setup(
    items: List<ITEM>,
    bindingClass: (LayoutInflater, ViewGroup, Boolean) -> T,
    manager: RecyclerView.LayoutManager = LinearLayoutManager(this.context),
    onBind: (item: ITEM, binding: T) -> Unit
): BaseAdapter<T, ITEM> {
    val baseAdapter by lazy {
        BaseAdapter(bindingClass, onBind)
    }
    baseAdapter.setItems(items)
    layoutManager = manager
    adapter = baseAdapter
    return baseAdapter
}

class BaseAdapter<T : ViewBinding, ITEM> constructor(
    bindingClass: (LayoutInflater, ViewGroup, Boolean) -> T,
    override val onBind: (item: ITEM, binding: T) -> Unit
) : AbstractAdapter<T, ITEM>(bindingClass)

abstract class AbstractAdapter<T : ViewBinding, ITEM> constructor(
    private val bindingClass: (LayoutInflater, ViewGroup, Boolean) -> T
) : RecyclerView.Adapter<AbstractAdapter.Holder<T>>() {

    abstract val onBind: (item: ITEM, binding: T) -> Unit

    private val items = mutableListOf<ITEM>()

    fun setItems(itemsList: List<ITEM>) {
        val gameDiffUtilCallback = DiffUtilCallback(
            items,
            itemsList
        )
        val diffCallback = DiffUtil.calculateDiff(gameDiffUtilCallback)
        items.clear()
        items.addAll(itemsList)
        updateAdapterWithDiffResult(diffCallback)
    }

    class Holder<V : ViewBinding>(val binding: V) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount(): Int = items.size
    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder<T> {
        val viewBinding = parent.viewBinding(bindingClass)
        return Holder(viewBinding)
    }

    override fun onBindViewHolder(holder: Holder<T>, position: Int) {
        onBind(items[position], holder.binding)
    }

    private fun updateAdapterWithDiffResult(result: DiffUtil.DiffResult) {
        result.dispatchUpdatesTo(this)
    }

    private fun calculateDiff(newItems: List<ITEM>): DiffUtil.DiffResult {
        return DiffUtil.calculateDiff(DiffUtilCallback(items, newItems))
    }

    private fun update(items: List<ITEM>) {
        updateAdapterWithDiffResult(calculateDiff(items))
    }


}

private inline fun <T : ViewBinding> ViewGroup.viewBinding(binding: (LayoutInflater, ViewGroup, Boolean) -> T): T {
    return binding(LayoutInflater.from(context), this, false)
}

private class DiffUtilCallback<ITEM>(
    private val oldItems: List<ITEM>,
    private val newItems: List<ITEM>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldItems.size
    override fun getNewListSize(): Int = newItems.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition] == newItems[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}