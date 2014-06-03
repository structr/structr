#!/bin/bash
#
# benchmark.sh - Structr benchmarking tool using "ab"
#
# Author: Christian Morgner (christian.morgner@structr.com)
#
#
# <insert structr header here> :)
#


# default benchmarking page
PAGE=http://localhost:8082/jumbotron

# default name for the output file
NOW=`date +%Y%m%d-%H%m`
OUTFILE=structr-benchmark-$NOW.csv

# initial number of threads
THREADS=1

# requests / thread factor
FACTOR=100

# do warm-up?
WARMUP=1

# max threads
MAX_THREADS=1000

# process parameters
while [ "$1" != "" ]; do

    case $1 in
    
        -f)
        	shift
            OUTFILE=$1
            ;;
            
        -c)
            WARMUP=0
            ;;
            
        -n)
        	shift
            FACTOR=$1
            ;;
            
        -m)
        	shift
            MAX_THREADS=$1
            ;;
            
        *)
        	echo "usage: $0 [-c] [-f outfile] [-n requests/thread]"
            exit 1
            ;;
            
    esac
    
    # shift parameters one to the left
    shift
done

if [ $WARMUP -eq 1 ]; then

	# warm-up phase (50k requests)
	echo "Warming up with 50000 requests before benchmarking.."
	echo -n " 1000 requests: "
	ab -n1000 -c20 -q $PAGE |grep 'Requests per second' |awk '{ print $4; }'

	echo -n " 1000 requests: "
	ab -n1000 -c20 -q $PAGE |grep 'Requests per second' |awk '{ print $4; }'

	echo -n " 2000 requests: "
	ab -n2000 -c20 -q $PAGE |grep 'Requests per second' |awk '{ print $4; }'

	echo -n " 2000 requests: "
	ab -n2000 -c20 -q $PAGE |grep 'Requests per second' |awk '{ print $4; }'

	echo -n " 2000 requests: "
	ab -n2000 -c20 -q $PAGE |grep 'Requests per second' |awk '{ print $4; }'

	echo -n " 4000 requests: "
	ab -n4000 -c20 -q $PAGE |grep 'Requests per second' |awk '{ print $4; }'

	echo -n " 8000 requests: "
	ab -n8000 -c20 -q $PAGE |grep 'Requests per second' |awk '{ print $4; }'

	echo -n "10000 requests: "
	ab -n10000 -c20 -q $PAGE |grep 'Requests per second' |awk '{ print $4; }'

	echo -n "10000 requests: "
	ab -n10000 -c20 -q $PAGE |grep 'Requests per second' |awk '{ print $4; }'

	echo -n "10000 requests: "
	ab -n10000 -c20 -q $PAGE |grep 'Requests per second' |awk '{ print $4; }'
fi

# benchmarking phase
echo "Benchmarking, output goes to $OUTFILE.."
while [ $THREADS -le $MAX_THREADS ]; do

	COUNT=$((THREADS*FACTOR))
	
	# limit count to 2000
	if [ $COUNT -gt 1000 ]; then
		COUNT=1000
	fi

	echo -n "$COUNT requests, $THREADS threads: "
	
	# create header row
	HEADER="$HEADER$THREADS,"

	# do benchmarking
	RESULT=`ab -n$COUNT -c$THREADS -q $PAGE |grep 'Requests per second' |awk '{ print $4; }'`
	
	echo $RESULT

	# create data row
	DATA="$DATA$RESULT,"

	STEP=$((THREADS / 10))

	# limit step increment to 5
	if [ $STEP -eq 0 ]; then STEP=1; fi

	THREADS=$((THREADS+STEP))

done

echo "$HEADER" >$OUTFILE 
echo "$DATA" >>$OUTFILE

echo "Benchmarking done.."