#!/bin/bash

#split into train, dev, devtest, and test sets so that each NP only appears in one of the sets

N=$(wc -l $1 | cut -f 1 -d ' ')

slice=$(($N / 10))

cat $1 | shuf > tmp

tail --lines=$((7 * $slice)) tmp > $2/train
tail --lines=$((8 * $slice)) tmp | head --lines=$slice > $2/dev
tail --lines=$((9 * $slice)) tmp | head --lines=$slice > $2/devtest
head --lines=$slice tmp > $2/test

rm tmp
