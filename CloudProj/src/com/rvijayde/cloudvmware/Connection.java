package com.rvijayde.cloudvmware;


import com.vmware.common.annotations.After;
import com.vmware.common.annotations.Before;
import com.vmware.common.annotations.Option;
import com.vmware.vim25.*;

import java.net.URL;
import java.util.Map;

/**
 * @see ConnectedVimServiceBase
 */
public interface Connection {
    // getters and setters
    @Option(name = "url", systemProperty = "vimService.url", description = "full url to the vSphere WS SDK service")
    void setUrl(String url);

    String getUrl();

    String getHost();

    Integer getPort();

    @Option(name = "username", systemProperty = "connection.username", description = "username on remote system")
    void setUsername(String username);

    String getUsername();

    @Option(name = "password", systemProperty = "connection.password", description = "password on remote system")
    void setPassword(String password);

    String getPassword();

    VimService getVimService();

    VimPortType getVimPort();

    ServiceContent getServiceContent();

    UserSession getUserSession();

    String getServiceInstanceName();

    @SuppressWarnings("rawtypes")
	Map getHeaders();

    ManagedObjectReference getServiceInstanceReference();

    @Before
    Connection connect();

    boolean isConnected();

    @After
    Connection disconnect();

    URL getURL();
}
