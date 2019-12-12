package library.autodispose

import io.reactivex.Observable
import library.autodispose.observable.ObservableProxy

fun <T> Observable<T>.autoDispose(scope: Observable<State>): ObservableProxy<T> {
    return `as`(AutoDispose.autoDispose(scope))
}

fun <T> Observable<T>.autoDispose(scope: AutoDisposeConverter<T>): ObservableProxy<T> {
    return `as`(scope)
}