# Licensed to the Apache Software Foundation (ASF) under one or more
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
#
#
# Byteman rules for testing Snapshot GC scenarios
#

### Testing automation with robot framework -- in progress
*** Variables ***
${RULE1}    /opt/hadoop/share/ozone/byteman/snapshot-gc/kds-purge-aos-snapshot-create-race.btm
${VOLUME}
${BUCKET}
${KEY_NAME}
${SNAPSHOT_NAME}

*** Settings ***
Resource            ../ozone-fi/BytemanKeywords.robot
Resource            snapshot-setup.robot
Resource            ../lib/os.robot
Library             String
Library             BuiltIn
Test Timeout        10 minutes

*** Test Cases ***

KDS Purge Keys and AOS Snapshot create race    
    # Step 1: Create a key in a random volume and bucket
    ${volume} =     Create volume
    ${bucket} =     Create bucket       ${VOLUME}       FILE_SYSTEM_OPTIMIZED
    ${key_name} =   Create key          ${VOLUME}       ${BUCKET}       /etc/hosts
    
    # Step 2: Inject Byteman rules into OM
    Add Byteman Rule       om1    ${RULE1}
    
    # Step 3: Delete the key created in Step 1
    ${result} =     Execute             ozone sh key delete /${VOLUME}/${BUCKET}/${KEY_NAME}
                    Should not contain  ${result}       Failed
    
    # Step 4: Wait for KeyDeletingService to be paused at submitPurgeRequest
    Sleep    1min
    
    # Step 5: Create a snapshot on the key's bucket
    ${snapshot_name} =   Create snapshot     ${VOLUME}       ${BUCKET}

    # Step 6: Wait for OMKeyPurgeRequest processing to complete
    Sleep    1s

    # Step 8: Verify that the test result is SUCCESS
    ${result} =    Get Byteman Result    om1       KDS_Purge_AOS_Snapshot_Create_Race
                   Should contain  ${result}       testStatus=TEST_COMPLETED,testResult=SUCCESS

    # Step 9: Remove Byteman rules from OM
    Remove Byteman Rule         om1       ${RULE1}
