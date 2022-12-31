package edu.mailman.roomdemo

import android.app.Application

class EmployeeApp : Application() {
    val db by lazy {
        EmployeeDatabase.getInstance(this)
    }
}