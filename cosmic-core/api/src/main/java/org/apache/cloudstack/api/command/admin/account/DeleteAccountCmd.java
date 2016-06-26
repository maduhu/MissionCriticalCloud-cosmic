package org.apache.cloudstack.api.command.admin.account;

import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.user.User;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.region.RegionService;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@APICommand(name = "deleteAccount", description = "Deletes a account, and all users associated with this account", responseObject = SuccessResponse.class, entityType = {Account
        .class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class DeleteAccountCmd extends BaseAsyncCmd {
    public static final Logger s_logger = LoggerFactory.getLogger(DeleteAccountCmd.class.getName());
    private static final String s_name = "deleteaccountresponse";
    @Inject
    RegionService _regionService;
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = AccountResponse.class, required = true, description = "Account id")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public static String getStaticName() {
        return s_name;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getEventType() {
        return EventTypes.EVENT_ACCOUNT_DELETE;
    }

    @Override
    public String getEventDescription() {
        final User user = _responseGenerator.findUserById(getId());
        return (user != null ? ("deleting User " + user.getUsername() + " (id: " + user.getId() + ") and accountId = " + user.getAccountId())
                : "user delete, but this user does not exist in the system");
    }

    public Long getId() {
        return id;
    }

    @Override
    public Long getInstanceId() {
        return id;
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.Account;
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Account Id: " + getId());

        final boolean result = _regionService.deleteUserAccount(this);
        if (result) {
            final SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete user account and all corresponding users");
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        final Account account = CallContext.current().getCallingAccount();// Let's give the caller here for event logging.
        if (account != null) {
            return account.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }
}
