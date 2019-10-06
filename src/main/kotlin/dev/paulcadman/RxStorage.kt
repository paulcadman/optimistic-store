package dev.paulcadman

import arrow.fx.ForIO
import arrow.fx.IO
import arrow.fx.extensions.io.async.async
import arrow.fx.fix
import arrow.fx.rx2.*
import arrow.fx.rx2.extensions.maybek.async.async
import arrow.fx.rx2.extensions.observablek.async.async
import io.reactivex.Maybe
import io.reactivex.Observable

val rxStorage = OptimisticStorage<ForObservableK, String>(InMemoryStorage(), ReadWriteStorageLock())

val maybeStorage = OptimisticStorage<ForMaybeK, String>(InMemoryStorage(), ReadWriteStorageLock())
val ioStorage = OptimisticStorage<ForIO, String>(InMemoryStorage(), ReadWriteStorageLock())

fun setString(s: String): Observable<StoreId> = rxStorage.run {
    ObservableK.async().run {
        new(s)
    }
}.value()

fun setStringMaybe(s: String): Maybe<StoreId> = maybeStorage.run {
    MaybeK.async().run {
        new(s)
    }
}.value()

fun setStringIO(s: String): IO<StoreId> = ioStorage.run {
    IO.async().run {
        new(s)
    }
}.fix()