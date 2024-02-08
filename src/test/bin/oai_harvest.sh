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
: ${UNTIL:="$(date +%Y-%m-%d)"}   # Also today's date
: ${METADATA_PREFIX:="xip"}
: ${CONTENT_TYPE:="application/xml"}
: ${USER_PASS:=""} # user:password
: ${OUTPUT_PREFIX:="oai_$(date +%Y%m%d-%H%M)_"}
: ${OUTPUT_POSTFIX:=".xml"}
: ${SET:=""}
popd > /dev/null

function usage() {
    echo "Usage: ./oai_harvest.sh [config]"
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
}

# Perform harvest
harvest() {
    local RESUMPTION_TOKEN=""
    local COUNTER=1
    local BASE="${SERVER}?verb=ListRecords&from=${FROM}&until=${UNTIL}&metadataPrefix=${METADATA_PREFIX}"
    if [[ ! -z "$SET" ]]; then
        BASE="${BASE}&set=${SET}"
    fi 
    
    echo "Performing harvest"

    while [[ true ]]; do
        local DEST="${OUTPUT_PREFIX}$(printf "%.5d" $COUNTER)${OUTPUT_POSTFIX}"
        
        if [[ -z "$RESUMPTION_TOKEN" ]]; then
            local CALL="$BASE"
        else
            local CALL="${SERVER}?verb=ListRecords&resumptionToken=${RESUMPTION_TOKEN}"
        fi

        echo " - $CALL -> $DEST"
        if [[ -z "$USER_PASS" ]]; then
            curl -s -X POST "$CALL" -H "Content-Type: ${CONTENT_TYPE}" > "$DEST"
        else
            curl -s -X POST "$CALL" -H "Content-Type: ${CONTENT_TYPE}" --user "$USER_PASS" > "$DEST"
        fi

        RESUMPTION_TOKEN="$(grep -o '<resumptionToken>.*</resumptionToken>' "$DEST" | sed 's/<resumptionToken>\(.*\)<\/resumptionToken>/\1/')"
        if [[ -z "$RESUMPTION_TOKEN" ]]; then
            break
        fi

        COUNTER=$((COUNTER+1))
    done
}

###############################################################################
# CODE
###############################################################################

check_parameters "$@"

START_TIME=$(date +%s)
info
echo ""
harvest
END_TIME=$(date +%s)
echo "Finished in $((END_TIME-START_TIME)) seconds"
