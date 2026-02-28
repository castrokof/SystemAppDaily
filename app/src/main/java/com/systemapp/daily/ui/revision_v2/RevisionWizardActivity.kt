package com.systemapp.daily.ui.revision_v2

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.systemapp.daily.R
import com.systemapp.daily.databinding.ActivityRevisionWizardBinding
import com.systemapp.daily.utils.Constants

class RevisionWizardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRevisionWizardBinding
    val viewModel: RevisionWizardViewModel by viewModels()

    private val stepNames = listOf("1.Registro", "2.Predio", "3.Familia", "4.Medidor", "5.Fin")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRevisionWizardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val idOrden = intent.getIntExtra(Constants.EXTRA_ORDEN_ID, -1)
        if (idOrden == -1) {
            Toast.makeText(this, "Orden no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.cargarOrden(idOrden)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Revisión"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        viewModel.orden.observe(this) { orden ->
            if (orden != null) {
                supportActionBar?.subtitle = "Predio: ${orden.codigoPredio}"
            }
        }

        // Setup ViewPager2 - disable swipe, navigate with tabs or buttons
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 5
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> Step1RegistroFragment()
                1 -> Step2PredioFragment()
                2 -> Step3FamiliaFragment()
                3 -> Step4MedidorFragment()
                else -> Step5FinalizarFragment()
            }
        }

        // Setup TabLayout con los nombres de pasos
        for (name in stepNames) {
            binding.tabLayoutSteps.addTab(binding.tabLayoutSteps.newTab().setText(name))
        }

        // Click en tab navega al paso
        binding.tabLayoutSteps.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.viewPager.currentItem = tab.position
                updateStepUI(tab.position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        updateStepUI(0)

        binding.btnAnterior.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current > 0) {
                binding.viewPager.currentItem = current - 1
                binding.tabLayoutSteps.selectTab(binding.tabLayoutSteps.getTabAt(current - 1))
                updateStepUI(current - 1)
            }
        }

        binding.btnSiguiente.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < 4) {
                binding.viewPager.currentItem = current + 1
                binding.tabLayoutSteps.selectTab(binding.tabLayoutSteps.getTabAt(current + 1))
                updateStepUI(current + 1)
            } else {
                // Step 5: Finalizar
                viewModel.finalizar()
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.btnSiguiente.isEnabled = !loading
            binding.btnAnterior.isEnabled = !loading
        }

        viewModel.saveResult.observe(this) { result ->
            if (result == null) return@observe
            when (result) {
                is RevisionWizardViewModel.SaveResult.Success -> {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(getColor(R.color.success))
                        .show()
                    binding.root.postDelayed({ finish() }, 1500)
                }
                is RevisionWizardViewModel.SaveResult.SavedLocal -> {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getColor(R.color.warning))
                        .show()
                    binding.root.postDelayed({ finish() }, 2000)
                }
                is RevisionWizardViewModel.SaveResult.Error -> {
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getColor(R.color.error))
                        .show()
                }
            }
        }
    }

    private fun updateStepUI(step: Int) {
        binding.progressSteps.progress = step + 1
        binding.btnAnterior.visibility = if (step == 0) View.INVISIBLE else View.VISIBLE
        binding.btnSiguiente.text = if (step == 4) "Finalizar" else "Siguiente"
    }
}
