package Bookings.ToConvert;

import Bookings.External.ExternalModule;
import perl.ArrayRef;
import perl.PerlDto;

public record MySubArgs(
        ArrayRef<ExternalModule.Poupa> poupas
) implements PerlDto {
}
