package org.apache.rocketmq.client.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.Getter;
import lombok.Setter;
import org.apache.rocketmq.client.remoting.AccessCredential;
import org.apache.rocketmq.client.remoting.Address;
import org.apache.rocketmq.client.remoting.CredentialsObservable;
import org.apache.rocketmq.client.remoting.Endpoints;
import org.apache.rocketmq.client.route.AddressScheme;
import org.apache.rocketmq.utility.RemotingUtil;
import org.apache.rocketmq.utility.UtilAll;

public class ClientConfig implements CredentialsObservable {
    private static final String CLIENT_ID_SEPARATOR = "@";

    @Getter
    protected final ReadWriteLock nameServerLock;

    private String groupName = "";
    // TODO: fix region_id here.
    @Getter
    private String regionId = "cn-hangzhou";
    @Getter
    private String arn = "";
    @Getter
    private String tenantId = "";
    // TODO: fix service name here.
    @Setter
    @Getter
    private String serviceName = "aone";
    @Getter
    private AccessCredential accessCredential = null;

    @Getter
    private final String clientId;

    @GuardedBy("nameServerLock")
    @Getter
    private final List<Endpoints> namesrvAddr;

    @Getter
    @Setter
    private boolean messageTracingEnabled = false;
    private boolean rpcTracingEnabled = false;

    public ClientConfig(String groupName) {
        this.groupName = groupName;
        this.namesrvAddr = new ArrayList<Endpoints>();
        this.nameServerLock = new ReentrantReadWriteLock();

        StringBuilder sb = new StringBuilder();
        final String clientIp = RemotingUtil.getLocalAddress();
        sb.append(clientIp);
        sb.append(CLIENT_ID_SEPARATOR);
        sb.append(UtilAll.processId());
        sb.append(CLIENT_ID_SEPARATOR);
        sb.append(System.nanoTime());
        this.clientId = sb.toString();
    }

    protected void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    protected String getGroupName() {
        return groupName;
    }

    public void setNamesrvAddr(String namesrv) {
        nameServerLock.writeLock().lock();
        try {
            this.namesrvAddr.clear();
            final String[] addressArray = namesrv.split(";");
            for (String address : addressArray) {
                // TODO: check name server format, IPv4/IPv6/DOMAIN_NAME
                final String[] split = address.split(":");
                String host = split[0];
                int port = Integer.parseInt(split[1]);

                List<Address> addresses = new ArrayList<Address>();
                addresses.add(new Address(host, port));
                this.namesrvAddr.add(new Endpoints(AddressScheme.IPv4, addresses));
            }
        } finally {
            nameServerLock.writeLock().unlock();
        }
    }

    // TODO: not allowed to update after client instance started(override in producer and consumer)
    public void setArn(String arn) {
        checkNotNull(arn, "Abstract resource name is null, please set it.");
        this.arn = arn;
    }


    // TODO: not allowed to update after client instance started(override in producer and consumer)
    public void setAccessCredential(AccessCredential accessCredential) {
        checkNotNull(accessCredential, "Access Credential is null, please set it.");
        this.accessCredential = accessCredential;
    }
}
