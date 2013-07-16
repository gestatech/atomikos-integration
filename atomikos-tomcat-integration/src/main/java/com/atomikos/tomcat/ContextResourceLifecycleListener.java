/*
 * File created on Jul 16, 2013 
 *
 * Copyright 2008-2011 Virginia Polytechnic Institute and State University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.atomikos.tomcat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;

/**
 * A {@link LifecycleListener} that provides a lifecycle callback to resources
 * in a web application's context that expose a public {@code close} method.
 *
 * @author Carl Harris
 */
public class ContextResourceLifecycleListener implements LifecycleListener {

  private static final String CONTEXT_RESOURCE_PATH = "java:/comp/env";
  private static final String GLOBAL_RESOURCE_PATH = "java:/";

  private static final Logger logger = 
      Logger.getLogger(ContextResourceLifecycleListener.class.getName());

  @Override
  public void lifecycleEvent(LifecycleEvent event) {
    if (!Lifecycle.CONFIGURE_STOP_EVENT.equals(event.getType())) {
      return;
    }
    try {
      Set<Object> contextResources = enumerateContext(
          CONTEXT_RESOURCE_PATH, null);
      Set<Object> globalResources = enumerateContext(
          GLOBAL_RESOURCE_PATH, CONTEXT_RESOURCE_PATH);

      Set<Object> resources = new HashSet<Object>();
      resources.addAll(contextResources);
      resources.removeAll(globalResources);
      closeResources(resources);
    }
    catch (NamingException ex) {
      logger.log(Level.SEVERE, ex.toString(), ex);
    }
  }

  private Set<Object> enumerateContext(String name,
      String excludedPath) throws NamingException {
    return enumerateContext(name, getNamingContext(name), excludedPath);
  }

  private Context getNamingContext(String name) throws NamingException {
    Context initCtx = new InitialContext();
    return (Context) initCtx.lookup(name);
  }
  
  private Set<Object> enumerateContext(String prefix, Context ctx,
      String excludedPath) throws NamingException {
    Set<Object> resources = new HashSet<Object>();
    NamingEnumeration<Binding> bindings = ctx.listBindings("");    
    while (bindings.hasMore()) {
      Binding binding = bindings.next();
      String name = prefix + binding.getName();
      Object value = ctx.lookup(binding.getName());
      if (excludedPath != null && name.startsWith(excludedPath)) {
        continue;
      }
      if (value instanceof Context) {
        resources.addAll(enumerateContext(name + "/", (Context) value, 
            excludedPath));
      }
      else {
        resources.add(value);
      }
    }
    return resources;
  }

  private void closeResources(Set<Object> resources) {
    
    for (Object resource : resources) {
      try {
        if (closeResource(resource, "close")) {
          logger.info("closed context resource: " + resource);
        }
      }
      catch (Exception ex) {
        logger.log(Level.WARNING, "failed to close resource: " + ex, ex);
      }
    }
  }

  private boolean closeResource(Object resource, String methodName) 
      throws IllegalAccessException, InvocationTargetException {
    try {
      Method method = resource.getClass().getMethod(methodName);
      method.invoke(resource);
      return true;
    }
    catch (NoSuchMethodException ex) {
      return false;
    }
  }

}
