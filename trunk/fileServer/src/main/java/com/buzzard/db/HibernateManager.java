package com.buzzard.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import com.buzzard.etc.BuzzardConstants;




public class HibernateManager
{
  private static final List<Class<?>> hibernateMappedClasses = 
      new ArrayList<Class<?>>();
  
  private static SessionFactory sessionFactory = constructSessionFactory();
  
  private static final Object LOCK = new Object();
  
  private static SessionFactory constructSessionFactory()
  {
    SessionFactory factory = null;
    
    try
    {
      factory = getHibernateSessionFactory();
    }
    catch(Exception e)
    {
      throw new RuntimeException(e);
    }
      
    return factory;
  }
  
  public static void registerHibernateMappableClass(Class<?> classType)
  {
    synchronized(LOCK)
    {
      for(Class<?> hibernateMappedClass:hibernateMappedClasses)
      {
        if(hibernateMappedClass.getName().equals(classType.getName()))
        {
          return;
        }
      }
      
      hibernateMappedClasses.add(classType);
    
      sessionFactory.close();
      sessionFactory = constructSessionFactory();
    }
  }
  
  
  public static final Session getSession()
  {
    synchronized(LOCK)
    {
      return sessionFactory.openSession();
    }
  }
  
  private static final SessionFactory getHibernateSessionFactory() throws IOException
  {
    FileInputStream hibernatePropertiesStream = 
        new FileInputStream(
            new File(BuzzardConstants.HIBERNATE_PROPERTIES_PATH));
    
    Properties hibernateProperties = new Properties();
    hibernateProperties.load(hibernatePropertiesStream);
    hibernatePropertiesStream.close();
    
    Configuration configuration = new Configuration();
    configuration.addProperties(hibernateProperties);
    
    for(Class<?> hibernateClass:hibernateMappedClasses)
    {
      configuration.addClass(hibernateClass);
    }
    
    ServiceRegistry registry = 
        new ServiceRegistryBuilder().applySettings(
            configuration.getProperties()).buildServiceRegistry();
    
    SessionFactory sessionFactory = 
        configuration.buildSessionFactory(registry);
    
    return sessionFactory;
  }
  
}

