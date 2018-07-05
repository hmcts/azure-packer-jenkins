require 'spec_helper'

describe file('/home/dynjenkins/.vault-token') do
  it { should be_file }
  it { should be_owned_by('dynjenkins') }
  it { should be_mode 600 }
end