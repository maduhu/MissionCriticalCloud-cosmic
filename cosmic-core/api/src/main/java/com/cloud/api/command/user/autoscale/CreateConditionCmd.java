package com.cloud.api.command.user.autoscale;

import com.cloud.api.APICommand;
import com.cloud.api.ApiCommandJobType;
import com.cloud.api.ApiConstants;
import com.cloud.api.ApiErrorCode;
import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ConditionResponse;
import com.cloud.api.response.CounterResponse;
import com.cloud.api.response.DomainResponse;
import com.cloud.context.CallContext;
import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.as.Condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@APICommand(name = "createCondition", description = "Creates a condition", responseObject = ConditionResponse.class, entityType = {Condition.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateConditionCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = LoggerFactory.getLogger(CreateConditionCmd.class.getName());
    private static final String s_name = "conditionresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.COUNTER_ID, type = CommandType.UUID, entityType = CounterResponse.class, required = true, description = "ID of the Counter.")
    private long counterId;

    @Parameter(name = ApiConstants.RELATIONAL_OPERATOR, type = CommandType.STRING, required = true, description = "Relational Operator to be used with threshold.")
    private String relationalOperator;

    @Parameter(name = ApiConstants.THRESHOLD, type = CommandType.LONG, required = true, description = "Threshold value.")
    private Long threshold;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "the account of the condition. " + "Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "the domain ID of the account.")
    private Long domainId;

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void create() throws ResourceAllocationException {
        Condition condition = null;
        condition = _autoScaleService.createCondition(this);

        if (condition != null) {
            setEntityId(condition.getId());
            setEntityUuid(condition.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create condition.");
        }
    }

    @Override
    public void execute() {
        final Condition condition = _entityMgr.findById(Condition.class, getEntityId());
        final ConditionResponse response = _responseGenerator.createConditionResponse(condition);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    // /////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        final Long accountId = _accountService.finalyzeAccountId(accountName, domainId, null, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }

    public Long getCounterId() {
        return counterId;
    }

    public String getRelationalOperator() {
        return relationalOperator;
    }

    public String getAccountName() {
        if (accountName == null) {
            return CallContext.current().getCallingAccount().getAccountName();
        }

        return accountName;
    }

    public Long getDomainId() {
        if (domainId == null) {
            return CallContext.current().getCallingAccount().getDomainId();
        }
        return domainId;
    }

    public Long getThreshold() {
        return threshold;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_CONDITION_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating a condition";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.Condition;
    }
}
