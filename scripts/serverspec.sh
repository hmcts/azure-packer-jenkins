#!/bin/bash
sudo gem install bundler -p http://proxyout.reform.hmcts.net:8080/ --no-ri --no-rdoc
cd /tmp/tests
bundle install --path=vendor
bundle exec rake spec