package com.example.weatherapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.weatherapp.databinding.AlarmScreenBinding
import com.example.weatherapp.databinding.FragmentMapsBinding
import com.example.weatherapp.mvvm.ViewModelFactory
import com.example.weatherapp.mvvm.WeatherVM
import com.example.weatherapp.service.RetrofitClient
import com.example.weatherapp.weatherRepository.WeatherRepository
import org.osmdroid.config.Configuration


class AlarmFragment : Fragment() {


    private lateinit var binding: AlarmScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = requireContext().packageName

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = AlarmScreenBinding.inflate(inflater, container, false)
        return binding.root

    }

}