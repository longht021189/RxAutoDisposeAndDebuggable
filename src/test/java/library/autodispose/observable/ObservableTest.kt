package library.autodispose.observable

import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import io.reactivex.subjects.BehaviorSubject
import library.autodispose.State
import library.autodispose.autoDispose
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class ObservableTest {

    companion object {
        private const val NO_RESULT = -1
        private const val RESULT_VALUE = 1
        private const val RESULT_VALUE_2 = 2
    }

    @Test
    fun testDelaySubscription() {
        val emitterRef = AtomicReference<ObservableEmitter<Int>?>(null)
        val observable = Observable.create { v: ObservableEmitter<Int>? -> emitterRef.set(v) }
        val emitterRef2 = AtomicReference<ObservableEmitter<Int>?>(null)
        val observable2 = Observable.create { v: ObservableEmitter<Int>? -> emitterRef2.set(v) }
        val observable3 = observable.delaySubscription(observable2)
        val disposable = observable3.subscribe()

        emitterRef.get()?.let {
            println("emitterRef != null")
        } ?: println("emitterRef == null")
        emitterRef2.get()?.let { emitter2 ->
            println("emitterDelayRef != null")
            emitter2.onNext(0)

            emitterRef.get()?.let { emitter1 ->
                println("[onNext] emitterRef != null")
                emitter1.onComplete()

                if (emitter2.isDisposed) {
                    println("[onComplete] emitterDelayRef isDisposed = true")
                } else {
                    println("[onComplete] emitterDelayRef isDisposed = false")
                }
                if (emitter1.isDisposed) {
                    println("[onComplete] emitterRef isDisposed = true")
                } else {
                    println("[onComplete] emitterRef isDisposed = false")
                }
                if (disposable.isDisposed) {
                    println("[onComplete] disposable isDisposed = true")
                } else {
                    println("[onComplete] disposable isDisposed = false")
                }
            } ?: println("[onNext] emitterRef == null")
        } ?: println("emitterDelayRef == null")

        if (!disposable.isDisposed) {
            disposable.dispose()
        }

        try {
            observable3.subscribe().dispose()
            println("isSubscribedSuccess = false")
        } catch (error: Throwable) {
            println(error.toString())
            println("isSubscribedSuccess = true")
        }
    }

    @Test
    fun create() {
        val emitterRef = AtomicReference<ObservableEmitter<Int>?>(null)
        val controller = BehaviorSubject.create<State>()
        val result = AtomicInteger(NO_RESULT)
        val observable = Observable.create { v: ObservableEmitter<Int>? -> emitterRef.set(v) }
        val disposable = observable.autoDispose(controller).subscribe(Consumer { i: Int? -> result.set(i!!) })

        Assert.assertNull("It's before Created => Emitter isn't Created", emitterRef.get())

        controller.onNext(State.Created)
        Assert.assertNotNull("It's Created => Emitter is Created", emitterRef.get())

        emitterRef.get()!!.onNext(RESULT_VALUE)
        Assert.assertEquals("It's Created => Consumer doesn't receive data", result.get().toLong(), NO_RESULT.toLong())

        disposable.dispose()
        Assert.assertTrue("It's Created and Dispose Manual", emitterRef.get()!!.isDisposed)
    }

    @Test
    fun resume() {
        val emitterRef = AtomicReference<ObservableEmitter<Int>?>(null)
        val controller = BehaviorSubject.create<State>()
        val result = AtomicInteger(NO_RESULT)
        val observable = Observable.create { v: ObservableEmitter<Int>? -> emitterRef.set(v) }
        val disposable = observable.autoDispose(controller).subscribe(Consumer { i: Int? -> result.set(i!!) })

        controller.onNext(State.Created)
        emitterRef.get()!!.onNext(RESULT_VALUE)
        Assert.assertEquals("It's Created => Consumer doesn't receive data", result.get().toLong(), NO_RESULT.toLong())

        controller.onNext(State.Resumed)
        Assert.assertEquals("It's Resumed => Consumer receive data", result.get().toLong(), RESULT_VALUE.toLong())

        disposable.dispose()
        Assert.assertTrue("It's Resumed and Dispose Manual", emitterRef.get()!!.isDisposed)
    }

    @Test
    fun pause() {
        val emitterRef = AtomicReference<ObservableEmitter<Int>?>(null)
        val controller = BehaviorSubject.create<State>()
        val result = AtomicInteger(NO_RESULT)
        val observable = Observable.create { v: ObservableEmitter<Int>? -> emitterRef.set(v) }
        val disposable = observable.autoDispose(controller).subscribe(Consumer { i: Int? -> result.set(i!!) })

        controller.onNext(State.Created)
        controller.onNext(State.Resumed)
        emitterRef.get()!!.onNext(RESULT_VALUE)
        Assert.assertEquals("It's Resumed => Consumer receive data", result.get().toLong(), RESULT_VALUE.toLong())

        controller.onNext(State.Paused)
        emitterRef.get()!!.onNext(RESULT_VALUE_2)
        Assert.assertEquals("It's Paused => Consumer doesn't receive data", result.get().toLong(), RESULT_VALUE.toLong())

        controller.onNext(State.Resumed)
        Assert.assertEquals("It's Resumed => Consumer receive next data", result.get().toLong(), RESULT_VALUE_2.toLong())

        disposable.dispose()
        Assert.assertTrue("It's Resumed and Dispose Manual", emitterRef.get()!!.isDisposed)
    }

    @Test
    fun destroy() {
        val emitterRef = AtomicReference<ObservableEmitter<Int>?>(null)
        val controller = BehaviorSubject.create<State>()
        val result = AtomicInteger(NO_RESULT)
        val observable = Observable.create { v: ObservableEmitter<Int>? -> emitterRef.set(v) }
        val disposable = observable.autoDispose(controller).subscribe(Consumer { i: Int? -> result.set(i!!) })

        controller.onNext(State.Created)
        controller.onNext(State.Resumed)
        emitterRef.get()!!.onNext(RESULT_VALUE)
        controller.onNext(State.Destroyed)
        Assert.assertTrue("It's Destroyed ", emitterRef.get()!!.isDisposed)
        Assert.assertTrue("It's Destroyed ", disposable.isDisposed)

        val disposable2 = Observable.never<Any>().autoDispose(controller).subscribe(Consumer { o: Any? -> })
        Assert.assertTrue("It's Destroyed ", disposable2.isDisposed)
    }

    @Test
    fun complete() {
        val emitterRef = AtomicReference<ObservableEmitter<Int>?>(null)
        val controller = BehaviorSubject.create<State>()
        val result = AtomicInteger(NO_RESULT)
        val error = AtomicReference<Throwable?>()
        val isComplete = AtomicBoolean(false)
        val observable = Observable.create { v: ObservableEmitter<Int>? -> emitterRef.set(v) }
        val disposable = observable.autoDispose(controller).subscribe(Consumer { i: Int? -> result.set(i!!) }, Consumer { v: Throwable? -> error.set(v) }, Action {
                    println("Completed")
                    isComplete.set(true)
                })

        controller.onNext(State.Created)
        emitterRef.get()!!.onComplete()

        try {
            Thread.sleep(200L)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        Assert.assertTrue("Completed", emitterRef.get()!!.isDisposed)
        Assert.assertTrue("Completed", isComplete.get())
        Assert.assertTrue("Completed", disposable.isDisposed)
    }

    @Test
    fun error() {
        val emitterRef = AtomicReference<ObservableEmitter<Int>?>(null)
        val errorRef = AtomicReference<Throwable?>(null)
        val controller = BehaviorSubject.create<State>()
        val result = AtomicInteger(NO_RESULT)
        val observable = Observable.create { v: ObservableEmitter<Int>? -> emitterRef.set(v) }
        val disposable = observable.autoDispose(controller).subscribe(Consumer { i: Int? -> result.set(i!!) }, Consumer { v: Throwable? -> errorRef.set(v) })

        controller.onNext(State.Created)

        val exception: Exception = RuntimeException()
        val stackTraceElements = exception.stackTrace

        emitterRef.get()!!.onError(exception)

        Assert.assertTrue("It's Error", emitterRef.get()!!.isDisposed)
        Assert.assertTrue("It's Error", disposable.isDisposed)
        Assert.assertNotNull("It's Error", errorRef.get())
        Assert.assertNotEquals("It's Error", errorRef.get()!!.stackTrace, stackTraceElements)

        errorRef.get()!!.printStackTrace(System.out)
    }
}