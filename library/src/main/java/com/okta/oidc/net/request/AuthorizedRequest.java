/*
 * Copyright (c) 2019, Okta, Inc. and/or its affiliates. All rights reserved.
 * The Okta software accompanied by this notice is provided pursuant to the Apache License,
 * Version 2.0 (the "License.")
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.okta.oidc.net.request;

import androidx.annotation.RestrictTo;

import com.okta.oidc.RequestCallback;
import com.okta.oidc.RequestDispatcher;
import com.okta.oidc.net.HttpConnection;
import com.okta.oidc.net.HttpResponse;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import static com.okta.oidc.util.AuthorizationException.TYPE_GENERAL_ERROR;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AuthorizedRequest extends BaseRequest<JSONObject, AuthorizationException> {

    AuthorizedRequest(HttpRequestBuilder.Authorized b) {
        super();
        mRequestType = Type.AUTHORIZED;
        mUri = b.mUri;
        HttpConnection.Builder builder = new HttpConnection.Builder();
        if (b.mPostParameters != null) {
            builder.setPostParameters(b.mPostParameters);
        }
        if (b.mProperties != null) {
            builder.setRequestProperties(b.mProperties);
        }
        mConnection = builder
                .setRequestMethod(b.mRequestMethod)
                .setRequestProperty("Authorization", "Bearer " + b.mTokenResponse.getAccessToken())
                .setRequestProperty("Accept", HttpConnection.JSON_CONTENT_TYPE)
                .create(b.mConn);
    }

    @Override
    public void dispatchRequest(RequestDispatcher dispatcher,
                                RequestCallback<JSONObject, AuthorizationException> callback) {
        dispatcher.submit(() -> {
            try {
                JSONObject json = executeRequest();
                dispatcher.submitResults(() -> callback.onSuccess(json));
            } catch (AuthorizationException ae) {
                dispatcher.submitResults(() -> callback.onError(ae.error, ae));
            }
        });
    }

    @Override
    public JSONObject executeRequest() throws AuthorizationException {
        AuthorizationException exception = null;
        HttpResponse response = null;
        try {
            response = openConnection();
            return response.asJson();
        } catch (IOException io) {
            exception = new AuthorizationException(TYPE_GENERAL_ERROR,
                    AuthorizationException.GeneralErrors.NETWORK_ERROR.code,
                    AuthorizationException.GeneralErrors.NETWORK_ERROR.error,
                    io.getMessage(), null, io.getCause());
        } catch (JSONException je) {
            exception = AuthorizationException.fromTemplate(AuthorizationException
                    .GeneralErrors.JSON_DESERIALIZATION_ERROR, je);
        } finally {
            if (response != null) {
                response.disconnect();
            }
            if (exception != null) {
                throw exception;
            }
        }
        return null;
    }
}
