#!/usr/bin/python

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.log import setLogLevel
from mininet.node import RemoteController
from mininet.cli import CLI
from mininet.node import Node

class MyTopo( Topo ):

    def __init__( self ):

        Topo.__init__( self )

        test = self.addHost('test', ip='192.168.44.101/24', mac='ea:e9:78:fb:fd:01')

        A = self.addHost('A', ip='0.0.0.0', mac='ea:e9:78:fb:fd:aa')
        B = self.addHost('B', ip='0.0.0.0', mac='ea:e9:78:fb:fd:bb')
        C = self.addHost('C', ip='0.0.0.0', mac='ea:e9:78:fb:fd:cc')

        portal = self.addHost('portal', ip='192.168.44.200/24', mac='ea:e9:78:fb:fd:2d')
        dhcp = self.addHost('dhcp', ip='192.168.44.201/24')

        s1 = self.addSwitch('s1', protocols='OpenFlow13')
        s2 = self.addSwitch('s2', protocols='OpenFlow13')
        s3 = self.addSwitch('s3', protocols='OpenFlow13')

        self.addLink(s1, portal, port1=1)
        self.addLink(s1, dhcp, port1=2)
        self.addLink(s1, test, port1=3)

        self.addLink(s1, s2, port1=4, port2=1)
        self.addLink(s1, s3, port1=5, port2=1)

        self.addLink(s2, A, port1=2)
        self.addLink(s2, B, port1=3)
        self.addLink(s2, C, port1=4)

        #self.addLink(s3, h2, port1=2)
        #self.addLink(s3, h3, port1=3)
        #self.addLink(s3, h4, port1=4)


def run():
    topo = MyTopo()
    net = Mininet(topo=topo, controller=None)
    net.addController('c0', controller=RemoteController, ip='127.0.0.1', port=6633)
    
    print("=== Add root ===")
    root = Node( 'root', inNamespace=False )
    intf = net.addLink( root, net['s1'] ).intf1
    root.setIP( '192.168.44.100/32', intf=intf )

    net.start()

    print("=== Run apache server ===")
    portal = net.getNodeByName('portal')
    portal.cmdPrint('/etc/init.d/apache2 restart')
    
    print("=== Run DHCP server ===")
    dhcp = net.getNodeByName('dhcp')
    dhcp.cmdPrint('service isc-dhcp-server restart &')

    print("=== Request IP ===")
    for host in net.hosts:
        if str(host) != 'portal' and str(host) != 'dhcp' and str(host) != 'test':
            host.cmdPrint('dhclient ' + host.defaultIntf().name)

    print("=== Open simple HTTP server ===")
    test = net.getNodeByName('test')
    test.cmdPrint('python -m SimpleHTTPServer 80 &')

    print("=== Set route ===")
    for route in [ '192.168.44.0/24' ]:
        root.cmd( 'route add -net ' + route + ' dev ' + str( intf ))

    CLI(net)
    net.stop()

if __name__ == '__main__':
    setLogLevel('info')
    run()

