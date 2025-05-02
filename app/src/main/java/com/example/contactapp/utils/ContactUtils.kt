package com.example.contactapp.utils

import android.content.Context
import android.provider.ContactsContract
import android.util.Log

object ContactUtils {

    fun deleteDuplicateContacts(context: Context): String {
        val contentResolver = context.contentResolver
        val contactMap = mutableMapOf<String, MutableList<String>>() // key: "name|phone", value: list of IDs

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
            ),
            null, null, null
        )

        if (cursor != null && cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)

            do {
                val name = cursor.getString(nameIndex) ?: continue
                val phone = cursor.getString(phoneIndex)?.replace("\\s|-".toRegex(), "") ?: continue
                val id = cursor.getString(idIndex) ?: continue

                val key = "$name|$phone"
                if (contactMap.containsKey(key)) {
                    contactMap[key]?.add(id)
                } else {
                    contactMap[key] = mutableListOf(id)
                }
            } while (cursor.moveToNext())

            cursor.close()
        }

        var deletedCount = 0

        // Удаляем все дубликаты (оставляем по одному)
        for ((_, ids) in contactMap) {
            if (ids.size > 1) {
                for (i in 1 until ids.size) { // оставляем первый, остальные удаляем
                    val contactUri = ContactsContract.RawContacts.CONTENT_URI
                        .buildUpon()
                        .appendQueryParameter(
                            ContactsContract.CALLER_IS_SYNCADAPTER,
                            "true"
                        ).build()

                    val rows = contentResolver.delete(
                        ContactsContract.RawContacts.CONTENT_URI,
                        ContactsContract.RawContacts.CONTACT_ID + "=?",
                        arrayOf(ids[i])
                    )

                    if (rows > 0) deletedCount++
                }
            }
        }

        return if (deletedCount > 0) "Удалено дубликатов: $deletedCount" else "Дубликаты не найдены"
    }
}
