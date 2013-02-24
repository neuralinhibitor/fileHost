package com.buzzard.db.models;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.criterion.Restrictions;

import com.buzzard.db.DBObject;



public class FileNameTable extends DBObject
{
  private long id;
  private long userID;
  private long fileID;
  private String fileName;
  
  public FileNameTable()
  {
    super(FileNameTable.class);
  }
  
  public static String getFileNameFromID(long userID, long fileID)
  {
    FileNameTable table = new FileNameTable();
    
    FileNameTable object = 
        (FileNameTable)
        table.getObject(
            Restrictions.eq("userID", userID),
            Restrictions.eq("fileID", fileID));
    
    return object.getFileName();
  }
  
  public static Collection<Long> getFileIDs(long userID)
  {
    FileNameTable table = new FileNameTable();
    
    List<DBObject> objects = 
        table.getObjects(Restrictions.eq("userID", userID));
    
    Set<Long> set = new HashSet<Long>();
    
    for(DBObject object:objects)
    {
      FileNameTable row = (FileNameTable) object;
      
      set.add(row.getFileID());
    }
    
    return set;
  }
  
  public static void deleteFileData(long userID)
  {
    FileNameTable table = new FileNameTable();
    
    List<DBObject> objects = 
        table.getObjects(
            Restrictions.eq("userID", userID));
    
    for(DBObject object:objects)
    {
      object.delete();
    }
  }
  
  public static void deleteFileData(long userID, String fileName)
  {
    FileNameTable table = new FileNameTable();
    
    DBObject object = 
        table.getObject(
            Restrictions.eq("userID", userID),
            Restrictions.eq("fileName", fileName));
    
    if(object != null)
    {
      object.delete();
    }
  }
  
  public static FileNameTable addFileNameTableEntryIfMissing(
      long userID,
      String fileName
      )
  {
    FileNameTable table = new FileNameTable();
    
    FileNameTable entry = 
        (FileNameTable)
        table.getObject(
            Restrictions.eq("userID", userID),
            Restrictions.eq("fileName", fileName));
        

    if(entry == null)
    {
      long fileID = table.getUniqueLongFieldID("fileID");
          
      table.setFileID(fileID);
      table.setUserID(userID);
      table.setFileName(fileName);
      
      table.save();
      
      return  table;
    }
    else
    {
      return entry;
    }
  }
  
  
  
  public long getId()
  {
    return id;
  }

  public void setId(long id)
  {
    this.id = id;
  }

  public long getUserID()
  {
    return userID;
  }

  public void setUserID(long userID)
  {
    this.userID = userID;
  }

  public long getFileID()
  {
    return fileID;
  }

  public void setFileID(long fileID)
  {
    this.fileID = fileID;
  }

  public String getFileName()
  {
    return fileName;
  }

  public void setFileName(String fileName)
  {
    this.fileName = fileName;
  }
  
  
}

