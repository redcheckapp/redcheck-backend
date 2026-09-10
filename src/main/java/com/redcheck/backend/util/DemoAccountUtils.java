package com.redcheck.backend.util;

import java.util.Set;

// The two seeded demo accounts (see DataInitializer) are shared, publicly
// reachable identities — anything that would let a caller take one over or
// lock other demo users out (change/set password, password-reset; account
// deletion is separately guarded in UserController#deleteMyAccount) must
// check against this single source of truth rather than a copy-pasted
// literal, so a future third demo account can't be missed in one spot.
public class DemoAccountUtils {

    private static final Set<String> DEMO_EMAILS = Set.of(
            "demo-es@redcheck.com",
            "demo-en@redcheck.com"
    );

    public static boolean isDemoAccount(String email) {
        return email != null && DEMO_EMAILS.contains(email.toLowerCase());
    }
}
