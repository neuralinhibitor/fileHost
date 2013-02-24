package com.buzzard.etc;



public class BuzzardConstants
{
  private static final String BUZZARD_DATA_ROOT = 
      String.format(
          "%s/buzzard/data",
          System.getProperty("user.home"));
  
  public static final String HIBERNATE_PROPERTIES_PATH = 
      String.format(
          "%s/hibernate.properties",
          BUZZARD_DATA_ROOT);
  
  public static final int MAX_DATA_CHUNK_SIZE = 4096*16;
}

