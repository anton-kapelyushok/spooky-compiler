package Bookings.Monetisation.Mopers.Bookings.Monetisation.Mopers.Core;

import Booking.Monetisation.Mopers.Bookings.Monetisation.Mopers.Core.Momo;
import Booking.Monetisation.Mopers.Codec;
import Booking.Monetisation.Mopers.Core.Codec.V1;
import Bookings.Monetisation.Mopers.RawMomo;
import perl.ArrayRef;
import perl.DieException;
import perl.HashRef;
import perl.PerlModule;

public class MomoModule implements PerlModule {

    private final Codec mainCodec;
    private final HashRef<String, Codec> codecs = new HashRef<>();

    public MomoModule(
            V1 v1Codec
    ) {
        mainCodec = v1Codec;
        codecs.put("1.0.0", v1Codec);
    }

    public Momo create(Codec.CreatePayload payload) {
        return mainCodec.create(payload);
    }

    public Momo decode(RawMomo rawMomo) {
        return _codec(new ArrayRef<>(rawMomo.major_version(), rawMomo.minor_version(), rawMomo.patch_version()))
                .decode(rawMomo);
    }

    public RawMomo encode(Momo momo) {
        return _codec(momo.version()).encode(momo);
    }

    private Codec _codec(ArrayRef<Integer> version) {
        var version_str = "" + version.get(0) + version.get(1) + version.get(2);

        if (codecs.has(version_str)) {
            throw new DieException("Momo version " + version_str + " not supported");
        }

        return codecs.get(version_str);
    }
}
