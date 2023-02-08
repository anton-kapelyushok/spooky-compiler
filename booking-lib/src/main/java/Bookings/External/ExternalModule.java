package Bookings.External;

import perl.ArrayRef;
import perl.PerlModule;

public interface ExternalModule extends PerlModule {

    ArrayRef<Loupa> convert_to_loupas(ArrayRef<Poupa> poupa);

    interface Poupa {
    }

    interface Loupa {
        String volobuev(Number arg);
    }
}
