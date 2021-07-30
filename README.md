# Intro

An implementation of the [GA4GH Discovery Search API](https://github.com/ga4gh-discovery/ga4gh-discovery-search), on top of
[PrestoSQL](https://prestosql.io). This software enables users to enumerate and query data surfaced by an instance of PrestoSQL
in a manner compliant with the GA4GH Discovery Search specification, and receive responses compliant with the 
[Table](https://github.com/ga4gh-discovery/ga4gh-discovery-search/blob/develop/TABLE.md) specification.  

# Updating This README

Any structural changes to this README should be checked against the README_TEMPLATE available in `dnastack-development-tools`, with that document updated as necessary.

# Quick Start
Get started in 30s.
### Prerequisites
- Java 11+
- A presto server you can access anonymously over HTTP(S).

### Build

```
mvn clean package
```

### Configure
 
Set these two environment variables.
```$xslt
PRESTO_DATASOURCE_URL=https://<your-presto-server>
SPRING_PROFILES_ACTIVE=no-auth
```

The search adapter requires a Postgres database. To start the app locally quickly with the default settings, you can 
spin up the database with this docker command:
```
docker run -d -p 5432:5432 --name ga4ghsearchadapterpresto -e POSTGRES_USER=ga4ghsearchadapterpresto -e POSTGRES_PASSWORD=ga4ghsearchadapterpresto postgres
```

### Run
```$xslt
mvn clean spring-boot:run
```

# Configuring (Advanced)

## Auth Profiles

The app can be deployed using one of 3 different spring profiles which configure the authentication expectations. The default profile
will be used if no other profile is activated. 

Each profile also enables the following set of spring configuration variables:

```bash
APP_AUTH_AUTHORIZATIONTYPE="bearer" or "basic" or "none"
APP_AUTH_ACCESSEVALUATOR="scope" or "wallet" # only applies when AUTHORIZATIONTYPE=bearer
APP_AUTH_GLOBALMETHODSECURITY_ENABLED=true or false # enables security annotations on REST endpoints 
```

To set a profile simply set the `SPRING_PROFILES_ACTIVE` environment variable 
to one of the three profiles outlined below:

#### `default` (JWT Authentication)

The default profile requires every inbound request to include a JWT, validated by the settings configured below.
The configuration is described by the [AuthConfig](src/main/java/org/ga4gh/discovery/search/security/AuthConfig.java)
class. This is the profile used if no profile is set.

Set the environment variables below, replacing the values below with values appropriate to your context. 

```bash
# (Required) The STS which issued this token.
APP_AUTH_TOKENISSUERS_0_ISSUERURI="https://your.expected.issuer.com"
# (Required) The Json Web Key Set URI (where to find token validation keys)
APP_AUTH_TOKENISSUERS_0_JWKSETURI="https://your.expected.issuer/oauth/jwks"
# (Optional) Set audience if you want your token's audience to be validated.
APP_AUTH_TOKENISSUERS_0_AUDIENCES_0_="ga4gh-search-adapter-presto"
# (Optional) Set scopes if you want your token's scopes to be validated. Set multiple with _SCOPES_1_, SCOPES_2_...
APP_AUTH_TOKENISSUERS_0_SCOPES_0_="read:*"
```

One may alternatively set the token validation key directly by setting the environment variable `APP_AUTH_TOKENISSUERS_1_RSAPUBLICKEY` to the desired key,
and omitting the `JWKSETURI` variable.

#### `wallet-auth` (Wallet Authentication - DNAstack)

The wallet-auth profile requires every inbound request to include a JWT, validated by the settings configured below.
The configuration is described by the [AuthConfig](src/main/java/org/ga4gh/discovery/search/security/AuthConfig.java)
class. This is the profile used if no profile is set.

The wallet-auth profile also sets up JWT-based authentication, and is configured with the same environment variables as the above, but also enables evaluation of Wallet-based access policies at endpoints.

#### `no-auth` (No Authentication)
**DO NOT USE IN PRODUCTION**

This profile will publicly expose all routes and does not require any authentication. Best left in your dev environment.

#### `basic-auth` (Basic Authentication)

This profile will protect API routes with `basic` authentication. Additionally, when a user makes a request, if they have
not logged in they will be redirected to a login screen. The default username is `user`, and the default password is set in
the `application.yaml`.

To configure the username and password, set the following environment variables:

```
SPRING_SECURITY_USER_NAME={some-user-name}
SPRING_SECURITY_USER_PASSWORD={some-password}
```

## Postgres Configuration
The search adapter uses presto to save queries, so that it can reparse them during pagination to re-evaluate functions
that need to be processed prior to submitting queries to presto.

The following is a quick start for local development:
```
docker pull postgres:latest
docker run -p 5432:5432 --rm --name ga4ghsearchadapterpresto -e POSTGRES_USER=ga4ghsearchadapterpresto -e POSTGRES_DB=ga4ghsearchadapterpresto -e POSTGRES_PASSWORD=ga4ghsearchadapterpresto postgres
``` 
## Presto Source Configuration

There are a number of required configuration properties that need to be set in order to communicate with a presto deployment. 
### Connectivity
Point the service to a presto server by setting the following environment variable:
>PRESTO_DATASOURCE_URL
### Authentication
If your presto instance is also protected, this adapter supports performing OAuth 2.0 Client Credential grants in order 
to retrieve access tokens for its configured Presto instance.

Configuration of the presto auth setup is quite easy and can be done directly through the following environment variables.

```bash
APP_AUTH_PRESTOOAUTHCLIENT_TOKENURI="https://your.sts/oauth/token"
APP_AUTH_PRESTOOAUTHCLIENT_CLIENTID="your-client-id"
APP_AUTH_PRESTOOAUTHCLIENT_CLIENTSECRET="your-client-secret"
APP_AUTH_PRESTOOAUTHCLIENT_AUDIENCE="your-requested-audience"
APP_AUTH_PRESTOOAUTHCLIENT_SCOPES="your space delimited requested scopes"
```
