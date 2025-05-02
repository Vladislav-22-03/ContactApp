package com.example.contactapp.utils

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import java.util.ArrayList
object ContactUtils {

    @WorkerThread
    fun deleteDuplicateContacts(context: Context): String {
        // Проверка разрешения
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            return "Требуется разрешение WRITE_CONTACTS"
        }

        val contentResolver = context.contentResolver

        // 1. Получаем контакты с группировкой по дубликатам
        val duplicates = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            "${ContactsContract.CommonDataKinds.Phone.NUMBER} IS NOT NULL AND ${ContactsContract.CommonDataKinds.Phone.NUMBER} != ''",
            null,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            buildMap<String, MutableList<Long>> {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex) ?: "Unknown"
                    val phone = cursor.getString(phoneIndex)?.replace("\\D".toRegex(), "") ?: continue
                    val id = cursor.getLong(idIndex)

                    val key = "$name|$phone"
                    getOrPut(key) { mutableListOf() }.add(id)
                }
            }.filterValues { it.size > 1 }
        } ?: return "Ошибка чтения контактов"

        if (duplicates.isEmpty()) return "Дубликаты не найдены"

        // 2. Удаление дубликатов
        var deletedCount = 0
        val operations = ArrayList<ContentProviderOperation>()

        duplicates.values.forEach { ids ->
            ids.drop(1).forEach { id ->
                operations.add(
                    ContentProviderOperation.newDelete(
                        ContactsContract.RawContacts.CONTENT_URI.buildUpon()
                            .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                            .build()
                    )
                        .withSelection("${ContactsContract.RawContacts.CONTACT_ID} = ?", arrayOf(id.toString()))
                        .build()
                )
            }
        }

        return try {
            val results = contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            deletedCount = results.count { it.count?.let { cnt -> cnt > 0 } ?: false }
            "Удалено дубликатов: $deletedCount"
        } catch (e: Exception) {
            Log.e("ContactUtils", "Ошибка удаления", e)
            "Дубликаты найдены, но не удалены (${e.message})"
        }
    }
}