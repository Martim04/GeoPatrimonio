package com.example.projeto

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val buttonEditProfile = view.findViewById<Button>(R.id.buttonEditProfile)
        val switchNotifications = view.findViewById<SwitchMaterial>(R.id.switchNotifications)
        val buttonPrivacySettings = view.findViewById<Button>(R.id.buttonPrivacySettings)
        val spinnerLanguage = view.findViewById<Spinner>(R.id.spinnerLanguage)
        val switchTheme = view.findViewById<SwitchMaterial>(R.id.switchTheme)

        buttonEditProfile.setOnClickListener {
            // Navegar para a tela de edição de perfil
            val intent = Intent(activity, EditProfileActivity::class.java)
            startActivity(intent)
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            // Alternar notificações
            if (isChecked) {
                Toast.makeText(requireContext(), "Notificações ativadas", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Notificações desativadas", Toast.LENGTH_SHORT).show()
            }
        }

        buttonPrivacySettings.setOnClickListener {
            // Navegar para a tela de configurações de privacidade
            val intent = Intent(activity, PrivacySettingsActivity::class.java)
            startActivity(intent)
        }

        // Configurar o spinner de idioma
        val languages = arrayOf("Português", "Inglês", "Espanhol")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedLanguage = languages[position]
                Toast.makeText(requireContext(), "Idioma selecionado: $selectedLanguage", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            // Alternar entre modo claro e escuro
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Snackbar.make(view, "Modo escuro ativado", Snackbar.LENGTH_SHORT).show()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Snackbar.make(view, "Modo claro ativado", Snackbar.LENGTH_SHORT).show()
            }
        }

        return view
    }
}