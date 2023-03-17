package Bookings.Monetisation.Mopers;

public class ClassShowcase {
    private String privateField;

    private static String privateStaticField;

    {
        privateField = "1";
        privateStaticField = "1";
    }

    static {
        privateStaticField = "3";
    }

    public ClassShowcase() {
        privateField = "4";
        privateStaticField = "5";
    }
}
