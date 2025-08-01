# Licensed to the Apache Software Foundation (ASF) under one or moreD
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

*** Settings ***
Documentation       Test ozone admin container create command without kinit on a SCM HA secure cluster
Library             BuiltIn
Resource            ../lib/os.robot

*** Test Cases ***
Create container without kinit
    ${output} =         Execute And Ignore Error          ozone admin container create
                        Should contain        ${output}   Permission denied
