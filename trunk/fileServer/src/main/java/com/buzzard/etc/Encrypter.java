package com.buzzard.etc;

import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

//attribution: 
//http://docs.oracle.com/javase/1.4.2/docs/guide/security/jce/JCERefGuide.html
public class Encrypter
{
  private static Cipher cipher = null;

  public static void encrypt(
      String text,
      byte[] key
      ) throws Exception
  {
    final byte[] textBytes = text.getBytes("UTF8");
    
    Security.addProvider(new com.sun.crypto.provider.SunJCE());
    
    KeyGenerator keyGenerator = KeyGenerator.getInstance("DESede");
    keyGenerator.init(168);
    SecretKey secretKey = keyGenerator.generateKey();
    
    String keyBytes = new String(secretKey.getEncoded());
    System.out.printf("key: [%s]\n", keyBytes);
    
    cipher = Cipher.getInstance("DESede");

    cipher.init(Cipher.ENCRYPT_MODE, secretKey);
    byte[] cipherBytes = cipher.doFinal(textBytes);
    String cipherText = new String(cipherBytes, "UTF8");

    cipher.init(Cipher.DECRYPT_MODE, secretKey);
    byte[] decryptedBytes = cipher.doFinal(cipherBytes);
    String decryptedText = new String(decryptedBytes, "UTF8");

    System.out.println("Before encryption: " + text);
    System.out.println("After encryption: " + cipherText);
    System.out.println("After decryption: " + decryptedText);
  }
  
}


