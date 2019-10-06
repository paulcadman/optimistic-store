package dev.paulcadman

import arrow.Kind
import arrow.core.right
import arrow.fx.ForIO
import arrow.fx.typeclasses.Async
import io.reactivex.Maybe
import org.junit.Test

import org.junit.Assert.*

class RxStorageKtTest {

    @Test
    fun getMaybeStorageGetsAStoreId() {

        val ioStorage = OptimisticStorage<ForIO, String>(InMemoryStorage(), TestStorageLock())

        setStringIO("abc").attempt().uns
    }
}

class TestStorageLock<F> : StorageLock<F> {
    var unlockWrite: () -> Unit = {}

    override fun <T> Async<F>.readLock(action: () -> T): Kind<F, T> = action().just()

    override fun <T> Async<F>.writeLock(action: () -> T): Kind<F, T> = async { cb ->
        val result = action()
        unlockWrite = { cb(result.right()) }
    }
}