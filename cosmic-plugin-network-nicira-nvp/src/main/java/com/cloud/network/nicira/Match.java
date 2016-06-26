//

//

package com.cloud.network.nicira;

/**
 *
 */
public class Match {
    private Integer protocol;
    private String sourceIpAddresses;
    private String destinationIpAddresses;
    private Integer sourcePort;
    private Integer destinationPort;
    private String ethertype = "IPv4";

    public Integer getProtocol() {
        return protocol;
    }

    public void setProtocol(final Integer protocol) {
        this.protocol = protocol;
    }

    public Integer getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(final Integer sourcePort) {
        this.sourcePort = sourcePort;
    }

    public Integer getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(final Integer destinationPort) {
        this.destinationPort = destinationPort;
    }

    public String getEthertype() {
        return ethertype;
    }

    public void setEthertype(final String ethertype) {
        this.ethertype = ethertype;
    }

    public String getSourceIpAddresses() {
        return sourceIpAddresses;
    }

    public void setSourceIpAddresses(final String sourceIpAddresses) {
        this.sourceIpAddresses = sourceIpAddresses;
    }

    public String getDestinationIpAddresses() {
        return destinationIpAddresses;
    }

    public void setDestinationIpAddresses(final String destinationIpAddresses) {
        this.destinationIpAddresses = destinationIpAddresses;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((destinationIpAddresses == null) ? 0 : destinationIpAddresses.hashCode());
        result = prime * result + ((destinationPort == null) ? 0 : destinationPort.hashCode());
        result = prime * result + ((ethertype == null) ? 0 : ethertype.hashCode());
        result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
        result = prime * result + ((sourceIpAddresses == null) ? 0 : sourceIpAddresses.hashCode());
        result = prime * result + ((sourcePort == null) ? 0 : sourcePort.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Match other = (Match) obj;
        if (destinationIpAddresses == null) {
            if (other.destinationIpAddresses != null) {
                return false;
            }
        } else if (!destinationIpAddresses.equals(other.destinationIpAddresses)) {
            return false;
        }
        if (destinationPort == null) {
            if (other.destinationPort != null) {
                return false;
            }
        } else if (!destinationPort.equals(other.destinationPort)) {
            return false;
        }
        if (ethertype == null) {
            if (other.ethertype != null) {
                return false;
            }
        } else if (!ethertype.equals(other.ethertype)) {
            return false;
        }
        if (protocol == null) {
            if (other.protocol != null) {
                return false;
            }
        } else if (!protocol.equals(other.protocol)) {
            return false;
        }
        if (sourceIpAddresses == null) {
            if (other.sourceIpAddresses != null) {
                return false;
            }
        } else if (!sourceIpAddresses.equals(other.sourceIpAddresses)) {
            return false;
        }
        if (sourcePort == null) {
            if (other.sourcePort != null) {
                return false;
            }
        } else if (!sourcePort.equals(other.sourcePort)) {
            return false;
        }
        return true;
    }
}
