package dev.paulcadman

import arrow.Kind
import arrow.core.Option
import arrow.core.getOption
import arrow.core.getOrElse
import arrow.core.right
import arrow.fx.typeclasses.Async
import arrow.typeclasses.Const
import arrow.typeclasses.MonadThrow
import arrow.typeclasses.const
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

typealias StoreId = Const<UUID, StoreIdT>

class StoreIdT private constructor()

typealias Version = Const<Int, VersionT>

val zero: Version = 0.const()

class VersionT private constructor()

data class VersionedData<A>(var version: Version, var data: A)

interface Storage<F, A> {
    fun Async<F>.get(id: StoreId): Kind<F, Option<VersionedData<A>>>

    fun Async<F>.put(data: A): Kind<F, StoreId>
}

sealed class SwapResult {
    object StaleVersion : SwapResult()
    object Missing : SwapResult()
    class NewVersion(var version: Version) : SwapResult()
}

interface StorageLock<F> {
    fun <T> Async<F>.readLock(action: () -> T): Kind<F, T>
    fun <T> Async<F>.writeLock(action: () -> T): Kind<F, T>
}

class OptimisticStorage<F, A>(var storage: Storage<F, A>, var lock: StorageLock<F>) {
    fun Async<F>.read(id: StoreId): Kind<F, Option<VersionedData<A>>> =
        lock.run { storage.run { readLock { get(id) }.flatten() } }

    fun Async<F>.new(data: A): Kind<F, StoreId> = lock.run { storage.run {
        writeLock {
            put(data)
        }.flatten()
    }
    }

    fun Async<F>.checkAndSwap(id: StoreId, version: Version, data: A): Kind<F, SwapResult> {
        return lock.run {
            storage.run {
                fx.async {
                    readLock { get(id) }.flatten().bind().fold(
                        { SwapResult.Missing },
                        { (currentVersion, _) ->
                            if (version.value() < currentVersion.value()) {
                                SwapResult.StaleVersion
                            } else {
                                writeLock { put(data) }.flatten()
                                    .map { SwapResult.NewVersion(currentVersion.value().inc().const()) }.bind()
                            }
                        }
                    )
                }
            }
        }
    }
}

class ReadWriteStorageLock<F> : StorageLock<F> {
    private val lock = ReentrantReadWriteLock()

    override fun <T> Async<F>.readLock(action: () -> T): Kind<F, T> = async { cb ->
        cb(lock.read(action).right())
    }

    override fun <T> Async<F>.writeLock(action: () -> T): Kind<F, T> = async { cb ->
        cb(lock.write(action).right())
    }
}

class InMemoryStorage<F, A> : Storage<F, A> {
    private val store: MutableMap<StoreId, VersionedData<A>> = mutableMapOf()


    override fun Async<F>.get(id: StoreId): Kind<F, Option<VersionedData<A>>> {
        return store.getOption(id).just()
    }

    override fun Async<F>.put(data: A): Kind<F, StoreId> {
        val storeId: StoreId = UUID.randomUUID().const()
        store[storeId] = VersionedData(zero, data)
        return storeId.just()
    }

}
