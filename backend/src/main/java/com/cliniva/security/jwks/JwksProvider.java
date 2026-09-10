package com.cliniva.security.jwks;

import java.security.PublicKey;
import java.util.Map;

public interface JwksProvider {
    Map<String, PublicKey> obterChaves();
}