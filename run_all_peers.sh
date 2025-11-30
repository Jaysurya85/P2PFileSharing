#!/bin/bash

START=1001
END=1006

for ID in $(seq $START $END); do
    alacritty msg create-window --command bash -c \
    "cd /run/media/jaysurya/_mnt_data/Development/cn/cn_project/build && echo Running Peer $ID && java PeerProcess $ID ;  exec bash" &
done
