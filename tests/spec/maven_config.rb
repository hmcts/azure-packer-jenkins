require 'spec_helper'

describe file('/home/dynjenkins/.m2/settings.xml') do
  it { should be_file }
  it { should be_owned_by('dynjenkins') }
end