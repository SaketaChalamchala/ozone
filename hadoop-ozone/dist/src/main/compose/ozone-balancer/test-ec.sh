#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#suite:balancer

set -u -o pipefail

COMPOSE_DIR="$( cd "$( dirname "${BASH_SOURCE0}" )" >/dev/null 2>&1 && pwd )"
export COMPOSE_DIR
export OM_SERVICE_ID="om"
export OM=om1
export SCM=scm1
export OZONE_REPLICATION_FACTOR=3

# shellcheck source=/dev/null
source "$COMPOSE_DIR/../testlib.sh"

start_docker_env
execute_robot_test ${OM} -v REPLICATION:rs-3-2-1024k -v TYPE:EC -v LOWER_LIMIT:0.7 -v UPPER_LIMIT:1.5 -N ozone-balancer-EC balancer/testBalancer.robot
