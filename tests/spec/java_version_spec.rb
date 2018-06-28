require 'spec_helper'

describe command('java -version') do
  its(:stdout) { should match /OpenJDK 64-Bit Server VM/ }
  its(:stdout) { should match /OpenJDK Runtime Environment/ }
end