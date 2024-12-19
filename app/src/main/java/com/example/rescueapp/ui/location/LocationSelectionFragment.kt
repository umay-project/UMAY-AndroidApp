package com.example.rescueapp.ui.location

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.rescueapp.R

class LocationSelectionFragment : Fragment() {

    private lateinit var citySpinner: Spinner
    private lateinit var districtSpinner: Spinner
    private lateinit var neighborhoodSpinner: Spinner
    private lateinit var submitButton: Button

    private val cities = listOf("Istanbul", "Ankara", "Izmir", "Bursa") // Example cities
    private val districts = mapOf(
        "Istanbul" to listOf("Kadikoy", "Besiktas", "Uskudar"),
        "Ankara" to listOf("Cankaya", "Kecioren", "Altindag"),
        "Izmir" to listOf("Konak", "Bornova", "Buca"),
        "Bursa" to listOf("Osmangazi", "Nilufer", "Yildirim")
    )
    private val neighborhoods = mapOf(
        "Kadikoy" to listOf("Moda", "Fenerbahce", "Goztepe"),
        "Besiktas" to listOf("Levent", "Etiler", "Bebek"),
        "Uskudar" to listOf("Altunizade", "Cengelkoy", "Camlica")
    )

    private var selectedCity: String? = null
    private var selectedDistrict: String? = null
    private var selectedNeighborhood: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_location_selection, container, false)

        citySpinner = rootView.findViewById(R.id.citySpinner)
        districtSpinner = rootView.findViewById(R.id.districtSpinner)
        neighborhoodSpinner = rootView.findViewById(R.id.neighborhoodSpinner)
        submitButton = rootView.findViewById(R.id.submitButton)

        setupCitySpinner()

        submitButton.setOnClickListener {
            if (selectedCity != null && selectedDistrict != null && selectedNeighborhood != null) {
                val fullAddress = "$selectedNeighborhood, $selectedDistrict, $selectedCity"
                sendToGoogleMapsAPI(fullAddress)
            } else {
                Toast.makeText(requireContext(), "Please select all fields", Toast.LENGTH_SHORT).show()
            }
        }

        return rootView
    }

    private fun setupCitySpinner() {
        val cityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cities)
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        citySpinner.adapter = cityAdapter

        citySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCity = cities[position]
                setupDistrictSpinner(selectedCity!!)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedCity = null
            }
        }
    }

    private fun setupDistrictSpinner(city: String) {
        val districtList = districts[city] ?: emptyList()
        val districtAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, districtList)
        districtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        districtSpinner.adapter = districtAdapter

        districtSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedDistrict = districtList[position]
                setupNeighborhoodSpinner(selectedDistrict!!)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedDistrict = null
            }
        }
    }

    private fun setupNeighborhoodSpinner(district: String) {
        val neighborhoodList = neighborhoods[district] ?: emptyList()
        val neighborhoodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, neighborhoodList)
        neighborhoodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        neighborhoodSpinner.adapter = neighborhoodAdapter

        neighborhoodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedNeighborhood = neighborhoodList[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedNeighborhood = null
            }
        }
    }

    private fun sendToGoogleMapsAPI(address: String) {

        Toast.makeText(requireContext(), "Selected: $address", Toast.LENGTH_SHORT).show()
    }
}
