#!/bin/bash

START=1001
END=1006

# for ID in $(seq $START $END); do
#     alacritty msg create-window \
#       --command bash -i -c "
#         cd /run/media/jaysurya/_mnt_data/Development/cn/cn_project/build &&
#         echo 'Running Peer $ID' &&
#         java PeerProcess $ID ;
#         echo '';
#         echo 'Peer $ID finished.' ;
#         exec bash" &
#
#     sleep 0.5   # optional delay so windows appear nicely staggered
#
PROJECT_DIR="/Users/YOUR_USERNAME/path/to/cn_project/build"

for ID in $(seq $START $END); do
    osascript <<EOF
tell application "Terminal"
    do script "cd $PROJECT_DIR; echo Running Peer $ID; java PeerProcess $ID; echo 'Peer $ID finished.'; exec bash"
end tell
EOF
    sleep 0.5   # optional delay so windows appear nicely staggered
done
