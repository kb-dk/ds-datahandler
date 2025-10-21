#!/bin/bash

#
# Simple script for harvesting from an OAI-PMH endpoint.
#
# Developed specifically to test harvesting from Preservica 6 & 7.
# No guarantees for general use.
#
#
# https://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ListRecords
#

###############################################################################
# CONFIG
###############################################################################


: ${CONFIG:="$1"}
: ${CONFIG:="oai_harvest.conf"}

if [[ -s "$CONFIG" ]]; then
    source "$CONFIG"     # Local overrides
fi
pushd ${BASH_SOURCE%/*} > /dev/null
if [[ -s "$CONFIG" ]]; then
    source "$CONFIG"     # Project overrides
fi
SHOME=`pwd`
: ${SERVER:=""} # https://example.com/OAI-PMH/
: ${FROM:="$(date +%Y-%m-%d)"} # Today's date
# : ${UNTIL:="$(date +%Y-%m-%d)"}   # Also today's date. No longer default set
: ${METADATA_PREFIX:="xip"}
: ${CONTENT_TYPE:="application/xml"}
: ${USER_PASS:=""} # user:password
: ${OUTPUT_PREFIX:="oai_$(date +%Y%m%d-%H%M)"}
: ${OUTPUT_POSTFIX:=".xml"}
: ${SET:=""}
: ${LOG:="${OUTPUT_PREFIX}.log"}
popd > /dev/null
START_TIME=$(date +"%Y-%m-%d %H:%M")

function usage() {
    cat <<EOF
Usage:
  FROM=YYYY-HH-MMThh:mm:ssZ UNTIL=YYYY-HH-MMThh:mm:ssZ ./oai_harvest.sh [config]

Sample call:
  FROM=2021-12-04T00:00:00Z UNTIL=2021-12-04T25:59:59Z ./oai_harvest.sh

UNTIL parameter is optional

Basic setup:
  Create a file 'oai_harvest.conf' in the folder where oai_harvest.sh is called.
  The file defines the properties for the harvest.
  A template is available as 'oai_harvest.conf.template' and contains properties
  like this sample:

# ---------------------------------------------------
: \${SERVER:="https://<KUANA_STAGE_MACHINE>/OAI-PMH/"}
: \${METADATA_PREFIX:="xip"}
: \${USER_PASS:="<USER>:<PASS>"}
: \${OUTPUT_PREFIX:="preservica_\$(date +%Y%m%d-%H%M)"}
: \${OUTPUT_POSTFIX:=".xml"}
# ---------------------------------------------------



For Royal Danish Library developers only:
  Basic setup can be done by calling aegis 'kb init' from the root of a
  ds-datahandler checkout.
EOF
    
    exit $1
}

check_parameters() {
    if [[ -z "$SERVER" ]]; then
        >&2 echo "Error: No SERVER specified"$'\n'
        usage 10
    fi
}

################################################################################
# FUNCTIONS
################################################################################

# Print setup information
info() {
    cat <<EOF
OAI-PMH harvest setup taken fron $CONFIG

SERVER=$SERVER
FROM=$FROM
UNTIL=$UNTIL
METADATA_PREFIX=$METADATA_PREFIX
CONTENT_TYPE=$CONTENT_TYPE
OUTPUT_PREFIX=$OUTPUT_PREFIX
OUTPUT_POSTFIX=$OUTPUT_POSTFIX
SET=$SET
EOF
    if [[ -z "$USER_PASS" ]]; then
        echo "USER_PASS=<Not present>"
    else
        echo "USER_PASS=<Present>"
    fi
    echo ""
}

# Perform harvest
harvest() {
    local RESUMPTION_TOKEN=""
    local COUNTER=1
    local BASE=""
    if [[ ! -z "$UNTIL" ]]; then
      BASE="${SERVER}?verb=ListRecords&from=${FROM}&until=${UNTIL}&metadataPrefix=${METADATA_PREFIX}"
    else
      BASE="${SERVER}?verb=ListRecords&from=${FROM}&metadataPrefix=${METADATA_PREFIX}"
   
    fi 
    
    local START_TIME=$(date +%s)
    if [[ ! -z "$SET" ]]; then
        BASE="${BASE}&set=${SET}"
    fi 
    
    echo "Storing harvested records in ${OUTPUT_PREFIX}_.....${OUTPUT_POSTFIX}"

    while [[ true ]]; do
        local LSTART=$(date +%s)
        local DEST="${OUTPUT_PREFIX}_$(printf "%.10d" $COUNTER)${OUTPUT_POSTFIX}"
        
        if [[ -z "$RESUMPTION_TOKEN" ]]; then
            local CALL="$BASE"
        else
            local CALL="${SERVER}?verb=ListRecords&resumptionToken=${RESUMPTION_TOKEN}"
        fi

        echo -n " - $CALL -> $DEST"
        if [[ -z "$USER_PASS" ]]; then
            curl -s -X POST "$CALL" -H "Content-Type: ${CONTENT_TYPE}" > "$DEST"
        else
            curl -s -X POST "$CALL" -H "Content-Type: ${CONTENT_TYPE}" --user "$USER_PASS" > "$DEST"
        fi

	if grep -q "401 returned" $DEST ; then
	    echo "Failed to get batch due to 401 error. Backing off and the retrying..."
	    sleep 3
	    continue
	fi

        RESUMPTION_TOKEN="$(grep -o '<resumptionToken>.*</resumptionToken>' "$DEST" | sed 's/<resumptionToken>\(.*\)<\/resumptionToken>/\1/')"

        local LEND=$(date +%s)
        LSECONDS=$((LEND-LSTART))
        echo " (${LSECONDS} seconds)"
        
        if [[ -z "$RESUMPTION_TOKEN" ]]; then
            break
        fi

        COUNTER=$((COUNTER+1))

    done
    local END_TIME=$(date +%s)
    # SECONDS is not local as it is to be used in report()
    SECONDS=$((END_TIME-START_TIME))
}

report() {
    T=$(mktemp)
    grep -o '<datestamp>[^<]*<\/datestamp>' ${OUTPUT_PREFIX}_*${OUTPUT_POSTFIX} | cut -d: -f2- | grep -o '[0-9T:.Z-]*'| sort > "$T"
    TOTAL=$(wc -l < "$T")
    SPEED=$(echo "scale=2;$TOTAL/$SECONDS" | bc) 
    FIRST=$(head -n 1 < "$T")
    LAST=$(tail -n 1 < "$T")
    rm "$T"

    UNIQUE_IDENTIFIERS=$(LC_ALL=C grep -horP '(?<=<identifier>oai:)(.*?)(?=</identifier)' . | sort | uniq | wc -l)
    
    echo "Finished harvesting"
    echo ""
    echo "Req. from:     $FROM"
    echo "First record:  $FIRST"
    echo "Last record:   $LAST"
    echo "Req. until:    $UNTIL"
    echo ""
    echo "Total records: $TOTAL"
    echo "Total seconds: $SECONDS"
    echo "Record/second: $SPEED"
    echo "Total unique identifiers: $UNIQUE_IDENTIFIERS"
    echo ""
    echo "Harvest start: $START_TIME"
    echo "Harvest end:   $(date +"%Y-%m-%d %H:%M")"
}

###############################################################################
# CODE
###############################################################################

check_parameters "$@"

info | tee -a "$LOG"
harvest | tee -a "$LOG"
report  | tee -a "$LOG"

echo ""
echo "Report available in $LOG"
