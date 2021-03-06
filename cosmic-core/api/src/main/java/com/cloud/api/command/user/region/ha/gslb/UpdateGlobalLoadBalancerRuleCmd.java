package com.cloud.api.command.user.region.ha.gslb;

import com.cloud.api.APICommand;
import com.cloud.api.ApiConstants;
import com.cloud.api.ApiErrorCode;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.GlobalLoadBalancerResponse;
import com.cloud.context.CallContext;
import com.cloud.event.EventTypes;
import com.cloud.region.ha.GlobalLoadBalancerRule;
import com.cloud.region.ha.GlobalLoadBalancingRulesService;
import com.cloud.user.Account;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@APICommand(name = "updateGlobalLoadBalancerRule", description = "update global load balancer rules.", responseObject = GlobalLoadBalancerResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateGlobalLoadBalancerRuleCmd extends BaseAsyncCmd {
    public static final Logger s_logger = LoggerFactory.getLogger(GlobalLoadBalancerResponse.class.getName());

    private static final String s_name = "updategloballoadbalancerruleresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////
    @Inject
    public GlobalLoadBalancingRulesService _gslbService;
    @Parameter(name = ApiConstants.ID,
            type = CommandType.UUID,
            entityType = GlobalLoadBalancerResponse.class,
            required = true,
            description = "the ID of the global load balancer rule")
    private Long id;
    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "the description of the load balancer rule", length = 4096)
    private String description;
    @Parameter(name = ApiConstants.GSLB_LB_METHOD,
            type = CommandType.STRING,
            required = false,
            description = "load balancer algorithm (roundrobin, leastconn, proximity) "
                    + "that is used to distributed traffic across the zones participating in global server load balancing, if not specified defaults to 'round robin'")
    private String algorithm;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////
    @Parameter(name = ApiConstants.GSLB_STICKY_SESSION_METHOD,
            type = CommandType.STRING,
            required = false,
            description = "session sticky method (sourceip) if not specified defaults to sourceip")
    private String stickyMethod;

    public String getDescription() {
        return description;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getGslbMethod() {
        return algorithm;
    }

    public String getStickyMethod() {
        return stickyMethod;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Global Load balancer Id: " + getId());
        final GlobalLoadBalancerRule gslbRule = _gslbService.updateGlobalLoadBalancerRule(this);
        if (gslbRule != null) {
            final GlobalLoadBalancerResponse response = _responseGenerator.createGlobalLoadBalancerResponse(gslbRule);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update global load balancer rule");
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        final GlobalLoadBalancerRule lb = _entityMgr.findById(GlobalLoadBalancerRule.class, getId());
        if (lb != null) {
            return lb.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_GLOBAL_LOAD_BALANCER_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "updating global load balancer rule";
    }
}
