package com.demo.weatherapp.app.framework

import io.realm.Realm

class RealmFactory {

    fun getRealm(): Realm = Realm.getDefaultInstance()
}