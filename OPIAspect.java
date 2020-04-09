/**
 *
 */

package com.nortelnetworks.ims.base.prov.ws.fw.opi.aspects;

import com.nortelnetworks.ims.base.prov.debug.OpiDebug;
import com.nortelnetworks.ims.base.prov.opi.server.SessionManager;
import com.nortelnetworks.ims.base.prov.ws.fw.opi.handler.*;
import com.nortelnetworks.ims.platform.shared.ws.api.ProvisioningException;
import com.nortelnetworks.mcp.base.fault.api.Swer;
import com.nortelnetworks.mcp.ne.base.flightrecorder.FlightRecord;
import com.nortelnetworks.mcp.ne.base.flightrecorder.FlightRecorder;
import org.jboss.aop.joinpoint.MethodInvocation;

import javax.jws.WebService;
import java.util.Calendar;
/**
 * The OPIAspect class is the Aspect for all OPI web service methods.
 * Every OPI method will be intercepted and this class will execute the pre and post
 * handlers for the methods.
 *
 * @author agarwal
 *
 */
public class OPIAspect {
   private static final String REQ_TYPE_PROV_CLIENT = "ProvClient";
   private static Object[] resultArray;
   /**
    * Method Interceptor for all web services in ims/app/prov/ws/services/../shared/*AdminIF.java
    *
    * @param invocation
    * @return
    * @throws Throwable
    */
   public Object methodAdvice(MethodInvocation invocation) throws Throwable {
      String methodName = invocation.getMethod().toString();
      FlightRecord flightRecord = null;
      long methodStartTimeInMillis = Calendar.getInstance().getTimeInMillis();
      OpiDebug.print("OPIAspect.methodAdvice accessing: " + methodName);
      boolean fromProvClient = false;
      boolean costCalculated = false;
      try {
         SessionManager.getMethodContext().setMethodName(invocation.getMethod().getName());
         SessionManager.getMethodContext().setSvcName((invocation.getTargetObject().getClass().getAnnotation(WebService.class)).serviceName());
         
         if(SessionManager.getRequestType()!=null) {
            fromProvClient = SessionManager.getRequestType().equalsIgnoreCase(REQ_TYPE_PROV_CLIENT);
         }
         if (fromProvClient){
            ProvOverloadHandler.getInstance().notifyMethodEntry(SessionManager.getMethodContext().getSvcName(), SessionManager.getMethodContext().getMethodName());
         }
         //this flag is set not to decrease neCost in case of exceptions
         costCalculated = true;
         WSMethodPreInvokeHandler.getInstance().handle(SessionManager.getMethodContext().getSvcName(),
                                                       SessionManager.getMethodContext().getMethodName(),
                                                       invocation.getArguments());
         OpiDebug.print("OPIAspect.methodAdvice pre invoke success");

         Object obj = invocation.invokeNext();
         resultArray = new Object[]{obj};
         OpiDebug.print("OPIAspect.methodAdvice method invoke success");
         WSMethodPostInvokeSuccessHandler.getInstance().handle(SessionManager.getMethodContext().getSvcName(),
                                                               SessionManager.getMethodContext().getMethodName(),
                                                               invocation.getArguments(), resultArray);
         OpiDebug.print("OPIAspect.methodAdvice post invoke success in " + (Calendar.getInstance().getTimeInMillis() - methodStartTimeInMillis)/1000.0 + " seconds for " + methodName );
         return obj;
      }
      catch (ProvisioningException pe) {
         invokeFailureHandler(methodStartTimeInMillis,invocation, pe);
         throw pe;
      }
      catch (RuntimeException re) {
         invokeFailureHandler(methodStartTimeInMillis,invocation, re);
         Swer.report(re);
         throw new ProvisioningException(re.getMessage(), "WSCommon.000003");
      }
      catch (Exception e) {
         invokeFailureHandler(methodStartTimeInMillis,invocation, e);
         throw new ProvisioningException(e.getMessage(), "WSCommon.000003");
      }
      finally {
         if (FlightRecorder.isEnabled()) {
            flightRecord = FlightRecord.builder()
                    .methodName(methodName)
                    .parameters(invocation.getArguments())
                    .startTime(methodStartTimeInMillis)
                    .results(resultArray)
                    .stopTime(Calendar.getInstance().getTimeInMillis())
                    .build();
            FlightRecorder.log(FlightRecorder.jsonConverter(flightRecord));
         }
         if (fromProvClient && costCalculated){
            ProvOverloadHandler.getInstance().notifyMethodExit(SessionManager.getMethodContext().getSvcName(), SessionManager.getMethodContext().getMethodName());
         }
         WSMethodPostInvokeCleanup.getInstance().cleanup(SessionManager.getMethodContext().getSvcName(),
                                                         SessionManager.getMethodContext().getMethodName());
      }
   }

   private void invokeFailureHandler(long methodStartTimeInMillis, MethodInvocation invocation, Exception e) throws ProvisioningException
   {
      OpiDebug.print("OPIAspect.methodAdvice method invoke failure in " + (Calendar.getInstance().getTimeInMillis() - methodStartTimeInMillis)/1000.0 + " seconds for " + invocation.getMethod().toString() , e);
      WSMethodPostInvokeFailureHandler.getInstance().handle(SessionManager.getMethodContext().getSvcName(),
                                                            SessionManager.getMethodContext().getMethodName(),
                                                            invocation.getArguments());
   }
}


