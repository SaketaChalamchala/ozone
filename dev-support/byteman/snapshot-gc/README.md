# Steps to run the Byteman Snapshot GC Test

This directory contains Byteman rule to run the SNapshot GC test in Line #8.
Rule file: `kds-purge-aos-snapshot-create-race.btm`. Test details are available in the rule file.

```
## Start a cluster - ozonesecureha already runs with byteman java option 
## OZONE_SERVER_OPTS="-javaagent:/opt/byteman.jar=listener:true,address:0.0.0.0,port:9091"
cd hadoop-ozone/dist/target/ozone-*-SNAPSHOT/compose/ozonesecure-ha
docker compose up -d

## Login to s3g terminal
kinit -kt /etc/security/keytabs/om.keytab om/om
ozone admin om roles
# Get the leader om, say om2

# Add the byteman rule to om2
bmsubmit -p 9091 -h om2 /opt/hadoop/share/ozone/byteman/snapshot-gc/kds-purge-aos-snapshot-create-race.btm

# Create and delete a key
ozone sh volume create /vol1
ozone sh bucket create /vol1/bucket1 -l OBJECT_STORE
ozone sh key put /vol1/bucket1/key1 /etc/hosts
ozone sh key delete /vol1/bucket1/key1

## Check om2 logs for the following lines
"BYTEMAN: KeyDeletingService.submitPurgeKeysRequest() called"
"BYTEMAN: KeyDeletingService.submitPurgeRequest called" ## This part is not working. The linking of "snapshotCreated" flag seems to be broken. Need to figure out why
"BYTEMAN: STEP 1 - Pausing KeyDeletingService before submitPurgeRequest"
"BYTEMAN: Waiting for snapshot creation..."

## Create snapshot
ozone sh snapshot create vol1/bucket1 snap1

## Check om2 logs for the following lines
"BYTEMAN: STEP 2 - Snapshot added to chain"
"BYTEMAN: STEP 3 - Snapshot created, resuming KeyDeletingService"
"BYTEMAN: STEP 4 - OMKeyPurgeRequest.validateAndUpdateCache completed"
"BYTEMAN: SUCCESS - Purge request failed as expected"

docker compose down
```
