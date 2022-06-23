package com.example.simpletextview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.simpletextview.ExampleType.*

class ExampleFragment : Fragment() {

    companion object {
        fun newInstance(type: ExampleType): Fragment =
            ExampleFragment().apply {
                arguments = Bundle().also {
                    it.putSerializable("type", type)
                }
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.example_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val type = requireArguments().getSerializable("type") as ExampleType
        view.findViewById<AppCompatTextView>(R.id.title).text = type.title
        view.findViewById<RecyclerView>(R.id.recycler_view).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = Adapter(type)
        }
    }
}

enum class ExampleType(val title: String) {
    LINEAR_APP_COMPAT("Linear - ACTextView"),
    LINEAR_SIMPLE("Linear - ACTextView"),
    RELATIVE_APP_COMPAT("Relative - ACTextView"),
    RELATIVE_SIMPLE("Relative - SimpleTextView"),
    CONSTRAINT_APP_COMPAT("Constraint - ACTextView"),
    CONSTRAINT_SIMPLE("Constraint - SimpleTextView")
}

class Adapter(private val type: ExampleType) : RecyclerView.Adapter<AnyViewHolder>() {

    private val items = mutableListOf<Int>().apply { repeat(200, ::add) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnyViewHolder =
        AnyViewHolder(getView(parent))

    private fun getView(parent: ViewGroup): View {
        val inflater = LayoutInflater.from(parent.context)
        return when (type) {
            LINEAR_APP_COMPAT -> inflater.inflate(R.layout.item_linear_app_compat, parent, false)
            LINEAR_SIMPLE -> inflater.inflate(R.layout.item_linear_simple, parent, false)
            RELATIVE_APP_COMPAT -> inflater.inflate(R.layout.item_relative_app_compat, parent, false)
            RELATIVE_SIMPLE -> inflater.inflate(R.layout.item_relative_simple, parent, false)
            CONSTRAINT_APP_COMPAT -> inflater.inflate(R.layout.item_constraint_app_compat, parent, false)
            CONSTRAINT_SIMPLE -> inflater.inflate(R.layout.item_constraint_simple, parent, false)
        }
    }

    override fun onBindViewHolder(holder: AnyViewHolder, position: Int) = Unit

    override fun getItemCount(): Int = items.size
}

class AnyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)