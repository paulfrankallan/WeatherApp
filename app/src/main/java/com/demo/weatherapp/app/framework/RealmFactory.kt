package com.demo.weatherapp.app.framework

import io.realm.Realm

open class RealmFactory {

    open fun getRealm(): Realm = Realm.getDefaultInstance()
}