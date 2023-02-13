package perl;

public final class HashRef<K, V> {
    @PerlIntrinsic
    public HashRef() {
    }

    @PerlIntrinsic
    public void put(K key, V v) {
    }

    @PerlIntrinsic
    public V get(K key) {
        return null;
    }

    @PerlIntrinsic
    public boolean has(K key) {
        return false;
    }
}
