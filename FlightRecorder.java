package com.nortelnetworks.mcp.ne.base.flightrecorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nortelnetworks.mcp.base.debug.DebugConstants;
import com.nortelnetworks.mcp.base.fault.api.DebugLog;
import com.nortelnetworks.mcp.base.fault.api.DebugLogTemplate;
import com.nortelnetworks.mcp.base.fault.api.FaultTemplateFactory;
import com.nortelnetworks.mcp.base.util.debug.StackTrace;
import lombok.SneakyThrows;

public class FlightRecorder {

    /** No debugging data will be produced. */
    public final static int OFF = DebugConstants.OFF;

    /** Minimal useful set of debugging data should be produced. */
    public final static int TERSE = DebugConstants.TERSE;

    /** Standard set of debugging data should be produced. */
    public final static int NORMAL = DebugConstants.NORMAL;

    /**
     * Comprehensive set of debugging data should be produced. Examples include
     * message traces, state machine traces, etc.
     */
    public final static int VERBOSE = DebugConstants.VERBOSE;

    public static final String FAMILY ="FLIGHTRECORD";

    public static final DebugLogTemplate DEBUG_TERSE = FaultTemplateFactory.getInstance().getDebugLogTemplate(FAMILY,
            0, TERSE);
    public static final DebugLogTemplate DEBUG_NORMAL = FaultTemplateFactory.getInstance().getDebugLogTemplate(FAMILY,
            1, NORMAL);
    public static final DebugLogTemplate DEBUG_VERBOSE = FaultTemplateFactory.getInstance().getDebugLogTemplate(FAMILY,
            2, VERBOSE);

    static
    {
        FaultTemplateFactory.getInstance().setDebugLevel(FAMILY, OFF);
    }

    public static boolean isEnabled()
    {
        return (FaultTemplateFactory.getInstance().getDebugLevel(FAMILY) != OFF);
    }

    public static void enable()
    {
        FaultTemplateFactory.getInstance().setDebugLevel(FAMILY, VERBOSE);
    }

    public static void disable()
    {
        FaultTemplateFactory.getInstance().setDebugLevel(FAMILY, OFF);
    }

    /**
     * Sends information to the Flight Record Log (DebugLog for now)
     *
     * @param  logString  String that needs to be sent to the log
     */

    public static void log(String logString, int level)
    {
        if (FaultTemplateFactory.getInstance().getDebugLevel(FAMILY) >= level)
        {
            DebugLog.report(getTemplate(level), "[** FLIGHTRECORD **]=> " + logString);
        }
    }

    public static void log(String logString, java.sql.SQLException ex, int level)
    {
        if ( FaultTemplateFactory.getInstance().getDebugLevel(FAMILY) >= level)
        {
            StringBuffer buff = new StringBuffer(256);
            buff.append("[** FLIGHTRECORD **]=> ").append(logString);
            buff.append("\n").append(StackTrace.getStackTraceString(ex));
            DebugLog.report(getTemplate(level), buff.toString());

        }
    }

    public static void log(String logString)
    {
        log(logString, VERBOSE);
    }

    public static void log(String logString, java.sql.SQLException ex)
    {
        log(logString, ex, VERBOSE);
    }

    private static DebugLogTemplate getTemplate(int level) {
        switch (level) {
            case TERSE:
                return DEBUG_TERSE;
            case NORMAL:
                return DEBUG_NORMAL;
            case VERBOSE:
                return DEBUG_VERBOSE;
            case OFF:
            default:
                return DEBUG_NORMAL;
        }
    }

    @SneakyThrows
    public static String jsonConverter(FlightRecord flightRecord){
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writeValueAsString(flightRecord);
        return jsonString;
    }
}

