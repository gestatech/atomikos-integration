package com.atomikos.hibernate;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.HibernateException;
import org.hibernate.service.jta.platform.internal.AbstractJtaPlatform;
import org.hibernate.service.jta.platform.spi.JtaPlatform;

/**
 * A Hibernate {@link JtaPlatform} that locates the Atomikos transaction
 * resources.
 *
 * @author Carl Harris
 */
public class AtomikosJtaPlatform extends AbstractJtaPlatform {

  private static final long serialVersionUID = -5696109290589577184L;

  /** JNDI name for the {@link TransactionManager} */
  public static final String TX_MANAGER_RESOURCE = 
      "java:/comp/env/TransactionManager";

  /** JNDI Name for the {@link UserTransaction} */
  public static final String USER_TX_RESOURCE = 
      "java:/comp/UserTransaction";
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected TransactionManager locateTransactionManager() {
    return (TransactionManager) locateResource(TX_MANAGER_RESOURCE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected UserTransaction locateUserTransaction() {
    return (UserTransaction) locateResource(USER_TX_RESOURCE);
  }

  /**
   * Locates a JNDI resource using the initial context
   * @param jndiName fully qualified name for the resource to locate
   * @return named resource
   * @throws HibernateException if the resource cannot be located due to
   *    a {@link NamingException}
   */
  private static Object locateResource(String jndiName) {
    try {
      Context initCtx = new InitialContext();
      return initCtx.lookup(jndiName);
    }
    catch (NamingException ex) {
      throw new HibernateException(ex);
    }
  }
  
}
