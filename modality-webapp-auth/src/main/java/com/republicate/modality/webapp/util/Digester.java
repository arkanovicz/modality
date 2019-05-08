package com.republicate.modality.webapp.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Digester
{
    public static String toHexMD5String(String target)
    {
        return toHexString(md5, target.getBytes(StandardCharsets.UTF_8));
    }

    public static String toHexMD5String(byte[] bytes)
    {
        return toHexString(md5, bytes);
    }

    public static String toHexSHA1String(String target)
    {
        return toHexString(sha1, target.getBytes(StandardCharsets.UTF_8));
    }

    public static String toHexSHA1String(byte[] bytes)
    {
        return toHexString(sha1, bytes);
    }

    private static String toHexString(MessageDigest algorithm, byte[] bytes)
    {
        byte[] digested = algorithm.digest(bytes);
        return hexEncode(digested);
    }

    private static char[] digits = { '0', '1', '2', '3', '4','5','6','7','8','9','a','b','c','d','e','f' };

    private static String hexEncode(byte[] bytes){
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < bytes.length; ++i)
        {
            byte b = bytes[i];
            result.append(digits[ (b&0xf0) >> 4 ]);
            result.append(digits[ b&0x0f]);
        }
        return result.toString();
    }

    private static MessageDigest md5;

    private static MessageDigest sha1;
}
