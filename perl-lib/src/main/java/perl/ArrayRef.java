package perl;

import java.util.function.Function;

public class ArrayRef<T> {
    @PerlIntrinsic
    public <R> ArrayRef<R> map(Function<T, R> mapper) {
        return null;
    }

    @PerlIntrinsic
    public T get(Integer nz) {
        return null;
    }
}
