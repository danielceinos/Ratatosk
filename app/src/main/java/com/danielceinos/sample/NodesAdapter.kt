package com.danielceinos.sample

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.danielceinos.ratatosk.NodeId
import com.danielceinos.ratatosk.models.EndpointId
import com.danielceinos.ratatosk.models.Node
import com.danielceinos.sample.databinding.RowNodeBinding

class NodesAdapter(private val onNodeClick: (Node) -> Unit) : RecyclerView.Adapter<NodesAdapter.ViewHolder>() {

    private var nodes: List<Node> = emptyList()
    private var nodesPings: Map<NodeId, Long> = emptyMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            RowNodeBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun getItemCount() = nodes.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.node = nodes[position]
        holder.binding.ping = "${nodesPings[nodes[position].nodeId]} ms"
        holder.binding.root.setOnClickListener { onNodeClick(nodes[position]) }
        holder.binding.executePendingBindings()
    }

    fun setNodes(nodes: List<Node>) {
        this.nodes = nodes
        notifyDataSetChanged()
    }

    fun setPings(pings: Map<NodeId, Long>) {
        nodesPings = pings
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: RowNodeBinding) : RecyclerView.ViewHolder(binding.root)
}

