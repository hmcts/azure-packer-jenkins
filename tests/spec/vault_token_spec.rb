require 'spec_helper'

describe file('/home/dynjenkins/.vault-token') do
  it { should be_file }
end