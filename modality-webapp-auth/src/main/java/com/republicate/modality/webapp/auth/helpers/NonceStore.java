package com.republicate.modality.webapp.auth.helpers;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class NonceStore
{
    private static int NONCE_LENGTH = 32;
    private static int NONCE_LIFETIME = 3600000; // one hour
    private static String NONCE_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz";

    public NonceStore()
    {
    }

    public String newNonce()
    {
        String nonce = RandomStringUtils.random(NONCE_LENGTH, NONCE_CHARS);
        noncesMap.put(nonce, new NonceInfo());
        return nonce;
    }

    public boolean checkNonce(final String nonce, final Integer nc)
    {
        return Optional.ofNullable(noncesMap.get(nonce)).filter(info -> nc == null || nc > info.sequence).map(info -> { if (nc != null) info.sequence = nc; return true; }).orElse(false);
    }

    LinkedHashMap<String, NonceInfo> noncesMap = new LinkedHashMap<String, NonceInfo>()
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest)
        {
            NonceInfo nonceInfo = (NonceInfo)eldest.getValue();
            return (new Date().getTime() - nonceInfo.time) > NONCE_LIFETIME;
        }
    };

    private class NonceInfo
    {
        long time = new Date().getTime();
        int sequence = 0;
    }
}
