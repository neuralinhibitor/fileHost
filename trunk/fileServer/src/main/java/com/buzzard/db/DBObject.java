package com.buzzard.db;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;






public abstract class DBObject
{
  private final Class<?> classType;
  
  public DBObject(Class<?> classType)
  {
    this.classType = classType;
    HibernateManager.registerHibernateMappableClass(classType);
  }
  
  protected Class<?> getClassType()
  {
    return classType;
  }
  
  public DBObject getObject(Criterion...restrictions)
  {
    List<DBObject> rows = getObjects(restrictions);
    
    if(rows.size() == 0)
    {
      return null;
    }
    else if(rows.size() == 1)
    {
      return rows.iterator().next();
    }
    else
    {
      throw new RuntimeException(
          String.format(
              "expected 0 or 1 row in filenametable but found %d",
              rows.size()));
    }
  }
  
  public List<DBObject> getObjects(Criterion...restrictions)
  {
    Session session = HibernateManager.getSession();
    Criteria criteria = session.createCriteria(classType);
    
    for(Criterion restriction:restrictions)
    {
      criteria.add(restriction);
    }
    
    List<DBObject> list = criteria.list();
    session.close();
    
    return list;
  }
  
  public List<Object> getObjects(
      ProjectionList projection,
      Criterion...restrictions
      )
  {
    Session session = HibernateManager.getSession();
    Criteria criteria = session.createCriteria(classType);
    
    for(Criterion restriction:restrictions)
    {
      criteria.add(restriction);
    }
    
    if(projection != null)
    {
      criteria.setProjection(projection);
    }
    
    List<Object> list = criteria.list();
    session.close();
    
    return list;
  }
  
  public void delete()
  {
    Session session = HibernateManager.getSession();
    Transaction transaction = session.beginTransaction();
    
    session.delete(this);
    
    transaction.commit();
    session.close();
  }
  
  public void save()
  {
    Session session = HibernateManager.getSession();
    Transaction transaction = session.beginTransaction();
    
    session.save(this);
    
    transaction.commit();
    session.close();
  }
  
  public long getUniqueLongFieldID(String fieldName)
  {
    Session session = HibernateManager.getSession();
    
    Criteria criteria = 
        session.createCriteria(classType).setProjection(Projections.max(fieldName));
    
    Object result = criteria.uniqueResult();
    
    long max = 0;
    
    if(result != null)
    {
      max = (Long)result;
    }
    
    session.close();
    
    return max+1;
  }
  
  
}

