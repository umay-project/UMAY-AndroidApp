package com.example.rescueapp.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rescueapp.R
import com.example.rescueapp.ui.controller.DebrisSiteAdapter
import com.example.rescueapp.ui.controller.DebrisSiteApi
import com.example.rescueapp.ui.models.DebrisSite
import okhttp3.OkHttpClient
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class DashboardFragment : Fragment() {

    private lateinit var debrisRecyclerView: RecyclerView
    private lateinit var debrisAdapter: DebrisSiteAdapter
    private lateinit var dashboardViewModel: DashboardViewModel
    private val debrisSites = mutableListOf<DebrisSite>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_dashboard, container, false)

        debrisRecyclerView = rootView.findViewById(R.id.debrisRecyclerView)
        debrisRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        debrisAdapter = DebrisSiteAdapter(debrisSites)
        debrisRecyclerView.adapter = debrisAdapter

        dashboardViewModel = ViewModelProvider(requireActivity())[DashboardViewModel::class.java]
        dashboardViewModel.bounds.observe(viewLifecycleOwner) { bounds ->
            fetchDebrisSites(
                convertToDDDMM(bounds.minLat),
                convertToDDDMM(bounds.maxLat),
                convertToDDDMM(bounds.minLong),
                convertToDDDMM(bounds.maxLong)
            )
        }

        return rootView
    }

    private fun fetchDebrisSites(minLat: Int, maxLat: Int, minLong: Int, maxLong: Int) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://umay.develop-er.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient())
            .build()

        val api = retrofit.create(DebrisSiteApi::class.java)
        api.getDebrisSites(minLat, maxLat, minLong, maxLong).enqueue(object : Callback<List<DebrisSite>> {
            override fun onResponse(call: Call<List<DebrisSite>>, response: Response<List<DebrisSite>>) {
                if (response.isSuccessful) {
                    debrisSites.clear()
                    debrisSites.addAll(response.body() ?: emptyList())
                    debrisAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show()
                    Log.e("DashboardFragment", "Response error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<List<DebrisSite>>, t: Throwable) {
                Log.e("DashboardFragment", "Error fetching debris sites", t)
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun convertToDDDMM(coordinate: Double): Int {
        val degrees = coordinate.toInt()
        val minutes = ((coordinate - degrees) * 60).toInt()
        return degrees * 100 + minutes
    }
}
