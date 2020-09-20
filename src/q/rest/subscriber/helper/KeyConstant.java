package q.rest.subscriber.helper;

import io.jsonwebtoken.Jwts;

import java.io.*;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

public class KeyConstant {

    private static final String FILE_DIRECTORY = "/configs/";
    private static final String PRIVATE_KEY_FILE = FILE_DIRECTORY + "id_rsa";
    private static final String PUBLIC_KEY_FILE = FILE_DIRECTORY +"id_rsa_pub.pem";

    private static final String PRIVATE_KEY_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String PRIVATE_KEY_FOOTER = "-----END RSA PRIVATE KEY-----";
    private static final String PUBLIC_KEY_HEADER = "-----BEGIN RSA PUBLIC KEY-----";
    private static final String PUBLIC_KEY_FOOTER = "-----END RSA PUBLIC KEY-----";

    public static PublicKey PUBLIC_KEY;
    private static PrivateKey PRIVATE_KEY;

    static{
        try {
            readPrivateKey();
            readPublicKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void readPrivateKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String keyContent = readFile(PRIVATE_KEY_FILE, PRIVATE_KEY_HEADER, PRIVATE_KEY_FOOTER);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyContent));
        PRIVATE_KEY = kf.generatePrivate(keySpecPKCS8);
    }

    private static void readPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String keyContent = readFile(PUBLIC_KEY_FILE, PUBLIC_KEY_HEADER, PUBLIC_KEY_FOOTER);
        org.bouncycastle.asn1.pkcs.RSAPublicKey pkcs1PublicKey = org.bouncycastle.asn1.pkcs.RSAPublicKey.getInstance(Base64.getDecoder().decode(keyContent));
        BigInteger modulus = pkcs1PublicKey.getModulus();
        BigInteger publicExponent = pkcs1PublicKey.getPublicExponent();
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, publicExponent);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PUBLIC_KEY = kf.generatePublic(keySpec);
    }

    public static String issueToken(int userId, Map<String, Object> claimsMap, Date issued, Date expiry) throws Exception{
        PrivateKey key = KeyConstant.PRIVATE_KEY;
        return Jwts.builder().addClaims(claimsMap)
                .setSubject(Integer.toString(userId))
                .setIssuedAt(issued)
                .setExpiration(expiry)
                .signWith(key).compact();
    }

    public static String issueToken(int userId, Map<String, Object> claimsMap) throws Exception{
        PrivateKey key = KeyConstant.PRIVATE_KEY;
        return Jwts.builder().addClaims(claimsMap)
                .setSubject(Integer.toString(userId))
                .setIssuedAt(new Date())
                .setExpiration(Helper.addDays(new Date(), 2))
                .signWith(key).compact();
    }

    private static String readFile(String path, String keyHeader, String keyFooter) throws IOException {
        StringBuilder s = new StringBuilder();
        InputStream is = new FileInputStream(path);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = null;
        while ((line = reader.readLine()) != null) {
            s.append(line);
        }
        String content = s.toString();
        content = content.replaceAll("\\n", "").replace(keyHeader, "").replace(keyFooter, "");
        return content;
    }
}
