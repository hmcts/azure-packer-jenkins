#!/bin/bash
sudo gem install bundler -p http://proxyout.reform.hmcts.net:8080/ --no-ri --no-rdoc
cd /tmp/tests
/usr/local/bin/bundle install --path=vendor
sudo /usr/local/bin/bundle exec rake spec