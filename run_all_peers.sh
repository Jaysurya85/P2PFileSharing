#!/bin/bash

START=1001
END=1006

for ID in $(seq $START $END); do
    alacritty msg create-window \
      --command bash -i -c "
        cd /run/media/jaysurya/_mnt_data/Development/cn/cn_project/build &&
        echo 'Running Peer $ID' &&
        java PeerProcess $ID ;
        echo '';
        echo 'Peer $ID finished.' ;
        exec bash" &

    sleep 0.5   # optional delay so windows appear nicely staggered
done
