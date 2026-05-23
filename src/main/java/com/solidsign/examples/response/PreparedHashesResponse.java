package com.solidsign.examples.response;
import java.util.List;

/**
 * Maps the PreparedHashesDTO returned by SolidSign API sign-preparation.
 * Forward this to the React frontend — it will use the browser extension
 * (eToken A3 or A1 from certstore) to sign each hash and return signatureValue[i].
 */
public class PreparedHashesResponse {
    public String finalNonce;
    public int hashCount;
    public List<HashItem> hashes;

    public static class HashItem { public int index; public String hash; }
}
