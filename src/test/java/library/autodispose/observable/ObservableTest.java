package library.autodispose.observable;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.disposables.Disposable;
import library.autodispose.State;
import library.autodispose.StateController;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ObservableTest {

    @Test
    public void testDelaySubscription() {
        final AtomicReference<ObservableEmitter<Integer>> emitterRef = new AtomicReference<>(null);
        final Observable<Integer> observable = Observable.create(emitterRef::set);

        final AtomicReference<ObservableEmitter<Integer>> emitterRef2 = new AtomicReference<>(null);
        final Observable<Integer> observable2 = Observable.create(emitterRef2::set);

        final Observable<Integer> observable3 = observable.delaySubscription(observable2);
        final Disposable disposable = observable3.subscribe();

        if (emitterRef.get() == null) {
            System.out.println("emitterRef == null");
        } else {
            System.out.println("emitterRef != null");
        }
        if (emitterRef2.get() == null) {
            System.out.println("emitterDelayRef == null");
        } else {
            System.out.println("emitterDelayRef != null");

            emitterRef2.get().onNext(0);
            if (emitterRef.get() == null) {
                System.out.println("[onNext] emitterRef == null");
            } else {
                System.out.println("[onNext] emitterRef != null");

                emitterRef.get().onComplete();
                if (emitterRef2.get().isDisposed()) {
                    System.out.println("[onComplete] emitterDelayRef isDisposed = true");
                } else {
                    System.out.println("[onComplete] emitterDelayRef isDisposed = false");
                }
                if (emitterRef.get().isDisposed()) {
                    System.out.println("[onComplete] emitterRef isDisposed = true");
                } else {
                    System.out.println("[onComplete] emitterRef isDisposed = false");
                }
                if (disposable.isDisposed()) {
                    System.out.println("[onComplete] disposable isDisposed = true");
                } else {
                    System.out.println("[onComplete] disposable isDisposed = false");
                }
            }
        }
        if (!disposable.isDisposed()) {
            disposable.dispose();
        }

        boolean isSubscribedSuccess = false;

        try {
            observable3.subscribe().dispose();
            isSubscribedSuccess = true;
        } catch (Throwable error) {
            System.out.println(error.toString());
        }

        if (isSubscribedSuccess) {
            System.out.println("isSubscribedSuccess = true");
        } else {
            System.out.println("isSubscribedSuccess = false");
        }
    }

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

    @Test
    public void error() {
        final int NO_RESULT = -1;

        final AtomicReference<ObservableEmitter<Integer>> emitterRef = new AtomicReference<>(null);
        final AtomicReference<Throwable> errorRef = new AtomicReference<>(null);
        final StateController controller = new StateController();
        final AtomicInteger result = new AtomicInteger(NO_RESULT);

        final Observable<Integer> observable = Observable.create(emitterRef::set);
        final Disposable disposable = observable
                .as(new ObservableConverter<>(controller))
                .subscribe(result::set, errorRef::set);

        controller.dispatchState(State.Created);

        final Exception exception = new RuntimeException();
        final StackTraceElement[] stackTraceElements = exception.getStackTrace();

        emitterRef.get().onError(exception);

        Assert.assertTrue("It's Error", emitterRef.get().isDisposed());
        Assert.assertTrue("It's Error", disposable.isDisposed());
        Assert.assertNotNull("It's Error", errorRef.get());
        Assert.assertNotEquals("It's Error", errorRef.get().getStackTrace(), stackTraceElements);

        errorRef.get().printStackTrace(System.out);
    }
}
