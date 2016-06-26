package org.apache.cloudstack.api.command.admin.pod;

import com.cloud.dc.Pod;
import com.cloud.utils.Pair;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@APICommand(name = "listPods", description = "Lists all Pods.", responseObject = PodResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListPodsByCmd extends BaseListCmd {
    public static final Logger s_logger = LoggerFactory.getLogger(ListPodsByCmd.class.getName());

    private static final String s_name = "listpodsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = PodResponse.class, description = "list Pods by ID")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "list Pods by name")
    private String podName;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "list Pods by Zone ID")
    private Long zoneId;

    @Parameter(name = ApiConstants.ALLOCATION_STATE, type = CommandType.STRING, description = "list pods by allocation state")
    private String allocationState;

    @Parameter(name = ApiConstants.SHOW_CAPACITIES, type = CommandType.BOOLEAN, description = "flag to display the capacity of the pods")
    private Boolean showCapacities;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getPodName() {
        return podName;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getAllocationState() {
        return allocationState;
    }

    public Boolean getShowCapacities() {
        return showCapacities;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        final Pair<List<? extends Pod>, Integer> result = _mgr.searchForPods(this);
        final ListResponse<PodResponse> response = new ListResponse<>();
        final List<PodResponse> podResponses = new ArrayList<>();
        for (final Pod pod : result.first()) {
            final PodResponse podResponse = _responseGenerator.createPodResponse(pod, showCapacities);
            podResponse.setObjectName("pod");
            podResponses.add(podResponse);
        }

        response.setResponses(podResponses, result.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
}
