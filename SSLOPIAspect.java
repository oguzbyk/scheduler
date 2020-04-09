package com.nortelnetworks.ims.base.prov.ws.fw.opi.aspects;

import javax.jws.WebService;

import org.jboss.aop.joinpoint.MethodInvocation;

import com.nortelnetworks.mcp.base.fault.api.Swer;

import com.nortelnetworks.ims.base.prov.common.util.ProvLogger;
import com.nortelnetworks.ims.base.prov.debug.OpiDebug;
import com.nortelnetworks.ims.base.prov.opi.server.SessionManager;
import com.nortelnetworks.ims.base.prov.opi.shared.ProvisionException;
import com.nortelnetworks.ims.base.prov.opi.util.TransactionUtil;
import com.nortelnetworks.ims.base.prov.ws.fw.common.handler.SSLDataSyncHandler;
import com.nortelnetworks.ims.platform.shared.ws.api.ProvisioningException;
import com.nortelnetworks.ims.platform.shared.ws.api.SSLProvisioningException;

/**
 * The SSLOPIAspect class is the Aspect for SSL web service methods. Every SSL
 * service method will be intercepted and this class will keep transaction and
 * for the methods.
 *
 * @author oaslan
 * @see SSLOPIAspect
 */

public class SSLOPIAspect
{
   /**
    * Method Interceptor for SSLAdminIF.java
    *
    * @param invocation
    * @return
    * @throws Throwable
    */
   public Object methodAdvice(MethodInvocation invocation) throws Throwable
   {
      try
      {
         String methodName = invocation.getMethod().getName();
         String svcName = (invocation.getTargetObject().getClass().getAnnotation(WebService.class)).serviceName();

         SessionManager.getMethodContext().setSSLMethodName(methodName);
         SessionManager.getMethodContext().setSSLSvcName(svcName);
         SessionManager.getMethodContext().setSSLOPI(true);

         TransactionUtil.getInstance().createAndInitializeTransaction(this);
         Object obj = invocation.invokeNext();
         TransactionUtil.getInstance().handleSucessfulTransaction(this);

         SessionManager.getMethodContext().setSSLOPI(false);
         SSLDataSyncHandler.getInstance().sendPostInvokeSuccessDS(invocation.getArguments(), svcName, methodName);

         ProvLogger.getInstance().sslPassLog(SessionManager.getMethodContext().getSSLMethodName(), invocation.getArguments());

         return obj;
      }
      catch (SSLProvisioningException pe)
      {
         invokeFailureHandler(invocation, pe);
         throw pe;
      }
      catch (RuntimeException re) {
         invokeFailureHandler(invocation, re);
         Swer.report(re);
         throw new SSLProvisioningException(re.getMessage(), "WSCommon.000003");
      }
      catch (Exception e) {
         invokeFailureHandler(invocation, e);
         throw new SSLProvisioningException(e.getMessage(), "WSCommon.000003");
      }
   }

   private void invokeFailureHandler(MethodInvocation invocation, Exception e) throws SSLProvisioningException
   {
      OpiDebug.print("SSLOPIAspect.methodAdvice exception", e);

      try
      {
         TransactionUtil.getInstance().handleFailedTransaction(this);
         ProvLogger.getInstance().sslFailLog(SessionManager.getMethodContext().getSSLMethodName(), invocation.getArguments());
      }
      catch (ProvisionException pe)
      {
         throw new SSLProvisioningException(pe.getMessage());
      }
      catch (ProvisioningException ppe)
      {
         throw new SSLProvisioningException(ppe.getMessage());
      }
   }
}
