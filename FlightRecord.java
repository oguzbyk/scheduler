package com.nortelnetworks.mcp.ne.base.flightrecorder;

import com.nortelnetworks.mcp.base.collections.Array;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class FlightRecord {
    private String serviceName;
    private String methodName;
    private Object[] parameters;
    private Object[] results;
    private long startTime;
    private long stopTime;

}
