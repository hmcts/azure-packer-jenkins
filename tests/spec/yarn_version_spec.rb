require 'spec_helper'

describe command('yarn version') do
  its(:stdout) { should match /yarn version v/} 
end