package com.buzzard.db.models;


import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.buzzard.db.DBObject;
import com.buzzard.etc.BuzzardConstants;



public class DataTable extends DBObject
{
  private long id;
  private long fileID;
  private long offset;
  private long length;
  private byte[] data;
  
  private long chunkOffset;
  
  public DataTable()
  {
    super(DataTable.class);
  }
  
  public static long persistData(
      long userID,
      String fileName,
      long dataOffset,
      byte[] fileData
      )
  {
    //create a row in the filename table if needed
    FileNameTable nameTableEntry = 
        FileNameTable.addFileNameTableEntryIfMissing(userID, fileName);
    
    //create one or more rows in the filedata table
    DataTable table = new DataTable();
    table.setChunkOffset(0);
    table.setOffset(dataOffset);
    table.setFileID(nameTableEntry.getFileID());
    
    while(table != null)
    {
      table = table.getNextTableEntry(dataOffset,fileData);
      
      if(table != null)
      {
        table.save();
      }
    }
    
    return nameTableEntry.getFileID();
  }
  
  public static byte[] getChunk(
      final long fileID, 
      final long offset
      )
  {
    DataTable row = new DataTable();
    
    DataTable object = 
        (DataTable)
        row.getObject(
            Restrictions.eq("fileID",fileID),
            Restrictions.eq("offset",offset));
    
    return object.getData();
  }
  
  public static Set<Long> getDataOffsets(final long fileID)
  {
    ProjectionList projection = Projections.projectionList();
    projection.add(Projections.property("offset"));
    
    
    DataTable retriever = new DataTable();
    List<Object> objects = 
        retriever.getObjects(
            projection,
            Restrictions.eq("fileID",fileID));
    
    Set<Long> offsets = new TreeSet<Long>();
    for(Object object:objects)
    {
      offsets.add((Long)object);
    }
    
    return offsets;
  }
  
  
  
  private DataTable getNextTableEntry(
      final long baseDataOffset,
      byte[] data
      )
  {
    long chunkOffset = this.getChunkOffset();
    long chunkLength = data.length - chunkOffset;
    
    verifyLong(chunkOffset);
    verifyLong(chunkLength);
    
    if(chunkLength == 0)
    {
      return null;
    }
    
    if(chunkLength > BuzzardConstants.MAX_DATA_CHUNK_SIZE)
    {
      chunkLength = BuzzardConstants.MAX_DATA_CHUNK_SIZE;
    }
    
    byte[] chunk = new byte[(int)chunkLength];
    
    System.arraycopy(
        data, 
        (int)chunkOffset, 
        chunk, 
        0, 
        (int)chunkLength);
    
    DataTable entry = new DataTable();
    entry.setData(chunk);
    entry.setFileID(this.getFileID());
    entry.setLength(chunkLength);
    entry.setOffset(baseDataOffset+chunkOffset);
    entry.setChunkOffset(chunkOffset + chunkLength);
    
    return entry;
  }
  
  private long getChunkOffset()
  {
    return this.chunkOffset;
  }
  
  private void setChunkOffset(long chunkOffset)
  {
    this.chunkOffset = chunkOffset;
  }
  
  private static void verifyLong(long l)
  {
    if(l > Integer.MAX_VALUE || l < Integer.MIN_VALUE)
    {
      throw new RuntimeException(
          String.format(
              "the value %d is not representable as an integer",
              l));
    }
  }
  
  
  public long getFileID()
  {
    return fileID;
  }
  public void setFileID(long fid)
  {
    this.fileID = fid;
  }
  public long getOffset()
  {
    return offset;
  }
  public void setOffset(long offset)
  {
    this.offset = offset;
  }
  public long getLength()
  {
    return length;
  }
  public void setLength(long length)
  {
    this.length = length;
  }
  public byte[] getData()
  {
    return data;
  }
  public void setData(byte[] data)
  {
    this.data = data;
  }

  public long getId()
  {
    return id;
  }

  public void setId(long id)
  {
    this.id = id;
  }
  
  
}





