package library.autodispose;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
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

        Assert.assertNull("It's before Created => Emitter isn't Created", emitterRef.get());

        controller.dispatchState(State.Created);
        Assert.assertNotNull("It's Created => Emitter is Created", emitterRef.get());

        emitterRef.get().onNext(RESULT_VALUE);
        Assert.assertEquals("It's Created => Consumer doesn't receive data", result.get(), NO_RESULT);

        disposable.dispose();
        Assert.assertTrue("It's Created and Dispose Manual", emitterRef.get().isDisposed());
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
        Assert.assertEquals("It's Created => Consumer doesn't receive data", result.get(), NO_RESULT);

        controller.dispatchState(State.Resumed);
        Assert.assertEquals("It's Resumed => Consumer receive data", result.get(), RESULT_VALUE);

        disposable.dispose();
        Assert.assertTrue("It's Resumed and Dispose Manual", emitterRef.get().isDisposed());
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
        Assert.assertEquals("It's Resumed => Consumer receive data", result.get(), RESULT_VALUE);

        controller.dispatchState(State.Paused);
        emitterRef.get().onNext(RESULT_VALUE_2);
        Assert.assertEquals("It's Paused => Consumer doesn't receive data", result.get(), RESULT_VALUE);

        controller.dispatchState(State.Resumed);
        Assert.assertEquals("It's Resumed => Consumer receive next data", result.get(), RESULT_VALUE_2);

        disposable.dispose();
        Assert.assertTrue("It's Resumed and Dispose Manual", emitterRef.get().isDisposed());
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

        Assert.assertTrue("It's Destroyed ", emitterRef.get().isDisposed());
        Assert.assertTrue("It's Destroyed ", disposable.isDisposed());

        boolean isDispatchSuccess = false;

        try {
            controller.dispatchState(State.Created);
            isDispatchSuccess = true;
        } catch (Throwable error) {
            System.out.println(error.toString());
        }

        if (isDispatchSuccess) {
            Assert.fail("It's Destroyed, can't call dispatchState");
        }

        final Disposable disposable2 = Observable.never()
                .as(new ObservableConverter<>(controller))
                .subscribe(o -> {});

        Assert.assertTrue("It's Destroyed ", disposable2.isDisposed());
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
                    System.out.println("Completed");
                    isComplete.set(true);
                });

        controller.dispatchState(State.Created);
        emitterRef.get().onComplete();

        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertTrue("Completed", emitterRef.get().isDisposed());
        Assert.assertTrue("Completed", isComplete.get());
        Assert.assertTrue("Completed", disposable.isDisposed());
    }

    /*@Test
    public void multiSubscribe() {
        final Observable<Object> observable = Observable.never();
        final StateController controller = new StateController();

        observable
                .as(new ObservableConverter<>(controller))
                .subscribe(obj -> {});

        boolean isSubscribeSuccess = false;

        try {
            observable
                    .as(new ObservableConverter<>(controller))
                    .subscribe(obj -> {});

            isSubscribeSuccess = true;
        } catch (Throwable error) {
            System.out.println(error.toString());
        }

        if (isSubscribeSuccess) {
            Assert.fail("It's Subscribed, can't call subscribe");
        }
    }

    @Test
    public void multiSubscribeOnStateCreated() {
        final Observable<Object> observable = Observable.never();
        final StateController controller = new StateController();

        observable
                .as(new ObservableConverter<>(controller))
                .subscribe(obj -> {});

        boolean isSubscribeSuccess = false;
        controller.dispatchState(State.Created);

        try {
            observable
                    .as(new ObservableConverter<>(controller))
                    .subscribe(obj -> {});

            isSubscribeSuccess = true;
        } catch (Throwable error) {
            System.out.println(error.toString());
        }

        if (isSubscribeSuccess) {
            Assert.fail("It's Subscribed, can't call subscribe");
        }
    }*/
}
