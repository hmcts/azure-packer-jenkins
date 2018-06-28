## I'm not actually sure this is necessary because it may come from jenkins,
## not from the CLI?

require 'spec_helper'

describe file('/home/dynjenkins/.gitconfig') do
  it { should be_file }
end