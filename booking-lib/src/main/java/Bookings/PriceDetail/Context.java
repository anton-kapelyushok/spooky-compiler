package Bookings.PriceDetail;

import perl.PerlDto;
import perl.PerlModule;
import perl.PerlName;

public interface Context extends PerlModule {
    @PerlName("new")
    Obj __new(NewContextArgs args);

    record NewContextArgs(
            Price price,
            Integer price_mode,
            Object travel_purpose,
            String cc1
    ) implements PerlDto {
    }

    interface Obj {
        Breakdown breakdown();
    }
}
