package com.buzzard.etc;

import java.io.IOException;
import java.io.InputStream;



public class ServiceHelper
{
  public static byte[] extractNextChunk(
      InputStream input
      ) throws IOException
  {
    byte[] chunk = new byte[BuzzardConstants.MAX_DATA_CHUNK_SIZE];
    
    int bytesRead = input.read(chunk);
    
    if(bytesRead == -1)
    {
      return null;
    }
    
    if(bytesRead != chunk.length)
    {
      byte[] newChunk = new byte[bytesRead];
      System.arraycopy(chunk, 0, newChunk, 0, bytesRead);
      chunk = newChunk;
    }
    
    if(bytesRead != chunk.length)
    {
      throw new RuntimeException(
          String.format(
              "expected a chunk of size %d but found a chunk of size %d",
              bytesRead,
              chunk.length));
    }
    
    return chunk;
  }
  
}

