# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.box = "./alpine-3.2.3.box"
  config.vm.hostname = "forms"
  config.vm.synced_folder "./", "/home/vagrant/app"
  config.vm.network :private_network, ip: "10.0.10.10"

  config.ssh.shell = "ash"

  config.dns.tld = "development.beardandcode.com"

  config.vm.provision "shell", inline: <<-SHELL
    echo 'http://distrib-coffee.ipsl.jussieu.fr/pub/linux/alpine/alpine/edge/testing' >> /etc/apk/repositories
    sudo apk update
    sudo apk add bash openjdk8

    echo 'export JAVA_HOME="/usr/lib/jvm/default-jvm"' >> .profile

    wget 'https://raw.githubusercontent.com/bagder/ca-bundle/master/ca-bundle.crt'
    wget 'https://java-keyutil.googlecode.com/files/keyutil-0.4.0.jar'

    sudo java -jar keyutil-0.4.0.jar --import --password changeit --import-pem-file ca-bundle.crt --keystore-file /usr/lib/jvm/default-jvm/jre/lib/security/cacerts

    rm keyutil-0.4.0.jar ca-bundle.crt

    echo 'export PATH="$HOME/bin:$PATH"' >> .profile
    echo 'export LEIN_REPL_HOST="0.0.0.0"' >> .profile
    echo 'export PORT=8080' >> .profile
    mkdir .lein
    echo '{:user {:plugins [[cider/cider-nrepl "0.8.2"]]}}' >> .lein/profiles.clj
    mkdir bin
    cd bin
    wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
    chmod 755 lein
    cd ..
    chown vagrant:vagrant .profile .lein .lein/profiles.clj bin bin/lein
    
    sudo apk add iptables
    sudo rc-update add iptables
    sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-ports 8080
    sudo /etc/init.d/iptables save
  SHELL
end
