package simple_class;

public class SimpleClass {
    private final String value1;
    private final String value2;
    private final String value3 = "loupa";

    SimpleClass(String value1) {
        this.value1 = value1;
        value2 = "value2";
    }

    public int sumOfLengthsPlusArg(int arg) {
        var partial = value1.length() + value2.length();
        return partial + arg;
    }
}
