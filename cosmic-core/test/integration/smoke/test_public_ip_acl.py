import logging
from marvin.cloudstackAPI import *
from marvin.cloudstackTestCase import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.lib.utils import *
from nose.plugins.attrib import attr

class TestPublicIpAcl(cloudstackTestCase):

    attributes = {
        'account': {
            'email': 'e.cartman@southpark.com',
            'firstname': 'Eric',
            'lastname': 'Cartman',
            'username': 'e.cartman',
            'password': 'southpark'
        },
        'default_offerings': {
            'vpc': 'Default VPC offering',
            'redundant_vpc': 'Redundant VPC offering',
            'network': 'DefaultIsolatedNetworkOfferingForVpcNetworks',
            'virtual_machine': 'Small Instance'
        },
        'vpcs': {
            'vpc1': {
                'name': 'vpc1',
                'displaytext': 'vpc1',
                'cidr': '10.1.0.0/16'
            }
        },
        'networks': {
            'network1': {
                'name': 'network1',
                'displaytext': 'network1',
                'gateway': '10.1.1.1',
                'netmask': '255.255.255.0'
            }
        },
        'vms': {
            'vm1': {
                'name': 'vm1',
                'displayname': 'vm1'
            }
        },
        'nat_rule': {
            'protocol': 'TCP',
            'publicport': 22,
            'privateport': 22
        },
        'acls': {
            'acl1': {
                'name': 'acl1',
                'description': 'acl1',
                'entries': {
                    'entry1': {
                        'protocol': 'TCP',
                        'action': 'Allow',
                        'traffictype': 'Ingress',
                        'startport': 22,
                        'endport': 22
                    }
                }
            },
            'acl2': {
                'name': 'acl2',
                'description': 'acl2',
                'entries': {
                    'entry2': {
                        'protocol': 'TCP',
                        'action': 'Deny',
                        'traffictype': 'Ingress',
                        'startport': 22,
                        'endport': 22
                    }
                }
            }
        }
    }

    @classmethod
    def setUpClass(cls):

        cls.test_client = super(TestPublicIpAcl, cls).getClsTestClient()
        cls.api_client = cls.test_client.getApiClient()

        cls.class_cleanup = []

        cls.logger = logging.getLogger('TestPublicIpAcl')
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(logging.StreamHandler())

    @classmethod
    def setup_infra(cls, redundant=False):

        if len(cls.class_cleanup) > 0:
            cleanup_resources(cls.api_client, cls.class_cleanup, cls.logger)
            cls.class_cleanup = []

        cls.zone = get_zone(cls.api_client, cls.test_client.getZoneForTests())
        cls.logger.debug("[TEST] Zone '%s' selected" % cls.zone.name)

        cls.domain = get_domain(cls.api_client)
        cls.logger.debug("[TEST] Domain '%s' selected" % cls.domain.name)

        cls.template = get_template(
            cls.api_client,
            cls.zone.id)

        cls.logger.debug("[TEST] Template '%s' selected" % cls.template.name)

        cls.account = Account.create(
            cls.api_client,
            cls.attributes['account'],
            admin=True,
            domainid=cls.domain.id)

        cls.class_cleanup += [cls.account]
        cls.logger.debug("[TEST] Account '%s' created", cls.account.name)

        cls.vpc_offering = cls.get_default_redundant_vpc_offering() if redundant else cls.get_default_vpc_offering()
        cls.logger.debug("[TEST] VPC Offering '%s' selected", cls.vpc_offering.name)

        cls.network_offering = cls.get_default_network_offering()
        cls.logger.debug("[TEST] Network Offering '%s' selected", cls.network_offering.name)

        cls.virtual_machine_offering = cls.get_default_virtual_machine_offering()
        cls.logger.debug("[TEST] Virtual Machine Offering '%s' selected", cls.virtual_machine_offering.name)

        cls.default_allow_acl = cls.get_default_acl('default_allow')
        cls.logger.debug("[TEST] ACL '%s' selected", cls.default_allow_acl.name)

        cls.default_deny_acl = cls.get_default_acl('default_deny')
        cls.logger.debug("[TEST] ACL '%s' selected", cls.default_deny_acl.name)

        cls.vpc1 = VPC.create(cls.api_client,
            cls.attributes['vpcs']['vpc1'],
            vpcofferingid=cls.vpc_offering.id,
            zoneid=cls.zone.id,
            domainid=cls.domain.id,
            account=cls.account.name)
        cls.logger.debug("[TEST] VPC '%s' created, CIDR: %s", cls.vpc1.name, cls.vpc1.cidr)

        cls.network1 = Network.create(cls.api_client,
            cls.attributes['networks']['network1'],
            networkofferingid=cls.network_offering.id,
            aclid=cls.default_allow_acl.id,
            vpcid=cls.vpc1.id,
            zoneid=cls.zone.id,
            domainid=cls.domain.id,
            accountid=cls.account.name)
        cls.logger.debug("[TEST] Network '%s' created, CIDR: %s, Gateway: %s", cls.network1.name, cls.network1.cidr, cls.network1.gateway)

        cls.vm1 = VirtualMachine.create(cls.api_client,
            cls.attributes['vms']['vm1'],
            templateid=cls.template.id,
            serviceofferingid=cls.virtual_machine_offering.id,
            networkids=[cls.network1.id],
            zoneid=cls.zone.id,
            domainid=cls.domain.id,
            accountid=cls.account.name)
        cls.logger.debug("[TEST] VM '%s' created, Network: %s, IP %s", cls.vm1.name, cls.network1.name, cls.vm1.nic[0].ipaddress)

        cls.public_ip1 = PublicIPAddress.create(cls.api_client,
            zoneid=cls.zone.id,
            domainid=cls.account.domainid,
            accountid=cls.account.name,
            vpcid=cls.vpc1.id,
            networkid=cls.network1.id)
        cls.logger.debug("[TEST] Public IP '%s' acquired, VPC: %s, Network: %s", cls.public_ip1.ipaddress.ipaddress, cls.vpc1.name, cls.network1.name)

        cls.nat_rule1 = NATRule.create(cls.api_client,
            cls.vm1,
            cls.attributes['nat_rule'],
            vpcid=cls.vpc1.id,
            networkid=cls.network1.id,
            ipaddressid=cls.public_ip1.ipaddress.id)
        cls.logger.debug("[TEST] Port Forwarding Rule '%s (%s) %s => %s' created",
            cls.nat_rule1.ipaddress,
            cls.nat_rule1.protocol,
            cls.nat_rule1.publicport,
            cls.nat_rule1.privateport)

    @classmethod
    def tearDownClass(cls):

        try:
            cleanup_resources(cls.api_client, cls.class_cleanup, cls.logger)

        except Exception as e:
            raise Exception("Exception: %s" % e)

    def setUp(self):

        self.method_cleanup = []

    def tearDown(self):

        try:
            cleanup_resources(self.api_client, self.method_cleanup, self.logger)

        except Exception as e:
            raise Exception("Exception: %s" % e)

    def test_acls(self, first_time_retries=2):
        self.define_acl(self.default_allow_acl)
        self.test_connectivity(retries=first_time_retries)
        self.define_acl(self.default_deny_acl)
        self.test_no_connectivity()
        self.define_custom_acl('acl1', 'entry1')
        self.test_connectivity()
        self.define_custom_acl('acl2', 'entry2')
        self.test_no_connectivity()
        self.define_acl(self.default_allow_acl)
        self.test_connectivity()

    @attr(tags=['advanced'], required_hardware='true')
    def test_01(self):

        self.setup_infra(redundant=False)
        self.test_acls(first_time_retries=10)

    @attr(tags=['advanced'], required_hardware='true')
    def test_02(self):

        self.cleanup_vpc()
        self.test_acls()

    @attr(tags=['advanced'], required_hardware='true')
    def test_03(self):

        self.setup_infra(redundant=True)
        self.test_acls(first_time_retries=10)

    @attr(tags=['advanced'], required_hardware='true')
    def test_04(self):

        self.cleanup_vpc()
        self.test_acls()

    @attr(tags=['advanced'], required_hardware='true')
    def test_05(self):

        self.stop_master_router(self.vpc1)
        self.test_acls()

    def test_connectivity(self, retries=2):

        try:
            self.vm1.get_ssh_client(ipaddress=self.public_ip1.ipaddress.ipaddress, reconnect=True, retries=retries)
            self.logger.debug('[TEST] Ensure connectivity: OK')

        except Exception as e:
            raise Exception("Exception: %s" % e)

    def test_no_connectivity(self):

        failed = False
        try:
            self.vm1.get_ssh_client(ipaddress=self.public_ip1.ipaddress.ipaddress, reconnect=True, retries=2)

        except Exception as e:
            self.logger.debug('[TEST] Ensure no connectivity: OK')
            failed = True

        self.assertTrue(failed)

    def cleanup_vpc(self):

        self.logger.debug("[TEST] Restarting VPC '%s' with 'cleanup=True'", self.vpc1.name)
        self.vpc1.restart(self.api_client, True)
        self.logger.debug("[TEST] VPC '%s' restarted", self.vpc1.name)

    def define_acl(self, acl):

        try:
            command = replaceNetworkACLList.replaceNetworkACLListCmd()
            command.aclid = acl.id
            command.publicipid = self.public_ip1.ipaddress.id
            response = self.api_client.replaceNetworkACLList(command)

        except Exception as e:
            raise Exception("Exception: %s" % e)

        self.assertTrue(response.success)
        self.logger.debug("[TEST] Public IP '%s' ACL replaced with '%s'", self.public_ip1.ipaddress.ipaddress, acl.name)

    def define_custom_acl(self, acl_config, acl_entry_config):

        acl = NetworkACLList.create(self.api_client,
            self.attributes['acls'][acl_config],
            vpcid=self.vpc1.id)

        NetworkACL.create(self.api_client,
            self.attributes['acls'][acl_config]['entries'][acl_entry_config],
            networkid=self.network1.id,
            aclid=acl.id)

        self.define_acl(acl)

    def stop_master_router(self, vpc):

        self.logger.debug("[TEST] Stopping Master Router of VPC '%s'...", vpc.name)
        routers = list_routers(self.api_client, domainid=self.domain.id, account=self.account.name, vpcid=vpc.id)
        for router in routers:
            if router.redundantstate == 'MASTER':
                cmd = stopRouter.stopRouterCmd()
                cmd.id = router.id
                cmd.forced = 'true'
                self.api_client.stopRouter(cmd)
                break

        for router in routers:
            if router.state == 'Running':
                hosts = list_hosts(self.api_client, zoneid=router.zoneid, type='Routing', state='Up', id=router.hostid)
                self.assertTrue(isinstance(hosts, list))
                host = next(iter(hosts or []), None)

                try:
                    host.user, host.passwd = get_host_credentials(self.config, host.ipaddress)
                    get_process_status(host.ipaddress, 22, host.user, host.passwd, router.linklocalip, "sh /opt/cloud/bin/checkrouter.sh ")

                except KeyError as e:
                    raise Exception("Exception: %s" % e)

        self.logger.debug("[TEST] Master Router of VPC '%s' stopped", vpc.name)

    @classmethod
    def get_default_vpc_offering(cls):

        offerings = list_vpc_offerings(cls.api_client)
        offerings = [offering for offering in offerings if offering.name == cls.attributes['default_offerings']['vpc']]
        return next(iter(offerings or []), None)

    @classmethod
    def get_default_redundant_vpc_offering(cls):

        offerings = list_vpc_offerings(cls.api_client)
        offerings = [offering for offering in offerings if offering.name == cls.attributes['default_offerings']['redundant_vpc']]
        return next(iter(offerings or []), None)

    @classmethod
    def get_default_network_offering(cls):

        offerings = list_network_offerings(cls.api_client)
        offerings = [offering for offering in offerings if offering.name == cls.attributes['default_offerings']['network']]
        return next(iter(offerings or []), None)

    @classmethod
    def get_default_virtual_machine_offering(cls):

        offerings = list_service_offering(cls.api_client)
        offerings = [offering for offering in offerings if offering.name == cls.attributes['default_offerings']['virtual_machine']]
        return next(iter(offerings or []), None)

    @classmethod
    def get_default_acl(cls, name):

        acls = NetworkACLList.list(cls.api_client)
        acls = [acl for acl in acls if acl.name == name]
        return next(iter(acls or []), None)

    @classmethod
    def get_default_allow_vpc_acl(cls, vpc): # check if it's better to get the ACL from the VPC

        acls = NetworkACLList.list(cls.api_client, vpcid=vpc.id)
        acls = [acl for acl in acls if acl.name == 'default_allow']
        return next(iter(acls or []), None)
