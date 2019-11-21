package library.autodispose

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import library.autodispose.observable.ObservableProxy

fun <T> Observable<T>.autoDispose(state: Observable<State>): ObservableProxy<T> {
    return this.`as`(AutoDispose.autoDispose(state))
}

fun <T> ObservableProxy<T>.subscribe(
        onNext: Consumer<T>? = null,
        onError: Consumer<Throwable>? = null,
        onComplete: Action? = null,
        onSubscribe: Consumer<in Disposable>? = null
): Disposable  {
    return subscribeActual(onNext, onError, onComplete, onSubscribe)
}