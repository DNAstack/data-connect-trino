package com.dnastack.ga4gh.dataconnect.adapter.security;

import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.MalformedClientSuppliedCredentialsException;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The request boundary for a caller's extra credentials: which of them it reads, and which it refuses. */
public class ClientSuppliedCredentialsReaderTest {

    /** A tenant that is not the request's, for the credentials a caller sends under a reserved name. */
    private static final String OTHER_TENANT = UUID.randomUUID().toString();

    private final ClientSuppliedCredentialsReader credentialsReader = new ClientSuppliedCredentialsReader();

    @Test
    public void parse_should_returnEachCredential_when_theHeaderCarriesSeveral() {
        // A credential this service knows nothing about is relayed as sent: which credentials Trino accepts is
        // Trino's business, not this service's.
        Map<String, String> credentials =
            credentialsReader.parse(List.of("userToken=a.b.c", "somethingElse=a-value"));

        assertThat(credentials)
            .as("the credentials read from the header")
            .containsExactly(entry("userToken", "a.b.c"), entry("somethingElse", "a-value"));
    }

    @Test
    public void parse_should_refuseTheHeader_when_aCredentialNamesATenant() {
        // Not because a caller naming a tenant gains anything -- the tenant indicator is caller-controlled by
        // contract, and the service evaluating policy holds it to the token it was sent -- but because Trino
        // takes the last value given for a name. Two of them would have this service audit the query under one
        // tenant while Trino ran it under another.
        assertThatThrownBy(() -> credentialsReader.parse(List.of("tenantId=" + OTHER_TENANT)))
            .as("reading a header whose caller named a tenant")
            .isInstanceOf(MalformedClientSuppliedCredentialsException.class)
            .hasMessageContaining("tenantId")
            .hasMessageContaining("sent by this service");
    }

    @Test
    public void parse_should_refuseTheHeader_when_aCredentialNamesTheTraceContext() {
        assertThatThrownBy(() -> credentialsReader.parse(List.of("traceparent=00-0af7651916cd43dd-b7ad6b71-01")))
            .as("reading a header whose caller named the trace context")
            .isInstanceOf(MalformedClientSuppliedCredentialsException.class)
            .hasMessageContaining("traceparent");
    }

    @Test
    public void parse_should_refuseTheHeader_when_aNameCarriesSurroundingSpace() {
        // Trino trims a credential's name, its header authenticator matches the name without trimming, and this
        // service relays by name. Rather than pick one of those readings, a padded name is refused: a caller
        // that cannot say plainly which credential it means is not answered with a guess.
        assertThatThrownBy(() -> credentialsReader.parse(List.of("userToken =a.b.c")))
            .as("reading a credential whose name is padded with space")
            .isInstanceOf(MalformedClientSuppliedCredentialsException.class)
            .hasMessageContaining("name=value");
    }

    @Test
    public void parse_should_refuseTheHeader_when_aCredentialCarriesASecondEquals() {
        // Trino splits on every '=' and refuses what is then not a pair, so this reaches it as a bad request
        // whatever this service does with it. Refusing it here says so while the caller can still be told why.
        assertThatThrownBy(() -> credentialsReader.parse(List.of("somethingElse=with=an=equals")))
            .as("reading a credential carrying more than one '='")
            .isInstanceOf(MalformedClientSuppliedCredentialsException.class)
            .hasMessageContaining("name=value");
    }

    @Test
    public void parse_should_refuseTheHeader_when_aValueCarriesAPercent() {
        // Trino URL-decodes a credential's value, so a value carrying an escape is not the value this service
        // read. Refusing the escape keeps the two readings the same without decoding on Trino's behalf.
        assertThatThrownBy(() -> credentialsReader.parse(List.of("userToken=one%20two")))
            .as("reading a credential whose value carries a percent escape")
            .isInstanceOf(MalformedClientSuppliedCredentialsException.class)
            .hasMessageContaining("name=value");
    }

    @Test
    public void parse_should_throwBadRequest_when_aCredentialCarriesNoEquals() {
        assertThatThrownBy(() -> credentialsReader.parse(List.of("userToken")))
            .isInstanceOf(MalformedClientSuppliedCredentialsException.class)
            .extracting(e -> ((MalformedClientSuppliedCredentialsException) e).httpStatus())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void parse_should_notEchoTheCredential_when_itCarriesNoEquals() {
        // The whole segment could be a bearer token the caller meant to name, so none of it is repeated back.
        assertThatThrownBy(() -> credentialsReader.parse(List.of("a.secret.token")))
            .hasMessageNotContaining("a.secret.token");
    }

    @Test
    public void parse_should_throwBadRequest_when_theSameCredentialIsSuppliedTwice() {
        assertThatThrownBy(() -> credentialsReader.parse(List.of("userToken=a.b.c", "userToken=d.e.f")))
            .isInstanceOf(MalformedClientSuppliedCredentialsException.class)
            .hasMessageContaining("userToken")
            .extracting(e -> ((MalformedClientSuppliedCredentialsException) e).httpStatus())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void parse_should_notEchoEitherValue_when_theSameCredentialIsSuppliedTwice() {
        assertThatThrownBy(() -> credentialsReader.parse(List.of("userToken=a.b.c", "userToken=d.e.f")))
            .hasMessageNotContaining("a.b.c")
            .hasMessageNotContaining("d.e.f");
    }

    @Test
    public void parse_should_returnNoCredentials_when_theHeaderIsAbsent() {
        assertThat(credentialsReader.parse(List.of()))
            .as("the credentials read from an absent header")
            .isEmpty();
    }

    @Test
    public void parse_should_ignoreABlankEntry_when_theHeaderEndsInASeparator() {
        // Spring splits the header on commas, so a trailing one yields a blank entry that names nothing. That is
        // a lenient client rather than a malformed credential, so it is skipped rather than refused.
        assertThat(credentialsReader.parse(List.of("userToken=a.b.c", " ")))
            .as("the credentials read from a header with a trailing separator")
            .containsExactly(entry("userToken", "a.b.c"));
    }
}
