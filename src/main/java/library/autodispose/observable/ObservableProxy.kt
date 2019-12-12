package library.autodispose.observable

import io.reactivex.disposables.Disposable
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer

interface ObservableProxy<T> {

    fun subscribe(
            onNext: Consumer<in T?>? = null,
            onError: Consumer<in Throwable?>? = null,
            onComplete: Action? = null,
            onSubscribe: Consumer<in Disposable?>? = null): Disposable
}