public class Overriding {
    static class Parent {
        Number method(String arg) {
            return 0;
        }
    }

    static class Child extends Parent {
        @Override
        Integer method(String arg) {
            return 0;
        }
    }
}
