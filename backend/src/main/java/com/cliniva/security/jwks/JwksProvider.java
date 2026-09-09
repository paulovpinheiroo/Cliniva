package com.cliniva.security.jwks;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;

public interface JwksProvider {
    Map<String, RSAPublicKey> obterChaves();
}