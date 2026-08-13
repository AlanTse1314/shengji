package com.shengji.app

import android.app.Application
import com.shengji.app.db.AppDatabase

class ShengJiApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
