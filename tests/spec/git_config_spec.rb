require 'spec_helper'

describe file('/home/dynjenkins/.gitconfig') do
  it { should be_file }
end