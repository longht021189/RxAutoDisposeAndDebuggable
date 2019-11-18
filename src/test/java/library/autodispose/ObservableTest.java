package library.autodispose;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ObservableTest {

    @Test
    public void create() {
        final AtomicBoolean isCreated = new AtomicBoolean(false);
        final AtomicReference<ObservableEmitter<Integer>> emitterRef = new AtomicReference<>(null);

        Observable<Integer> observable = Observable.create(emitter -> {
            isCreated.set(true);
            emitterRef.set(emitter);
        });

        observable
                .as(new ObservableConverter<>())
                .subscribe(null, null, null, null);
    }

    @Test
    public void destroy() {

    }

    @Test
    public void resume() {

    }

    @Test
    public void pause() {

    }
}
