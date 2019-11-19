package library.autodispose;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.disposables.Disposable;
import org.junit.Test;
import org.junit.Assert;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ObservableTest {

    @Test
    public void create() {
        final int NO_RESULT = -1;
        final int RESULT_VALUE = 1;

        final AtomicReference<ObservableEmitter<Integer>> emitterRef = new AtomicReference<>(null);
        final StateController controller = new StateController();
        final AtomicInteger result = new AtomicInteger(NO_RESULT);

        Observable<Integer> observable = Observable.create(emitterRef::set);
        final Disposable disposable = observable
                .as(new ObservableConverter<>(controller)).subscribe(result::set);

        Assert.assertNull("Chưa có dispatch state " +
                "nào nên emitter chưa được khởi tạo", emitterRef.get());

        controller.dispatchState(State.Created);

        Assert.assertNotNull("Đã dispatch state Created " +
                "nên emitter đã được khởi tạo", emitterRef.get());

        emitterRef.get().onNext(RESULT_VALUE);

        Assert.assertEquals("Vì đang ở state Created " +
                "nên consumer không nhận giá trị", result.get(), NO_RESULT);

        disposable.dispose();

        Assert.assertTrue("Dù đang ở state Created " +
                "nhưng stream đã bị dispose bơi manual", emitterRef.get().isDisposed());
    }

    @Test
    public void resume() {
        final int NO_RESULT = -1;
        final int RESULT_VALUE = 1;

        final AtomicReference<ObservableEmitter<Integer>> emitterRef = new AtomicReference<>(null);
        final StateController controller = new StateController();
        final AtomicInteger result = new AtomicInteger(NO_RESULT);

        Observable<Integer> observable = Observable.create(emitterRef::set);
        final Disposable disposable = observable
                .as(new ObservableConverter<>(controller)).subscribe(result::set);

        controller.dispatchState(State.Created);
        emitterRef.get().onNext(RESULT_VALUE);

        Assert.assertEquals("Vì đang ở state Created " +
                "nên consumer không nhận giá trị", result.get(), NO_RESULT);

        controller.dispatchState(State.Resumed);

        Assert.assertEquals("Vì đang ở state Resumed " +
                "nên consumer nhận giá trị", result.get(), RESULT_VALUE);

        disposable.dispose();

        Assert.assertTrue("Dù đang ở state Resumed " +
                "nhưng stream đã bị dispose bơi manual", emitterRef.get().isDisposed());
    }

    @Test
    public void pause() {
        final int NO_RESULT = -1;
        final int RESULT_VALUE = 1;
        final int RESULT_VALUE_2 = 2;

        final AtomicReference<ObservableEmitter<Integer>> emitterRef = new AtomicReference<>(null);
        final StateController controller = new StateController();
        final AtomicInteger result = new AtomicInteger(NO_RESULT);

        Observable<Integer> observable = Observable.create(emitterRef::set);
        final Disposable disposable = observable
                .as(new ObservableConverter<>(controller)).subscribe(result::set);

        controller.dispatchState(State.Created);
        controller.dispatchState(State.Resumed);
        emitterRef.get().onNext(RESULT_VALUE);

        Assert.assertEquals("Vì đang ở state Resumed " +
                "nên consumer nhận giá trị", result.get(), RESULT_VALUE);

        controller.dispatchState(State.Paused);
        emitterRef.get().onNext(RESULT_VALUE_2);

        Assert.assertEquals("Vì đang ở state Paused " +
                "nên consumer không nhận giá trị", result.get(), RESULT_VALUE);

        controller.dispatchState(State.Resumed);

        Assert.assertEquals("Vì trở lại state Resumed " +
                "nên consumer tiếp tục nhận giá trị tiếp theo", result.get(), RESULT_VALUE_2);

        disposable.dispose();

        Assert.assertTrue("Dù đang ở state Resumed " +
                "nhưng stream đã bị dispose bơi manual", emitterRef.get().isDisposed());
    }

    @Test
    public void destroy() {
        final int NO_RESULT = -1;
        final int RESULT_VALUE = 1;

        final AtomicReference<ObservableEmitter<Integer>> emitterRef = new AtomicReference<>(null);
        final StateController controller = new StateController();
        final AtomicInteger result = new AtomicInteger(NO_RESULT);

        Observable<Integer> observable = Observable.create(emitterRef::set);
        final Disposable disposable = observable
                .as(new ObservableConverter<>(controller)).subscribe(result::set);

        controller.dispatchState(State.Created);
        controller.dispatchState(State.Resumed);
        emitterRef.get().onNext(RESULT_VALUE);
        controller.dispatchState(State.Destroyed);

        Assert.assertTrue("Đang ở state Destroyed ", emitterRef.get().isDisposed());
        Assert.assertTrue("Đang ở state Destroyed ", disposable.isDisposed());

        try {
            controller.dispatchState(State.Created);
            Assert.fail("State Destroyed không thể tiếp dispatchState");
        } catch (Throwable ignore) { }

        final Disposable disposable2 = Observable.never()
                .as(new ObservableConverter<>(controller))
                .subscribe(o -> {});

        Assert.assertTrue("Đang ở state Destroyed ", disposable2.isDisposed());
    }

    @Test
    public void complete() {
        final int NO_RESULT = -1;

        final AtomicReference<ObservableEmitter<Integer>> emitterRef = new AtomicReference<>(null);
        final StateController controller = new StateController();
        final AtomicInteger result = new AtomicInteger(NO_RESULT);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicBoolean isComplete = new AtomicBoolean(false);

        Observable<Integer> observable = Observable.create(emitterRef::set);

        final Disposable disposable = observable
                .as(new ObservableConverter<>(controller))
                .subscribe(result::set, error::set, () -> {
                    isComplete.set(true);
                });

        controller.dispatchState(State.Created);
        emitterRef.get().onComplete();

        Assert.assertTrue("Đã complete", emitterRef.get().isDisposed());
        Assert.assertTrue("Đã complete", disposable.isDisposed());
        Assert.assertTrue("Đã complete", isComplete.get());
    }
}
