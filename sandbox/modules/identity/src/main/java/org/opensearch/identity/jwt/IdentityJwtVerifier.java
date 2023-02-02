/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.identity.jwt;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.KeyType;
import org.apache.cxf.rs.security.jose.jwk.PublicKeyUse;
import org.apache.cxf.rs.security.jose.jws.JwsJwtCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtException;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.identity.ConfigConstants;
import org.opensearch.identity.configuration.model.InternalUsersModel;
import org.opensearch.identity.realm.InternalUsersStore;

public class IdentityJwtVerifier extends AbstractJwtVerifier {
    private final static Logger log = LogManager.getLogger(JwtVerifier.class);

    private static IdentityJwtVerifier INSTANCE;

    private IdentityJwtVerifier() { }

    public static IdentityJwtVerifier getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new IdentityJwtVerifier();
        }

        return INSTANCE;
    }

    public void init(String signingKey) {
        if (this.signingKey == null) {
            this.signingKey = signingKey;
        }
    }
}
