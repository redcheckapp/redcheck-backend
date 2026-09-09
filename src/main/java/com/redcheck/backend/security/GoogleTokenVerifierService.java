package com.redcheck.backend.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.redcheck.backend.exception.InvalidGoogleTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

// Verifies a Google Identity Services ID token entirely offline (signature
// check against Google's published public keys + audience/issuer/expiry
// checks) — no call to Google's servers on the request path, and no client
// secret involved, since Sign-in-with-Google only needs to confirm who the
// token is for, not exchange an authorization code.
@Service
public class GoogleTokenVerifierService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierService(@Value("${app.google.client-id}") String googleClientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(googleClientId))
                .build();
    }

    public GoogleUserInfo verify(String idToken) {
        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new InvalidGoogleTokenException();
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();

            return new GoogleUserInfo(
                    payload.getSubject(),
                    payload.getEmail(),
                    Boolean.TRUE.equals(payload.getEmailVerified()),
                    (String) payload.get("name")
            );
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            throw new InvalidGoogleTokenException();
        }
    }
}
