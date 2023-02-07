package Bookings.Payment.Util;

import perl.PerlModule;

public interface Pricing extends PerlModule {
    Bookings.PriceDetail.Price _summary_for_stay(Object stay_info, Integer nr_guests);
}
