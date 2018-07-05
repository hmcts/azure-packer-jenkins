require 'spec_helper'

describe file('/usr/local/bin/packer')
  it { should be_file }
end

describe file('/usr/local/bin/terraform')
  it { should be_file }
end