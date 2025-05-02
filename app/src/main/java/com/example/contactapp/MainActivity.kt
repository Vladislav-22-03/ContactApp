package com.example.contactapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.contactapp.model.Contact
import com.example.contactapp.utils.ContactUtils

class MainActivity : AppCompatActivity() {

    private lateinit var contactAdapter: ContactAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.contactsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val deleteButton = findViewById<Button>(R.id.deleteDuplicatesButton)
        deleteButton.setOnClickListener {
            val result = ContactUtils.deleteDuplicateContacts(this)
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
            loadContacts()
        }

        val permissions = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )

        if (!permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, 100)
        } else {
            loadContacts()
        }
    }

    private fun loadContacts() {
        val contacts = getContacts()
        contactAdapter = ContactAdapter(contacts)
        recyclerView.adapter = contactAdapter
    }

    private fun getContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone._ID, // Добавляем ID
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex) ?: "Unknown"
                val phone = it.getString(phoneIndex) ?: ""

                contacts.add(Contact(
                    id = id,
                    name = name,
                    phone = phone
                ))
            }
        }

        return contacts
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            loadContacts()
        } else {
            Toast.makeText(this, "Требуются разрешения для чтения и удаления контактов", Toast.LENGTH_LONG).show()
        }
    }
}
