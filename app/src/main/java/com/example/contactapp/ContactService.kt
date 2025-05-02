package com.example.contactapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.contactapp.utils.ContactUtils  // Убедитесь, что утилита для контактов подключена

class ContactService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Здесь можно вызвать метод, который удалит дубликаты
        val result = ContactUtils.deleteDuplicateContacts(this)

        // Отправим результат обратно
        val resultIntent = Intent("com.example.contactapp.DELETE_STATUS")
        resultIntent.putExtra("result", result)
        sendBroadcast(resultIntent)

        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
