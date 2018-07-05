require 'spec_helper'

describe package('docker-ce') do
  it { should be_installed }
end

describe package('jq') do
  it { should be_installed }
end
