# Okta OpenID Connect & OAuth 2.0 API library

## Overview

This library currently supports:

[OpenID Connect & OAuth 2.0 API](https://developer.okta.com/docs/api/resources/oidc/)

## Requirements

Okta OIDC SDK supports Android API 19 and above. [Chrome custom tab](https://developer.chrome.com/multidevice/android/customtabs) enabled browsers
are needed by the library for browser initiated authorization. App must use FragmentActivity or any extensions of it to work with the library. An Okta developer account is needed to run the sample.

## Installation

Add the `Okta OIDC` dependency to your `build.gradle` file:

```gradle
implementation 'com.okta.oidc.android:okta-oidc-androidx:1.0.0'
```

## Sample app

A sample is contained within this repository. For more information on how to
build, test and configure the sample, see the sample [README](https://github.com/okta/okta-oidc-android/blob/master/app/README.md).

### Configuration

First the Authenticate client must have a account to interact with Okta's OIDC provider. Create a `OIDCAccount` like the following example:

```java
account = new OIDCAccount.Builder()
    .clientId("{clientId}")
    .redirectUri("{redirectUri}")
    .endSessionRedirectUri("{endSessionUri}")
    .scopes("openid", "profile", "offline_access")
    .discoveryUri("https://{yourOktaDomain}")
    .create();
```

Then create a `client` like the following:

```Java
client = new AuthenticateClient.Builder()
    .withAccount(account)
    .withContext(getApplicationContext())
    .withStorage(new SimpleOktaStorage(this))
    .withTabColor(getColorCompat(R.color.colorPrimary))
    .create();
```

After creating the client, register a callback to receive authorization results.

```java
client.registerCallback(new ResultCallback<AuthorizationStatus, AuthorizationException>() {
    @Override
    public void onSuccess(@NonNull AuthorizationStatus status) {
        if (status == AuthorizationStatus.AUTHORIZED) {
            //client is authorized.
            Tokens tokens = client.getTokens();
        } else if (status == AuthorizationStatus.LOGGED_OUT) {
            //this only clears the browser session.
        } else if (status == AuthorizationStatus.IN_PROGRESS) {
            //authorization is in progress.
        }
    }

    @Override
    public void onCancel() {
        //authorization canceled
    }

    @Override
    public void onError(@NonNull String msg, AuthorizationException error) {
     //error encounted
    }
}, this);
```

The `client` can now be used to authenticate users and authorizing access.

### Using JSON configuration file

You can also create a `account` by poviding a JSON file.
Create a file called `okta_oidc_config.json` in your application's `res/raw/` directory with the following contents:

```json
{
  "client_id": "{clientId}",
  "redirect_uri": "{redirectUri}",
  "end_session_redirect_uri": "{endSessionUri}",
  "scopes": [
    "openid",
    "profile",
    "offline_access"
  ],
  "issuer_uri": "https://{yourOktaDomain}"
}
```

Use this JSON file to create a `account`:

```java
account = new OIDCAccount.Builder()
    .withResId(this, R.id.okta_oidc_config)
    .create();
```

**Note**: To receive a **refresh_token**, you must include the `offline_access` scope.

## Obtaining an access token

The authorization flow consists of four stages.

1. Service discovery - This uses the issuer_uri or discoveryUri to get a list of endpoints.
2. Authorizing the user with crome custom tabs to obtain an authorization code.
3. Exchanging the authorizaton code for a access token, ID token, and refresh token.
4. Using the tokens to interact with a resource server for access to user data.

This is all done in the background by the SDK. For example to login you can call:

```java
client.logIn(this, null);
```

The results will be returned in the registered callback. If the application needs to send extra
data to the api endpoint, `AuthenticationPayload` can be used:

```java
Payload payload = new AuthenticationPayload.Builder()
    .setLoginHint("youraccount@okta.com")
    .addParameter("max_age", "5000")
    .build();

client.logIn(this, payload);
```

## onActivityResult override

ATTENTION! This library uses a nested fragment and the `onActivityResult` method to receive data from the browser.
In the case that you override the 'onActivityResult' method you must invoke 'super.onActivityResult()' method.

```java
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
```

## Using the Tokens

Once the user is authorized you can use the client object to call the OIDC endpoints

### Get user information

An example of getting user information from [userinfo](https://developer.okta.com/docs/api/resources/oidc/#userinfo) endpoint:

```java
client.getUserProfile(new RequestCallback<JSONObject, AuthorizationException>() {
    @Override
    public void onSuccess(@NonNull JSONObject result) {
        //handle JSONObject result.
    }

    @Override
    public void onError(String error, AuthorizationException exception) {
        //handle failed userinfo request
    }
});
```

In `onSuccess` the userinfo returned is a `JSONObject` with the [response properties](https://developer.okta.com/docs/api/resources/oidc/#response-example-success-5).

### Performing Authorized Requests

In addition to the built in endpoints, you can use the client interface to perform your own authorized requests, whatever they might be. You can call `authorizedRequest` requests and have the access token automatically added to the `Authorization` header with the standard OAuth 2.0 prefix of `Bearer`.

```java
final Uri uri;
HashMap<String, String> properties = new HashMap<>();
properties.put("queryparam", "queryparam");
HashMap<String, String> postParameters = new HashMap<>();
postParameters.put("postparam", "postparam");

client.authorizedRequest(uri, properties,
                postParameters, HttpConnection.RequestMethod.POST, new RequestCallback<JSONObject, AuthorizationException>() {
    @Override
    public void onSuccess(@NonNull JSONObject result) {
        //handle JSONObject result.
    }

    @Override
    public void onError(String error, AuthorizationException exception) {
        //handle failed request
    }
});
```

### Refresh a Token

You can refresh the `tokens` when the following request:

```java
client.refreshToken(new RequestCallback<Tokens, AuthorizationException>() {
    @Override
    public void onSuccess(@NonNull Tokens result) {
        //handle success.
    }

    @Override
    public void onError(String error, AuthorizationException exception) {
        //handle request failure
    }
});
```

### Revoking a Token

Tokens can be revoked with the following request:

```java
client.revokeToken(client.getTokens().getRefreshToken(),
    new RequestCallback<Boolean, AuthorizationException>() {
        @Override
        public void onSuccess(@NonNull Boolean result) {
            //handle result
        }
        @Override
        public void onError(String error, AuthorizationException exception) {
            //handle request error
        }
    });
```

**Note:** *Access, refresh and ID tokens need to be revoked in separate requests. The request only revokes the specified token*

### Introspect a token

Tokens can be checked for more detailed information by using the introspect endpoint:

```java
client.introspectToken(client.getTokens().getRefreshToken(),
    TokenTypeHint.REFRESH_TOKEN, new RequestCallback<IntrospectResponse, AuthorizationException>() {
        @Override
        public void onSuccess(@NonNull IntrospectResponse result) {
            //handle introspect response.
        }

        @Override
        public void onError(String error, AuthorizationException exception) {
            //handle request error
        }
    }
);
```

A list of the response properties can be found [here](https://developer.okta.com/docs/api/resources/oidc/#response-properties-3)

## Logging out

If the user is logged in using the browser initiated authorization flow, then logging out
is a two or three step process depending on revoking the tokens.

1. Clear the browser session.
2. [Revoke the tokens](#Revoking-a-Token) (optional)
3. Clear the stored tokens in [memmory](#Token-Management).

### Clear session

In order to clear the browser session you have to call `signOutFromOkta()`.

```java
    client.signOutFromOkta(this);
}
```

This clears the current browser session only. It does not remove or revoke the cached tokens stored in the `client`.
Until the tokens are removed or revoked, the user can still access data from the resource server.

### Clear tokens from device

Tokens can be removed from the device by simply calling:

```java
    client.clear();
```

After this the user is logged out.

## Token Management

Tokens are encrypted and securely stored in the private Shared Preferences.
If you do not want `AuthenticateClient` to store the data you can pass in a empty interface when creating the `client`

```java


client = new AuthenticateClient.Builder()
    .withAccount(mOktaAccount)
    .withContext(getApplicationContext())
    .withStorage(new OktaStorage() {
                @Override
                public void save(@NonNull String key, @NonNull String value) {
                }
                @Override
                public String get(@NonNull String key) {
                    return null;
                }
                @Override
                public void delete(@NonNull String key) {
                }})
    .withTabColor(getColorCompat(R.color.colorPrimary))
    .create();
```

The library provides a storage interface and encryption interface. These interfaces allow the developer to override the default implementation if they wish to use custom encryption or storage mechanism. For more see the [advance configuration](#Providing-custom-storage) section.

## Advance configuration

The library allows customization to specific parts the SDK to meet developer needs.

## Providing browser used for authorization

The default browser used for authorization is Chrome. If you want to change it FireFox, you can add this in the `AuthenticateClient.Builder()`:

```java

String SAMSUNG = "com.sec.android.app.sbrowser";
String FIREFOX = "org.mozilla.firefox";

client = new AuthenticateClient.Builder()
    .withAccount(mOktaAccount)
    .withContext(getApplicationContext())
    .withStorage(new SimpleOktaStorage(this))
    .withTabColor(getColorCompat(R.color.colorPrimary))
    .supportedBrowsers(FIREFOX, SAMSUNG)
    .create();
```

The library will attempt to use FireFox then Samsung browsers first.
If none are found it will default to Chrome.

## Customize HTTP requests

You can customize how HTTP connections are made by implementing the `HttpConnectionFactory` interface. For example if you want to customize the SSL socket factory:

```java
private class MyConnectionFactory implements HttpConnectionFactory {
    @Override
    public HttpURLConnection build(@NonNull URL url) throws Exception {
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustManager, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }
        });
        return (HttpURLConnection) url.openConnection();
    }
}

client = new AuthenticateClient.Builder()
    .withAccount(mOktaAccount)
    .withContext(getApplicationContext())
    .withStorage(new SimpleOktaStorage(this))
    .withTabColor(getColorCompat(R.color.colorPrimary))
    .withHttpConnectionFactory(new MyConnectionFactory())
    .create();
```

## Providing custom storage

The library uses a simple storage using shared preferences to store data. If you wish to use SQL or any other storage mechanism you can implement the storage interface and use it when creating `AuthenticateClient`.

```java
public class MyStorage implements OktaStorage {
    @Override
    public void save(@NonNull String key, @NonNull String value) {
        //Provide implementation
    }

    @Nullable
    @Override
    public String get(@NonNull String key) {
        return null; //Provide implementation
    }

    @Override
    public void delete(@NonNull String key) {
        //Provide implementation
    }
}

client = new AuthenticateClient.Builder()
    .withAccount(mOktaAccount)
    .withContext(getApplicationContext())
    .withStorage(new MyStorage())
    .withTabColor(getColorCompat(R.color.colorPrimary))
    .supportedBrowsers(FIREFOX, SAMSUNG)
    .create();
```

## Providing custom encryption

TODO add after encryption and smart lock branch is merged.