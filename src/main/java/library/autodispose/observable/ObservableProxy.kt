package library.autodispose.observable

import io.reactivex.disposables.Disposable
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer

interface ObservableProxy<T> {

    fun subscribe(
            onNext: Consumer<T>? = null,
            onError: Consumer<Throwable>? = null,
            onComplete: Action? = null,
            onSubscribe: Consumer<Disposable>? = null): Disposable

    fun doOnSubscribe(
            onSubscribe: Consumer<Disposable>): ObservableProxy<T>
}