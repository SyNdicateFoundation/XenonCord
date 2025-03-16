package ir.xenoncommunity.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Check {


    public void checkNull(Object obj, String reason) throws AssertionError {
        Check.checkNull(obj, new String[]{reason});
    }


    public void checkNull(Object obj, String... reason) throws AssertionError {
        if (obj != null) return;
        throw new AssertionError(String.join(" ", reason));
    }

    public void checkNull(String reason, Object... objects) throws AssertionError {
        for (Object object : objects) {
            Check.checkNull(object, reason);
        }
    }

    public void checkNull(Object... objects) throws AssertionError {
        for (Object object : objects) {
            Check.checkNull(object);
        }
    }

    public void finite(double d, String reason) throws AssertionError {
        if (Double.isFinite(d)) return;
        throw new AssertionError(reason);
    }

    public void finite(float d, String reason) throws AssertionError {
        if (Float.isFinite(d)) return;
        throw new AssertionError(reason);
    }
}
