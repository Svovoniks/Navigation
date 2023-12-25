package com.svovo.navigation

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.svovo.navigation.databinding.ItemRouteBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DiscoverFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DiscoverFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var adapter: RouteAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_discover, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loginNotif = view.findViewById<TextView>(R.id.login_notif)

        if (MainActivity.loggedIn) {
            loginNotif.visibility = View.INVISIBLE
        }
        else {
            loginNotif.visibility = View.VISIBLE
        }

        val manager = LinearLayoutManager(context)
        val adapter = RouteAdapter()

        adapter.data = MainActivity.user?.trails ?: listOf()

        this.adapter = adapter

        MainActivity.user?.fetchTrails { adapter.notifyDataSetChanged() }

        val recyclerView = view.findViewById<RecyclerView>(R.id.routes_recycler)
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        MainActivity.user?.fetchTrails { adapter?.notifyDataSetChanged() }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment DiscoverFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            DiscoverFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun loadFragment() {
        val fragment = activity?.supportFragmentManager?.findFragmentById(R.id.nav_host_fragment_content_main)
        MainActivity.mapFlag = true
        fragment?.let {
            val transaction =  activity?.supportFragmentManager?.beginTransaction()
            transaction?.remove(it)?.commitAllowingStateLoss() ?: false
        }
    }

    inner class RouteAdapter : RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {

        var data: List<Trail> = emptyList()
            set(newValue) {
                field = newValue
                notifyDataSetChanged()
            }
        inner class RouteViewHolder(val binding: ItemRouteBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemRouteBinding.inflate(inflater, parent, false)

            return RouteViewHolder(binding)
        }

        override fun getItemCount(): Int = data.size

        override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
            val trail = data[position]
            val context = holder.itemView.context

            holder.itemView.setOnClickListener {
                MainActivity.pathManager?.buildFromTrail(trail)
                MainActivity.bottomNav?.selectedItemId = R.id.mapFragment
                MainActivity.bottomNav?.invalidate()
                loadFragment()
            }

            with(holder.binding) {
                routeNameTextview.text = trail.name
            }
        }
    }
}

