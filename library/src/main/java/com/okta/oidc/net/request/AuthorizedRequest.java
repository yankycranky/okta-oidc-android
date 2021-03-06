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

import com.okta.oidc.net.ConnectionParameters;
import com.okta.oidc.net.HttpResponse;
import com.okta.oidc.net.OktaHttpClient;
import com.okta.oidc.util.AuthorizationException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AuthorizedRequest extends BaseRequest<JSONObject, AuthorizationException> {

    AuthorizedRequest(HttpRequestBuilder.Authorized b) {
        super();
        mRequestType = b.mRequestType;
        mUri = b.mUri;
        ConnectionParameters.ParameterBuilder builder = new ConnectionParameters.ParameterBuilder();
        if (b.mPostParameters != null) {
            builder.setPostParameters(b.mPostParameters);
        }
        if (b.mProperties != null) {
            builder.setRequestProperties(b.mProperties);
        }
        mConnParams = builder
                .setRequestMethod(b.mRequestMethod)
                .setRequestProperty("Authorization", "Bearer " + b.mTokenResponse.getAccessToken())
                .setRequestProperty("Accept", ConnectionParameters.JSON_CONTENT_TYPE)
                .setRequestType(mRequestType)
                .create();
    }

    @Override
    public JSONObject executeRequest(OktaHttpClient client) throws AuthorizationException {
        AuthorizationException exception = null;
        HttpResponse response = null;
        try {
            response = openConnection(client);
            return response.asJson();
        } catch (IOException io) {
            exception = new AuthorizationException(io.getMessage(), io);
        } catch (JSONException je) {
            exception = AuthorizationException.fromTemplate(AuthorizationException
                    .GeneralErrors.JSON_DESERIALIZATION_ERROR, je);
        } catch (Exception e) {
            exception = AuthorizationException.fromTemplate(AuthorizationException
                    .GeneralErrors.NETWORK_ERROR, e);
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
